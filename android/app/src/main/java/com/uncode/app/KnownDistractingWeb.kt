package com.uncode.app

import java.util.Locale

/**
 * Authoritative Single Source of Truth for Digital Web Distractions (Stage 2 Web Gate).
 *
 * Centralizes all digital dopamine traps:
 * 1. Social Media & Algorithmic Feeds (TikTok, Instagram, Twitter/X, Reddit, Threads, Twitch, etc.)
 * 2. YouTube Core & Streaming Domains (youtube.com, youtu.be, etc.)
 * 3. Web-Based Gaming Portals, .IO Arenas, Cloud APKs, Retro Emulators, and Unblocked Mirrors
 * 4. Online Gambling, Casinos, Sportsbooks, and Skin/Case Opening
 * 5. Adult, Pornography, Live Cams, Adult Subscriptions, and Erotica
 * 6. Web Proxies, CGI/PHP/Node Unblockers, and Bypass Tunnels
 * 7. Piracy Streaming, Manga/Manhwa, Webtoons, Torrents, and Vertical Short-Dramas
 * 8. Casual Dating, Hookups, and Random Video Cam Chat
 * 9. Viral Gossip, Clickbait, and Time-Wasters
 * 10. Crypto Meme Coin Speculation, Token Casinos, and DEX Scanners
 * 11. Suspicious Restricted gTLDs (.casino, .bet, .poker, .adult, .porn, .xxx, .sex, .cam)
 * 12. Browser Mini-Games (chrome://dino, edge://surf, opera://game, Google arcade doodles)
 * 13. Conversational AI Assistant Web Portals (governed strictly by Unified Policy)
 */
object KnownDistractingWeb {

    // ── 1. Social Media & Algorithmic Feeds ──
    @JvmField
    val SOCIAL_MEDIA_DOMAINS: Set<String> = hashSetOf(
        "tiktok.com", "byteoversea.com", "ibytedtos.com", "musical.ly",
        "douyin.com", "iesdouyin.com", "kuaishou.com", "kwai.com",
        "instagram.com", "cdninstagram.com",
        "facebook.com", "fb.com", "fbcdn.net", "fbsbx.com",
        "twitter.com", "x.com", "twimg.com", "t.co",
        "reddit.com", "redd.it", "redditmedia.com", "redditstatic.com",
        "threads.net",
        "snapchat.com", "sc-cdn.net",
        "discord.com", "discordapp.com", "discordapp.net",
        "twitch.tv", "ttvnw.net", "kick.com", "trovo.live",
        "netflix.com", "nflxvideo.net", "nflximg.net",
        "disneyplus.com", "hulu.com", "primevideo.com",
        "bilibili.tv", "bilibili.com",
        "9gag.com", "pinterest.com", "tumblr.com",
        "lemon8-app.com", "bereal.com", "bsky.app", "mastodon.social", "mastodon.online",
        "truthsocial.com", "cohost.org", "plurk.com",
        "quora.com", "imgur.com", "ifunny.co", "likee.video",
        "triller.co", "weibo.com", "weibo.cn", "xiaohongshu.com",
        "zhihu.com", "tieba.baidu.com", "douban.com",
        "vk.com", "ok.ru", "taringa.net", "funnyordie.com", "clubhouse.com",
        "vimeo.com", "dailymotion.com", "rumble.com", "odysee.com"
    )

    // ── 2. YouTube Core Domains ──
    @JvmField
    val YOUTUBE_DOMAINS: Set<String> = hashSetOf(
        "youtube.com", "youtu.be", "ytimg.com", "googlevideo.com", "youtube-nocookie.com"
    )

