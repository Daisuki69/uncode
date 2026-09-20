package com.uncode.app;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * On-device local app classification engine.
 *
 * Implements a 5-layer offline rule hierarchy using Android ApplicationInfo metadata
 * (declared categories, application labels, and semantic keywords) to dynamically
 * distinguish legitimate study/productivity tools from hostile or distracting apps.
 *
 * 100% offline — preserves the client-side BYOK architecture with zero server dependencies
 * and zero Gemini API token overhead.
 */
public final class AppClassifier {

    private static final String TAG = "AppClassifier";

    // Standard Android ApplicationInfo category constants (API 26+)
    public static final int CATEGORY_UNDEFINED = -1;
    public static final int CATEGORY_GAME = 0;
    public static final int CATEGORY_AUDIO = 1;
    public static final int CATEGORY_VIDEO = 2;
    public static final int CATEGORY_IMAGE = 3;
    public static final int CATEGORY_SOCIAL = 4;
    public static final int CATEGORY_NEWS = 5;
    public static final int CATEGORY_MAPS = 6;
    public static final int CATEGORY_PRODUCTIVITY = 7;
    public static final int CATEGORY_ACCESSIBILITY = 8; // API 31+

    /**
     * Thread-safe cache mapping packageName -> isBlocked (true = BLOCK, false = ALLOW).
     * Avoids querying PackageManager repeatedly during frequent accessibility events.
     */
    private static final Map<String, Boolean> decisionCache = new ConcurrentHashMap<>();

    /**
     * Essential telephony, in-call, SIM Toolkit (STK), MMS, carrier and emergency packages
     * that must never be blocked during lockdown to ensure emergency calling, SIM management,
     * carrier push messages, and critical communication remain operational.
     */
    public static final Set<String> TELEPHONY_PACKAGES = new HashSet<>(Arrays.asList(
        // Core Telephony, In-Call UI & Telecom
        "com.android.phone",
        "com.android.server.telecom",
        "com.android.incallui",
        "com.google.android.dialer",
        "com.samsung.android.dialer",
        "com.samsung.android.incallui",
        "com.samsung.android.app.telephonyui",
        "com.sec.android.app.servicemodeapp",
        "com.miui.telephonyui",
        "com.oppo.telephonyui",
        "com.coloros.telephonyui",
        "com.vivo.telephonyui",
        "com.asus.telephonyui",

        // SIM Card Toolkit (STK) & SIM Application Services (AOSP, Samsung, MTK, Transsion, Qualcomm, etc.)
        "com.android.stk",                         // AOSP SIM Toolkit
        "com.android.stk2",                        // AOSP Dual-SIM STK slot 2
        "com.google.android.stk",                  // Google SIM Toolkit
        "com.sec.android.app.simappdialog",        // Samsung STK dialog & Flash SMS popup (Critical for Globe/Smart)
        "com.sec.android.app.simsetting",          // Samsung SIM card manager
        "com.sec.android.app.simsettings",         // Samsung SIM settings variant
        "com.mediatek.stk",                        // MediaTek SIM Toolkit (Infinix, Tecno, etc.)
        "com.mediatek.stk2",                       // MediaTek SIM Toolkit slot 2
        "com.mediatek.simprocessor",               // MediaTek SIM Processor
        "com.mediatek.engineermode",               // MediaTek Engineer Mode
        "com.transsion.simtoolkit",                // Transsion SIM Toolkit (Infinix, Tecno, Itel)
        "com.transsion.stk",                       // Transsion STK
        "com.qualcomm.qti.simcontacts",            // Qualcomm SIM Contacts
        "com.qualcomm.qti.uim",                    // Qualcomm User Identity Module
        "com.qualcomm.qti.modemtestmode",          // Qualcomm Modem test
        "com.vivo.stk",                            // Vivo SIM Toolkit
        "com.coloros.simsettings",                 // Oppo / Realme SIM Settings
        "com.oppo.stk",                            // Oppo STK
        "com.coloros.stk",                         // ColorOS STK
        "com.huawei.stk",                          // Huawei SIM Toolkit
        "com.motorola.stk",                        // Motorola STK
        "com.zte.stk",                             // ZTE STK
        "com.oneplus.stk",                         // OnePlus STK

        // MMS & Native Carrier Messaging Services
        "com.android.mms",                         // AOSP Messaging / MMS
        "com.android.mms.service",                 // AOSP MMS Service
        "com.google.android.apps.messaging",       // Google Messages / RCS / Class 0 Flash SMS (Default on Infinix/Pixel/Samsung)
        "com.samsung.android.messaging",           // Samsung Messages / MMS
        "com.transsion.mms",                       // Transsion MMS / SMS
        "com.coloros.mms",                         // ColorOS / Oppo MMS
        "com.vivo.mms",                            // Vivo MMS
        "com.huawei.message",                      // Huawei Messaging
        "com.motorola.messaging",                  // Motorola Messaging
        "com.asus.message",                        // ASUS Messaging
        "com.zte.mms",                             // ZTE MMS

        // Cell Broadcast, Wireless Emergency Alerts (WEA) & Flash Alerts
        "com.android.cellbroadcastreceiver",       // AOSP Cell Broadcast
        "com.android.cellbroadcastreceiver.module",// Android Mainline Cell Broadcast Module
        "com.android.cellbroadcastservice",        // AOSP Cell Broadcast Service
        "com.google.android.cellbroadcastreceiver",// Google Emergency Alerts
        "com.google.android.cellbroadcastservice", // Google Cell Broadcast Service
        "com.mediatek.cellbroadcastreceiver",      // MediaTek Cell Broadcast
        "com.transsion.cellbroadcastreceiver",     // Transsion Cell Broadcast
        "com.oplus.cellbroadcastreceiver",         // Oppo / Realme Cell Broadcast
        "com.qualcomm.qti.cellbroadcastreceiver",  // Qualcomm Cell Broadcast
        "com.sec.android.app.wlantest",            // Samsung carrier wireless test
        "com.sec.android.app.safetyinformation",   // Samsung Safety / Emergency Information

        // Carrier Default Apps, Carrier Configuration & RCS/IMS
        "com.android.carrierdefaultapp",           // Android Carrier Default App
        "com.android.carrierconfig",               // Carrier Config
        "com.google.android.carrierconfig",        // Google Carrier Config
        "com.google.android.ims",                  // Google Carrier Services / RCS
        "com.samsung.android.ims",                 // Samsung IMS
        "com.sec.android.carrier.carrierwifi",     // Samsung Carrier Wi-Fi
        "com.shannon.imsservice",                  // Samsung Exynos IMS Service
        "com.mediatek.ims",                        // MediaTek IMS

        // Philippine Carrier Ecosystem (Globe Telecom, Smart Communications, DITO)
        "ph.com.globe",                            // Globe Telecom Carrier Services
        "ph.com.globe.globeathome",                // Globe at Home
        "ph.com.globe.globeonesuperapp",           // GlobeOne
        "com.globe.services",                      // Globe Services / SIM Menu
        "com.globe.telecom",                       // Globe Telecom
        "ph.com.smart",                            // Smart Communications
        "com.smart.services",                      // Smart Services / SIM Menu
        "ph.dito.telecommunity",                   // DITO Telecommunity
        "com.dito.services"                        // DITO Services
    ));

