package com.numberone.numberone

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class CrashActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val err = intent.getStringExtra("err") ?: "Unknown error"
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(32, 64, 32, 32) }
        val btn = Button(this).apply {
            text = "نسخ نص الخطأ"
            setOnClickListener {
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("Crash", err))
            }
        }
        val tv = TextView(this).apply { text = err; textSize = 12f; setTextIsSelectable(true) }
        val sv = ScrollView(this).apply { addView(tv) }
        root.addView(btn)
        root.addView(sv)
        setContentView(root)
    }
}
