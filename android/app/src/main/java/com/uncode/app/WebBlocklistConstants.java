package com.uncode.app;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * WebBlocklistConstants:
 * Massive, comprehensive database of digital distractions and academic safe-havens:
 * - Gaming (Web portals, viral .io, cloud streaming, emulators, unblocked mirrors, casual)
 * - Gambling (Casinos, sportsbooks, crypto betting, skin/case gambling)
 * - Adult & Explicit (Pornography, live cams, adult subscription platforms, erotica)
 * - Web Proxies (CGI/PHP/Node unblockers, bypass tunnels)
 * - Piracy & Media Binging (Free streaming, anime, manga/webtoons, vertical short dramas, torrents)
 * - Social Media & Feeds (Shorts/Reels web platforms, algorithmic feeds)
 * - Dating & Random Cam Chat
 * - Viral Gossip & Time-Wasters
 * - Crypto Speculation & Meme Coin Casinos
 * - Suspicious gTLDs
 *
 * Plus Tier 1 Academic & Developer Safe-List (Encyclopedias, LMS, STEM/Math calculators,
 * Research Journals, Developer Docs, Language Learning, Productivity Suites, and worldwide .edu, .ac, and .gov TLDs).
 *
 * Shared between LocalDnsVpnService (DNS socket layer) and
 * LockAccessibilityService (real-time DOM & address bar layer).
 */
public final class WebBlocklistConstants {

    private WebBlocklistConstants() {}

    // ── Distracting Social Media, Video Loops & Algorithmic Feeds ──
    public static final Set<String> BLACKLISTED_WEB_DOMAINS = new HashSet<>(Arrays.asList(
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
    ));

    // ── YouTube Core Domains ──
    public static final Set<String> YOUTUBE_DOMAINS = new HashSet<>(Arrays.asList(
        "youtube.com", "youtu.be", "ytimg.com", "googlevideo.com", "youtube-nocookie.com"
    ));

    // ── Massive Web-Based Gaming Database (400+ Domains) ──
    public static final Set<String> WEB_GAMING_DOMAINS = new HashSet<>(Arrays.asList(
        // A. Mega Portals & Aggregators
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

        // B. Viral .IO & Multiplayer Arena Games
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

        // C. Cloud Gaming & Web APK Streaming
        "now.gg", "nowgg.me", "nowgg.io",
        "play.geforcenow.com", "geforcenow.com",
        "luna.amazon.com",
        "boosteroid.com", "cloud.boosteroid.com",
        "shadow.tech", "vortex.gg", "airgpu.com",
        "x.bluestacks.com",

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
        "romsgames.net", "emulatorgames.net", "retrostic.com", "playretro.ca",
        "emulator.games", "retrogames.cz", "retrogames.fun", "retro-games.cc",

        // F. Unblocked Games Dedicated Networks, Mirrors & Popular Web Titles
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

        // G. Casual, Board, Puzzle & Gaming Platforms
        "chess.com", "lichess.org", "chess24.com", "chessbomb.com",
        "geoguessr.com", "worldle.teuteuf.fr", "globle-game.com", "geoguess.games", "city-guesser.com",
        "2048game.com", "play2048.co", "2048.io",
        "sudoku.com", "websudoku.com", "nonograms.org",
        "wordlewebsite.com", "wordle.org", "quordle.com", "octordle.com", "sedecordle.com",
        "solitaired.com", "cardgames.io", "solitaireparadise.com", "247solitaire.com",
        "sporcle.com", "jetpunk.com", "tetr.io", "jstris.jezevec10.com",
        "roblox.com", "rbxcdn.com", "steamcommunity.com", "store.steampowered.com",
        "epicgames.com", "riotgames.com", "battle.net"
    ));

    // ── Online Gambling, Sportsbooks & Skin Betting Domains (100+ Domains) ──
    public static final Set<String> GAMBLING_DOMAINS = new HashSet<>(Arrays.asList(
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
    ));

    // ── Adult, Explicit & NSFW Domains (100+ Domains) ──
    public static final Set<String> ADULT_DOMAINS = new HashSet<>(Arrays.asList(
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
    ));

