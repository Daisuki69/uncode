package com.uncode.app;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Hardcoded blacklist of distracting applications.
 * Strictly prohibited from being whitelisted during system lockdown.
 */
public final class BlacklistConstants {

    private BlacklistConstants() {}

    public static final Set<String> HARDCODED_BLACKLISTED_PACKAGES = new HashSet<>(Arrays.asList(
        // ── KissKH, Asian Dramas & Anime Streaming ──
        "id.kisskh.twa",                       // KissKH TWA
        "com.kisskh.app",                      // KissKH App
        "com.kissasian.app",                   // KissAsian
        "tv.danmaku.bili",                     // Bilibili (Mainland China)
        "com.bstar.intl",                      // Bilibili Global / SEA (Play Store)
        "com.bilibili.app.in",                 // Bilibili Global / India
        "tv.danmaku.bilibilihd",               // Bilibili HD (Tablets)
        "com.bilibili.comic",                  // Bilibili Comics
        "com.bilibili.comic.intl",             // Bilibili Comics International
        "com.bilibili.studio",                 // Bilibili Studio
        "com.iqiyi",                           // iQIYI
        "com.iqiyi.i18n",                      // iQIYI International
        "com.qiyi.video",                      // iQIYI China
        "com.tencent.qqlivei18n",              // WeTV (Tencent Video Global)
        "com.tencent.qqlive",                  // Tencent Video
        "com.vuclip.viu",                      // Viu: Dramas, TV Shows & Movies
        "com.viki.android",                    // Viki: Asian Dramas & Movies
        "com.youku.international",             // Youku International
        "com.youku.phone",                     // Youku China
        "com.hunantv.imgo.activity",           // Mango TV
        "com.mgtv.tv",                         // Mango TV International
        "com.crunchyroll.crunchyroid",         // Crunchyroll
        "com.funimation.funimationnow",        // Funimation
        "com.hidive.android",                  // HIDIVE
        "tv.cinedigm.retrocrush",              // RetroCrush Anime

        // ── Unofficial / Piracy / Third-party Streaming ──
        "com.artem.scotepio",                  // Loklok - Dramas & Movies
        "com.darmiu.folasia",                  // Loklok - Pocket Dramas
        "com.tarparos.phigaea",                // Loklok Variant
        "com.lagradost.cloudstream3",          // CloudStream
        "com.movieboxpro.android",             // MovieBoxPro
        "com.anilab.android",                  // Anilab
        "com.anilab.animtvappr",               // Anilab TV
        "com.stremio.one",                     // Stremio
        "com.onstream.app",                    // OnStream
        "com.popcorntime",                     // Popcorn Time

        // ── Webtoons, Manga & Light Novels ──
        "com.naver.linewebtoon",               // Webtoon
        "com.tapastic",                        // Tapas Comics & Novels
        "com.kakao.page",                      // KakaoPage
        "com.kakaowebtoon.app",                // Kakao Webtoon
        "jp.co.shueisha.mangaplus",            // Manga Plus by Shueisha
        "com.contentsfirst.tappytoon",         // Tappytoon
        "eu.kanade.tachiyomi",                 // Tachiyomi
        "app.mihon",                           // Mihon
        "eu.kanade.tachiyomi.anime",           // Aniyomi
        "app.aniyomi",                         // Aniyomi

        // ── Micro-Drama & Vertical Short Video Series ──
        "com.newleaf.app.android.victor",      // ReelShort
        "com.storymatrix.drama",               // DramaBox
        "live.shorttv.apps",                   // ShortMax
        "com.goodreels.app",                   // GoodShort
        "com.chao.novel.moboreels",            // MoboReels
        "com.topshort.video",                  // TopShort

        // ── Global & Western Video Streaming / OTT ──
        "com.netflix.mediaclient",             // Netflix
        "com.netflix.ninja",                   // Netflix Android TV
        "com.disney.disneyplus",               // Disney+
        "in.startv.hotstar",                   // Disney+ Hotstar
        "tv.twitch.android.app",               // Twitch
        "com.kick.app",                        // Kick Streaming
        "com.amazon.avod.thirdpartyclient",    // Prime Video
        "com.amazon.amazonvideo.livingroom",   // Prime Video Android TV
        "com.hulu.plus",                       // Hulu
        "com.wbd.stream",                      // Max (HBO)
        "com.hbo.hbonow",                      // HBO Now
        "com.hbo.gobro",                       // HBO GO
        "com.peacocktv.peacockandroid",        // Peacock TV
        "com.cbs.app",                         // Paramount+
        "com.cbs.ott",                         // Paramount+ OTT
        "com.apple.atve.androidtv.appletv",    // Apple TV
        "com.tubitv",                          // Tubi TV
        "tv.pluto.android",                    // Pluto TV
        "com.dailymotion.videoplayer",         // Dailymotion
        "com.vimeo.android.videoapp",          // Vimeo
        "com.plexapp.android",                 // Plex
        "com.jio.media.ondemand",              // JioCinema
        "com.graymatrix.did",                  // Zee5
        "com.sonyliv",                         // SonyLIV
        "com.mxtech.videoplayer.ad",           // MX Player

        // ── Lucky Patcher, Cracking & Game Mod Tools ──
        "com.chelpus.luckypatcher",            // Lucky Patcher
        "ru.chelpus.patcher",                  // Lucky Patcher Installer
        "com.forpda.lp",                       // Lucky Patcher 4PDA
        "com.dimonvideo.luckypatcher",         // Lucky Patcher DimonVideo
        "catch_.me_.if_.you_.can_",            // GameGuardian
        "com.gameguardian",                    // GameGuardian clone
        "org.sbtools.gamehack",                // SB Game Hacker
        "org.cree.creehack",                   // Creehack
        "madkite.freedom",                     // Freedom APK
        "cc.madkite.freedom",                  // Freedom APK Alternative

        // ── Modded App Stores & Cloners ──
        "com.happymod.apk",                    // HappyMod
        "cm.aptoide.pt",                       // Aptoide
        "net.appx.acmarket",                   // ACMarket
        "org.mobilism.android",                // Mobilism
        "com.androeed",                        // Androeed
        "com.tutuapp.android",                 // TutuApp
        // ── Parallel Space, Virtual Containers & App Cloners (Milestone 17) ──
        "com.lbe.parallel.intl",               // Parallel Space (Global)
        "com.lbe.parallel.intl.arm64",         // Parallel Space 64-bit
        "com.lbe.parallel.intl.arm32",         // Parallel Space 32-bit
        "com.lbe.parallel.parallel",           // Parallel Space Classic
        "com.lbe.parallel.pro",                // Parallel Space Pro
        "com.lbe.parallel.pro.arm64",          // Parallel Space Pro 64-bit
        "com.lbe.doubleinstance",              // Parallel Space Engine
        "com.parallel.space.lite",             // Parallel Space Lite
        "com.parallel.space.pro",              // Parallel Space Pro Standalone
        "com.trendmicro.tpacket.parallellite", // Parallel Lite
        "com.excelliance.dualaid",             // Dual Space
        "com.excelliance.dualaid.arm64",       // Dual Space 64-bit
        "com.excelliance.dualaid.arm32",       // Dual Space 32-bit
        "com.excelliance.multiaccounts",       // Multi Accounts
        "clone.app.dualspace",                 // Dual Space Clone
        "com.dualspace.multispace.android",    // Multi Space
        "com.dualspace.multispace.arm64",      // Multi Space 64-bit
        "com.dualspace.multispace.arm32",      // Multi Space 32-bit
        "com.polestar.super.clone",            // Super Clone
        "com.polestar.clone.parallel.dual.multiple.accounts", // Clone App - Parallel Space
        "com.twoaccounts.parallelapp",         // 2Accounts
        "multi.parallel.dualspace.cloner",     // Multi Parallel
        "com.clone.android.dual.space",        // Clone Space
        "com.jumobile.multiapp",               // Multiple Accounts
        "com.applisto.appcloner",              // App Cloner
        "com.applisto.appcloner.premium",      // App Cloner Premium
        "com.oasisfeng.island",                // Island (Work Profile Cloner)
        "net.typeblog.shelter",                // Shelter (Work Profile Sandbox)
        "com.vmos.pro",                        // VMOS Pro Virtual Android
        "com.vmos.app",                        // VMOS Lite
        "com.vphonegaga.titan",                // VPhoneGaGa Virtual Machine
        "com.f1player",                        // F1VM Virtual Android
        "com.gspace.android",                  // GSpace Virtual Container
        "io.va.exposed",                       // VirtualApp Runtime
        "com.x8zs.sandbox",                    // X8 Sandbox
        "com.samsung.knox.securefolder",       // Samsung Secure Folder
        "com.sec.knox.switcher",               // Knox Container Switcher
        "com.sec.knox.app.container",          // Knox App Container
        "com.samsung.android.knox.containercore", // Knox Core Container
        "com.miui.secondspace",                // Xiaomi Second Space
        "com.miui.securityadd.insspace",       // Xiaomi Second Space Manager

        // ── Modded Social Media & Clients ──
        "com.instaprime.android",              // InstaPrime
        "com.instander.android",               // Instander
        "com.aeroinsta.android",               // AeroInsta
        "com.honista.app",                     // Honista
        "app.revanced.android.youtube",        // YouTube ReVanced
        "app.rvx.android.youtube",             // ReVanced Extended
        "org.schabi.newpipe",                  // NewPipe
        "com.teamsmart.videomanager.tv",       // SmartTube
        "com.vanced.android.youtube",          // Vanced (Legacy)
        "com.gbwhatsapp",                      // GBWhatsApp
        "com.fmwhatsapp",                      // FMWhatsApp
        "com.yowhatsapp",                      // YoWhatsApp
        "com.aerowhatsapp",                    // Aero WhatsApp
        "org.telegram.plus",                   // Plus Messenger

        // ── Social Media & Short-Form Video ──
        "com.zhiliaoapp.musically",            // TikTok (Global)
        "com.ss.android.ugc.trill",            // TikTok (Asia / Alternative)
        "com.ss.android.ugc.aweme",            // Douyin
        "com.ss.android.ugc.aweme.lite",       // Douyin Lite
        "com.kwai.video",                      // Kwai
        "com.smile.gifmaker",                  // Kuaishou
        "com.kuaishou.nebula",                 // Kuaishou Lite
        "video.like",                          // Likee
        "com.lemon.lvoverseas",                // CapCut (Global)
        "com.lemon.lv",                        // CapCut (China)
        "com.instagram.android",               // Instagram
        "com.instagram.lite",                  // Instagram Lite
        "com.instagram.barcelona",             // Threads
        "com.google.android.youtube",          // YouTube Main App
        "com.google.android.apps.youtube.kids",// YouTube Kids
        "com.google.android.apps.youtube.creator", // YouTube Studio
        "com.snapchat.android",                // Snapchat
        "com.facebook.katana",                 // Facebook
        "com.facebook.lite",                   // Facebook Lite
        "com.twitter.android",                 // X / Twitter
        "com.twitter.android.lite",            // X / Twitter Lite
        "com.reddit.frontpage",                // Reddit
        "com.rubenmayayo.reddit",              // Infinity for Reddit
        "me.ccrama.redditslide",               // Slide for Reddit
        "com.pinterest",                       // Pinterest
        "com.tumblr",                          // Tumblr
        "com.bereal.ft",                       // BeReal
        "xyz.blueskyweb.app",                  // Bluesky
        "org.joinmastodon.android",            // Mastodon
        "com.sina.weibo",                      // Weibo
        "com.xingin.xhs",                      // Xiaohongshu (RED)
        "com.bd.nproject",                     // Lemon8
        "com.narvii.amino.master",             // Amino
        "com.ninegag.android.app",             // 9GAG
        "com.imgur.mobile",                    // Imgur
        "mobi.ifunny",                         // iFunny

        // ── Gaming & Platforms ──
        "com.roblox.client",                   // Roblox
        "com.discord",                         // Discord
        "com.valvesoftware.android.steam.community", // Steam
        "com.mihoyo.genshinimpact",            // Genshin Impact
        "com.cognosphere.genshinimpact.oversea", // Genshin Impact Global
        "com.hoyoverse.hkrpgoversea",           // Honkai: Star Rail
        "com.mihoyo.hkrpg",                    // Honkai: Star Rail CN
        "com.hoyoverse.nap",                   // Zenless Zone Zero Global
        "com.cognosphere.nap.oversea",         // Zenless Zone Zero Oversea
        "com.tencent.ig",                      // PUBG Mobile
        "com.pubg.krmobile",                   // PUBG Mobile KR
        "com.pubg.imobile",                    // BGMI
        "com.vng.pubgmobile",                  // PUBG Mobile VN
        "com.dts.freefireth",                  // Free Fire
        "com.dts.freefiremax",                 // Free Fire Max
        "com.activision.callofduty.shooter",   // Call of Duty: Mobile
        "com.activision.callofduty.warzone",   // Call of Duty: Warzone Mobile
        "com.mobile.legends",                  // Mobile Legends: Bang Bang
        "com.riotgames.league.wildrift",       // League of Legends: Wild Rift
        "com.riotgames.league.teamfighttactics", // Teamfight Tactics
        "com.garena.game.kgid",                // Arena of Valor
        "com.supercell.brawlstars",            // Brawl Stars
        "com.supercell.clashofclans",          // Clash of Clans
        "com.supercell.clashroyale",           // Clash Royale
        "com.supercell.squad",                 // Squad Busters
        "com.supercell.hayday",                // Hay Day
        "com.supercell.boombeach",             // Boom Beach
        "com.king.candycrushsaga",             // Candy Crush Saga
        "com.king.candycrushsodasaga",         // Candy Crush Soda Saga
        "com.king.farmheroessaga",             // Farm Heroes Saga
        "com.kiloo.subwaysurf",                // Subway Surfers
        "com.imangi.templerun",                // Temple Run
        "com.imangi.templerun2",               // Temple Run 2
        "com.innersloth.spacemafia",           // Among Us
        "com.mojang.minecraftpe",              // Minecraft
        "com.ea.gp.fifamobile",                // EA FC Mobile
        "jp.konami.pesam",                     // eFootball
        "com.nianticlabs.pokemongo",           // Pokémon GO
        "jp.pokemon.pokemonunite",             // Pokémon UNITE
        "coxeta",                              // Coxeta Rhythm Game
        "com.taptap",                          // TapTap
        "com.taptap.global",                   // TapTap Global
        "com.epicgames.portal",                // Epic Games Store
        "org.ppsspp.ppsspp",                   // PPSSPP
        "org.ppsspp.ppssppgold",               // PPSSPP Gold
        "com.retroarch",                       // RetroArch
        "org.dolphinemu.dolphinemu",           // Dolphin Emulator
        "xyz.aethersx2.android",               // AetherSX2

        // ── Shopping & E-Commerce ──
        "com.shopee.ph",                       // Shopee PH
        "com.shopee.id",                       // Shopee ID
        "com.shopee.my",                       // Shopee MY
        "com.shopee.sg",                       // Shopee SG
        "com.shopee.th",                       // Shopee TH
        "com.shopee.vn",                       // Shopee VN
        "com.shopee.tw",                       // Shopee TW
        "com.shopee.br",                       // Shopee BR
        "com.lazada.android",                  // Lazada
        "com.amazon.mshop.android.shopping",   // Amazon Shopping
        "com.zzkko",                           // Shein
        "com.einnovation.temu",                // Temu
        "com.alibaba.aliexpresshd",            // AliExpress
        "com.taobao.taobao",                   // Taobao
        "com.ebay.mobile",                     // eBay
        "com.tokopedia.tkpd",                  // Tokopedia
        "com.bukalapak.android",               // Bukalapak
        "com.mercadolibre",                    // Mercado Libre

        // ── Virtual OS, Containers & Sandbox Bypass Tools ──
        "com.vmos.app",                        // VMOS Virtual Android
        "com.vmos.pro",                        // VMOS Pro
        "com.vmos.vmospro",                    // VMOS Pro
        "com.f1player.f1vm",                   // F1 VM
        "com.f1vm.android",                    // F1 VM Android
        "com.vphonegaga.titan",                // VPhoneGaGa
        "com.x8zs.sandbox",                    // X8 Sandbox
        "com.oasisfeng.island",                // Island (Work profile bypass)
        "net.typeblog.shelter",                // Shelter (Work profile bypass)
        "bin.mt.plus",                         // MT Manager (APK reverse engineering/cloning)
        "com.mcal.np",                         // NP Manager
        "ru.maximoff.apktool",                 // Apktool M
        "com.google.android.apps.apkeditor",   // APK Editor

        // ── Fanfic, Web Novels & Light Novels ──
        "wp.wattpad",                          // Wattpad
        "com.qidian.Int.reader",               // Webnovel
        "mobi.mangatoon.novel",                // MangaToon
        "mobi.mangatoon.noveltoon",            // NovelToon
        "com.wuxiaworld.mobile",               // Wuxiaworld
        "com.shosetsu.android",                // Shosetsu

        // ── Live Video Feeds & Stranger Chats ──
        "video.chat.ometv",                    // OmeTV
        "com.duoduo.ometv",                    // OmeTV Alternative
        "sg.bigo.live",                        // Bigo Live
        "com.sgiggle.production",              // Tango
        "com.machipopo.media17",               // 17LIVE
        "co.yubo.mobile",                      // Yubo

        // ── Dating & Swiping Apps ──
        "com.tinder",                          // Tinder
        "com.bumble.app",                      // Bumble
        "co.hinge.app",                        // Hinge
        "com.badoo.mobile",                    // Badoo
        "com.zenmen.omi",                      // Omi
        "com.grindrapp.android",               // Grindr

        // ── Gambling & Sports Betting ──
        "com.draftkings.dknative",             // DraftKings
        "com.fanduel.sportsbook",              // FanDuel
        "com.pokerstars.android",              // PokerStars
        "com.bet365Wrapper.Bet365",            // Bet365
        "com.xbet.android",                    // 1xBet
        "com.stake.mobile",                    // Stake

        // ── Resale & Bidding ──
        "com.thecarousell.Carousell",           // Carousell
        "fr.vinted",                           // Vinted
        "io.depop",                            // Depop
        "com.xunmeng.pinduoduo",               // Pinduoduo

        // ── Hyper-Casual, .IO & Satisfying Time-Wasters ──
        "io.voodoo.holeio",                    // Hole.io
        "com.furunwang.woodturning",           // Woodturning 3D
        "com.voodoo.woodturning",              // Woodturning 3D (Voodoo)
        "io.voodoo.paperio",                   // Paper.io
        "io.voodoo.paper2",                    // Paper.io 2
        "com.h8games.helixjump",               // Helix Jump
        "io.voodoo.crowdcity",                 // Crowd City
        "com.cassette.aquapark",               // Aquapark.io
        "io.voodoo.flappydunk",                // Flappy Dunk
        "air.com.hypah.io.slither",            // Slither.io
        "com.miniclip.agar.io",                // Agar.io
        "com.miniclip.diep.io",                // Diep.io
        "com.amelosinteractive.snake",         // Snake.io
        "com.gorilla.boltrend.survivorio",     // Survivor!.io
        "com.habby.survivorio",                // Survivor!.io
        "com.habby.archero",                   // Archero
        "com.freeplay.clash3d",                // Join Clash 3D
        "com.superpow.snake",                  // Join Clash 3D Alternative
        "com.garawell.bridgerace",             // Bridge Race
        "com.pronetis.ironball",               // Going Balls
        "com.supersonic.tallmanrun",           // Tall Man Run
        "com.kitkagames.fallbuddies",          // Stumble Guys
        "com.amanotes.pamadiscodancing",       // Tiles Hop: EDM Rush!
        "com.youmusic.magictiles",             // Magic Tiles 3
        "com.fortafygames.colorswitch",        // Color Switch
        "com.yodo1.crossyroad",                // Crossy Road
        "com.halfbrick.fruitninjafree",        // Fruit Ninja
        "com.zeptolab.ctr.ads",                // Cut the Rope
        "com.easybrain.block.puzzle.game",     // Blockudoku
        "com.androbaby.game2048",              // 2048
        "com.ketchapp.stack",                  // Stack
        "com.ketchapp.knifehit",               // Knife Hit
        "com.ketchapp.rider",                  // Rider
        "com.mhappsgaming.sandballs",          // Sand Balls
        "com.saygames.johnnytrigger",          // Johnny Trigger
        "com.redfox.myhotel",                  // My Perfect Hotel
        "com.saygames.racemaster",             // Race Master 3D
        "com.rolllic.hairchallenge",           // Hair Challenge
        "com.zynga.highheels",                 // High Heels!
        "com.crazylabs.tiedye",                // Tie Dye
        "com.crazylabs.asmr.slicing",          // ASMR Slicing
        "com.crazylabs.soapcutting",           // Soap Cutting
        "com.lionstudios.happyglass",          // Happy Glass
        "com.lionstudios.savethegirl",         // Save The Girl
        "com.lionstudios.mrbullet",            // Mr Bullet
        "com.lionstudios.pullhimout"           // Pull Him Out
    ));

