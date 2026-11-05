package com.maakyuo.samsungkeyboard

import android.content.ClipboardManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import java.io.File
import java.io.FileOutputStream
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin
import org.json.JSONArray

class KeyboardService : InputMethodService() {
    private var shifted = false
    private var symbols = false
    private var symbolPage = 0
    private var clipboardMode = false
    private var english = false
    private val clipboardHistory = mutableListOf<String>()
    private var clipboardManager: ClipboardManager? = null
    private val clipboardListener = ClipboardManager.OnPrimaryClipChangedListener { captureClipboard() }
    private var l: Int? = null
    private var v: Int? = null
    private var t = 0
    private var soundPool: SoundPool? = null
    private var clickSound = 0
    private var soundReady = false
    private val handler = Handler(Looper.getMainLooper())
    private var preview: PopupWindow? = null
    private var longPressed = false
    private var pressedKey: String? = null

    private val initials = listOf("ㄱ","ㄲ","ㄴ","ㄷ","ㄸ","ㄹ","ㅁ","ㅂ","ㅃ","ㅅ","ㅆ","ㅇ","ㅈ","ㅉ","ㅊ","ㅋ","ㅌ","ㅍ","ㅎ")
    private val medials = listOf("ㅏ","ㅐ","ㅑ","ㅒ","ㅓ","ㅔ","ㅕ","ㅖ","ㅗ","ㅘ","ㅙ","ㅚ","ㅛ","ㅜ","ㅝ","ㅞ","ㅟ","ㅠ","ㅡ","ㅢ","ㅣ")
    private val finals = listOf("","ㄱ","ㄲ","ㄳ","ㄴ","ㄵ","ㄶ","ㄷ","ㄹ","ㄺ","ㄻ","ㄼ","ㄽ","ㄾ","ㄿ","ㅀ","ㅁ","ㅂ","ㅄ","ㅅ","ㅆ","ㅇ","ㅈ","ㅊ","ㅋ","ㅌ","ㅍ","ㅎ")
    private val vowelJoin = mapOf("ㅗㅏ" to "ㅘ","ㅗㅐ" to "ㅙ","ㅗㅣ" to "ㅚ","ㅜㅓ" to "ㅝ","ㅜㅔ" to "ㅞ","ㅜㅣ" to "ㅟ","ㅡㅣ" to "ㅢ")
    private val finalJoin = mapOf("ㄱㅅ" to "ㄳ","ㄴㅈ" to "ㄵ","ㄴㅎ" to "ㄶ","ㄹㄱ" to "ㄺ","ㄹㅁ" to "ㄻ","ㄹㅂ" to "ㄼ","ㄹㅅ" to "ㄽ","ㄹㅌ" to "ㄾ","ㄹㅍ" to "ㄿ","ㄹㅎ" to "ㅀ","ㅂㅅ" to "ㅄ")
    private val tense = mapOf("ㄱ" to "ㄲ","ㄷ" to "ㄸ","ㅂ" to "ㅃ","ㅅ" to "ㅆ","ㅈ" to "ㅉ")
    private val numberRow = listOf("1","2","3","4","5","6","7","8","9","0")
    private val koreanRows = listOf(
        listOf("ㅂ","ㅈ","ㄷ","ㄱ","ㅅ","ㅛ","ㅕ","ㅑ","ㅐ","ㅔ"),
        listOf("ㅁ","ㄴ","ㅇ","ㄹ","ㅎ","ㅗ","ㅓ","ㅏ","ㅣ"),
        listOf("⇧","ㅋ","ㅌ","ㅊ","ㅍ","ㅠ","ㅜ","ㅡ","⌫")
    )
    private val englishRows = listOf(
        listOf("q","w","e","r","t","y","u","i","o","p"),
        listOf("a","s","d","f","g","h","j","k","l"),
        listOf("⇧","z","x","c","v","b","n","m","⌫")
    )
    private val symbolPages = listOf(
        listOf(
            listOf("@","#","$","%","&","-","+","(",")","/"),
            listOf("*","\"","'",":",";","!","?","=","_"),
            listOf("‹","\\","~","<",">","[","]","{","}","›")
        ),
        listOf(
            listOf("€","£","¥","₩","₹","¢","₽","₿","§","¶"),
            listOf("±","×","÷","≠","≈","≤","≥","∞","∝"),
            listOf("‹","°","‰","√","∑","∏","∆","µ","Ω","›")
        ),
        listOf(
            listOf("←","→","↑","↓","↔","↕","↖","↗","↘","↙"),
            listOf("★","☆","♡","♥","◆","◇","●","○","■"),
            listOf("‹","□","△","▽","※","℃","℉","…","✓","›")
        )
    )