    // ── 3. Web-Based Gaming Portals & Arenas ──
    @JvmField
    val GAMING_DOMAINS: Set<String> = hashSetOf(
        // Mega Portals & Aggregators
        "poki.com", "poki-gdn.com", "poki.cz", "poki.nl", "poki.com.br", "poki.games", "poki-games.com", "unblocked-poki.com",
        "crazygames.com", "crazygames.co.uk", "crazygames.fr", "crazygames.io", "crazy-games.com",
        "coolmathgames.com", "coolmath-games.com", "coolmath.com", "coolmath4kids.com",
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
        "miniclip.com", "maxgames.com", "freeonlinegames.com", "b-games.com",
        "arcadespot.com", "playminigames.ru", "free80sarcade.com", "classicreload.com",
        "gdevelop.io",

        // Viral .IO & Multiplayer Arena Games
        "slither.io", "slitherio.org", "slitherplus.io", "wormate.io", "wormax.io",
        "agar.io", "agar.pro", "agariogame.club", "agar.bio", "agarz.com",
        "diep.io", "krunker.io", "yendis.ch",
        "1v1.lol", "1v1.school", "justfall.lol",
        "paper.io", "paper-io.com", "paperio2.com",
        "hole.io", "hole-io.com",
        "surviv.io", "survev.io", "suroi.io", "survivio.org",
        "skribbl.io", "gartic.io", "garticphone.com", "drawasaurus.org",
        "shellshock.io", "eggcombat.com", "shellshockers.io", "shellshockers.com",
        "deeeep.io", "bloxd.io", "voxiom.io", "smashkarts.io", "smashkarts.com",
        "ev.io", "zombs.io", "zombsroyale.io", "starve.io", "moomoo.io",
        "narrow.one", "venge.io", "bonk.io", "bonk2.io",
        "wings.io", "brutal.io", "splix.io", "lordz.io",
        "flyordie.io", "evojaws.io", "evoworld.io",
        "digdig.io", "yohoho.io", "taming.io", "betrayal.io",
        "battledudes.io", "lolbeans.io", "warbrokers.io", "repuls.io", "kirka.io", "miniroyale2.io",
        "curvefever.pro", "curvefever.com", "littlebigsnake.com", "arrow.io",
        "polytrack.io", "cubeshot.io", "bulletforce.org", "combat-online.com",
        "iogames.space", "iogames.onl", "io-games.io",

        // Cloud Gaming & Web APK Streaming
        "now.gg", "nowgg.me", "nowgg.io",
        "play.geforcenow.com", "geforcenow.com",
        "luna.amazon.com",
        "boosteroid.com", "cloud.boosteroid.com",
        "shadow.tech", "vortex.gg", "airgpu.com",
        "x.bluestacks.com",

        // Indie Web Runtimes & CDNs
        "itch.zone", "itch.io", "gamejolt.com", "gx.games",
        "simmer.io", "lexaloffle.com", "flowlab.io", "arcade.construct.net",

        // Web Emulators & Retro Gaming
        "emulatoronline.com", "retrogames.cc", "playretrogames.com", "vimm.net",
        "emupedia.net", "emupedia.org", "afterplay.io", "eclipseemu.me",
        "webretro.org", "game-oldies.com", "ssega.com", "playminigames.net",
        "playclassic.games", "dosgames.com", "playdosgames.com",
        "online-emulators.com", "retrogames.onl", "myabandonware.com",
        "consoleroms.com", "archaic-bingo.com", "wowroms.com", "freeroms.com",
        "romsgames.net", "emulatorgames.net", "retrostic.com", "playretro.ca",
        "emulator.games", "retrogames.cz", "retrogames.fun", "retro-games.cc",

        // Unblocked Dedicated Networks & Titles
        "unblocked-games.com", "unblockedgames66.com", "unblockedgames66plus.com", "unblockedgames66ez.com",
        "unblockedgames76.com", "unblockedgames77.com", "unblockedgames99.com",
        "unblockedgames500.com", "unblockedgames119.com", "unblockedgames24h.com",
        "classroom6x.com", "classroom-6x.org",
        "slope-game.com", "slopeunblocked.org", "slopegame.online", "slopegame.io",
        "hoodamath.com", "mathplayground.com", "abcya.com", "primarygames.com",
        "unblocked-games-76.com", "unblockedgame76.com", "unblockedgamesworld.com", "unblockedgamespod.com",
        "unblockedgames.me", "unblocked-games-s.com", "unblockedgame.io", "unblockedgamesfree.com",
        "tyronesunblockedgames.com", "tyroneunblockedgames.com", "tyronesgames.com",
        "unblocked-games-world.com", "unblockedgames911.com", "unblockedgames1000.com",
        "unblockedgames88.com", "unblockedgamespremium.com", "ezpzunblockedgames.com",
        "unblocked66.com", "unblocked76.com", "unblocked77.com", "unblocked99.com",
        "retrobowl.me", "retrobowlonline.net", "retrobowl25.com", "retrobowlcollege.net",
        "geometrydash.io", "geometrydashlite.online", "geometrydashlite.io", "geometry-dash.co",
        "drift-hunters.com", "drift-hunters.io", "driftboss.io", "driftboss.net",
        "motox3m.co", "moto-x3m.net", "motox3m.org", "moto-x3m.co",
        "subwaysurfers.com", "subway-surfers.org", "subwaysurfersgame.online",
        "crossyroad.com", "crossy-road.online", "flappybird.ee", "flappybird.io",
        "run3.io", "run3unblocked.com", "run3online.org", "run3.app",
        "papas-games.com", "flipline.com", "fliplinegames.com", "papasfreezeria.net", "papaspizzeria.net",
        "happywheels.com", "totaljerkface.com", "happywheelsgame.org",
        "stickmanhook.com", "stickmanhook.org", "stickman-hook.online",
        "bitlifeonline.com", "bitlife.com", "bitlifeonline.io",
        "idlebreakout.com", "idle-breakout.com", "cookieclicker.ee", "cookie-clicker2.com", "cookieclicker2.com",
        "doodle-jump.com", "doodlejump.io", "cuttherope.net", "cut-the-rope.online",
        "bad-ice-cream.com", "badicecream.org", "fireboyandwatergirl.com", "fireboywatergirl.io",
        "madalin-stunt-cars.com", "madalinstuntcars2.com", "madalinstuntcars3.io",
        "basket-random.com", "soccer-random.com", "volley-random.com", "boxing-random.com",
        "basketbros.io", "footballbros.io", "soccerbros.io",

        // Casual, Board & Puzzle Gaming
        "chess.com", "lichess.org", "chess24.com", "chessbomb.com",
        "geoguessr.com", "worldle.teuteuf.fr", "globle-game.com", "geoguess.games", "city-guesser.com",
        "2048game.com", "play2048.co", "2048.io",
        "sudoku.com", "websudoku.com", "nonograms.org",
        "wordlewebsite.com", "wordle.org", "quordle.com", "octordle.com", "sedecordle.com",
        "solitaired.com", "cardgames.io", "solitaireparadise.com", "247solitaire.com",
        "sporcle.com", "jetpunk.com", "tetr.io", "jstris.jezevec10.com",
        "roblox.com", "rbxcdn.com", "steamcommunity.com", "store.steampowered.com",
        "epicgames.com", "riotgames.com", "battle.net"
    )