    public static boolean isBlacklisted(String packageName) {
        if (packageName == null) return false;
        String lower = packageName.trim().toLowerCase();

        // Explicitly allow system package installers per user instruction
        if (lower.contains("packageinstaller")) {
            return false;
        }

        if (HARDCODED_BLACKLISTED_PACKAGES.contains(lower)) {
            return true;
        }
        // Heuristic substring signature checks to automatically block unlisted clones, forks, and modded APKs
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
               lower.contains("bilibili") ||
               lower.contains("danmaku.bili") ||
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
               lower.contains("wattpad") ||
               lower.contains("webnovel") ||
               lower.contains("wuxiaworld") ||
               lower.contains("ometv") ||
               lower.contains("bigo") ||
               lower.contains("tinder") ||
               lower.contains("bumble") ||
               lower.contains("pokerstars") ||
               lower.contains("bet365") ||
               lower.contains("draftkings") ||
               lower.contains("fanduel") ||
               lower.contains("carousell") ||
               lower.contains("vinted") ||
               lower.contains("depop") ||
               lower.contains("holeio") ||
               lower.contains("woodturning") ||
               lower.contains("paperio") ||
               lower.contains("helixjump") ||
               lower.contains("crowdcity") ||
               lower.contains("aquapark") ||
               lower.contains("slither") ||
               lower.contains("agar.io") ||
               lower.contains("snake.io") ||
               lower.contains("survivorio") ||
               lower.contains("bridgerace") ||
               lower.contains("stumbleguys") ||
               lower.contains("voodoo") ||
               lower.contains("saygames") ||
               lower.contains("lionstudios") ||
               lower.contains("crazylabs") ||
               lower.contains("ketchapp") ||
               lower.contains("coxeta");
    }
}