    override fun onCreate() {
        super.onCreate()
        initClickSound()
        clipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        loadClipboardHistory()
        clipboardManager?.addPrimaryClipChangedListener(clipboardListener)
        captureClipboard()
    }

    override fun onDestroy() {
        preview?.dismiss()
        clipboardManager?.removePrimaryClipChangedListener(clipboardListener)
        soundPool?.release()
        soundPool = null
        super.onDestroy()
    }

    private fun initClickSound() {
        try {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            soundPool = SoundPool.Builder().setMaxStreams(6).setAudioAttributes(attrs).build()
            val file = File(cacheDir, "soft_click.wav")
            val rate = 44100
            val count = (rate * 0.046).toInt()
            val pcm = ByteArray(count * 2)
            for (i in 0 until count) {
                val x = i.toDouble() / count
                val decay = (1.0 - x).coerceAtLeast(0.0).let { it * it }
                val attack = if (i < rate * 0.002) i.toDouble() / (rate * 0.002) else 1.0
                val body = sin(2.0 * PI * 720.0 * i / rate) * 0.48 +
                    sin(2.0 * PI * 1380.0 * i / rate) * 0.22 +
                    sin(2.0 * PI * 210.0 * i / rate) * 0.18
                val sample = (body * decay * attack * 12500).toInt().coerceIn(-32768, 32767)
                pcm[i * 2] = sample.toByte()
                pcm[i * 2 + 1] = (sample shr 8).toByte()
            }
            FileOutputStream(file).use { out ->
                fun writeInt(value: Int) {
                    out.write(value and 255); out.write((value shr 8) and 255)
                    out.write((value shr 16) and 255); out.write((value shr 24) and 255)
                }
                fun writeShort(value: Int) { out.write(value and 255); out.write((value shr 8) and 255) }
                out.write("RIFF".toByteArray()); writeInt(36 + pcm.size)
                out.write("WAVEfmt ".toByteArray()); writeInt(16)
                writeShort(1); writeShort(1); writeInt(rate); writeInt(rate * 2)
                writeShort(2); writeShort(16)
                out.write("data".toByteArray()); writeInt(pcm.size); out.write(pcm)
            }
            clickSound = soundPool!!.load(file.absolutePath, 1)
            soundPool!!.setOnLoadCompleteListener { _, sampleId, status ->
                if (sampleId == clickSound && status == 0) soundReady = true
            }
        } catch (_: Exception) { soundReady = false }
    }

    private fun playClick() {
        if (soundReady && clickSound != 0) soundPool?.play(clickSound, 0.55f, 0.55f, 1, 0, 1f)
    }

    override fun onCreateInputView(): View = buildKeyboard()

