package com.maakyuo.samsungkeyboard

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.roundToInt

class KeyboardService : InputMethodService() {
    private var shifted = false
    private var symbols = false
    private var l: Int? = null
    private var v: Int? = null
    private var t: Int = 0

    private val initials = listOf("ㄱ","ㄲ","ㄴ","ㄷ","ㄸ","ㄹ","ㅁ","ㅂ","ㅃ","ㅅ","ㅆ","ㅇ","ㅈ","ㅉ","ㅊ","ㅋ","ㅌ","ㅍ","ㅎ")
    private val medials = listOf("ㅏ","ㅐ","ㅑ","ㅒ","ㅓ","ㅔ","ㅕ","ㅖ","ㅗ","ㅘ","ㅙ","ㅚ","ㅛ","ㅜ","ㅝ","ㅞ","ㅟ","ㅠ","ㅡ","ㅢ","ㅣ")
    private val finals = listOf("","ㄱ","ㄲ","ㄳ","ㄴ","ㄵ","ㄶ","ㄷ","ㄹ","ㄺ","ㄻ","ㄼ","ㄽ","ㄾ","ㄿ","ㅀ","ㅁ","ㅂ","ㅄ","ㅅ","ㅆ","ㅇ","ㅈ","ㅊ","ㅋ","ㅌ","ㅍ","ㅎ")
    private val vowelJoin = mapOf("ㅗㅏ" to "ㅘ","ㅗㅐ" to "ㅙ","ㅗㅣ" to "ㅚ","ㅜㅓ" to "ㅝ","ㅜㅔ" to "ㅞ","ㅜㅣ" to "ㅟ","ㅡㅣ" to "ㅢ")
    private val finalJoin = mapOf("ㄱㅅ" to "ㄳ","ㄴㅈ" to "ㄵ","ㄴㅎ" to "ㄶ","ㄹㄱ" to "ㄺ","ㄹㅁ" to "ㄻ","ㄹㅂ" to "ㄼ","ㄹㅅ" to "ㄽ","ㄹㅌ" to "ㄾ","ㄹㅍ" to "ㄿ","ㄹㅎ" to "ㅀ","ㅂㅅ" to "ㅄ")
    private val initialJoin = mapOf("ㄱㄱ" to "ㄲ","ㄷㄷ" to "ㄸ","ㅂㅂ" to "ㅃ","ㅅㅅ" to "ㅆ","ㅈㅈ" to "ㅉ")
    private val firstRow = listOf("ㅂ","ㅈ","ㄷ","ㄱ","ㅅ","ㅛ","ㅕ","ㅑ","ㅐ","ㅔ")
    private val secondRow = listOf("ㅁ","ㄴ","ㅇ","ㄹ","ㅎ","ㅗ","ㅓ","ㅏ","ㅣ")
    private val thirdRow = listOf("⇧","ㅋ","ㅌ","ㅊ","ㅍ","ㅠ","ㅜ","ㅡ","⌫")
    private val symbolRows = listOf(
        listOf("1","2","3","4","5","6","7","8","9","0"),
        listOf("@","#","$","%","&","-","+","(",")","/"),
        listOf("⇧","*","\"","' ",":",";","!","?","⌫")
    )

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