    // ── 4. Online Gambling, Casinos & Skin Betting ──
    @JvmField
    val GAMBLING_DOMAINS: Set<String> = hashSetOf(
        "stake.com", "stake.us", "stake.bet", "stake.games", "stake.bz", "stake.ceo",
        "roobet.com", "roobet.fun", "roobet.gg",
        "bet365.com", "bet365.es", "bet365.gr", "bovada.lv", "betonline.ag",
        "pokerstars.com", "pokerstars.eu", "draftkings.com", "fanduel.com", "betmgm.com", "caesars.com",
        "wynnbet.com", "betway.com", "1xbet.com", "parimatch.com", "betfair.com", "betfair.it",
        "paddypower.com", "williamhill.com", "unibet.com", "unibet.fr", "unibet.co.uk",
        "bwin.com", "bwin.de", "ggbet.com",
        "betsson.com", "betsafe.com", "nordicbet.com", "spinpalace.com", "jackpotcity.com",
        "ignitioncasino.eu", "slots.lv", "cafecasino.lv", "mybookie.ag", "sportsbetting.ag",
        "cloudbet.com", "bitstarz.com", "bc.game", "7bitcasino.com", "fortunejack.com",
        "crypto-games.net", "csgoempire.com", "csgoroll.com", "hellcase.com", "gamdom.com",
        "duelbits.com", "rollbit.com", "rollbit.org", "bitsler.com", "primedice.com",
        "datdrop.com", "farmskins.com", "key-drop.com", "rustclash.com", "clash.gg",
        "shuffle.com", "rainbet.com", "flush.com", "luckyblock.com", "megadice.com",
        "sportingbet.com", "ladbrokes.com", "coral.co.uk", "skybet.com", "betfred.com",
        "boylesports.com", "betclic.com", "winamax.fr", "snai.it", "sisal.it",
        "eurobet.it", "bet9ja.com", "sportybet.com", "hollywoodbets.net", "superbet.com",
        "dafabet.com", "m88.com", "fun88.com", "w88.com", "sbobet.com", "12bet.com",
        "bk8.com", "bodog.com", "caliente.mx", "betano.com", "chumbacasino.com",
        "luckylandslots.com", "pulsz.com", "wowvegas.com", "high5casino.com",
        "888casino.com", "888poker.com", "888sport.com", "888.com",
        "partycasino.com", "partypoker.com", "leovegas.com", "casumo.com", "mrgreen.com",
        "interwetten.com", "tipico.de", "bet-at-home.com", "10bet.com", "marathonbet.com",
        "pinnacle.com", "vave.com", "trustdice.win", "metaspins.com", "jackbit.com",
        "wild.io", "gamblezen.com", "bitcasino.io", "sportsbet.io",
        "csgoclicker.com", "dmarket.com", "skinsmonkey.com", "skinport.com"
    )

    // ── 5. Adult, Explicit & NSFW ──
    @JvmField
    val ADULT_DOMAINS: Set<String> = hashSetOf(
        "pornhub.com", "xvideos.com", "xvideos2.com", "xnxx.com", "xnxx2.com",
        "xhamster.com", "xhamster.desi", "xhamster1.desi", "redtube.com", "redtube.net",
        "youporn.com", "spankbang.com", "spankbang.party", "tube8.com", "tube8.es",
        "beeg.com", "drtuber.com", "tnaflix.com", "nuvid.com", "thumbzilla.com",
        "chaturbate.com", "chaturbate.to", "chaturbate.vip", "stripchat.com", "stripchat.im",
        "bongacams.com", "camsoda.com", "cam4.com", "cam4.es", "cams.org", "livejasmin.com",
        "flirt4free.com", "imlive.com", "myfreecams.com", "cams.com", "streamate.com",
        "jerkmate.com", "cammodels.com",
        "onlyfans.com", "fansly.com", "loyalfans.com", "manyvids.com", "justforfans.com", "modelhub.com",
        "nhentai.net", "hanime.tv", "hentaihaven.xxx", "rule34.xxx", "rule34video.com", "gelbooru.com",
        "danbooru.donmai.us", "e-hentai.org", "exhentai.org", "sankakucomplex.com", "tbib.org",
        "pururin.io", "simply-hentai.com", "luscious.net", "doujins.com", "fakku.net", "hentaicity.com",
        "erome.com", "fapello.com", "thothub.to", "bunkr.is", "bunkr.ru", "bunkr.site",
        "coomer.party", "coomer.su", "kemono.party", "kemono.su", "literotica.com",
        "redgifs.com", "pornmd.com", "motherless.com", "empflix.com", "pornone.com",
        "heavy-r.com", "brazzers.com", "realitykings.com", "bangbros.com", "naughtyamerica.com",
        "badboys.com", "asstr.org", "simpcity.su", "cyberdrop.me", "hitomi.la",
        "tsumino.com", "hentai2read.com", "multporn.net",
        "daftsex.com", "eporner.com", "hqporner.com", "porn300.com", "txxx.com",
        "porndig.com", "fuq.com", "javhd.com", "javfree.me", "missav.com", "jable.tv", "netflav.com",
        "mofos.com", "twistys.com", "rk.com", "sweeties.com",
        "porntube.com", "slutload.com", "keezmovies.com", "sunporno.com", "pornhat.com"
    )

