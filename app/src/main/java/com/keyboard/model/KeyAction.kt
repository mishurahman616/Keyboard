package com.keyboard.model

sealed class KeyAction {
    data class Character(val char: String) : KeyAction()
    data object Backspace : KeyAction()
    data object Space : KeyAction()
    data object Enter : KeyAction()
    data object Shift : KeyAction()
    data object LanguageSwitch : KeyAction()
    data object Symbols : KeyAction()
}
