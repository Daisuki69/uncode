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

        // 5. DoH Canary -> Sinkholed
        val dohDns = WebClassifier.classifyDomain("use-application-dns.net", false)
        assertTrue(dohDns.isBlocked)
    }
}
