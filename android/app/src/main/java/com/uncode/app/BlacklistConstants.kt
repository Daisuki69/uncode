package com.uncode.app

import java.util.Locale

/**
 * Hardcoded blacklist of anti-tamper and hardware-level bloatware applications.
 * Strictly prohibited from being whitelisted during system lockdown.
 * Consumer apps (YouTube, TikTok, games, etc.) are now dynamically handled
 * by AppClassifier categories and the unified whitelist.
 */
object BlacklistConstants {

    @JvmField
    val HARDCODED_BLACKLISTED_PACKAGES: Set<String> = hashSetOf(
        // Stage 1 Anti-Tamper Shield
        "com.android.settings",
        "com.miui.securitycenter",
        "com.coloros.safecenter",
        "com.samsung.android.lool",
        "com.transsion.phonemaster",
        "com.vivo.safecenter",
        "com.iqoo.secure",
        "com.huawei.systemmanager",
        "com.google.android.apps.wellbeing",

        // Stage 1 Hardware Bloatware & Game Boosters (UAD-NG Ground Truth)
        "com.xiaomi.joyose",
        "com.miui.gamebooster",
        "com.samsung.android.game.gamehome",
        "com.oplus.games",
        "com.coloros.gamespace",
        "com.xiaomi.mipicks",
        "com.transsion.palmostore",
        "com.heytap.market",
        "com.glance.internet"
    )

    @JvmStatic
    fun isBlacklisted(packageName: String?): Boolean {
        return isBlacklisted(packageName, null)
    }

    @JvmStatic
    fun isBlacklisted(packageName: String?, appLabel: String?): Boolean {
        // Stage 1 Master Veto: Anti-Tamper & Hardware Bloatware
        if (!packageName.isNullOrEmpty()) {
            val lower = packageName.trim().lowercase(Locale.ROOT)
            if (lower.contains("packageinstaller")) {
                return false
            }
            if (HARDCODED_BLACKLISTED_PACKAGES.contains(lower) ||
                AppClassifier.isSettingsOrDeviceManager(packageName, appLabel) ||
                AppClassifier.isStage1Bloat(packageName, appLabel)
            ) {
                return true
            }
            if (hasHostileSubstring(lower)) {
                return true
            }
        }
        // Substring checks for PWA and Browser Window Titles (KissKH, Anime, Manga, Web Games)
        if (!appLabel.isNullOrEmpty()) {
            val lowerLabel = appLabel.trim().lowercase(Locale.ROOT)
            val normalizedLabel = lowerLabel.replace(" ", "").replace("-", "").replace("_", "").replace(".", "")
            if (hasHostileSubstring(lowerLabel) || hasHostileSubstring(normalizedLabel)) {
                return true
            }
        }
        return false
    }

    private fun hasHostileSubstring(lower: String?): Boolean {
        if (lower.isNullOrEmpty()) return false
        return lower.contains("parallel") ||
            lower.contains("dualspace") ||
            lower.contains("multispace") ||
            lower.contains("superclone") ||
            lower.contains("appcloner") ||
            lower.contains("2accounts") ||
            lower.contains("multiaccounts") ||
            lower.contains("secondspace") ||
            lower.contains("securefolder") ||
            lower.contains("privatespace") ||
            lower.contains("gspace") ||
            lower.contains("island") ||
            lower.contains("shelter") ||
            lower.contains("calculatorvault") ||
            lower.contains("photovault") ||
            lower.contains("gallerylock") ||
            lower.contains("kisskh") ||
            lower.contains("kissasian") ||
            lower.contains("bilibili") ||
            lower.contains("danmaku.bili") ||
            lower.contains("webapk") ||
            lower.contains("crazygames") ||
            lower.contains("123movies") ||
            lower.contains("fmovies") ||
            lower.contains("soap2day") ||
            lower.contains("aniwave") ||
            lower.contains("9anime") ||
            lower.contains("gogoanime") ||
            lower.contains("mangadex") ||
            lower.contains("asurascans") ||
            lower.contains("manganelo") ||
            lower.contains("snackshort") ||
            lower.contains("playlet") ||
            lower.contains("sereal") ||
            lower.contains("chelpus") ||
            lower.contains("luckypatcher") ||
            lower.contains("instaprime") ||
            lower.contains("instander") ||
            lower.contains("aeroinsta") ||
            lower.contains("honista") ||
            lower.contains("gameguardian") ||
            lower.contains("happymod") ||
            lower.contains("acmarket") ||
            lower.contains("dramabox") ||
            lower.contains("reelshort") ||
            lower.contains("shortmax") ||
            lower.contains("goodshort") ||
            lower.contains("moboreels") ||
            lower.contains("topshort") ||
            lower.contains("netshort") ||
            lower.contains("shorttv") ||
            lower.contains("kalostv") ||
            lower.contains("loklok") ||
            lower.contains("cloudstream") ||
            lower.contains("movieboxpro") ||
            lower.contains("anilab") ||
            lower.contains("stremio") ||
            lower.contains("onstream") ||
            lower.contains("revanced") ||
            lower.contains("gbwhatsapp") ||
            lower.contains("fmwhatsapp") ||
            lower.contains("yowhatsapp") ||
            lower.contains("vmos") ||
            lower.contains("f1vm") ||
            lower.contains("vphonegaga") ||
            lower.contains("x8zs") ||
            lower.contains("teamviewer") ||
            lower.contains("anydesk") ||
            lower.contains("rustdesk") ||
            lower.contains("remotedesktop") ||
            lower.contains("screenshare") ||
            lower.contains("screenmirror") ||
            lower.contains("screenstream") ||
            lower.contains("airdroid") ||
            lower.contains("airmirror") ||
            lower.contains("apowermirror") ||
            lower.contains("splashtop") ||
            lower.contains("parsec") ||
            lower.contains("moonlight") ||
            lower.contains("steamlink") ||
            lower.contains("vysor") ||
            lower.contains("nethunter") ||
            lower.contains("nhkex") ||
            lower.contains("droidvnc") ||
            lower.contains("musically") ||
            lower.contains("tiktok") ||
            lower.contains("trill") ||
            (lower.contains("youtube") && !lower.contains("music")) ||
            lower.contains("reddit")
    }
}
