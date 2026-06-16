package com.example.autorespondershopee

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var statusView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        statusView = TextView(this).apply {
            textSize = 16f
            setTextColor(Color.rgb(80, 80, 80))
        }

        val titleView = TextView(this).apply {
            text = "AutoResponderShopee"
            textSize = 24f
            setTextColor(Color.rgb(238, 77, 45))
            gravity = Gravity.CENTER_HORIZONTAL
        }

        val descriptionView = TextView(this).apply {
            text = "Ative o serviço de acessibilidade para automatizar a resposta das avaliações na Shopee. O app procura por \"Responder\", preenche \"Sua Resposta...\" e toca em \"ENVIAR\" usando a árvore de acessibilidade."
            textSize = 16f
            setTextColor(Color.rgb(45, 45, 45))
        }

        val openSettingsButton = Button(this).apply {
            text = "Abrir acessibilidade"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(32), dp(24), dp(24))
            addView(titleView, matchWrap())
            addView(descriptionView, matchWrap(top = 24))
            addView(openSettingsButton, matchWrap(top = 24))
            addView(statusView, matchWrap(top = 24))
        }

        setContentView(container)
    }

    override fun onResume() {
        super.onResume()
        statusView.text = if (isAccessibilityServiceEnabled()) {
            "Status: serviço ativo."
        } else {
            "Status: serviço ainda não ativado."
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expectedService = ComponentName(
            this,
            ShopeeAccessibilityService::class.java
        ).flattenToString()

        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        return enabledServices
            .split(':')
            .any { it.equals(expectedService, ignoreCase = true) }
    }

    private fun matchWrap(top: Int = 0): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = dp(top)
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
