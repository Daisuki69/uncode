package com.uncode.app;

import android.os.Build;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import android.util.Log;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * On-device local web content & browser tab classification engine.
 *
 * Serves as the single, authoritative web intelligence brain across both:
 * 1. LockAccessibilityService (Accessibility DOM & window layer)
 * 2. LocalDnsVpnService (DNS packet & socket layer)
 *
 * Multi-genre detection scope:
 * - Genre 1: Web Games, .IO Arenas, Unblocked Mirrors, Cloud APKs, Retro Emulators, Casual Hubs
 * - Genre 2: Online Gambling, Slots, Sportsbooks, Crypto Betting, Skin/Case Opening
 * - Genre 3: Adult, Pornography, Sexually Explicit, NSFW, Live Cams, Erotica
 * - Genre 4: Web Proxies, CGI/PHP/Node Unblockers, Bypass Tunnels (School Escape Hatches)
 * - Genre 5: Piracy Streaming, Torrents, Anime, Manga/Webtoons, Vertical Short-Dramas
 * - Genre 6: Social Media Web Feeds (TikTok, Instagram, Twitter/X, Reddit, Threads web)
 * - Genre 7: Casual Dating, Hookups & Random Video Cam Chat
 * - Genre 8: Gossip, Viral Clickbait & Time-Wasters
 * - Genre 9: Crypto Meme Coin Trading & Speculation
 * - Genre 10: Suspicious gTLDs (.casino, .bet, .poker, .adult, .porn, .xxx, .sex, .cam)
 *
 * Invariants:
 * - Single Gate: Raw search queries, typing in address bars, and search results (Google/Bing/DDG) are NEVER blocked.
 * - Tier 1 Academic Safe-List: Google Docs, Classroom, Wikipedia, Desmos, .edu are 100% immune.
 */
public final class WebClassifier {

    private static final String TAG = "WebClassifier";

    public static final class ClassificationResult {
        public final boolean isBlocked;
        public final String reason;

        public ClassificationResult(boolean isBlocked, String reason) {
            this.isBlocked = isBlocked;
            this.reason = reason;
        }

        public static ClassificationResult allowed() {
            return new ClassificationResult(false, null);
        }

        public static ClassificationResult allowed(String reason) {
            return new ClassificationResult(false, reason);
        }

        public static ClassificationResult blocked(String reason) {
            return new ClassificationResult(true, reason);
        }
    }

    // Cache mapping normalized destination URL / domain -> ClassificationResult
    private static final Map<String, ClassificationResult> decisionCache = new ConcurrentHashMap<>();

    // Maximum DOM traversal depth (increased to 25 to inspect deep Chromium WebView and CustomTab hierarchies)
    private static final int MAX_DOM_DEPTH = 25;
    private static final int MAX_NODE_SCAN_COUNT = 350;

    // Educational SafeSearch VIPs
    private static final byte[] GOOGLE_SAFE_VIP = new byte[] { (byte) 216, (byte) 239, 38, 120 };
    private static final byte[] BING_SAFE_VIP = new byte[] { (byte) 204, 79, (byte) 197, (byte) 220 };
    private static final byte[] DUCKDUCKGO_SAFE_VIP = new byte[] { 52, (byte) 142, 124, (byte) 215 };

    // ── Genre 2: Gambling & Casino Substrings & Domains ──
    private static final String[] GAMBLING_SIGNATURES = {
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
    };

    // ── Genre 3: Adult, Explicit & NSFW Substrings ──
    private static final String[] ADULT_SIGNATURES = {
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
    };

    // ── Genre 4: Web Proxies & Firewall Bypass Tunnels ──
    private static final String[] PROXY_SIGNATURES = {
        "croxyproxy", "proxysite", "4everproxy", "kproxy", "hide.me/proxy", "blockaway",
        "rammerhead", "ultraviolet", "womginx", "incognitoweb", "tomp.app", "interstellar-proxy",
        "unblockvideos", "unblock-youtube", "unblockwebsites", "free-proxy-server", "web-proxy",
        "proxyium", "zalmos", "whoer.net/webproxy", "proxfree", "hidester", "anonymouse.org",
        "plainproxy", "zend2.com", "filterbypass", "holyub", "titaniumnetwork", "school-unblocker",
        "bypass-filter", "webunblocker", "ludicrous.icu", "hypertabs", "rhizome", "astroid.wtf",
        "nebula-proxy", "shuttle-proxy", "metallic.space", "mercury-proxy", "artclass.site",
        "math-study.xyz", "algebrahelp.space", "history-class.net", "scramjet", "hidemyass",
        "proxydaddy", "newipnow", "croxy", "unblock-web"
    };

