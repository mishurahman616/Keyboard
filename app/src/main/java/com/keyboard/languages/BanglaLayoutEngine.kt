package com.keyboard.languages

class BanglaLayoutEngine {
    private val probhatMap = mapOf(
        'q' to 'ঙ', 'w' to 'য', 'e' to 'ড', 'r' to 'প', 't' to 'ট',
        'y' to 'চ', 'u' to 'জ', 'i' to 'হ', 'o' to 'গ', 'p' to 'ড়',
        'a' to 'ৃ', 's' to 'ূ', 'd' to 'ি', 'f' to 'া', 'g' to '্',
        'h' to 'ব', 'j' to 'ক', 'k' to 'ত', 'l' to 'দ',
        'z' to 'ো', 'x' to 'ে', 'c' to 'অ', 'v' to 'ভ', 'b' to 'ন',
        'n' to 'ম', 'm' to 'স'
    )

    fun mapKey(key: Char): String = probhatMap[key.lowercaseChar()]?.toString() ?: key.toString()
}
