package com.keyboard.prediction

import kotlin.math.min

class AutoCorrect(private val trie: Trie) {

    fun correct(token: String, maxDistance: Int = 2): String {
        if (token.length < 3) return token
        val candidates = trie.findByPrefix(token.take(1), limit = 50)
        val best = candidates
            .map { it.first }
            .map { it to levenshtein(it, token.lowercase()) }
            .filter { it.second <= maxDistance }
            .minByOrNull { it.second }
        return best?.first ?: token
    }

    private fun levenshtein(a: String, b: String): Int {
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j

        for (i in 1..a.length) {
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                dp[i][j] = min(
                    min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[a.length][b.length]
    }
}
