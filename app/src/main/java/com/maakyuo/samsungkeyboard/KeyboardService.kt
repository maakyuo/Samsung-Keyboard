package com.maakyuo.samsungkeyboard

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
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
import kotlin.math.roundToInt

class KeyboardService : InputMethodService() {
    private var shifted = false
    private var symbols = false
    private var symbolPage = 0
    private var english = false
    private var l: Int? = null
    private var v: Int? = null
    private var t = 0
    private val handler = Handler(Looper.getMainLooper())
    private var preview: PopupWindow? = null

    private val initials = listOf("ㄱ","ㄲ","ㄴ","ㄷ","ㄸ","ㄹ","ㅁ","ㅂ","ㅃ","ㅅ","ㅆ","ㅇ","ㅈ","ㅉ","ㅊ","ㅋ","ㅌ","ㅍ","ㅎ")
    private val medials = listOf("ㅏ","ㅐ","ㅑ","ㅒ","ㅓ","ㅔ","ㅕ","ㅖ","ㅗ","ㅘ","ㅙ","ㅚ","ㅛ","ㅜ","ㅝ","ㅞ","ㅟ","ㅠ","ㅡ","ㅢ","ㅣ")
    private val finals = listOf("","ㄱ","ㄲ","ㄳ","ㄴ","ㄵ","ㄶ","ㄷ","ㄹ","ㄺ","ㄻ","ㄼ","ㄽ","ㄾ","ㄿ","ㅀ","ㅁ","ㅂ","ㅄ","ㅅ","ㅆ","ㅇ","ㅈ","ㅊ","ㅋ","ㅌ","ㅍ","ㅎ")
    private val vowelJoin = mapOf("ㅗㅏ" to "ㅘ","ㅗㅐ" to "ㅙ","ㅗㅣ" to "ㅚ","ㅜㅓ" to "ㅝ","ㅜㅔ" to "ㅞ","ㅜㅣ" to "ㅟ","ㅡㅣ" to "ㅢ")
    private val finalJoin = mapOf("ㄱㅅ" to "ㄳ","ㄴㅈ" to "ㄵ","ㄴㅎ" to "ㄶ","ㄹㄱ" to "ㄺ","ㄹㅁ" to "ㄻ","ㄹㅂ" to "ㄼ","ㄹㅅ" to "ㄽ","ㄹㅌ" to "ㄾ","ㄹㅍ" to "ㄿ","ㄹㅎ" to "ㅀ","ㅂㅅ" to "ㅄ")
    private val tense = mapOf("ㄱ" to "ㄲ","ㄷ" to "ㄸ","ㅂ" to "ㅃ","ㅅ" to "ㅆ","ㅈ" to "ㅉ")
    private val shiftedMedials = mapOf("ㅐ" to "ㅒ","ㅔ" to "ㅖ")
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
        KeyboardSound.initialize(this)
    }

    override fun onDestroy() {
        preview?.dismiss()
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    private fun playClick(view: View) {
        KeyboardSound.play(this)
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
        addStyledRow(root, numberRow, 0, 0)
        val rows = when {
            symbols -> symbolPages[symbolPage].map { row ->
                row.map { key -> when (key) {
                    "‹" -> "${symbolPage + 1}/3"
                    "›" -> "⌫"
                    else -> key
                } }
            }
            english -> if (shifted) englishRows.map { row -> row.map { it.uppercase() } } else englishRows
            else -> koreanRows
        }
        rows.forEachIndexed { rowIndex, keys -> addStyledRow(root, keys, rowIndex + 1, rowIndex) }
        val bottom = if (symbols) listOf("가나다","한/영","space",".","↵")
            else listOf("?123","한/영","space",".","↵")
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
            val modifier = key in listOf("⇧","⌫","?123","↵","한/영","가나다","‹","›") || key.matches(Regex("[1-3]/3"))
            val weight = when (key) {
                "space" -> 2.65f
                "한/영" -> 1.15f
                "?123","가나다" -> 1.18f
                "⇧","⌫","↵","‹","›" -> 1.22f
                else -> if (key.matches(Regex("[1-3]/3"))) 1.22f else 1f
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
            "⇧" -> "⇧"
            else -> if (shifted && !english && !symbols) {
                (tense[key] ?: shiftedMedials[key] ?: key).trim()
            } else key.trim()
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
            textSize = when {
                key == "space" -> 10f
                key in listOf("?123","한/영","가나다") -> 11f
                key.matches(Regex("[1-3]/3")) -> 13f
                else -> 17f
            }
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
        val alternateKey = tense[key] ?: shiftedMedials[key]
        val supportsLongPress = alternateKey != null && !english && !symbols
        class PressSession(val pointerId: Int) {
            var dispatched = false
            var longHandled = false
            var fallback: Runnable? = null
            var longPress: Runnable? = null
        }
        var activeSession: PressSession? = null

        fun restoreKeyFace(view: View) {
            view.animate().cancel()
            view.animate().scaleX(1f).scaleY(1f).translationY(0f).setDuration(90).start()
        }
        fun dispatchOnce(session: PressSession, view: View, value: String) {
            if (session.dispatched) return
            session.dispatched = true
            session.fallback?.let { handler.removeCallbacks(it) }
            session.longPress?.let { handler.removeCallbacks(it) }
            playClick(view)
            onKeyPressed(value)
            if (value != key) showPreview(view, value)
        }
        fun clearSession(session: PressSession, view: View) {
            session.fallback?.let { handler.removeCallbacks(it) }
            session.longPress?.let { handler.removeCallbacks(it) }
            if (activeSession === session) activeSession = null
            hidePreview()
            restoreKeyFace(view)
            view.parent?.requestDisallowInterceptTouchEvent(false)
        }

        face.setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    activeSession?.let { previous ->
                        if (!previous.dispatched && !previous.longHandled) dispatchOnce(previous, view, key)
                        clearSession(previous, view)
                    }
                    val session = PressSession(event.getPointerId(0))
                    activeSession = session
                    view.parent?.requestDisallowInterceptTouchEvent(true)
                    view.animate().cancel()
                    view.animate().scaleX(0.94f).scaleY(0.94f)
                        .translationY(dp(1).toFloat()).setDuration(55).start()

                    session.fallback = Runnable {
                        if (activeSession === session && !session.dispatched) dispatchOnce(session, view, key)
                    }
                    handler.postDelayed(session.fallback!!, if (supportsLongPress) 520L else 55L)

                    if (supportsLongPress) {
                        session.longPress = Runnable {
                            if (activeSession === session && !session.dispatched) {
                                session.longHandled = true
                                dispatchOnce(session, view, alternateKey!!)
                            }
                        }
                        handler.postDelayed(session.longPress!!, 430L)
                    }
                    true
                }
                MotionEvent.ACTION_MOVE -> true
                MotionEvent.ACTION_POINTER_DOWN -> true
                MotionEvent.ACTION_POINTER_UP -> {
                    val session = activeSession
                    if (session != null && event.getPointerId(event.actionIndex) == session.pointerId) {
                        if (!session.dispatched && !session.longHandled) dispatchOnce(session, view, key)
                        clearSession(session, view)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val session = activeSession
                    if (session != null) {
                        if (!session.dispatched && !session.longHandled) dispatchOnce(session, view, key)
                        clearSession(session, view)
                    } else restoreKeyFace(view)
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    activeSession?.let { session ->
                        if (!session.dispatched && !session.longHandled) dispatchOnce(session, view, key)
                        clearSession(session, view)
                    }
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
            key.matches(Regex("[1-3]/3")) -> { symbolPage = (symbolPage + 1) % symbolPages.size; setInputView(buildKeyboard()) }
            key == "‹" -> { symbolPage = (symbolPage - 1 + symbolPages.size) % symbolPages.size; setInputView(buildKeyboard()) }
            key == "›" -> { symbolPage = (symbolPage + 1) % symbolPages.size; setInputView(buildKeyboard()) }
            key == "⌫" -> deleteOne(ic)
            key == "?123" -> { finishComposition(ic); symbols = true; symbolPage = 0; setInputView(buildKeyboard()) }
            key == "가나다" -> { finishComposition(ic); symbols = false; setInputView(buildKeyboard()) }
            key == "한/영" -> {
                finishComposition(ic); english = !english; symbols = false; shifted = false
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
                    typeJamo(if (shifted) tense[key] ?: shiftedMedials[key] ?: key else key, ic)
                    if (shifted) { shifted = false; setInputView(buildKeyboard()) }
                }
            }
        }
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
