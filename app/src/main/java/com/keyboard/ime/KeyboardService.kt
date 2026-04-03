package com.keyboard.ime

import android.inputmethodservice.InputMethodService
import android.text.InputType
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
        currentWord.clear()

        // Check if predictions should be enabled
        val isPassword = attribute?.let { isPasswordField(it.inputType) } ?: false
        candidateView?.setPredictionEnabled(!isPassword)
        keyboardView?.updateCandidates(emptyList())
    }

    override fun onFinishInput() {
        super.onFinishInput()
        currentWord.clear()
    }

    private fun handleKeyAction(action: KeyAction) {
        android.util.Log.d("KeyboardService", "handleKeyAction: $action")
        val ic: InputConnection = currentInputConnection ?: return

        when (action) {
            is KeyAction.Character -> {
                val char = action.char
                val translated = when (languageMode) {
                    LanguageMode.ENGLISH -> {
                        if (isShifted) char.uppercase() else char.lowercase()
                    }

                    LanguageMode.BANGLA_PHONETIC -> phoneticEngine.transliterate(currentWord.toString() + char)
                    LanguageMode.BANGLA_LAYOUT -> layoutEngine.mapKey(char.firstOrNull() ?: char[0])
                }

                if (languageMode == LanguageMode.BANGLA_PHONETIC && currentWord.isNotEmpty()) {
                    // Delete the previous partial transliteration
                    ic.deleteSurroundingText(currentWord.length, 0)
                    ic.commitText(translated, 1)
                } else {
                    ic.commitText(translated, 1)
                }

                // Update current word tracking
                if (char.all { it.isLetter() }) {
                    currentWord.append(char)
                    requestSuggestions()
                } else {
                    // Non-letter character breaks the word
                    if (currentWord.isNotEmpty()) {
                        previousWord = currentWord.toString()
                        learnCurrentWord()
                    }
                    currentWord.clear()
                    keyboardView?.updateCandidates(emptyList())
                }

                // Reset shift after typing
                if (isShifted) {
                    isShifted = false
                    keyboardView?.setShifted(false)
                }
            }

            KeyAction.Backspace -> {
                if (currentWord.isNotEmpty()) {
                    currentWord.deleteAt(currentWord.length - 1)
                    requestSuggestions()
                }
                ic.deleteSurroundingText(1, 0)
            }

            KeyAction.Space -> {
                if (currentWord.isNotEmpty()) {
                    previousWord = currentWord.toString()
                    learnCurrentWord()
                }
                ic.commitText(" ", 1)
                currentWord.clear()
                keyboardView?.updateCandidates(emptyList())
            }

            KeyAction.Enter -> {
                if (currentWord.isNotEmpty()) {
                    previousWord = currentWord.toString()
                    learnCurrentWord()
                }
                ic.performEditorAction(EditorInfo.IME_ACTION_GO)
                currentWord.clear()
                keyboardView?.updateCandidates(emptyList())
            }

            KeyAction.Shift -> {
                if (isSymbolsMode) {
                    // TODO: Implement second symbols page
                } else {
                    isShifted = !isShifted
                    keyboardView?.setShifted(isShifted)
                }
            }

            KeyAction.LanguageSwitch -> {
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
                
                // Clear current word when switching languages
                currentWord.clear()
                keyboardView?.updateCandidates(emptyList())
            }

            KeyAction.Symbols -> {
                isSymbolsMode = !isSymbolsMode
                keyboardView?.setSymbolsMode(isSymbolsMode)
            }
        }
    }

    private fun commitCandidate(candidate: String) {
        // Replace the current word with the selected candidate
        if (currentWord.isNotEmpty()) {
            // Delete the typed characters
            currentInputConnection?.deleteSurroundingText(currentWord.length, 0)
        }

        // Commit the selected candidate
        currentInputConnection?.commitText(candidate, 1)

        // Learn the word
        lifecycleScope.launch {
            suggestionEngine.learnWord(candidate)
        }

        previousWord = candidate
        currentWord.clear()
        keyboardView?.updateCandidates(emptyList())
    }

    private fun requestSuggestions() {
        // Debounce suggestions
        suggestionJob?.cancel()
        suggestionJob = lifecycleScope.launch {
            delay(50) // Small debounce
            val token = currentWord.toString()
            if (token.length >= 1) {
                val suggestions = suggestionEngine.suggest(
                    token,
                    previousWord,
                    EditorInfo.TYPE_TEXT_VARIATION_NORMAL
                )
                keyboardView?.updateCandidates(suggestions.map { it.word })
            } else {
                keyboardView?.updateCandidates(emptyList())
            }
        }
    }

    private fun learnCurrentWord() {
        val word = currentWord.toString()
        if (word.length >= 2) {
            lifecycleScope.launch {
                suggestionEngine.learnWord(word)
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
