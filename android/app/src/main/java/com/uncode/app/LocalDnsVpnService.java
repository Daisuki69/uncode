package com.uncode.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * LocalDnsVpnService:
 * Ultra-lightweight loopback Android VpnService that operates entirely on-device.
 *
 * It only routes traffic for its virtual DNS IP (10.111.222.1/32).
 * All standard TCP/HTTPS connections (Chrome, apps, streaming) flow directly
 * over Wi-Fi / Cellular with ZERO latency overhead and 100% BYOK privacy.
 *
 * For DNS queries:
 * - Distracting/blacklisted domains (TikTok, Instagram, Reddit, etc.) are sinkholed to 0.0.0.0.
 * - Allowed study/research domains are forwarded to upstream DNS (1.1.1.1 / 8.8.8.8) via protected sockets.
 */
public class LocalDnsVpnService extends VpnService {

    private static final String TAG = "LocalDnsVpnService";
    private static final String CHANNEL_ID = "qiezka_dns_vpn";
    private static final int NOTIF_ID = 8801;
    private static final String PREFS_NAME = "uncode_lock";

    private static final String VPN_INTERFACE_IP = "10.111.222.1";
    private static final String VPN_DNS_SERVER_IP = "10.111.222.2";


    public static volatile boolean isRunning = false;

    private ParcelFileDescriptor vpnInterface = null;
    private Thread workerThread = null;
    private ExecutorService dnsExecutor = null;
    private volatile boolean shouldRun = false;

    // Web distraction and gaming databases are now centralized in WebBlocklistConstants.java


    public static void startVpn(Context context) {
        if (isRunning) return;
        try {
            Intent intent = new Intent(context, LocalDnsVpnService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent);
            } else {
                context.startService(intent);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to start LocalDnsVpnService: " + e.getMessage());
        }
    }

    public static void stopVpn(Context context) {
        try {
            Intent intent = new Intent(context, LocalDnsVpnService.class);
            context.stopService(intent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to stop LocalDnsVpnService: " + e.getMessage());
        }
    }

    public static void updateNotification(Context context) {
        if (!isRunning || context == null) return;
        try {
            Intent intent = new Intent(context, LocalDnsVpnService.class);
            intent.setAction("ACTION_UPDATE_NOTIF");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent);
            } else {
                context.startService(intent);
            }
        } catch (Exception ignore) {}
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();
        Notification notif = buildNotification();

