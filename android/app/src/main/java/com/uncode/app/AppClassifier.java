package com.uncode.app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
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
            if ((lowerLabel.contains("facebook") && !lowerLabel.contains("messenger")) || lowerLabel.contains("instagram") ||
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
     * Recognized Online Class, Lecture & Video Conferencing applications.
     * Essential communication and class infrastructure (Google Meet, Zoom, Teams, Webex).
     */
    public static final Set<String> KNOWN_CONFERENCE_PACKAGES = new HashSet<>(Arrays.asList(
        "com.google.android.apps.tachyon",
        "com.google.android.apps.meetings",
        "us.zoom.videomeetings",
        "com.microsoft.teams",
        "com.cisco.webex.meetings"
    ));

    public static boolean isMeetingOrVideoConferenceApp(String pkg, String appLabel) {
        if (pkg == null) return false;
        String lowerPkg = pkg.toLowerCase(Locale.ROOT).trim();
        if (KNOWN_CONFERENCE_PACKAGES.contains(lowerPkg)) {
            return true;
        }
        if (lowerPkg.contains(".tachyon") || lowerPkg.contains(".zoom.") ||
            lowerPkg.contains("teams.quickshare") || lowerPkg.contains("webex")) {
            return true;
        }
        if (appLabel != null && !appLabel.trim().isEmpty()) {
            String lowerLabel = appLabel.toLowerCase(Locale.ROOT).trim();
            if (lowerLabel.equals("meet") || lowerLabel.equals("google meet") ||
                lowerLabel.equals("zoom") || lowerLabel.equals("zoom workplace") ||
                lowerLabel.equals("teams") || lowerLabel.equals("microsoft teams") ||
                lowerLabel.equals("webex") || lowerLabel.equals("webex meetings")) {
                return true;
            }
        }
        return false;
    }

    public static boolean isCameraApp(Context context, String pkg, String appLabel) {
        return KnownSafe.isCameraApp(context, pkg, appLabel);
    }

    public static boolean isCameraIntentHandler(PackageManager pm, String pkg) {
        if (pm == null || pkg == null || pkg.trim().isEmpty()) return false;
        try {
            Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            captureIntent.setPackage(pkg);
            List<ResolveInfo> captureList = pm.queryIntentActivities(captureIntent, 0);
            if (captureList != null && !captureList.isEmpty()) {
                return true;
            }

            Intent stillIntent = new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA);
            stillIntent.setPackage(pkg);
            List<ResolveInfo> stillList = pm.queryIntentActivities(stillIntent, 0);
            if (stillList != null && !stillList.isEmpty()) {
                return true;
            }

            Intent secureIntent = new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE);
            secureIntent.setPackage(pkg);
            List<ResolveInfo> secureList = pm.queryIntentActivities(secureIntent, 0);
            if (secureList != null && !secureList.isEmpty()) {
                return true;
            }
        } catch (Exception ignore) {}
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
        // Rhythm Games & Tap Games
        "rhythm game", "coxeta", "costheta", "phigros", "arcaea", "cytus", "deemo", "dynamix",
        "kalpa", "lanota", "rotaeno", "musedash", "muse dash", "project sekai",

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
        // Rhythm Game Signatures
        ".costheta.", ".coxeta.", ".phigros.", ".arcaea.", ".cytus.", ".dynamix.",
        ".musedash.", ".teamrhythmicals.", ".rhythm.",
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
        "history", "anatomy", "flashcard", "flashcards", "quizlet", "quiz", "quizzes",
        "gizmo", "saveall", "tutor", "tutoring", "exam", "exams", "testprep", "revision",
        "spaced repetition", "studysmarter", "kahoot", "quizizz", "brainscape",
        "homework", "learning", "coding", "compiler", "terminal", "pydroid", "ide",
        "meeting", "conference", "webinar", "teams", "zoom",
        "camera", "kamera", "gcam", "lmc", "sgcam", "agc", "snapcam", "photograph", "lens"
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
    public static boolean isYoutubePackage(String pkg) {
        if (pkg == null) return false;
        UnifiedService yt = UnifiedPolicyRegistry.SERVICES.get("youtube");
        return yt != null && yt.getPackages().contains(pkg.toLowerCase(Locale.ROOT).trim());
    }

    /**
     * Evaluates whether a package is recognized in KnownSafe (MSG_GATE2):
     * 1. QIEZKA itself
     * 2. User-configured UI Allowed Apps whitelist
     * 3. Active & installed keyboard / Input Method Editors (IMEs)
     *
     * Note: YouTube and AI are NEVER in KnownSafe. They reside in KnownDistracting,
     * and their execution is governed by UnifiedPolicyRegistry.
     */
    public static Set<String> getActiveServices(Context context) {
        if (context == null) return Collections.emptySet();
        try {
            SharedPreferences prefs = context.getSharedPreferences("uncode_lock", Context.MODE_PRIVATE);
            if (prefs != null) {
                Set<String> activeServices = prefs.getStringSet("active_unified_services", null);
                Set<String> effectiveServices = activeServices != null ? new HashSet<>(activeServices) : new HashSet<>();
                if (prefs.getBoolean("allow_youtube", false)) {
                    effectiveServices.add("youtube");
                }
                return effectiveServices;
            }
        } catch (Exception ignore) {}
        return Collections.emptySet();
    }

    /**
     * Evaluates whether a package is recognized in KnownSafe (MSG_GATE2).
     * Delegates directly to authoritative KnownSafe.kt.
     */
    public static boolean isKnownSafe(Context context, String pkg, Set<String> userWhitelist) {
        return KnownSafe.isKnownSafe(context, pkg, userWhitelist, getActiveServices(context));
    }

    /**
     * Evaluates whether a package is allowed by the user via the Unified Policy Registry.
     */
    public static boolean isPackageAllowedByUnifiedPolicy(Context context, String pkg) {
        if (context == null || pkg == null || pkg.trim().isEmpty()) {
            return false;
        }
        return UnifiedPolicyRegistry.isPackageAllowedByService(pkg, getActiveServices(context));
    }

    /**
     * Classifies whether a package should be blocked during lockdown.
     * Follows the Stage 1 -> Stage 2 (MSG_GATE -> MSG_GATE2 -> MSG_GATE3) -> Secondary Classifier -> Stage 3 Gateway architecture.
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

        // Self-immunity: QIEZKA itself is never blocked
        if (context != null && pkg.equals(context.getPackageName())) {
            return false;
        }

        // STAGE 2 — Branch 1: Web Browsers are inspected by WebClassifier at URL/DOM level and NEVER blocked at package level
        if (LockAccessibilityService.isBrowserPackage(pkg)) {
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

        // STAGE 1 — MASTER VETO GATE: Anti-Tamper Shield (Android Settings, MIUI Security Center, OEM Phone Managers)
        if (isSettingsOrDeviceManager(pkg, appLabel)) {
            return true;
        }

        // STAGE 1 — MASTER VETO GATE: Hardware Bloatware & Game Boosters (Joyose, GameCenter, PalmStore, Glance)
        if (isStage1Bloat(pkg, appLabel)) {
            return true;
        }

        // Home Launchers (including system and third-party launchers) are never blocked
        if (LockAccessibilityService.isLauncherApp(context, pkg)) {
            return false;
        }

        // Check in-memory decision cache
        Boolean cached = decisionCache.get(pkg);
        if (cached != null) {
            return cached;
        }

        // ── STAGE 2 — INITIAL APP POLICY EVALUATION (Truth Table) ──
        boolean isDistracting = KnownDistracting.isKnownDistracting(pkg, appLabel); // MSG_GATE
        boolean isSafe = isKnownSafe(context, pkg, userWhitelist);                  // MSG_GATE2

        // MSG_GATE3 — Native Truth Table (Flowchart Alignment)
        // 1. YES KnownDistracting, YES KnownSafe (e.g. YouTube / AI enabled by user in Unified Policy) -> ALLOW
        if (isDistracting && isSafe) {
            decisionCache.put(pkg, false);
            return false;
        }

        // 2. NO KnownDistracting, YES KnownSafe (e.g. Google Classroom / Anki / Gboard / User-Allowed App) -> ALLOW
        if (!isDistracting && isSafe) {
            decisionCache.put(pkg, false);
            return false;
        }

        // 3. YES KnownDistracting, NO KnownSafe (e.g. TikTok, Steam, Coxeta, or disabled Unified Policy service) -> BLOCK
        if (isDistracting && !isSafe) {
            decisionCache.put(pkg, true);
            return true;
        }

        // 4. NO KnownDistracting, NO KnownSafe -> Proceed to Secondary App Classifier heuristics
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
                // Direct messaging applications (WhatsApp, Telegram, Signal, Messenger)
                // and online lecture conferencing tools (Meet, Zoom, Teams)
                if (isMessagingApp(pkg, appLabel) || isMeetingOrVideoConferenceApp(pkg, appLabel)) {
                    Log.d(TAG, "Allowed messaging/meeting app claiming CATEGORY_SOCIAL: " + pkg);
                    return false;
                }
                Log.i(TAG, "Blocked by CATEGORY_SOCIAL: " + pkg + " (" + appLabel + ")");
                return true;

            case CATEGORY_VIDEO:
                // Video conferencing and online lecture tools are legitimate communication tools,
                // distinct from entertainment video streaming services (YouTube, Netflix, TikTok).
                if (isMeetingOrVideoConferenceApp(pkg, appLabel)) {
                    Log.d(TAG, "Allowed meeting / video conference app claiming CATEGORY_VIDEO: " + pkg);
                    return false;
                }
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

        // ── Camera App Gate (Celso Azevedo GCam Ports, Open Camera & OEM Cameras) ──
        if (KnownSafe.isCameraApp(context, pkg, appLabel) || isCameraIntentHandler(pm, pkg)) {
            Log.d(TAG, "Allowed as verified camera application: " + pkg + " (" + appLabel + ")");
            return false;
        }

        // ── Layer 4b: Positive Academic & Educational Promotion ──
        // Promotes unknown or un-categorized apps if their label or package contains academic indicators
        if (hasPositiveAcademicSignals(lowerLabel, lowerPkg)) {
            Log.i(TAG, "Promoted & Allowed by academic keyword heuristics: " + pkg + " (" + appLabel + ")");
            return false;
        }

        // ── STAGE 3 — UNIVERSAL SYSTEM GATEWAY (SYSALLOW) ──
        // If it is on the system partition (pre-installed by OEM in /system, /vendor, /product)
        // and has passed all Stage 1 Master Veto checks above (not YouTube, not social, not game, not settings/manager),
        // it is a verified legitimate OEM hardware tool, SIM/STK, Telephony, or system utility!
        if (appInfo != null) {
            boolean isSystemApp = (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0 ||
                                  (appInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0;
            if (isSystemApp) {
                // Guard: Ensure general web browsers still route through WebClassifier URL inspection
                if (!lowerPkg.contains("browser") && !lowerPkg.contains("chrome") && !lowerPkg.contains("firefox")) {
                    Log.d(TAG, "Allowed by Stage 3 Universal System Gateway (SYSALLOW): " + pkg + " (" + appLabel + ")");
                    return false;
                }
            }
        }

        // ── Stage 2: Home Launcher Exemption ──
        // Home launchers (user's home screen) are always allowed to execute and must never fall through to Layer 5
        if (LockAccessibilityService.isLauncherApp(context, pkg)) {
            Log.d(TAG, "Allowed as Home Launcher: " + pkg + " (" + appLabel + ")");
            return false;
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
            lowerPkg.contains(".settings") || lowerPkg.contains("securitycore") ||
            lowerPkg.contains("permissioncontroller") ||
            lowerPkg.contains("setupwizard") ||
            lowerPkg.equals("com.android.provision") ||
            lowerPkg.contains("smartswitch") ||
            lowerPkg.contains("easymover") ||
            lowerPkg.contains("switchphone")) {
            return true;
        }
        if (appLabel != null && !appLabel.trim().isEmpty()) {
            String lowerLabel = appLabel.toLowerCase(Locale.ROOT);
            if (lowerLabel.equals("settings") || lowerLabel.contains("phone manager") ||
                lowerLabel.contains("device care") || lowerLabel.contains("security center") ||
                lowerLabel.contains("app manager") || lowerLabel.contains("cleaner") ||
                lowerLabel.contains("battery saver") || lowerLabel.contains("system manager") ||
                lowerLabel.contains("setup wizard")) {
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

    /**
     * Unified Stage 1 Master Veto Gate.
     * Evaluates anti-tamper protections (Settings, Device Managers, Setup Wizards),
     * OEM bloatware / game boosters, and Known Distracting apps.
     * Any package returning true here is strictly vetoed across the entire OS (cannot run,
     * cannot be exempted, and cannot be a Home launcher).
     */
    public static boolean isStage1Vetoed(String pkg, String appLabel) {
        if (pkg == null || pkg.trim().isEmpty()) return true;
        return isSettingsOrDeviceManager(pkg, appLabel) ||
               isStage1Bloat(pkg, appLabel) ||
               KnownDistracting.isKnownDistracting(pkg, appLabel);
    }

    /**
     * Evaluates whether an application qualifies as an allowed Stage 3 Universal System Gateway (SYSALLOW) app.
     * If an app is on the system partition (pre-installed by OEM in /system, /vendor, /product) and passed all
     * Stage 1 Master Veto checks (not settings/device manager, not bloatware, not games, not distracting video/social,
     * not a general web browser), it is already permitted dynamically by the operating system during lockdown.
     *
     * In accordance with UI declutter architecture, apps that pass Stage 3 SYSALLOW must NOT be rendered
     * at the UI (they are hidden from candidate selection and user whitelist).
     */
    public static boolean isPassedStage3SystemAllow(ApplicationInfo appInfo, String pkg, String appLabel) {
        if (appInfo == null || pkg == null) return false;
        boolean isSystemApp = (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0 ||
                              (appInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0;
        if (!isSystemApp) return false;

        String lowerPkg = pkg.toLowerCase(Locale.ROOT).trim();
        String lowerLabel = appLabel != null ? appLabel.toLowerCase(Locale.ROOT).trim() : "";

        // Web browsers must NOT be treated as invisible system utilities; they require WebClassifier URL inspection
        if (lowerPkg.contains("browser") || lowerPkg.contains("chrome") || lowerPkg.contains("firefox") || lowerPkg.contains("opera")) {
            return false;
        }

        // Must not be anti-tamper settings or device managers (Stage 1)
        if (isSettingsOrDeviceManager(pkg, appLabel)) return false;

        // Must not be hardware bloatware or game boosters (Stage 1)
        if (isStage1Bloat(pkg, appLabel)) return false;

        // Must not be a known distraction or game
        if (KnownDistracting.isKnownDistracting(pkg, appLabel)) return false;

        // Negative distraction heuristics (games, gambling, social feeds)
        if (hasNegativeDistractionSignals(lowerLabel, lowerPkg)) return false;

        // Verified legitimate OEM system partition tool!
        return true;
    }

    public static boolean isForbiddenDistraction(Context context, String pkg) {
        if (pkg == null || context == null) return false;
        if (pkg.equals("com.android.settings") || isSettingsOrDeviceManager(pkg, null)) return true;
        if (isStage1Bloat(pkg, null)) return true;
        if (KnownDistracting.isKnownDistracting(pkg)) return true;
        try {
            PackageManager pm = context.getPackageManager();
            if (pm == null) return false;
            ApplicationInfo appInfo = pm.getApplicationInfo(pkg, 0);
            CharSequence labelChar = pm.getApplicationLabel(appInfo);
            String appLabel = labelChar != null ? labelChar.toString() : "";
            if (KnownDistracting.isKnownDistracting(pkg, appLabel)) return true;
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
