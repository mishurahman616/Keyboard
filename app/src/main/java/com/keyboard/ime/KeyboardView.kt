package com.keyboard.ime

import android.content.Context
import android.util.AttributeSet
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
    private var isEmojiMode = false
    private val handler = Handler(Looper.getMainLooper())

    data class EmojiData(val emoji: String, val description: String, val usage: String)

    private val emojiList = listOf(
        EmojiData("😀", "Grinning face", "Use when happy or friendly"),
        EmojiData("😃", "Smiling face with big eyes", "Use when genuinely happy"),
        EmojiData("😄", "Smiling face with smiling eyes", "Use when very happy"),
        EmojiData("😁", "Beaming face with smiling eyes", "Use when excited and happy"),
        EmojiData("😆", "Grinning squinting face", "Use when laughing hard"),
        EmojiData("😅", "Grinning face with sweat", "Use when relieved or nervous"),
        EmojiData("😂", "Face with tears of joy", "Use when something is hilarious"),
        EmojiData("🤣", "Rolling on the floor laughing", "Use when extremely funny"),
        EmojiData("😊", "Smiling face with smiling eyes", "Use when pleased or content"),
        EmojiData("😇", "Smiling face with halo", "Use when being innocent or good"),
        EmojiData("🙂", "Slightly smiling face", "Use for polite acknowledgment"),
        EmojiData("🙃", "Upside-down face", "Use when being sarcastic or silly"),
        EmojiData("😉", "Winking face", "Use when joking or flirting"),
        EmojiData("😌", "Relieved face", "Use when relaxed or thankful"),
        EmojiData("😍", "Smiling face with heart-eyes", "Use when you love something"),
        EmojiData("🥰", "Smiling face with hearts", "Use when feeling loved"),
        EmojiData("😘", "Face blowing a kiss", "Use to send affection"),
        EmojiData("😗", "Kissing face", "Use when showing affection"),
        EmojiData("😙", "Kissing face with smiling eyes", "Use when happily affectionate"),
        EmojiData("😚", "Kissing face with closed eyes", "Use when sweetly affectionate"),
        EmojiData("😋", "Face savoring food", "Use when food is delicious"),
        EmojiData("😛", "Face with tongue", "Use when being playful"),
        EmojiData("😝", "Squinting face with tongue", "Use when being silly"),
        EmojiData("😜", "Winking face with tongue", "Use when joking around"),
        EmojiData("🤪", "Zany face", "Use when being crazy or wild"),
        EmojiData("🤨", "Face with raised eyebrow", "Use when skeptical"),
        EmojiData("🧐", "Face with monocle", "Use when examining closely"),
        EmojiData("🤓", "Nerd face", "Use when being geeky or studious"),
        EmojiData("😎", "Smiling face with sunglasses", "Use when being cool or confident"),
        EmojiData("🤩", "Star-struck", "Use when amazed by someone"),
        EmojiData("🥳", "Partying face", "Use when celebrating"),
        EmojiData("😏", "Smirking face", "Use when being suggestive"),
        EmojiData("😒", "Unamused face", "Use when unimpressed"),
        EmojiData("😞", "Disappointed face", "Use when let down"),
        EmojiData("😔", "Pensive face", "Use when thinking deeply"),
        EmojiData("😟", "Worried face", "Use when concerned"),
        EmojiData("😕", "Confused face", "Use when puzzled"),
        EmojiData("🙁", "Slightly frowning face", "Use when sad"),
        EmojiData("☹️", "Frowning face", "Use when unhappy"),
        EmojiData("😣", "Persevering face", "Use when struggling"),
        EmojiData("😖", "Confounded face", "Use when frustrated"),
        EmojiData("😫", "Tired face", "Use when exhausted"),
        EmojiData("😩", "Weary face", "Use when overwhelmed"),
        EmojiData("🥺", "Pleading face", "Use when begging or cute"),
        EmojiData("😢", "Crying face", "Use when sad"),
        EmojiData("😭", "Loudly crying face", "Use when very sad or laughing"),
        EmojiData("😤", "Face with steam from nose", "Use when annoyed"),
        EmojiData("😠", "Angry face", "Use when mad"),
        EmojiData("😡", "Pouting face", "Use when very angry"),
        EmojiData("🤬", "Face with symbols on mouth", "Use when furious"),
        EmojiData("🤯", "Exploding head", "Use when mind blown"),
        EmojiData("😳", "Flushed face", "Use when embarrassed"),
        EmojiData("🥵", "Hot face", "Use when too hot"),
        EmojiData("🥶", "Cold face", "Use when freezing"),
        EmojiData("😱", "Face screaming in fear", "Use when shocked"),
        EmojiData("😨", "Fearful face", "Use when scared"),
        EmojiData("😰", "Anxious face with sweat", "Use when worried"),
        EmojiData("😥", "Sad but relieved face", "Use when worried but okay"),
        EmojiData("😓", "Downcast face with sweat", "Use when stressed"),
        EmojiData("🤗", "Smiling face with open hands", "Use when hugging"),
        EmojiData("🤔", "Thinking face", "Use when pondering"),
        EmojiData("🤭", "Face with hand over mouth", "Use when surprised or giggling"),
        EmojiData("🤫", "Shushing face", "Use when asking for quiet"),
        EmojiData("🤥", "Lying face", "Use when being dishonest"),
        EmojiData("😶", "Face without mouth", "Use when speechless"),
        EmojiData("😐", "Neutral face", "Use when indifferent"),
        EmojiData("😑", "Expressionless face", "Use when annoyed"),
        EmojiData("😬", "Grimacing face", "Use when awkward"),
        EmojiData("🙄", "Face with rolling eyes", "Use when annoyed"),
        EmojiData("😯", "Hushed face", "Use when surprised"),
        EmojiData("😦", "Frowning face with open mouth", "Use when shocked"),
        EmojiData("😧", "Anguished face", "Use when in pain"),
        EmojiData("😮", "Face with open mouth", "Use when surprised"),
        EmojiData("😲", "Astonished face", "Use when amazed"),
        EmojiData("🥱", "Yawning face", "Use when tired or bored"),
        EmojiData("😴", "Sleeping face", "Use when sleepy"),
        EmojiData("🤤", "Drooling face", "Use when desiring something"),
        EmojiData("😪", "Sleepy face", "Use when drowsy"),
        EmojiData("😵", "Knocked-out face", "Use when dizzy"),
        EmojiData("🤐", "Zipper-mouth face", "Use when keeping secret"),
        EmojiData("🥴", "Woozy face", "Use when intoxicated"),
        EmojiData("🤢", "Nauseated face", "Use when sick"),
        EmojiData("🤮", "Face vomiting", "Use when disgusted"),
        EmojiData("🤧", "Sneezing face", "Use when sick"),
        EmojiData("😷", "Face with medical mask", "Use when sick or precautions"),
        EmojiData("🤒", "Face with thermometer", "Use when ill"),
        EmojiData("🤕", "Face with head-bandage", "Use when injured"),
        EmojiData("🤑", "Money-mouth face", "Use about wealth"),
        EmojiData("🤠", "Cowboy hat face", "Use when adventurous"),
        EmojiData("😈", "Smiling face with horns", "Use when mischievous"),
        EmojiData("👿", "Angry face with horns", "Use when evil or angry"),
        EmojiData("👹", "Ogre", "Use when monstrous"),
        EmojiData("👺", "Goblin", "Use when tricky"),
        EmojiData("🤡", "Clown face", "Use when silly"),
        EmojiData("💩", "Pile of poo", "Use when joking or silly"),
        EmojiData("👻", "Ghost", "Use for spooky fun"),
        EmojiData("💀", "Skull", "Use when dead or laughing hard"),
        EmojiData("☠️", "Skull and crossbones", "Use for danger or poison")
    )
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

        findViewById<android.widget.ImageButton>(R.id.key_paste)?.setOnClickListener {
            dispatchKey(KeyAction.Paste)
        }

        findViewById<android.widget.ImageButton>(R.id.key_emoji)?.setOnClickListener {
            toggleEmojiMode()
        }

        findViewById<android.widget.ImageButton>(R.id.btn_close_emoji)?.setOnClickListener {
            if (isEmojiMode) toggleEmojiMode()
        }

        findViewById<android.widget.ImageButton>(R.id.btn_emoji_backspace)?.let { button ->
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

    private fun toggleEmojiMode() {
        isEmojiMode = !isEmojiMode
        val keyboardContainer = findViewById<LinearLayout>(R.id.keyboard_rows_container)
        val emojiPicker = findViewById<LinearLayout>(R.id.emoji_picker_container)
        val emojiRecycler = findViewById<RecyclerView>(R.id.emoji_recycler)
        val emojiButton = findViewById<android.widget.ImageButton>(R.id.key_emoji)

        if (isEmojiMode) {
            keyboardContainer.visibility = View.GONE
            emojiPicker.visibility = View.VISIBLE
            emojiButton.setImageResource(R.drawable.ic_keyboard)
            setupEmojiRecycler(emojiRecycler)
        } else {
            keyboardContainer.visibility = View.VISIBLE
            emojiPicker.visibility = View.GONE
            emojiButton.setImageResource(R.drawable.ic_emoji)
        }
    }

    private fun setupEmojiRecycler(recyclerView: RecyclerView) {
        recyclerView.layoutManager = GridLayoutManager(context, 7)
        recyclerView.adapter = EmojiAdapter(emojiList) { emoji ->
            dispatchKey(KeyAction.Character(emoji))
        }
    }

    private class EmojiAdapter(
        private val emojis: List<EmojiData>,
        private val onClick: (String) -> Unit
    ) : RecyclerView.Adapter<EmojiAdapter.ViewHolder>() {

        class ViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val tv = TextView(parent.context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    parent.width / 7,
                    150
                )
                gravity = android.view.Gravity.CENTER
                textSize = 24f
                val attrs = intArrayOf(android.R.attr.selectableItemBackground)
                val typedArray = context.obtainStyledAttributes(attrs)
                background = typedArray.getDrawable(0)
                typedArray.recycle()
                isClickable = true
                isFocusable = true
            }
            return ViewHolder(tv)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val emojiData = emojis[position]
            holder.textView.text = emojiData.emoji
            holder.textView.setOnClickListener { onClick(emojiData.emoji) }
            holder.textView.setOnLongClickListener {
                val tooltipText = "${emojiData.description}\n${emojiData.usage}"
                android.widget.Toast.makeText(holder.textView.context, tooltipText, android.widget.Toast.LENGTH_LONG).show()
                true
            }
        }

        override fun getItemCount() = emojis.size
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
