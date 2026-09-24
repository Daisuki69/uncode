package com.uncode.app

import java.util.Locale

/**
 * Registry of explicitly known distracting applications (Milestone / Flow Update V3).
 *
 * Implements a pure package-list architecture for Stage 2 Initial App Policy evaluation (MSG_GATE):
 * - Video & Entertainment Streaming (YouTube, Netflix, Twitch, Disney+, Hulu, TikTok, etc.)
 * - Social Media Feeds & Forums (Instagram, Twitter/X, Facebook feeds, Reddit, Threads, etc.)
 * - Short Dramas, Web Novels, Manga & Comics (ReelShort, DramaBox, ShortMax, Webtoon, etc.)
 * - Video Streaming Aggregators & Piracy (Stremio, OnStream, Loklok, KissKH, etc.)
 * - Top Mobile Games & Gaming Communities (Mobile Legends, Genshin, PUBG, Roblox, Steam, etc.)
 * - Remote Desktop & Screen Mirror Bypasses (TeamViewer, AnyDesk, RustDesk, Parsec, Vysor, etc.)
 * - Sandboxes, Cloners, Game Cheats & Hidden Vaults (Parallel Space, Dual Space, VMOS, Lucky Patcher, etc.)
 *
 * Interoperates with MSG_GATE3:
 * - If in KnownDistracting AND in KnownSafe (user-configured UI whitelist / YouTube policy) -> ALLOW
 * - If in KnownDistracting AND NOT in KnownSafe -> BLOCK
 * - If NOT in KnownDistracting AND in KnownSafe -> ALLOW
 * - If NOT in KnownDistracting AND NOT in KnownSafe -> Defer to Secondary App Classifier (heuristics)
 */
object KnownDistracting {