    // ── 6. Web Proxies & Bypass Tunnels ──
    @JvmField
    val PROXY_DOMAINS: Set<String> = hashSetOf(
        "croxyproxy.com", "croxy.network", "croxy.org", "proxysite.com", "proxysite.cloud",
        "4everproxy.com", "kproxy.com", "hide.me", "blockaway.net", "blockaway.site",
        "rammerhead.org", "ultraviolet.host", "womginx.com", "incognitoweb.app",
        "tomp.app", "interstellar-proxy.com", "interstellar-proxy.org",
        "unblockvideos.com", "unblock-youtube.net", "unblockwebsites.org", "unblock-web.net", "unblockproxy.me",
        "free-proxy-server.net", "web-proxy.online", "proxyium.com", "proxydaddy.com", "newipnow.com",
        "zalmos.com", "proxfree.com", "hidester.com", "anonymouse.org", "anonymouse.cz", "plainproxy.com",
        "zend2.com", "filterbypass.me", "whoer.net", "123proxy.com", "vpnbook.com",
        "holyub.com", "titaniumnetwork.org", "ludicrous.icu", "hypertabs.net", "rhizome.ludicrous.icu",
        "astroid.wtf", "nebula-proxy.io", "shuttle-proxy.herokuapp.com", "metallic.space",
        "mercury-proxy.github.io", "artclass.site", "math-study.xyz", "algebrahelp.space",
        "history-class.net", "scramjet.org", "megaproxy.com", "hidemyass.com"
    )

    // ── 7. Piracy Streaming, Manga & Short Dramas ──
    @JvmField
    val PIRACY_AND_MEDIA_DOMAINS: Set<String> = hashSetOf(
        "kisskh.co", "kisskh.me", "kisskh.ovh", "kisskh.tv", "kisskh.id",
        "kissasian.cam", "kissasian.lu", "kissasian.sh", "kissasian.li", "kissasian.video",
        "123movies.net", "123moviesgo.to", "fmovies.to", "fmovies.ps", "putlocker.pe",
        "soap2day.to", "soap2day.ac", "lookmovie2.to", "flixtor.to", "sflix.to",
        "bflix.gg", "solarmovie.pe", "solarmovie.cr", "primewire.tf", "primewire.li",
        "yesmovies.ag", "cineb.rs", "watchseries.mx", "watchseries.pe", "azmovies.net",
        "hurawatch.at", "moviesjoy.is", "gomovies.sx", "cinezone.to", "myflixer.to",
        "tinyzone.tv", "vumoo.to", "vumoo.id", "losmovies.today", "afdah.info", "hdtoday.tv",
        "fbox.to", "attacker.tv", "divxcrave.org", "swatchseries.is", "cmovies.so",
        "aniwave.to", "9anime.to", "zoro.to", "hianime.to", "hianime.sx", "kickassanime.mx", "kickassanime.am",
        "animepahe.ru", "gogoanime3.co", "gogoanimes.fi", "gogoanime.pe", "gogoanime.to",
        "animesuge.to", "animeflv.net", "marin.moe", "animedao.to", "kissanime.com.ru", "yugenanime.tv", "yugen.to",
        "aniwatch.to", "kaido.to", "miruro.tv", "allanime.to",
        "mangadex.org", "mangakakalot.com", "manganelo.com", "asurascans.com", "asuracomic.net",
        "reaperscans.com", "flamescans.org", "chapmanganelo.com", "mangabat.com", "mangafox.me",
        "mangahere.cc", "readmanga.me", "mangareader.to", "mangapark.net", "mangaclash.com",
        "aquamanga.com", "mangatx.com", "manhwa18.com", "toonily.com", "toonily.net", "webtoons.com",
        "tapas.io", "lezhin.com", "mangafreak.net", "mangahub.io", "mangapill.com", "mangago.me",
        "novelupdates.com", "lightnovelpub.vip", "wuxiaworld.com",
        "reelshort.com", "dramabox.com", "dramaboxapp.com", "shortmax.com", "goodshort.com",
        "moboreels.com", "topshort.tv", "snackshort.com", "netshort.com", "shorttv.net", "kalostv.com",
        "sereal.plus", "flexitv.app", "meloshort.com", "shotshort.com", "stardust.tv", "playlet.com",
        "thepiratebay.org", "piratebay.org", "1337x.to", "1337x.is", "yts.mx", "torrentgalaxy.to", "eztv.re",
        "fitgirl-repacks.site", "dodi-repacks.site", "steamrip.com", "oceanofgames.com",
        "skidrowreloaded.com", "rutracker.org", "nyaa.si", "limetorrents.lol", "limetorrents.pro",
        "magnetdl.com", "rarbg.to", "torrentday.com", "iptorrents.com"
    )

