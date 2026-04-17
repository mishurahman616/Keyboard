package com.keyboard.ime

import android.inputmethodservice.InputMethodService
import android.text.InputType
import android.content.Intent
import android.view.ContextThemeWrapper
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.lifecycleScope
import com.keyboard.R
import com.keyboard.data.DictionaryDatabase
import com.keyboard.languages.BanglaLayoutEngine
import com.keyboard.languages.BanglaPhoneticEngine
import com.keyboard.languages.EnglishEngine
import com.keyboard.model.KeyAction
import com.keyboard.model.LanguageMode
import com.keyboard.prediction.AutoCorrect
import com.keyboard.prediction.SuggestionEngine
import com.keyboard.prediction.Trie
import com.keyboard.theme.ThemeManager
import com.keyboard.ui.CandidateView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Keyboard IME Service
 *
 * Implements a multilingual keyboard with:
 * - English QWERTY input
 * - Bangla Phonetic (romanized) input with smart diff-based updates
 * - Bangla Probhat Layout input
 * - Trie-based autocomplete suggestions
 * - User dictionary learning
 */
class KeyboardService : InputMethodService(), LifecycleOwner {

    private var keyboardView: KeyboardView? = null
    private var candidateView: CandidateView? = null

    private lateinit var suggestionEngine: SuggestionEngine
    private val trie = Trie()
    private val englishEngine = EnglishEngine()
    private val phoneticEngine = BanglaPhoneticEngine()
    private val layoutEngine = BanglaLayoutEngine()

    private var languageMode: LanguageMode = LanguageMode.ENGLISH
    private var isShifted = false
    private var isSymbolsMode = false
    private var currentWord = StringBuilder()
    private var previousWord: String? = null

    // Track last transliteration for diff-based updates
    private var lastTransliteration: String = ""
    private var composingStart: Int = -1

    private var suggestionJob: Job? = null
    private val lifecycleRegistry = LifecycleRegistry(this)

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override fun onCreate() {
        super.onCreate()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        initializeEngine()
    }

