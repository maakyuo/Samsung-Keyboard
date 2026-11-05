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
            setPadding(30, 58, 30, 30)
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(Color.rgb(34,32,38), Color.rgb(66,42,54)))
        }
        root.addView(TextView(this).apply {
            text = "하나코 나나 키보드"
            textSize = 25f
            setTextColor(Color.rgb(255,153,185))
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        })
        root.addView(TextView(this).apply {
            text = "TOP SECRET  /  LEVEL 7\nAGENT NANA"
            textSize = 12f
            setTextColor(Color.rgb(255,211,222))
            gravity = Gravity.CENTER
            letterSpacing = .12f
            setPadding(0, 12, 0, 38)
        })
        root.addView(actionButton("키보드 사용 설정") { startActivity(Intent("android.settings.INPUT_METHOD_SETTINGS")) })
        root.addView(actionButton("키보드 선택") { (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker() })
        setContentView(root)
    }

    private fun actionButton(caption: String, action: () -> Unit) = Button(this).apply {
        text = caption
        isAllCaps = false
        textSize = 16f
        setTextColor(Color.rgb(255,239,244))
        background = GradientDrawable().apply {
            cornerRadius = 20f
            setColor(Color.rgb(173,69,111))
            setStroke(1, Color.rgb(255,157,189))
        }
        setPadding(28, 14, 28, 14)
        setOnClickListener { action() }
        layoutParams = LinearLayout.LayoutParams(-1, 58).apply { bottomMargin = 16 }
    }
}
