package com.keyboard.ui

import android.content.Context
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import com.keyboard.model.Suggestion

class CandidateView(context: Context) : HorizontalScrollView(context) {
    private val container = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
    private var predictionEnabled: Boolean = true

    init {
        addView(container)
    }

    fun render(items: List<Suggestion>) {
        container.removeAllViews()
        if (!predictionEnabled) return
        items.forEach { suggestion ->
            container.addView(TextView(context).apply {
                text = suggestion.word
                textSize = 16f
                setPadding(24, 12, 24, 12)
            })
        }
    }

    fun setPredictionEnabled(enabled: Boolean) {
        predictionEnabled = enabled
    }
}
