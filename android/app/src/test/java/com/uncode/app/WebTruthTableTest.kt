package com.uncode.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WebTruthTableTest {

    @Before
    fun setUp() {
        WebClassifier.clearCache()
    }

    @Test
    fun testVerificationSearchImmunity() {
        // Search Engine Results Pages (SERP) are research pages and must NEVER be classified as destination target sites
        assertFalse(WebClassifier.isDestinationUrl("google.com/search?q=calculus+derivation"))
        assertFalse(WebClassifier.isDestinationUrl("https://www.google.com/search?q=physics"))
        assertFalse(WebClassifier.isDestinationUrl("www.bing.com/search?q=discrete+mathematics"))
        assertFalse(WebClassifier.isDestinationUrl("duckduckgo.com/?q=linear+algebra"))
        assertFalse(WebClassifier.isDestinationUrl("search.yahoo.com/search?p=organic+chemistry"))
        assertFalse(WebClassifier.isDestinationUrl("ecosia.org/search?q=cellular+biology"))

        // Active typing in search box (containing spaces) is NOT a destination website
        assertFalse(WebClassifier.isDestinationUrl("how to solve differential equations"))
        assertFalse(WebClassifier.isDestinationUrl("mit 18.01 lecture notes"))

        // Consequently, WebClassifier.classify must immediately return ALLOW for search queries without DOM traversal
        val serpResult = WebClassifier.classify("https://www.google.com/search?q=quantum+physics", null, false)
        assertFalse(serpResult.isBlocked)

        val queryResult = WebClassifier.classify("calculus integral formula", null, false)
        assertFalse(queryResult.isBlocked)
    }

    @Test
    fun testBranch1YesDistractingYesSafe() {
        // YES KnownDistractingWeb, YES KnownSafeWeb -> WEB_ALLOW_BROWSER (e.g. Whitelisted YouTube / Gemini / ChatGPT)
        // 1. YouTube allowed via policy or allowYoutube flag
        val ytWithPolicy = WebClassifier.classify(
            "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            null,
            false,
            setOf("youtube.com")
        )
        assertFalse(ytWithPolicy.isBlocked)

        val ytWithFlag = WebClassifier.classify(
            "https://m.youtube.com/watch?v=12345",
            null,
            true,
            emptySet()
        )
        assertFalse(ytWithFlag.isBlocked)

        val baiduVideoWithFlag = WebClassifier.classify(
            "https://v.baidu.com/v?word=calculus",
            null,
            true,
            emptySet()
        )
        assertFalse(baiduVideoWithFlag.isBlocked)

        val baiduVideoWithPolicy = WebClassifier.classify(
            "https://video.baidu.com/v?word=physics",
            null,
            false,
            setOf("video.baidu.com")
        )
        assertFalse(baiduVideoWithPolicy.isBlocked)

        // 2. AI Assistant domains allowed via Unified Policy
        val chatGptAllowed = WebClassifier.classify(
            "https://chatgpt.com/c/68fa1234-abcd",
            null,
            false,
            setOf("chatgpt.com")
        )
        assertFalse(chatGptAllowed.isBlocked)

        val geminiAllowed = WebClassifier.classify(
            "https://gemini.google.com/app/12345",
            null,
            false,
            setOf("gemini.google.com")
        )
        assertFalse(geminiAllowed.isBlocked)

        val claudeAllowed = WebClassifier.classify(
            "https://claude.ai/chat/abcd",
            null,
            false,
            setOf("claude.ai")
        )
        assertFalse(claudeAllowed.isBlocked)
    }

    @Test
    fun testBranch2NoDistractingYesSafe() {
        // NO KnownDistractingWeb, YES KnownSafeWeb -> WEB_ALLOW_BROWSER (e.g. Wikipedia, Docs, LMS, .edu)
        val wikiResult = WebClassifier.classify("https://en.wikipedia.org/wiki/Computer_science", null, false)
        assertFalse(wikiResult.isBlocked)

        val stanfordResult = WebClassifier.classify("https://cs.stanford.edu/degrees/undergrad", null, false)
        assertFalse(stanfordResult.isBlocked)

        val canvasResult = WebClassifier.classify("https://canvas.instructure.com/courses/123", null, false)
        assertFalse(canvasResult.isBlocked)

        val docsResult = WebClassifier.classify("https://docs.google.com/document/d/123", null, false)
        assertFalse(docsResult.isBlocked)

        val githubResult = WebClassifier.classify("https://github.com/torvalds/linux", null, false)
        assertFalse(githubResult.isBlocked)

        val desmosResult = WebClassifier.classify("https://www.desmos.com/calculator", null, false)
        assertFalse(desmosResult.isBlocked)

        val gizmoResult = WebClassifier.classify("https://gizmo.ai/deck/12345", null, false)
        assertFalse(gizmoResult.isBlocked)

        val kahootResult = WebClassifier.classify("https://kahoot.it/challenge/123", null, false)
        assertFalse(kahootResult.isBlocked)

        val blankResult = WebClassifier.classify("about:blank", null, false)
        assertFalse(blankResult.isBlocked)
    }

    @Test
    fun testBranch3YesDistractingNoSafe() {
        // YES KnownDistractingWeb, NO KnownSafeWeb -> WEB_REMEDIATE (e.g. TikTok, KissKH, y8.com, unapproved YouTube/AI)
        val tiktokResult = WebClassifier.classify("https://www.tiktok.com/@creator/video/123", null, false)
        assertTrue(tiktokResult.isBlocked)

        val pokiResult = WebClassifier.classify("https://poki.com/en/g/subway-surfers", null, false)
        assertTrue(pokiResult.isBlocked)

        val stakeResult = WebClassifier.classify("https://stake.com/casino/games", null, false)
        assertTrue(stakeResult.isBlocked)

        val kisskhResult = WebClassifier.classify("https://kisskh.co/Drama/Test-Show", null, false)
        assertTrue(kisskhResult.isBlocked)

        // YouTube when NOT whitelisted
        val ytBlocked = WebClassifier.classify("https://www.youtube.com/watch?v=123", null, false, emptySet())
        assertTrue(ytBlocked.isBlocked)

        // ChatGPT when NOT in policy
        val chatGptBlocked = WebClassifier.classify("https://chatgpt.com/c/123", null, false, emptySet())
        assertTrue(chatGptBlocked.isBlocked)

        // Browser mini-game scheme
        val dinoBlocked = WebClassifier.classify("chrome://dino", null, false)
        assertTrue(dinoBlocked.isBlocked)
    }

    @Test
    fun testBranch4CFallbackUnclassifiedCleanWeb() {
        // NO KnownDistractingWeb, NO KnownSafeWeb -> WEB_TREE_SCANS
        // When root is null (zero academic promotion indicators in DOM), triggers Layer 5 Fallback
        val unknownSite = WebClassifier.classify("https://random-local-bakery.com/menu", null, false)
        assertTrue(unknownSite.isBlocked)
        assertTrue(unknownSite.reason?.contains("Layer 5 Fallback") == true)
    }

    @Test
    fun testDnsClassificationTruthTable() {
        // 1. Social media & distractions -> Blocked at DNS layer
        val tiktokDns = WebClassifier.classifyDomain("tiktok.com", false)
        assertTrue(tiktokDns.isBlocked)

        val pokiDns = WebClassifier.classifyDomain("poki.com", false)
        assertTrue(pokiDns.isBlocked)

        val stakeDns = WebClassifier.classifyDomain("stake.com", false)
        assertTrue(stakeDns.isBlocked)

        // 2. YouTube DNS: Blocked when false, Allowed when true or in allowedDomains
        val ytBlockedDns = WebClassifier.classifyDomain("youtube.com", false)
        assertTrue(ytBlockedDns.isBlocked)

        WebClassifier.clearCache()
        val ytAllowedDns = WebClassifier.classifyDomain("youtube.com", true)
        assertFalse(ytAllowedDns.isBlocked)

        WebClassifier.clearCache()
        val ytPolicyDns = WebClassifier.classifyDomain("youtube.com", false, setOf("youtube.com"))
        assertFalse(ytPolicyDns.isBlocked)

        WebClassifier.clearCache()
        val baiduVideoDns = WebClassifier.classifyDomain("v.baidu.com", true)
        assertFalse(baiduVideoDns.isBlocked)

        WebClassifier.clearCache()
        val haokanVideoDns = WebClassifier.classifyDomain("haokan.baidu.com", false, setOf("haokan.baidu.com"))
        assertFalse(haokanVideoDns.isBlocked)

        // 3. AI DNS: Blocked when not in policy, Allowed when in policy
        WebClassifier.clearCache()
        val aiBlockedDns = WebClassifier.classifyDomain("chatgpt.com", false, emptySet())
        assertTrue(aiBlockedDns.isBlocked)

        WebClassifier.clearCache()
        val aiAllowedDns = WebClassifier.classifyDomain("chatgpt.com", false, setOf("chatgpt.com"))
        assertFalse(aiAllowedDns.isBlocked)

        // 4. Academic DNS -> Always allowed
        val wikiDns = WebClassifier.classifyDomain("wikipedia.org", false)
        assertFalse(wikiDns.isBlocked)

        val stanfordDns = WebClassifier.classifyDomain("stanford.edu", false)
        assertFalse(stanfordDns.isBlocked)

        val gizmoDns = WebClassifier.classifyDomain("gizmo.ai", false)
        assertFalse(gizmoDns.isBlocked)

        // 5. DoH Canary -> Sinkholed
        val dohDns = WebClassifier.classifyDomain("use-application-dns.net", false)
        assertTrue(dohDns.isBlocked)
    }

    @Test
    fun testAddressBarCandidateValidation() {
        // 1. Placeholder hint strings containing spaces must be rejected (returns false)
        assertFalse(LockAccessibilityService.isValidUrlCandidate("Search or type web address"))
        assertFalse(LockAccessibilityService.isValidUrlCandidate("Search or enter address"))
        assertFalse(LockAccessibilityService.isValidUrlCandidate("Search or type URL"))
        assertFalse(LockAccessibilityService.isValidUrlCandidate("Ask Gemini or type a question"))
        assertFalse(LockAccessibilityService.isValidUrlCandidate("how to solve differential equations"))

        // 2. Common browser placeholder words without spaces must be rejected
        assertFalse(LockAccessibilityService.isValidUrlCandidate("search"))
        assertFalse(LockAccessibilityService.isValidUrlCandidate("Search"))
        assertFalse(LockAccessibilityService.isValidUrlCandidate("SEARCH"))
        assertFalse(LockAccessibilityService.isValidUrlCandidate("search..."))
        assertFalse(LockAccessibilityService.isValidUrlCandidate("about:blank"))
        assertFalse(LockAccessibilityService.isValidUrlCandidate("chrome://newtab"))
        assertFalse(LockAccessibilityService.isValidUrlCandidate(""))
        assertFalse(LockAccessibilityService.isValidUrlCandidate("   "))
        assertFalse(LockAccessibilityService.isValidUrlCandidate(null))

        // 3. Genuine web URLs and domain navigation targets must be accepted (returns true)
        assertTrue(LockAccessibilityService.isValidUrlCandidate("chatgpt.com"))
        assertTrue(LockAccessibilityService.isValidUrlCandidate("https://chatgpt.com"))
        assertTrue(LockAccessibilityService.isValidUrlCandidate("http://192.168.1.1"))
        assertTrue(LockAccessibilityService.isValidUrlCandidate("khanacademy.org/math"))
        assertTrue(LockAccessibilityService.isValidUrlCandidate("wikipedia.org"))
        assertTrue(LockAccessibilityService.isValidUrlCandidate("m.youtube.com"))
        assertTrue(LockAccessibilityService.isValidUrlCandidate("canvas.instructure.com/courses/123"))
    }

    @Test
    fun testSoftwareRepositoriesAndSearchEngines() {
        // 1. Verified Software Repositories must be recognized and allowed
        assertTrue(KnownSafeWeb.isSoftwareRepository("uptodown.com"))
        assertTrue(KnownSafeWeb.isSoftwareRepository("https://en.uptodown.com/android"))
        assertTrue(KnownSafeWeb.isSoftwareRepository("apkmirror.com"))
        assertTrue(KnownSafeWeb.isSoftwareRepository("f-droid.org"))
        assertTrue(KnownSafeWeb.isSoftwareRepository("apkpure.com"))
        assertTrue(KnownSafeWeb.isKnownSafeWeb("uptodown.com", null))
        assertTrue(KnownSafeWeb.isKnownSafeWeb("https://en.uptodown.com/android", null))

        val uptodownDns = WebClassifier.classifyDomain("uptodown.com", false)
        assertFalse(uptodownDns.isBlocked)

        // 2. Multilingual Search Engines must be recognized
        assertTrue(KnownSafeWeb.isSearchEngine("baidu.com"))
        assertTrue(KnownSafeWeb.isSearchEngine("m.baidu.com"))
        assertTrue(KnownSafeWeb.isKnownSafeWeb("baidu.com", null))
        assertTrue(KnownSafeWeb.isKnownSafeWeb("m.baidu.com", null))

        // 3. Baidu SERP Search Queries and Homepages are research pages, NOT destination websites
        assertFalse(WebClassifier.isDestinationUrl("https://m.baidu.com/s?wd=operating+system"))
        assertFalse(WebClassifier.isDestinationUrl("https://baidu.com/s?wd=calculus"))
        assertFalse(WebClassifier.isDestinationUrl("https://www.baidu.com/s?wd=ai"))
        assertFalse(WebClassifier.isDestinationUrl("https://m.baidu.com"))
        assertFalse(WebClassifier.isDestinationUrl("https://baidu.com"))
        assertFalse(WebClassifier.isDestinationUrl("https://www.baidu.com"))

        // 4. Baidu Video destinations ARE destination websites governed by Unified Policy Videos
        assertTrue(WebClassifier.isDestinationUrl("https://v.baidu.com/v?word=linear+algebra"))
        assertTrue(WebClassifier.isDestinationUrl("https://video.baidu.com/v?word=physics"))
        assertTrue(WebClassifier.isDestinationUrl("https://haokan.baidu.com/v?pd=wisenatural"))
        assertTrue(WebClassifier.isDestinationUrl("https://m.baidu.com/video"))
        assertTrue(WebClassifier.isDestinationUrl("https://baidu.com/video"))

        // When Videos service is disabled, Baidu video is blocked
        val blockedVid = WebClassifier.classify("https://v.baidu.com/v?word=calculus", null, false)
        assertTrue(blockedVid.isBlocked)
        val blockedPath = WebClassifier.classify("https://m.baidu.com/video", null, false)
        assertTrue(blockedPath.isBlocked)

        // When Videos service is enabled, Baidu video is allowed
        val allowedVid = WebClassifier.classify("https://v.baidu.com/v?word=calculus", null, true)
        assertFalse(allowedVid.isBlocked)
        val allowedPath = WebClassifier.classify("https://m.baidu.com/video", null, true)
        assertFalse(allowedPath.isBlocked)
    }

    @Test
    fun testPortalSuperAppAndSubActivityRemediation() {
        // 1. Curated Allowed Portal Super-Apps must be recognized (Exactly Baidu & NAVER)
        assertTrue(AppClassifier.isPortalSuperApp("com.baidu.searchbox"))
        assertTrue(AppClassifier.isPortalSuperApp("com.nhn.android.search"))
        assertFalse(AppClassifier.isPortalSuperApp("com.transsion.phoenix"))
        assertFalse(AppClassifier.isPortalSuperApp("com.UCMobile"))
        assertFalse(AppClassifier.isPortalSuperApp("com.android.chrome"))

        // 2. Disallowed unmanaged super-apps
        assertTrue(AppClassifier.isDisallowedPortalSuperApp("com.transsion.phoenix"))
        assertTrue(AppClassifier.isDisallowedPortalSuperApp("com.UCMobile"))

        // 3. Allowed Portal Super-Apps are recognized as browser portals at the package level
        assertTrue(AppClassifier.isBrowserPackage(null, "com.baidu.searchbox", "Baidu"))
        assertTrue(AppClassifier.isBrowserPackage(null, "com.nhn.android.search", "NAVER"))

        // Disallowed portals are strictly blocked from browser package gate
        assertFalse(AppClassifier.isBrowserPackage(null, "com.transsion.phoenix", "Phoenix"))
        assertFalse(AppClassifier.isBrowserPackage(null, "com.UCMobile", "UC Browser"))

        // 4. Short video reels and distracting novel sub-activities inside portal super-apps must be identified
        assertTrue(AppClassifier.isDistractingSubActivity("com.baidu.searchbox", "com.baidu.searchbox.video.feedflow.tab.VideoTabActivity"))
        assertTrue(AppClassifier.isDistractingSubActivity("com.baidu.searchbox", "com.baidu.searchbox.discovery.novel.NovelHomeActivity"))
        assertTrue(AppClassifier.isDistractingSubActivity("com.nhn.android.search", "com.nhn.android.clip.ui.ClipActivity"))
        assertTrue(AppClassifier.isDistractingSubActivity("com.transsion.phoenix", "com.transsion.phoenix.reels.ShortVideoActivity"))

        // 5. Everything else in super-apps (search, utilities, downloads, files) is allowed without hardcoding
        assertFalse(AppClassifier.isDistractingSubActivity("com.baidu.searchbox", "com.baidu.searchbox.MainActivity"))
        assertFalse(AppClassifier.isDistractingSubActivity("com.baidu.searchbox", "com.baidu.searchbox.BoxBrowserActivity"))
        assertFalse(AppClassifier.isDistractingSubActivity("com.baidu.searchbox", "com.baidu.searchbox.download.center.ui.fusion.FileManagerActivity"))
        assertFalse(AppClassifier.isDistractingSubActivity("com.baidu.searchbox", "com.baidu.searchbox.download.center.ui.fusion.DownloadManagerActivity"))
        assertFalse(AppClassifier.isDistractingSubActivity("com.baidu.searchbox", "com.baidu.searchbox.WebActivity"))
        assertFalse(AppClassifier.isDistractingSubActivity("com.nhn.android.search", "com.nhn.android.search.universe.UniverseActivity"))
        assertFalse(AppClassifier.isDistractingSubActivity("com.nhn.android.search", "com.nhn.android.search.browser.InAppBrowserActivity"))
    }

    @Test
    fun testHttpDnsAndInAppWebClassification() {
        // 1. HTTPDNS endpoints must be identified as DoH/HTTPDNS endpoints for local sinkholing
        assertTrue(WebBlocklistConstants.isDohEndpointOrCanary("httpdns.baidu.com"))
        assertTrue(WebBlocklistConstants.isDohEndpointOrCanary("httpdns.baidubce.com"))
        assertTrue(WebBlocklistConstants.isDohEndpointOrCanary("sa5.tuisong.baidu.com"))
        assertTrue(WebBlocklistConstants.isDohEndpointOrCanary("httpdns.aliyun.com"))
        assertTrue(WebBlocklistConstants.isDohEndpointOrCanary("httpdns.qq.com"))
        assertTrue(WebBlocklistConstants.isDohEndpointOrCanary("httpdns.pro"))

        // Standard educational/research domains must NOT be flagged as DoH
        assertFalse(WebBlocklistConstants.isDohEndpointOrCanary("wikipedia.org"))
        assertFalse(WebBlocklistConstants.isDohEndpointOrCanary("baidu.com"))
        assertFalse(WebBlocklistConstants.isDohEndpointOrCanary("naver.com"))

        // 2. HTTPDNS queries at DNS level are sinkholed to force standard system DNS fallback
        val baiduHttpDnsRes = WebClassifier.classifyDomain("httpdns.baidu.com", false, null)
        assertTrue(baiduHttpDnsRes.isBlocked)

        // 3. Y8 domain and its CDN/game assets must be classified as blocked at DNS and WebClassifier level
        assertTrue(KnownDistractingWeb.isKnownDistractingWeb("y8.com"))
        assertTrue(KnownDistractingWeb.isKnownDistractingWeb("www.y8.com"))
        assertTrue(KnownDistractingWeb.isKnownDistractingWeb("img.y8.com"))
        assertTrue(KnownDistractingWeb.isKnownDistractingWeb("cdn.y8.com"))
        assertTrue(KnownDistractingWeb.isKnownDistractingWeb("account.y8.com"))
        assertTrue(KnownDistractingWeb.isKnownDistractingWeb("playtomic.y8.com"))

        assertTrue(WebBlocklistConstants.isWebGameDomain("y8.com"))
        assertTrue(WebBlocklistConstants.isWebGameDomain("www.y8.com"))

        val y8Res = WebClassifier.classifyDomain("y8.com", false, null)
        assertTrue(y8Res.isBlocked)
        val y8WwwRes = WebClassifier.classifyDomain("www.y8.com", false, null)
        assertTrue(y8WwwRes.isBlocked)

        // 4. In-App Web Classification: null root returns allowed without crashing
        val inAppRes = WebClassifier.classifyInAppWeb(null, false, null)
        assertFalse(inAppRes.isBlocked)
    }

    @Test
    fun testLinkContextMenuImmunity() {
        // Link Context Menus are preview overlays / action sheets and must never be classified as web page DOMs
        assertFalse(WebClassifier.isLinkContextMenu(null))
        assertFalse(WebClassifier.isLinkContextMenuSubtree(null))
        assertFalse(LockAccessibilityService.isLinkContextMenu(null, "com.android.chrome"))

        // Null root for DOM classification returns allowed
        val domRes = WebClassifier.classifyDom(null, false, emptySet())
        assertFalse(domRes.isBlocked)

        val inAppRes = WebClassifier.classifyInAppWeb(null, false, emptySet())
        assertFalse(inAppRes.isBlocked)
    }

    @Test
    fun testShoppingAppAndWebClassification() {
        // 1. Native shopping app package IDs are classified as distracting in KnownDistracting
        assertTrue(KnownDistracting.isKnownDistracting("com.shopee.ph"))
        assertTrue(KnownDistracting.isKnownDistracting("com.shopee.id"))
        assertTrue(KnownDistracting.isKnownDistracting("com.shopee.my"))
        assertTrue(KnownDistracting.isKnownDistracting("com.lazada.android"))
        assertTrue(KnownDistracting.isKnownDistracting("com.tokopedia.tkpd"))
        assertTrue(KnownDistracting.isKnownDistracting("kr.co.coupang.ecommerce"))
        assertTrue(KnownDistracting.isKnownDistracting("com.elevenst"))
        assertTrue(KnownDistracting.isKnownDistracting("com.gmarket.market"))
        assertTrue(KnownDistracting.isKnownDistracting("com.ebay.kr.gmarket"))
        assertTrue(KnownDistracting.isKnownDistracting("com.taobao.taobao"))
        assertTrue(KnownDistracting.isKnownDistracting("com.jingdong.app.mall"))
        assertTrue(KnownDistracting.isKnownDistracting("com.xunmeng.pinduoduo"))
        assertTrue(KnownDistracting.isKnownDistracting("com.einnovation.temu"))
        assertTrue(KnownDistracting.isKnownDistracting("com.zzkko"))
        assertTrue(KnownDistracting.isKnownDistracting("com.alibaba.aliexpresshd"))
        assertTrue(KnownDistracting.isKnownDistracting("com.amazon.mShop.android.shopping"))
        assertTrue(KnownDistracting.isKnownDistracting("com.ebay.mobile"))
        assertTrue(KnownDistracting.isKnownDistracting("com.flipkart.android"))
        assertTrue(KnownDistracting.isKnownDistracting("com.mercadolibre"))

        // 2. Package signature and Label signature detection for unknown shopping apps
        assertTrue(KnownDistracting.isKnownDistracting("com.example.shopping.mall"))
        assertTrue(KnownDistracting.isKnownDistracting("com.unknown.ecommerce.app"))
        assertTrue(KnownDistracting.isKnownDistracting("com.random.store", "Flash Deal Shopping Store"))
        assertTrue(KnownDistracting.isKnownDistracting("com.generic.market", "Online Marketplace"))
        assertTrue(KnownDistracting.isKnownDistracting("kr.random.store", "온라인 쇼핑몰"))

        // 3. Stage 1 Master Veto Gate verifies native shopping packages
        assertTrue(AppClassifier.isStage1Vetoed("com.shopee.ph", "Shopee"))
        assertTrue(AppClassifier.isStage1Vetoed("kr.co.coupang.ecommerce", "쿠팡"))
        assertTrue(AppClassifier.isStage1Vetoed("com.amazon.mShop.android.shopping", "Amazon Shopping"))

        // 4. Web shopping domains in KnownDistractingWeb
        assertTrue(KnownDistractingWeb.isKnownDistractingWeb("shopping.naver.com"))
        assertTrue(KnownDistractingWeb.isKnownDistractingWeb("m.shopping.naver.com"))
        assertTrue(KnownDistractingWeb.isKnownDistractingWeb("smartstore.naver.com"))
        assertTrue(KnownDistractingWeb.isKnownDistractingWeb("mall.baidu.com"))
        assertTrue(KnownDistractingWeb.isKnownDistractingWeb("youxuan.baidu.com"))
        assertTrue(KnownDistractingWeb.isKnownDistractingWeb("coupang.com"))
        assertTrue(KnownDistractingWeb.isKnownDistractingWeb("shopee.ph"))
        assertTrue(KnownDistractingWeb.isKnownDistractingWeb("lazada.com.ph"))
        assertTrue(KnownDistractingWeb.isKnownDistractingWeb("amazon.com"))
        assertTrue(KnownDistractingWeb.isKnownDistractingWeb("ebay.com"))
        assertTrue(KnownDistractingWeb.isKnownDistractingWeb("temu.com"))
        assertTrue(KnownDistractingWeb.isKnownDistractingWeb("shein.com"))

        // 5. Portals themselves remain unblocked for search / main service
        assertFalse(WebClassifier.classifyDomain("naver.com", false, null).isBlocked)
        assertFalse(WebClassifier.classifyDomain("search.naver.com", false, null).isBlocked)
        assertFalse(WebClassifier.classifyDomain("baidu.com", false, null).isBlocked)

        // 6. Shopping subdomains are blocked by WebClassifier
        assertTrue(WebClassifier.classifyDomain("shopping.naver.com", false, null).isBlocked)
        assertTrue(WebClassifier.classifyDomain("m.shopping.naver.com", false, null).isBlocked)
        assertTrue(WebClassifier.classifyDomain("mall.baidu.com", false, null).isBlocked)
        assertTrue(WebClassifier.classifyDomain("youxuan.baidu.com", false, null).isBlocked)
        assertTrue(WebClassifier.classifyDomain("www.coupang.com", false, null).isBlocked)
    }
}

