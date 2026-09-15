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
     * Essential telephony and in-call packages that must never be blocked during lockdown
     * to ensure emergency calling and critical incoming calls remain operational.
     */
    public static final Set<String> TELEPHONY_PACKAGES = new HashSet<>(Arrays.asList(
        "com.android.phone",
        "com.android.server.telecom",
        "com.android.incallui",
        "com.google.android.dialer",
        "com.samsung.android.dialer",
        "com.samsung.android.incallui"
    ));

    /**
     * Negative Distraction Keywords.
     * If an app's label or package name contains any of these terms, it is strictly BLOCKED,
     * even if the APK falsely declares itself as CATEGORY_PRODUCTIVITY.
     */
    private static final String[] NEGATIVE_LABEL_KEYWORDS = {
        // Gaming & Gambling
        "game", "games", "gaming", "casino", "poker", "slots", "jackpot", "betting",
        "arcade", "puzzle", "simulator", "tycoon", "rpg", "mmo", "survivor", "arena",
        "battle royale", "racing", "dungeon", "quest", "brawl", "clash of",
        // Casual Dating & Hookups
        "dating", "tinder", "bumble", "flirt", "randomchat", "hookup", "omegle", "ometv",
        // Modding, Cheating & App Cloning
        "lucky patcher", "gameguardian", "cheat", "hack", "patcher", "cloner", "dual space",
        "parallel space", "virtual space",
        // Distracting Entertainment & Comics
        "manga", "webtoon", "comic", "anime", "shortmax", "reelshort", "dramabox", "goodshort"
    };

    private static final String[] NEGATIVE_PKG_SUBSTRINGS = {
        ".game.", ".games.", ".gaming.", ".casino.", ".poker.", ".slots.", ".bet.",
        ".arcade.", ".simulator.", ".tycoon.", ".dating.", ".hookup.", ".cheat.",
        ".cloner.", ".dualspace.", ".parallel.", ".vmos.", ".virtual."
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

        // Tier 1b: Telephony & Emergency in-call UI
        if (TELEPHONY_PACKAGES.contains(pkg)) {
            return false;
        }

        // Tier 1c: Hardcoded Distraction Blacklist & Hostile Signatures MUST take precedence!
        // YouTube native app, TikTok, games, and social media can NEVER be allowed.
        if (BlacklistConstants.isBlacklisted(pkg)) {
            return true;
        }

        // Tier 1d: Explicit User Whitelist from AppSettings
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
        // Tier 2a: System Settings is strictly blocked to prevent tampering
        if (pkg.equals("com.android.settings")) {
            return true;
        }

        // Tier 2b: Hardcoded Distraction Blacklist & Hostile Signatures
        if (BlacklistConstants.isBlacklisted(pkg)) {
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
                // Safe productivity / cloud / study app
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

        // ── Layer 5: Conservative Fallback ──
        // Unknown app with undefined category and no educational signals is blocked
        Log.i(TAG, "Blocked by conservative fallback (unknown app): " + pkg + " (" + appLabel + ")");
        return true;
    }

    private static boolean hasNegativeDistractionSignals(String lowerLabel, String lowerPkg) {
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
