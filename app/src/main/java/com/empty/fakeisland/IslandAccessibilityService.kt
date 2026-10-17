package com.empty.fakeisland

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import kotlin.math.roundToInt
@SuppressLint("AccessibilityPolicy")
class IslandAccessibilityService :  AccessibilityService(){
    private lateinit var windowManager: WindowManager
    private lateinit var prefs: SharedPreferences
    private var islandView: View? = null
    private var params: WindowManager.LayoutParams? = null
    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> applyConfig() }

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        prefs = IslandPrefs.of(this)
        prefs.registerOnSharedPreferenceChangeListener(prefsListener)
        addIsland()
        applyConfig()
    }

    private fun addIsland() {
        if (islandView != null) return

        val view = View(this).apply {
            background = GradientDrawable().apply {
                setColor(0xFF000000.toInt())
                cornerRadius = 1000f
            }
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }

        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            layoutInDisplayCutoutMode =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                else
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            title = "FakeIsland"
        }

        islandView = view
        params = lp
        try {
            windowManager.addView(view, lp)
        } catch (_: Exception) {
            islandView = null
            params = null
        }
    }

    private fun applyConfig() {
        val view = islandView ?: return
        val lp = params ?: return
        val cfg = IslandPrefs.read(prefs)

        lp.width = dp(cfg.widthDp)
        lp.height = dp(cfg.heightDp)
        lp.x = dp(cfg.xDp)
        lp.y = dp(cfg.yDp)

        val landscape =
            resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val visible = cfg.enabled && !(landscape && cfg.hideInLandscape)
        view.visibility = if (visible) View.VISIBLE else View.GONE

        try {
            windowManager.updateViewLayout(view, lp)
        } catch (_: Exception) {
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        applyConfig()
    }

    private fun dp(value: Float): Int =
        (value * resources.displayMetrics.density).roundToInt()

    private fun removeIsland() {
        islandView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {
            }
        }
        islandView = null
        params = null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) { /* not used */ }

    override fun onInterrupt() { /* not used */ }

    override fun onUnbind(intent: Intent?): Boolean {
        if (::prefs.isInitialized) prefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
        removeIsland()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        removeIsland()
        super.onDestroy()
    }
}