package com.uncode.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.nio.ByteBuffer
import java.util.ArrayList
import java.util.Arrays
import java.util.HashSet
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * LocalDnsVpnService:
 * Ultra-lightweight loopback Android VpnService that operates entirely on-device.
 *
 * It only routes traffic for its virtual DNS IP (10.111.222.1/32 and fd00:111:222::1/128).
 * All standard TCP/HTTPS connections (Chrome, apps, streaming) flow directly
 * over Wi-Fi / Cellular with ZERO latency overhead and 100% BYOK privacy.
 *
 * For DNS queries:
 * - Distracting/blacklisted domains (TikTok, Instagram, Reddit, etc.) are sinkholed to NXDOMAIN.
 * - Allowed study/research domains are forwarded to upstream DNS (1.1.1.1 / 8.8.8.8) via protected sockets.
 */
class LocalDnsVpnService : VpnService() {

    companion object {
        private const val TAG = "LocalDnsVpnService"
        private const val CHANNEL_ID = "qiezka_dns_vpn"
        private const val NOTIF_ID = 8801
        private const val PREFS_NAME = "uncode_lock"

        const val ACTION_STOP = "com.uncode.app.ACTION_STOP_VPN"

        @JvmField
        @Volatile
        var isRunning: Boolean = false

        @Volatile
        private var sInstance: LocalDnsVpnService? = null

        private const val VPN_INTERFACE_IP = "10.111.222.1"
        private const val VPN_DNS_SERVER_IP = "10.111.222.2"
        private const val VPN_INTERFACE_IPV6 = "fd00:111:222::1"
        private const val VPN_DNS_SERVER_IPV6 = "fd00:111:222::2"

        @JvmStatic
        fun startVpn(context: Context) {
            if (isRunning) return
            try {
                val intent = Intent(context, LocalDnsVpnService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start LocalDnsVpnService: ${e.message}")
            }
        }

        @JvmStatic
        fun stopVpn(context: Context?) {
            isRunning = false

            // 1. Direct synchronous teardown of active TUN interface & worker threads
            val instance = sInstance
            if (instance != null) {
                try {
                    instance.stopDnsSinkhole()
                    instance.stopSelf()
                } catch (e: Exception) {
                    Log.w(TAG, "Direct instance teardown warning: ${e.message}")
                }
            }

            // 2. Standard Android service teardown via stopService
            if (context != null) {
                try {
                    val stopIntent = Intent(context, LocalDnsVpnService::class.java)
                    context.stopService(stopIntent)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to call context.stopService: ${e.message}")
                }
            }
        }

        @JvmStatic
        fun updateNotification(context: Context?) {
            if (!isRunning || context == null) return
            try {
                val instance = sInstance
                if (instance != null) {
                    val notif = instance.buildNotification()
                    val nm = instance.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    nm?.notify(NOTIF_ID, notif)
                    return
                }
                val intent = Intent(context, LocalDnsVpnService::class.java).apply {
                    action = "ACTION_UPDATE_NOTIF"
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (ignore: Exception) {}
        }
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private var workerThread: Thread? = null
    private var dnsExecutor: ExecutorService? = null
    @Volatile
    private var shouldRun: Boolean = false

    override fun onCreate() {
        super.onCreate()
        sInstance = this
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null && ACTION_STOP == intent.action) {
            Log.i(TAG, "Received ACTION_STOP: cleanly closing tun0 and stopping service")
            stopDnsSinkhole()
            stopSelf()
            return START_NOT_STICKY
        }

        createNotificationChannel()
        val notif = buildNotification()

        if (intent != null && "ACTION_UPDATE_NOTIF" == intent.action) {
            try {
                if (Build.VERSION.SDK_INT >= 34) {
                    startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED)
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED)
                } else {
                    startForeground(NOTIF_ID, notif)
                }
            } catch (e: Exception) {
                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                nm?.notify(NOTIF_ID, notif)
            }
            return START_STICKY
        }

        if (Build.VERSION.SDK_INT >= 34) {
            try {
                startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED)
            } catch (e: Exception) {
                Log.w(TAG, "systemExempted startForeground failed, falling back to specialUse: ${e.message}")
                startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED)
            } catch (e: Exception) {
                startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            }
        } else {
            startForeground(NOTIF_ID, notif)
        }

