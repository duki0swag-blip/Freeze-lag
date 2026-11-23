package com.carter.vpnblocker

import android.app.*
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import java.io.FileInputStream
import java.util.concurrent.atomic.AtomicBoolean

class BlockVpnService : VpnService() {
    companion object {
        const val ACTION_START = "com.carter.vpnblocker.START"
        const val ACTION_STOP = "com.carter.vpnblocker.STOP"
        const val CHANNEL_ID = "cart3r_firewall"
        @Volatile var isRunning = false
            private set
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private var worker: Thread? = null
    private val running = AtomicBoolean(false)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopFirewall()
            else -> startFirewall()
        }
        return START_NOT_STICKY
    }

    private fun startFirewall() {
        if (running.get()) return
        createNotificationChannel()
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("CART3R VPN Blocker")
            .setContentText("El bloqueo de Internet está activo")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setOngoing(true)
            .build()
        startForeground(7, notification)

        try {
            val builder = Builder()
                .setSession("CART3R Local Firewall")
                .addAddress("10.111.0.1", 32)
                .addRoute("0.0.0.0", 0)
                .addDnsServer("10.111.0.2")
            // IPv6 route prevents IPv6 traffic from bypassing the local firewall.
            builder.addAddress("fd00:1:fd00:1::1", 128)
            builder.addRoute("::", 0)

            vpnInterface = builder.establish()
            if (vpnInterface == null) {
                stopSelf()
                return
            }
            running.set(true)
            isRunning = true
            val fd = vpnInterface!!.fileDescriptor
            worker = Thread {
                // Drain and discard packets. No packet is forwarded to the Internet.
                // A dedicated thread avoids filling the TUN buffer and stalling the device.
                try {
                    FileInputStream(fd).use { input ->
                        val buffer = ByteArray(32767)
                        while (running.get()) {
                            val count = input.read(buffer)
                            if (count < 0) break
                        }
                    }
                } catch (_: Exception) {
                    // Closing the descriptor during stop is expected.
                }
            }.apply { name = "CART3R-TUN-Dropper"; isDaemon = true; start() }
            sendBroadcast(Intent(FloatingBubbleService.ACTION_UPDATE).setPackage(packageName))
        } catch (_: Exception) {
            stopFirewall()
        }
    }

    private fun stopFirewall() {
        running.set(false)
        isRunning = false
        try { vpnInterface?.close() } catch (_: Exception) {}
        vpnInterface = null
        worker?.interrupt()
        worker = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        sendBroadcast(Intent(FloatingBubbleService.ACTION_UPDATE).setPackage(packageName))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Firewall local", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        running.set(false)
        isRunning = false
        try { vpnInterface?.close() } catch (_: Exception) {}
        vpnInterface = null
        super.onDestroy()
    }
}
