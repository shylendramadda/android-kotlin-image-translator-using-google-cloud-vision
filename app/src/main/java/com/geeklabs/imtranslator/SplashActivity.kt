package com.geeklabs.imtranslator

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat
import android.support.v7.app.AppCompatActivity
import android.view.Window
import android.view.WindowManager

class SplashActivity : AppCompatActivity() {

    private val SPLASH_TIME_OUT: Long = 2000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        hideStatusBar()

        Handler().postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, SPLASH_TIME_OUT)
    }

    private fun hideStatusBar() {
        if (Build.VERSION.SDK_INT >= 16) {
            window.setFlags(AccessibilityNodeInfoCompat.ACTION_NEXT_HTML_ELEMENT, AccessibilityNodeInfoCompat.ACTION_NEXT_HTML_ELEMENT);
            window.decorView.systemUiVisibility = 3328
        } else {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            this.window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

}