    public static boolean isSimOrCarrierService(String pkg, String appLabel) {
        return LockAccessibilityService.isSimOrCarrierService(pkg, appLabel);
    }

    public static boolean isSimOrCarrierService(String pkg) {
        return LockAccessibilityService.isSimOrCarrierService(pkg, null);
    }

    /**
     * Legitimate App Store and Package Installer packages allowed during lockdown
     * and consequence modes for installation and maintenance (Milestone 17).
     */
    public static final Set<String> APP_STORE_AND_INSTALLER_PACKAGES = new HashSet<>(Arrays.asList(
        "com.android.vending",                     // Google Play Store
        "com.google.android.feedback",             // Play Store feedback
        "com.google.android.gms",                  // Google Play Services
        "com.google.android.packageinstaller",     // Android Package Installer (Allowed)
        "com.android.packageinstaller",            // AOSP Package Installer (Allowed)
        "com.sec.android.app.samsungapps"          // Samsung Galaxy Store
    ));

    public static boolean isInstallerOrStoreApp(String pkg) {
        if (pkg == null) return false;
        return APP_STORE_AND_INSTALLER_PACKAGES.contains(pkg) || pkg.toLowerCase(Locale.ROOT).contains("packageinstaller");
    }

    /**
     * Recognized Direct Messaging & Communication applications.
     * Unlike social media feeds, messaging apps can be whitelisted by users in Allowed Apps.
     */
    public static final Set<String> KNOWN_MESSAGING_PACKAGES = new HashSet<>(Arrays.asList(
        "org.telegram.messenger",
        "org.telegram.plus",
        "org.thunderdog.challegram",
        "nekox.messenger",
        "org.forkgram.messenger",
        "org.telegram.BApplication",
        "com.facebook.orca",
        "com.facebook.mlite",
        "com.whatsapp",
        "com.whatsapp.w4b",
        "org.thoughtcrime.securesms",
        "com.viber.voip",
        "jp.naver.line.android",
        "com.tencent.mm",
        "com.discord",
        "com.Slack",
        "com.google.android.apps.messaging",
        "com.samsung.android.messaging",
        "com.android.mms"
    ));