    @JvmField
    val KNOWN_DISTRACTING_PACKAGES: Set<String> = hashSetOf(
        // ── Video & Entertainment Streaming ──
        "com.google.android.youtube",
        "com.google.android.youtube.tv",
        "com.google.android.apps.youtube.unplugged", // YouTube TV
        "com.google.android.apps.youtube.kids",      // YouTube Kids
        "app.revanced.android.youtube",
        "org.schabi.newpipe",
        "app.libre_tube",
        "org.polymc.tubular",
        "com.netflix.mediaclient",
        "com.netflix.ninja",
        "tv.twitch.android.app",
        "com.disney.disneyplus",
        "com.hulu.plus",
        "com.amazon.avod.thirdpartyclient",          // Prime Video
        "com.wbd.stream",                            // Max / HBO Max
        "com.hbo.hbonow",
        "com.crunchyroll.crunchyroll",
        "com.bilibili.app.in",
        "tv.danmaku.bili",
        "com.zhiliaoapp.musically",                  // TikTok (Global)
        "com.ss.android.ugc.trill",                  // TikTok (Southeast Asia / Variant)
        "com.ss.android.ugc.aweme",                  // Douyin
        "com.kwai.video",                            // Kuaishou / Kwai
        "video.like",                                // Likee

        // ── AI Chatbots & Virtual Assistants ──
        "com.google.android.apps.gemini",            // Google Gemini
        "com.google.android.apps.bard",
        "com.openai.chatgpt",                        // ChatGPT / OpenAI
        "com.anthropic.claude",                      // Claude AI
        "ai.perplexity.app",                         // Perplexity AI
        "ai.perplexity.app.android",
        "com.microsoft.copilot",                     // Microsoft Copilot
        "com.poe.android",                           // Poe AI
        "com.quora.poe.android",
        "com.characterai.chat",                      // Character.ai
        "com.deepseek.chat",                         // DeepSeek
        "ai.inflection.pi",                          // Pi AI

        // ── Short-Drama Streaming & Web Novels / Manga / Comics ──
        "com.cd.shortdrama",                         // ReelShort
        "com.reelshort.android",
        "com.shortmax.android",                      // ShortMax
        "com.dramabox.app",                          // DramaBox
        "com.goodshort.drama",                       // GoodShort
        "com.snackshort.app",                        // SnackShort
        "com.moboreels.app",                         // MoboReels
        "com.netshort.app",                          // NetShort
        "com.shorttv.app",                           // ShortTV
        "com.kalostv.app",                           // Kalos TV
        "com.playlet.app",                           // Playlet
        "com.sereal.app",                            // Sereal+
        "com.naver.linewebtoon",                     // WEBTOON
        "org.mangadex.app",                          // MangaDex
        "com.kakao.page",                            // KakaoPage
        "com.tapas.mobile",                          // Tapas
        "wp.wattpad",                                // Wattpad
        "com.qidian.Int.reader",                     // Webnovel

        // ── Piracy Streaming, Scrapers & Aggregators ──
        "com.stremio.one",                           // Stremio
        "com.cloudstream.app",                       // CloudStream
        "com.onstream.app",                          // OnStream
        "com.movieboxpro.android",                   // MovieBoxPro
        "com.allinone.loklok",                       // Loklok
        "com.anilab.app",                            // Anilab
        "id.kisskh.twa",                             // KissKH PWA / TWA
        "app.revanced.android.youtube",              // ReVanced YouTube
        "app.revanced.android.apps.youtube.music",
        "org.schabi.newpipe",                        // NewPipe

        // ── Social Media Feeds & Discussion Forums ──
        "com.instagram.android",
        "com.instagram.barcelona",                   // Threads
        "com.facebook.katana",                       // Facebook Main Feed App
        "com.facebook.lite",                         // Facebook Lite Feed App
        "com.twitter.android",                       // X / Twitter
        "com.reddit.frontpage",                      // Reddit
        "com.pinterest",                             // Pinterest
        "com.tumblr",                                // Tumblr
        "com.snapchat.android",                      // Snapchat
        "xyz.blueskyweb.app",                        // Bluesky
        "org.joinmastodon.android",                  // Mastodon

        // ── Casual Dating & Random Video Chat ──
        "com.tinder",
        "com.bumble.app",
        "co.hinge.app",
        "com.badoo.mobile",
        "com.ometv.android",                         // OmeTV

        // ── Top Mobile Games & Gaming Communities ──
        "com.mobile.legends",                        // Mobile Legends: Bang Bang
        "com.miHoYo.GenshinImpact",                  // Genshin Impact
        "com.cognosphere.genshinimpact.oversea",     // Genshin Impact Global
        "com.HoYoverse.hkrpgoversea",                // Honkai: Star Rail
        "com.HoYoverse.Nap",                         // Zenless Zone Zero
        "com.tencent.ig",                            // PUBG Mobile
        "com.pubg.imobile",                          // Battlegrounds Mobile
        "com.pubg.newstate",                         // PUBG: New State
        "com.dts.freefireth",                        // Free Fire
        "com.dts.freefiremax",                       // Free Fire MAX
        "com.activision.callofduty.shopper",         // Call of Duty: Mobile
        "com.roblox.client",                         // Roblox
        "com.mojang.minecraftpe",                    // Minecraft
        "com.supercell.brawlstars",                  // Brawl Stars
        "com.supercell.clashofclans",                // Clash of Clans
        "com.supercell.clashroyale",                 // Clash Royale
        "com.supercell.hayday",                      // Hay Day
        "com.riotgames.league.wildrift",             // League of Legends: Wild Rift
        "com.riotgames.legendsofruneterra",          // Legends of Runeterra
        "com.kiloo.subwaysurf",                      // Subway Surfers
        "com.king.candycrushsaga",                   // Candy Crush Saga
        "com.nianticlabs.pokemongo",                 // Pokémon GO
        "com.valvesoftware.android.steam.community", // Steam Mobile
        "com.epicgames.portal",                      // Epic Games Store

        // ── Remote Desktop, Screen Share & VNC Workarounds ──
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
        "com.limelight",                             // Moonlight Game Streaming
        "com.valvesoftware.steamlink",
        "com.sand.airdroid",
        "com.sand.airmirror",
        "com.apowersoft.mirror",
        "com.koushikdutta.vysor",
        "info.dvkr.screenstream",
        "net.christianbeier.droidvnc_ng",
        "com.offsec.nethunter",
        "com.offsec.nethunter.kex",
        "com.offsec.nhkex",

        // ── Virtual Sandboxes, App Cloners, Game Cheats & Hidden Vaults ──
        "com.lbe.parallel.intl",                     // Parallel Space
        "com.lbe.parallel.intl.arm64",
        "com.excelliance.dualaid",                   // Dual Space
        "com.polestar.super.clone",                  // Super Clone
        "com.applisto.appcloner",                    // App Cloner
        "com.trendtech.multipleaccount",             // 2Accounts
        "com.vmos.pro",                              // VMOS
        "com.vmos.glb",
        "com.f1player",                              // F1 VM
        "com.vphonegaga.titan",                      // VPhoneGaGa
        "com.x8zs.sandbox",                          // X8 Sandbox
        "com.chelpus.lackypatch",                    // Lucky Patcher
        "catch_.me_.if_.you_.can_",                  // GameGuardian
        "com.topjohnwu.magisk"
    )

