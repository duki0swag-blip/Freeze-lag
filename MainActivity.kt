package com.carter.vpnblocker

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.*
import android.graphics.Color
import android.graphics.Typeface

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var sizeLabel: TextView
    private lateinit var sizeSeek: SeekBar
    private lateinit var bubbleSwitch: Switch
    private val prefs by lazy { getSharedPreferences("settings", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        refreshStatus()
    }

    private fun buildUi() {
        val scroll = ScrollView(this)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 28, 24, 28)
            setBackgroundColor(Color.rgb(12, 10, 12))
        }
        scroll.addView(layout)
        setContentView(scroll)

        fun text(value: String, size: Float, color: Int = Color.WHITE, bold: Boolean = false): TextView =
            TextView(this).apply {
                text = value; textSize = size; setTextColor(color)
                if (bold) setTypeface(null, Typeface.BOLD)
                setPadding(0, 8, 0, 8)
            }

        layout.addView(text("CART3R", 30f, Color.rgb(255, 55, 78), true))
        layout.addView(text("VPN BLOCKER  •  FIREWALL LOCAL", 13f, Color.LTGRAY, true))
        status = text("Estado: comprobando…", 18f, Color.WHITE, true)
        layout.addView(status)

        val toggle = Button(this).apply {
            text = "ACTIVAR / DESACTIVAR BLOQUEO"
            setOnClickListener { toggleBlocking() }
        }
        layout.addView(toggle, matchWidth())

        layout.addView(text("BOTÓN FLOTANTE", 18f, Color.rgb(255, 90, 110), true))
        bubbleSwitch = Switch(this).apply {
            text = "Mostrar botón flotante sobre otras apps"
            isChecked = prefs.getBoolean("bubble_enabled", false)
            setTextColor(Color.WHITE)
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean("bubble_enabled", checked).apply()
                if (checked) enableBubble() else stopService(Intent(this@MainActivity, FloatingBubbleService::class.java))
            }
        }
        layout.addView(bubbleSwitch)

        layout.addView(text("Tamaño del botón flotante", 16f, Color.WHITE, true))
        sizeLabel = text("", 14f, Color.LTGRAY)
        layout.addView(sizeLabel)
        sizeSeek = SeekBar(this).apply {
            max = 80
            progress = prefs.getInt("bubble_size", 56) - 40
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val px = progress + 40
                    sizeLabel.text = "$px dp"
                    prefs.edit().putInt("bubble_size", px).apply()
                    sendBroadcast(Intent(FloatingBubbleService.ACTION_UPDATE).setPackage(packageName))
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
        layout.addView(sizeSeek, matchWidth())
        sizeLabel.text = "${sizeSeek.progress + 40} dp"

        layout.addView(text("OPCIONES", 18f, Color.rgb(255, 90, 110), true))
        val permission = Button(this).apply {
            text = "Permiso para mostrar sobre otras apps"
            setOnClickListener {
                if (!Settings.canDrawOverlays(this@MainActivity)) {
                    startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")))
                } else Toast.makeText(this@MainActivity, "Permiso ya concedido", Toast.LENGTH_SHORT).show()
            }
        }
        layout.addView(permission, matchWidth())

        val showHelp = Button(this).apply {
            text = "¿Cómo funciona?"
            setOnClickListener {
                android.app.AlertDialog.Builder(this@MainActivity)
                    .setTitle("Firewall VPN local")
                    .setMessage("Esta app crea una VPN local en el teléfono y descarta los paquetes de red. No conecta con un servidor VPN, no oculta tu IP y no cifra el tráfico hacia un servidor remoto. Mientras esté activo, las apps no tendrán acceso a Internet. Desactívalo desde la app o el botón flotante.")
                    .setPositiveButton("Entendido", null).show()
            }
        }
        layout.addView(showHelp, matchWidth())
        layout.addView(text("Consejo: Android solo permite una VPN activa a la vez. Si usas otra VPN, tendrás que desconectarla primero.", 13f, Color.LTGRAY))
    }

    private fun matchWidth() = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
    ).apply { topMargin = 10 }

    private fun toggleBlocking() {
        if (BlockVpnService.isRunning) {
            stopService(Intent(this, BlockVpnService::class.java))
            refreshStatus()
            return
        }
        val prepare = VpnService.prepare(this)
        if (prepare != null) startActivityForResult(prepare, 1001)
        else startBlockService()
    }

    @Deprecated("Deprecated in Android API but retained for broad device compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001 && resultCode == RESULT_OK) startBlockService()
    }

    private fun startBlockService() {
        val intent = Intent(this, BlockVpnService::class.java).setAction(BlockVpnService.ACTION_START)
        startForegroundService(intent)
        status.postDelayed({ refreshStatus() }, 500)
    }

    private fun enableBubble() {
        if (!Settings.canDrawOverlays(this)) {
            bubbleSwitch.isChecked = false
            Toast.makeText(this, "Concede primero el permiso de superposición", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            return
        }
        startService(Intent(this, FloatingBubbleService::class.java))
    }

    private fun refreshStatus() {
        status.text = if (BlockVpnService.isRunning) "● BLOQUEO ACTIVADO" else "○ INTERNET PERMITIDO"
        status.setTextColor(if (BlockVpnService.isRunning) Color.rgb(255, 75, 90) else Color.rgb(80, 210, 140))
    }

    override fun onResume() {
        super.onResume()
        if (::bubbleSwitch.isInitialized) {
            val enabled = prefs.getBoolean("bubble_enabled", false)
            bubbleSwitch.isChecked = enabled
            if (enabled && Settings.canDrawOverlays(this)) startService(Intent(this, FloatingBubbleService::class.java))
            refreshStatus()
        }
    }
}