    // ── Web Proxies & Firewall Bypass Tunnels (50+ Domains) ──
    public static final Set<String> PROXY_DOMAINS = new HashSet<>(Arrays.asList(
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
    ));

    // ── Piracy Streaming, Manga & Short Dramas (100+ Domains) ──
    public static final Set<String> PIRACY_AND_MEDIA_DOMAINS = new HashSet<>(Arrays.asList(
        // Free Video Streaming
        "123movies.net", "123moviesgo.to", "fmovies.to", "fmovies.ps", "putlocker.pe",
        "soap2day.to", "soap2day.ac", "lookmovie2.to", "flixtor.to", "sflix.to",
        "bflix.gg", "solarmovie.pe", "solarmovie.cr", "primewire.tf", "primewire.li",
        "yesmovies.ag", "cineb.rs", "watchseries.mx", "watchseries.pe", "azmovies.net",
        "hurawatch.at", "moviesjoy.is", "gomovies.sx", "cinezone.to", "myflixer.to",
        "tinyzone.tv", "vumoo.to", "vumoo.id", "losmovies.today", "afdah.info", "hdtoday.tv",
        "fbox.to", "attacker.tv", "divxcrave.org", "swatchseries.is", "cmovies.so",

        // Anime Streaming
        "aniwave.to", "9anime.to", "zoro.to", "hianime.to", "hianime.sx", "kickassanime.mx", "kickassanime.am",
        "animepahe.ru", "gogoanime3.co", "gogoanimes.fi", "gogoanime.pe", "gogoanime.to",
        "animesuge.to", "animeflv.net", "marin.moe", "animedao.to", "kissanime.com.ru", "yugenanime.tv", "yugen.to",
        "aniwatch.to", "kaido.to", "miruro.tv", "allanime.to",

        // Manga, Manhwa & Webtoons
        "mangadex.org", "mangakakalot.com", "manganelo.com", "asurascans.com", "asuracomic.net",
        "reaperscans.com", "flamescans.org", "chapmanganelo.com", "mangabat.com", "mangafox.me",
        "mangahere.cc", "readmanga.me", "mangareader.to", "mangapark.net", "mangaclash.com",
        "aquamanga.com", "mangatx.com", "manhwa18.com", "toonily.com", "toonily.net", "webtoons.com",
        "tapas.io", "lezhin.com", "mangafreak.net", "mangahub.io", "mangapill.com", "mangago.me",
        "novelupdates.com", "lightnovelpub.vip", "wuxiaworld.com",

        // Vertical Short-Dramas
        "reelshort.com", "dramabox.com", "dramaboxapp.com", "shortmax.com", "goodshort.com",
        "moboreels.com", "topshort.tv", "snackshort.com", "netshort.com", "shorttv.net", "kalostv.com",
        "sereal.plus", "flexitv.app", "meloshort.com", "shotshort.com", "stardust.tv", "playlet.com",

        // Torrents & Warez
        "thepiratebay.org", "piratebay.org", "1337x.to", "1337x.is", "yts.mx", "torrentgalaxy.to", "eztv.re",
        "fitgirl-repacks.site", "dodi-repacks.site", "steamrip.com", "oceanofgames.com",
        "skidrowreloaded.com", "rutracker.org", "nyaa.si", "limetorrents.lol", "limetorrents.pro",
        "magnetdl.com", "rarbg.to", "torrentday.com", "iptorrents.com"
    ));

    // ── Dating & Random Video Cam Chat (50+ Domains) ──
    public static final Set<String> DATING_AND_CHAT_DOMAINS = new HashSet<>(Arrays.asList(
        "tinder.com", "bumble.com", "badoo.com", "hinge.co", "okcupid.com",
        "match.com", "pof.com", "coffeemeetsbagel.com", "zoosk.com", "happn.com",
        "grindr.com", "scruff.com", "feeld.co", "ashleymadison.com", "adultfriendfinder.com",
        "pure.app", "her.com", "chappyapp.com", "luxy.com", "sugarbook.com", "seeking.com",
        "passion.com", "fling.com", "c-date.com", "camfrog.com", "follr.com",
        "liveme.com", "bigo.tv", "tango.me", "yubo.live",
        "omegle.com", "ometv.chat", "chatroulette.com", "emeraldchat.com",
        "monkey.app", "camsurf.com", "coomeet.com", "shagle.com", "joingy.com",
        "tinychat.com", "chathub.cam", "bazoocam.org"
    ));