    override fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        super.onDestroy()
    }

    private fun initializeEngine() {
        val db = DictionaryDatabase.instance(this)
        suggestionEngine = SuggestionEngine(trie, AutoCorrect(trie), db.wordDao())
        
        // Load initial dictionaries in background
        lifecycleScope.launch(Dispatchers.IO) {
            // Priority 1: Common English Stop Words (Highest Priority)
            loadCommonWords()

            // Priority 2: Raw word lists (Standard Priority)
            loadDictionary("dictionaries/english_raw.txt", "en")
            loadDictionary("dictionaries/bangla_raw.txt", "bn")
            
            // Priority 3: Learned words from DB
            val learnedWords = db.wordDao().getAll()
            learnedWords.forEach { trie.insert(it.word, it.frequency + 200) }
            
            android.util.Log.d("KeyboardService", "Dictionary loading complete")
        }
    }

    private fun loadCommonWords() {
        val commonEn = listOf(
            "the", "be", "to", "of", "and", "a", "in", "that", "have", "I",
            "it", "for", "not", "on", "with", "he", "as", "you", "do", "at",
            "this", "but", "his", "by", "from", "they", "we", "say", "her", "she",
            "or", "an", "will", "my", "one", "all", "would", "there", "their", "what",
            "so", "up", "out", "if", "about", "who", "get", "which", "go", "me"
        )
        commonEn.forEach { trie.insert(it, 1000) }

        val commonBn = listOf(
            "আমি", "তুমি", "সে", "আমরা", "আপনার", "এই", "কি", "করে", "হয়", "না",
            "জীবন", "মানুষ", "দেশ", "কাজ", "ভাল", "মা", "বাবা", "ভাই", "বোন", "বন্ধু"
        )
        commonBn.forEach { trie.insert(it, 1000) }
    }

    private fun loadDictionary(assetPath: String, lang: String) {
        val isEnglish = lang == "en"
        // English filter: Only alphabetic words, at least 2 chars, or 'a'/'I'
        val englishWordRegex = Regex("^[a-zA-Z]{2,}|^[aAIi]$")

        try {
            assets.open(assetPath).bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    if (line.isBlank()) return@forEach
                    val parts = line.split(",")
                    
                    var word: String? = null
                    var freq = 100 // Default frequency for raw list

                    if (parts.size >= 2) {
                        word = parts[0].trim()
                        freq = parts[1].trim().toIntOrNull() ?: 100
                    } else {
                        val lineStr = line.trim()
                        if (lineStr.isNotEmpty()) {
                            val tabParts = lineStr.split("\t")
                            word = if (tabParts.size >= 2) tabParts[1].trim() else tabParts[0].trim()
                        }
                    }

                    if (word != null && word.isNotEmpty()) {
                        if (isEnglish) {
                            // Apply English sanity filter
                            if (word.matches(englishWordRegex)) {
                                trie.insert(word, freq)
                            }
                        } else {
                            // For Bangla, just ensure it's not numbers/symbols (basic check)
                            if (!word.any { it.isDigit() }) {
                                trie.insert(word, freq)
                            }
                        }
                    }
                }
            }
            android.util.Log.d("KeyboardService", "Loaded dictionary: $assetPath")
        } catch (e: Exception) {
            android.util.Log.e("KeyboardService", "Error loading dictionary $assetPath", e)
        }
    }

    override fun onCreateInputView(): View {
        android.util.Log.d("KeyboardService", "onCreateInputView called")

        // Wrap context with theme to ensure Material3 attributes are resolved
        val contextThemeWrapper = ContextThemeWrapper(this, R.style.Theme_Keyboard)
        val themedInflater = layoutInflater.cloneInContext(contextThemeWrapper)

        return try {
            val view = themedInflater.inflate(R.layout.keyboard_layout, null) as KeyboardView
            keyboardView = view

            // Setup key listener
            view.setOnKeyListener { action ->
                handleKeyAction(action)
            }

            // Setup settings button
            view.findViewById<View>(R.id.btn_open_settings)?.setOnClickListener {
                val intent = Intent(this, com.keyboard.ui.SettingsActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }

            // Setup candidate click listener
            view.setCandidateClickListener { candidate ->
                commitCandidate(candidate)
            }

            // Initialize candidate view reference in KeyboardView
            view.findViewById<CandidateView>(R.id.candidate_view)?.let {
                candidateView = it
            }

            android.util.Log.d("KeyboardService", "onCreateInputView successful: $view")
            view
        } catch (e: Exception) {
            android.util.Log.e("KeyboardService", "Error inflating keyboard layout", e)
            // Return a simple view to avoid crash and see if it shows up
            View(this).apply {
                layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    500
                )
                setBackgroundColor(android.graphics.Color.RED)
            }
        }
    }

    override fun onEvaluateInputViewShown(): Boolean {
        super.onEvaluateInputViewShown()
        return true
    }

    override fun onWindowShown() {
        super.onWindowShown()
        android.util.Log.d("KeyboardService", "onWindowShown called")
        keyboardView?.applySettings()
    }

    override fun onCreateCandidatesView(): View? {
        return null
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        resetInputState()

        // Check if predictions should be enabled
        val isPassword = attribute?.let { isPasswordField(it.inputType) } ?: false
        candidateView?.setPredictionEnabled(!isPassword)
        keyboardView?.updateCandidates(emptyList())
    }

    override fun onFinishInput() {
        super.onFinishInput()
        resetInputState()
    }

    private fun resetInputState() {
        currentWord.clear()
        lastTransliteration = ""
        composingStart = -1
    }

    private fun handleKeyAction(action: KeyAction) {
        android.util.Log.d("KeyboardService", "handleKeyAction: $action")
        val ic: InputConnection = currentInputConnection ?: return

        when (action) {
            is KeyAction.Character -> handleCharacterInput(ic, action.char)
            KeyAction.Backspace -> handleBackspace(ic)
            KeyAction.Space -> handleSpace(ic)
            KeyAction.Enter -> handleEnter(ic)
            KeyAction.Shift -> handleShift()
            KeyAction.LanguageSwitch -> handleLanguageSwitch()
            KeyAction.Symbols -> handleSymbolsToggle()
            KeyAction.Tutorial -> handleTutorial()
            KeyAction.Emoji -> handleEmojiToggle()
            KeyAction.Copy -> handleCopy(ic)
            KeyAction.Paste -> handlePaste(ic)
        }
    }

    private fun handleCopy(ic: InputConnection) {
        val selected = ic.getSelectedText(0)
        if (selected != null && selected.isNotEmpty()) {
            val clipboard = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("Keyboard Copy", selected)
            clipboard.setPrimaryClip(clip)
            android.widget.Toast.makeText(this, "Copied to clipboard", android.widget.Toast.LENGTH_SHORT).show()
        } else {
            // If no text is selected, some users expect a "Select All" behavior or a hint
            android.widget.Toast.makeText(this, "Select text first to copy", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun handlePaste(ic: InputConnection) {
        val clipboard = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = clipboard.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val textToPaste = clip.getItemAt(0).text
            if (textToPaste != null) {
                ic.commitText(textToPaste, 1)
            }
        }
    }

    private fun handleTutorial() {
        val intent = Intent(this, com.keyboard.ui.MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    private fun handleEmojiToggle() {
        // Emoji toggle logic is now handled in KeyboardView to show/hide the emoji picker.
    }

    private fun handleCharacterInput(ic: InputConnection, rawChar: String) {
        val char = if (isShifted) rawChar.uppercase() else rawChar.lowercase()
        
        val translated = when (languageMode) {
            LanguageMode.ENGLISH -> char
            LanguageMode.BANGLA_PHONETIC -> {
                val newWord = currentWord.toString() + char
                phoneticEngine.transliterate(newWord)
            }
            LanguageMode.BANGLA_LAYOUT -> {
                layoutEngine.mapKey(
                    rawChar.firstOrNull() ?: rawChar[0],
                    isShifted,
                    isSymbolsMode
                )
            }
        }

        if (isWordPart(translated)) {
            if (languageMode == LanguageMode.BANGLA_PHONETIC) {
                applyTransliterationDiff(ic, translated)
                currentWord.append(char)
            } else {
                ic.commitText(translated, 1)
                currentWord.append(translated)
            }
            requestSuggestions()
        } else {
            ic.commitText(translated, 1)
            handleWordBreak()
            resetInputState()
            keyboardView?.updateCandidates(emptyList())
        }

        // Reset shift after typing if not in Caps Lock
        if (isShifted && !isCapsLock) {
            isShifted = false
            keyboardView?.setShifted(false)
        }
    }

    private fun isWordPart(text: String): Boolean {
        if (text.isEmpty()) return false
        return text.all { c ->
            val type = Character.getType(c).toByte()
            c.isLetter() || 
            type == Character.NON_SPACING_MARK || 
            type == Character.COMBINING_SPACING_MARK
        }
    }

    /**
     * Apply only the changed portion of transliteration using diff algorithm.
     * This prevents the flickering caused by deleting and retyping the entire word.
     */
    private fun applyTransliterationDiff(ic: InputConnection, newTransliteration: String) {
        if (lastTransliteration.isEmpty()) {
            // First character - just commit
            ic.commitText(newTransliteration, 1)
        } else {
            // Find common prefix between old and new
            val commonPrefixLength = findCommonPrefixLength(lastTransliteration, newTransliteration)

            // Calculate what needs to change
            val charsToDelete = lastTransliteration.length - commonPrefixLength
            val charsToAdd = newTransliteration.substring(commonPrefixLength)

            android.util.Log.d("KeyboardService", "Diff: delete $charsToDelete, add '$charsToAdd'")

            // Delete only the changed suffix
            if (charsToDelete > 0) {
                ic.deleteSurroundingText(charsToDelete, 0)
            }

            // Add the new suffix
            if (charsToAdd.isNotEmpty()) {
                ic.commitText(charsToAdd, 1)
            }
        }

        lastTransliteration = newTransliteration
    }

    /**
     * Find the length of common prefix between two strings
     */
    private fun findCommonPrefixLength(a: String, b: String): Int {
        val minLength = minOf(a.length, b.length)
        for (i in 0 until minLength) {
            if (a[i] != b[i]) return i
        }
        return minLength
    }

    private fun handleBackspace(ic: InputConnection) {
        if (currentWord.isNotEmpty()) {
            currentWord.deleteAt(currentWord.length - 1)

            if (languageMode == LanguageMode.BANGLA_PHONETIC) {
                // Recalculate transliteration after backspace
                val newTransliteration = if (currentWord.isNotEmpty()) {
                    phoneticEngine.transliterate(currentWord.toString())
                } else ""

                applyTransliterationDiff(ic, newTransliteration)
                requestSuggestions()
            } else {
                deleteLastCharProperly(ic)
                requestSuggestions()
            }
        } else {
            deleteLastCharProperly(ic)
        }
    }

    /**
     * Properly deletes the last character, handling multi-byte characters (emojis)
     * by detecting surrogate pairs and supplementary characters.
     */
    private fun deleteLastCharProperly(ic: InputConnection) {
        // Get text before cursor to check what we're deleting
        val before = ic.getTextBeforeCursor(2, 0) ?: return

        if (before.isEmpty()) return

        // Check if the last character is a supplementary character (emoji)
        // Emojis are typically in the range U+10000 and above, encoded as surrogate pairs
        val lastChar = before.last()
        val charsToDelete = if (lastChar.isLowSurrogate() && before.length > 1) {
            // This is the second half of a surrogate pair (emoji), delete both
            2
        } else {
            1
        }

        ic.deleteSurroundingText(charsToDelete, 0)
    }

    private fun handleSpace(ic: InputConnection) {
        handleWordBreak()
        ic.commitText(" ", 1)
        resetInputState()
        keyboardView?.updateCandidates(emptyList())
    }

    private fun handleEnter(ic: InputConnection) {
        handleWordBreak()
        ic.performEditorAction(EditorInfo.IME_ACTION_GO)
        resetInputState()
        keyboardView?.updateCandidates(emptyList())
    }

    private fun handleWordBreak() {
        if (currentWord.isNotEmpty()) {
            previousWord = currentWord.toString()
            learnCurrentWord()
        }
    }

    private var isCapsLock = false
    private var lastShiftClickTime = 0L

    private fun handleShift() {
        if (isSymbolsMode) {
            isShifted = !isShifted
            keyboardView?.setShifted(isShifted)
            return
        }

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastShiftClickTime < 300) {
            // Double click: Toggle Caps Lock
            isCapsLock = !isCapsLock
            isShifted = isCapsLock
            lastShiftClickTime = 0 // Reset
        } else {
            // Single click: Toggle Shift
            if (isCapsLock) {
                isCapsLock = false
                isShifted = false
            } else {
                isShifted = !isShifted
            }
            lastShiftClickTime = currentTime
        }

        keyboardView?.setShifted(isShifted)
    }

    private fun handleLanguageSwitch() {
        languageMode = when (languageMode) {
            LanguageMode.ENGLISH -> LanguageMode.BANGLA_PHONETIC
            LanguageMode.BANGLA_PHONETIC -> LanguageMode.BANGLA_LAYOUT
            LanguageMode.BANGLA_LAYOUT -> LanguageMode.ENGLISH
        }

        // Notify view about language change
        keyboardView?.setLanguage(languageMode)

        // Clear symbols mode when switching language
        isSymbolsMode = false
        keyboardView?.setSymbolsMode(false)

        // Reset input state
        resetInputState()
        keyboardView?.updateCandidates(emptyList())

        android.util.Log.d("KeyboardService", "Switched to $languageMode")
    }

    private fun handleSymbolsToggle() {
        isSymbolsMode = !isSymbolsMode
        keyboardView?.setSymbolsMode(isSymbolsMode)

        // If in Bangla Layout mode, symbols should show Latin/symbol characters
        if (languageMode == LanguageMode.BANGLA_LAYOUT) {
            keyboardView?.setLanguage(languageMode) // Refresh labels
        }
    }

    private fun commitCandidate(candidate: String) {
        val ic = currentInputConnection ?: return

        // Replace the current word with the selected candidate
        if (currentWord.isNotEmpty()) {
            // Delete the typed characters using composing region
            val deleteCount = if (languageMode == LanguageMode.BANGLA_PHONETIC) {
                lastTransliteration.length
            } else {
                currentWord.length
            }
            ic.deleteSurroundingText(deleteCount, 0)
        }

        // Commit the selected candidate
        ic.commitText(candidate, 1)

        // Learn the word
        lifecycleScope.launch {
            suggestionEngine.learnWord(candidate)
        }

        previousWord = candidate
        resetInputState()
        keyboardView?.updateCandidates(emptyList())
    }

    private fun requestSuggestions() {
        // Debounce suggestions
        suggestionJob?.cancel()
        suggestionJob = lifecycleScope.launch {
            delay(50) // Small debounce for performance
            
            val token = if (languageMode == LanguageMode.BANGLA_PHONETIC) {
                phoneticEngine.transliterate(currentWord.toString())
            } else {
                currentWord.toString()
            }

            if (token.isNotEmpty()) {
                try {
                    val suggestions = suggestionEngine.suggest(
                        token,
                        previousWord,
                        EditorInfo.TYPE_TEXT_VARIATION_NORMAL
                    )
                    keyboardView?.updateCandidates(suggestions.map { it.word })
                } catch (e: Exception) {
                    android.util.Log.e("KeyboardService", "Error getting suggestions", e)
                }
            } else {
                keyboardView?.updateCandidates(emptyList())
            }
        }
    }

    private fun learnCurrentWord() {
        val word = if (languageMode == LanguageMode.BANGLA_PHONETIC) {
            phoneticEngine.transliterate(currentWord.toString())
        } else {
            currentWord.toString()
        }

        if (word.length >= 2) {
            lifecycleScope.launch {
                try {
                    suggestionEngine.learnWord(word)
                } catch (e: Exception) {
                    android.util.Log.e("KeyboardService", "Error learning word", e)
                }
            }
        }
    }

    private fun isPasswordField(inputType: Int): Boolean {
        val variation = inputType and InputType.TYPE_MASK_VARIATION
        return variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
    }
}