    // ── 8. Dating & Random Cam Chat ──
    @JvmField
    val DATING_AND_CHAT_DOMAINS: Set<String> = hashSetOf(
        "tinder.com", "bumble.com", "badoo.com", "hinge.co", "okcupid.com",
        "match.com", "pof.com", "coffeemeetsbagel.com", "zoosk.com", "happn.com",
        "grindr.com", "scruff.com", "feeld.co", "ashleymadison.com", "adultfriendfinder.com",
        "pure.app", "her.com", "chappyapp.com", "luxy.com", "sugarbook.com", "seeking.com",
        "passion.com", "fling.com", "c-date.com", "camfrog.com", "follr.com",
        "liveme.com", "bigo.tv", "tango.me", "yubo.live",
        "omegle.com", "ometv.chat", "chatroulette.com", "emeraldchat.com",
        "monkey.app", "camsurf.com", "coomeet.com", "shagle.com", "joingy.com",
        "tinychat.com", "chathub.cam", "bazoocam.org"
    )

    // ── 9. Gossip & Viral Time-Wasters ──
    @JvmField
    val TIME_WASTER_DOMAINS: Set<String> = hashSetOf(
        "buzzfeed.com", "boredpanda.com", "thechive.com", "dailymail.co.uk", "tmz.com",
        "hollywoodlife.com", "eonline.com", "cracked.com", "ranker.com", "listverse.com",
        "knowyourmeme.com", "cheezburger.com", "failblog.org", "distractify.com", "upworthy.com",
        "diply.com", "scoopwhoop.com", "wonderwall.com", "usmagazine.com", "people.com",
        "perezhilton.com", "radaronline.com", "instyle.com", "cosmopolitan.com"
    )

    // ── 10. Crypto Speculation & Meme Coin Casinos ──
    @JvmField
    val CRYPTO_SPECULATION_DOMAINS: Set<String> = hashSetOf(
        "pump.fun", "dexscreener.com", "birdeye.so", "raydium.io", "pancakeswap.finance",
        "uniswap.org", "sushiswap.com", "jupiter.ag", "dextools.io", "coingecko.com", "coinmarketcap.com"
    )

    // ── 11. Restricted gTLDs ──
    private val RESTRICTED_GTLDS: Array<String> = arrayOf(
        ".casino", ".bet", ".poker", ".adult", ".porn", ".xxx", ".sex", ".cam", ".dating", ".vodka", ".bingo"
    )

    // ── 12. Conversational AI Assistant Domains ──
    @JvmField
    val AI_ASSISTANT_DOMAINS: Set<String> = hashSetOf(
        "gemini.google.com", "bard.google.com", "chatgpt.com", "chat.openai.com", "openai.com",
        "claude.ai", "anthropic.com", "perplexity.ai", "copilot.microsoft.com", "poe.com",
        "character.ai", "deepseek.com", "pi.ai", "grok.com", "x.ai",
        "oaistatic.com", "oaiusercontent.com", "claudeusercontent.com", "pplx.ai"
    )

    // ── Signatures & Substrings ──
    private val GAMBLING_SIGNATURES: Array<String> = arrayOf(
        "casino", "bet365", "roobet", "bovada", "betonline", "pokerstars",
        "draftkings", "fanduel", "betmgm", "caesars.com", "wynnbet", "sportsbook",
        "jackpot", "slotmachine", "roulette", "blackjack", "baccarat", "crypto-casino", "cryptocasino",
        "csgoempire", "csgoroll", "hellcase", "gamdom", "duelbits", "rollbit", "betway", "1xbet",
        "parimatch", "betfair", "paddypower", "williamhill", "unibet", "bwin", "ggbet", "betsson",
        "spinpalace", "jackpotcity", "ignitioncasino", "slots.lv", "cafecasino", "mybookie",
        "sportsbetting", "cloudbet", "bitstarz", "bc.game", "7bitcasino", "fortunejack", "crypto-games",
        "datdrop", "farmskins", "key-drop", "rustclash", "clash.gg", "shuffle.com", "rainbet",
        "luckyblock", "megadice", "ladbrokes", "coral.co.uk", "skybet", "betfred", "boylesports",
        "betclic", "winamax", "bet9ja", "sportybet", "dafabet", "chumbacasino", "luckylandslots",
        "pulsz.com", "wowvegas", "high5casino", "betting", "bookmaker", "poker", "spinslot",
        "888casino", "888poker", "888sport", "partycasino", "partypoker", "leovegas", "casumo", "mrgreen",
        "betsafe", "nordicbet", "interwetten", "tipico", "bet-at-home", "10bet", "marathonbet",
        "pinnacle", "vave.com", "trustdice", "metaspins", "jackbit", "wild.io", "bitsler", "primedice",
        "skinsmonkey", "skinport", "gamblezen", "bitcasino", "sportsbet.io", "csgoclicker"
    )

