package com.uncode.app

import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import java.util.HashSet
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * On-device local web content & browser tab classification engine.
 *
 * Serves as the single, authoritative web intelligence brain across both:
 * 1. LockAccessibilityService (Accessibility DOM & window layer)
 * 2. LocalDnsVpnService (DNS packet & socket layer)
 */
class WebClassifier private constructor() {

    class ClassificationResult(
        @JvmField val isBlocked: Boolean,
        @JvmField val isPending: Boolean,
        @JvmField val reason: String?
    ) {
        constructor(isBlocked: Boolean, reason: String?) : this(isBlocked, false, reason)

        companion object {
            @JvmStatic
            fun allowed(): ClassificationResult = ClassificationResult(false, false, null)

            @JvmStatic
            fun allowed(reason: String?): ClassificationResult = ClassificationResult(false, false, reason)

            @JvmStatic
            fun blocked(reason: String?): ClassificationResult = ClassificationResult(true, false, reason)

            @JvmStatic
            fun pendingLoad(reason: String?): ClassificationResult = ClassificationResult(false, true, reason)
        }
    }

    private class DomScanStats {
        var gamingTitleScore: Int = 0
        var gameHudScore: Int = 0
        var academicScore: Int = 0
        var totalNodesScanned: Int = 0
        var contentTextsScanned: Int = 0
        var detectedToken: String? = null
    }

