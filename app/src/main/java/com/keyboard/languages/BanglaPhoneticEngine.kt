package com.keyboard.languages

/**
 * Bangla Phonetic Transliteration Engine
 *
 * Converts Romanized input to Bangla script using rule-based transliteration.
 * Features:
 * - Comprehensive conjunct consonant support
 * - Automatic vowel sign attachment
 * - Reph initiation for 'r' combinations
 * - Inherent vowel support (o) to prevent unwanted conjuncts
 */
class BanglaPhoneticEngine {

    /**
     * Phonetic rules in priority order (longer matches first).
     * Matches are case-sensitive to allow distinction where needed (e.g., t vs T).
     */
    private val orderedRules = listOf(
        // === Three-letter conjuncts ===
        "ksh" to "ক্ষ", "Ksh" to "ক্ষ", "KSH" to "ক্ষ",
        "ngk" to "ঙ্ক", "NGK" to "ঙ্ক",
        "ngg" to "ঙ্গ", "NGG" to "ঙ্গ",
        "ndh" to "ন্ধ", "NDH" to "ন্ধ",
        "nth" to "ন্থ", "NTH" to "ন্থ",
        "rri" to "ঋ", "Rri" to "ঋ",

        // === Two-letter conjuncts/modifiers ===
        "kh" to "খ", "Kh" to "খ", "KH" to "খ",
        "gh" to "ঘ", "Gh" to "ঘ", "GH" to "ঘ",
        "ch" to "ছ", "Ch" to "ছ", "CH" to "ছ",
        "chh" to "ছ", "Chh" to "ছ", "CHH" to "ছ",
        "jh" to "ঝ", "Jh" to "ঝ", "JH" to "ঝ",
        "th" to "থ", "tH" to "থ", "Th" to "ঠ", "TH" to "ঠ",
        "dh" to "ধ", "dH" to "ধ", "Dh" to "ঢ", "DH" to "ঢ",
        "ph" to "ফ", "Ph" to "ফ", "PH" to "ফ",
        "bh" to "ভ", "Bh" to "ভ", "BH" to "ভ",
        "sh" to "শ", "Sh" to "শ", "SH" to "শ",
        "ss" to "ষ", "Ss" to "ষ", "SS" to "ষ",
        "ng" to "ঙ", "Ng" to "ঙ", "NG" to "ঙ",
        "rh" to "ঢ়", "Rh" to "ঢ়", "RH" to "ঢ়",
        "qq" to "ঁ",
        "^" to "ঁ",
        "NG" to "ং",
        " :" to "ঃ",

        // === Vowels (standalone) ===
        "aa" to "আ", "AA" to "আ",
        "ii" to "ঈ", "II" to "ঈ",
        "uu" to "ঊ", "UU" to "ঊ",
        "oi" to "ঐ", "OI" to "ঐ",
        "ou" to "ঔ", "OU" to "ঔ",
        "au" to "ঔ", "AU" to "ঔ",
        "a" to "আ", "A" to "আ",
        "i" to "ই", "I" to "ঈ",
        "u" to "উ", "U" to "ঊ",
        "e" to "এ", "E" to "এ",
        "o" to "অ", "O" to "ও",

        // === Consonants (single) ===
        "k" to "ক", "K" to "ক",
        "g" to "গ", "G" to "গ",
        "c" to "চ", "C" to "চ",
        "j" to "জ", "J" to "জ",
        "t" to "ত", "T" to "ট",
        "d" to "দ", "D" to "ড",
        "n" to "ন", "N" to "ণ",
        "p" to "প", "P" to "প",
        "b" to "ব", "B" to "ব",
        "m" to "ম", "M" to "ম",
        "y" to "য়", "Y" to "য়",
        "r" to "র", "R" to "ড়",
        "x" to "ক্স", "X" to "ক্স",
        "q" to "ক", "Q" to "ক",
        "l" to "ল", "L" to "ল",
        "s" to "স", "S" to "ষ",
        "h" to "হ", "H" to "হ",
        "f" to "ফ", "F" to "ফ",
        "v" to "ভ", "V" to "ভ",
        "z" to "য", "Z" to "য",
        "w" to "ওয়", "W" to "ওয়"
    )