    // ── Gossip & Viral Boredom Time-Wasters (30+ Domains) ──
    public static final Set<String> TIME_WASTER_DOMAINS = new HashSet<>(Arrays.asList(
        "buzzfeed.com", "boredpanda.com", "thechive.com", "dailymail.co.uk", "tmz.com",
        "hollywoodlife.com", "eonline.com", "cracked.com", "ranker.com", "listverse.com",
        "knowyourmeme.com", "cheezburger.com", "failblog.org", "distractify.com", "upworthy.com",
        "diply.com", "scoopwhoop.com", "wonderwall.com", "usmagazine.com", "people.com",
        "perezhilton.com", "radaronline.com", "instyle.com", "cosmopolitan.com"
    ));

    // ── Crypto Speculation & Meme Coin Casinos ──
    public static final Set<String> CRYPTO_SPECULATION_DOMAINS = new HashSet<>(Arrays.asList(
        "pump.fun", "dexscreener.com", "birdeye.so", "raydium.io", "pancakeswap.finance",
        "uniswap.org", "sushiswap.com", "jupiter.ag", "dextools.io", "coingecko.com", "coinmarketcap.com"
    ));

    // ── Vast Academic, Research & Developer Safe-List (Tier 1 Absolute Immunity - 100+ Domains) ──
    public static final Set<String> ACADEMIC_EXEMPT_DOMAINS = new HashSet<>(Arrays.asList(
        // General Reference, Dictionaries & Encyclopedias
        "wikipedia.org", "wikimedia.org", "wiktionary.org", "wikibooks.org", "wikisource.org",
        "wikiversity.org", "wikidata.org", "britannica.com", "worldbook.com",
        "merriam-webster.com", "dictionary.com", "thesaurus.com", "oxfordlearnersdictionaries.com",
        "cambridge.org", "collinsdictionary.com", "vocabulary.com", "etymonline.com",
        "plato.stanford.edu", "iep.utm.edu",

        // Learning Management Systems (LMS) & School Portals
        "canvaslms.com", "instructure.com", "blackboard.com", "schoology.com",
        "moodle.org", "moodle.com", "edmodo.com", "classroom.google.com",
        "powerschool.com", "infinitecampus.com", "seesaw.me", "d2l.com", "brightspace.com",
        "edgenuity.com", "collegeboard.org", "apcentral.collegeboard.org", "act.org",

        // Science, Mathematics, STEM & Computational Tools
        "desmos.com", "geogebra.org", "wolframalpha.com", "wolfram.com",
        "symbolab.com", "mathway.com", "cymath.com", "integral-calculator.com",
        "derivative-calculator.net", "quickmath.com", "mathsisfun.com", "mathisfun.com", "purplemath.com",
        "khanacademy.org", "brilliant.org", "physicsclassroom.com", "phet.colorado.edu",
        "chemguide.co.uk", "ptable.com", "biomanbio.com", "cellsalive.com",
        "hyperphysics.phy-astr.gsu.edu", "projecteuler.net", "oeis.org",

        // Scholarly Research, Journals & Scientific Repositories
        "arxiv.org", "biorxiv.org", "medrxiv.org", "researchgate.net", "jstor.org",
        "nature.com", "science.org", "sciencedirect.com", "springer.com", "wiley.com",
        "cell.com", "pnas.org", "ieee.org", "ieeexplore.ieee.org", "acm.org", "dl.acm.org",
        "ncbi.nlm.nih.gov", "nih.gov", "pubmed.ncbi.nlm.nih.gov", "scholar.google.com",
        "semanticscholar.org", "scopus.com", "frontiersin.org", "plos.org", "plosone.org",
        "cern.ch", "nasa.gov", "noaa.gov", "usgs.gov",

        // Online Learning Platforms, Study Guides & Open Courseware
        "coursera.org", "edx.org", "udemy.com", "futurelearn.com", "openlearning.com",
        "quizlet.com", "brainly.com", "chegg.com", "coursehero.com", "studocu.com", "scribd.com",
        "academia.edu", "gutenberg.org", "archive.org", "openlibrary.org",
        "sparknotes.com", "cliffsnotes.com", "litcharts.com",

        // Language Learning
        "duolingo.com", "babbel.com", "memrise.com", "busuu.com", "lingvist.com", "ankiweb.net",
        "reverso.net", "wordreference.com", "linguee.com", "deepl.com", "translate.google.com",

        // Computer Science, Programming & Developer Docs
        "developer.mozilla.org", "w3schools.com", "geeksforgeeks.org",
        "stackoverflow.com", "stackexchange.com", "superuser.com", "serverfault.com", "askubuntu.com",
        "github.com", "gitlab.com", "bitbucket.org",
        "leetcode.com", "hackerrank.com", "codewars.com", "neetcode.io",
        "freecodecamp.org", "codecademy.com", "tutorialspoint.com", "javatpoint.com", "baeldung.com",
        "docs.oracle.com", "docs.python.org", "nodejs.org", "typescriptlang.org",
        "react.dev", "angular.dev", "vuejs.org", "nextjs.org", "svelte.dev",
        "tailwindcss.com", "getbootstrap.com",
        "developer.android.com", "developer.apple.com", "learn.microsoft.com",
        "aws.amazon.com", "cloud.google.com", "kubernetes.io", "docker.com",
        "docs.unity3d.com", "docs.godotengine.org", "unrealengine.com",
        "kaggle.com", "huggingface.co", "paperswithcode.com",
        "overleaf.com", "latex-project.org", "ctan.org",

        // Student Productivity & Document Workspaces
        "docs.google.com", "drive.google.com", "sheets.google.com", "slides.google.com",
        "forms.google.com", "sites.google.com", "keep.google.com", "calendar.google.com",
        "notion.so", "notion.site", "obsidian.md", "trello.com", "miro.com", "figma.com", "canva.com",
        "lucidchart.com", "coggle.it", "draw.io", "diagrams.net",
        "office.com", "onedrive.live.com", "onenote.com", "sharepoint.com"
    ));

