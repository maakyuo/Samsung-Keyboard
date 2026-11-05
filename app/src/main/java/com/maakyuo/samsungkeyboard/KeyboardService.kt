package com.maakyuo.samsungkeyboard

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.roundToInt

class KeyboardService : InputMethodService() {
    private var shifted = false
    private var symbols = false
    private var composing = ""
    private val pink = Color.rgb(255, 220, 235)
    private val lavender = Color.rgb(232, 222, 255)
    private val ink = Color.rgb(75, 54, 79)
    private val vowelPairs = mapOf("ㅗㅏ" to "ㅘ", "ㅗㅐ" to "ㅙ", "ㅗㅣ" to "ㅚ", "ㅜㅓ" to "ㅝ", "ㅜㅔ" to "ㅞ", "ㅜㅣ" to "ㅟ", "ㅡㅣ" to "ㅢ")
    private val finalPairs = mapOf("ㄱㅅ" to "ㄳ", "ㄴㅈ" to "ㄵ", "ㄴㅎ" to "ㄶ", "ㄹㄱ" to "ㄺ", "ㄹㅁ" to "ㄻ", "ㄹㅂ" to "ㄼ", "ㄹㅅ" to "ㄽ", "ㄹㅌ" to "ㄾ", "ㄹㅍ" to "ㄿ", "ㄹㅎ" to "ㅀ", "ㅂㅅ" to "ㅄ")
    private val initialPairs = mapOf("ㄱㄱ" to "ㄲ", "ㄷㄷ" to "ㄸ", "ㅂㅂ" to "ㅃ", "ㅅㅅ" to "ㅆ", "ㅈㅈ" to "ㅉ")
    private val initialIndex = "ㄱㄲㄴㄷㄸㄹㅁㅂㅃㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎ"
    private val medialIndex = listOf("ㅏ","ㅐ","ㅑ","ㅒ","ㅓ","ㅔ","ㅕ","ㅖ","ㅗ","ㅘ","ㅙ","ㅚ","ㅛ","ㅜ","ㅝ","ㅞ","ㅟ","ㅠ","ㅡ","ㅢ","ㅣ")
    private val finalIndex = listOf("", "ㄱ","ㄲ","ㄳ","ㄴ","ㄵ","ㄶ","ㄷ","ㄹ","ㄺ","ㄻ","ㄼ","ㄽ","ㄾ","ㄿ","ㅀ","ㅁ","ㅂ","ㅄ","ㅅ","ㅆ","ㅇ","ㅈ","ㅊ","ㅋ","ㅌ","ㅍ","ㅎ")
    private val baseRows = listOf(
        listOf("ㅂ","ㅈ","ㄷ","ㄱ","ㅅ","ㅛ","ㅕ","ㅑ","ㅐ","ㅔ"),
        listOf("ㅁ","ㄴ","ㅇ","ㄹ","ㅎ","ㅗ","ㅓ","ㅏ","ㅣ"),
        listOf("⇧","ㅋ","ㅌ","ㅊ","ㅍ","ㅠ","ㅜ","ㅡ","⌫"),
        listOf("?123",",","한/영","space",".","↵")
    )

    override fun onCreateInputView(): View = buildKeyboard()

