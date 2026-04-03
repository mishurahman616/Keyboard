package com.keyboard.ui

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.setPadding
import com.keyboard.model.Suggestion

class CandidateView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : HorizontalScrollView(context, attrs, defStyleAttr) {

    private lateinit var container: LinearLayout
    private var predictionEnabled: Boolean = true
    private var onCandidateClickListener: ((String) -> Unit)? = null

    init {
        setupContainer()
        isHorizontalScrollBarEnabled = false
    }

    private fun setupContainer() {
        container = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.MATCH_PARENT
            )
        }
        addView(container)
    }

    fun render(items: List<Suggestion>) {
        container.removeAllViews()
        if (!predictionEnabled || items.isEmpty()) return

        items.forEach { suggestion ->
            container.addView(createCandidateView(suggestion.word))
        }
    }

    fun renderSimple(words: List<String>) {
        container.removeAllViews()
        if (!predictionEnabled || words.isEmpty()) return

        words.forEach { word ->
            container.addView(createCandidateView(word))
        }
    }

    private fun createCandidateView(word: String): TextView {
        return TextView(context).apply {
            text = word
            textSize = 16f
            setPadding(24)
            gravity = Gravity.CENTER
            setBackgroundResource(android.R.drawable.list_selector_background)
            isClickable = true
            isFocusable = true
            setOnClickListener {
                onCandidateClickListener?.invoke(word)
            }
        }
    }

    fun setOnCandidateClickListener(listener: (String) -> Unit) {
        onCandidateClickListener = listener
    }

    fun setPredictionEnabled(enabled: Boolean) {
        predictionEnabled = enabled
        if (!enabled) {
            container.removeAllViews()
        }
    }
}
