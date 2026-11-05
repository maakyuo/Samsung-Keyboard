package com.maakyuo.samsungkeyboard

import android.inputmethodservice.InputMethodService
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.view.View
import android.view.inputmethod.InputConnection
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class KeyboardService : InputMethodService() {
    private var pending = ""
    private val initials = "ㄱㄲㄴㄷㄸㄹㅁㅂㅃㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎ"
    private val medials = "ㅏㅐㅑㅒㅓㅔㅕㅖㅗㅛㅜㅠㅡㅣ"
    private val finals = " ㄱㄲㄳㄴㄵㄶㄷㄹㄺㄻㄼㄽㄾㄿㅀㅁㅂㅄㅅㅆㅇㅈㅊㅋㅌㅍㅎ"
    private val initialKeys = mapOf("ㄱ" to 0,"ㄲ" to 1,"ㄴ" to 2,"ㄷ" to 3,"ㄸ" to 4,"ㄹ" to 5,"ㅁ" to 6,"ㅂ" to 7,"ㅃ" to 8,"ㅅ" to 9,"ㅆ" to 10,"ㅇ" to 11,"ㅈ" to 12,"ㅉ" to 13,"ㅊ" to 14,"ㅋ" to 15,"ㅌ" to 16,"ㅍ" to 17,"ㅎ" to 18)
    private val medialKeys = mapOf("ㅏ" to 0,"ㅐ" to 1,"ㅑ" to 2,"ㅒ" to 3,"ㅓ" to 4,"ㅔ" to 5,"ㅕ" to 6,"ㅖ" to 7,"ㅗ" to 8,"ㅛ" to 9,"ㅜ" to 10,"ㅠ" to 11,"ㅡ" to 12,"ㅣ" to 13)
    override fun onCreateInputView(): View {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(3,3,3,3) }
        val uri = getSharedPreferences("keyboard", MODE_PRIVATE).getString("background", null)
        if (uri != null) try { val b = contentResolver.openInputStream(Uri.parse(uri))?.use { BitmapFactory.decodeStream(it) }; if (b != null) root.background = BitmapDrawable(resources,b) } catch (_: Exception) {}
        val rows = listOf(listOf("ㅂ","ㅈ","ㄷ","ㄱ","ㅅ","ㅛ","ㅕ","ㅑ","ㅐ","ㅔ"), listOf("ㅁ","ㄴ","ㅇ","ㄹ","ㅎ","ㅗ","ㅓ","ㅏ","ㅣ"), listOf("ㅋ","ㅌ","ㅊ","ㅍ","ㅠ","ㅜ","ㅡ","⌫"), listOf("123","한/영"," , ","띄어쓰기","⏎"))
        rows.forEach { row ->
            val line = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            row.forEach { key ->
                val btn = Button(this).apply { text = key; isAllCaps = false; textSize = 16f; setTextColor(Color.BLACK); setBackgroundColor(0xCCFFFFFF.toInt()); setPadding(1,1,1,1) }
                btn.layoutParams = LinearLayout.LayoutParams(0, resources.displayMetrics.density.let { (48*it).toInt() }, 1f).apply { setMargins(2,2,2,2) }
                btn.setOnClickListener { press(key) }
                line.addView(btn)
            }
            root.addView(line)
        }
        return root
    }
    private fun press(k: String) {
        val ic = currentInputConnection ?: return
        when(k) {
            "⌫" -> { if (pending.isNotEmpty()) { pending = ""; ic.setComposingText("",1) } else ic.deleteSurroundingText(1,0) }
            "띄어쓰기" -> { commitPending(ic); ic.commitText(" ",1) }
            "⏎" -> { commitPending(ic); ic.commitText("\n",1) }
            "한/영" -> { commitPending(ic); ic.commitText("",1) }
            "123" -> { commitPending(ic); ic.commitText("123",1) }
            " , " -> { commitPending(ic); ic.commitText(",",1) }
            else -> typeJamo(k,ic)
        }
    }
    private fun typeJamo(k: String, ic: InputConnection) {
        val composed = compose(pending, k)
        if (composed != null) { pending = composed; ic.setComposingText(pending,1) }
        else { commitPending(ic); pending = k; ic.setComposingText(pending,1) }
    }
    private fun commitPending(ic: InputConnection) { if (pending.isNotEmpty()) { ic.commitText(pending,1); pending = "" } }
    private fun compose(old: String, next: String): String? {
        if (old.isEmpty()) return null
        val a = old.last()
        if (a in initials && next in medials) {
            val li = initials.indexOf(a); val vi = medials.indexOf(next)
            return (0xAC00 + (li*21+vi)*28).toChar().toString()
        }
        val cp = a.code
        if (cp in 0xAC00..0xD7A3 && next in finals.drop(1)) {
            val t = finals.indexOf(next)
            val syllable = cp - 0xAC00
            if (syllable % 28 == 0 && t > 0) return (cp+t).toChar().toString()
        }
        return null
    }
}