        startDnsSinkhole()
        return START_STICKY
    }

    override fun onDestroy() {
        stopDnsSinkhole()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
        } catch (e: Exception) {
            Log.w(TAG, "stopForeground error: ${e.message}")
        }
        if (sInstance == this) {
            sInstance = null
        }
        super.onDestroy()
    }

    @Synchronized
    private fun startDnsSinkhole() {
        if (workerThread != null && workerThread?.isAlive == true) return

        try {
            val builder = Builder().apply {
                setSession("QIEZKA DNS Guard")
                setMtu(1500)
                addAddress(VPN_INTERFACE_IP, 24)
                addDnsServer(VPN_DNS_SERVER_IP)
                addRoute(VPN_DNS_SERVER_IP, 32) // Virtual DNS IP

                // IPv6 Virtual Interface & DNS
                try {
                    addAddress(VPN_INTERFACE_IPV6, 128)
                    addDnsServer(VPN_DNS_SERVER_IPV6)
                    addRoute(VPN_DNS_SERVER_IPV6, 128)
                } catch (e: Exception) {
                    Log.w(TAG, "IPv6 address/DNS configuration warning: ${e.message}")
                }

                // Route common public DNS servers
                try {
                    addRoute("8.8.8.8", 32)
                    addRoute("8.8.4.4", 32)
                    addRoute("1.1.1.1", 32)
                    addRoute("1.0.0.1", 32)
                    addRoute("9.9.9.9", 32)
                    addRoute("208.67.222.222", 32)
                    addRoute("208.67.220.220", 32)
                    addRoute("94.140.14.14", 32)
                    addRoute("94.140.15.15", 32)
                    addRoute("2001:4860:4860::8888", 128)
                    addRoute("2001:4860:4860::8844", 128)
                    addRoute("2606:4700:4700::1111", 128)
                    addRoute("2606:4700:4700::1001", 128)
                    addRoute("2620:fe::fe", 128)
                    addRoute("2620:fe::9", 128)
                    addRoute("180.76.76.200", 32) // Baidu HTTPDNS primary
                    addRoute("180.76.76.76", 32)  // Baidu HTTPDNS secondary
                    addRoute("203.107.1.1", 32)   // Alibaba HTTPDNS primary
                    addRoute("203.107.1.33", 32)  // Alibaba HTTPDNS secondary
                    addRoute("119.29.29.29", 32)  // Tencent HTTPDNS
                } catch (ignore: Exception) {}

                // Intercept all physical networks' underlying DNS servers
                try {
                    val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                    if (cm != null) {
                        val allNets = cm.allNetworks
                        for (net in allNets) {
                            val caps = cm.getNetworkCapabilities(net)
                            if (caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                                continue
                            }
                            val lp = cm.getLinkProperties(net)
                            if (lp != null) {
                                for (dns in lp.dnsServers) {
                                    val host = dns.hostAddress
                                    if (host != null) {
                                        if (dns is Inet4Address) {
                                            addRoute(host, 32)
                                        } else if (dns is Inet6Address) {
                                            addRoute(host, 128)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (ignore: Exception) {}

                try {
                    addDisallowedApplication(packageName)
                } catch (ignore: Exception) {}

                setBlocking(true)
            }

            vpnInterface = builder.establish()
            if (vpnInterface == null) {
                Log.e(TAG, "Failed to establish VPN interface (prepare() may be required)")
                stopSelf()
                return
            }

            shouldRun = true
            isRunning = true

            dnsExecutor = Executors.newFixedThreadPool(8)
            workerThread = Thread(DnsWorker(), "QiezkaDnsWorker").apply {
                start()
            }
            Log.i(TAG, "LocalDnsVpnService started successfully. Interface=$VPN_INTERFACE_IP, DNS=$VPN_DNS_SERVER_IP")
        } catch (e: Exception) {
            Log.e(TAG, "Error establishing DNS VPN: ${e.message}", e)
            stopSelf()
        }
    }

    @Synchronized
    private fun stopDnsSinkhole() {
        shouldRun = false
        isRunning = false

        workerThread?.interrupt()
        workerThread = null

        dnsExecutor?.shutdownNow()
        dnsExecutor = null

        vpnInterface?.let {
            try {
                it.close()
            } catch (ignore: Exception) {}
            vpnInterface = null
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
        } catch (ignore: Exception) {}

        Log.i(TAG, "LocalDnsVpnService stopped")
    }

    private inner class DnsWorker : Runnable {
        override fun run() {
            val vpn = vpnInterface ?: return

            val `in` = FileInputStream(vpn.fileDescriptor)
            val out = FileOutputStream(vpn.fileDescriptor)
            val buffer = ByteArray(32767)

            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

            while (shouldRun && !Thread.currentThread().isInterrupted) {
                try {
                    val length = `in`.read(buffer)
                    if (length <= 0) continue

                    val version = (buffer[0].toInt() shr 4) and 0x0F
                    val isIpv6 = (version == 6)
                    val ipHeaderLength: Int
                    val srcPort: Int
                    val dstPort: Int
                    val dnsOffset: Int
                    val dnsLength: Int

                    if (version == 4) {
                        if (length < 28) continue
                        val protocol = buffer[9].toInt() and 0xFF
                        if (protocol != 17) continue // UDP only (17)
                        ipHeaderLength = (buffer[0].toInt() and 0x0F) * 4
                        if (length < ipHeaderLength + 8) continue
                        srcPort = ((buffer[ipHeaderLength].toInt() and 0xFF) shl 8) or (buffer[ipHeaderLength + 1].toInt() and 0xFF)
                        dstPort = ((buffer[ipHeaderLength + 2].toInt() and 0xFF) shl 8) or (buffer[ipHeaderLength + 3].toInt() and 0xFF)
                        dnsOffset = ipHeaderLength + 8
                        dnsLength = length - dnsOffset
                    } else if (version == 6) {
                        if (length < 48) continue // 40-byte IPv6 header + 8-byte UDP header
                        val nextHeader = buffer[6].toInt() and 0xFF
                        if (nextHeader != 17) continue // UDP only
                        ipHeaderLength = 40
                        srcPort = ((buffer[40].toInt() and 0xFF) shl 8) or (buffer[41].toInt() and 0xFF)
                        dstPort = ((buffer[42].toInt() and 0xFF) shl 8) or (buffer[43].toInt() and 0xFF)
                        dnsOffset = 48
                        dnsLength = length - dnsOffset
                    } else {
                        continue
                    }

                    if (dstPort != 53) continue // Only handle DNS queries
                    if (dnsLength < 12) continue // Minimum DNS header size

                    val packetData = Arrays.copyOf(buffer, length)
                    val finalIsIpv6 = isIpv6
                    val finalIpHeaderLen = ipHeaderLength
                    val finalSrcPort = srcPort
                    val finalDstPort = dstPort
                    val finalDnsOffset = dnsOffset
                    val finalDnsLength = dnsLength

                    val executor = dnsExecutor
                    if (executor != null && !executor.isShutdown) {
                        executor.execute {
                            try {
                                handleDnsPacket(
                                    packetData,
                                    finalIsIpv6,
                                    finalIpHeaderLen,
                                    finalSrcPort,
                                    finalDstPort,
                                    finalDnsOffset,
                                    finalDnsLength,
                                    prefs,
                                    out
                                )
                            } catch (e: Exception) {
                                Log.d(TAG, "handleDnsPacket error: ${e.message}")
                            }
                        }
                    }
                } catch (e: Exception) {
                    if (!shouldRun) break
                }
            }
        }
    }

    private fun handleDnsPacket(
        packet: ByteArray,
        isIpv6: Boolean,
        ipHeaderLength: Int,
        srcPort: Int,
        dstPort: Int,
        dnsOffset: Int,
        dnsLength: Int,
        prefs: SharedPreferences,
        out: FileOutputStream
    ) {
        val queryDomain = parseDnsQuestionDomain(packet, dnsOffset, packet.size) ?: ""
        val lowerDomain = queryDomain.toLowerCase(Locale.US)

        val isLockdown = prefs.getBoolean("lockdown_active", false)
        val isConsequence = prefs.getBoolean("consequence_active", false)
        val isEnforcing = isLockdown || isConsequence

        // Defense-in-depth: if system is in idle free time (no active session), bypass WebClassifier
        if (!isEnforcing) {
            val dnsPayload = Arrays.copyOfRange(packet, dnsOffset, dnsOffset + dnsLength)
            val upstreamResponse = forwardToUpstreamDns(dnsPayload, prefs)
            if (upstreamResponse != null) {
                val responseIpPacket = if (isIpv6) {
                    buildUdpIp6Packet(packet, dstPort, srcPort, upstreamResponse)
                } else {
                    buildUdpIpPacket(packet, ipHeaderLength, dstPort, srcPort, upstreamResponse)
                }
                synchronized(out) {
                    try {
                        out.write(responseIpPacket)
                    } catch (ignore: Exception) {}
                }
            }
            return
        }

        val allowYoutube = prefs.getBoolean("allow_youtube", false)
        val activeServiceIds = prefs.getStringSet("active_unified_services", null)
        val allowedDomains = HashSet<String>(UnifiedPolicyRegistry.getDomainsForServices(activeServiceIds))
        val customAllowed = prefs.getStringSet("allowed_domains", null)
        if (customAllowed != null) allowedDomains.addAll(customAllowed)
        if (allowYoutube) {
            val yt = UnifiedPolicyRegistry.SERVICES["youtube"]
            if (yt != null) allowedDomains.addAll(yt.domains)
        }

        // 1. Check if domain is blocked via WebClassifier multi-genre intelligence
        val classResult = WebClassifier.classifyDomain(lowerDomain, allowYoutube, allowedDomains)
        if (classResult.isBlocked) {
            // Synthesize local sinkhole NXDOMAIN response
            val responseDns = buildSinkholeResponse(packet, dnsOffset, dnsLength)
            if (responseDns != null) {
                val responseIpPacket = if (isIpv6) {
                    buildUdpIp6Packet(packet, dstPort, srcPort, responseDns)
                } else {
                    buildUdpIpPacket(packet, ipHeaderLength, dstPort, srcPort, responseDns)
                }
                synchronized(out) {
                    try {
                        out.write(responseIpPacket)
                    } catch (ignore: Exception) {}
                }
                Log.d(TAG, "Sinkholed DNS query (NXDOMAIN): $queryDomain -> ${classResult.reason}")
            }
            return
        }

        // 2. Forward legitimate study/research query to upstream DNS resolver (Cloudflare / Google)
        val dnsPayload = Arrays.copyOfRange(packet, dnsOffset, dnsOffset + dnsLength)
        val upstreamResponse = forwardToUpstreamDns(dnsPayload, prefs)
        if (upstreamResponse != null) {
            val responseIpPacket = if (isIpv6) {
                buildUdpIp6Packet(packet, dstPort, srcPort, upstreamResponse)
            } else {
                buildUdpIpPacket(packet, ipHeaderLength, dstPort, srcPort, upstreamResponse)
            }
            synchronized(out) {
                try {
                    out.write(responseIpPacket)
                } catch (ignore: Exception) {}
            }
        }
    }

    private fun parseDnsQuestionDomain(packet: ByteArray, dnsOffset: Int, totalLength: Int): String? {
        var pos = dnsOffset + 12 // Skip 12-byte header
        val sb = StringBuilder()

        while (pos < totalLength) {
            val labelLen = packet[pos].toInt() and 0xFF
            if (labelLen == 0) break
            pos++
            if (pos + labelLen > totalLength) return null

            if (sb.isNotEmpty()) sb.append('.')
            for (i in 0 until labelLen) {
                sb.append((packet[pos + i].toInt() and 0xFF).toChar())
            }
            pos += labelLen
        }
        return sb.toString()
    }

    /**
     * Calculates the exact byte length of the DNS Question section.
     */
    private fun getDnsQuestionLength(packet: ByteArray, dnsOffset: Int, totalLength: Int): Int {
        var pos = dnsOffset + 12
        while (pos < totalLength) {
            val labelLen = packet[pos].toInt() and 0xFF
            if (labelLen == 0) {
                pos++
                break
            }
            pos += 1 + labelLen
        }
        if (pos + 4 <= totalLength) {
            return (pos + 4) - (dnsOffset + 12)
        }
        return Math.max(0, totalLength - (dnsOffset + 12))
    }

    private fun buildSinkholeResponse(packet: ByteArray, dnsOffset: Int, dnsLength: Int): ByteArray? {
        return try {
            val qLen = getDnsQuestionLength(packet, dnsOffset, packet.size)
            val respLen = 12 + qLen
            val buf = ByteBuffer.allocate(respLen)

            // 1. Transaction ID (2 bytes)
            buf.put(packet[dnsOffset])
            buf.put(packet[dnsOffset + 1])

            // 2. Flags: Response, Opcode 0, Authoritative, Recursion Available, RCODE = 3 (NXDOMAIN)
            buf.put(0x81.toByte())
            buf.put(0x83.toByte())

            // 3. QDCOUNT = 1, ANCOUNT = 0, NSCOUNT = 0, ARCOUNT = 0
            buf.putShort(1.toShort())
            buf.putShort(0.toShort())
            buf.putShort(0.toShort())
            buf.putShort(0.toShort())

            // 4. Copy strictly the Question Section
            buf.put(packet, dnsOffset + 12, qLen)

            Arrays.copyOf(buf.array(), buf.position())
        } catch (e: Exception) {
            null
        }
    }

    private fun parseDnsQuestionType(packet: ByteArray, dnsOffset: Int, totalLength: Int): Int {
        var pos = dnsOffset + 12
        while (pos < totalLength) {
            val labelLen = packet[pos].toInt() and 0xFF
            if (labelLen == 0) {
                pos++
                break
            }
            pos += 1 + labelLen
        }
        if (pos + 2 <= totalLength) {
            return ((packet[pos].toInt() and 0xFF) shl 8) or (packet[pos + 1].toInt() and 0xFF)
        }
        return 1 // Default to Type A
    }

    private fun buildARecordResponse(packet: ByteArray, dnsOffset: Int, dnsLength: Int, ipBytes: ByteArray): ByteArray? {
        return try {
            val qLen = getDnsQuestionLength(packet, dnsOffset, packet.size)
            val respLen = 12 + qLen + 16
            val buf = ByteBuffer.allocate(respLen)

            // 1. Transaction ID (2 bytes)
            buf.put(packet[dnsOffset])
            buf.put(packet[dnsOffset + 1])

            // 2. Flags: Response, Opcode 0, Authoritative, Recursion Available, RCODE = 0 (NOERROR)
            buf.put(0x81.toByte())
            buf.put(0x80.toByte())

            // 3. QDCOUNT = 1, ANCOUNT = 1, NSCOUNT = 0, ARCOUNT = 0
            buf.putShort(1.toShort())
            buf.putShort(1.toShort())
            buf.putShort(0.toShort())
            buf.putShort(0.toShort())

            // 4. Copy strictly the Question Section
            buf.put(packet, dnsOffset + 12, qLen)

            // 5. Answer Section (Compression pointer to Question at offset 12 -> 0xC00C)
            buf.put(0xC0.toByte())
            buf.put(0x0C.toByte())
            buf.putShort(1.toShort()) // Type A
            buf.putShort(1.toShort()) // Class IN
            buf.putInt(300)          // TTL: 300s
            buf.putShort(4.toShort()) // Data length: 4 bytes
            buf.put(ipBytes)

            Arrays.copyOf(buf.array(), buf.position())
        } catch (e: Exception) {
            null
        }
    }

    private fun buildNoDataResponse(packet: ByteArray, dnsOffset: Int, dnsLength: Int): ByteArray? {
        return try {
            val qLen = getDnsQuestionLength(packet, dnsOffset, packet.size)
            val respLen = 12 + qLen
            val buf = ByteBuffer.allocate(respLen)

            // 1. Transaction ID (2 bytes)
            buf.put(packet[dnsOffset])
            buf.put(packet[dnsOffset + 1])

            // 2. Flags: Response, Opcode 0, Authoritative, Recursion Available, RCODE = 0 (NOERROR)
            buf.put(0x81.toByte())
            buf.put(0x80.toByte())

            // 3. QDCOUNT = 1, ANCOUNT = 0, NSCOUNT = 0, ARCOUNT = 0
            buf.putShort(1.toShort())
            buf.putShort(0.toShort())
            buf.putShort(0.toShort())
            buf.putShort(0.toShort())

            // 4. Copy strictly the Question Section
            buf.put(packet, dnsOffset + 12, qLen)

            Arrays.copyOf(buf.array(), buf.position())
        } catch (e: Exception) {
            null
        }
    }

    private fun getUpstreamDnsServers(prefs: SharedPreferences): List<InetAddress> {
        val servers = ArrayList<InetAddress>()
        addDnsServer(servers, "1.1.1.1")
        addDnsServer(servers, "1.0.0.1")
        addDnsServer(servers, "8.8.8.8")
        addDnsServer(servers, "8.8.4.4")
        addDnsServer(servers, "2606:4700:4700::1111")
        addDnsServer(servers, "2001:4860:4860::8888")

        try {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            if (cm != null) {
                val allNets = cm.allNetworks
                for (net in allNets) {
                    val caps = cm.getNetworkCapabilities(net)
                    if (caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                        continue
                    }
                    val lp = cm.getLinkProperties(net)
                    if (lp != null) {
                        for (dns in lp.dnsServers) {
                            if (!servers.contains(dns)) {
                                servers.add(dns)
                            }
                        }
                    }
                }
            }
        } catch (ignore: Exception) {}

        return servers
    }

    private fun addDnsServer(list: MutableList<InetAddress>, ip: String) {
        try {
            list.add(InetAddress.getByName(ip))
        } catch (ignore: Exception) {}
    }

    private fun forwardToUpstreamDns(queryPayload: ByteArray, prefs: SharedPreferences): ByteArray? {
        val upstreamServers = getUpstreamDnsServers(prefs)
        var socket: DatagramSocket? = null
        try {
            socket = DatagramSocket()
            protect(socket) // ESSENTIAL: Exclude this socket from the VPN TUN interface!
            socket.soTimeout = 1200

            for (server in upstreamServers) {
                try {
                    val outPacket = DatagramPacket(queryPayload, queryPayload.size, server, 53)
                    socket.send(outPacket)

                    val recvBuf = ByteArray(2048)
                    val inPacket = DatagramPacket(recvBuf, recvBuf.size)
                    socket.receive(inPacket)

                    return Arrays.copyOf(inPacket.data, inPacket.length)
                } catch (nextServer: Exception) {
                    // Try next upstream server
                }
            }
            return null
        } catch (e: Exception) {
            return null
        } finally {
            if (socket != null && !socket.isClosed) {
                socket.close()
            }
        }
    }

    private fun buildUdpIpPacket(
        origPacket: ByteArray,
        ipHeaderLen: Int,
        newSrcPort: Int,
        newDstPort: Int,
        udpPayload: ByteArray
    ): ByteArray {
        val totalLen = 20 + 8 + udpPayload.size
        val ipPacket = ByteArray(totalLen)

        // IPv4 Header
        ipPacket[0] = 0x45.toByte() // IPv4, Header length = 5 (20 bytes)
        ipPacket[1] = 0x00.toByte() // DSCP / ECN
        ipPacket[2] = ((totalLen shr 8) and 0xFF).toByte()
        ipPacket[3] = (totalLen and 0xFF).toByte()
        ipPacket[4] = 0x00.toByte()
        ipPacket[5] = 0x01.toByte() // ID
        ipPacket[6] = 0x00.toByte()
        ipPacket[7] = 0x00.toByte() // Flags & Fragment Offset
        ipPacket[8] = 64.toByte()   // TTL
        ipPacket[9] = 17.toByte()   // Protocol: UDP

        // Swap Source IP and Destination IP
        System.arraycopy(origPacket, 16, ipPacket, 12, 4) // New src IP = orig dst IP (10.111.222.1)
        System.arraycopy(origPacket, 12, ipPacket, 16, 4) // New dst IP = orig src IP

        // IP Checksum
        val ipChecksum = computeChecksum(ipPacket, 0, 20)
        ipPacket[10] = ((ipChecksum shr 8) and 0xFF).toByte()
        ipPacket[11] = (ipChecksum and 0xFF).toByte()

        // UDP Header
        val udpLen = 8 + udpPayload.size
        ipPacket[20] = ((newSrcPort shr 8) and 0xFF).toByte()
        ipPacket[21] = (newSrcPort and 0xFF).toByte()
        ipPacket[22] = ((newDstPort shr 8) and 0xFF).toByte()
        ipPacket[23] = (newDstPort and 0xFF).toByte()
        ipPacket[24] = ((udpLen shr 8) and 0xFF).toByte()
        ipPacket[25] = (udpLen and 0xFF).toByte()
        ipPacket[26] = 0x00.toByte() // Checksum 0 = disabled for UDP IPv4
        ipPacket[27] = 0x00.toByte()

        // Payload
        System.arraycopy(udpPayload, 0, ipPacket, 28, udpPayload.size)
        return ipPacket
    }

    private fun computeChecksum(buf: ByteArray, offset: Int, length: Int): Int {
        var sum = 0
        var i = 0
        while (i < length) {
            val high = (buf[offset + i].toInt() and 0xFF) shl 8
            val low = if (i + 1 < length) (buf[offset + i + 1].toInt() and 0xFF) else 0
            sum += (high or low)
            i += 2
        }
        while ((sum shr 16) > 0) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }
        return sum.inv() and 0xFFFF
    }

    private fun buildUdpIp6Packet(
        origPacket: ByteArray,
        newSrcPort: Int,
        newDstPort: Int,
        udpPayload: ByteArray
    ): ByteArray {
        val udpLen = 8 + udpPayload.size
        val totalLen = 40 + udpLen
        val ipPacket = ByteArray(totalLen)

        // IPv6 Header (40 bytes)
        ipPacket[0] = 0x60.toByte() // Version 6, Traffic Class high = 0
        ipPacket[1] = 0x00.toByte() // Traffic Class low = 0, Flow Label high = 0
        ipPacket[2] = 0x00.toByte() // Flow label mid
        ipPacket[3] = 0x00.toByte() // Flow label low
        ipPacket[4] = ((udpLen shr 8) and 0xFF).toByte() // Payload length
        ipPacket[5] = (udpLen and 0xFF).toByte()
        ipPacket[6] = 17.toByte()   // Next Header: UDP (17)
        ipPacket[7] = 64.toByte()   // Hop Limit

        val newSrcIp = ByteArray(16)
        val newDstIp = ByteArray(16)
        System.arraycopy(origPacket, 24, newSrcIp, 0, 16) // new src = orig dst
        System.arraycopy(origPacket, 8, newDstIp, 0, 16)  // new dst = orig src

        System.arraycopy(newSrcIp, 0, ipPacket, 8, 16)
        System.arraycopy(newDstIp, 0, ipPacket, 24, 16)

        // UDP Header
        ipPacket[40] = ((newSrcPort shr 8) and 0xFF).toByte()
        ipPacket[41] = (newSrcPort and 0xFF).toByte()
        ipPacket[42] = ((newDstPort shr 8) and 0xFF).toByte()
        ipPacket[43] = (newDstPort and 0xFF).toByte()
        ipPacket[44] = ((udpLen shr 8) and 0xFF).toByte()
        ipPacket[45] = (udpLen and 0xFF).toByte()

        // Checksum (RFC 2460 / RFC 8200 mandatory for IPv6 UDP)
        val checksum = computeUdpIpv6Checksum(newSrcIp, newDstIp, udpPayload, newSrcPort, newDstPort)
        ipPacket[46] = ((checksum shr 8) and 0xFF).toByte()
        ipPacket[47] = (checksum and 0xFF).toByte()

        // Payload
        System.arraycopy(udpPayload, 0, ipPacket, 48, udpPayload.size)
        return ipPacket
    }

    private fun computeUdpIpv6Checksum(
        srcIp: ByteArray,
        dstIp: ByteArray,
        udpPayload: ByteArray,
        srcPort: Int,
        dstPort: Int
    ): Int {
        val udpLen = 8 + udpPayload.size
        var sum = 0L

        // 1. Source IPv6 Address (16 bytes = 8 16-bit words)
        var i = 0
        while (i < 16) {
            sum += ((srcIp[i].toInt() and 0xFF) shl 8) or (srcIp[i + 1].toInt() and 0xFF)
            i += 2
        }

        // 2. Destination IPv6 Address (16 bytes = 8 16-bit words)
        i = 0
        while (i < 16) {
            sum += ((dstIp[i].toInt() and 0xFF) shl 8) or (dstIp[i + 1].toInt() and 0xFF)
            i += 2
        }

        // 3. UDP Length (32 bits in IPv6 pseudo header)
        sum += (udpLen shr 16) and 0xFFFF
        sum += udpLen and 0xFFFF

        // 4. Next Header (Upper layer protocol = 17)
        sum += 17L

        // 5. UDP Source & Destination Ports
        sum += srcPort.toLong()
        sum += dstPort.toLong()

        // 6. UDP Length
        sum += udpLen.toLong()

        // 7. Payload
        i = 0
        while (i < udpPayload.size) {
            val high = (udpPayload[i].toInt() and 0xFF) shl 8
            val low = if (i + 1 < udpPayload.size) (udpPayload[i + 1].toInt() and 0xFF) else 0
            sum += (high or low).toLong()
            i += 2
        }

        // Fold 32-bit sum to 16 bits
        while ((sum shr 16) > 0) {
            sum = (sum and 0xFFFFL) + (sum shr 16)
        }

        val checksum = (sum.inv() and 0xFFFFL).toInt()
        return if (checksum == 0) 0xFFFF else checksum
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "QIEZKA Web Guard",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Active DNS sinkhole protecting focus sessions"
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        return builder
            .setContentTitle("QIEZKA Web Guard Active")
            .setContentText("WebClassifier On-Device Protection Active")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .build()
    }
}
