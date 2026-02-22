package com.keyboard.ime

import android.inputmethodservice.InputMethodService
import android.view.View
import android.text.InputType
import android.view.inputmethod.EditorInfo
import com.keyboard.R
import com.keyboard.data.DictionaryDatabase
import com.keyboard.languages.BanglaLayoutEngine
import com.keyboard.languages.BanglaPhoneticEngine
import com.keyboard.languages.EnglishEngine
import com.keyboard.model.LanguageMode
import com.keyboard.prediction.AutoCorrect
import com.keyboard.prediction.SuggestionEngine
import com.keyboard.prediction.Trie
import com.keyboard.theme.ThemeManager
import com.keyboard.ui.CandidateView

class KeyboardService : InputMethodService() {
    private lateinit var keyboardView: KeyboardView
    private lateinit var candidateView: CandidateView

    private val trie = Trie()
    private val englishEngine = EnglishEngine()
    private val phoneticEngine = BanglaPhoneticEngine()
    private val layoutEngine = BanglaLayoutEngine()

    private var languageMode: LanguageMode = LanguageMode.ENGLISH

    override fun onCreateInputView(): View {
        ThemeManager.apply(this)
        keyboardView = layoutInflater.inflate(R.layout.keyboard_layout, null) as KeyboardView
        keyboardView.setOnKeyListener(::onKeyPressed)
        return keyboardView
    }

    override fun onCreateCandidatesView(): View {
        candidateView = CandidateView(this)
        return candidateView
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        if (attribute != null) {
            candidateView.setPredictionEnabled(!isPassword(attribute.inputType))
        }
    }

    private fun onKeyPressed(text: String) {
        val translated = when (languageMode) {
            LanguageMode.ENGLISH -> englishEngine.normalize(text)
            LanguageMode.BANGLA_PHONETIC -> phoneticEngine.transliterate(text)
            LanguageMode.BANGLA_LAYOUT -> text.map(layoutEngine::mapKey).joinToString(separator = "")
        }
        currentInputConnection.commitText(translated, 1)
    }

    private fun isPassword(inputType: Int): Boolean {
        val variation = inputType and InputType.TYPE_MASK_VARIATION
        return variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
    }

    private fun suggestionEngine(): SuggestionEngine {
        val db = DictionaryDatabase.instance(this)
        return SuggestionEngine(trie, AutoCorrect(trie), db.wordDao())
    }
}
