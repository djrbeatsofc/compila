package com.example.autorespondershopee

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var statusView: TextView
    private lateinit var messageEditText: EditText

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
            text = "Configure a mensagem, permita sobreposição e ative o serviço de acessibilidade. Depois use o botão flutuante Play sobre a Shopee para iniciar."
            textSize = 16f
            setTextColor(Color.rgb(45, 45, 45))
        }

        messageEditText = EditText(this).apply {
            minLines = 4
            gravity = Gravity.TOP
            textSize = 16f
            setText(loadReplyMessage())
            hint = "Mensagem para responder avaliações"
        }

        val saveMessageButton = Button(this).apply {
            text = "Salvar mensagem"
            setOnClickListener {
                saveReplyMessage(messageEditText.text?.toString().orEmpty())
                Toast.makeText(
                    this@MainActivity,
                    "Mensagem salva.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        val overlayButton = Button(this).apply {
            text = "Permitir botão flutuante"
            setOnClickListener {
                openOverlayPermissionSettings()
            }
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
            addView(messageEditText, matchWrap(top = 24))
            addView(saveMessageButton, matchWrap(top = 12))
            addView(overlayButton, matchWrap(top = 12))
            addView(openSettingsButton, matchWrap(top = 24))
            addView(statusView, matchWrap(top = 24))
        }

        setContentView(ScrollView(this).apply { addView(container) })
    }

    override fun onResume() {
        super.onResume()
        statusView.text = if (isAccessibilityServiceEnabled()) {
            "Status: serviço ativo. Sobreposição: ${overlayStatusText()}."
        } else {
            "Status: serviço ainda não ativado. Sobreposição: ${overlayStatusText()}."
        }
    }

    private fun loadReplyMessage(): String {
        return getSharedPreferences(AppConfig.PREFS_NAME, MODE_PRIVATE)
            .getString(AppConfig.KEY_REPLY_MESSAGE, AppConfig.DEFAULT_REPLY_MESSAGE)
            .orEmpty()
    }

    private fun saveReplyMessage(message: String) {
        val sanitizedMessage = message.ifBlank { AppConfig.DEFAULT_REPLY_MESSAGE }
        getSharedPreferences(AppConfig.PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putString(AppConfig.KEY_REPLY_MESSAGE, sanitizedMessage)
            .apply()
    }

    private fun openOverlayPermissionSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
    }

    private fun overlayStatusText(): String {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)) {
            "permitida"
        } else {
            "pendente"
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
