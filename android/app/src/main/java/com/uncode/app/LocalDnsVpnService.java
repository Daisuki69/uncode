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

    // Educational & Enterprise SafeSearch VIPs (forces strict filtering at DNS socket level)
    // forcesafesearch.google.com -> 216.239.38.120 (Google Search & YouTube Restricted Mode)
    private static final byte[] GOOGLE_SAFE_VIP = new byte[] { (byte) 216, (byte) 239, 38, 120 };
    // strict.bing.com -> 204.79.197.220
    private static final byte[] BING_SAFE_VIP = new byte[] { (byte) 204, 79, (byte) 197, (byte) 220 };
    // safe.duckduckgo.com -> 52.142.124.215
    private static final byte[] DUCKDUCKGO_SAFE_VIP = new byte[] { 52, (byte) 142, 124, (byte) 215 };

    public static volatile boolean isRunning = false;

    private ParcelFileDescriptor vpnInterface = null;
    private Thread workerThread = null;
    private ExecutorService dnsExecutor = null;
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

    // Comprehensive Web-Based Games Database (250+ Domains)
    private static final Set<String> WEB_GAMING_DOMAINS = new HashSet<>(Arrays.asList(
        // A. Mega Portals & Aggregators
        "poki.com", "poki-gdn.com", "poki.cz", "poki.nl", "poki.com.br",
        "crazygames.com", "crazygames.co.uk", "crazygames.fr", "crazygames.io",
        "coolmathgames.com", "coolmath-games.com", "coolmath.com",
        "kongregate.com", "kongregate.io",
        "armorgames.com", "armorgamesonline.com",
        "newgrounds.com", "ungrounded.net",
        "y8.com", "y8games.com", "id.net",
        "friv.com", "friv5.me", "friv.cm", "friv.today", "friv.cool", "frivclassic.com", "friv-2017.com",
        "miniplay.com", "minijuegos.com",
        "addictinggames.com", "silvergames.com",
        "kizi.com", "kizi10.org",
        "gamepix.com", "lagged.com",
        "agame.com", "a-game.com", "gamesgames.com",
        "snokido.com", "snokido.fr", "snokido.net",
        "kbhgames.com", "playhop.com", "1001games.com",
        "twoplayergames.org", "2playergames.com", "pomu.com", "paisdelosjuegos.com",
        "games2girls.com", "girlsgogames.com", "mousebreaker.com", "stickpage.com",
        "speele.nl", "jetztspielen.de", "gry.pl", "jeuxjeuxjeux.fr",
        "arkadium.com", "gameforge.com", "gameflare.com", "gamepost.com",
        "titotu.io", "kevin.games", "zone.msn.com", "plays.org", "bubbleshooter.net",

        // B. Viral .IO & Multiplayer Arena Games
        "slither.io", "slitherio.org", "agar.io", "agar.pro", "agariogame.club",
        "diep.io", "krunker.io", "yendis.ch",
        "1v1.lol", "1v1.school", "justfall.lol",
        "paper.io", "paper-io.com", "paperio2.com",
        "hole.io", "hole-io.com",
        "surviv.io", "survev.io", "suroi.io",
        "skribbl.io", "gartic.io", "garticphone.com", "drawasaurus.org",
        "shellshock.io", "eggcombat.com", "shellshockers.io",
        "deeeep.io", "bloxd.io", "voxiom.io", "smashkarts.io",
        "ev.io", "zombs.io", "zombsroyale.io", "starve.io", "moomoo.io",
        "narrow.one", "venge.io", "bonk.io", "bonk2.io",
        "wings.io", "brutal.io", "splix.io", "lordz.io",
        "flyordie.io", "evojaws.io", "evoworld.io",
        "digdig.io", "yohoho.io", "taming.io", "betrayal.io",
        "battledudes.io", "lolbeans.io", "warbrokers.io",
        "curvefever.pro", "curvefever.com", "littlebigsnake.com", "arrow.io",
        "iogames.space", "iogames.onl", "io-games.io",

        // C. Cloud Gaming & Web APK Streaming Backdoors
        "now.gg", "nowgg.me", "nowgg.io",
        "play.geforcenow.com", "geforcenow.com",
        "luna.amazon.com",
        "boosteroid.com", "cloud.boosteroid.com",
        "shadow.tech", "vortex.gg", "airgpu.com",

        // D. Indie Web Runtimes & CDNs
        "itch.zone", "itch.io", "gamejolt.com", "gx.games",
        "simmer.io", "lexaloffle.com", "flowlab.io", "arcade.construct.net",

        // E. Web Emulators & Retro Gaming
        "emulatoronline.com", "retrogames.cc", "playretrogames.com", "vimm.net",
        "emupedia.net", "emupedia.org", "afterplay.io", "eclipseemu.me",
        "webretro.org", "game-oldies.com", "ssega.com", "playminigames.net",
        "playclassic.games", "dosgames.com", "playdosgames.com",
        "online-emulators.com", "retrogames.onl", "myabandonware.com",
        "consoleroms.com", "archaic-bingo.com", "wowroms.com", "freeroms.com",

        // F. Unblocked Games Dedicated Networks & Mirrors
        "unblocked-games.com", "unblockedgames66.com", "unblockedgames66plus.com", "unblockedgames66ez.com",
        "unblockedgames76.com", "unblockedgames77.com", "unblockedgames99.com",
        "unblockedgames500.com", "unblockedgames119.com", "unblockedgames24h.com",
        "classroom6x.com", "classroom-6x.org",
        "slope-game.com", "slopeunblocked.org", "slopegame.online",
        "hoodamath.com", "mathplayground.com", "abcya.com", "primarygames.com",
        "freeonlinegames.com", "b-games.com", "unblocked-games-76.com",
        "unblockedgame76.com", "unblockedgamesworld.com", "unblockedgamespod.com",
        "unblockedgames.me", "unblocked-games-s.com", "unblockedgame.io", "unblockedgamesfree.com",

        // G. Casual, Board, Puzzle & Incremental Games
        "chess.com", "lichess.org", "chess24.com", "chessbomb.com",
        "geoguessr.com", "worldle.teuteuf.fr", "globle-game.com", "geoguess.games", "city-guesser.com",
        "2048game.com", "play2048.co", "2048.io",
        "sudoku.com", "websudoku.com", "nonograms.org",
        "wordlewebsite.com", "wordle.org", "quordle.com", "octordle.com", "sedecordle.com",
        "solitaired.com", "cardgames.io", "solitaireparadise.com", "247solitaire.com",
        "sporcle.com", "jetpunk.com", "tetr.io", "jstris.jezevec10.com", "cookieclicker.ee"
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
            builder.addRoute(VPN_DNS_SERVER_IP, 32); // ONLY capture traffic to our virtual DNS IP!

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
        boolean blockWebGames = prefs.getBoolean("block_web_games", true);
        boolean enforceSafeSearch = prefs.getBoolean("enforce_safesearch", true);

        // 1. Check if domain is blocked (distractions, unblocked web games, youtube if disallowed)
        boolean isBlacklisted = isDomainBlocked(lowerDomain, allowYoutube, blockWebGames);

        if (isBlacklisted) {
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
                Log.d(TAG, "Sinkholed DNS query (NXDOMAIN): " + queryDomain);
            }
            return;
        }

        // 2. School-Grade SafeSearch DNS VIP Routing (Google, Bing, DuckDuckGo, YouTube)
        if (enforceSafeSearch) {
            byte[] safeIp = getSafeSearchVip(lowerDomain);
            if (safeIp != null) {
                int qType = parseDnsQuestionType(packet, dnsOffset, packet.length);
                if (qType == 1) { // Type A (IPv4)
                    byte[] responseDns = buildARecordResponse(packet, dnsOffset, dnsLength, safeIp);
                    if (responseDns != null) {
                        byte[] responseIpPacket = buildUdpIpPacket(
                            packet, ipHeaderLength, dstPort, srcPort, responseDns
                        );
                        synchronized (out) {
                            try {
                                out.write(responseIpPacket);
                            } catch (Exception ignore) {}
                        }
                        Log.d(TAG, "Enforced SafeSearch VIP for: " + queryDomain);
                        return;
                    }
                } else if (qType == 28) { // Type AAAA (IPv6) -> return NODATA so client immediately resolves IPv4 SafeSearch VIP
                    byte[] responseDns = buildNoDataResponse(packet, dnsOffset, dnsLength);
                    if (responseDns != null) {
                        byte[] responseIpPacket = buildUdpIpPacket(
                            packet, ipHeaderLength, dstPort, srcPort, responseDns
                        );
                        synchronized (out) {
                            try {
                                out.write(responseIpPacket);
                            } catch (Exception ignore) {}
                        }
                        return;
                    }
                }
            }
        }

        // 3. Forward query to selected upstream DNS resolver (CleanBrowsing School / Cloudflare / AdGuard / System)
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

    private boolean isDomainBlocked(String domain, boolean allowYoutube, boolean blockWebGames) {
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

        // Web-based games (250+ curated domains and dynamic CDN/mirror patterns)
        if (blockWebGames) {
            for (String g : WEB_GAMING_DOMAINS) {
                if (lower.equals(g) || lower.endsWith("." + g)) {
                    return true;
                }
            }
            if (lower.endsWith(".itch.zone") || lower.endsWith(".poki-gdn.com")) {
                return true;
            }
            if (lower.contains("unblocked") && (lower.contains("game") || lower.contains("66") || lower.contains("76") || lower.contains("slope"))) {
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
            ByteBuffer buf = ByteBuffer.allocate(dnsLength);
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

            // 4. Copy original Question Section
            int qLen = dnsLength - 12;
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
            int respLen = dnsLength + 16;
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

            // 4. Copy original Question Section
            int qLen = dnsLength - 12;
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
            ByteBuffer buf = ByteBuffer.allocate(dnsLength);
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

            // 4. Copy original Question Section
            int qLen = dnsLength - 12;
            buf.put(packet, dnsOffset + 12, qLen);

            return Arrays.copyOf(buf.array(), buf.position());
        } catch (Exception e) {
            return null;
        }
    }

    private byte[] getSafeSearchVip(String domain) {
        if (domain == null || domain.isEmpty()) return null;

        if (isGoogleSearchDomain(domain)) {
            return GOOGLE_SAFE_VIP;
        }
        if (domain.equals("bing.com") || domain.equals("www.bing.com") || (domain.endsWith(".bing.com") && (domain.startsWith("www.") || domain.startsWith("cn.")))) {
            return BING_SAFE_VIP;
        }
        if (domain.equals("duckduckgo.com") || domain.equals("www.duckduckgo.com")) {
            return DUCKDUCKGO_SAFE_VIP;
        }
        if (domain.equals("youtube.com") || domain.equals("www.youtube.com") || domain.equals("m.youtube.com") || domain.equals("youtubei.googleapis.com")) {
            return GOOGLE_SAFE_VIP;
        }
        return null;
    }

    private boolean isGoogleSearchDomain(String domain) {
        if (domain.equals("google.com") || domain.equals("www.google.com")) return true;
        if (domain.contains("google.")) {
            // Keep critical academic tools and Google services unrestricted
            if (domain.startsWith("drive.") || domain.startsWith("docs.") || domain.startsWith("mail.") ||
                domain.startsWith("accounts.") || domain.startsWith("play.") || domain.startsWith("fonts.") ||
                domain.startsWith("meet.") || domain.startsWith("classroom.") || domain.startsWith("calendar.") ||
                domain.startsWith("admin.") || domain.startsWith("cloud.")) {
                return false;
            }
            return domain.startsWith("www.google.") || domain.startsWith("google.");
        }
        return false;
    }

    private List<InetAddress> getUpstreamDnsServers(SharedPreferences prefs) {
        List<InetAddress> servers = new ArrayList<>();
        String profile = prefs != null ? prefs.getString("dns_filter_profile", "cleanbrowsing") : "cleanbrowsing";

        if ("cleanbrowsing".equalsIgnoreCase(profile)) {
            // CleanBrowsing Family / School Filter (Blocks Adult, Malicious, Proxies, and enforces SafeSearch)
            addDnsServer(servers, "185.228.168.168");
            addDnsServer(servers, "185.228.169.168");
            // Failover redundancy to Cloudflare Family & AdGuard Family
            addDnsServer(servers, "1.1.1.3");
            addDnsServer(servers, "94.140.14.15");
        } else if ("cloudflare_family".equalsIgnoreCase(profile)) {
            // Cloudflare 1.1.1.3 for Families (Malware & Adult Content Blocked)
            addDnsServer(servers, "1.1.1.3");
            addDnsServer(servers, "1.0.0.3");
            addDnsServer(servers, "185.228.168.168");
        } else if ("adguard_family".equalsIgnoreCase(profile)) {
            // AdGuard Family Protection (Adult content & tracking blocked)
            addDnsServer(servers, "94.140.14.15");
            addDnsServer(servers, "94.140.15.16");
            addDnsServer(servers, "185.228.168.168");
        } else {
            // Standard Public DNS (Active network DHCP DNS + Cloudflare 1.1.1.1 + Google 8.8.8.8)
            try {
                ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                if (cm != null) {
                    Network activeNetwork = cm.getActiveNetwork();
                    if (activeNetwork != null) {
                        LinkProperties lp = cm.getLinkProperties(activeNetwork);
                        if (lp != null) {
                            for (InetAddress dns : lp.getDnsServers()) {
                                if (dns instanceof Inet4Address) {
                                    String host = dns.getHostAddress();
                                    if (!host.equals(VPN_INTERFACE_IP) && !host.equals(VPN_DNS_SERVER_IP)) {
                                        servers.add(dns);
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception ignore) {}

            addDnsServer(servers, "1.1.1.1");
            addDnsServer(servers, "8.8.8.8");
            addDnsServer(servers, "9.9.9.9");
            addDnsServer(servers, "1.0.0.1");
        }

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

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String profile = prefs != null ? prefs.getString("dns_filter_profile", "cleanbrowsing") : "cleanbrowsing";
        boolean safeSearch = prefs != null && prefs.getBoolean("enforce_safesearch", true);

        String profileName = "CleanBrowsing School Filter";
        if ("cloudflare_family".equalsIgnoreCase(profile)) profileName = "Cloudflare for Families";
        else if ("adguard_family".equalsIgnoreCase(profile)) profileName = "AdGuard Family";
        else if ("standard".equalsIgnoreCase(profile)) profileName = "Standard DNS";

        String subText = profileName + (safeSearch ? " • SafeSearch Active" : "");

        return builder
            .setContentTitle("QIEZKA Web Guard Active")
            .setContentText(subText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .build();
    }
}
