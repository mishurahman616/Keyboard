package com.keyboard.ime

import android.content.Context
import android.util.AttributeSet
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.keyboard.R
import com.keyboard.model.KeyAction
import com.keyboard.model.LanguageMode
import com.keyboard.ui.CandidateView

class KeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private var onKeyListener: ((KeyAction) -> Unit)? = null
    private var candidateView: CandidateView? = null
    private val keyButtons = mutableMapOf<Int, Button>()
    private var isShifted = false
    private var isSymbolsMode = false
    private var currentLanguage = LanguageMode.ENGLISH
    private val handler = Handler(Looper.getMainLooper())
    private var backspaceRunnable: Runnable? = null

    private val qwertyKeys = mapOf(
        R.id.key_q to "q", R.id.key_w to "w", R.id.key_e to "e",
        R.id.key_r to "r", R.id.key_t to "t", R.id.key_y to "y",
        R.id.key_u to "u", R.id.key_i to "i", R.id.key_o to "o",
        R.id.key_p to "p", R.id.key_a to "a", R.id.key_s to "s",
        R.id.key_d to "d", R.id.key_f to "f", R.id.key_g to "g",
        R.id.key_h to "h", R.id.key_j to "j", R.id.key_k to "k",
        R.id.key_l to "l", R.id.key_z to "z", R.id.key_x to "x",
        R.id.key_c to "c", R.id.key_v to "v", R.id.key_b to "b",
        R.id.key_n to "n", R.id.key_m to "m"
    )

    private val symbolsKeys = mapOf(
        R.id.key_q to "[", R.id.key_w to "]", R.id.key_e to "{",
        R.id.key_r to "}", R.id.key_t to "#", R.id.key_y to "%",
        R.id.key_u to "^", R.id.key_i to "*", R.id.key_o to "+",
        R.id.key_p to "=", R.id.key_a to "_", R.id.key_s to "\\",
        R.id.key_d to "|", R.id.key_f to "~", R.id.key_g to "<",
        R.id.key_h to ">", R.id.key_j to "$", R.id.key_k to "€",
        R.id.key_l to "£", R.id.key_z to "•", R.id.key_x to "¶",
        R.id.key_c to "÷", R.id.key_v to "×", R.id.key_b to "{",
        R.id.key_n to "}", R.id.key_m to "!"
    )

    private val banglaNumberKeys = mapOf(
        R.id.key_1 to "১", R.id.key_2 to "২", R.id.key_3 to "৩",
        R.id.key_4 to "৪", R.id.key_5 to "৫", R.id.key_6 to "৬",
        R.id.key_7 to "৭", R.id.key_8 to "৮", R.id.key_9 to "৯",
        R.id.key_0 to "০"
    )

    private val banglaLayoutKeys = mapOf(
        R.id.key_q to "ঙ", R.id.key_w to "য", R.id.key_e to "ড",
        R.id.key_r to "প", R.id.key_t to "ট", R.id.key_y to "চ",
        R.id.key_u to "জ", R.id.key_i to "হ", R.id.key_o to "গ",
        R.id.key_p to "ড়", R.id.key_a to "ু", R.id.key_s to "ৃ",
        R.id.key_d to "ি", R.id.key_f to "া", R.id.key_g to "্",
        R.id.key_h to "ব", R.id.key_j to "ক", R.id.key_k to "ত",
        R.id.key_l to "দ", R.id.key_z to "ো", R.id.key_x to "ে",
        R.id.key_c to "অ", R.id.key_v to "র", R.id.key_b to "ন",
        R.id.key_n to "ম", R.id.key_m to "স"
    )

    private val banglaLayoutShiftedKeys = mapOf(
        R.id.key_q to "ঙ", R.id.key_w to "য", R.id.key_e to "ঢ",
        R.id.key_r to "ফ", R.id.key_t to "ঠ", R.id.key_y to "ছ",
        R.id.key_u to "ঝ", R.id.key_i to "ঞ", R.id.key_o to "ঘ",
        R.id.key_p to "ঢ়", R.id.key_a to "ূ", R.id.key_s to "ষ",
        R.id.key_d to "ী", R.id.key_f to "অ", R.id.key_g to "্",
        R.id.key_h to "ভ", R.id.key_j to "খ", R.id.key_k to "থ",
        R.id.key_l to "ধ", R.id.key_z to "ৌ", R.id.key_x to "ৈ",
        R.id.key_c to "আ", R.id.key_v to "ভ", R.id.key_b to "ণ",
        R.id.key_n to "ঙ", R.id.key_m to "শ"
    )

    init {
        orientation = VERTICAL
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        android.util.Log.d("KeyboardView", "onFinishInflate called")
        setupKeys()
    }

    fun setOnKeyListener(listener: (KeyAction) -> Unit) {
        onKeyListener = listener
    }

    fun setCandidateClickListener(listener: (String) -> Unit) {
        candidateView?.setOnCandidateClickListener(listener)
    }

    fun updateCandidates(candidates: List<String>) {
        candidateView?.renderSimple(candidates)
    }

    fun setShifted(shifted: Boolean) {
        isShifted = shifted
        updateKeyLabels()
    }

    fun setSymbolsMode(symbols: Boolean) {
        isSymbolsMode = symbols
        updateKeyLabels()
    }

    fun setLanguage(mode: LanguageMode) {
        currentLanguage = mode
        updateKeyLabels()
    }

    private fun setupKeys() {
        android.util.Log.d("KeyboardView", "setupKeys started")
        candidateView = findViewById(R.id.candidate_view)

        // Dedicated Number row
        val numberIds = listOf(
            R.id.key_1, R.id.key_2, R.id.key_3, R.id.key_4, R.id.key_5,
            R.id.key_6, R.id.key_7, R.id.key_8, R.id.key_9, R.id.key_0
        )
        for (id in numberIds) {
            findViewById<Button>(id)?.let { button ->
                keyButtons[id] = button
                button.setOnClickListener {
                    dispatchKey(KeyAction.Character(button.text.toString()))
                }
            }
        }

        // Letter/Symbol keys
        for (id in qwertyKeys.keys) {
            findViewById<Button>(id)?.let { button ->
                keyButtons[id] = button
                button.setOnClickListener {
                    val text = button.text.toString()
                    dispatchKey(KeyAction.Character(text))
                }
            }
        }

        // Special keys
        findViewById<Button>(R.id.key_shift)?.setOnClickListener {
            dispatchKey(KeyAction.Shift)
        }

        findViewById<Button>(R.id.key_backspace)?.let { button ->
            button.setOnClickListener {
                dispatchKey(KeyAction.Backspace)
            }
            button.setOnLongClickListener {
                startBackspaceRepeating()
                true
            }
            button.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
                    stopBackspaceRepeating()
                }
                false
            }
        }

        findViewById<Button>(R.id.key_space)?.setOnClickListener {
            dispatchKey(KeyAction.Space)
        }

        findViewById<Button>(R.id.key_enter)?.setOnClickListener {
            dispatchKey(KeyAction.Enter)
        }

        findViewById<Button>(R.id.key_language)?.setOnClickListener {
            dispatchKey(KeyAction.LanguageSwitch)
        }

        findViewById<Button>(R.id.key_symbols)?.setOnClickListener {
            dispatchKey(KeyAction.Symbols)
        }

        findViewById<Button>(R.id.key_comma)?.let { button ->
            button.setOnClickListener {
                dispatchKey(KeyAction.Character(","))
            }
            button.setOnLongClickListener {
                dispatchKey(KeyAction.Tutorial)
                true
            }
        }

        findViewById<Button>(R.id.key_period)?.setOnClickListener {
            dispatchKey(KeyAction.Character("."))
        }

        findViewById<android.widget.ImageButton>(R.id.key_copy)?.setOnClickListener {
            dispatchKey(KeyAction.Copy)
        }

        findViewById<android.widget.ImageButton>(R.id.key_paste)?.setOnClickListener {
            dispatchKey(KeyAction.Paste)
        }

        updateKeyLabels()
    }

    private fun updateKeyLabels() {
        val mapping = when {
            isSymbolsMode -> symbolsKeys
            currentLanguage == LanguageMode.BANGLA_LAYOUT -> {
                if (isShifted) banglaLayoutShiftedKeys else banglaLayoutKeys
            }
            else -> qwertyKeys
        }

        for ((id, char) in mapping) {
            keyButtons[id]?.text = if (!isSymbolsMode && isShifted && currentLanguage != LanguageMode.BANGLA_LAYOUT) {
                char.uppercase()
            } else {
                char
            }
        }

        // Update number row for Bangla
        val numberMapping = if (currentLanguage == LanguageMode.BANGLA_LAYOUT || currentLanguage == LanguageMode.BANGLA_PHONETIC) {
            banglaNumberKeys
        } else {
            mapOf(
                R.id.key_1 to "1", R.id.key_2 to "2", R.id.key_3 to "3",
                R.id.key_4 to "4", R.id.key_5 to "5", R.id.key_6 to "6",
                R.id.key_7 to "7", R.id.key_8 to "8", R.id.key_9 to "9",
                R.id.key_0 to "0"
            )
        }

        for ((id, char) in numberMapping) {
            keyButtons[id]?.text = char
        }

        // Update shift key appearance/text
        findViewById<Button>(R.id.key_shift)?.apply {
            text = if (isSymbolsMode) "1/2" else "⇧"
            alpha = if (isShifted) 1.0f else 0.7f
        }

        findViewById<Button>(R.id.key_symbols)?.text = if (isSymbolsMode) "ABC" else "?123"

        // Update spacebar with current language name
        findViewById<Button>(R.id.key_space)?.text = when (currentLanguage) {
            LanguageMode.ENGLISH -> "English"
            LanguageMode.BANGLA_PHONETIC -> "বাংলা (Phonetic)"
            LanguageMode.BANGLA_LAYOUT -> "বাংলা (Layout)"
        }
    }

    private fun dispatchKey(action: KeyAction) {
        onKeyListener?.invoke(action)
    }

    private fun startBackspaceRepeating() {
        backspaceRunnable = object : Runnable {
            override fun run() {
                dispatchKey(KeyAction.Backspace)
                handler.postDelayed(this, 50)
            }
        }
        handler.post(backspaceRunnable!!)
    }

    private fun stopBackspaceRepeating() {
        backspaceRunnable?.let {
            handler.removeCallbacks(it)
            backspaceRunnable = null
        }
    }
}
