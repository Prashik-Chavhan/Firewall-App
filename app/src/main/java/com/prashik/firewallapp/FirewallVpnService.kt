package com.prashik.firewallapp

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.Process
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.mutableStateListOf
import androidx.core.app.NotificationCompat
import com.prashik.firewallapp.model.data.TrafficLogResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.coroutineContext

class FirewallVpnService : VpnService() {

    companion object {
        private var vpnInterface: ParcelFileDescriptor? = null
        var isRunning = AtomicBoolean(false)
        val allTrafficLogs = mutableStateListOf<TrafficLogResponse>()
        val filteredTrafficLogs = mutableStateListOf<TrafficLogResponse>()
        private var logReadingJob: Job? = null
        const val CHANNEL_ID = "VPN_SERVICE_CHANNEL"
        const val NOTIFICATION_ID = 1
    }

    @SuppressLint("ForegroundServiceType")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val action = intent?.action

        if (action == "STOP_VPN_SERVICE") {
            vpnInterface?.close()
            vpnInterface = null
            isRunning.set(false)
            allTrafficLogs.clear()
            filteredTrafficLogs.clear()
            onDestroy()
            stopSelf()
            logReadingJob?.cancel()
            return START_NOT_STICKY
        }
        val notification = buildForegroundNotification()
        startForeground(NOTIFICATION_ID, notification)
        if (!isRunning.get()) {
            isRunning.set(true)

            val builder = Builder()
                .setSession(getString(R.string.app_name))
                .addAddress("10.0.0.2", 32) // fake local vpn
                .addAddress("fd00:1:fd00:1:fd00:1:fd00:1", 128)
                .addRoute("0.0.0.0", 0)
                .addRoute("::", 0)
                .addDnsServer("8.8.8.8")
                .addDnsServer("2001:4860:4860::8888") // Google DNS IPv6

            vpnInterface = builder.establish()

            logReadingJob = CoroutineScope(Dispatchers.IO).launch {
                readPackets()
            }
        }