    // ── Genre 5: Piracy Streaming, Manga & Short-Dramas ──
    private static final String[] PIRACY_AND_DRAMA_SIGNATURES = {
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
    };

    // ── Genre 7: Casual Dating & Video Cam Chat ──
    private static final String[] DATING_SIGNATURES = {
        "tinder.com", "bumble.com", "badoo.com", "hinge.co", "okcupid.com", "match.com",
        "pof.com", "coffeemeetsbagel", "zoosk.com", "happn.com", "grindr.com", "scruff.com",
        "feeld.co", "ashleymadison", "adultfriendfinder", "omegle.com", "ometv", "chatroulette",
        "emeraldchat", "monkey.app", "camsurf", "coomeet", "shagle.com", "joingy.com",
        "tinychat.com", "chathub", "bazoocam", "pure.app", "her.com", "chappyapp", "luxy.com",
        "sugarbook", "seeking.com", "passion.com", "fling.com", "c-date", "camfrog",
        "liveme", "bigo.tv", "tango.me", "yubo.live"
    };

    // ── Genre 8: Gossip, Clickbait & Time-Wasters ──
    private static final String[] TIME_WASTER_SIGNATURES = {
        "buzzfeed.com", "boredpanda.com", "thechive.com", "dailymail.co.uk", "tmz.com",
        "hollywoodlife.com", "eonline.com", "cracked.com", "ranker.com", "listverse.com",
        "knowyourmeme.com", "cheezburger", "failblog", "distractify", "upworthy", "diply",
        "scoopwhoop", "wonderwall", "usmagazine", "perezhilton", "radaronline", "cosmopolitan"
    };

    // ── Genre 9: Crypto Meme Coin Speculation ──
    private static final String[] CRYPTO_SPECULATION_SIGNATURES = {
        "pump.fun", "dexscreener.com", "birdeye.so", "raydium.io", "pancakeswap.finance",
        "uniswap", "sushiswap", "jupiter.ag", "dextools.io", "coingecko", "coinmarketcap"
    };

    // ── Suspicious Generic Top-Level Domains (gTLDs) ──
    private static final String[] SUSPICIOUS_GTLDS = {
        ".casino", ".bet", ".poker", ".adult", ".porn", ".xxx", ".sex", ".cam", ".dating", ".vodka", ".bingo"
    };

    // ── Gaming In-Game HUD & Interactive Control Tokens ──
    private static final String[] GAME_HUD_TOKENS = {
        "score:", "score :", "high score", "highscore", "leaderboard",
        "game over", "gameover", "play again", "restart game", "new game", "restart",
        "level 1", "stage 1", "next level", "select level", "round 1", "wave 1",
        "controls: wasd", "controls : wasd", "wasd", "arrow keys", "press space to jump",
        "tap to play", "click to play", "press any key to start", "mouse to aim",
        "coins:", "coins :", "lives:", "lives :", "health:", "select character",
        "unitywebgl", "unity webgl", "snake_arcade", "playcanvas", "phaser", "godot webgl"
    };

    // ── Specific Web Gaming Title Phrases ──
    private static final String[] GAMING_TITLE_PHRASES = {
        "unblocked games", "unblocked game", "play online", "online game", "free online games",
        "free games", "browser game", "html5 game", "arcade game", "action game", "rpg game",
        "puzzle game", "racing game", "simulator game", "3d game", "retro games", "retro emulator",
        "crazy games", "poki games", "y8 games", "coolmath games", "friv games", "kizi games",
        "slope game", "retro bowl", "1v1.lol", "agar.io", "slither.io", "paper.io", "snake.io",
        "play for free", "free-to-play game", "play now free"
    };

    // ── Real-time DOM Heuristics for Other Distraction Genres ──
    private static final String[] DOM_GAMBLING_TOKENS = {
        "place bet", "spin now", "deposit bonus", "poker table", "live dealer", "jackpot prize", "sportsbook"
    };

