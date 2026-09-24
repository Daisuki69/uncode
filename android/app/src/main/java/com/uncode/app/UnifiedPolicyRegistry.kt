package com.uncode.app

import java.util.Locale

/**
 * Unified Service Policy Registry.
 *
 * Couples Android application package IDs and web host domains into a single,
 * type-safe data structure. When a user enables a service in the App UI, both the
 * native application packages and web browser domains are simultaneously allowed.
 *
 * Zero runtime string parsing: evaluates via O(1) HashSet lookups.
 */
data class UnifiedService(
    val id: String,
    val displayName: String,
    val description: String = "",
    val badge: String = "App + Web",
    val iconName: String = "Globe",
    val themeColor: String = "indigo",
    val packages: Set<String> = emptySet(),
    val domains: Set<String> = emptySet()
)

object UnifiedPolicyRegistry {

    @JvmField
    val SERVICES: Map<String, UnifiedService> = mapOf(
        "youtube" to UnifiedService(
            id = "youtube",
            displayName = "YouTube",
            description = "Allows YouTube application and YouTube web browser domains.",
            badge = "App + Web",
            iconName = "Video",
            themeColor = "red",
            packages = setOf(
                "com.google.android.youtube",
                "com.google.android.youtube.tv",
                "com.google.android.apps.youtube.unplugged",
                "com.google.android.apps.youtube.kids",
                "app.revanced.android.youtube",
                "org.schabi.newpipe",
                "app.libre_tube",
                "org.polymc.tubular"
            ),
            domains = setOf(
                "youtube.com",
                "youtu.be",
                "m.youtube.com"
            )
        ),
        "ai" to UnifiedService(
            id = "ai",
            displayName = "AI",
            description = "Allows AI assistant applications (Gemini, ChatGPT, Claude, etc.) and web domains.",
            badge = "App + Web",
            iconName = "Sparkles",
            themeColor = "purple",
            packages = setOf(
                "com.google.android.apps.gemini",
                "com.google.android.apps.bard",
                "com.openai.chatgpt",
                "com.anthropic.claude",
                "ai.perplexity.app",
                "ai.perplexity.app.android",
                "com.microsoft.copilot",
                "com.poe.android",
                "com.quora.poe.android",
                "com.characterai.chat",
                "com.deepseek.chat",
                "ai.inflection.pi",
                "ai.x.grok"
            ),
            domains = setOf(
                "gemini.google.com",
                "bard.google.com",
                "chatgpt.com",
                "chat.openai.com",
                "openai.com",
                "claude.ai",
                "anthropic.com",
                "perplexity.ai",
                "copilot.microsoft.com",
                "poe.com",
                "character.ai",
                "deepseek.com",
                "pi.ai",
                "grok.com",
                "x.ai",
                "oaistatic.com",
                "oaiusercontent.com",
                "claudeusercontent.com",
                "pplx.ai"
            )
        )
    )

    @JvmStatic
    fun isPackageRegisteredInAnyService(pkg: String?): Boolean {
        if (pkg == null) return false
        val lowerPkg = pkg.lowercase(Locale.US).trim()
        for (service in SERVICES.values) {
            if (service.packages.contains(lowerPkg)) {
                return true
            }
        }
        return false
    }

    @JvmStatic
    fun getServiceForPackage(pkg: String?): UnifiedService? {
        if (pkg == null) return null
        val lowerPkg = pkg.lowercase(Locale.US).trim()
        for (service in SERVICES.values) {
            if (service.packages.contains(lowerPkg)) {
                return service
            }
        }
        return null
    }

    @JvmStatic
    fun getPackagesForServices(activeServiceIds: Set<String>?): Set<String> {
        val result = HashSet<String>()
        if (activeServiceIds.isNullOrEmpty()) return result
        for (id in activeServiceIds) {
            val key = id.lowercase(Locale.US).trim()
            SERVICES[key]?.packages?.let { result.addAll(it) }
        }
        return result
    }

    @JvmStatic
    fun getDomainsForServices(activeServiceIds: Set<String>?): Set<String> {
        val result = HashSet<String>()
        if (activeServiceIds.isNullOrEmpty()) return result
        for (id in activeServiceIds) {
            val key = id.lowercase(Locale.US).trim()
            SERVICES[key]?.domains?.let { result.addAll(it) }
        }
        return result
    }

    @JvmStatic
    fun isPackageAllowedByService(pkg: String?, activeServiceIds: Set<String>?): Boolean {
        if (pkg == null || activeServiceIds.isNullOrEmpty()) return false
        val lowerPkg = pkg.lowercase(Locale.US).trim()
        for (id in activeServiceIds) {
            val key = id.lowercase(Locale.US).trim()
            val service = SERVICES[key] ?: continue
            if (service.packages.contains(lowerPkg)) {
                return true
            }
        }
        return false
    }

    @JvmStatic
    fun isDomainAllowedByService(host: String?, activeServiceIds: Set<String>?): Boolean {
        if (host == null || activeServiceIds.isNullOrEmpty()) return false
        val cleanHost = WebBlocklistConstants.extractHost(host)
        if (cleanHost.isEmpty()) return false
        for (id in activeServiceIds) {
            val key = id.lowercase(Locale.US).trim()
            val service = SERVICES[key] ?: continue
            for (domain in service.domains) {
                if (cleanHost == domain || cleanHost.endsWith(".$domain")) {
                    return true
                }
            }
        }
        return false
    }
}