    private val ADULT_SIGNATURES: Array<String> = arrayOf(
        "pornhub", "xvideos", "xvideos2", "xnxx", "xnxx2", "xhamster", "redtube", "youporn", "spankbang", "tube8",
        "beeg.com", "drtuber", "tnaflix", "nuvid", "thumbzilla", "chaturbate", "stripchat",
        "bongacams", "camsoda", "cam4.com", "cam4.es", "livejasmin", "flirt4free", "imlive", "myfreecams",
        "cams.com", "streamate", "onlyfans", "fansly", "loyalfans", "manyvids", "justforfans",
        "modelhub", "nhentai", "hanime", "hentai", "rule34", "gelbooru", "danbooru", "e-hentai",
        "exhentai", "sankakucomplex", "erome.com", "fapello", "thothub", "bunkr.", "coomer.",
        "kemono.", "literotica", "redgifs", "pornmd", "motherless", "empflix", "pornone",
        "heavy-r", "brazzers", "realitykings", "bangbros", "naughtyamerica", "badboys.com",
        "asstr.org", "simpcity", "cyberdrop", "hitomi.la", "tsumino", "hentai2read", "multporn",
        "porno", "porn", "xxx", "nsfw", "camgirl", "webcam-model", "erotic", "jav",
        "daftsex", "eporner", "hqporner", "porn300", "txxx", "porndig", "fuq.com", "javhd", "javfree",
        "missav", "jable.tv", "netflav", "mofos", "twistys", "jerkmate", "cammodels", "rule34video",
        "pururin", "simply-hentai", "luscious.net", "doujins", "fakku", "porntube", "slutload",
        "keezmovies", "sunporno", "pornhat"
    )

    private val PROXY_SIGNATURES: Array<String> = arrayOf(
        "croxyproxy", "proxysite", "4everproxy", "kproxy", "hide.me/proxy", "blockaway",
        "rammerhead", "ultraviolet", "womginx", "incognitoweb", "tomp.app", "interstellar-proxy",
        "unblockvideos", "unblock-youtube", "unblockwebsites", "free-proxy-server", "web-proxy",
        "proxyium", "zalmos", "whoer.net/webproxy", "proxfree", "hidester", "anonymouse.org",
        "plainproxy", "zend2.com", "filterbypass", "holyub", "titaniumnetwork", "school-unblocker",
        "bypass-filter", "webunblocker", "ludicrous.icu", "hypertabs", "rhizome", "astroid.wtf",
        "nebula-proxy", "shuttle-proxy", "metallic.space", "mercury-proxy", "artclass.site",
        "math-study.xyz", "algebrahelp.space", "history-class.net", "scramjet", "hidemyass",
        "proxydaddy", "newipnow", "croxy", "unblock-web"
    )

    private val PIRACY_AND_DRAMA_SIGNATURES: Array<String> = arrayOf(
        "aniwave", "9anime", "zoro.to", "hianime", "kickassanime", "animepahe", "gogoanime",
        "123movies", "fmovies", "putlocker", "soap2day", "lookmovie", "flixtor", "sflix",
        "bflix", "hurawatch", "moviesjoy", "gomovies", "cinezone", "myflixer", "tinyzone",
        "vumoo", "losmovies", "afdah", "hdtoday", "animesuge", "animeflv", "marin.moe",
        "mangadex.org", "mangakakalot", "manganelo", "asurascans", "asuracomic", "reaperscans",
        "flamescans", "chapmanganelo", "mangabat", "mangafox", "mangahere", "readmanga",
        "mangareader", "mangapark", "manhwa18", "toonily", "reelshort", "dramabox", "shortmax",
        "goodshort", "moboreels", "topshort", "snackshort", "netshort", "shorttv", "kalostv",
        "thepiratebay", "1337x", "yts.mx", "torrentgalaxy", "fitgirl-repacks", "dodi-repacks",
        "steamrip", "oceanofgames", "skidrowreloaded", "rutracker", "nyaa.si", "limetorrents",
        "fbox.to", "aniwatch", "kaido.to", "miruro", "allanime", "mangafreak", "mangahub",
        "mangapill", "mangago", "novelupdates", "lightnovelpub", "wuxiaworld", "attacker.tv",
        "divxcrave", "swatchseries", "cmovies", "rarbg", "torrentday", "iptorrents",
        "sereal.plus", "flexitv", "meloshort", "shotshort", "stardust.tv", "playlet"
    )

    private val DATING_SIGNATURES: Array<String> = arrayOf(
        "tinder.com", "bumble.com", "badoo.com", "hinge.co", "okcupid.com", "match.com",
        "pof.com", "coffeemeetsbagel", "zoosk.com", "happn.com", "grindr.com", "scruff.com",
        "feeld.co", "ashleymadison", "adultfriendfinder", "omegle.com", "ometv", "chatroulette",
        "emeraldchat", "monkey.app", "camsurf", "coomeet", "shagle.com", "joingy.com",
        "tinychat.com", "chathub", "bazoocam", "pure.app", "her.com", "chappyapp", "luxy.com",
        "sugarbook", "seeking.com", "passion.com", "fling.com", "c-date", "camfrog",
        "liveme", "bigo.tv", "tango.me", "yubo.live"
    )