    private static final String[] DOM_ADULT_TOKENS = {
        "18+ only", "enter adult site", "live cam model", "mature audience only", "join onlyfans", "free adult video"
    };

    private static final String[] DOM_PIRACY_TOKENS = {
        "stream server", "watch in hd free", "download episode", "unlock next episode", "drama coins", "download torrent",
        "continue watching", "lastest update", "latest update", "top k-drama", "top c-drama", "k-drama", "c-drama",
        "watch history", "sign in now to save your watch history"
    };

    // ── Academic Promotion Keywords (Protects research papers & study guides) ──
    private static final String[] ACADEMIC_PROMOTION_KEYWORDS = {
        "curriculum", "syllabus", "coursework", "lecture notes", "homework",
        "textbook", "peer-reviewed", "journal", "abstract", "methodology",
        "bibliography", "scholarly", "encyclopedia", "definition", "thesaurus",
        "research paper", "dissertation", "citation", "academic", "theorem",
        "hypothesis", "laboratory", "experiment", "proof", "derivation", "formula",
        "calculus", "algebra", "geometry", "physics", "chemistry", "biology",
        "documentation", "api reference", "developer guide"
    };

    private WebClassifier() {}

    public static void clearCache() {
        decisionCache.clear();
        Log.d(TAG, "WebClassifier decision cache cleared");
    }