    /**
     * Vowel signs (kar) that attach to consonants
     */
    private val vowelSigns = mapOf(
        "a" to "া", "A" to "া",
        "i" to "ি", "I" to "ী",
        "ii" to "ী", "II" to "ী",
        "u" to "ু", "U" to "ূ",
        "uu" to "ূ", "UU" to "ূ",
        "e" to "ে", "E" to "ে",
        "o" to "",     // inherent vowel (breaks conjunct cluster)
        "O" to "ো",    // o-kar
        "oi" to "ৈ", "OI" to "ৈ",
        "ou" to "ৌ", "OU" to "ৌ",
        "rri" to "ৃ"
    )

    /**
     * Consonants that form yantara (্য) combinations
     */
    private val yantaraConsonants = setOf(
        "k", "K", "kh", "Kh", "KH", "g", "G", "gh", "Gh", "GH", "c", "C", "ch", "Ch", "CH", "chh", "Chh", "CHH", "j", "J", "jh", "Jh", "JH",
        "t", "T", "th", "Th", "TH", "tH", "d", "D", "dh", "Dh", "DH", "dH", "n", "N", "p", "P", "ph", "Ph", "PH", "b", "B", "bh", "Bh", "BH",
        "m", "M", "l", "L", "s", "S", "sh", "Sh", "SH", "ss", "Ss", "SS", "h", "H", "ksh", "Ksh", "KSH", "rh", "Rh", "RH", "R",
        "ndh", "NDH", "nth", "NTH", "ngk", "NGK", "ngg", "NGG"
    )

    /**
     * Main transliteration function
     */
    fun transliterate(input: String): String {
        if (input.isBlank()) return ""

        val tokens = parseTokens(input)
        val out = StringBuilder()

        for ((index, token) in tokens.withIndex()) {
            val prev = tokens.getOrNull(index - 1)

            when {
                // Handle yantara (্য) for 'y' after consonant
                token.equals("y", ignoreCase = true) && prev?.isConsonantToken() == true && prev in yantaraConsonants -> {
                    out.append("্য")
                }

                // Handle antasta ya (য়) - standalone 'y'
                token.equals("y", ignoreCase = true) -> {
                    out.append("য়")
                }

                // Vowel signs attach to previous consonant
                token in vowelSigns && prev?.isConsonantToken() == true -> {
                    out.append(vowelSigns.getValue(token))
                }

                // Double consonant (and not a vowel sign like 'o') - insert halant
                token.isConsonantToken() && prev?.isConsonantToken() == true -> {
                    out.append("্")
                    out.append(resolve(token))
                }

                // Default: resolve token to Bangla
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

    private fun resolve(token: String): String {
        return orderedRules.firstOrNull { it.first == token }?.second ?: token
    }

    private fun String.isConsonantToken(): Boolean {
        return this in CONSONANT_TOKENS
    }

    companion object {
        private val CONSONANT_TOKENS = setOf(
            "k", "K", "kh", "Kh", "KH", "g", "G", "gh", "Gh", "GH", "ng", "Ng", "NG",
            "c", "C", "ch", "Ch", "CH", "chh", "Chh", "CHH", "j", "J", "jh", "Jh", "JH",
            "t", "T", "th", "Th", "TH", "tH", "d", "D", "dh", "Dh", "DH", "dH", "n", "N",
            "p", "P", "ph", "Ph", "PH", "b", "B", "bh", "Bh", "BH", "m", "M",
            "y", "Y", "r", "R", "l", "L", "s", "S", "sh", "Sh", "SH", "ss", "Ss", "SS", "h", "H",
            "f", "F", "v", "V", "z", "Z", "w", "W", "ksh", "Ksh", "KSH", "x", "X", "q", "Q",
            "ndh", "NDH", "nth", "NTH", "ngk", "NGK", "ngg", "NGG"
        )
    }
}
