package com.keyboard.ui

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.keyboard.R

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val seekBarHeight = findViewById<SeekBar>(R.id.seekbar_height)
        val tvHeightValue = findViewById<TextView>(R.id.tv_height_value)
        val seekBarWidth = findViewById<SeekBar>(R.id.seekbar_width)
        val tvWidthValue = findViewById<TextView>(R.id.tv_width_value)
        val btnBack = findViewById<Button>(R.id.btn_back)

        val prefs = getSharedPreferences("keyboard_settings", Context.MODE_PRIVATE)
        
        // Height Setup
        val currentHeight = prefs.getInt("key_height_percent", 50)
        seekBarHeight.progress = currentHeight
        updateHeightLabel(tvHeightValue, currentHeight)

        seekBarHeight.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                updateHeightLabel(tvHeightValue, progress)
                prefs.edit().putInt("key_height_percent", progress).apply()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Width Setup
        val currentWidth = prefs.getInt("key_width_percent", 0)
        seekBarWidth.progress = currentWidth
        updateWidthLabel(tvWidthValue, currentWidth)

        seekBarWidth.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                updateWidthLabel(tvWidthValue, progress)
                prefs.edit().putInt("key_width_percent", progress).apply()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun updateHeightLabel(view: TextView, progress: Int) {
        val label = when {
            progress < 30 -> "Small"
            progress < 60 -> "Standard"
            progress < 85 -> "Large"
            else -> "Extra Large"
        }
        view.text = "Height: $label ($progress%)"
    }

    private fun updateWidthLabel(view: TextView, progress: Int) {
        val label = when {
            progress == 0 -> "Full Width"
            progress < 30 -> "Narrow"
            progress < 60 -> "Compact"
            else -> "Very Compact"
        }
        view.text = "Side Padding: $label ($progress%)"
    }
}