    private fun buildKeyboard(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(5), dp(6), dp(5), dp(7))
            background = shape(Color.rgb(22,22,26), 16, Color.rgb(235,112,151), 1)
        }
        val banner = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(7), 0, dp(5), dp(5))
        }
        val title = TextView(this).apply {
            text = "TOP SECRET  /  LEVEL 7"
            textSize = 10f
            letterSpacing = .12f
            setTextColor(Color.rgb(255,137,174))
            typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
        }
        banner.addView(title, LinearLayout.LayoutParams(0, dp(25), 1f))
        banner.addView(TextView(this).apply {
            text = "NANA  •  🐰"
            textSize = 10f
            setTextColor(Color.rgb(255,180,202))
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        })
        root.addView(banner)
        if (clipboardMode) {
            val heading = TextView(this).apply {
                text = "CLIPBOARD  •  최근 복사 항목"
                textSize = 13f
                gravity = Gravity.CENTER_VERTICAL
                setTextColor(Color.rgb(255,151,184))
                setPadding(dp(8), dp(5), dp(8), dp(5))
            }
            root.addView(heading, LinearLayout.LayoutParams(-1, dp(34)))
            if (clipboardHistory.isEmpty()) {
                val empty = TextView(this).apply {
                    text = "저장된 항목이 없습니다"
                    gravity = Gravity.CENTER
                    setTextColor(Color.LTGRAY)
                    textSize = 14f
                }
                root.addView(empty, LinearLayout.LayoutParams(-1, 0, 1f))
            } else {
                clipboardHistory.take(5).forEachIndexed { index, value ->
                    val item = makeKey("clip:$index", false, 0, index)
                    root.addView(item, LinearLayout.LayoutParams(-1, 0, 1f).apply {
                        setMargins(dp(3), dp(2), dp(3), dp(2))
                    })
                }
            }
            addStyledRow(root, listOf("닫기"), 4, 4)
            return root
        }
        addStyledRow(root, numberRow, 0, 0)
        val rows = when {
            symbols -> symbolPages[symbolPage]
            english -> englishRows
            else -> koreanRows
        }
        rows.forEachIndexed { rowIndex, keys -> addStyledRow(root, keys, rowIndex + 1, rowIndex) }
        val bottom = if (symbols) listOf("가나다","📋","한/영","space",".","↵")
            else listOf("?123","📋","한/영","space",".","↵")
        addStyledRow(root, bottom, 4, 4)
        return root
    }

    private fun addStyledRow(root: LinearLayout, keys: List<String>, rowIndex: Int, decorationRow: Int) {
        val line = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(if (rowIndex == 2) dp(8) else 0, dp(2), if (rowIndex == 2) dp(8) else 0, dp(2))
        }
        keys.forEachIndexed { keyIndex, key ->
            val modifier = key in listOf("⇧","⌫","?123","↵","한/영","가나다","‹","›","📋","닫기") || key.startsWith("clip:")
            val weight = when (key) {
                "space" -> 2.65f
                "한/영" -> 1.15f
                "?123","가나다" -> 1.18f
                "⇧","⌫","↵","‹","›","📋","닫기" -> 1.22f
                else -> if (key.matches(Regex("[1-3]/3"))) 1.22f else 1f
                else -> 1f
            }
            val keyHeight = if (rowIndex == 4) 43 else 39
            line.addView(makeKey(key, modifier, decorationRow, keyIndex), LinearLayout.LayoutParams(0, dp(keyHeight), weight).apply {
                setMargins(dp(2), dp(2), dp(2), dp(2))
            })
        }
        root.addView(line)
    }

    private fun makeKey(key: String, modifier: Boolean, row: Int, index: Int): View {
        val dark = modifier || key in listOf("🔫","🐰")
        val label = when (key) {
            "space" -> if (english) "English" else "한글"
            "⇧" -> if (shifted) "⬆" else "⇧"
            else -> if (key.startsWith("clip:")) clipboardHistory.getOrNull(key.substringAfter(":").toIntOrNull() ?: -1)?.take(34) ?: "" else key.trim()
        }
        val face = FrameLayout(this).apply {
            isClickable = true
            isFocusable = true
            background = shape(
                if (dark) Color.rgb(35,34,39) else Color.rgb(248,247,244),
                if (modifier) 7 else 6,
                if (dark) Color.rgb(92,77,88) else Color.rgb(218,210,207),
                1
            )
            elevation = dp(if (modifier) 1 else 2).toFloat()
        }
        val text = TextView(this).apply {
            this.text = label
            gravity = Gravity.CENTER
            textSize = when (key) { "space" -> 10f; "?123","한/영","가나다" -> 11f; else -> 17f }
            setTextColor(if (dark) Color.rgb(255,151,184) else Color.rgb(37,36,40))
            typeface = Typeface.create(if (dark) "sans-serif-medium" else "sans-serif", if (dark) Typeface.BOLD else Typeface.NORMAL)
        }
        face.addView(text, FrameLayout.LayoutParams(-1, -1))
        if (!dark && key !in listOf("space",",",".")) {
            val fleck = TextView(this).apply {
                this.text = listOf("·","✦","／","•","✧")[(row * 3 + index) % 5]
                textSize = 10f
                setTextColor(listOf(Color.rgb(237,105,145),Color.rgb(111,180,215),Color.rgb(239,193,93))[(row + index) % 3])
                gravity = Gravity.TOP or Gravity.END
                setPadding(0, dp(1), dp(3), 0)
            }
            face.addView(fleck, FrameLayout.LayoutParams(-1, -1))
        }
        var longHandled = false
        val longAction = Runnable {
            val alternate = tense[key]
            if (alternate != null && !english && !symbols) {
                longHandled = true
                showPreview(face, alternate)
            }
        }
        face.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    longHandled = false
                    view.isPressed = true
                    showPreview(face, label)
                    handler.postDelayed(longAction, 430)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    handler.removeCallbacks(longAction)
                    hidePreview()
                    view.isPressed = false
                    playClick()
                    if (longHandled) onKeyPressed(tense[key] ?: key) else onKeyPressed(key)
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    handler.removeCallbacks(longAction)
                    hidePreview()
                    view.isPressed = false
                    true
                }
                else -> true
            }
        }
        return face
    }

    private fun showPreview(anchor: View, value: String) {
        hidePreview()
        val bubble = TextView(this).apply {
            text = value; gravity = Gravity.CENTER
            textSize = 25f; typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(39,35,40))
            background = shape(Color.rgb(255,249,251), 13, Color.rgb(235,112,151), 2)
            elevation = dp(8).toFloat()
        }
        preview = PopupWindow(bubble, dp(54), dp(58), false).apply {
            isClippingEnabled = false
            elevation = dp(8).toFloat()
            showAsDropDown(anchor, 0, -dp(66), Gravity.TOP or Gravity.CENTER_HORIZONTAL)
        }
    }

    private fun hidePreview() {
        preview?.dismiss()
        preview = null
    }

    private fun shape(color: Int, radius: Int, stroke: Int, strokeWidth: Int) =
        GradientDrawable().apply {
            setColor(color); cornerRadius = dp(radius).toFloat()
            setStroke(dp(strokeWidth), stroke)
        }

    private fun onKeyPressed(key: String) {
        val ic = currentInputConnection ?: return
        when {
            key == "⇧" -> {
                if (symbols) symbolPage = (symbolPage + 1) % symbolPages.size else shifted = !shifted
                setInputView(buildKeyboard())
            }
            key == "‹" -> { symbolPage = (symbolPage - 1 + symbolPages.size) % symbolPages.size; setInputView(buildKeyboard()) }
            key == "›" -> { symbolPage = (symbolPage + 1) % symbolPages.size; setInputView(buildKeyboard()) }
            key == "📋" -> { captureClipboard(); clipboardMode = true; setInputView(buildKeyboard()) }
            key == "닫기" -> { clipboardMode = false; setInputView(buildKeyboard()) }
            key.startsWith("clip:") -> {
                val index = key.substringAfter(":").toIntOrNull()
                val value = index?.let { clipboardHistory.getOrNull(it) }
                if (value != null) { finishComposition(ic); ic.commitText(value, 1); clipboardMode = false; setInputView(buildKeyboard()) }
            }
            key == "⌫" -> deleteOne(ic)
            key == "?123" -> { finishComposition(ic); symbols = true; clipboardMode = false; symbolPage = 0; setInputView(buildKeyboard()) }
            key == "가나다" -> { finishComposition(ic); symbols = false; clipboardMode = false; setInputView(buildKeyboard()) }
            key == "한/영" -> {
                finishComposition(ic); english = !english; symbols = false; clipboardMode = false; shifted = false
                setInputView(buildKeyboard())
            }
            key == "space" -> { finishComposition(ic); ic.commitText(" ", 1) }
            key == "↵" -> {
                finishComposition(ic)
                val action = currentInputEditorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION) ?: EditorInfo.IME_ACTION_NONE
                if (action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED)
                    ic.performEditorAction(action) else ic.commitText("\\n", 1)
            }
            key == "," -> { finishComposition(ic); ic.commitText(",", 1) }
            key == "." -> { finishComposition(ic); ic.commitText(".", 1) }
            else -> {
                if (english || symbols || key.length > 1 && key !in initials) {
                    finishComposition(ic)
                    val value = if (english && shifted) key.uppercase() else key
                    ic.commitText(value, 1)
                    if (english && shifted) { shifted = false; setInputView(buildKeyboard()) }
                } else {
                    typeJamo(if (shifted) tense[key] ?: key else key, ic)
                    if (shifted) { shifted = false; setInputView(buildKeyboard()) }
                }
            }
        }
    }

    private fun loadClipboardHistory() {
        clipboardHistory.clear()
        val saved = getSharedPreferences("keyboard_clipboard", MODE_PRIVATE).getString("items", "[]") ?: "[]"
        try {
            val array = JSONArray(saved)
            for (i in 0 until array.length()) array.optString(i).takeIf { it.isNotBlank() }?.let { clipboardHistory.add(it) }
        } catch (_: Exception) { clipboardHistory.clear() }
    }

    private fun saveClipboardHistory() {
        val array = JSONArray()
        clipboardHistory.forEach { array.put(it) }
        getSharedPreferences("keyboard_clipboard", MODE_PRIVATE).edit().putString("items", array.toString()).apply()
    }

    private fun captureClipboard() {
        val clip = clipboardManager?.primaryClip ?: return
        if (clip.itemCount == 0) return
        val value = clip.getItemAt(0).coerceToText(this)?.toString()?.trim().orEmpty()
        if (value.isBlank()) return
        clipboardHistory.remove(value)
        clipboardHistory.add(0, value)
        while (clipboardHistory.size > 20) clipboardHistory.removeAt(clipboardHistory.lastIndex)
        saveClipboardHistory()
    }

    private fun typeJamo(char: String, ic: InputConnection) {
        if (char in medials) { addVowel(char, ic); return }
        if (char in initials) { addConsonant(char, ic); return }
        finishComposition(ic); ic.commitText(char, 1)
    }

    private fun addConsonant(char: String, ic: InputConnection) {
        val newL = initials.indexOf(char)
        val newT = finals.indexOf(char)
        when {
            l == null && v == null -> if (newL >= 0) l = newL else ic.commitText(char, 1)
            l != null && v == null -> {
                val doubled = tense[initials[l!!] ]
                if (doubled == char) l = initials.indexOf(char)
                else { finishComposition(ic); l = newL }
            }
            l != null && v != null && t == 0 -> if (newT > 0) t = newT else { finishComposition(ic); l = newL }
            l != null && v != null -> {
                val joined = finalJoin[finals[t] + char]
                if (joined != null) t = finals.indexOf(joined)
                else { finishComposition(ic); l = newL }
            }
            else -> { finishComposition(ic); l = newL }
        }
        showComposition(ic)
    }

    private fun addVowel(char: String, ic: InputConnection) {
        val newV = medials.indexOf(char)
        when {
            l == null && v == null -> v = newV
            l == null && v != null -> {
                val joined = vowelJoin[medials[v!!] + char]
                if (joined != null) v = medials.indexOf(joined) else { finishComposition(ic); v = newV }
            }
            l != null && v == null -> v = newV
            l != null && v != null && t == 0 -> {
                val joined = vowelJoin[medials[v!!] + char]
                if (joined != null) v = medials.indexOf(joined) else { finishComposition(ic); v = newV }
            }
            l != null && v != null && t != 0 -> {
                val finalText = finals[t]
                val split = finalJoin.entries.firstOrNull { it.value == finalText }?.key
                val keep = split?.first()?.toString() ?: ""
                val move = split?.last()?.toString() ?: finalText
                val oldText = syllable(l!!, v!!, if (keep.isEmpty()) 0 else finals.indexOf(keep))
                ic.commitText(oldText, 1)
                l = initials.indexOf(move).takeIf { it >= 0 }; v = newV; t = 0
            }
        }
        showComposition(ic)
    }

    private fun syllable(li: Int, vi: Int, ti: Int): String =
        (0xAC00 + (li * 21 + vi) * 28 + ti).toChar().toString()

    private fun currentComposition(): String = when {
        l != null && v != null -> syllable(l!!, v!!, t)
        l != null -> initials[l!!]
        v != null -> medials[v!!]
        else -> ""
    }

    private fun showComposition(ic: InputConnection) {
        val value = currentComposition()
        if (value.isNotEmpty()) ic.setComposingText(value, 1)
    }

    private fun finishComposition(ic: InputConnection) {
        val value = currentComposition()
        if (value.isNotEmpty()) ic.commitText(value, 1)
        l = null; v = null; t = 0
    }

    private fun deleteOne(ic: InputConnection) {
        when {
            t != 0 -> {
                val compound = finalJoin.entries.firstOrNull { it.value == finals[t] }?.key
                t = if (compound == null) 0 else finals.indexOf(compound.first().toString())
            }
            v != null -> {
                val compound = vowelJoin.entries.firstOrNull { it.value == medials[v!!] }?.key
                if (compound != null) v = medials.indexOf(compound.first().toString()) else v = null
            }
            l != null -> l = null
            else -> ic.deleteSurroundingText(1, 0)
        }
        val value = currentComposition()
        ic.setComposingText(value, 1)
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).roundToInt()
    private fun Int.absoluteValueSafe() = if (this == Int.MIN_VALUE) 0 else kotlin.math.abs(this)
}