    public static boolean isMessagingApp(String pkg, String appLabel) {
        if (pkg == null) return false;
        String lowerPkg = pkg.toLowerCase(Locale.ROOT).trim();
        if (KNOWN_MESSAGING_PACKAGES.contains(lowerPkg)) {
            return true;
        }
        // Exclude social network feeds explicitly
        if (lowerPkg.contains("facebook.katana") || lowerPkg.contains("facebook.lite") ||
            lowerPkg.contains("instagram") || lowerPkg.contains("twitter") ||
            lowerPkg.contains("reddit") || lowerPkg.contains("tiktok") ||
            lowerPkg.contains("musically")) {
            return false;
        }
        if (lowerPkg.contains("telegram") || lowerPkg.contains("challegram") ||
            lowerPkg.contains("nekogram") || lowerPkg.contains("forkgram") ||
            lowerPkg.contains("bgram") || lowerPkg.contains("messenger") ||
            lowerPkg.contains("whatsapp") || lowerPkg.contains(".signal") ||
            lowerPkg.contains("viber") || lowerPkg.contains("wechat") ||
            lowerPkg.contains(".line") || lowerPkg.contains("orca")) {
            return true;
        }
        if (appLabel != null && !appLabel.trim().isEmpty()) {
            String lowerLabel = appLabel.toLowerCase(Locale.ROOT).trim();
            if (lowerLabel.contains("facebook") || lowerLabel.contains("instagram") ||
                lowerLabel.contains("twitter") || lowerLabel.contains("reddit") ||
                lowerLabel.contains("tiktok")) {
                return false;
            }
            if (lowerLabel.contains("telegram") || lowerLabel.contains("messenger") ||
                lowerLabel.contains("whatsapp") || lowerLabel.contains("signal") ||
                lowerLabel.contains("viber") || lowerLabel.contains("wechat") ||
                lowerLabel.contains("challegram") || lowerLabel.contains("nekogram")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Remote Desktop, Screen Sharing, and Screen Mirroring packages.
     * Intercepted to prevent using desktop streaming or Kali NetHunter VNC/KeX as a bypass workaround.
     */
    public static final Set<String> REMOTE_DESKTOP_PACKAGES = new HashSet<>(Arrays.asList(
        "com.teamviewer.teamviewer.market.mobile",
        "com.teamviewer.quicksupport.market",
        "com.teamviewer.host.market",
        "com.anydesk.anydeskandroid",
        "com.anydesk.adcontrol.ad1",
        "com.carriez.rustdesk",
        "com.google.chromeremotedesktop",
        "com.microsoft.rdc.android",
        "com.splashtop.remote.pad.v2",
        "com.splashtop.remote",
        "com.realvnc.viewer.android",
        "tv.parsec.client",
        "com.limelight",
        "com.valvesoftware.steamlink",
        "com.sand.airdroid",
        "com.sand.airmirror",
        "com.apowersoft.mirror",
        "com.koushikdutta.vysor",
        "info.dvkr.screenstream",
        // Kali NetHunter & Magisk Screen Share / VNC
        "com.offsec.nethunter",
        "com.offsec.nethunter.kex",
        "com.offsec.nhkex",
        "net.christianbeier.droidvnc_ng",
        "com.termux.x11"
    ));

    public static boolean isRemoteDesktopOrScreenShare(String pkg, String appLabel) {
        if (pkg == null) return false;
        String lowerPkg = pkg.toLowerCase(Locale.ROOT).trim();
        if (REMOTE_DESKTOP_PACKAGES.contains(lowerPkg)) {
            return true;
        }
        if (lowerPkg.contains("teamviewer") || lowerPkg.contains("anydesk") ||
            lowerPkg.contains("rustdesk") || lowerPkg.contains("remotedesktop") ||
            lowerPkg.contains("screenmirror") || lowerPkg.contains("screenshare") ||
            lowerPkg.contains("screenstream") || lowerPkg.contains("splashtop") ||
            lowerPkg.contains(".parsec.") || lowerPkg.contains("airdroid") ||
            lowerPkg.contains("airmirror") || lowerPkg.contains("apowermirror") ||
            lowerPkg.contains("nethunter") || lowerPkg.contains("nhkex") ||
            lowerPkg.contains("droidvnc") || lowerPkg.contains("vncviewer") ||
            lowerPkg.contains(".vnc.")) {
            return true;
        }
        if (appLabel != null && !appLabel.trim().isEmpty()) {
            String lowerLabel = appLabel.toLowerCase(Locale.ROOT).trim();
            if (lowerLabel.contains("teamviewer") || lowerLabel.contains("anydesk") ||
                lowerLabel.contains("rustdesk") || lowerLabel.contains("remote desktop") ||
                lowerLabel.contains("screen share") || lowerLabel.contains("screen mirror") ||
                lowerLabel.contains("screen stream") || lowerLabel.contains("splashtop") ||
                lowerLabel.contains("parsec") || lowerLabel.contains("moonlight") ||
                lowerLabel.contains("steam link") || lowerLabel.contains("vysor") ||
                lowerLabel.contains("airdroid") || lowerLabel.contains("nethunter") ||
                lowerLabel.contains("kex") || lowerLabel.contains("droidvnc") ||
                lowerLabel.contains("vnc viewer")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Negative Distraction Keywords (Milestone 17: Widened Recognition Engine).
     * If an app's label contains any of these terms, it is strictly BLOCKED,
     * even if the APK falsely declares itself as CATEGORY_PRODUCTIVITY.
     */
    private static final String[] NEGATIVE_LABEL_KEYWORDS = {
        // Gaming & Gambling (Widened Recognition)
        "game", "games", "gaming", "arcade", "puzzle", "simulator", "tycoon", "rpg", "mmorpg", "mmo",
        "fps", "tps", "battle royale", "racing", "dungeon", "quest", "brawl", "clash", "casino", "poker",
        "slots", "jackpot", "betting", "lottery", "roulette", "blackjack", "solitaire", "idle", "clicker",
        "hypercasual", "survivor", "arena", "runner", "crafting", "tower defense", "mahjong", "bingo",
        "gacha", "otome", "visual novel", "tamagotchi", "virtual pet", "block craft", "zombie", "shooter",
        "sniper", "defense", "multiplayer", "board game", "card game", "minigame",

        // Remote Desktop, Screen Share & VNC Workarounds
        "teamviewer", "anydesk", "rustdesk", "remote desktop", "screen share", "screen mirror",
        "screen stream", "splashtop", "parsec", "moonlight", "steam link", "vysor", "airdroid",
        "airmirror", "apowermirror", "nethunter", "kex", "droidvnc", "vnc viewer",

        // Parallel Space, Virtual Containers & App Cloners (Milestone 17)
        "parallel space", "dual space", "dual app", "multi space", "multiple accounts", "2accounts",
        "clone app", "app cloner", "virtual android", "virtual space", "second space", "dual clone",
        "super clone", "do multiple", "water clone", "island", "shelter", "vmos", "vphonegaga", "f1 vm",
        "f1vm", "x8 sandbox", "two accounts", "space lite", "parallel lite", "multi parallel",

        // Cheating, Modding & App Hiders
        "lucky patcher", "gameguardian", "cheat", "hack", "patcher", "app hider", "hide apps",
        "calculator vault", "photo vault", "gallery lock", "secret vault",

        // Casual Dating & Hookups
        "dating", "tinder", "bumble", "flirt", "randomchat", "hookup", "omegle", "ometv", "chat room",

        // Short-Dramas, Web Novels, Comics & Anime Streamers
        "short drama", "shortmax", "reelshort", "dramabox", "goodshort", "snackshort", "moboreels",
        "netshort", "webtoon", "manga", "manhwa", "manhua", "comic", "comics", "anime", "webnovel",
        "light novel", "fanfiction", "wattpad", "wuxia", "livestream", "live stream", "broadcast",
        "kisskh", "kissasian", "bilibili", "loklok", "cloudstream", "stremio", "onstream",

        // Social Feeds, Video & Forum Distractions
        "tiktok", "tik tok", "douyin", "reddit"
    };

    private static final String[] NEGATIVE_PKG_SUBSTRINGS = {
        ".game.", ".games.", ".gaming.", ".casino.", ".poker.", ".slots.", ".bet.",
        ".arcade.", ".simulator.", ".tycoon.", ".dating.", ".hookup.", ".cheat.",
        ".cloner.", ".dualspace.", ".parallel.", ".vmos.", ".virtual.",
        // Remote Desktop, Screen Share & VNC Workarounds
        ".teamviewer.", ".anydesk.", ".rustdesk.", ".remotedesktop.", ".screenshare.",
        ".screenmirror.", ".screenstream.", ".splashtop.", ".parsec.", ".airdroid.",
        ".nethunter.", ".kex.", ".droidvnc.",
        // Milestone 17: Container & Sandbox Signatures
        ".clone.", ".dual.", ".double.", ".secondspace.", ".appcloner.", ".island.",
        ".shelter.", ".vphonegaga.", ".f1player.", ".gspace.", ".vault.", ".gallerylock.",
        ".multispace.", ".superclone.", ".2accounts.", ".x8zs.", ".shortdrama.", ".dramabox.",
        ".reelshort.", ".shortmax.", ".goodshort.", ".webtoon.", ".manga.", ".manhwa.", ".comic.",
        ".webnovel.", ".wattpad.", ".gacha.", ".rpg.", ".brawl.",
        // Short video & social signatures
        ".musically.", ".trill.", ".aweme.",
        // Stage 1 Bloatware & Game Booster Signatures (UAD-NG Ground Truth)
        "joyose", "gamecenter", "gamebooster", "gamemode", "gamehome", "gamespace",
        "shortvideo", "mipicks", "palmstore", "glance"
    };

    /**
     * Positive Academic & Educational Keywords.
     * Used to promote unknown apps (such as unlisted university portals, local study hubs,
     * or specialized textbook readers) with CATEGORY_UNDEFINED to ALLOWED.
     */
    private static final String[] POSITIVE_ACADEMIC_KEYWORDS = {
        // Academic & School Institutions
        "study", "school", "university", "college", "campus", "academy", "course",
        "coursework", "class", "classroom", "lecture", "curriculum", "syllabus",
        "student", "faculty", "edu",
        // Learning Tools, Science, Math & Languages
        "reader", "pdf", "epub", "ebook", "dictionary", "thesaurus", "vocab",
        "vocabulary", "grammar", "translate", "translator", "formula", "calculator",
        "desmos", "geometry", "algebra", "physics", "chemistry", "biology", "science",
        "history", "anatomy", "flashcard", "flashcards", "quizlet", "homework",
        "learning", "coding", "compiler", "terminal", "pydroid", "ide"
    };

    private AppClassifier() {}

    /**
     * Clears the classification cache. Called when whitelist changes in AppSettings.
     */
    public static void clearCache() {
        decisionCache.clear();
        Log.d(TAG, "Classification decision cache cleared");
    }

    /**
     * Invalidates a specific package from cache (e.g. upon package update/install).
     */
    public static void invalidatePackage(String pkg) {
        if (pkg != null) {
            decisionCache.remove(pkg);
        }
    }

    /**
     * Classifies whether a package should be blocked during lockdown.
     *
     * @param context       Android Context for PackageManager access.
     * @param pkg           The target package name.
     * @param userWhitelist The user-configured whitelist from AppSettings.
     * @return true if BLOCKED, false if ALLOWED.
     */
    public static boolean isPackageBlocked(Context context, String pkg, Set<String> userWhitelist) {
        if (pkg == null || pkg.trim().isEmpty()) {
            return false;
        }

        // Tier 1a: Core OS framework and QIEZKA itself
        if (pkg.equals("android") || pkg.equals("com.android.systemui")) {
            return false;
        }
        if (context != null && pkg.equals(context.getPackageName())) {
            return false;
        }

        String appLabel = null;
        if (context != null) {
            try {
                PackageManager pm = context.getPackageManager();
                if (pm != null) {
                    ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
                    CharSequence lbl = pm.getApplicationLabel(ai);
                    if (lbl != null) appLabel = lbl.toString();
                }
            } catch (Exception ignore) {}
        }

        // Tier 1b: Telephony, Emergency in-call UI, SIM Toolkit (STK), MMS & Carrier Push
        if (TELEPHONY_PACKAGES.contains(pkg) || isSimOrCarrierService(pkg, appLabel)) {
            return false;
        }

        // Tier 1b-2: Standalone Universal Android Debloater (UAD-NG) System Allowlist
        // (Emergency services, AOSP Screenshot Markup, IntentResolver/Chooser, Captive Portal, etc.)
        if (SystemUadAllowlist.isUadSystemAllowed(pkg, appLabel)) {
            return false;
        }

        // Tier 1c: System App Store & Package Installers are explicitly allowed
        if (isInstallerOrStoreApp(pkg)) {
            return false;
        }

        // STAGE 1 — MASTER VETO GATE: Anti-Tamper Shield (Android Settings, MIUI Security Center, OEM Phone Managers)
        if (isSettingsOrDeviceManager(pkg, appLabel)) {
            return true;
        }

        // STAGE 1 — MASTER VETO GATE: Hardware Bloatware & Game Boosters (Joyose, GameCenter, PalmStore, Glance)
        if (isStage1Bloat(pkg, appLabel)) {
            return true;
        }

        // STAGE 2 — APP CLASSIFIER GATE: Category Game, Category Social & Negative Distraction Heuristics can NEVER be whitelisted!
        if (isForbiddenDistraction(context, pkg)) {
            return true;
        }

        // STAGE 2b: Explicit User Whitelist from AppSettings (Unified Whitelist)
        // (Allows user/forks to whitelist tools, audio, or video apps like YouTube if explicitly selected!)
        if (userWhitelist != null && userWhitelist.contains(pkg)) {
            return false;
        }

        // Check in-memory decision cache
        Boolean cached = decisionCache.get(pkg);
        if (cached != null) {
            return cached;
        }

        // Evaluate and store
        boolean blocked = evaluatePackage(context, pkg);
        decisionCache.put(pkg, blocked);
        return blocked;
    }

    /**
     * Internal multi-layer heuristic evaluation.
     */
    private static boolean evaluatePackage(Context context, String pkg) {
        // STAGE 1 — MASTER VETO GATE: Anti-Tamper Shield & Bloatware
        if (isSettingsOrDeviceManager(pkg, null) || isStage1Bloat(pkg, null)) {
            return true;
        }

        if (context == null) {
            return true; // Conservative fallback
        }

        PackageManager pm = context.getPackageManager();
        if (pm == null) {
            return true;
        }

        ApplicationInfo appInfo;
        String appLabel = "";
        try {
            appInfo = pm.getApplicationInfo(pkg, 0);
            CharSequence labelChar = pm.getApplicationLabel(appInfo);
            if (labelChar != null) {
                appLabel = labelChar.toString();
            }
        } catch (PackageManager.NameNotFoundException e) {
            // App not found or uninstalled
            return true;
        } catch (Exception e) {
            Log.w(TAG, "Error querying ApplicationInfo for " + pkg + ": " + e.getMessage());
            return true;
        }

        String lowerLabel = appLabel.toLowerCase(Locale.ROOT).trim();
        String lowerPkg = pkg.toLowerCase(Locale.ROOT).trim();

        // STAGE 1 — MASTER VETO GATE: Anti-Tamper Shield (Checks both package name and app label)
        if (isSettingsOrDeviceManager(pkg, appLabel) || isStage1Bloat(pkg, appLabel)) {
            Log.i(TAG, "Blocked by Stage 1 Master Veto: " + pkg + " (" + appLabel + ")");
            return true;
        }

        // Milestone 20: WebAPK & PWA Deep Metadata Inspection
        if (pkg.startsWith("org.chromium.webapk") || pkg.contains("webapk")) {
            if (hasNegativeDistractionSignals(lowerLabel, lowerPkg)) {
                Log.w(TAG, "Blocked WebAPK by negative signals: " + pkg + " (" + appLabel + ")");
                return true;
            }
            try {
                ApplicationInfo appInfoWithMeta = pm.getApplicationInfo(pkg, PackageManager.GET_META_DATA);
                if (appInfoWithMeta != null && appInfoWithMeta.metaData != null) {
                    String startUrl = appInfoWithMeta.metaData.getString("org.chromium.webapk.shell_apk.startUrl");
                    if (startUrl == null) {
                        startUrl = appInfoWithMeta.metaData.getString("org.chromium.webapk.shell_apk.scopeUrl");
                    }
                    if (startUrl != null && !startUrl.isEmpty()) {
                        Log.i(TAG, "Inspecting WebAPK startUrl: " + startUrl + " for pkg: " + pkg);
                        String lowerStart = startUrl.toLowerCase(Locale.ROOT);
                        if (WebBlocklistConstants.isPiracyOrMediaDomain(lowerStart) ||
                            WebBlocklistConstants.isWebGameDomain(lowerStart) ||
                            WebBlocklistConstants.isGamblingDomain(lowerStart) ||
                            WebBlocklistConstants.isAdultDomain(lowerStart) ||
                            BlacklistConstants.isBlacklisted(startUrl, appLabel)) {
                            Log.w(TAG, "Blocked WebAPK matching blocklist: " + pkg + " (" + appLabel + ", " + startUrl + ")");
                            return true;
                        }
                        WebClassifier.ClassificationResult urlResult = WebClassifier.classify(startUrl, null, false);
                        if (urlResult.isBlocked) {
                            Log.w(TAG, "Blocked WebAPK via WebClassifier: " + pkg + " (" + appLabel + ", " + startUrl + " - " + urlResult.reason + ")");
                            return true;
                        }
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Error inspecting WebAPK meta-data for " + pkg + ": " + e.getMessage());
            }
        }

        // Milestone 17: Fake Calculator & Secret Vault Inspection
        if (isFakeCalculatorVault(pm, pkg, lowerLabel, lowerPkg)) {
            Log.i(TAG, "Blocked as fake calculator vault: " + pkg + " (" + appLabel + ")");
            return true;
        }

        // ── Layer 4a: Negative Distraction Signals ──
        // Overrides any claimed category: if it looks like a game/gamble/mod, block immediately!
        if (hasNegativeDistractionSignals(lowerLabel, lowerPkg)) {
            Log.i(TAG, "Blocked by negative distraction heuristics: " + pkg + " (" + appLabel + ")");
            return true;
        }

        // ── Layer 3: Android OS Application Category ──
        int category = CATEGORY_UNDEFINED;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && appInfo != null) {
            category = appInfo.category;
        }

        switch (category) {
            case CATEGORY_GAME:
                Log.i(TAG, "Blocked by CATEGORY_GAME: " + pkg + " (" + appLabel + ")");
                return true;

            case CATEGORY_SOCIAL:
                Log.i(TAG, "Blocked by CATEGORY_SOCIAL: " + pkg + " (" + appLabel + ")");
                return true;

            case CATEGORY_VIDEO:
                Log.i(TAG, "Blocked by CATEGORY_VIDEO: " + pkg + " (" + appLabel + ")");
                return true;

            case CATEGORY_NEWS:
                Log.i(TAG, "Blocked by CATEGORY_NEWS: " + pkg + " (" + appLabel + ")");
                return true;

            case CATEGORY_ACCESSIBILITY:
                Log.d(TAG, "Allowed by CATEGORY_ACCESSIBILITY: " + pkg);
                return false;

            case CATEGORY_MAPS:
                Log.d(TAG, "Allowed by CATEGORY_MAPS: " + pkg);
                return false;

            case CATEGORY_AUDIO:
                // Safe study audio / podcasts (since negative signals were already ruled out)
                Log.d(TAG, "Allowed by CATEGORY_AUDIO: " + pkg);
                return false;

            case CATEGORY_IMAGE:
                // Safe photo / scanning / art tools
                Log.d(TAG, "Allowed by CATEGORY_IMAGE: " + pkg);
                return false;

            case CATEGORY_PRODUCTIVITY:
                // Safe productivity / cloud / study app (unless it is a remote desktop or screen share bypass)
                if (isRemoteDesktopOrScreenShare(lowerPkg, lowerLabel)) {
                    Log.i(TAG, "Blocked remote desktop / screen share claiming CATEGORY_PRODUCTIVITY: " + pkg + " (" + appLabel + ")");
                    return true;
                }
                Log.d(TAG, "Allowed by CATEGORY_PRODUCTIVITY: " + pkg + " (" + appLabel + ")");
                return false;

            case CATEGORY_UNDEFINED:
            default:
                break;
        }

        // ── Layer 4b: Positive Academic & Educational Promotion ──
        // Promotes unknown or un-categorized apps if their label or package contains academic indicators
        if (hasPositiveAcademicSignals(lowerLabel, lowerPkg)) {
            Log.i(TAG, "Promoted & Allowed by academic keyword heuristics: " + pkg + " (" + appLabel + ")");
            return false;
        }

        // ── Layer 4c: SIM Card Toolkit (STK), MMS & Carrier Services ──
        if (isSimOrCarrierService(pkg, appLabel)) {
            Log.d(TAG, "Allowed as SIM toolkit / Carrier / MMS service: " + pkg + " (" + appLabel + ")");
            return false;
        }

        // ── Layer 4d: Standalone Universal Android Debloater (UAD-NG) System Allowlist ──
        if (SystemUadAllowlist.isUadSystemAllowed(pkg, appLabel)) {
            Log.d(TAG, "Allowed by UAD-NG System Allowlist: " + pkg + " (" + appLabel + ")");
            return false;
        }

        // ── Stage 2: Universal System Partition Gateway ──
        // If it is on the system partition (pre-installed by OEM in /system, /vendor, /product)
        // and has passed all Stage 1 Master Veto checks above (not YouTube, not social, not game, not settings/manager),
        // it is a verified legitimate OEM hardware tool or sub-APK!
        if (appInfo != null) {
            boolean isSystemApp = (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0 ||
                                  (appInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0;
            if (isSystemApp) {
                // Guard: Ensure general web browsers still route through WebClassifier URL inspection
                if (!lowerPkg.contains("browser") && !lowerPkg.contains("chrome") && !lowerPkg.contains("firefox")) {
                    Log.d(TAG, "Allowed by Universal System Partition Gateway: " + pkg + " (" + appLabel + ")");
                    return false;
                }
            }
        }

        // ── Layer 5: Conservative Fallback ──
        // Unknown user-installed third-party app with undefined category and no educational signals is blocked
        Log.i(TAG, "Blocked by conservative fallback (unknown user app): " + pkg + " (" + appLabel + ")");
        return true;
    }

    /**
     * Identifies Android Settings, MIUI Security Center, and OEM Device Care/Phone Managers
     * that provide UI to force-stop QIEZKA, clear app data, or revoke permissions.
     */
    public static boolean isSettingsOrDeviceManager(String pkg, String appLabel) {
        if (pkg == null) return false;
        String lowerPkg = pkg.toLowerCase(Locale.ROOT);
        if (lowerPkg.equals("com.android.settings") ||
            lowerPkg.equals("com.miui.securitycenter") ||
            lowerPkg.equals("com.coloros.safecenter") ||
            lowerPkg.equals("com.samsung.android.lool") ||
            lowerPkg.equals("com.transsion.phonemaster") ||
            lowerPkg.equals("com.vivo.safecenter") ||
            lowerPkg.equals("com.iqoo.secure") ||
            lowerPkg.equals("com.huawei.systemmanager") ||
            lowerPkg.equals("com.google.android.apps.wellbeing") ||
            lowerPkg.contains(".settings") || lowerPkg.contains("securitycore")) {
            return true;
        }
        if (appLabel != null && !appLabel.trim().isEmpty()) {
            String lowerLabel = appLabel.toLowerCase(Locale.ROOT);
            if (lowerLabel.equals("settings") || lowerLabel.contains("phone manager") ||
                lowerLabel.contains("device care") || lowerLabel.contains("security center") ||
                lowerLabel.contains("app manager") || lowerLabel.contains("cleaner") ||
                lowerLabel.contains("battery saver") || lowerLabel.contains("system manager")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Identifies UAD-NG-derived hardware-level bloatware, Game Turbo daemons,
     * and instant game portal stores.
     */
    public static boolean isStage1Bloat(String pkg, String appLabel) {
        if (pkg == null) return false;
        String lowerPkg = pkg.toLowerCase(Locale.ROOT);
        if (lowerPkg.contains("joyose") || lowerPkg.contains("gamecenter") ||
            lowerPkg.contains("gamebooster") || lowerPkg.contains("gamemode") ||
            lowerPkg.contains("gamehome") || lowerPkg.contains("gamespace") ||
            lowerPkg.contains("mipicks") || lowerPkg.contains("palmstore") ||
            lowerPkg.contains("glance")) {
            return true;
        }
        if (appLabel != null && !appLabel.trim().isEmpty()) {
            String lowerLabel = appLabel.toLowerCase(Locale.ROOT);
            if (lowerLabel.contains("game center") || lowerLabel.contains("game booster") ||
                lowerLabel.contains("palm store") || lowerLabel.contains("getapps")) {
                return true;
            }
        }
        return false;
    }

    public static boolean isForbiddenDistraction(Context context, String pkg) {
        if (pkg == null || context == null) return false;
        if (pkg.equals("com.android.settings") || isSettingsOrDeviceManager(pkg, null)) return true;
        if (isStage1Bloat(pkg, null)) return true;
        try {
            PackageManager pm = context.getPackageManager();
            if (pm == null) return false;
            ApplicationInfo appInfo = pm.getApplicationInfo(pkg, 0);
            CharSequence labelChar = pm.getApplicationLabel(appInfo);
            String appLabel = labelChar != null ? labelChar.toString() : "";
            String lowerPkg = pkg.toLowerCase(Locale.ROOT).trim();
            String lowerLabel = appLabel.toLowerCase(Locale.ROOT).trim();

            // 1. Remote Desktop, Screen Sharing & Kali NetHunter KeX/VNC Workarounds
            if (isRemoteDesktopOrScreenShare(lowerPkg, lowerLabel)) {
                return true;
            }

            // 2. BlacklistConstants check
            if (BlacklistConstants.isBlacklisted(pkg, appLabel)) return true;

            // 3. Short video & TikTok variants (both global musically, trill, and aweme)
            if (lowerPkg.contains("musically") || lowerPkg.contains(".trill") || lowerPkg.contains(".aweme") ||
                lowerLabel.contains("tiktok") || lowerLabel.contains("tik tok") || lowerLabel.contains("douyin")) {
                return true;
            }

            // 4. Video Streaming & Entertainment (YouTube, Netflix, Twitch, Bilibili, Stremio, ReVanced)
            if (lowerPkg.contains("youtube") || lowerPkg.contains("netflix") || lowerPkg.contains("twitch") ||
                lowerPkg.contains("bilibili") || lowerPkg.contains("danmaku.bili") || lowerPkg.contains("stremio") ||
                lowerPkg.contains("revanced") || lowerPkg.contains("newpipe") ||
                lowerLabel.contains("youtube") || lowerLabel.contains("netflix") || lowerLabel.contains("twitch") ||
                lowerLabel.contains("bilibili") || lowerLabel.contains("stremio")) {
                // Ensure safe audio (like YouTube Music) is not blocked as a video distraction
                if (!lowerPkg.contains("music") && !lowerLabel.contains("music")) {
                    return true;
                }
            }

            // 5. Social Media Feeds & Forums (Instagram, Twitter/X, Facebook Feed, Reddit, Threads, Pinterest)
            if (lowerPkg.contains("instagram") || lowerPkg.contains("twitter") ||
                lowerPkg.equals("com.facebook.katana") || lowerPkg.equals("com.facebook.lite") ||
                lowerPkg.contains("reddit") || lowerPkg.contains("pinterest") || lowerPkg.contains("threads") ||
                lowerLabel.contains("instagram") || lowerLabel.contains("twitter") ||
                lowerLabel.equals("facebook") || lowerLabel.contains("reddit") || lowerLabel.contains("pinterest")) {
                return true;
            }

            // 6. Category evaluation
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (appInfo.category == ApplicationInfo.CATEGORY_GAME) {
                    return true;
                }
                if (appInfo.category == ApplicationInfo.CATEGORY_VIDEO) {
                    if (!lowerPkg.contains("music") && !lowerLabel.contains("music")) {
                        return true;
                    }
                }
                if (appInfo.category == ApplicationInfo.CATEGORY_NEWS) {
                    if (lowerPkg.contains("reddit") || lowerLabel.contains("reddit")) {
                        return true;
                    }
                }
                if (appInfo.category == ApplicationInfo.CATEGORY_SOCIAL) {
                    // DIRECT MESSAGING APPS (Telegram, Messenger, WhatsApp, Signal) ARE NOT FORBIDDEN DISTRACTIONS!
                    // They can be whitelisted by the user in Allowed Apps.
                    if (!isMessagingApp(lowerPkg, lowerLabel)) {
                        return true;
                    }
                }
            }
            if ((appInfo.flags & ApplicationInfo.FLAG_IS_GAME) != 0) {
                return true;
            }

            // 7. Fake Calculator Vaults
            if (isFakeCalculatorVault(pm, pkg, lowerLabel, lowerPkg)) {
                return true;
            }

            // 8. Negative Distraction Heuristics (Games, Gambling, Modding, Adult, Comics)
            if (hasNegativeDistractionSignals(lowerLabel, lowerPkg)) {
                return true;
            }
        } catch (Exception ignore) {}
        return false;
    }

    /**
     * Inspects whether an application declared as Calculator/Calc is actually a disguised vault.
     * Genuine offline calculators never request Camera, Storage/Media, or Photo access.
     */
    public static boolean isFakeCalculatorVault(PackageManager pm, String pkg, String lowerLabel, String lowerPkg) {
        if (lowerLabel.contains("calc") || lowerLabel.contains("calculator") ||
            lowerPkg.contains("calc") || lowerPkg.contains("calculator")) {

            // Substring vault indicators
            if (lowerPkg.contains("vault") || lowerPkg.contains("hide") || lowerPkg.contains("secret") ||
                lowerPkg.contains("lock") || lowerPkg.contains("protect") || lowerPkg.contains("private") ||
                lowerPkg.contains("privacy") || lowerLabel.contains("vault") || lowerLabel.contains("hide") ||
                lowerLabel.contains("lock") || lowerLabel.contains("secret")) {
                return true;
            }

            // Real Android permission inspection (Milestone 17):
            // Genuine calculators (Google, Samsung, Mi, Desmos) never need camera or media storage.
            if (pm != null && pkg != null) {
                try {
                    android.content.pm.PackageInfo pi = pm.getPackageInfo(pkg, PackageManager.GET_PERMISSIONS);
                    if (pi != null && pi.requestedPermissions != null) {
                        for (String perm : pi.requestedPermissions) {
                            if (perm == null) continue;
                            if (perm.equals("android.permission.CAMERA") ||
                                perm.equals("android.permission.READ_EXTERNAL_STORAGE") ||
                                perm.equals("android.permission.MANAGE_EXTERNAL_STORAGE") ||
                                perm.equals("android.permission.READ_MEDIA_IMAGES") ||
                                perm.equals("android.permission.READ_MEDIA_VIDEO") ||
                                perm.equals("android.permission.SYSTEM_ALERT_WINDOW")) {
                                Log.w(TAG, "Fake calculator vault detected (requests " + perm + "): " + pkg);
                                return true;
                            }
                        }
                    }
                } catch (Exception ignore) {}
            }
        }
        return false;
    }

    public static boolean hasNegativeDistractionSignals(String lowerLabel, String lowerPkg) {
        // Milestone 17: Fake Calculator Vault Substring Detection Fallback
        if (lowerLabel.contains("calc") || lowerLabel.contains("calculator")) {
            if (lowerPkg.contains("vault") || lowerPkg.contains("hide") || lowerPkg.contains("secret") ||
                lowerPkg.contains("lock") || lowerPkg.contains("protect") || lowerPkg.contains("private") ||
                lowerPkg.contains("privacy") || lowerLabel.contains("vault") || lowerLabel.contains("hide") ||
                lowerLabel.contains("lock") || lowerLabel.contains("secret")) {
                return true;
            }
        }

        for (String kw : NEGATIVE_LABEL_KEYWORDS) {
            if (lowerLabel.contains(kw)) {
                return true;
            }
        }
        for (String sub : NEGATIVE_PKG_SUBSTRINGS) {
            if (lowerPkg.contains(sub)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasPositiveAcademicSignals(String lowerLabel, String lowerPkg) {
        for (String kw : POSITIVE_ACADEMIC_KEYWORDS) {
            if (lowerLabel.contains(kw) || lowerPkg.contains("." + kw) || lowerPkg.contains(kw + ".")) {
                return true;
            }
        }
        return false;
    }
}
