package com.maakyuo.samsungkeyboard

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class SettingsActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(36, 64, 36, 36)
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(Color.rgb(255,239,247), Color.rgb(239,232,255)))
        }
        root.addView(TextView(this).apply {
            text = "하나코 나나 키보드"
            textSize = 25f
            setTextColor(Color.rgb(91,61,91))
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        })
        root.addView(TextView(this).apply {
            text = "나만의 귀여운 자판으로 입력해 보세요."
            textSize = 14f
            setTextColor(Color.rgb(125,96,126))
            gravity = Gravity.CENTER
            setPadding(0, 12, 0, 36)
        })
        root.addView(actionButton("키보드 사용 설정") { startActivity(Intent("android.settings.INPUT_METHOD_SETTINGS")) })
        root.addView(actionButton("키보드 선택") { (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker() })
        setContentView(root)
    }

    private fun actionButton(caption: String, action: () -> Unit) = Button(this).apply {
        text = caption
        isAllCaps = false
        textSize = 16f
        setTextColor(Color.WHITE)
        background = GradientDrawable().apply {
            cornerRadius = 28f
            setColor(Color.rgb(210,153,190))
        }
        setPadding(28, 14, 28, 14)
        setOnClickListener { action() }
        val params = LinearLayout.LayoutParams(-1, 58).apply { bottomMargin = 16 }
        layoutParams = params
    }
}