    /**
     * Single Gate: Determines whether the given string represents an actual destination website URL.
     * Returns false for user typing, search queries, search suggestions, and search engine results pages.
     */
    public static boolean isDestinationUrl(String text) {
        if (text == null) return false;
        String clean = text.trim().toLowerCase(Locale.US);
        if (clean.isEmpty()) return false;

        // 1. If it contains spaces, it is a search query or typing in progress
        if (clean.contains(" ")) {
            return false;
        }

        // 2. Internal browser schemes are destination URLs
        if (clean.startsWith("chrome://") || clean.startsWith("edge://") || clean.startsWith("opera://")) {
            return true;
        }

        // 3. Strip leading http:// or https:// for domain parsing
        String withoutScheme = clean;
        if (withoutScheme.startsWith("https://")) {
            withoutScheme = withoutScheme.substring(8);
        } else if (withoutScheme.startsWith("http://")) {
            withoutScheme = withoutScheme.substring(7);
        }

        // 4. Search Engine Results Pages (SERP) are research pages, NOT destination websites!
        if (withoutScheme.startsWith("www.google.") || withoutScheme.startsWith("google.")) {
            // Dedicated Google arcade doodle links are destination game targets
            if (withoutScheme.contains("/fbx?fbx=") || withoutScheme.contains("/logos/") || withoutScheme.contains("/doodles/")) {
                return true;
            }
            // Standard search results (e.g. google.com/search?q=...) -> NOT a destination website
            return false;
        }
        if (withoutScheme.startsWith("www.bing.com/search") || withoutScheme.startsWith("bing.com/search") ||
            withoutScheme.startsWith("duckduckgo.com") || withoutScheme.startsWith("search.yahoo.com") ||
            withoutScheme.startsWith("ecosia.org/search") || withoutScheme.startsWith("qwant.com") ||
            withoutScheme.startsWith("baidu.com/s") || withoutScheme.startsWith("yandex.com/search")) {
            return false;
        }

        // 5. Must contain a dot (.) to represent a valid domain (e.g. "minesweeperonline.com", "2048.io")
        int dotIndex = withoutScheme.indexOf('.');
        if (dotIndex <= 0 || dotIndex >= withoutScheme.length() - 1) {
            return false;
        }

        // 6. Check if it has a valid hostname structure with a TLD of 2+ alphabetic letters
        String host = withoutScheme;
        int slashIndex = host.indexOf('/');
        if (slashIndex != -1) {
            host = host.substring(0, slashIndex);
        }
        int colonIndex = host.indexOf(':');
        if (colonIndex != -1) {
            host = host.substring(0, colonIndex);
        }

        int lastDot = host.lastIndexOf('.');
        if (lastDot == -1 || lastDot == host.length() - 1) {
            return false;
        }

        String tld = host.substring(lastDot + 1);
        if (tld.length() < 2) {
            return false;
        }
        for (int i = 0; i < tld.length(); i++) {
            char c = tld.charAt(i);
            if (!Character.isLetter(c)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Authoritative destination website classification entry point (for LockAccessibilityService).
    /**
     * Standalone PWA and WebAPK classification for web applications running without an address bar.
     * Evaluates the window title, manifest app label, and active DOM hierarchy.
     */
    public static ClassificationResult classifyStandalonePwa(String windowTitle, AccessibilityNodeInfo root, boolean allowYoutube) {
        if (windowTitle != null && !windowTitle.trim().isEmpty()) {
            String cleanTitle = windowTitle.trim();
            String lowerTitle = cleanTitle.toLowerCase(Locale.US);

            if (WebBlocklistConstants.isAcademicExempt(lowerTitle)) {
                return ClassificationResult.allowed();
            }

            if (BlacklistConstants.isBlacklisted("", cleanTitle)) {
                return ClassificationResult.blocked("Distracting PWA app blocked: " + cleanTitle);
            }

            ClassificationResult catRes = evaluateMultiGenreCategories(lowerTitle);
            if (catRes.isBlocked) {
                return catRes;
            }
        }

        if (root != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    android.view.accessibility.AccessibilityWindowInfo win = root.getWindow();
                    if (win != null && win.getTitle() != null && WebBlocklistConstants.isAcademicExempt(win.getTitle().toString())) {
                        return ClassificationResult.allowed();
                    }
                } catch (Exception ignore) {}
            }

            DomScanStats stats = new DomScanStats();
            ClassificationResult domRes = inspectDom(root, 0, stats);
            if (domRes.isBlocked) {
                return domRes;
            }
        }

        return ClassificationResult.allowed();
    }

    /**
     * Main on-device semantic classification entry point for Accessibility Service.
     * Evaluates destination URLs and DOM content in real-time.
     *
     * @param rawUrl        The extracted address bar string.
     * @param root          The browser window's AccessibilityNodeInfo root.
     * @param allowYoutube  Whether educational long-form YouTube web is permitted.
     * @return ClassificationResult indicating whether to allow or block with reason.
     */
    public static ClassificationResult classify(String rawUrl, AccessibilityNodeInfo root, boolean allowYoutube) {
        return classify(rawUrl, root, allowYoutube, null);
    }

    public static ClassificationResult classify(String rawUrl, AccessibilityNodeInfo root, boolean allowYoutube, Set<String> allowedDomains) {
        String cleanUrl = rawUrl != null ? rawUrl.trim().toLowerCase(Locale.US) : "";

        // ── SINGLE GATE: Is this an actual destination website URL? ──
        if (!isDestinationUrl(cleanUrl)) {
            return ClassificationResult.allowed();
        }

        // Fast cache check
        ClassificationResult cached = decisionCache.get(cleanUrl);
        if (cached != null) {
            return cached;
        }

        // ── LAYER 1: KnownSafeWeb & Academic Safe-List Immunity ──
        if (WebBlocklistConstants.isAcademicExempt(cleanUrl)) {
            ClassificationResult res = ClassificationResult.allowed();
            decisionCache.put(cleanUrl, res);
            return res;
        }

        String host = WebBlocklistConstants.extractHost(cleanUrl);
        if (allowedDomains != null && !allowedDomains.isEmpty()) {
            for (String allowedDomain : allowedDomains) {
                if (host.equals(allowedDomain) || host.endsWith("." + allowedDomain)) {
                    ClassificationResult res = ClassificationResult.allowed("Allowed by Unified Policy: " + allowedDomain);
                    decisionCache.put(cleanUrl, res);
                    return res;
                }
            }
        }

        if (root != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                android.view.accessibility.AccessibilityWindowInfo win = root.getWindow();
                if (win != null && win.getTitle() != null && WebBlocklistConstants.isAcademicExempt(win.getTitle().toString())) {
                    ClassificationResult res = ClassificationResult.allowed();
                    decisionCache.put(cleanUrl, res);
                    return res;
                }
            } catch (Exception ignore) {}
        }

        // ── LAYER 2: Protocol Schemes & Dedicated Arcade Links ──
        if (cleanUrl.startsWith("chrome://dino") || cleanUrl.contains("chrome://network-error/-106") ||
            cleanUrl.startsWith("edge://surf") || cleanUrl.startsWith("opera://game")) {
            ClassificationResult res = ClassificationResult.blocked("Browser mini-games are blocked during study sessions");
            decisionCache.put(cleanUrl, res);
            return res;
        }

        if (cleanUrl.contains("/fbx?fbx=") || cleanUrl.contains("snake_arcade")) {
            ClassificationResult res = ClassificationResult.blocked("Interactive browser game blocked during focus mode");
            decisionCache.put(cleanUrl, res);
            return res;
        }

        // YouTube rules (Broad allow/block without complex DOM checks per user specification)
        if (cleanUrl.contains("youtube.com") || cleanUrl.contains("youtu.be")) {
            boolean isYtAllowed = allowYoutube || (allowedDomains != null && (allowedDomains.contains("youtube.com") || allowedDomains.contains("youtu.be")));
            if (!isYtAllowed) {
                ClassificationResult res = ClassificationResult.blocked("YouTube is blocked during focus mode");
                decisionCache.put(cleanUrl, res);
                return res;
            } else {
                // Broad allowance: zero fragile DOM inspection, just allow
                ClassificationResult res = ClassificationResult.allowed("YouTube allowed broadly");
                decisionCache.put(cleanUrl, res);
                return res;
            }
        }

        // ── LAYER 3: Multi-Genre Threat Category Evaluation ──
        ClassificationResult categoryCheck = evaluateMultiGenreCategories(cleanUrl);
        if (categoryCheck.isBlocked) {
            decisionCache.put(cleanUrl, categoryCheck);
            return categoryCheck;
        }

        // ── LAYER 4: Real-Time Active Tab DOM Inspection ──
        if (root != null) {
            DomScanStats stats = new DomScanStats();
            ClassificationResult domRes = inspectDom(root, 0, stats);
            if (domRes.isBlocked) {
                decisionCache.put(cleanUrl, domRes);
                return domRes;
            }
        }

        // Default: Safe informational website
        ClassificationResult allowedRes = ClassificationResult.allowed();
        decisionCache.put(cleanUrl, allowedRes);
        return allowedRes;
    }