    private val TIME_WASTER_SIGNATURES: Array<String> = arrayOf(
        "buzzfeed.com", "boredpanda.com", "thechive.com", "dailymail.co.uk", "tmz.com",
        "hollywoodlife.com", "eonline.com", "cracked.com", "ranker.com", "listverse.com",
        "knowyourmeme.com", "cheezburger", "failblog", "distractify", "upworthy", "diply",
        "scoopwhoop", "wonderwall", "usmagazine", "perezhilton", "radaronline", "cosmopolitan"
    )

    private val CRYPTO_SPECULATION_SIGNATURES: Array<String> = arrayOf(
        "pump.fun", "dexscreener.com", "birdeye.so", "raydium.io", "pancakeswap.finance",
        "uniswap", "sushiswap", "jupiter.ag", "dextools.io", "coingecko", "coinmarketcap"
    )

    // ── Entry Point ──

    @JvmStatic
    fun isKnownDistractingWeb(urlOrDomain: String?): Boolean {
        if (urlOrDomain == null || urlOrDomain.trim().isEmpty()) return false
        val clean = urlOrDomain.trim().lowercase(Locale.US)

        // Protocol mini-games & dedicated arcade doodles
        if (clean.startsWith("chrome://dino") || clean.contains("chrome://network-error/-106") ||
            clean.startsWith("edge://surf") || clean.startsWith("opera://game") ||
            clean.contains("/fbx?fbx=") || clean.contains("snake_arcade")) {
            return true
        }

        val host = WebBlocklistConstants.extractHost(clean)

        // Core YouTube
        if (isCoreYoutubeDomain(clean)) return true

        // Conversational AI Assistants
        if (isAiAssistantDomain(clean)) return true

        // Category domain sets
        if (WebBlocklistConstants.matchesDomainSet(clean, SOCIAL_MEDIA_DOMAINS)) return true
        if (WebBlocklistConstants.matchesDomainSet(clean, GAMING_DOMAINS)) return true
        if (WebBlocklistConstants.matchesDomainSet(clean, GAMBLING_DOMAINS)) return true
        if (WebBlocklistConstants.matchesDomainSet(clean, ADULT_DOMAINS)) return true
        if (WebBlocklistConstants.matchesDomainSet(clean, PROXY_DOMAINS)) return true
        if (WebBlocklistConstants.matchesDomainSet(clean, PIRACY_AND_MEDIA_DOMAINS)) return true
        if (WebBlocklistConstants.matchesDomainSet(clean, DATING_AND_CHAT_DOMAINS)) return true
        if (WebBlocklistConstants.matchesDomainSet(clean, TIME_WASTER_DOMAINS)) return true
        if (WebBlocklistConstants.matchesDomainSet(clean, CRYPTO_SPECULATION_DOMAINS)) return true

        // Web gaming signatures & path patterns
        if (hasGamingUrlSignatures(clean)) return true

        // Substring signatures
        for (g in GAMBLING_SIGNATURES) { if (clean.contains(g)) return true }
        for (a in ADULT_SIGNATURES) { if (clean.contains(a)) return true }
        for (p in PROXY_SIGNATURES) { if (clean.contains(p)) return true }
        for (s in PIRACY_AND_DRAMA_SIGNATURES) { if (clean.contains(s)) return true }
        for (d in DATING_SIGNATURES) { if (clean.contains(d)) return true }
        for (t in TIME_WASTER_SIGNATURES) { if (clean.contains(t)) return true }
        for (c in CRYPTO_SPECULATION_SIGNATURES) { if (clean.contains(c)) return true }

        // Restricted gTLDs
        for (gtld in RESTRICTED_GTLDS) {
            if (host.endsWith(gtld)) return true
        }

        return false
    }