    /**
     * Checks if a domain or URL is immune under the academic whitelist.
     * Enforces worldwide educational (.edu, .ac.*) and governmental (.gov, .mil) domain immunity.
     */
    public static boolean isAcademicExempt(String lowerUrl) {
        if (lowerUrl == null) return false;
        for (String exempt : ACADEMIC_EXEMPT_DOMAINS) {
            if (lowerUrl.contains(exempt)) return true;
        }
        return lowerUrl.contains(".edu/") || lowerUrl.endsWith(".edu") ||
               lowerUrl.contains(".edu.") || lowerUrl.contains(".ac.uk") ||
               lowerUrl.contains(".ac.jp") || lowerUrl.contains(".ac.in") ||
               lowerUrl.contains(".edu.au") || lowerUrl.contains(".edu.sg") ||
               lowerUrl.contains(".edu.ph") || lowerUrl.contains(".edu.cn") ||
               lowerUrl.contains(".edu.br") || lowerUrl.contains(".edu.mx") ||
               lowerUrl.contains(".edu.ng") || lowerUrl.contains(".gov/") ||
               lowerUrl.endsWith(".gov") || lowerUrl.contains(".gov.") ||
               lowerUrl.contains(".gov.uk") || lowerUrl.contains(".gov.au") ||
               lowerUrl.contains(".gov.ph") || lowerUrl.contains(".mil/") ||
               lowerUrl.endsWith(".mil");
    }