        return START_STICKY
    }

    private suspend fun readPackets() {
        val inputStream = FileInputStream(vpnInterface?.fileDescriptor)
        val buffer = ByteArray(32767)

        while (coroutineContext.isActive) {
            try {
                val length = inputStream.read(buffer)
                if (length > 0) {
                    val packet = buffer.copyOf(length)
                    val trafficLog = parsePacket(packet)

                    if (trafficLog == null) continue

                    if (
                        !trafficLog.appName.contains("Unknown", ignoreCase = true)
                    ) {
                        if (filteredTrafficLogs.size >= 50) {
                            filteredTrafficLogs.removeFirstOrNull()
                        }
                        filteredTrafficLogs.add(trafficLog)
                    }

                    if (!allTrafficLogs.contains(trafficLog)) {
                        if (allTrafficLogs.size >= 50) {
                            allTrafficLogs.removeFirstOrNull()
                        }
                        allTrafficLogs.add(trafficLog)
                    }
                }
            } catch (_: Exception) {
                break
            }
        }
    }

    private fun parsePacket(packet: ByteArray): TrafficLogResponse? {
        if (packet.isEmpty()) return null

        val version = (packet[0].toInt() shr 4) and 0x0F
        val protocolByte: Int
        val sourceIp: String
        val destIp: String
        val sourcePort: Int
        val destPort: Int
        val protocol: String

        when (version) {
            4 -> {
                if (packet.size < 20) {
                    return null
                }
                protocolByte = packet[9].toInt() and 0xFF
                protocol = when (protocolByte) {
                    6 -> "TCP"
                    17 -> "UDP"
                    else -> "Unknown"
                }

                val srcBytes = packet.sliceArray(12..15)
                val destBytes = packet.sliceArray(16..19)

                sourceIp = InetAddress.getByAddress(srcBytes).hostAddress ?: "Unknown"
                destIp = InetAddress.getByAddress(destBytes).hostAddress ?: "Unknown"

                sourcePort = ((packet[20].toInt() and 0xFF) shl 8) or
                        (packet[21].toInt() and 0xFF)

                destPort = ((packet[22].toInt() and 0xFF) shl 8) or
                        (packet[23].toInt() and 0xFF)
            }

            6 -> {
                if (packet.size < 40) {
                    return null
                }

                protocolByte = packet[6].toInt() and 0xFF
                protocol = when (protocolByte) {
                    6 -> "TCP"
                    17 -> "UDP"
                    else -> "Unknown"
                }

                val srcBytes = packet.sliceArray(8..23)
                val dstBytes = packet.sliceArray(24..39)

                sourceIp = InetAddress.getByAddress(srcBytes).hostAddress ?: "Unknown"
                destIp = InetAddress.getByAddress(dstBytes).hostAddress ?: "Unknown"

                sourcePort = ((packet[40].toInt() and 0xFF) shl 8) or
                        (packet[41].toInt() and 0xFF)

                destPort = ((packet[42].toInt() and 0xFF) shl 8) or
                        (packet[43].toInt() and 0xFF)
            }

            else -> return null
        }

        val timeStamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        val uid = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getUidFromIpPort(sourceIp, sourcePort, destIp, destPort, protocolByte)
        } else {
            val isIPv6 = version == 6
            val localHex = if (isIPv6) {
                ipPortToLocalHexIPv6(sourceIp, sourcePort)
            } else {
                ipPortToLocalHex(sourceIp, sourcePort)
            }
            findUidForConnection(localHex, protocol, isIPv6)
        }

        val appName = uid?.let { getAppNameFromUid(this, it) ?: "Unknown App (UID: $uid)" }

        return TrafficLogResponse(
            appName = appName ?: "Unknown App",
            protocol = protocol,
            srcIp = sourceIp,
            dstIp = destIp,
            srcPort = sourcePort,
            dstPort = destPort,
            timeStamp = timeStamp,
        )
    }

    private fun ipPortToLocalHexIPv6(ip: String, port: Int): String {
        val byteArray = InetAddress.getByName(ip).address
        val hexIp = byteArray.joinToString("") {
            "%02x".format(it)
        }.padEnd(32, '0')

        val hexPort = port.toString(16).padStart(4, '0').uppercase()
        Log.e("Firewall App", hexPort)
        return "$hexIp:$hexPort"
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun getUidFromIpPort(
        srcIp: String,
        srcPort: Int,
        destIp: String,
        destPort: Int,
        protocol: Int
    ): Int? {
        val connectivityManager =
            getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager

        if (protocol != 6 && protocol != 17) return null
        val uid = connectivityManager.getConnectionOwnerUid(
            protocol,
            InetSocketAddress(srcIp, srcPort),
            InetSocketAddress(destIp, destPort)
        )

        return if (uid != Process.INVALID_UID) uid else -1
    }

    private fun ipPortToLocalHex(
        sourceIp: String,
        sourcePort: Int
    ): String {
        val ipHex = sourceIp.split(".")
            .map { it.toInt().toString(16).padStart(2, '0') }
            .reversed()
            .joinToString("")

        val portHex = Integer.toHexString(sourcePort).padStart(4, '0').uppercase()

        Log.e("Firewall App", portHex)
        return "$ipHex:$portHex"
    }

    private fun findUidForConnection(localHex: String, protocol: String, isIPv6: Boolean): Int? {

        val filePath = when {
            protocol.equals("TCP", ignoreCase = true) && isIPv6 -> "/proc/net/tcp6"
            protocol.equals("TCP", ignoreCase = true) && !isIPv6 -> "/proc/net/tcp"
            protocol.equals("UDP", ignoreCase = true) && isIPv6 -> "/proc/net/udp6"
            protocol.equals("UDP", ignoreCase = true) && !isIPv6 -> "/proc/net/udp"
            else -> return null // Unsupported protocol
        }
        File(filePath).useLines { lines ->
            lines.forEach { line ->
                if (line.contains(localHex)) {
                    val columns = line.trim().split(Regex("\\s+"))
                    Log.d("Firewall Vpn", "${columns.getOrNull(7)?.toIntOrNull()}")
                    return columns.getOrNull(7)?.toIntOrNull()
                }
            }
        }
        return null
    }

    private fun getAppNameFromUid(context: Context, uid: Int): String? {
        return try {
            val pm = context.packageManager
            val packageNames = pm.getPackagesForUid(uid)

            if (packageNames != null && packageNames.isNotEmpty()) {
                val appInfo = pm.getApplicationInfo(packageNames[0], 0)
                pm.getApplicationLabel(appInfo).toString()
            } else {
                Log.e("Firewall App", "Failed for UID: $uid")
                null
            }
        } catch (e: Exception) {
            Log.e("FirewallVPN", "Error resolving UID $uid: ${e.message}")
            null
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "My Foreground Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    private fun buildForegroundNotification(): Notification {

        createNotificationChannel()

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Firewall VPN Running")
            .setContentText("Monitoring app traffic...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)

        return builder.build()
    }
}