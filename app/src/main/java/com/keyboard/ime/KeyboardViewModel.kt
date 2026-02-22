package com.keyboard.ime

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keyboard.model.Suggestion
import com.keyboard.prediction.SuggestionEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class KeyboardViewModel(
    private val suggestionEngine: SuggestionEngine
) : ViewModel() {
    private val _suggestions = MutableStateFlow<List<Suggestion>>(emptyList())
    val suggestions: StateFlow<List<Suggestion>> = _suggestions.asStateFlow()

    fun refreshSuggestions(token: String, previous: String?, inputType: Int) {
        viewModelScope.launch {
            _suggestions.value = suggestionEngine.suggest(token, previous, inputType)
        }
    }

    fun learn(word: String) {
        viewModelScope.launch { suggestionEngine.learnWord(word) }
    }
}
