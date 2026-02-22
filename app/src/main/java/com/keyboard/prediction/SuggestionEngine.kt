package com.keyboard.prediction

import android.text.InputType
import com.keyboard.data.WordDao
import com.keyboard.model.Suggestion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SuggestionEngine(
    private val trie: Trie,
    private val autoCorrect: AutoCorrect,
    private val wordDao: WordDao
) {

    suspend fun suggest(
        currentToken: String,
        previousToken: String?,
        inputType: Int
    ): List<Suggestion> = withContext(Dispatchers.Default) {
        if (isPasswordType(inputType) || currentToken.isBlank()) return@withContext emptyList()

        val corrected = autoCorrect.correct(currentToken)
        val prefix = trie.findByPrefix(corrected, limit = 6)
            .map { Suggestion(it.first, it.second.toDouble(), "trie") }

        val nextWord = previousToken
            ?.takeIf { it.isNotBlank() }
            ?.let { token -> wordDao.getNextWordCandidates(token, 3) }
            .orEmpty()
            .map { Suggestion(it.word, it.frequency.toDouble(), "next-word") }

        (prefix + nextWord)
            .distinctBy { it.word }
            .sortedByDescending { it.score }
            .take(6)
    }

    suspend fun learnWord(word: String) {
        if (word.length < 2) return
        trie.insert(word)
        wordDao.incrementFrequency(word.lowercase())
    }

    private fun isPasswordType(inputType: Int): Boolean {
        val variation = inputType and InputType.TYPE_MASK_VARIATION
        return variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
    }
}
