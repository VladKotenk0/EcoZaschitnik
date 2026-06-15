package com.example.ecozaschitnik.ui

import android.graphics.Color
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.example.ecozaschitnik.R
import com.google.android.material.appbar.MaterialToolbar

object EcoUi {

    fun enableEdgeToEdge(activity: AppCompatActivity) {
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)
        applySystemBarStyle(activity)
    }

    fun applySystemBarStyle(activity: AppCompatActivity) {
        val darkBackground = isColorDark(activity.getColor(R.color.screen_bg))
        val useLightSystemIcons = ThemeManager.isNight(activity) || darkBackground
        val window = activity.window
        window.statusBarColor = activity.getColor(R.color.app_status_bar)
        window.navigationBarColor = activity.getColor(R.color.app_nav_bar)
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightStatusBars = !useLightSystemIcons
        controller.isAppearanceLightNavigationBars = !useLightSystemIcons
    }

    private fun isColorDark(color: Int): Boolean {
        val luminance = 0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)
        return luminance < 140
    }

    fun setupToolbar(activity: AppCompatActivity, toolbar: MaterialToolbar, title: String? = null) {
        activity.setSupportActionBar(toolbar)
        activity.supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            this.title = title
        }
        toolbar.titleMarginTop = 0
        toolbar.titleMarginBottom = 0
        toolbar.setNavigationOnClickListener {
            activity.onBackPressedDispatcher.onBackPressed()
        }
        toolbar.inflateMenu(R.menu.menu_toolbar)
        ThemeManager.updateToolbarThemeIcon(toolbar, activity)
        toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_theme) {
                ThemeManager.toggle(activity)
                true
            } else {
                false
            }
        }
    }

    fun applySystemBarInsets(root: View) {
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, windowInsets ->
            val bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            WindowInsetsCompat.CONSUMED
        }
        ViewCompat.requestApplyInsets(root)
    }

    fun applyToolbarTopInset(toolbar: MaterialToolbar) {
        val toolbarHost = toolbar.parent as? View ?: return
        val spacer = toolbarHost.findViewById<View>(R.id.toolbarStatusBarSpacer) ?: return

        ViewCompat.setOnApplyWindowInsetsListener(toolbarHost) { _, windowInsets ->
            val topInset = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            spacer.layoutParams = spacer.layoutParams.apply {
                height = topInset
            }
            windowInsets
        }
        ViewCompat.requestApplyInsets(toolbarHost)
    }

    fun applyBottomInset(content: View) {
        ViewCompat.setOnApplyWindowInsetsListener(content) { view, windowInsets ->
            val bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                view.paddingLeft,
                view.paddingTop,
                view.paddingRight,
                bars.bottom,
            )
            windowInsets
        }
        ViewCompat.requestApplyInsets(content)
    }
}