    @JvmStatic
    fun getDistractionReason(urlOrDomain: String?): String {
        if (urlOrDomain == null) return "Restricted website"
        val clean = urlOrDomain.trim().lowercase(Locale.US)

        if (clean.startsWith("chrome://dino") || clean.startsWith("edge://surf") || clean.startsWith("opera://game") ||
            clean.contains("/fbx?fbx=") || clean.contains("snake_arcade")) {
            return "Browser mini-games and arcade doodles are blocked during focus mode"
        }
        if (isCoreYoutubeDomain(clean)) {
            return "YouTube is blocked during focus mode"
        }
        if (isAiAssistantDomain(clean)) {
            return "AI assistants are blocked during focus mode"
        }
        if (WebBlocklistConstants.matchesDomainSet(clean, GAMING_DOMAINS) || hasGamingUrlSignatures(clean)) {
            return "Web-based game blocked during focus mode"
        }
        if (WebBlocklistConstants.matchesDomainSet(clean, GAMBLING_DOMAINS)) {
            return "Gambling and casino site blocked during focus mode"
        }
        if (WebBlocklistConstants.matchesDomainSet(clean, ADULT_DOMAINS)) {
            return "Adult and explicit content blocked during focus mode"
        }
        if (WebBlocklistConstants.matchesDomainSet(clean, PROXY_DOMAINS)) {
            return "Web proxy and bypass tunnel blocked during focus mode"
        }
        if (WebBlocklistConstants.matchesDomainSet(clean, PIRACY_AND_MEDIA_DOMAINS)) {
            return "Entertainment streaming portal blocked during focus mode"
        }
        if (WebBlocklistConstants.matchesDomainSet(clean, SOCIAL_MEDIA_DOMAINS)) {
            return "Distracting social website blocked during focus mode"
        }
        if (WebBlocklistConstants.matchesDomainSet(clean, DATING_AND_CHAT_DOMAINS)) {
            return "Dating and social chat site blocked during focus mode"
        }
        if (WebBlocklistConstants.matchesDomainSet(clean, TIME_WASTER_DOMAINS)) {
            return "Time-wasting clickbait site blocked during focus mode"
        }
        if (WebBlocklistConstants.matchesDomainSet(clean, CRYPTO_SPECULATION_DOMAINS)) {
            return "Crypto speculation portal blocked during focus mode"
        }
        val host = WebBlocklistConstants.extractHost(clean)
        for (gtld in RESTRICTED_GTLDS) {
            if (host.endsWith(gtld)) {
                return "Restricted domain category blocked during focus mode: $gtld"
            }
        }
        return "Distracting web content blocked during focus mode"
    }

    @JvmStatic
    fun isCoreYoutubeDomain(hostOrUrl: String?): Boolean {
        if (hostOrUrl == null) return false
        val clean = hostOrUrl.lowercase(Locale.US).trim()
        if (clean.contains("youtube.com") || clean.contains("youtu.be")) return true
        return WebBlocklistConstants.matchesDomainSet(clean, YOUTUBE_DOMAINS)
    }

    @JvmStatic
    fun isAiAssistantDomain(hostOrUrl: String?): Boolean {
        if (hostOrUrl == null) return false
        val clean = hostOrUrl.lowercase(Locale.US).trim()
        val host = WebBlocklistConstants.extractHost(clean)
        for (aiDomain in AI_ASSISTANT_DOMAINS) {
            if (host == aiDomain || host.endsWith(".$aiDomain") || clean.contains(aiDomain)) {
                return true
            }
        }
        return false
    }

    @JvmStatic
    fun hasGamingUrlSignatures(cleanUrl: String): Boolean {
        if (cleanUrl.startsWith("game.") || cleanUrl.startsWith("games.") ||
            cleanUrl.startsWith("play.") || cleanUrl.startsWith("arcade.") ||
            cleanUrl.contains(".game.") || cleanUrl.contains(".games.")) {
            if (!cleanUrl.contains("google.com") && !cleanUrl.contains("apple.com") &&
                !cleanUrl.contains("microsoft.com") && !cleanUrl.contains("amazon.com")) {
                return true
            }
        }

        if (cleanUrl.contains("unblocked-games") || cleanUrl.contains("unblockedgames") ||
            cleanUrl.contains("unblocked_games") || cleanUrl.contains("classroom6x") ||
            cleanUrl.contains("slope-game") || cleanUrl.contains("slopeunblocked") ||
            cleanUrl.contains("slopegame") || cleanUrl.contains("retrobowl") ||
            cleanUrl.contains("moto-x3m") || cleanUrl.contains("now.gg") ||
            cleanUrl.contains(".itch.zone") || cleanUrl.contains("poki-gdn.com")) {
            return true
        }

        if (cleanUrl.contains("/play/") || cleanUrl.contains("/game/") ||
            cleanUrl.contains("/games/") || cleanUrl.contains("/arcade/") ||
            cleanUrl.contains("/play-now/") || cleanUrl.contains("/html5/") ||
            cleanUrl.contains("/unblocked/") || cleanUrl.contains("/emulator/")) {
            if (!cleanUrl.contains("developer.mozilla.org") &&
                !cleanUrl.contains("github.com") &&
                !cleanUrl.contains("stackoverflow.com")) {
                return true
            }
        }

        if (cleanUrl.contains(".github.io/") || cleanUrl.contains("sites.google.com/") ||
            cleanUrl.contains(".vercel.app/") || cleanUrl.contains(".netlify.app/") ||
            cleanUrl.contains(".replit.app/") || cleanUrl.contains(".repl.co/") ||
            cleanUrl.contains(".web.app/") || cleanUrl.contains(".firebaseapp.com/") ||
            cleanUrl.contains(".glitch.me/")) {
            if (cleanUrl.contains("unblocked") || cleanUrl.contains("game") ||
                cleanUrl.contains("arcade") || cleanUrl.contains("slope") ||
                cleanUrl.contains("retro") || cleanUrl.contains("1v1") ||
                cleanUrl.contains("emulator") || cleanUrl.contains("2048") ||
                cleanUrl.contains("cookieclicker") || cleanUrl.contains("minecraft") ||
                cleanUrl.contains("fnaf") || cleanUrl.contains("geometry")) {
                return true
            }
        }

        return false
    }
}
