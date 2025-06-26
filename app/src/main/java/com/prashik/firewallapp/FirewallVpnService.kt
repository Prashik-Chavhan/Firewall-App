package com.prashik.firewallapp

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import java.util.concurrent.atomic.AtomicBoolean

class FirewallVpnService : VpnService() {

    companion object {
        private var vpnInterface: ParcelFileDescriptor? = null
        var isRunning = AtomicBoolean(false)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val action = intent?.action

        if (action == "STOP_VPN_SERVICE") {
            vpnInterface?.close()
            vpnInterface = null
            isRunning.set(false)
            onDestroy()
            stopSelf()
            return START_NOT_STICKY
        }
        if (!isRunning.get()) {
            isRunning.set(true)

            val builder = Builder()
                .setSession(getString(R.string.app_name))
                .addAddress("10.0.0.2", 32) // fake local vpn
                .addDnsServer("8.8.8.8")
                .addRoute("0.0.0.0", 0)

            vpnInterface = builder.establish()
        }

        return START_STICKY
    }
}