    /**
     * Domain classification entry point (for LocalDnsVpnService).
     * Evaluates incoming DNS hostnames across all threat genres.
     */
    public static ClassificationResult classifyDomain(String domain, boolean allowYoutube) {
        return classifyDomain(domain, allowYoutube, null);
    }

    public static ClassificationResult classifyDomain(String domain, boolean allowYoutube, Set<String> allowedDomains) {
        if (domain == null || domain.isEmpty()) {
            return ClassificationResult.allowed();
        }
        String lower = domain.trim().toLowerCase(Locale.US);

        // Fast cache check
        ClassificationResult cached = decisionCache.get(lower);
        if (cached != null) {
            return cached;
        }

        // Layer 1: Academic Immunity & KnownSafeWeb
        if (WebBlocklistConstants.isAcademicExempt(lower)) {
            ClassificationResult res = ClassificationResult.allowed();
            decisionCache.put(lower, res);
            return res;
        }

        if (allowedDomains != null && !allowedDomains.isEmpty()) {
            for (String allowedDomain : allowedDomains) {
                if (lower.equals(allowedDomain) || lower.endsWith("." + allowedDomain)) {
                    ClassificationResult res = ClassificationResult.allowed("Allowed by Unified Policy: " + allowedDomain);
                    decisionCache.put(lower, res);
                    return res;
                }
            }
        }

        // YouTube handling
        boolean isYtAllowed = allowYoutube || (allowedDomains != null && (allowedDomains.contains("youtube.com") || allowedDomains.contains("youtu.be")));
        if (lower.contains("youtube.com") || lower.contains("youtu.be")) {
            if (!isYtAllowed) {
                ClassificationResult res = ClassificationResult.blocked("YouTube is blocked during focus mode");
                decisionCache.put(lower, res);
                return res;
            } else {
                ClassificationResult res = ClassificationResult.allowed("YouTube allowed broadly");
                decisionCache.put(lower, res);
                return res;
            }
        }

        // DoH canary & endpoints
        if (WebBlocklistConstants.isDohEndpointOrCanary(lower)) {
            ClassificationResult res = ClassificationResult.blocked("DoH endpoint sinkholed to enforce local DNS filtering");
            decisionCache.put(lower, res);
            return res;
        }

        // Evaluate across all distraction categories
        ClassificationResult catResult = evaluateMultiGenreCategories(lower);
        decisionCache.put(lower, catResult);
        return catResult;
    }

