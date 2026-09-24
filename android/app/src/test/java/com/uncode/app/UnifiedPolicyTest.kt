package com.uncode.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UnifiedPolicyTest {

    @Test
    fun testYouTubeInKnownDistracting() {
        // Official YouTube app packages
        assertTrue(KnownDistracting.isKnownDistracting("com.google.android.youtube", "YouTube"))
        assertTrue(KnownDistracting.isKnownDistracting("com.google.android.youtube.tv", "YouTube"))
        assertTrue(KnownDistracting.isKnownDistracting("com.google.android.apps.youtube.kids", "YouTube Kids"))
        // Third-party YouTube clients
        assertTrue(KnownDistracting.isKnownDistracting("app.revanced.android.youtube", "ReVanced"))
        assertTrue(KnownDistracting.isKnownDistracting("org.schabi.newpipe", "NewPipe"))
        assertTrue(KnownDistracting.isKnownDistracting("app.libre_tube", "LibreTube"))
    }

    @Test
    fun testAiInKnownDistracting() {
        // AI Chatbots & Assistants
        assertTrue(KnownDistracting.isKnownDistracting("com.openai.chatgpt", "ChatGPT"))
        assertTrue(KnownDistracting.isKnownDistracting("com.google.android.apps.gemini", "Gemini"))
        assertTrue(KnownDistracting.isKnownDistracting("com.google.android.apps.bard", "Bard"))
        assertTrue(KnownDistracting.isKnownDistracting("com.anthropic.claude", "Claude"))
        assertTrue(KnownDistracting.isKnownDistracting("ai.perplexity.app", "Perplexity"))
        assertTrue(KnownDistracting.isKnownDistracting("com.microsoft.copilot", "Copilot"))
        assertTrue(KnownDistracting.isKnownDistracting("com.poe.android", "Poe"))
        assertTrue(KnownDistracting.isKnownDistracting("com.characterai.chat", "Character.ai"))
    }

    @Test
    fun testUnifiedPolicyRegistryStructure() {
        val yt = UnifiedPolicyRegistry.SERVICES["youtube"]
        assertNotNull(yt)
        assertEquals("YouTube", yt?.displayName)
        assertEquals("Video", yt?.iconName)
        assertEquals("red", yt?.themeColor)
        assertTrue(yt?.packages?.contains("com.google.android.youtube") == true)
        assertTrue(yt?.domains?.contains("youtube.com") == true)

        val ai = UnifiedPolicyRegistry.SERVICES["ai"]
        assertNotNull(ai)
        assertEquals("AI", ai?.displayName)
        assertEquals("Sparkles", ai?.iconName)
        assertEquals("purple", ai?.themeColor)
        assertTrue(ai?.packages?.contains("com.openai.chatgpt") == true)
        assertTrue(ai?.packages?.contains("com.anthropic.claude") == true)
        assertTrue(ai?.packages?.contains("com.google.android.apps.gemini") == true)
        assertTrue(ai?.domains?.contains("chatgpt.com") == true)
        assertTrue(ai?.domains?.contains("claude.ai") == true)
        assertTrue(ai?.domains?.contains("gemini.google.com") == true)
    }

    @Test
    fun testUnifiedPolicyResolution() {
        // When AI is enabled, AI packages are allowed; YouTube is not
        val aiOnly = setOf("ai")
        assertTrue(UnifiedPolicyRegistry.isPackageAllowedByService("com.openai.chatgpt", aiOnly))
        assertTrue(UnifiedPolicyRegistry.isPackageAllowedByService("com.anthropic.claude", aiOnly))
        assertTrue(UnifiedPolicyRegistry.isPackageAllowedByService("com.google.android.apps.gemini", aiOnly))
        assertFalse(UnifiedPolicyRegistry.isPackageAllowedByService("com.google.android.youtube", aiOnly))

        // When YouTube is enabled, YouTube packages are allowed; AI is not
        val ytOnly = setOf("youtube")
        assertTrue(UnifiedPolicyRegistry.isPackageAllowedByService("com.google.android.youtube", ytOnly))
        assertFalse(UnifiedPolicyRegistry.isPackageAllowedByService("com.openai.chatgpt", ytOnly))

        // When both enabled
        val both = setOf("youtube", "ai")
        assertTrue(UnifiedPolicyRegistry.isPackageAllowedByService("com.google.android.youtube", both))
        assertTrue(UnifiedPolicyRegistry.isPackageAllowedByService("com.openai.chatgpt", both))

        // When neither enabled
        val none = emptySet<String>()
        assertFalse(UnifiedPolicyRegistry.isPackageAllowedByService("com.google.android.youtube", none))
        assertFalse(UnifiedPolicyRegistry.isPackageAllowedByService("com.openai.chatgpt", none))
    }

    @Test
    fun testDomainResolution() {
        val aiOnly = setOf("ai")
        assertTrue(UnifiedPolicyRegistry.isDomainAllowedByService("chatgpt.com", aiOnly))
        assertTrue(UnifiedPolicyRegistry.isDomainAllowedByService("api.openai.com", aiOnly))
        assertTrue(UnifiedPolicyRegistry.isDomainAllowedByService("claude.ai", aiOnly))
        assertFalse(UnifiedPolicyRegistry.isDomainAllowedByService("youtube.com", aiOnly))

        val ytOnly = setOf("youtube")
        assertTrue(UnifiedPolicyRegistry.isDomainAllowedByService("youtube.com", ytOnly))
        assertTrue(UnifiedPolicyRegistry.isDomainAllowedByService("m.youtube.com", ytOnly))
        assertTrue(UnifiedPolicyRegistry.isDomainAllowedByService("youtu.be", ytOnly))
        assertFalse(UnifiedPolicyRegistry.isDomainAllowedByService("chatgpt.com", ytOnly))
    }

    @Test
    fun testIsPackageRegisteredInAnyService() {
        assertTrue(UnifiedPolicyRegistry.isPackageRegisteredInAnyService("com.google.android.apps.gemini"))
        assertTrue(UnifiedPolicyRegistry.isPackageRegisteredInAnyService("com.google.android.apps.bard"))
        assertTrue(UnifiedPolicyRegistry.isPackageRegisteredInAnyService("com.openai.chatgpt"))
        assertTrue(UnifiedPolicyRegistry.isPackageRegisteredInAnyService("com.google.android.youtube"))
        assertTrue(UnifiedPolicyRegistry.isPackageRegisteredInAnyService("ai.x.grok"))
        assertFalse(UnifiedPolicyRegistry.isPackageRegisteredInAnyService("com.google.android.googlequicksearchbox"))
        assertFalse(UnifiedPolicyRegistry.isPackageRegisteredInAnyService("com.spotify.music"))
    }

    @Test
    fun testSubpathDomainResolution() {
        val aiOnly = setOf("ai")
        // Direct conversation subpaths must match their canonical root domains
        assertTrue(UnifiedPolicyRegistry.isDomainAllowedByService("chatgpt.com/c/68fa9123-4567", aiOnly))
        assertTrue(UnifiedPolicyRegistry.isDomainAllowedByService("https://chatgpt.com/c/test-convo-id", aiOnly))
        assertTrue(UnifiedPolicyRegistry.isDomainAllowedByService("gemini.google.com/app", aiOnly))
        assertTrue(UnifiedPolicyRegistry.isDomainAllowedByService("https://gemini.google.com/app/12345", aiOnly))
        assertTrue(UnifiedPolicyRegistry.isDomainAllowedByService("https://claude.ai/chat/abcd", aiOnly))
        assertTrue(UnifiedPolicyRegistry.isDomainAllowedByService("https://perplexity.ai/search/query", aiOnly))
        assertTrue(UnifiedPolicyRegistry.isDomainAllowedByService("chatgpt.com:443/c/123", aiOnly))

        // Companion AI CDN and asset domains
        assertTrue(UnifiedPolicyRegistry.isDomainAllowedByService("cdn.oaistatic.com/assets/index.js", aiOnly))
        assertTrue(UnifiedPolicyRegistry.isDomainAllowedByService("oaiusercontent.com/files/test.png", aiOnly))
        assertTrue(UnifiedPolicyRegistry.isDomainAllowedByService("claudeusercontent.com/blob/123", aiOnly))
        assertTrue(UnifiedPolicyRegistry.isDomainAllowedByService("pplx.ai/share/123", aiOnly))

        // Unrelated domains must not match
        assertFalse(UnifiedPolicyRegistry.isDomainAllowedByService("wikipedia.org/wiki/ChatGPT", aiOnly))
        assertFalse(UnifiedPolicyRegistry.isDomainAllowedByService("google.com/search?q=gemini", aiOnly))
        assertFalse(UnifiedPolicyRegistry.isDomainAllowedByService("github.com/openai", aiOnly))
    }

    @Test
    fun testExtractHostSafety() {
        assertEquals("chatgpt.com", WebBlocklistConstants.extractHost("chatgpt.com/c/12345"))
        assertEquals("chatgpt.com", WebBlocklistConstants.extractHost("https://chatgpt.com/c/12345?ref=share#heading"))
        assertEquals("gemini.google.com", WebBlocklistConstants.extractHost("https://gemini.google.com/app"))
        assertEquals("gemini.google.com", WebBlocklistConstants.extractHost("gemini.google.com:443/app"))
        assertEquals("cdn.oaistatic.com", WebBlocklistConstants.extractHost("https://cdn.oaistatic.com/v2/chunk.js"))
    }
}
