package com.fooddeal.companion

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 64, 48, 48)
        }
        root.addView(TextView(this).apply {
            text = "FoodDeal Install Test"
            textSize = 28f
        })
        root.addView(TextView(this).apply {
            text = "\nInstallation test passed.\n\nThis build intentionally has NO AccessibilityService or sensitive permissions.\n\nIf this APK installs successfully, we have confirmed that the earlier Play Protect block was caused by the companion's sensitive Accessibility capability."
            textSize = 18f
        })
        setContentView(root)
    }
}