        if (intent != null && "ACTION_UPDATE_NOTIF".equals(intent.getAction())) {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.notify(NOTIF_ID, notif);
            }
            return START_STICKY;
        }

        if (Build.VERSION.SDK_INT >= 34) {
            try {
                startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED);
            } catch (Exception e) {
                Log.w(TAG, "systemExempted startForeground failed, falling back to specialUse: " + e.getMessage());
                startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED);
            } catch (Exception e) {
                startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            }
        } else {
            startForeground(NOTIF_ID, notif);
        }

        startDnsSinkhole();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        stopDnsSinkhole();
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE);
            } else {
                stopForeground(true);
            }
        } catch (Exception e) {
            Log.w(TAG, "stopForeground error: " + e.getMessage());
        }
        super.onDestroy();
    }

    private synchronized void startDnsSinkhole() {
        if (workerThread != null && workerThread.isAlive()) return;

        try {
            Builder builder = new Builder();
            builder.setSession("QIEZKA DNS Guard");
            builder.setMtu(1500);

            builder.addAddress(VPN_INTERFACE_IP, 24);
            builder.addDnsServer(VPN_DNS_SERVER_IP);
            builder.addRoute(VPN_DNS_SERVER_IP, 32); // Virtual DNS IP
            // Route common public DNS servers so apps cannot bypass local DNS by querying public resolvers directly
            try {
                builder.addRoute("8.8.8.8", 32);
                builder.addRoute("8.8.4.4", 32);
                builder.addRoute("1.1.1.1", 32);
                builder.addRoute("1.0.0.1", 32);
                builder.addRoute("9.9.9.9", 32);
                builder.addRoute("208.67.222.222", 32);
            } catch (Exception ignore) {}

            try {
                builder.addDisallowedApplication(getPackageName());
            } catch (Exception ignore) {}

            builder.setBlocking(true);

            vpnInterface = builder.establish();
            if (vpnInterface == null) {
                Log.e(TAG, "Failed to establish VPN interface (prepare() may be required)");
                stopSelf();
                return;
            }

            shouldRun = true;
            isRunning = true;

            dnsExecutor = Executors.newFixedThreadPool(8);
            workerThread = new Thread(new DnsWorker(), "QiezkaDnsWorker");
            workerThread.start();
            Log.i(TAG, "LocalDnsVpnService started successfully. Interface=" + VPN_INTERFACE_IP + ", DNS=" + VPN_DNS_SERVER_IP);
        } catch (Exception e) {
            Log.e(TAG, "Error establishing DNS VPN: " + e.getMessage(), e);
            stopSelf();
        }
    }

    private synchronized void stopDnsSinkhole() {
        shouldRun = false;
        isRunning = false;

        if (workerThread != null) {
            workerThread.interrupt();
            workerThread = null;
        }

        if (dnsExecutor != null) {
            dnsExecutor.shutdownNow();
            dnsExecutor = null;
        }

        if (vpnInterface != null) {
            try {
                vpnInterface.close();
            } catch (Exception ignore) {}
            vpnInterface = null;
        }
        Log.i(TAG, "LocalDnsVpnService stopped");
    }

    private class DnsWorker implements Runnable {
        @Override
        public void run() {
            if (vpnInterface == null) return;

            FileInputStream in = new FileInputStream(vpnInterface.getFileDescriptor());
            FileOutputStream out = new FileOutputStream(vpnInterface.getFileDescriptor());
            byte[] buffer = new byte[32767];

            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

            while (shouldRun && !Thread.currentThread().isInterrupted()) {
                try {
                    int length = in.read(buffer);
                    if (length <= 0) continue;

                    // IPv4 Header verification
                    if (length < 28) continue;
                    int version = (buffer[0] >> 4) & 0x0F;
                    if (version != 4) continue; // IPv4 only

                    int protocol = buffer[9] & 0xFF;
                    if (protocol != 17) continue; // UDP only (17)

                    int ipHeaderLength = (buffer[0] & 0x0F) * 4;
                    if (length < ipHeaderLength + 8) continue;

                    int srcPort = ((buffer[ipHeaderLength] & 0xFF) << 8) | (buffer[ipHeaderLength + 1] & 0xFF);
                    int dstPort = ((buffer[ipHeaderLength + 2] & 0xFF) << 8) | (buffer[ipHeaderLength + 3] & 0xFF);

                    if (dstPort != 53) continue; // Only handle DNS queries

                    int dnsOffset = ipHeaderLength + 8;
                    int dnsLength = length - dnsOffset;
                    if (dnsLength < 12) continue; // Minimum DNS header size

                    final byte[] packetData = Arrays.copyOf(buffer, length);
                    final int finalIpHeaderLen = ipHeaderLength;
                    final int finalSrcPort = srcPort;
                    final int finalDstPort = dstPort;
                    final int finalDnsOffset = dnsOffset;
                    final int finalDnsLength = dnsLength;

                    if (dnsExecutor != null && !dnsExecutor.isShutdown()) {
                        dnsExecutor.execute(() -> {
                            try {
                                handleDnsPacket(packetData, finalIpHeaderLen, finalSrcPort, finalDstPort, finalDnsOffset, finalDnsLength, prefs, out);
                            } catch (Exception e) {
                                Log.d(TAG, "handleDnsPacket error: " + e.getMessage());
                            }
                        });
                    }
                } catch (Exception e) {
                    if (!shouldRun) break;
                }
            }
        }
    }

    private void handleDnsPacket(byte[] packet, int ipHeaderLength, int srcPort, int dstPort, int dnsOffset, int dnsLength, SharedPreferences prefs, FileOutputStream out) {
        String queryDomain = parseDnsQuestionDomain(packet, dnsOffset, packet.length);
        if (queryDomain == null) queryDomain = "";
        String lowerDomain = queryDomain.toLowerCase(Locale.US);

        boolean allowYoutube = prefs.getBoolean("allow_youtube", false);
        // 1. Check if domain is blocked via WebClassifier multi-genre intelligence
        WebClassifier.ClassificationResult classResult = WebClassifier.classifyDomain(lowerDomain, allowYoutube);
        if (classResult.isBlocked) {
            // Synthesize local sinkhole NXDOMAIN response
            byte[] responseDns = buildSinkholeResponse(packet, dnsOffset, dnsLength);
            if (responseDns != null) {
                byte[] responseIpPacket = buildUdpIpPacket(
                    packet, ipHeaderLength, dstPort, srcPort, responseDns
                );
                synchronized (out) {
                    try {
                        out.write(responseIpPacket);
                    } catch (Exception ignore) {}
                }
                Log.d(TAG, "Sinkholed DNS query (NXDOMAIN): " + queryDomain + " -> " + classResult.reason);
            }
            return;
        }

        // 2. Forward legitimate study/research query to upstream DNS resolver (Cloudflare / Google)
        byte[] dnsPayload = Arrays.copyOfRange(packet, dnsOffset, dnsOffset + dnsLength);
        byte[] upstreamResponse = forwardToUpstreamDns(dnsPayload, prefs);
        if (upstreamResponse != null) {
            byte[] responseIpPacket = buildUdpIpPacket(
                packet, ipHeaderLength, dstPort, srcPort, upstreamResponse
            );
            synchronized (out) {
                try {
                    out.write(responseIpPacket);
                } catch (Exception ignore) {}
            }
        }
    }


    private String parseDnsQuestionDomain(byte[] packet, int dnsOffset, int totalLength) {
        int pos = dnsOffset + 12; // Skip 12-byte header
        StringBuilder sb = new StringBuilder();

        while (pos < totalLength) {
            int labelLen = packet[pos] & 0xFF;
            if (labelLen == 0) break;
            pos++;
            if (pos + labelLen > totalLength) return null;

            if (sb.length() > 0) sb.append('.');
            for (int i = 0; i < labelLen; i++) {
                sb.append((char) (packet[pos + i] & 0xFF));
            }
            pos += labelLen;
        }
        return sb.toString();
    }

    /**
     * Calculates the exact byte length of the DNS Question section.
     * RFC 1035 / RFC 6891: Question section ends with QTYPE (2 bytes) and QCLASS (2 bytes)
     * after the terminating 0x00 root label. Ensures any subsequent EDNS0 OPT pseudo-records
     * in the query's Additional section are excluded from the synthesized Question section.
     */
    private int getDnsQuestionLength(byte[] packet, int dnsOffset, int totalLength) {
        int pos = dnsOffset + 12;
        while (pos < totalLength) {
            int labelLen = packet[pos] & 0xFF;
            if (labelLen == 0) {
                pos++;
                break;
            }
            pos += 1 + labelLen;
        }
        if (pos + 4 <= totalLength) {
            return (pos + 4) - (dnsOffset + 12);
        }
        return Math.max(0, totalLength - (dnsOffset + 12));
    }

    private byte[] buildSinkholeResponse(byte[] packet, int dnsOffset, int dnsLength) {
        try {
            int qLen = getDnsQuestionLength(packet, dnsOffset, packet.length);
            int respLen = 12 + qLen;
            ByteBuffer buf = ByteBuffer.allocate(respLen);

            // 1. Transaction ID (2 bytes)
            buf.put(packet[dnsOffset]);
            buf.put(packet[dnsOffset + 1]);

            // 2. Flags: Response, Opcode 0, Authoritative, Recursion Available, RCODE = 3 (NXDOMAIN)
            buf.put((byte) 0x81);
            buf.put((byte) 0x83);

            // 3. QDCOUNT = 1, ANCOUNT = 0, NSCOUNT = 0, ARCOUNT = 0
            buf.putShort((short) 1);
            buf.putShort((short) 0);
            buf.putShort((short) 0);
            buf.putShort((short) 0);

            // 4. Copy strictly the Question Section
            buf.put(packet, dnsOffset + 12, qLen);

            return Arrays.copyOf(buf.array(), buf.position());
        } catch (Exception e) {
            return null;
        }
    }

    private int parseDnsQuestionType(byte[] packet, int dnsOffset, int totalLength) {
        int pos = dnsOffset + 12;
        while (pos < totalLength) {
            int labelLen = packet[pos] & 0xFF;
            if (labelLen == 0) {
                pos++;
                break;
            }
            pos += 1 + labelLen;
        }
        if (pos + 2 <= totalLength) {
            return ((packet[pos] & 0xFF) << 8) | (packet[pos + 1] & 0xFF);
        }
        return 1; // Default to Type A
    }

    private byte[] buildARecordResponse(byte[] packet, int dnsOffset, int dnsLength, byte[] ipBytes) {
        try {
            int qLen = getDnsQuestionLength(packet, dnsOffset, packet.length);
            int respLen = 12 + qLen + 16;
            ByteBuffer buf = ByteBuffer.allocate(respLen);

            // 1. Transaction ID (2 bytes)
            buf.put(packet[dnsOffset]);
            buf.put(packet[dnsOffset + 1]);

            // 2. Flags: Response, Opcode 0, Authoritative, Recursion Available, RCODE = 0 (NOERROR)
            buf.put((byte) 0x81);
            buf.put((byte) 0x80);

            // 3. QDCOUNT = 1, ANCOUNT = 1, NSCOUNT = 0, ARCOUNT = 0
            buf.putShort((short) 1);
            buf.putShort((short) 1);
            buf.putShort((short) 0);
            buf.putShort((short) 0);

            // 4. Copy strictly the Question Section (RFC 1035 / RFC 6891 compliant)
            buf.put(packet, dnsOffset + 12, qLen);

            // 5. Answer Section (Compression pointer to Question at offset 12 -> 0xC00C)
            buf.put((byte) 0xC0);
            buf.put((byte) 0x0C);
            buf.putShort((short) 1); // Type A
            buf.putShort((short) 1); // Class IN
            buf.putInt(300);         // TTL: 300s
            buf.putShort((short) 4); // Data length: 4 bytes
            buf.put(ipBytes);

            return Arrays.copyOf(buf.array(), buf.position());
        } catch (Exception e) {
            return null;
        }
    }

    private byte[] buildNoDataResponse(byte[] packet, int dnsOffset, int dnsLength) {
        try {
            int qLen = getDnsQuestionLength(packet, dnsOffset, packet.length);
            int respLen = 12 + qLen;
            ByteBuffer buf = ByteBuffer.allocate(respLen);

            // 1. Transaction ID (2 bytes)
            buf.put(packet[dnsOffset]);
            buf.put(packet[dnsOffset + 1]);

            // 2. Flags: Response, Opcode 0, Authoritative, Recursion Available, RCODE = 0 (NOERROR)
            buf.put((byte) 0x81);
            buf.put((byte) 0x80);

            // 3. QDCOUNT = 1, ANCOUNT = 0, NSCOUNT = 0, ARCOUNT = 0
            buf.putShort((short) 1);
            buf.putShort((short) 0);
            buf.putShort((short) 0);
            buf.putShort((short) 0);

            // 4. Copy strictly the Question Section
            buf.put(packet, dnsOffset + 12, qLen);

            return Arrays.copyOf(buf.array(), buf.position());
        } catch (Exception e) {
            return null;
        }
    }



    private List<InetAddress> getUpstreamDnsServers(SharedPreferences prefs) {
        List<InetAddress> servers = new ArrayList<>();
        // High-performance upstream resolvers for non-distraction research
        addDnsServer(servers, "1.1.1.1");
        addDnsServer(servers, "1.0.0.1");
        addDnsServer(servers, "8.8.8.8");
        addDnsServer(servers, "8.8.4.4");
        return servers;
    }

    private void addDnsServer(List<InetAddress> list, String ip) {
        try {
            list.add(InetAddress.getByName(ip));
        } catch (Exception ignore) {}
    }

    private byte[] forwardToUpstreamDns(byte[] queryPayload, SharedPreferences prefs) {
        List<InetAddress> upstreamServers = getUpstreamDnsServers(prefs);
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket();
            protect(socket); // ESSENTIAL: Exclude this socket from the VPN TUN interface!
            socket.setSoTimeout(1200);

            for (InetAddress server : upstreamServers) {
                try {
                    DatagramPacket outPacket = new DatagramPacket(queryPayload, queryPayload.length, server, 53);
                    socket.send(outPacket);

                    byte[] recvBuf = new byte[2048];
                    DatagramPacket inPacket = new DatagramPacket(recvBuf, recvBuf.length);
                    socket.receive(inPacket);

                    return Arrays.copyOf(inPacket.getData(), inPacket.getLength());
                } catch (Exception nextServer) {
                    // Try next upstream server
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }
    }

    private byte[] buildUdpIpPacket(byte[] origPacket, int ipHeaderLen, int newSrcPort, int newDstPort, byte[] udpPayload) {
        int totalLen = 20 + 8 + udpPayload.length;
        byte[] ipPacket = new byte[totalLen];

        // IPv4 Header
        ipPacket[0] = 0x45; // IPv4, Header length = 5 (20 bytes)
        ipPacket[1] = 0x00; // DSCP / ECN
        ipPacket[2] = (byte) ((totalLen >> 8) & 0xFF);
        ipPacket[3] = (byte) (totalLen & 0xFF);
        ipPacket[4] = 0x00;
        ipPacket[5] = 0x01; // ID
        ipPacket[6] = 0x00;
        ipPacket[7] = 0x00; // Flags & Fragment Offset
        ipPacket[8] = 64;   // TTL
        ipPacket[9] = 17;   // Protocol: UDP

        // Swap Source IP and Destination IP
        System.arraycopy(origPacket, 16, ipPacket, 12, 4); // New src IP = orig dst IP (10.111.222.1)
        System.arraycopy(origPacket, 12, ipPacket, 16, 4); // New dst IP = orig src IP

        // IP Checksum
        int ipChecksum = computeChecksum(ipPacket, 0, 20);
        ipPacket[10] = (byte) ((ipChecksum >> 8) & 0xFF);
        ipPacket[11] = (byte) (ipChecksum & 0xFF);

        // UDP Header
        int udpLen = 8 + udpPayload.length;
        ipPacket[20] = (byte) ((newSrcPort >> 8) & 0xFF);
        ipPacket[21] = (byte) (newSrcPort & 0xFF);
        ipPacket[22] = (byte) ((newDstPort >> 8) & 0xFF);
        ipPacket[23] = (byte) (newDstPort & 0xFF);
        ipPacket[24] = (byte) ((udpLen >> 8) & 0xFF);
        ipPacket[25] = (byte) (udpLen & 0xFF);
        ipPacket[26] = 0x00; // Checksum 0 = disabled for UDP IPv4
        ipPacket[27] = 0x00;

        // Payload
        System.arraycopy(udpPayload, 0, ipPacket, 28, udpPayload.length);
        return ipPacket;
    }

    private int computeChecksum(byte[] buf, int offset, int length) {
        int sum = 0;
        for (int i = 0; i < length; i += 2) {
            int high = (buf[offset + i] & 0xFF) << 8;
            int low = (i + 1 < length) ? (buf[offset + i + 1] & 0xFF) : 0;
            sum += (high | low);
        }
        while ((sum >> 16) > 0) {
            sum = (sum & 0xFFFF) + (sum >> 16);
        }
        return (~sum) & 0xFFFF;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "QIEZKA Web Guard",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Active DNS sinkhole protecting focus sessions");
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.createNotificationChannel(channel);
            }
        }
    }

    private Notification buildNotification() {
        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }

        return builder
            .setContentTitle("QIEZKA Web Guard Active")
            .setContentText("WebClassifier On-Device Protection Active")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .build();
    }
}
