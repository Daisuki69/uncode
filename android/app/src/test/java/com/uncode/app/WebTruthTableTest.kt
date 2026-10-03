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

        // 3. Baidu SERP Search Queries and Video Search Pages are research pages, NOT destination websites
        assertFalse(WebClassifier.isDestinationUrl("https://m.baidu.com/s?wd=operating+system"))
        assertFalse(WebClassifier.isDestinationUrl("https://baidu.com/s?wd=calculus"))
        assertFalse(WebClassifier.isDestinationUrl("https://www.baidu.com/s?wd=ai"))
        assertFalse(WebClassifier.isDestinationUrl("https://v.baidu.com/v?word=linear+algebra"))
        assertFalse(WebClassifier.isDestinationUrl("https://video.baidu.com/v?word=physics"))
        assertFalse(WebClassifier.isDestinationUrl("https://haokan.baidu.com/v?pd=wisenatural"))
    }

    @Test
    fun testPortalSuperAppAndSubActivityRemediation() {
        // 1. Portal Super-Apps must be recognized
        assertTrue(AppClassifier.isPortalSuperApp("com.baidu.searchbox"))
        assertTrue(AppClassifier.isPortalSuperApp("com.transsion.phoenix"))
        assertTrue(AppClassifier.isPortalSuperApp("com.UCMobile"))
        assertFalse(AppClassifier.isPortalSuperApp("com.android.chrome"))

        // 2. Portal Super-Apps are recognized as browser portals at the package level
        assertTrue(AppClassifier.isBrowserPackage(null, "com.baidu.searchbox", "Baidu"))

        // 3. Short video reels and distracting sub-activities inside portal super-apps must be identified
        assertTrue(AppClassifier.isDistractingSubActivity("com.baidu.searchbox", "com.baidu.searchbox.video.feedflow.tab.VideoTabActivity"))
        assertTrue(AppClassifier.isDistractingSubActivity("com.baidu.searchbox", "com.baidu.searchbox.discovery.novel.NovelHomeActivity"))
        assertTrue(AppClassifier.isDistractingSubActivity("com.transsion.phoenix", "com.transsion.phoenix.reels.ShortVideoActivity"))

        // 4. Primary search and browsing activities must NOT be flagged as distracting sub-activities
        assertFalse(AppClassifier.isDistractingSubActivity("com.baidu.searchbox", "com.baidu.searchbox.MainActivity"))
        assertFalse(AppClassifier.isDistractingSubActivity("com.baidu.searchbox", "com.baidu.searchbox.BoxBrowserActivity"))

        // 5. Super-app utility activities (FileManager, DownloadManager, WebActivity, etc.) must be recognized
        assertTrue(AppClassifier.isSuperAppUtilityActivity("com.baidu.searchbox", "com.baidu.searchbox.download.center.ui.fusion.FileManagerActivity"))
        assertTrue(AppClassifier.isSuperAppUtilityActivity("com.baidu.searchbox", "com.baidu.searchbox.download.center.ui.fusion.DownloadManagerActivity"))
        assertTrue(AppClassifier.isSuperAppUtilityActivity("com.baidu.searchbox", "com.baidu.searchbox.BoxBrowserActivity"))
        assertTrue(AppClassifier.isSuperAppUtilityActivity("com.baidu.searchbox", "com.baidu.searchbox.WebActivity"))
        assertFalse(AppClassifier.isSuperAppUtilityActivity("com.baidu.searchbox", "com.baidu.searchbox.video.feedflow.tab.VideoTabActivity"))
        assertFalse(AppClassifier.isSuperAppUtilityActivity("com.android.chrome", "com.google.android.apps.chrome.Main"))
    }
}
