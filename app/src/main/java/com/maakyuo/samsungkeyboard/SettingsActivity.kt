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
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView

class SettingsActivity : Activity() {
    private val prefs by lazy { getSharedPreferences(KeyboardSound.PREFS, Context.MODE_PRIVATE) }
    private val names = listOf(
        "삼성 레트로풍", "묵직한 기계식", "짧은 기계식", "맑은 딸깍", "낮은 톡",
        "부드러운 타건", "선명한 클릭", "깊은 타건", "가벼운 스위치", "단단한 스위치",
        "빈티지 타자기", "금속 키캡", "나무 키캡", "둥근 클릭", "빠른 클릭",
        "낮고 짧은 소리", "높고 짧은 소리", "두 번 울림", "절제된 클릭", "경쾌한 클릭",
        "부드러운 톡톡", "전자식 클릭", "고전 휴대기기", "아케이드 버튼", "정숙한 타건",
        "또렷한 타건", "두꺼운 타건", "탄력 있는 클릭", "미세한 스냅", "강한 레트로 클릭"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        KeyboardSound.initialize(this)
        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(34), dp(24), dp(36))
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
            setPadding(0, 12, 0, 25)
        })
        root.addView(actionButton("키보드 사용 설정") { startActivity(Intent("android.settings.INPUT_METHOD_SETTINGS")) })
        root.addView(actionButton("키보드 선택") { (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker() })
        root.addView(TextView(this).apply {
            text = "키보드 터치음"
            textSize = 20f
            setTextColor(Color.rgb(255,153,185))
            typeface = Typeface.DEFAULT_BOLD
            setPadding(2, 18, 2, 8)
        })
        root.addView(Switch(this).apply {
            text = "터치음 사용"
            textSize = 24f
            setTextColor(Color.rgb(255,239,244))
            isChecked = prefs.getBoolean(KeyboardSound.KEY_ENABLED, true)
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean(KeyboardSound.KEY_ENABLED, checked).apply()
            }
        }, LinearLayout.LayoutParams(-1, 162))
        root.addView(TextView(this).apply {
            text = "소리를 눌러 미리 듣고 원하는 소리를 선택해."
            textSize = 13f
            setTextColor(Color.rgb(255,211,222))
            setPadding(2, 4, 2, 14)
        })
        val selectedLabel = TextView(this).apply {
            textSize = 14f
            setTextColor(Color.rgb(255,190,210))
            setPadding(2, 4, 2, 12)
        }
        fun refreshSelected() {
            val index = prefs.getInt(KeyboardSound.KEY_INDEX, 0).coerceIn(0, names.lastIndex)
            selectedLabel.text = "현재 선택: ${index + 1}. ${names[index]}"
        }
        refreshSelected()
        root.addView(selectedLabel)
        names.forEachIndexed { index, name ->
            root.addView(actionButton("%02d  ·  %s  ▶".format(index + 1, name)) {
                prefs.edit().putInt(KeyboardSound.KEY_INDEX, index).apply()
                refreshSelected()
                KeyboardSound.preview(this, index)
            })
        }
        scroll.addView(root)
        setContentView(scroll)
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private fun actionButton(caption: String, action: () -> Unit) = Button(this).apply {
        text = caption
        isAllCaps = false
        textSize = 20f
        setTextColor(Color.rgb(255,239,244))
        background = GradientDrawable().apply {
            cornerRadius = 16f
            setColor(Color.rgb(112,55,79))
            setStroke(1, Color.rgb(235,112,151))
        }
        setPadding(36, 24, 36, 24)
        setOnClickListener { action() }
        layoutParams = LinearLayout.LayoutParams(-1, 156).apply { bottomMargin = 18 }
    }
}