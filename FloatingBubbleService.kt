package com.carter.vpnblocker

import android.app.Service
import android.content.*
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.IBinder
import android.provider.Settings
import android.view.*
import android.widget.TextView
import android.widget.Toast
import android.net.VpnService
import android.os.Build
import android.content.pm.PackageManager
import android.view.WindowManager

class FloatingBubbleService : Service() {
    companion object { const val ACTION_UPDATE = "com.carter.vpnblocker.UPDATE_BUBBLE" }
    private lateinit var windowManager: WindowManager
    private var bubble: TextView? = null
    private var receiver: BroadcastReceiver? = null
    private var params: WindowManager.LayoutParams? = null

    override fun onCreate() {
        super.onCreate()
        if (!Settings.canDrawOverlays(this)) { stopSelf(); return }
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createBubble()
        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) { updateBubble() }
        }
        registerReceiver(receiver, IntentFilter(ACTION_UPDATE))
    }

    private fun createBubble() {
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val size = prefs.getInt("bubble_size", 56)
        val view = TextView(this).apply {
            text = if (BlockVpnService.isRunning) "OFF" else "ON"
            textSize = 13f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.rgb(190, 20, 42))
                setStroke(3, Color.rgb(255, 90, 110))
            }
            elevation = 12f
        }
        val type = if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                   else WindowManager.LayoutParams.TYPE_PHONE
        val p = WindowManager.LayoutParams(
            (size * resources.displayMetrics.density).toInt(),
            (size * resources.displayMetrics.density).toInt(),
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START; x = 24; y = 180 }
        view.setOnClickListener { toggleFirewall() }
        view.setOnTouchListener(object : View.OnTouchListener {
            private var downX = 0f; private var downY = 0f
            private var startX = 0; private var startY = 0
            private var moved = false
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        downX = event.rawX; downY = event.rawY
                        startX = p.x; startY = p.y; moved = false
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - downX).toInt()
                        val dy = (event.rawY - downY).toInt()
                        if (kotlin.math.abs(dx) > 8 || kotlin.math.abs(dy) > 8) moved = true
                        p.x = startX + dx; p.y = startY + dy
                        try { windowManager.updateViewLayout(v, p) } catch (_: Exception) {}
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!moved) v.performClick()
                        return true
                    }
                }
                return false
            }
        })
        bubble = view; params = p
        windowManager.addView(view, p)
    }

    private fun toggleFirewall() {
        if (BlockVpnService.isRunning) {
            stopService(Intent(this, BlockVpnService::class.java).setAction(BlockVpnService.ACTION_STOP))
            Toast.makeText(this, "Bloqueo desactivado", Toast.LENGTH_SHORT).show()
        } else {
            val prepare = VpnService.prepare(this)
            if (prepare != null) {
                Toast.makeText(this, "Abre CART3R y autoriza la conexión VPN", Toast.LENGTH_LONG).show()
            } else {
                startForegroundService(Intent(this, BlockVpnService::class.java).setAction(BlockVpnService.ACTION_START))
            }
        }
        bubble?.postDelayed({ updateBubble() }, 400)
    }

    private fun updateBubble() {
        bubble?.text = if (BlockVpnService.isRunning) "OFF" else "ON"
        val size = getSharedPreferences("settings", MODE_PRIVATE).getInt("bubble_size", 56)
        val px = (size * resources.displayMetrics.density).toInt()
        params?.width = px; params?.height = px
        try { if (bubble?.isAttachedToWindow == true) windowManager.updateViewLayout(bubble, params) } catch (_: Exception) {}
    }

    override fun onDestroy() {
        try { receiver?.let { unregisterReceiver(it) } } catch (_: Exception) {}
        try { bubble?.let { windowManager.removeView(it) } } catch (_: Exception) {}
        bubble = null
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null
}