    /**
     * Primary fast-path lookup to determine if a package is an explicitly known distracting app.
     * Pure O(1) HashSet check on normalized package name.
     */
    @JvmStatic
    fun isKnownDistracting(packageName: String?): Boolean {
        if (packageName.isNullOrBlank()) return false
        val lower = packageName.trim().lowercase(Locale.ROOT)

        // 1. Direct O(1) HashSet match
        if (KNOWN_DISTRACTING_PACKAGES.contains(lower)) {
            return true
        }

        // 2. High-confidence package substring signatures
        return hasDistractingPackageSignature(lower)
    }

    /**
     * Extended lookup checking both package name and application label for known distraction patterns.
     */
    @JvmStatic
    fun isKnownDistracting(packageName: String?, appLabel: String?): Boolean {
        if (isKnownDistracting(packageName)) return true
        if (!appLabel.isNullOrBlank()) {
            val lowerLabel = appLabel.trim().lowercase(Locale.ROOT)
            if (hasDistractingLabelSignature(lowerLabel)) {
                return true
            }
        }
        return false
    }

    private fun hasDistractingPackageSignature(lower: String): Boolean {
        // Exclude legitimate audio players (like YouTube Music or Spotify) from being blocked as video/distraction
        if (lower.contains("music") || lower.contains("audio") || lower.contains("podcast")) {
            return false
        }

        return lower.contains(".musically.") ||
               lower.contains(".trill.") ||
               lower.contains(".aweme.") ||
               lower.contains(".dramabox.") ||
               lower.contains(".reelshort.") ||
               lower.contains(".shortmax.") ||
               lower.contains(".goodshort.") ||
               lower.contains(".snackshort.") ||
               lower.contains(".shortdrama.") ||
               lower.contains(".webnovel.") ||
               lower.contains(".wattpad.") ||
               lower.contains(".webtoon.") ||
               lower.contains(".stremio.") ||
               lower.contains(".cloudstream.") ||
               lower.contains(".onstream.") ||
               lower.contains(".loklok.") ||
               lower.contains(".kisskh.") ||
               lower.contains(".kissasian.") ||
               lower.contains(".game.") ||
               lower.contains(".games.") ||
               lower.contains(".gaming.") ||
               lower.contains(".casino.") ||
               lower.contains(".poker.") ||
               lower.contains(".slots.") ||
               lower.contains(".bet.") ||
               lower.contains(".cloner.") ||
               lower.contains(".dualspace.") ||
               lower.contains(".parallel.") ||
               lower.contains(".vmos.") ||
               lower.contains(".vphonegaga.") ||
               lower.contains(".f1player.") ||
               lower.contains(".teamviewer.") ||
               lower.contains(".anydesk.") ||
               lower.contains(".rustdesk.") ||
               lower.contains(".remotedesktop.") ||
               lower.contains(".screenshare.") ||
               lower.contains(".screenmirror.")
    }

    private fun hasDistractingLabelSignature(lowerLabel: String): Boolean {
        if (lowerLabel.contains("music") || lowerLabel.contains("audio") || lowerLabel.contains("podcast")) {
            return false
        }
        return lowerLabel.equals("tiktok") ||
               lowerLabel.equals("tik tok") ||
               lowerLabel.equals("douyin") ||
               lowerLabel.equals("instagram") ||
               lowerLabel.equals("reddit") ||
               lowerLabel.equals("threads") ||
               lowerLabel.equals("twitter") ||
               lowerLabel.equals("snapchat") ||
               lowerLabel.contains("short drama") ||
               lowerLabel.contains("reelshort") ||
               lowerLabel.contains("shortmax") ||
               lowerLabel.contains("dramabox") ||
               lowerLabel.contains("goodshort") ||
               lowerLabel.contains("snackshort") ||
               lowerLabel.contains("webtoon") ||
               lowerLabel.contains("manga") ||
               lowerLabel.contains("webnovel") ||
               lowerLabel.contains("stremio") ||
               lowerLabel.contains("kisskh") ||
               lowerLabel.contains("remote desktop") ||
               lowerLabel.contains("screen mirror") ||
               lowerLabel.contains("screen share")
    }
}