    private fun buildKeyboard(): View {
        val outer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(5), dp(7), dp(5), dp(7))
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(Color.rgb(255,239,247), Color.rgb(239,232,255))).apply { cornerRadius = dp(17).toFloat() }
        }
        val top = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(dp(4),0,dp(4),dp(4)) }
        top.addView(label("하나코 나나", 13f, ink, false).apply { gravity = Gravity.CENTER_VERTICAL })
        val spacer = View(this)
        top.addView(spacer, LinearLayout.LayoutParams(0, dp(28), 1f))
        top.addView(label("♡", 20f, Color.rgb(222,117,167), true))
        outer.addView(top)
        val rows = if (symbols) symbolRows() else baseRows
        rows.forEachIndexed { index, row ->
            val line = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER; setPadding(0, dp(2), 0, dp(2)) }
            row.forEach { key ->
                val special = key in listOf("⇧","⌫","?123","↵","한/영","space")
                val width = when (key) { "space" -> 4.0f; "한/영" -> 1.45f; "?123" -> 1.25f; "⇧","⌫","↵" -> 1.15f; else -> 1f }
                val view = keyView(key, special, index)
                line.addView(view, LinearLayout.LayoutParams(0, dp(if (index == 3) 43 else 45), width).apply { setMargins(dp(2), dp(2), dp(2), dp(2)) })
            }
            outer.addView(line)
        }
        return outer
    }

    private fun symbolRows() = listOf(
        listOf("1","2","3","4","5","6","7","8","9","0"),
        listOf("@","#","$","%","&","-","+","(",")","/"),
        listOf("⇧","*","\"","' ",":",";","!","?","⌫"),
        listOf("가나다",",","한/영","space",".","↵")
    )

    private fun label(text: String, size: Float, color: Int, bold: Boolean) = TextView(this).apply {
        this.text = text; textSize = size; setTextColor(color); gravity = Gravity.CENTER
        if (bold) typeface = Typeface.DEFAULT_BOLD
    }

    private fun keyView(key: String, special: Boolean, row: Int): View {
        val shown = when (key) { "space" -> " "; "?123" -> "?123"; "↵" -> "↵"; else -> key }
        return TextView(this).apply {
            text = shown
            textSize = when (key) { "space" -> 10f; "?123","한/영" -> 11f; else -> 18f }
            setTextColor(if (special) Color.rgb(255,255,255) else ink)
            gravity = Gravity.CENTER
            typeface = Typeface.create(if (special) "sans-serif-medium" else "sans-serif", if (special) Typeface.BOLD else Typeface.NORMAL)
            background = GradientDrawable().apply {
                cornerRadius = dp(if (special) 11 else 9).toFloat()
                setColor(when (key) {
                    "⇧","⌫","?123","↵" -> Color.rgb(190,153,205)
                    "한/영" -> Color.rgb(222,151,185)
                    "space" -> Color.rgb(255,250,253)
                    else -> if (row % 2 == 0) Color.rgb(255,255,255) else Color.rgb(255,248,252)
                })
                setStroke(dp(1), if (special) Color.rgb(255,255,255) else Color.rgb(246,218,235))
            }
            elevation = dp(if (special) 1 else 2).toFloat()
            setOnClickListener { press(key) }
        }
    }

    private fun press(key: String) {
        val ic = currentInputConnection ?: return
        when (key) {
            "⇧" -> { shifted = !shifted; setInputView(buildKeyboard()) }
            "⌫" -> backspace(ic)
            "?123" -> { commitComposing(ic); symbols = true; setInputView(buildKeyboard()) }
            "가나다" -> { symbols = false; setInputView(buildKeyboard()) }
            "한/영" -> { commitComposing(ic); requestHideSelf(0) }
            "space" -> { commitComposing(ic); ic.commitText(" ", 1) }
            "↵" -> { commitComposing(ic); val action = currentInputEditorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION) ?: EditorInfo.IME_ACTION_NONE; if (action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) ic.performEditorAction(action) else ic.commitText("\n",1) }
            "," -> { commitComposing(ic); ic.commitText(",",1) }
            "." -> { commitComposing(ic); ic.commitText(".",1) }
            else -> typeCharacter(if (shifted) doubleKey(key) else key, ic)
        }
    }

    private fun doubleKey(k: String) = mapOf("ㅂ" to "ㅃ","ㅈ" to "ㅉ","ㄷ" to "ㄸ","ㄱ" to "ㄲ","ㅅ" to "ㅆ").getOrDefault(k,k)

    private fun typeCharacter(k: String, ic: InputConnection) {
        if (k.length != 1 || k[0] !in "ㄱㄲㄴㄷㄸㄹㅁㅂㅃㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎㅏㅐㅑㅒㅓㅔㅕㅖㅗㅛㅜㅠㅡㅣ") {
            commitComposing(ic); ic.commitText(k,1); return
        }
        val next = composeNext(composing, k)
        if (next == null) { commitComposing(ic); composing = k }
        else composing = next
        ic.setComposingText(composing, 1)
        if (shifted) { shifted = false; setInputView(buildKeyboard()) }
    }

    private fun composeNext(old: String, next: String): String? {
        if (old.isEmpty()) return null
        val last = old.last()
        if (last in "ㅏㅐㅑㅒㅓㅔㅕㅖㅗㅛㅜㅠㅡㅣ") {
            val pair = vowelPairs["$last$next"]
            if (pair != null) return old.dropLast(1) + pair
            return null
        }
        if (last in initialIndex) {
            if (next in "ㅏㅐㅑㅒㅓㅔㅕㅖㅗㅛㅜㅠㅡㅣ") {
                val li = initialIndex.indexOf(last); val vi = medialIndex.indexOf(next)
                return (0xAC00 + (li * 21 + vi) * 28).toChar().toString()
            }
            val doubled = initialPairs["$last$next"]
            return doubled
        }
        val cp = last.code
        if (cp in 0xAC00..0xD7A3) {
            val n = cp - 0xAC00; val li = n / 588; val vi = (n % 588) / 28; val ti = n % 28
            if (next in "ㅏㅐㅑㅒㅓㅔㅕㅖㅗㅛㅜㅠㅡㅣ") {
                if (ti == 0) {
                    val v = medialIndex[vi]
                    val combined = vowelPairs["$v$next"] ?: return null
                    return old.dropLast(1) + (0xAC00 + (li * 21 + medialIndex.indexOf(combined)) * 28).toChar()
                }
                val final = finalIndex[ti]
                val split = finalPairs.entries.firstOrNull { it.value == final }
                val moved = split?.key?.last()?.toString() ?: final
                val remain = split?.key?.first()?.toString() ?: ""
                val base = remain.ifEmpty { "" }
                val syllable = if (remain.isEmpty()) old.dropLast(1) else old.dropLast(1) + (0xAC00 + (li * 21 + vi) * 28 + finalIndex.indexOf(remain)).toChar()
                val newInitial = initialIndex.indexOf(moved)
                if (newInitial < 0) return null
                return syllable + (0xAC00 + newInitial * 588 + medialIndex.indexOf(next) * 28).toChar()
            }
            if (ti == 0) {
                val t = finalIndex.indexOf(next)
                if (t > 0) return old.dropLast(1) + (cp + t).toChar()
            } else {
                val combined = finalPairs["${finalIndex[ti]}$next"]
                if (combined != null) return old.dropLast(1) + (0xAC00 + li * 588 + vi * 28 + finalIndex.indexOf(combined)).toChar()
            }
        }
        return null
    }

    private fun commitComposing(ic: InputConnection) { if (composing.isNotEmpty()) { ic.commitText(composing,1); composing = "" } }
    private fun backspace(ic: InputConnection) {
        if (composing.isNotEmpty()) { composing = composing.dropLast(1); if (composing.isEmpty()) ic.setComposingText("",1) else ic.setComposingText(composing,1) }
        else ic.deleteSurroundingText(1,0)
    }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).roundToInt()
}