        val rows = if (symbols) symbolRows + listOf(listOf("가나다",",","한/영","space",".","↵")) else listOf(firstRow, secondRow, thirdRow, listOf("?123",",","한/영","space",".","↵"))
        rows.forEachIndexed { rowIndex, keys ->
            val line = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                setPadding(if (rowIndex == 1) dp(8) else 0, dp(2), if (rowIndex == 1) dp(8) else 0, dp(2))
            }
            keys.forEachIndexed { keyIndex, key ->
                val modifier = key in listOf("⇧","⌫","?123","↵","한/영","가나다")
                val weight = when (key) {
                    "space" -> 2.65f
                    "한/영" -> 1.15f
                    "?123","가나다" -> 1.18f
                    "⇧","⌫","↵" -> 1.22f
                    else -> 1f
                }
                val keyHeight = if (rowIndex == rows.lastIndex) 43 else 47
                line.addView(makeKey(key, modifier, rowIndex, keyIndex), LinearLayout.LayoutParams(0, dp(keyHeight), weight).apply {
                    setMargins(dp(2), dp(2), dp(2), dp(2))
                })
            }
            root.addView(line)
        }
        return root
    }

    private fun makeKey(key: String, modifier: Boolean, row: Int, index: Int): View {
        val dark = modifier || key in listOf("🔫","🐰")
        val label = when (key) {
            "space" -> "한글"
            "⇧" -> if (shifted) "⬆" else "⇧"
            else -> key
        }
        val face = FrameLayout(this).apply {
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
        face.setOnClickListener { onKeyPressed(key) }
        return face
    }

    private fun shape(color: Int, radius: Int, stroke: Int, strokeWidth: Int) =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
            setStroke(dp(strokeWidth), stroke)
        }

    private fun onKeyPressed(key: String) {
        val ic = currentInputConnection ?: return
        when (key) {
            "⇧" -> { shifted = !shifted; setInputView(buildKeyboard()) }
            "⌫" -> deleteOne(ic)
            "?123" -> { finishComposition(ic); symbols = true; setInputView(buildKeyboard()) }
            "가나다" -> { finishComposition(ic); symbols = false; setInputView(buildKeyboard()) }
            "한/영" -> { finishComposition(ic); requestHideSelf(0) }
            "space" -> { finishComposition(ic); ic.commitText(" ", 1) }
            "↵" -> {
                finishComposition(ic)
                val action = currentInputEditorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION) ?: EditorInfo.IME_ACTION_NONE
                if (action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) ic.performEditorAction(action) else ic.commitText("\n", 1)
            }
            "," -> { finishComposition(ic); ic.commitText(",", 1) }
            "." -> { finishComposition(ic); ic.commitText(".", 1) }
            else -> {
                val actual = if (shifted) initialJoin[key + key] ?: key else key
                typeJamo(actual, ic)
                if (shifted) { shifted = false; setInputView(buildKeyboard()) }
            }
        }
    }

    private fun typeJamo(char: String, ic: InputConnection) {
        if (char.length != 1) { finishComposition(ic); ic.commitText(char, 1); return }
        if (char in medials) { addVowel(char, ic); return }
        if (char in initials) { addConsonant(char, ic); return }
        finishComposition(ic)
        ic.commitText(char, 1)
    }

    private fun addConsonant(char: String, ic: InputConnection) {
        val newL = initials.indexOf(char)
        val newT = finals.indexOf(char)
        if (l == null && v == null) {
            if (newL >= 0) l = newL else { ic.commitText(char, 1); return }
        } else if (l != null && v == null) {
            val doubled = initialJoin[initials[l!!] + char]
            if (doubled != null) l = initials.indexOf(doubled)
            else { finishComposition(ic); l = newL }
        } else if (l != null && v != null && t == 0) {
            if (newT > 0) t = newT else { finishComposition(ic); l = newL }
        } else if (l != null && v != null) {
            val joined = finalJoin[finals[t] + char]
            if (joined != null) t = finals.indexOf(joined)
            else { finishComposition(ic); l = newL }
        } else {
            finishComposition(ic); l = newL
        }
        showComposition(ic)
    }

    private fun addVowel(char: String, ic: InputConnection) {
        val newV = medials.indexOf(char)
        if (l == null && v == null) {
            v = newV
        } else if (l == null && v != null) {
            val joined = vowelJoin[medials[v!!] + char]
            if (joined != null) v = medials.indexOf(joined)
            else { finishComposition(ic); v = newV }
        } else if (l != null && v == null) {
            v = newV
        } else if (l != null && v != null && t == 0) {
            val joined = vowelJoin[medials[v!!] + char]
            if (joined != null) v = medials.indexOf(joined)
            else { finishComposition(ic); v = newV }
        } else if (l != null && v != null && t != 0) {
            val finalText = finals[t]
            val split = finalJoin.entries.firstOrNull { it.value == finalText }?.key
            val keep = split?.first()?.toString() ?: ""
            val move = split?.last()?.toString() ?: finalText
            val oldL = l!!
            val oldV = v!!
            val oldT = if (keep.isEmpty()) 0 else finals.indexOf(keep)
            val oldText = syllable(oldL, oldV, oldT)
            ic.commitText(oldText, 1)
            l = initials.indexOf(move).takeIf { it >= 0 }
            v = newV
            t = 0
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
        }
        val value = currentComposition()
        if (value.isEmpty()) ic.setComposingText("", 1) else ic.setComposingText(value, 1)
        if (l == null && v == null && t == 0) ic.deleteSurroundingText(1, 0)
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).roundToInt()
}
