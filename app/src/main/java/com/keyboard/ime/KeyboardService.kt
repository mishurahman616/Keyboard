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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

    private fun handleCharacterInput(ic: InputConnection, rawChar: String) {
        val char = if (isShifted) rawChar.uppercase() else rawChar.lowercase()
        
        val translated = when (languageMode) {
            LanguageMode.ENGLISH -> {
                char
            }
            LanguageMode.BANGLA_PHONETIC -> {
                // Build the new word
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

        when (languageMode) {
            LanguageMode.BANGLA_PHONETIC -> {
                // Use diff-based update for smooth typing
                applyTransliterationDiff(ic, translated)
                currentWord.append(char)
                requestSuggestions()
            }
            else -> {
                // Direct commit for English and Layout modes
                ic.commitText(translated, 1)

                if (char.all { it.isLetter() }) {
                    currentWord.append(char)
                    requestSuggestions()
                } else {
                    handleWordBreak()
                }
            }
        }

        // Reset shift after typing (single-shift mode like mobile keyboards)
        if (isShifted && languageMode == LanguageMode.ENGLISH) {
            isShifted = false
            keyboardView?.setShifted(false)
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
                ic.deleteSurroundingText(1, 0)
                requestSuggestions()
            }
        } else {
            ic.deleteSurroundingText(1, 0)
        }
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

    private fun handleShift() {
        if (isSymbolsMode) {
            // TODO: Implement second symbols page
            android.util.Log.d("KeyboardService", "Shift in symbols mode - page 2 not implemented")
        } else {
            isShifted = !isShifted
            keyboardView?.setShifted(isShifted)
        }
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
            val token = currentWord.toString()
            if (token.length >= 1) {
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
        val word = currentWord.toString()
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