    companion object {
        private const val TAG = "WebClassifier"

        // Cache mapping normalized destination URL / domain -> ClassificationResult
        private val decisionCache = ConcurrentHashMap<String, ClassificationResult>()

        // Maximum DOM traversal depth
        private const val MAX_DOM_DEPTH = 25
        private const val MAX_NODE_SCAN_COUNT = 350

        // Educational SafeSearch VIPs
        private val GOOGLE_SAFE_VIP = byteArrayOf(216.toByte(), 239.toByte(), 38, 120)
        private val BING_SAFE_VIP = byteArrayOf(204.toByte(), 79, 197.toByte(), 220.toByte())
        private val DUCKDUCKGO_SAFE_VIP = byteArrayOf(52, 142.toByte(), 124, 215.toByte())

        // ── Genre 2: Gambling & Casino Substrings & Domains ──
        private val GAMBLING_SIGNATURES = arrayOf(
            "casino", "bet365", "stake.com", "stake.us", "stake.bet", "stake.games", "roobet", "bovada", "betonline", "pokerstars",
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

        // ── Genre 3: Adult, Explicit & NSFW Substrings ──
        private val ADULT_SIGNATURES = arrayOf(
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

        // ── Genre 4: Web Proxies & Firewall Bypass Tunnels ──
        private val PROXY_SIGNATURES = arrayOf(
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

        // ── Genre 5: Piracy Streaming, Manga & Short-Dramas ──
        private val PIRACY_AND_DRAMA_SIGNATURES = arrayOf(
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

        // ── Genre 7: Casual Dating & Video Cam Chat ──
        private val DATING_SIGNATURES = arrayOf(
            "tinder.com", "bumble.com", "badoo.com", "hinge.co", "okcupid.com", "match.com",
            "pof.com", "coffeemeetsbagel", "zoosk.com", "happn.com", "grindr.com", "scruff.com",
            "feeld.co", "ashleymadison", "adultfriendfinder", "omegle.com", "ometv", "chatroulette",
            "emeraldchat", "monkey.app", "camsurf", "coomeet", "shagle.com", "joingy.com",
            "tinychat.com", "chathub", "bazoocam", "pure.app", "her.com", "chappyapp", "luxy.com",
            "sugarbook", "seeking.com", "passion.com", "fling.com", "c-date", "camfrog",
            "liveme", "bigo.tv", "tango.me", "yubo.live"
        )

        // ── Genre 8: Gossip, Clickbait & Time-Wasters ──
        private val TIME_WASTER_SIGNATURES = arrayOf(
            "buzzfeed.com", "boredpanda.com", "thechive.com", "dailymail.co.uk", "tmz.com",
            "hollywoodlife.com", "eonline.com", "cracked.com", "ranker.com", "listverse.com",
            "knowyourmeme.com", "cheezburger", "failblog", "distractify", "upworthy", "diply",
            "scoopwhoop", "wonderwall", "usmagazine", "perezhilton", "radaronline", "cosmopolitan"
        )

        // ── Genre 9: Crypto Meme Coin Speculation ──
        private val CRYPTO_SPECULATION_SIGNATURES = arrayOf(
            "pump.fun", "dexscreener.com", "birdeye.so", "raydium.io", "pancakeswap.finance",
            "uniswap", "sushiswap", "jupiter.ag", "dextools.io", "coingecko", "coinmarketcap"
        )

        // ── Suspicious Generic Top-Level Domains (gTLDs) ──
        private val SUSPICIOUS_GTLDS = arrayOf(
            ".casino", ".bet", ".poker", ".adult", ".porn", ".xxx", ".sex", ".cam", ".dating", ".vodka", ".bingo"
        )

        // ── Gaming In-Game HUD & Interactive Control Tokens ──
        private val GAME_HUD_TOKENS = arrayOf(
            "score:", "score :", "high score", "highscore", "leaderboard",
            "game over", "gameover", "play again", "restart game", "new game", "restart",
            "level 1", "stage 1", "next level", "select level", "round 1", "wave 1",
            "controls: wasd", "controls : wasd", "wasd", "arrow keys", "press space to jump",
            "tap to play", "click to play", "press any key to start", "mouse to aim",
            "coins:", "coins :", "lives:", "lives :", "health:", "select character",
            "unitywebgl", "unity webgl", "snake_arcade", "playcanvas", "phaser", "godot webgl"
        )

        // ── Specific Web Gaming Title Phrases ──
        private val GAMING_TITLE_PHRASES = arrayOf(
            "unblocked games", "unblocked game", "play online", "online game", "free online games",
            "free games", "browser game", "html5 game", "arcade game", "action game", "rpg game",
            "puzzle game", "racing game", "simulator game", "3d game", "retro games", "retro emulator",
            "crazy games", "poki games", "y8 games", "coolmath games", "friv games", "kizi games",
            "slope game", "retro bowl", "1v1.lol", "agar.io", "slither.io", "paper.io", "snake.io",
            "play for free", "free-to-play game", "play now free"
        )

        // ── Real-time DOM Heuristics for Other Distraction Genres ──
        private val DOM_GAMBLING_TOKENS = arrayOf(
            "place bet", "spin now", "deposit bonus", "poker table", "live dealer", "jackpot prize", "sportsbook"
        )

        private val DOM_ADULT_TOKENS = arrayOf(
            "18+ only", "enter adult site", "live cam model", "mature audience only", "join onlyfans", "free adult video"
        )

        private val DOM_PIRACY_TOKENS = arrayOf(
            "stream server", "watch in hd free", "download episode", "unlock next episode", "drama coins", "download torrent",
            "continue watching", "lastest update", "latest update", "top k-drama", "top c-drama", "k-drama", "c-drama",
            "watch history", "sign in now to save your watch history", "kisskh"
        )

        // ── Real-time Shopping & E-Commerce DOM Triggers ──
        private val DOM_SHOPPING_TOKENS = arrayOf(
            // English / International
            "add to cart", "buy now", "shopping cart", "proceed to checkout", "flash sale",
            "free shipping", "order total", "my orders", "place order", "track order",
            // Korean (NAVER Shopping, Coupang, 11st, Gmarket, etc.)
            "장바구니", "구매하기", "바로구매", "마이쇼핑", "네이버쇼핑", "n배송",
            "오늘배송", "새벽배송", "쇼핑&페이", "쇼핑앤페이", "스마트스토어", "주문/결제",
            "주문 결제", "배송지 및 상품", "초고속 쇼핑", "네이버+ 스토어", "n+ 스토어",
            "슈퍼특가", "쇼핑라이브",
            // Chinese (Baidu Mall, Taobao, JD, Pinduoduo)
            "加入购物车", "立即购买", "购物车", "我的订单", "去结算", "确认订单", "商品详情", "拼单",
            "百度优选", "度小店",
            // Southeast Asian
            "dagdag sa cart", "bilhin ngayon", "troli belanja", "beli sekarang", "keranjang belanja"
        )

        // ── Academic Promotion Keywords ──
        private val ACADEMIC_PROMOTION_KEYWORDS = arrayOf(
            "curriculum", "syllabus", "coursework", "lecture notes", "homework",
            "textbook", "peer-reviewed", "journal", "abstract", "methodology",
            "bibliography", "scholarly", "encyclopedia", "definition", "thesaurus",
            "research paper", "dissertation", "citation", "academic", "theorem",
            "scientific hypothesis", "laboratory", "experiment", "proof", "derivation", "formula",
            "calculus", "algebra", "geometry", "physics", "chemistry", "biology",
            "documentation", "api reference", "developer guide"
        )

        @JvmStatic
        fun clearCache() {
            decisionCache.clear()
            try {
                Log.d(TAG, "WebClassifier decision cache cleared")
            } catch (_: Throwable) {}
        }

        private fun recycleNodes(nodes: List<AccessibilityNodeInfo>?) {
            if (nodes != null) {
                for (n in nodes) {
                    try {
                        n.recycle()
                    } catch (_: Exception) {}
                }
            }
        }

        @JvmStatic
        fun isLinkContextMenu(root: AccessibilityNodeInfo?): Boolean {
            if (root == null) return false
            try {
                val resId = root.viewIdResourceName
                if (resId != null) {
                    val idStr = resId.toString()
                    if (idStr.endsWith(":id/context_menu_layout") || idStr.endsWith(":id/context_menu_frame") ||
                        idStr.endsWith(":id/context_menu_list_view") || idStr.endsWith(":id/menu_header_url") ||
                        idStr.endsWith(":id/title_and_url") || idStr.endsWith(":id/context_menu_dialog") ||
                        idStr.endsWith(":id/link_context_menu") || idStr.endsWith(":id/mozac_browser_menu_list")
                    ) {
                        return true
                    }
                }

                // Chromium family standard context menu IDs
                val queryIds = arrayOf(
                    "com.android.chrome:id/context_menu_layout",
                    "com.android.chrome:id/menu_header_url",
                    "org.chromium.chrome:id/context_menu_layout",
                    "org.chromium.chrome:id/menu_header_url"
                )
                for (qId in queryIds) {
                    val nodes = root.findAccessibilityNodeInfosByViewId(qId)
                    if (nodes != null && nodes.isNotEmpty()) {
                        recycleNodes(nodes)
                        return true
                    }
                }

                // Semantic check for browser/search link action menus
                var openNodes = root.findAccessibilityNodeInfosByText("Open in new tab")
                if (openNodes == null || openNodes.isEmpty()) {
                    openNodes = root.findAccessibilityNodeInfosByText("Open in browser")
                }
                if (openNodes != null && openNodes.isNotEmpty()) {
                    recycleNodes(openNodes)
                    val actionTexts = arrayOf("Copy link address", "Copy link text", "Copy link", "Share link")
                    for (action in actionTexts) {
                        val actionNodes = root.findAccessibilityNodeInfosByText(action)
                        if (actionNodes != null && actionNodes.isNotEmpty()) {
                            recycleNodes(actionNodes)
                            return true
                        }
                    }
                }
            } catch (_: Exception) {}
            return false
        }

        @JvmStatic
        fun isLinkContextMenuSubtree(node: AccessibilityNodeInfo?): Boolean {
            if (node == null) return false
            try {
                val resId = node.viewIdResourceName
                if (resId != null) {
                    val idStr = resId.toString()
                    if (idStr.endsWith(":id/context_menu_layout") || idStr.endsWith(":id/context_menu_frame") ||
                        idStr.endsWith(":id/context_menu_list_view") || idStr.endsWith(":id/menu_header_url") ||
                        idStr.endsWith(":id/menu_header_alt_text") || idStr.endsWith(":id/title_and_url") ||
                        idStr.endsWith(":id/context_menu_dialog") || idStr.endsWith(":id/link_context_menu") ||
                        idStr.endsWith(":id/mozac_browser_menu_list")
                    ) {
                        return true
                    }
                }
            } catch (_: Exception) {}
            return false
        }

        @JvmStatic
        fun isBrowserShellWidget(node: AccessibilityNodeInfo?): Boolean {
            if (node == null) return false
            try {
                val resId = node.viewIdResourceName
                if (resId != null) {
                    val idStr = resId.toString()
                    if (idStr.endsWith(":id/mv_tiles_layout") || idStr.endsWith(":id/mv_tiles_container") ||
                        idStr.endsWith(":id/tile_view") || idStr.endsWith(":id/tile_view_title") ||
                        idStr.endsWith(":id/tile_view_icon") || idStr.endsWith(":id/tile_text_container") ||
                        idStr.endsWith(":id/tile_view_highlight") || idStr.endsWith(":id/feed_stream_recycler_view") ||
                        idStr.endsWith(":id/quickaccess_layout") || idStr.endsWith(":id/top_sites_container") ||
                        idStr.endsWith(":id/ntp_content")
                    ) {
                        return true
                    }
                }
            } catch (_: Exception) {}
            return false
        }

        @JvmStatic
        fun isDestinationUrl(text: String?): Boolean {
            if (text == null) return false
            val clean = text.trim().lowercase(Locale.US)
            if (clean.isEmpty()) return false

            // 1. If it contains spaces, it is a search query or typing in progress
            if (clean.contains(" ")) {
                return false
            }

            // 2. Internal browser schemes are destination URLs
            if (clean.startsWith("chrome://") || clean.startsWith("edge://") || clean.startsWith("opera://")) {
                return true
            }

            // 3. Strip leading http:// or https:// for domain parsing
            var withoutScheme = clean
            if (withoutScheme.startsWith("https://")) {
                withoutScheme = withoutScheme.substring(8)
            } else if (withoutScheme.startsWith("http://")) {
                withoutScheme = withoutScheme.substring(7)
            }

            // 4. Search Engine Results Pages (SERP) are research pages, NOT destination websites!
            if (withoutScheme.startsWith("www.google.") || withoutScheme.startsWith("google.")) {
                if (withoutScheme.contains("/url?q=") || withoutScheme.contains("/url?url=")) {
                    return true
                }
                if (withoutScheme.contains("/fbx?fbx=") || withoutScheme.contains("/logos/") || withoutScheme.contains("/doodles/")) {
                    return true
                }
                return false
            }

            // Video and media portals on Baidu are destination websites!
            if (withoutScheme.startsWith("v.baidu.com") ||
                withoutScheme.startsWith("video.baidu.com") ||
                withoutScheme.startsWith("haokan.baidu.com") ||
                withoutScheme.startsWith("m.baidu.com/video") ||
                withoutScheme.startsWith("baidu.com/video") ||
                withoutScheme.startsWith("www.baidu.com/video") ||
                withoutScheme.startsWith("tieba.baidu.com")
            ) {
                return true
            }

            if (withoutScheme.startsWith("www.bing.com/search") || withoutScheme.startsWith("bing.com/search") ||
                withoutScheme.startsWith("duckduckgo.com") || withoutScheme.startsWith("search.yahoo.com") ||
                withoutScheme.startsWith("ecosia.org/search") || withoutScheme.startsWith("qwant.com") ||
                withoutScheme.startsWith("baidu.com") || withoutScheme.startsWith("www.baidu.com") ||
                withoutScheme.startsWith("m.baidu.com") ||
                withoutScheme.startsWith("yandex.com/search") ||
                withoutScheme.startsWith("startpage.com")
            ) {
                return false
            }

            // 5. Must contain a dot (.) to represent a valid domain
            val dotIndex = withoutScheme.indexOf('.')
            if (dotIndex <= 0 || dotIndex >= withoutScheme.length - 1) {
                return false
            }

            // 6. Check if it has a valid hostname structure with a TLD of 2+ alphabetic letters
            var host = withoutScheme
            val slashIndex = host.indexOf('/')
            if (slashIndex != -1) {
                host = host.substring(0, slashIndex)
            }
            val colonIndex = host.indexOf(':')
            if (colonIndex != -1) {
                host = host.substring(0, colonIndex)
            }

            val lastDot = host.lastIndexOf('.')
            if (lastDot == -1 || lastDot == host.length - 1) {
                return false
            }

            val tld = host.substring(lastDot + 1)
            if (tld.length < 2) {
                return false
            }
            for (i in 0 until tld.length) {
                val c = tld[i]
                if (!c.isLetter()) {
                    return false
                }
            }

            return true
        }

        @JvmStatic
        fun classifyDom(root: AccessibilityNodeInfo?, allowYoutube: Boolean, allowedDomains: Set<String>?): ClassificationResult {
            if (root == null) return ClassificationResult.allowed()

            // Guard: Link Context Menus are native browser action overlays, not page DOMs
            if (isLinkContextMenu(root)) {
                return ClassificationResult.allowed()
            }

            // Guard: System Launchers / SystemUI must NEVER be audited as web containers
            val rootPkgCs = root.packageName
            if (rootPkgCs != null) {
                val rootPkg = rootPkgCs.toString()
                if (KnownSafe.isLauncherApp(null, rootPkg) || rootPkg.contains("launcher") || "com.android.systemui" == rootPkg) {
                    return ClassificationResult.allowed()
                }
            }

            // 1. Check window title
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    val win = root.window
                    if (win != null && win.title != null) {
                        val title = win.title.toString()
                        if (KnownSafeWeb.isAcademicExempt(title)) {
                            return ClassificationResult.allowed()
                        }
                        if (KnownDistractingWeb.isKnownDistractingWeb(title)) {
                            return ClassificationResult.blocked(KnownDistractingWeb.getDistractionReason(title))
                        }
                        if (!allowYoutube) {
                            val lowerTitle = title.lowercase(Locale.ROOT)
                            if (lowerTitle.contains("동영상") || lowerTitle.contains("short-video") || lowerTitle.contains("reels")) {
                                return ClassificationResult.blocked("Video stream restricted during focus lockdown")
                            }
                        }
                    }
                } catch (_: Exception) {}
            }

            // 2. Perform deep DOM inspection across the Chromium node hierarchy
            val effectiveAllowed = buildEffectiveAllowed(allowYoutube, allowedDomains)
            val stats = DomScanStats()
            val domRes = inspectDom(root, 0, stats, effectiveAllowed)
            if (domRes.isBlocked) {
                return domRes
            }

            // 3. Web games detected via title / platform tokens without academic context
            if (stats.academicScore == 0 && stats.gamingTitleScore >= 1) {
                return ClassificationResult.blocked("Web-based game detected (Title/Platform): " + (stats.detectedToken ?: "game"))
            }

            // 4. Sparse / Loading Skeleton Guard
            if (stats.totalNodesScanned < 30 && stats.contentTextsScanned <= 1 && stats.academicScore == 0) {
                return ClassificationResult.pendingLoad("DOM skeleton loading in progress (" + stats.totalNodesScanned + " nodes, " + stats.contentTextsScanned + " texts)")
            }

            return ClassificationResult.allowed()
        }

        @JvmStatic
        fun classifyStandalonePwa(windowTitle: String?, root: AccessibilityNodeInfo?, allowYoutube: Boolean): ClassificationResult {
            return classifyStandalonePwa(windowTitle, root, allowYoutube, null)
        }

        @JvmStatic
        fun classifyStandalonePwa(windowTitle: String?, root: AccessibilityNodeInfo?, allowYoutube: Boolean, allowedDomains: Set<String>?): ClassificationResult {
            return classifyDom(root, allowYoutube, allowedDomains)
        }

        @JvmStatic
        fun classifyInAppWeb(root: AccessibilityNodeInfo?, allowYoutube: Boolean, allowedDomains: Set<String>?): ClassificationResult {
            if (root == null) return ClassificationResult.allowed()

            // Guard: Link Context Menus are native browser action overlays, not in-app web content
            if (isLinkContextMenu(root)) {
                return ClassificationResult.allowed()
            }

            // Guard: System Launchers / SystemUI must NEVER be audited as in-app web views
            val rootPkgCs = root.packageName
            if (rootPkgCs != null) {
                val rootPkg = rootPkgCs.toString()
                if (KnownSafe.isLauncherApp(null, rootPkg) || rootPkg.contains("launcher") || "com.android.systemui" == rootPkg) {
                    return ClassificationResult.allowed()
                }
            }

            // 1. Check window title
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    val win = root.window
                    if (win != null && win.title != null) {
                        val title = win.title.toString()
                        if (KnownSafeWeb.isAcademicExempt(title)) {
                            return ClassificationResult.allowed()
                        }
                        // Fast-path for NAVER search engine results
                        if (title.contains(": 네이버 검색") || title.contains(": 네이버 통합검색") ||
                            title.endsWith("네이버 검색") || title.endsWith("네이버 통합검색")
                        ) {
                            return ClassificationResult.allowed()
                        }
                        if (KnownDistractingWeb.isKnownDistractingWeb(title)) {
                            return ClassificationResult.blocked(KnownDistractingWeb.getDistractionReason(title))
                        }
                        if (title.contains("스토어") || title.contains("쇼핑") || title.contains("smartstore") || title.contains("shopping.naver")) {
                            return ClassificationResult.blocked("Online shopping portal detected: $title")
                        }
                        if (!allowYoutube) {
                            val lowerTitle = title.lowercase(Locale.ROOT)
                            if (lowerTitle.contains("클립검색") || lowerTitle.contains("동영상") ||
                                lowerTitle.contains("clip.naver") || lowerTitle.contains("tv.naver")
                            ) {
                                return ClassificationResult.blocked("In-app video stream / clip search restricted during focus lockdown")
                            }
                        }
                    }
                } catch (_: Exception) {}
            }

            // Check for in-app clip search form when video policy is restricted
            if (!allowYoutube) {
                try {
                    val clipFormNodes = root.findAccessibilityNodeInfosByViewId("clip_form")
                    if (clipFormNodes != null && clipFormNodes.isNotEmpty()) {
                        return ClassificationResult.blocked("In-app video clip search form restricted during focus lockdown")
                    }
                } catch (_: Exception) {}
                try {
                    val clipSearchNodes = root.findAccessibilityNodeInfosByText("클립검색")
                    if (clipSearchNodes != null && clipSearchNodes.isNotEmpty()) {
                        return ClassificationResult.blocked("In-app clip search results restricted during focus lockdown")
                    }
                } catch (_: Exception) {}
            }

            // 2. Perform deep DOM inspection across the WebView node hierarchy
            val effectiveAllowed = buildEffectiveAllowed(allowYoutube, allowedDomains)
            val stats = DomScanStats()
            val domRes = inspectDom(root, 0, stats, effectiveAllowed)
            if (domRes.isBlocked) {
                return domRes
            }

            // If strong gaming title phrases were detected even without full HUD controls
            if (stats.academicScore == 0 && stats.gamingTitleScore >= 1) {
                return ClassificationResult.blocked("Web-based game detected (Title/Platform): " + (stats.detectedToken ?: "game"))
            }

            // 3. Sparse / Loading Skeleton Guard
            if (stats.totalNodesScanned < 30 && stats.contentTextsScanned <= 1 && stats.academicScore == 0) {
                return ClassificationResult.pendingLoad("In-app DOM skeleton loading in progress (" + stats.totalNodesScanned + " nodes, " + stats.contentTextsScanned + " texts)")
            }

            // 4. Legitimate study / reading content survived distraction checks -> ALLOW
            return ClassificationResult.allowed()
        }

        @JvmStatic
        fun classify(rawUrl: String?, root: AccessibilityNodeInfo?, allowYoutube: Boolean): ClassificationResult {
            return classify(rawUrl, root, allowYoutube, null)
        }

        @JvmStatic
        fun classify(rawUrl: String?, root: AccessibilityNodeInfo?, allowYoutube: Boolean, allowedDomains: Set<String>?): ClassificationResult {
            val cleanUrl = rawUrl?.trim()?.lowercase(Locale.US) ?: ""

            // ── 0. SINGLE GATE: Is this an actual destination website URL? ──
            if (!isDestinationUrl(cleanUrl)) {
                return ClassificationResult.allowed()
            }

            // Fast cache check incorporating active policy fingerprint
            val cacheKey = cleanUrl + "@yt:" + (if (allowYoutube) "1" else "0") + "@dom:" + (allowedDomains?.hashCode() ?: 0)
            val cached = decisionCache[cacheKey]
            if (cached != null) {
                return cached
            }

            // ── STAGE 1: LINK SHIMS & REDIRECTIONS IN TRANSIT ──
            val host = WebBlocklistConstants.extractHost(cleanUrl)
            if (WebBlocklistConstants.isPureLinkShimDomain(host)) {
                // If it contains an explicit redirect target (u=, q=, url=) pointing to a known distracting domain, block immediately
                val explicitTarget = extractExplicitRedirectTarget(cleanUrl)
                if (explicitTarget != null && KnownDistractingWeb.isKnownDistractingWeb(explicitTarget)) {
                    val res = ClassificationResult.blocked(KnownDistractingWeb.getDistractionReason(explicitTarget))
                    decisionCache[cacheKey] = res
                    return res
                }
                // Otherwise grant a settling delay to allow the HTTP 302 redirect to land on the destination website
                return ClassificationResult.pendingLoad("Redirect in transit: awaiting settled destination ($host)")
            }

            // Window title academic check (Chromium tabs / CustomTabs)
            var isWindowTitleAcademic = false
            if (root != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    val win = root.window
                    if (win != null && win.title != null && KnownSafeWeb.isAcademicExempt(win.title.toString())) {
                        isWindowTitleAcademic = true
                    }
                } catch (_: Exception) {}
            }

            // Build effective allowed domains
            val effectiveAllowed = buildEffectiveAllowed(allowYoutube, allowedDomains)

            // ── STAGE 2 — INITIAL WEB POLICY EVALUATION (Truth Table) ──
            val isSafeWeb = isWindowTitleAcademic || KnownSafeWeb.isKnownSafeWeb(cleanUrl, effectiveAllowed)
            val isDistractingWeb = KnownDistractingWeb.isKnownDistractingWeb(cleanUrl)

            // ── WEB_MSG_GATE3 — Initial Web Policy Result Classifier ──

            // Branch 1: YES KnownDistractingWeb, YES KnownSafeWeb -> WEB_ALLOW_BROWSER
            if (isDistractingWeb && isSafeWeb) {
                val res = ClassificationResult.allowed("Allowed by Unified Policy")
                decisionCache[cacheKey] = res
                return res
            }

            // Branch 2: NO KnownDistractingWeb, YES KnownSafeWeb -> WEB_ALLOW_BROWSER
            if (!isDistractingWeb && isSafeWeb) {
                val res = ClassificationResult.allowed()
                decisionCache[cacheKey] = res
                return res
            }

            // Branch 3: YES KnownDistractingWeb, NO KnownSafeWeb -> WEB_REMEDIATE
            if (isDistractingWeb && !isSafeWeb) {
                val reason = KnownDistractingWeb.getDistractionReason(cleanUrl)
                val res = ClassificationResult.blocked(reason)
                decisionCache[cacheKey] = res
                return res
            }

            // Branch 4: NO KnownDistractingWeb, NO KnownSafeWeb (Unclassified URL) -> WEB_TREE_SCANS
            if (root != null) {
                val stats = DomScanStats()
                val domRes = inspectDom(root, 0, stats, effectiveAllowed)

                // Branch 4B: Detected as Distracting Web Category
                if (domRes.isBlocked) {
                    decisionCache[cacheKey] = domRes
                    return domRes
                }

                // Branch 4A: Detected Academic & Productivity
                if (stats.academicScore > 0 || isWindowTitleAcademic) {
                    val res = ClassificationResult.allowed("Detected Academic & Productivity")
                    decisionCache[cacheKey] = res
                    return res
                }
            }

            // Branch 4C: ❓ Clean Undefined Web -> WEB_FALLBACK -> WEB_REMEDIATE
            val fallbackRes = ClassificationResult.blocked(
                "Layer 5 Fallback: Unknown unclassified web content is restricted during focus lockdown"
            )
            decisionCache[cacheKey] = fallbackRes
            return fallbackRes
        }

