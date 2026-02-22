package com.keyboard.ime

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout

class KeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private var onKey: ((String) -> Unit)? = null

    fun setOnKeyListener(listener: (String) -> Unit) {
        onKey = listener
    }

    fun dispatchKey(text: String) {
        onKey?.invoke(text)
    }
}
