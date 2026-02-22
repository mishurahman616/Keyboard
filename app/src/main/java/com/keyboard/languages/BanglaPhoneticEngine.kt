package com.keyboard.languages

class BanglaPhoneticEngine {
    private val orderedRules = listOf(
        "ksh" to "ক্ষ", "ngk" to "ঙ্ক", "ng" to "ং",
        "chh" to "ছ", "kh" to "খ", "gh" to "ঘ", "th" to "থ", "dh" to "ধ", "ph" to "ফ", "bh" to "ভ", "sh" to "শ",
        "rr" to "ড়", "rh" to "ঢ়",
        "a" to "অ", "i" to "ই", "ii" to "ঈ", "u" to "উ", "uu" to "ঊ", "e" to "এ", "oi" to "ঐ", "o" to "ও", "ou" to "ঔ",
        "k" to "ক", "g" to "গ", "c" to "চ", "j" to "জ", "t" to "ত", "d" to "দ", "n" to "ন", "p" to "প", "b" to "ব", "m" to "ম", "r" to "র", "l" to "ল", "s" to "স", "h" to "হ", "y" to "য়"
    )

    private val vowelSigns = mapOf(
        "a" to "া", "i" to "ি", "ii" to "ী", "u" to "ু", "uu" to "ূ", "e" to "ে", "oi" to "ৈ", "o" to "ো", "ou" to "ৌ"
    )

    fun transliterate(input: String): String {
        val tokens = parseTokens(input.lowercase())
        val out = StringBuilder()

        for ((index, token) in tokens.withIndex()) {
            val prev = tokens.getOrNull(index - 1)
            when {
                token == "r" && tokens.getOrNull(index + 1)?.firstOrNull()?.isLetter() == true && prev?.isConsonantToken() == false -> {
                    out.append("র্") // Reph initiation
                }
                token in vowelSigns && prev?.isConsonantToken() == true -> out.append(vowelSigns.getValue(token))
                token.isConsonantToken() && prev?.isConsonantToken() == true -> {
                    out.append("্")
                    out.append(resolve(token))
                }
                else -> out.append(resolve(token))
            }
        }
        return out.toString()
    }

    private fun parseTokens(raw: String): List<String> {
        val tokens = mutableListOf<String>()
        var cursor = 0
        while (cursor < raw.length) {
            val match = orderedRules
                .map { it.first }
                .filter { raw.startsWith(it, cursor) }
                .maxByOrNull { it.length }
            if (match != null) {
                tokens += match
                cursor += match.length
            } else {
                tokens += raw[cursor].toString()
                cursor++
            }
        }
        return tokens
    }

    private fun resolve(token: String): String = orderedRules.firstOrNull { it.first == token }?.second ?: token

    private fun String.isConsonantToken(): Boolean = this in setOf(
        "k", "kh", "g", "gh", "c", "chh", "j", "t", "th", "d", "dh", "n", "p", "ph", "b", "bh", "m", "r", "l", "sh", "s", "h", "y", "ksh"
    )
}
