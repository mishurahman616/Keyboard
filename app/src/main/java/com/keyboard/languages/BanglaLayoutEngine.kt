package com.keyboard.languages

/**
 * Bangla Probhat Layout Engine
 *
 * Implements the Probhat keyboard layout standard for Bangla typing.
 * This is a fixed-layout (non-phonetic) input method where each key
 * maps directly to a Bangla character.
 *
 * Layout Reference:
 * https://en.wikipedia.org/wiki/Probhat
 */
class BanglaLayoutEngine {

    /**
     * Standard Probhat layout mapping for letter keys
     */
    private val probhatBaseMap = mapOf(
        // Row 1: Number keys (with shift give symbols)
        '1' to "১",        '!' to "ঃ",
        '2' to "২",        '@' to "ঋ",
        '3' to "৩",        '#' to "ঁ",
        '4' to "৪",        '$' to "৳",
        '5' to "৫",        '%' to "%",
        '6' to "৬",        '^' to "্",
        '7' to "৭",        '&' to "ৎ",
        '8' to "৮",        '*' to "ঽ",
        '9' to "৯",        '(' to "(",
        '0' to "০",        ')' to ")",

        // Row 2: Top letter row
        'q' to "ঙ",
        'w' to "য",
        'e' to "ড",        'E' to "ঢ",
        'r' to "প",        'R' to "ফ",
        't' to "ট",        'T' to "ঠ",
        'y' to "চ",        'Y' to "ছ",
        'u' to "জ",        'U' to "ঝ",
        'i' to "হ",        'I' to "ঞ",
        'o' to "গ",        'O' to "ঘ",
        'p' to "ড়",        'P' to "ঢ়",
        '[' to "ে",         '{' to "ৈ",
        ']' to "ো",         '}' to "ৌ",

        // Row 3: Home row
        'a' to "ু",         'A' to "ূ",
        's' to "ৃ",         'S' to "ষ",
        'd' to "ি",         'D' to "ী",
        'f' to "া",         'F' to "অ",
        'g' to "্",         'G' to "্",
        'h' to "ব",         'H' to "ভ",
        'j' to "ক",         'J' to "খ",
        'k' to "ত",         'K' to "থ",
        'l' to "দ",         'L' to "ধ",
        ';' to "্",         ':' to "ৃ",
        '\'' to "়",        '"' to "\"",

        // Row 4: Bottom row
        'z' to "ো",         'Z' to "ৌ",
        'x' to "ে",         'X' to "ৈ",
        'c' to "অ",         'C' to "আ",
        'v' to "র",         'V' to "ভ",
        'b' to "ন",         'B' to "ণ",
        'n' to "ম",         'N' to "ঙ",
        'm' to "স",         'M' to "শ",
        ',' to ",",          '<' to "এ",
        '.' to ".",          '>' to "ঐ",
        '/' to "য",          '?' to "য়"
    )

    /**
     * Alternative characters when in symbols mode
     */
    private val symbolsMap = mapOf(
        // Row 1
        '1' to "1",   '2' to "2",   '3' to "3",
        '4' to "4",   '5' to "5",   '6' to "6",
        '7' to "7",   '8' to "8",   '9' to "9",   '0' to "0",

        // Row 2
        'q' to "[",   'w' to "]",   'e' to "{",   'r' to "}",
        't' to "#",   'y' to "%",   'u' to "^",   'i' to "*",
        'o' to "+",   'p' to "=",

        // Row 3
        'a' to "_",   's' to "\\",  'd' to "|",   'f' to "~",
        'g' to "<",   'h' to ">",   'j' to "$",   'k' to "€",
        'l' to "£",

        // Row 4
        'z' to "•",   'x' to "¶",   'c' to "÷",   'v' to "×",
        'b' to "{",   'n' to "}",  'm' to "!",
        ',' to ":",   '.' to "\"",  '/' to "?"
    )

    /**
     * Conjunct helper - returns hasant if both characters are consonants
     */
    fun getHasanta(): String = "্"

    /**
     * Map a single key press to Bangla output
     *
     * @param key The character pressed
     * @param isShifted Whether shift is active
     * @param isSymbolsMode Whether in symbols mode
     * @return The Bangla character(s) to output
     */
    fun mapKey(key: Char, isShifted: Boolean = false, isSymbolsMode: Boolean = false): String {
        // In symbols mode, use symbols map
        if (isSymbolsMode) {
            return symbolsMap[key]?.toString() ?: key.toString()
        }

        // Try shifted version first if applicable
        val lookupKey = if (isShifted) {
            val shifted = key.uppercaseChar()
            probhatBaseMap[shifted]?.let { return it }
            shifted
        } else {
            key
        }

        // Try base map
        return probhatBaseMap[lookupKey]?.toString()
            ?: probhatBaseMap[lookupKey.lowercaseChar()]?.toString()
            ?: key.toString()
    }

    /**
     * Get the display label for a key (what to show on the keyboard button)
     */
    fun getKeyLabel(key: Char, isShifted: Boolean = false): String {
        val result = if (isShifted) {
            // Check if shifted version exists
            val shifted = key.uppercaseChar()
            probhatBaseMap[shifted]
                ?: probhatBaseMap[key]?.toString()
                ?: key.toString()
        } else {
            probhatBaseMap[key]?.toString() ?: key.toString()
        }
        return result
    }

    /**
     * Get all Bangla number characters
     */
    fun getBanglaNumbers(): Map<Char, String> {
        return mapOf(
            '0' to "০", '1' to "১", '2' to "২",
            '3' to "৩", '4' to "৪", '5' to "৫",
            '6' to "৬", '7' to "৭", '8' to "৮", '9' to "৯"
        )
    }

    /**
     * Get all vowel signs (kar) for this layout
     */
    fun getVowelSigns(): Map<Char, String> {
        return mapOf(
            'f' to "া",   // aa-kar (key f)
            'd' to "ি",   // i-kar (key d)
            's' to "ূ",   // u-kar (key s)
            'a' to "ৃ",   // ri-kar (key a)
            'x' to "ে",   // e-kar (key x)
            '[' to "ে",   // e-kar alternative (key [)
            'z' to "ো",   // o-kar (key z)
            ']' to "ো"    // o-kar alternative (key ])
        )
    }

    companion object {
        // Test the layout with common words
        val TEST_WORDS = mapOf(
            "amar" to listOf('j', 'f', 'n', 'r'),      // আমার (mine)
            "bangla" to listOf('b', 'f', 'h', 'k', 'x', 'f'), // বাংলা
            "valobasa" to listOf('v', 'f', 'k', 'n', 'b', 'f'), // ভালোবাসা
            "matrivasa" to listOf('n', 'f', 'k', 'b', 'v', 'f', 'b', 'f'), // মাতৃভাষা
            "sadhinota" to listOf('m', 'f', 'd', 'h', 'j', 'k', 'f'), // স্বাধীনতা
            "bharat" to listOf('b', 'h', 'f', 'k', 'f', 'k'), // ভারত
            "pakistan" to listOf('n', 'f', 'j', 'i', 'k', 'b', 'k') // পাকিস্তান
        )
    }
}