        @JvmStatic
        fun classifyDomain(domain: String?, allowYoutube: Boolean): ClassificationResult {
            return classifyDomain(domain, allowYoutube, null)
        }

        @JvmStatic
        fun classifyDomain(domain: String?, allowYoutube: Boolean, allowedDomains: Set<String>?): ClassificationResult {
            if (domain.isNullOrEmpty()) {
                return ClassificationResult.allowed()
            }
            val lower = domain.trim().lowercase(Locale.US)

            // Fast cache check incorporating active policy fingerprint
            val cacheKey = lower + "@yt:" + (if (allowYoutube) "1" else "0") + "@dom:" + (allowedDomains?.hashCode() ?: 0)
            val cached = decisionCache[cacheKey]
            if (cached != null) {
                return cached
            }

            val effectiveAllowed = buildEffectiveAllowed(allowYoutube, allowedDomains)

            // DoH and HTTPDNS endpoints must ALWAYS be sinkholed to NXDOMAIN to enforce local filtering
            if (WebBlocklistConstants.isDohEndpointOrCanary(lower)) {
                val res = ClassificationResult.blocked("DoH/HTTPDNS endpoint sinkholed to enforce local DNS filtering")
                decisionCache[cacheKey] = res
                return res
            }

            // Pure link shims and redirect forwarders must be forwarded to upstream DNS
            // so the browser can receive the HTTP 302/301 redirect to the destination website
            if (WebBlocklistConstants.isPureLinkShimDomain(lower)) {
                val res = ClassificationResult.allowed("Pure link shim redirect domain forwarded to resolve outbound destination")
                decisionCache[cacheKey] = res
                return res
            }

            val isSafeWeb = KnownSafeWeb.isKnownSafeWeb(lower, effectiveAllowed)
            val isDistractingWeb = KnownDistractingWeb.isKnownDistractingWeb(lower)

            // Branch 1: YES KnownDistractingWeb, YES KnownSafeWeb -> ALLOW
            if (isDistractingWeb && isSafeWeb) {
                val res = ClassificationResult.allowed("Allowed by Unified Policy")
                decisionCache[cacheKey] = res
                return res
            }

            // Branch 2: NO KnownDistractingWeb, YES KnownSafeWeb -> ALLOW
            if (!isDistractingWeb && isSafeWeb) {
                val res = ClassificationResult.allowed()
                decisionCache[cacheKey] = res
                return res
            }

            // Branch 3: YES KnownDistractingWeb, NO KnownSafeWeb -> BLOCK (NXDOMAIN sinkhole)
            if (isDistractingWeb && !isSafeWeb) {
                val reason = if (WebBlocklistConstants.isDohEndpointOrCanary(lower))
                    "DoH endpoint sinkholed to enforce local DNS filtering"
                else
                    KnownDistractingWeb.getDistractionReason(lower)
                val res = ClassificationResult.blocked(reason)
                decisionCache[cacheKey] = res
                return res
            }

            // Branch 4: Unclassified domain at DNS level -> Forward to upstream resolver
            val allowedRes = ClassificationResult.allowed()
            decisionCache[cacheKey] = allowedRes
            return allowedRes
        }

