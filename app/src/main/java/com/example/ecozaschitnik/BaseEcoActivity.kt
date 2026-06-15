package com.example.ecozaschitnik

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.ecozaschitnik.ui.EcoUi
import com.google.android.material.appbar.MaterialToolbar

open class BaseEcoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EcoUi.enableEdgeToEdge(this)
    }

    override fun onResume() {
        super.onResume()
        EcoUi.applySystemBarStyle(this)
    }

    protected fun bindToolbar(title: String? = null): MaterialToolbar {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
            ?: throw IllegalStateException("Toolbar (@id/toolbar) not found in layout")
        setupToolbar(toolbar, title)
        return toolbar
    }

    protected fun setupToolbar(toolbar: MaterialToolbar, title: String? = null) {
        EcoUi.setupToolbar(this, toolbar, title)
        EcoUi.applyToolbarTopInset(toolbar)
    }

    protected fun applyContentBottomInset(content: View) {
        EcoUi.applyBottomInset(content)
    }
}
