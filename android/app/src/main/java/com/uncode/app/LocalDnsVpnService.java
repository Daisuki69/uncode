package com.uncode.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

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

    private static final String VPN_ADDRESS = "10.111.222.1";
    private static final String UPSTREAM_DNS_PRIMARY = "1.1.1.1";
    private static final String UPSTREAM_DNS_SECONDARY = "8.8.8.8";

    public static volatile boolean isRunning = false;

    private ParcelFileDescriptor vpnInterface = null;
    private Thread workerThread = null;
    private volatile boolean shouldRun = false;

    // Distracting domains sinkholed at the socket/DNS level
    private static final Set<String> BLACKLISTED_DOMAINS = new HashSet<>(Arrays.asList(
        "tiktok.com", "byteoversea.com", "ibytedtos.com", "musical.ly",
        "instagram.com", "cdninstagram.com",
        "facebook.com", "fb.com", "fbcdn.net", "fbsbx.com",
        "twitter.com", "x.com", "twimg.com", "t.co",
        "reddit.com", "redd.it", "redditmedia.com", "redditstatic.com",
        "threads.net",
        "snapchat.com", "sc-cdn.net",
        "discord.com", "discordapp.com", "discordapp.net",
        "twitch.tv", "ttvnw.net",
        "netflix.com", "nflxvideo.net", "nflximg.net",
        "disneyplus.com", "hulu.com", "primevideo.com",
        "webtoons.com", "mangadex.org", "mangakakalot.com", "bilibili.tv", "bilibili.com",
        "roblox.com", "rbxcdn.com",
        "9gag.com", "pinterest.com", "tumblr.com"
    ));

    private static final Set<String> YOUTUBE_DOMAINS = new HashSet<>(Arrays.asList(
        "youtube.com", "youtu.be", "ytimg.com", "googlevideo.com", "youtube-nocookie.com"
    ));

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

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();
        Notification notif = buildNotification();
        startForeground(NOTIF_ID, notif);

        startDnsSinkhole();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        stopDnsSinkhole();
        super.onDestroy();
    }

    private synchronized void startDnsSinkhole() {
        if (workerThread != null && workerThread.isAlive()) return;

        try {
            Builder builder = new Builder();
            builder.setSession("QIEZKA DNS Guard");
            builder.setMtu(1500);

            builder.addAddress(VPN_ADDRESS, 32);
            builder.addDnsServer(VPN_ADDRESS);
            builder.addRoute(VPN_ADDRESS, 32); // ONLY capture traffic to our virtual DNS IP!

            builder.setBlocking(true);

            vpnInterface = builder.establish();
            if (vpnInterface == null) {
                Log.e(TAG, "Failed to establish VPN interface (prepare() may be required)");
                stopSelf();
                return;
            }

            shouldRun = true;
            isRunning = true;

            workerThread = new Thread(new DnsWorker(), "QiezkaDnsWorker");
            workerThread.start();
            Log.i(TAG, "LocalDnsVpnService started successfully on " + VPN_ADDRESS);
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
            byte[] packet = new byte[32767];

            SharedPreferences prefs = getSharedPreferences("UncodeLockPrefs", Context.MODE_PRIVATE);

            while (shouldRun && !Thread.currentThread().isInterrupted()) {
                try {
                    int length = in.read(packet);
                    if (length <= 0) continue;

                    // IPv4 Header verification
                    if (length < 28) continue;
                    int version = (packet[0] >> 4) & 0x0F;
                    if (version != 4) continue; // IPv4 only

                    int protocol = packet[9] & 0xFF;
                    if (protocol != 17) continue; // UDP only (17)

                    int ipHeaderLength = (packet[0] & 0x0F) * 4;
                    if (length < ipHeaderLength + 8) continue;

                    int srcPort = ((packet[ipHeaderLength] & 0xFF) << 8) | (packet[ipHeaderLength + 1] & 0xFF);
                    int dstPort = ((packet[ipHeaderLength + 2] & 0xFF) << 8) | (packet[ipHeaderLength + 3] & 0xFF);

                    if (dstPort != 53) continue; // Only handle DNS queries

                    int dnsOffset = ipHeaderLength + 8;
                    int dnsLength = length - dnsOffset;
                    if (dnsLength < 12) continue; // Minimum DNS header size

                    // Parse DNS QNAME
                    String queryDomain = parseDnsQuestionDomain(packet, dnsOffset, length);
                    if (queryDomain == null) queryDomain = "";

                    boolean allowYoutube = prefs.getBoolean("allow_youtube", false);
                    boolean isBlacklisted = isDomainBlocked(queryDomain, allowYoutube);

                    if (isBlacklisted) {
                        // Synthesize local sinkhole 0.0.0.0 DNS response
                        byte[] responseDns = buildSinkholeResponse(packet, dnsOffset, dnsLength);
                        if (responseDns != null) {
                            byte[] responseIpPacket = buildUdpIpPacket(
                                packet, ipHeaderLength, dstPort, srcPort, responseDns
                            );
                            out.write(responseIpPacket);
                            Log.d(TAG, "Sinkholed DNS query: " + queryDomain);
                        }
                    } else {
                        // Forward query to upstream DNS using a protected socket
                        byte[] dnsPayload = Arrays.copyOfRange(packet, dnsOffset, dnsOffset + dnsLength);
                        byte[] upstreamResponse = forwardToUpstreamDns(dnsPayload);
                        if (upstreamResponse != null) {
                            byte[] responseIpPacket = buildUdpIpPacket(
                                packet, ipHeaderLength, dstPort, srcPort, upstreamResponse
                            );
                            out.write(responseIpPacket);
                        }
                    }
                } catch (Exception e) {
                    if (!shouldRun) break;
                }
            }
        }
    }

    private boolean isDomainBlocked(String domain, boolean allowYoutube) {
        if (domain == null || domain.isEmpty()) return false;
        String lower = domain.toLowerCase(Locale.US);

        // YouTube is blocked by default at the DNS level unless allow_youtube is explicitly enabled.
        // When allow_youtube is enabled, DNS queries resolve normally so the browser can load educational content,
        // while the Accessibility Service URL Guard strictly blocks YouTube Shorts (/shorts/*).
        if (!allowYoutube) {
            for (String yt : YOUTUBE_DOMAINS) {
                if (lower.equals(yt) || lower.endsWith("." + yt)) return true;
            }
        }

        // Blacklisted distraction domains
        for (String b : BLACKLISTED_DOMAINS) {
            if (lower.equals(b) || lower.endsWith("." + b)) {
                return true;
            }
        }
        return false;
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

    private byte[] buildSinkholeResponse(byte[] packet, int dnsOffset, int dnsLength) {
        try {
            ByteBuffer buf = ByteBuffer.allocate(dnsLength + 16);
            // 1. Transaction ID (2 bytes)
            buf.put(packet[dnsOffset]);
            buf.put(packet[dnsOffset + 1]);

            // 2. Flags: Standard query response, No error (0x8180)
            buf.put((byte) 0x81);
            buf.put((byte) 0x80);

            // 3. QDCOUNT = 1
            buf.put((byte) 0x00);
            buf.put((byte) 0x01);

            // 4. ANCOUNT = 1
            buf.put((byte) 0x00);
            buf.put((byte) 0x01);

            // 5. NSCOUNT = 0, ARCOUNT = 0
            buf.putShort((short) 0);
            buf.putShort((short) 0);

            // 6. Copy original Question Section
            int qLen = dnsLength - 12;
            buf.put(packet, dnsOffset + 12, qLen);

            // 7. Answer Section (Pointer to name at offset 12: 0xC00C)
            buf.put((byte) 0xC0);
            buf.put((byte) 0x0C);
            buf.putShort((short) 1);     // TYPE: A (IPv4)
            buf.putShort((short) 1);     // CLASS: IN
            buf.putInt(60);              // TTL: 60s
            buf.putShort((short) 4);     // RDLENGTH: 4 bytes
            buf.put(new byte[]{0, 0, 0, 0}); // IP: 0.0.0.0

            return Arrays.copyOf(buf.array(), buf.position());
        } catch (Exception e) {
            return null;
        }
    }

    private byte[] forwardToUpstreamDns(byte[] queryPayload) {
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket();
            protect(socket); // ESSENTIAL: Exclude this socket from the VPN TUN interface!
            socket.setSoTimeout(2500);

            InetAddress upstream = InetAddress.getByName(UPSTREAM_DNS_PRIMARY);
            DatagramPacket outPacket = new DatagramPacket(queryPayload, queryPayload.length, upstream, 53);
            socket.send(outPacket);

            byte[] recvBuf = new byte[2048];
            DatagramPacket inPacket = new DatagramPacket(recvBuf, recvBuf.length);
            socket.receive(inPacket);

            return Arrays.copyOf(inPacket.getData(), inPacket.getLength());
        } catch (Exception e1) {
            // Fallback to secondary upstream DNS (8.8.8.8)
            try {
                if (socket != null && !socket.isClosed()) {
                    InetAddress upstreamSec = InetAddress.getByName(UPSTREAM_DNS_SECONDARY);
                    DatagramPacket outPacket = new DatagramPacket(queryPayload, queryPayload.length, upstreamSec, 53);
                    socket.send(outPacket);

                    byte[] recvBuf = new byte[2048];
                    DatagramPacket inPacket = new DatagramPacket(recvBuf, recvBuf.length);
                    socket.receive(inPacket);
                    return Arrays.copyOf(inPacket.getData(), inPacket.getLength());
                }
            } catch (Exception e2) {
                // Secondary failed as well
            }
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
            .setContentText("DNS Sinkhole active — blocking distracting websites")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .build();
    }
}