        private fun extractExplicitRedirectTarget(url: String): String? {
            try {
                val qIdx = url.indexOf('?')
                if (qIdx == -1 || qIdx == url.length - 1) return null
                val query = url.substring(qIdx + 1)
                val pairs = query.split('&')
                for (pair in pairs) {
                    val eqIdx = pair.indexOf('=')
                    if (eqIdx != -1) {
                        val key = pair.substring(0, eqIdx).trim()
                        if (key == "u" || key == "q" || key == "url") {
                            val rawVal = pair.substring(eqIdx + 1).trim()
                            val decoded = java.net.URLDecoder.decode(rawVal, "UTF-8")
                            if (decoded.startsWith("http://") || decoded.startsWith("https://")) {
                                return decoded
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
            return null
        }

        private fun evaluateMultiGenreCategories(text: String): ClassificationResult {
            if (KnownDistractingWeb.isKnownDistractingWeb(text)) {
                return ClassificationResult.blocked(KnownDistractingWeb.getDistractionReason(text))
            }
            return ClassificationResult.allowed()
        }

        private fun hasGamingUrlSignatures(cleanUrl: String): Boolean {
            // Gaming subdomains
            if (cleanUrl.startsWith("game.") || cleanUrl.startsWith("games.") ||
                cleanUrl.startsWith("play.") || cleanUrl.startsWith("arcade.") ||
                cleanUrl.contains(".game.") || cleanUrl.contains(".games.")
            ) {
                if (!cleanUrl.contains("google.com") && !cleanUrl.contains("apple.com") &&
                    !cleanUrl.contains("microsoft.com") && !cleanUrl.contains("amazon.com")
                ) {
                    return true
                }
            }

            // Unblocked gaming mirror path patterns
            if (cleanUrl.contains("unblocked-games") || cleanUrl.contains("unblockedgames") ||
                cleanUrl.contains("unblocked_games") || cleanUrl.contains("classroom6x") ||
                cleanUrl.contains("slope-game") || cleanUrl.contains("slopeunblocked") ||
                cleanUrl.contains("slopegame") || cleanUrl.contains("retrobowl") ||
                cleanUrl.contains("moto-x3m") || cleanUrl.contains("now.gg") ||
                cleanUrl.contains(".itch.zone") || cleanUrl.contains("poki-gdn.com")
            ) {
                return true
            }

            // Specific gaming path markers
            if (cleanUrl.contains("/play/") || cleanUrl.contains("/game/") ||
                cleanUrl.contains("/games/") || cleanUrl.contains("/arcade/") ||
                cleanUrl.contains("/play-now/") || cleanUrl.contains("/html5/") ||
                cleanUrl.contains("/unblocked/") || cleanUrl.contains("/emulator/")
            ) {
                if (!cleanUrl.contains("developer.mozilla.org") &&
                    !cleanUrl.contains("github.com") &&
                    !cleanUrl.contains("stackoverflow.com")
                ) {
                    return true
                }
            }

            // Student-hosted game environments
            if (cleanUrl.contains(".github.io/") || cleanUrl.contains("sites.google.com/") ||
                cleanUrl.contains(".vercel.app/") || cleanUrl.contains(".netlify.app/") ||
                cleanUrl.contains(".replit.app/") || cleanUrl.contains(".repl.co/") ||
                cleanUrl.contains(".web.app/") || cleanUrl.contains(".firebaseapp.com/") ||
                cleanUrl.contains(".glitch.me/")
            ) {
                if (cleanUrl.contains("unblocked") || cleanUrl.contains("game") ||
                    cleanUrl.contains("arcade") || cleanUrl.contains("slope") ||
                    cleanUrl.contains("retro") || cleanUrl.contains("1v1") ||
                    cleanUrl.contains("emulator") || cleanUrl.contains("2048") ||
                    cleanUrl.contains("cookieclicker") || cleanUrl.contains("minecraft") ||
                    cleanUrl.contains("fnaf") || cleanUrl.contains("geometry")
                ) {
                    return true
                }
            }

            return false
        }

        @JvmStatic
        fun getSafeSearchVip(domain: String?): ByteArray? {
            if (domain == null) return null
            val lower = domain.lowercase(Locale.US)

            // Google Search & YouTube VIP: forcesafesearch.google.com -> 216.239.38.120
            if (lower.startsWith("www.google.") || lower.startsWith("google.") ||
                lower == "youtube.com" || lower == "www.youtube.com" ||
                lower == "m.youtube.com" || lower == "youtubei.googleapis.com"
            ) {
                // Keep critical academic tools unrestricted
                if (lower.startsWith("drive.") || lower.startsWith("docs.") || lower.startsWith("mail.") ||
                    lower.startsWith("accounts.") || lower.startsWith("fonts.") || lower.startsWith("meet.") ||
                    lower.startsWith("classroom.") || lower.startsWith("calendar.") || lower.startsWith("cloud.")
                ) {
                    return null
                }
                if (!WebBlocklistConstants.isAcademicExempt(lower)) {
                    return GOOGLE_SAFE_VIP
                }
            }

            // Bing Strict VIP: strict.bing.com -> 204.79.197.220
            if (lower == "www.bing.com" || lower == "bing.com" ||
                (lower.endsWith(".bing.com") && (lower.startsWith("www.") || lower.startsWith("cn.")))
            ) {
                return BING_SAFE_VIP
            }

            // DuckDuckGo Safe VIP: safe.duckduckgo.com -> 52.142.124.215
            if (lower == "duckduckgo.com" || lower == "www.duckduckgo.com") {
                return DUCKDUCKGO_SAFE_VIP
            }

            return null
        }

        @JvmStatic
        fun buildEffectiveAllowed(allowYoutube: Boolean, allowedDomains: Set<String>?): Set<String> {
            val effective = if (allowedDomains != null) HashSet(allowedDomains) else HashSet()
            if (allowYoutube) {
                val yt = UnifiedPolicyRegistry.SERVICES["youtube"]
                if (yt != null) {
                    effective.addAll(yt.domains)
                } else {
                    effective.add("youtube.com")
                    effective.add("youtu.be")
                    effective.add("m.youtube.com")
                    effective.add("googlevideo.com")
                    effective.add("ytimg.com")
                    effective.add("youtube-nocookie.com")
                    effective.add("v.baidu.com")
                    effective.add("video.baidu.com")
                    effective.add("haokan.baidu.com")
                    effective.add("m.baidu.com/video")
                    effective.add("baidu.com/video")
                }
            }
            return effective
        }

        private fun inspectDom(
            node: AccessibilityNodeInfo?,
            depth: Int,
            stats: DomScanStats,
            effectiveAllowed: Set<String>
        ): ClassificationResult {
            if (node == null || depth > MAX_DOM_DEPTH || stats.totalNodesScanned >= MAX_NODE_SCAN_COUNT) {
                return ClassificationResult.allowed()
            }
            if (isLinkContextMenuSubtree(node) || isBrowserShellWidget(node)) {
                return ClassificationResult.allowed()
            }
            stats.totalNodesScanned++

            val textSeq = node.text
            val descSeq = node.contentDescription
            val text = textSeq?.toString()?.lowercase(Locale.US)?.trim()
            val desc = descSeq?.toString()?.lowercase(Locale.US)?.trim()

            val values = arrayOf(text, desc)
            for (valStr in values) {
                if (valStr == null || valStr.length < 2) continue
                stats.contentTextsScanned++

                // Academic immunity check on node text/description
                if (WebBlocklistConstants.isAcademicExempt(valStr)) {
                    stats.academicScore++
                    continue
                }

                // Policy immunity check
                if (effectiveAllowed.isNotEmpty()) {
                    if (effectiveAllowed.contains(valStr) || KnownSafeWeb.isKnownSafeWeb(valStr, effectiveAllowed)) {
                        continue
                    }
                }

                // Direct known distracting web domain / signature match
                if (stats.academicScore == 0 && KnownDistractingWeb.isKnownDistractingWeb(valStr)) {
                    return ClassificationResult.blocked(KnownDistractingWeb.getDistractionReason(valStr))
                }

                // Direct web game domain match in title/content
                if (stats.academicScore == 0 && WebBlocklistConstants.isWebGameDomain(valStr)) {
                    return ClassificationResult.blocked("Web-based game detected: $valStr")
                }

                // Direct piracy / media domain check
                if (stats.academicScore == 0 && WebBlocklistConstants.isPiracyOrMediaDomain(valStr)) {
                    return ClassificationResult.blocked("Piracy / media streaming portal detected: $valStr")
                }

                // Hardcoded distraction blacklist match
                if (stats.academicScore == 0 && BlacklistConstants.isBlacklisted("", valStr)) {
                    return ClassificationResult.blocked("Distracting web app signature detected: $valStr")
                }

                // Real-time Gambling DOM triggers
                for (gTok in DOM_GAMBLING_TOKENS) {
                    if (valStr.contains(gTok) && stats.academicScore < 3) {
                        return ClassificationResult.blocked("Interactive gambling controls detected: $gTok")
                    }
                }

                // Real-time Adult DOM triggers
                for (aTok in DOM_ADULT_TOKENS) {
                    if (valStr.contains(aTok) && stats.academicScore < 3) {
                        return ClassificationResult.blocked("Adult/explicit content gate detected: $aTok")
                    }
                }

                // Real-time Piracy/Short-Drama binge triggers
                for (pTok in DOM_PIRACY_TOKENS) {
                    if (valStr.contains(pTok) && stats.academicScore < 3) {
                        return ClassificationResult.blocked("Piracy/short-drama streaming player detected: $pTok")
                    }
                }

                // Real-time Shopping DOM triggers
                for (shTok in DOM_SHOPPING_TOKENS) {
                    if (valStr.contains(shTok) && stats.academicScore < 3) {
                        return ClassificationResult.blocked("Online shopping and e-commerce portal detected: $shTok")
                    }
                }

                // Check gaming title phrases
                for (titlePhrase in GAMING_TITLE_PHRASES) {
                    if (valStr.contains(titlePhrase)) {
                        stats.gamingTitleScore++
                        stats.detectedToken = titlePhrase
                        break
                    }
                }

                // Check gaming HUD & interactive control tokens
                for (hudToken in GAME_HUD_TOKENS) {
                    if (valStr.contains(hudToken)) {
                        stats.gameHudScore++
                        if (stats.detectedToken == null) {
                            stats.detectedToken = hudToken
                        }
                        break
                    }
                }

                // Check academic promotion keywords
                for (acadKw in ACADEMIC_PROMOTION_KEYWORDS) {
                    if (valStr.contains(acadKw)) {
                        stats.academicScore++
                        break
                    }
                }

                // Decisive compound threshold:
                // 1. Title indicates game + at least 1 HUD control button detected
                if (stats.gamingTitleScore >= 1 && stats.gameHudScore >= 1) {
                    return ClassificationResult.blocked("Web-based game detected (Title + Controls): " + stats.detectedToken)
                }

                // 2. Strong in-game HUD indicators
                if (stats.gameHudScore >= 2 && stats.academicScore == 0) {
                    return ClassificationResult.blocked("Interactive game controls detected: " + stats.detectedToken)
                }
            }

            val childCount = node.childCount
            for (i in 0 until childCount) {
                val child = node.getChild(i)
                if (child != null) {
                    val childRes = inspectDom(child, depth + 1, stats, effectiveAllowed)
                    if (childRes.isBlocked) {
                        return childRes
                    }
                }
            }

            return ClassificationResult.allowed()
        }
    }
}