    /**
     * Multi-genre category evaluation across all digital distractions.
     */
    private static ClassificationResult evaluateMultiGenreCategories(String text) {
        // Genre 1: Web Games & Portals
        if (WebBlocklistConstants.isWebGameDomain(text)) {
            return ClassificationResult.blocked("Web-based game blocked during focus mode");
        }
        if (hasGamingUrlSignatures(text)) {
            return ClassificationResult.blocked("Web-based game path blocked during focus mode");
        }

        // Genre 2: Gambling & Casino
        if (WebBlocklistConstants.isGamblingDomain(text)) {
            return ClassificationResult.blocked("Gambling and casino site blocked during focus mode");
        }
        for (String g : GAMBLING_SIGNATURES) {
            if (text.contains(g)) {
                return ClassificationResult.blocked("Gambling and casino site blocked during focus mode");
            }
        }

        // Genre 3: Adult, Explicit & NSFW
        if (WebBlocklistConstants.isAdultDomain(text)) {
            return ClassificationResult.blocked("Adult and explicit content blocked during focus mode");
        }
        for (String a : ADULT_SIGNATURES) {
            if (text.contains(a)) {
                return ClassificationResult.blocked("Adult and explicit content blocked during focus mode");
            }
        }

        // Genre 4: Web Proxies & Filter Bypass Tunnels
        if (WebBlocklistConstants.isProxyDomain(text)) {
            return ClassificationResult.blocked("Web proxy and bypass tunnel blocked during focus mode");
        }
        for (String p : PROXY_SIGNATURES) {
            if (text.contains(p)) {
                return ClassificationResult.blocked("Web proxy and bypass tunnel blocked during focus mode");
            }
        }

        // Genre 5: Piracy Streaming, Manga & Short-Dramas
        if (WebBlocklistConstants.isPiracyOrMediaDomain(text)) {
            return ClassificationResult.blocked("Entertainment streaming portal blocked during focus mode");
        }
        for (String s : PIRACY_AND_DRAMA_SIGNATURES) {
            if (text.contains(s)) {
                return ClassificationResult.blocked("Entertainment streaming portal blocked during focus mode");
            }
        }

        // Genre 6: Social Media Web Feeds
        if (WebBlocklistConstants.isBlacklistedDomain(text)) {
            return ClassificationResult.blocked("Distracting social website blocked during focus mode");
        }

        // Genre 7: Dating & Video Chat
        if (WebBlocklistConstants.isDatingDomain(text)) {
            return ClassificationResult.blocked("Dating and social chat site blocked during focus mode");
        }
        for (String d : DATING_SIGNATURES) {
            if (text.contains(d)) {
                return ClassificationResult.blocked("Dating and social chat site blocked during focus mode");
            }
        }

        // Genre 8: Gossip & Viral Time-Wasters
        if (WebBlocklistConstants.isTimeWasterDomain(text)) {
            return ClassificationResult.blocked("Time-wasting clickbait site blocked during focus mode");
        }
        for (String t : TIME_WASTER_SIGNATURES) {
            if (text.contains(t)) {
                return ClassificationResult.blocked("Time-wasting clickbait site blocked during focus mode");
            }
        }

        // Genre 9: Crypto Meme Coin Speculation
        if (WebBlocklistConstants.isCryptoSpeculationDomain(text)) {
            return ClassificationResult.blocked("Crypto speculation portal blocked during focus mode");
        }
        for (String c : CRYPTO_SPECULATION_SIGNATURES) {
            if (text.contains(c)) {
                return ClassificationResult.blocked("Crypto speculation portal blocked during focus mode");
            }
        }

        // Genre 10: Suspicious gTLDs (.casino, .bet, .poker, .adult, .porn, .xxx, .sex, .cam)
        for (String gtld : SUSPICIOUS_GTLDS) {
            if (text.endsWith(gtld) || text.contains(gtld + "/")) {
                return ClassificationResult.blocked("Restricted domain category blocked during focus mode: " + gtld);
            }
        }

        return ClassificationResult.allowed();
    }

