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
    val packages: Set<String> = emptySet(),
    val domains: Set<String> = emptySet()
)

object UnifiedPolicyRegistry {

    @JvmField
    val SERVICES: Map<String, UnifiedService> = mapOf(
        "youtube" to UnifiedService(
            id = "youtube",
            displayName = "YouTube",
            packages = setOf(
                "com.google.android.youtube",
                "com.google.android.youtube.tv",
                "app.revanced.android.youtube",
                "org.schabi.newpipe",
                "app.libre_tube"
            ),
            domains = setOf(
                "youtube.com",
                "youtu.be",
                "m.youtube.com"
            )
        ),
        "gemini" to UnifiedService(
            id = "gemini",
            displayName = "Google Gemini",
            packages = setOf(
                "com.google.android.apps.gemini",
                "com.google.android.apps.bard"
            ),
            domains = setOf(
                "gemini.google.com",
                "bard.google.com"
            )
        ),
        "openai" to UnifiedService(
            id = "openai",
            displayName = "ChatGPT / OpenAI",
            packages = setOf(
                "com.openai.chatgpt"
            ),
            domains = setOf(
                "chatgpt.com",
                "chat.openai.com",
                "openai.com"
            )
        ),
        "claude" to UnifiedService(
            id = "claude",
            displayName = "Claude AI",
            packages = setOf(
                "com.anthropic.claude"
            ),
            domains = setOf(
                "claude.ai",
                "anthropic.com"
            )
        )
    )

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
        val lowerHost = host.lowercase(Locale.US).trim()
        for (id in activeServiceIds) {
            val key = id.lowercase(Locale.US).trim()
            val service = SERVICES[key] ?: continue
            for (domain in service.domains) {
                if (lowerHost == domain || lowerHost.endsWith(".$domain")) {
                    return true
                }
            }
        }
        return false
    }
}