    /**
     * Checks if a domain matches blacklisted social media and feeds.
     */
    public static boolean isBlacklistedDomain(String lowerDomain) {
        if (lowerDomain == null) return false;
        for (String b : BLACKLISTED_WEB_DOMAINS) {
            if (lowerDomain.equals(b) || lowerDomain.endsWith("." + b) || lowerDomain.contains("/" + b)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a domain is a DNS-over-HTTPS (DoH) canary or provider endpoint.
     * Returning NXDOMAIN forces Chromium/Chrome to disable internal DoH and fall back to system DNS.
     */
    public static boolean isDohEndpointOrCanary(String lowerDomain) {
        if (lowerDomain == null) return false;
        return lowerDomain.equals("use-application-dns.net") || lowerDomain.endsWith(".use-application-dns.net") ||
               lowerDomain.equals("dns.google") || lowerDomain.endsWith(".dns.google") ||
               lowerDomain.equals("cloudflare-dns.com") || lowerDomain.endsWith(".cloudflare-dns.com") ||
               lowerDomain.equals("dns.quad9.net") || lowerDomain.endsWith(".dns.quad9.net") ||
               lowerDomain.equals("dns.adguard-dns.com") || lowerDomain.endsWith(".dns.adguard-dns.com");
    }

    /**
     * Checks if a URL represents a dedicated Google arcade doodle link (e.g. snake_arcade).
     */
    public static boolean isDedicatedArcadeLink(String url) {
        if (url == null) return false;
        String lower = url.toLowerCase(Locale.US).trim();
        return lower.contains("/fbx?fbx=") || lower.contains("snake_arcade") ||
               lower.contains("fbx=snake_arcade") || lower.contains("/logos/") || lower.contains("/doodles/");
    }

    /**
     * Checks if a domain or URL points to a web-based game.
     */
    public static boolean isWebGameDomain(String lowerDomain) {
        if (lowerDomain == null) return false;
        for (String g : WEB_GAMING_DOMAINS) {
            if (lowerDomain.equals(g) || lowerDomain.endsWith("." + g) || lowerDomain.contains("/" + g)) {
                return true;
            }
        }
        if (lowerDomain.endsWith(".itch.zone") || lowerDomain.contains("poki-gdn.com")) {
            return true;
        }
        if (lowerDomain.contains("unblocked") && (lowerDomain.contains("game") || lowerDomain.contains("66") || lowerDomain.contains("76") || lowerDomain.contains("slope"))) {
            return true;
        }
        return false;
    }

    /**
     * Checks if a domain or URL points to gambling/casinos/sportsbooks.
     */
    public static boolean isGamblingDomain(String lowerDomain) {
        if (lowerDomain == null) return false;
        for (String g : GAMBLING_DOMAINS) {
            if (lowerDomain.equals(g) || lowerDomain.endsWith("." + g) || lowerDomain.contains("/" + g)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a domain or URL points to adult/explicit content.
     */
    public static boolean isAdultDomain(String lowerDomain) {
        if (lowerDomain == null) return false;
        for (String a : ADULT_DOMAINS) {
            if (lowerDomain.equals(a) || lowerDomain.endsWith("." + a) || lowerDomain.contains("/" + a)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a domain or URL points to web proxies or bypass unblockers.
     */
    public static boolean isProxyDomain(String lowerDomain) {
        if (lowerDomain == null) return false;
        for (String p : PROXY_DOMAINS) {
            if (lowerDomain.equals(p) || lowerDomain.endsWith("." + p) || lowerDomain.contains("/" + p)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a domain or URL points to piracy streaming, manga, or short dramas.
     */
    public static boolean isPiracyOrMediaDomain(String lowerDomain) {
        if (lowerDomain == null) return false;
        for (String s : PIRACY_AND_MEDIA_DOMAINS) {
            if (lowerDomain.equals(s) || lowerDomain.endsWith("." + s) || lowerDomain.contains("/" + s)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a domain points to dating or random video cam chat.
     */
    public static boolean isDatingDomain(String lowerDomain) {
        if (lowerDomain == null) return false;
        for (String d : DATING_AND_CHAT_DOMAINS) {
            if (lowerDomain.equals(d) || lowerDomain.endsWith("." + d) || lowerDomain.contains("/" + d)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a domain points to time-wasting viral gossip.
     */
    public static boolean isTimeWasterDomain(String lowerDomain) {
        if (lowerDomain == null) return false;
        for (String t : TIME_WASTER_DOMAINS) {
            if (lowerDomain.equals(t) || lowerDomain.endsWith("." + t) || lowerDomain.contains("/" + t)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a domain points to crypto meme coin gambling/speculation.
     */
    public static boolean isCryptoSpeculationDomain(String lowerDomain) {
        if (lowerDomain == null) return false;
        for (String c : CRYPTO_SPECULATION_DOMAINS) {
            if (lowerDomain.equals(c) || lowerDomain.endsWith("." + c) || lowerDomain.contains("/" + c)) {
                return true;
            }
        }
        return false;
    }
}