    /**
     * Inspects URL path segments and subdomains for gaming indicators.
     */
    private static boolean hasGamingUrlSignatures(String cleanUrl) {
        // Gaming subdomains
        if (cleanUrl.startsWith("game.") || cleanUrl.startsWith("games.") ||
            cleanUrl.startsWith("play.") || cleanUrl.startsWith("arcade.") ||
            cleanUrl.contains(".game.") || cleanUrl.contains(".games.")) {
            if (!cleanUrl.contains("google.com") && !cleanUrl.contains("apple.com") &&
                !cleanUrl.contains("microsoft.com") && !cleanUrl.contains("amazon.com")) {
                return true;
            }
        }

        // Unblocked gaming mirror path patterns
        if (cleanUrl.contains("unblocked-games") || cleanUrl.contains("unblockedgames") ||
            cleanUrl.contains("unblocked_games") || cleanUrl.contains("classroom6x") ||
            cleanUrl.contains("slope-game") || cleanUrl.contains("slopeunblocked") ||
            cleanUrl.contains("slopegame") || cleanUrl.contains("retrobowl") ||
            cleanUrl.contains("moto-x3m") || cleanUrl.contains("now.gg") ||
            cleanUrl.contains(".itch.zone") || cleanUrl.contains("poki-gdn.com")) {
            return true;
        }

        // Specific gaming path markers
        if (cleanUrl.contains("/play/") || cleanUrl.contains("/game/") ||
            cleanUrl.contains("/games/") || cleanUrl.contains("/arcade/") ||
            cleanUrl.contains("/play-now/") || cleanUrl.contains("/html5/") ||
            cleanUrl.contains("/unblocked/") || cleanUrl.contains("/emulator/")) {
            if (!cleanUrl.contains("developer.mozilla.org") &&
                !cleanUrl.contains("github.com") &&
                !cleanUrl.contains("stackoverflow.com")) {
                return true;
            }
        }

        // Student-hosted game environments (GitHub Pages, Google Sites, Vercel, Netlify, Replit, Glitch)
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
                return true;
            }
        }

        return false;
    }

    /**
     * Returns the strict educational SafeSearch VIP for the given search engine domain.
     */
    public static byte[] getSafeSearchVip(String domain) {
        if (domain == null) return null;
        String lower = domain.toLowerCase(Locale.US);

        // Google Search & YouTube VIP: forcesafesearch.google.com -> 216.239.38.120
        if (lower.startsWith("www.google.") || lower.startsWith("google.") ||
            lower.equals("youtube.com") || lower.equals("www.youtube.com") ||
            lower.equals("m.youtube.com") || lower.equals("youtubei.googleapis.com")) {
            // Keep critical academic tools unrestricted
            if (lower.startsWith("drive.") || lower.startsWith("docs.") || lower.startsWith("mail.") ||
                lower.startsWith("accounts.") || lower.startsWith("fonts.") || lower.startsWith("meet.") ||
                lower.startsWith("classroom.") || lower.startsWith("calendar.") || lower.startsWith("cloud.")) {
                return null;
            }
            if (!WebBlocklistConstants.isAcademicExempt(lower)) {
                return GOOGLE_SAFE_VIP;
            }
        }

        // Bing Strict VIP: strict.bing.com -> 204.79.197.220
        if (lower.equals("www.bing.com") || lower.equals("bing.com") ||
            (lower.endsWith(".bing.com") && (lower.startsWith("www.") || lower.startsWith("cn.")))) {
            return BING_SAFE_VIP;
        }

        // DuckDuckGo Safe VIP: safe.duckduckgo.com -> 52.142.124.215
        if (lower.equals("duckduckgo.com") || lower.equals("www.duckduckgo.com")) {
            return DUCKDUCKGO_SAFE_VIP;
        }

        return null;
    }

    private static class DomScanStats {
        int gamingTitleScore = 0;
        int gameHudScore = 0;
        int academicScore = 0;
        int totalNodesScanned = 0;
        String detectedToken = null;
    }

    /**
     * Crawls active destination window DOM nodes to evaluate real-time distraction tokens:
     * in-game HUD controls, gambling buttons, adult triggers, and streaming players.
     */
    private static ClassificationResult inspectDom(AccessibilityNodeInfo node, int depth, DomScanStats stats) {
        if (node == null || depth > MAX_DOM_DEPTH || stats.totalNodesScanned >= MAX_NODE_SCAN_COUNT) {
            return ClassificationResult.allowed();
        }
        stats.totalNodesScanned++;

        CharSequence textSeq = node.getText();
        CharSequence descSeq = node.getContentDescription();
        String text = textSeq != null ? textSeq.toString().toLowerCase(Locale.US).trim() : null;
        String desc = descSeq != null ? descSeq.toString().toLowerCase(Locale.US).trim() : null;

        String[] values = {text, desc};
        for (String val : values) {
            if (val == null || val.length() < 2) continue;

            // Academic immunity check on node text/description
            if (WebBlocklistConstants.isAcademicExempt(val)) {
                stats.academicScore++;
                continue;
            }

            // Direct web game domain match in title/content (only on non-academic pages)
            if (stats.academicScore == 0 && WebBlocklistConstants.isWebGameDomain(val)) {
                return ClassificationResult.blocked("Web-based game detected: " + val);
            }

            // Direct piracy / media domain check (only on non-academic pages)
            if (stats.academicScore == 0 && WebBlocklistConstants.isPiracyOrMediaDomain(val)) {
                return ClassificationResult.blocked("Piracy / media streaming portal detected: " + val);
            }

            // Hardcoded distraction blacklist match (e.g. KissKH, DramaBox, Loklok, etc.)
            if (stats.academicScore == 0 && BlacklistConstants.isBlacklisted("", val)) {
                return ClassificationResult.blocked("Distracting web app signature detected: " + val);
            }

            // Real-time Gambling DOM triggers
            for (String gTok : DOM_GAMBLING_TOKENS) {
                if (val.contains(gTok) && stats.academicScore == 0) {
                    return ClassificationResult.blocked("Interactive gambling controls detected: " + gTok);
                }
            }

            // Real-time Adult DOM triggers
            for (String aTok : DOM_ADULT_TOKENS) {
                if (val.contains(aTok) && stats.academicScore == 0) {
                    return ClassificationResult.blocked("Adult/explicit content gate detected: " + aTok);
                }
            }

            // Real-time Piracy/Short-Drama binge triggers
            for (String pTok : DOM_PIRACY_TOKENS) {
                if (val.contains(pTok) && stats.academicScore == 0) {
                    return ClassificationResult.blocked("Piracy/short-drama streaming player detected: " + pTok);
                }
            }

            // Check gaming title phrases
            for (String titlePhrase : GAMING_TITLE_PHRASES) {
                if (val.contains(titlePhrase)) {
                    stats.gamingTitleScore++;
                    stats.detectedToken = titlePhrase;
                    break;
                }
            }

            // Check gaming HUD & interactive control tokens
            for (String hudToken : GAME_HUD_TOKENS) {
                if (val.contains(hudToken)) {
                    stats.gameHudScore++;
                    if (stats.detectedToken == null) {
                        stats.detectedToken = hudToken;
                    }
                    break;
                }
            }

            // Check academic promotion keywords
            for (String acadKw : ACADEMIC_PROMOTION_KEYWORDS) {
                if (val.contains(acadKw)) {
                    stats.academicScore++;
                    break;
                }
            }

            // Decisive compound threshold:
            // 1. Title indicates game + at least 1 HUD control button detected
            if (stats.gamingTitleScore >= 1 && stats.gameHudScore >= 1) {
                return ClassificationResult.blocked("Web-based game detected (Title + Controls): " + stats.detectedToken);
            }

            // 2. Strong in-game HUD indicators (2+ independent game control tokens on screen)
            if (stats.gameHudScore >= 2 && stats.academicScore == 0) {
                return ClassificationResult.blocked("Interactive game controls detected: " + stats.detectedToken);
            }
        }

        int childCount = node.getChildCount();
        for (int i = 0; i < childCount; i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                ClassificationResult childRes = inspectDom(child, depth + 1, stats);
                if (childRes.isBlocked) {
                    return childRes;
                }
            }
        }

        return ClassificationResult.allowed();
    }
}
