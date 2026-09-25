package com.uncode.app

import java.util.Locale

/**
 * WebBlocklistConstants:
 * Central bridge and utility constants for web domain processing.
 *
 * Distraction domain sets are authoritatively maintained in [KnownDistractingWeb].
 * Academic safe-list and institutional domains are authoritatively maintained in [KnownSafeWeb].
 *
 * Shared between LocalDnsVpnService (DNS socket layer) and
 * LockAccessibilityService (real-time DOM & address bar layer).
 */
object WebBlocklistConstants {

    // ── Distracting Social Media, Video Loops & Algorithmic Feeds ──
    @JvmField
    val BLACKLISTED_WEB_DOMAINS: Set<String> = KnownDistractingWeb.SOCIAL_MEDIA_DOMAINS

    // ── YouTube Core Domains ──
    @JvmField
    val YOUTUBE_DOMAINS: Set<String> = KnownDistractingWeb.YOUTUBE_DOMAINS

    // ── Web-Based Gaming Database ──
    @JvmField
    val WEB_GAMING_DOMAINS: Set<String> = KnownDistractingWeb.GAMING_DOMAINS

    // ── Online Gambling & Sportsbooks ──
    @JvmField
    val GAMBLING_DOMAINS: Set<String> = KnownDistractingWeb.GAMBLING_DOMAINS

    // ── Adult & Explicit Content ──
    @JvmField
    val ADULT_DOMAINS: Set<String> = KnownDistractingWeb.ADULT_DOMAINS

    // ── Web Proxies & Bypass Tunnels ──
    @JvmField
    val PROXY_DOMAINS: Set<String> = KnownDistractingWeb.PROXY_DOMAINS

    // ── Piracy Streaming, Manga & Short Dramas ──
    @JvmField
    val PIRACY_AND_MEDIA_DOMAINS: Set<String> = KnownDistractingWeb.PIRACY_AND_MEDIA_DOMAINS

    // ── Dating & Random Cam Chat ──
    @JvmField
    val DATING_AND_CHAT_DOMAINS: Set<String> = KnownDistractingWeb.DATING_AND_CHAT_DOMAINS

    // ── Gossip & Viral Time-Wasters ──
    @JvmField
    val TIME_WASTER_DOMAINS: Set<String> = KnownDistractingWeb.TIME_WASTER_DOMAINS

    // ── Crypto Speculation & Meme Coin Casinos ──
    @JvmField
    val CRYPTO_SPECULATION_DOMAINS: Set<String> = KnownDistractingWeb.CRYPTO_SPECULATION_DOMAINS

    // ── Vast Academic, Research & Developer Safe-List ──
    @JvmField
    val ACADEMIC_EXEMPT_DOMAINS: Set<String> = KnownSafeWeb.ACADEMIC_EXEMPT_DOMAINS

    /**
     * Checks if a domain or URL is immune under the academic whitelist.
     * Delegates authoritatively to [KnownSafeWeb.isAcademicExempt].
     */
    @JvmStatic
    fun isAcademicExempt(lowerUrl: String?): Boolean {
        return KnownSafeWeb.isAcademicExempt(lowerUrl)
    }

    /**
     * Extracts the canonical host from a domain string or full URL.
     * Strips scheme, path, query parameters, hash, port, and leading 'www.'.
     * E.g.:
     *   "https://www.y8.com/tags/2_player?ref=1#top" -> "y8.com"
     *   "y8.com/tags/2_player" -> "y8.com"
     *   "sub.example.co.uk:8080/foo" -> "sub.example.co.uk"
     *   "www.roblox.com" -> "roblox.com"
     */
    @JvmStatic
    fun extractHost(raw: String?): String {
        if (raw == null) return ""
        var s = raw.lowercase(Locale.US).trim()
        val schemeIdx = s.indexOf("://")
        if (schemeIdx != -1) {
            s = s.substring(schemeIdx + 3)
        }
        val slashIdx = s.indexOf('/')
        if (slashIdx != -1) {
            s = s.substring(0, slashIdx)
        }
        val qIdx = s.indexOf('?')
        if (qIdx != -1) {
            s = s.substring(0, qIdx)
        }
        val hIdx = s.indexOf('#')
        if (hIdx != -1) {
            s = s.substring(0, hIdx)
        }
        val colonIdx = s.indexOf(':')
        if (colonIdx != -1) {
            s = s.substring(0, colonIdx)
        }
        if (s.startsWith("www.")) {
            s = s.substring(4)
        }
        while (s.endsWith(".")) {
            s = s.substring(0, s.length - 1)
        }
        return s.trim()
    }

    /**
     * Fast domain set matcher.
     * Extracts canonical host, then tests exact match and iteratively strips subdomains.
     * E.g. "game.y8.com" tests "game.y8.com", then "y8.com".
     */
    @JvmStatic
    fun matchesDomainSet(domainOrUrl: String?, targetDomains: Set<String>?): Boolean {
        if (domainOrUrl == null || targetDomains == null || targetDomains.isEmpty()) {
            return false
        }
        val host = extractHost(domainOrUrl)
        if (host.isEmpty()) return false

        var current = host
        while (current.isNotEmpty()) {
            if (targetDomains.contains(current)) {
                return true
            }
            val dotIndex = current.indexOf('.')
            if (dotIndex == -1) {
                break
            }
            current = current.substring(dotIndex + 1)
        }
        return false
    }

    /**
     * Checks if a domain matches blacklisted social media and feeds.
     */
    @JvmStatic
    fun isBlacklistedDomain(domainOrUrl: String?): Boolean {
        return matchesDomainSet(domainOrUrl, BLACKLISTED_WEB_DOMAINS)
    }

    /**
     * Checks if a domain matches core YouTube domains.
     */
    @JvmStatic
    fun isYoutubeDomain(domainOrUrl: String?): Boolean {
        return matchesDomainSet(domainOrUrl, YOUTUBE_DOMAINS)
    }

    /**
     * Checks if a domain is a DNS-over-HTTPS (DoH) canary or provider endpoint.
     * Returning NXDOMAIN forces Chromium/Chrome to disable internal DoH and fall back to system DNS.
     */
    @JvmStatic
    fun isDohEndpointOrCanary(domainOrUrl: String?): Boolean {
        if (domainOrUrl == null) return false
        val host = extractHost(domainOrUrl)
        return host == "use-application-dns.net" || host.endsWith(".use-application-dns.net") ||
               host == "dns.google" || host.endsWith(".dns.google") ||
               host == "cloudflare-dns.com" || host.endsWith(".cloudflare-dns.com") ||
               host == "dns.quad9.net" || host.endsWith(".dns.quad9.net") ||
               host == "dns.adguard-dns.com" || host.endsWith(".dns.adguard-dns.com")
    }

    /**
     * Checks if a URL represents a dedicated Google arcade doodle link (e.g. snake_arcade).
     */
    @JvmStatic
    fun isDedicatedArcadeLink(url: String?): Boolean {
        if (url == null) return false
        val lower = url.lowercase(Locale.US).trim()
        return lower.contains("/fbx?fbx=") || lower.contains("snake_arcade") ||
               lower.contains("fbx=snake_arcade") || lower.contains("/logos/") || lower.contains("/doodles/")
    }

    /**
     * Checks if a domain or URL points to a web-based game.
     */
    @JvmStatic
    fun isWebGameDomain(domainOrUrl: String?): Boolean {
        if (domainOrUrl == null) return false
        if (matchesDomainSet(domainOrUrl, WEB_GAMING_DOMAINS)) {
            return true
        }
        val host = extractHost(domainOrUrl)
        if (host.endsWith("itch.zone") || host.contains("poki-gdn.com")) {
            return true
        }
        if (host.contains("unblocked") && (host.contains("game") || host.contains("66") || host.contains("76") || host.contains("slope"))) {
            return true
        }
        val lower = domainOrUrl.lowercase(Locale.US)
        if (lower.contains("unblocked") && (lower.contains("game") || lower.contains("66") || lower.contains("76") || lower.contains("slope"))) {
            return true
        }
        return false
    }

    /**
     * Checks if a domain or URL points to gambling/casinos/sportsbooks.
     */
    @JvmStatic
    fun isGamblingDomain(domainOrUrl: String?): Boolean {
        return matchesDomainSet(domainOrUrl, GAMBLING_DOMAINS)
    }

    /**
     * Checks if a domain or URL points to adult/explicit content.
     */
    @JvmStatic
    fun isAdultDomain(domainOrUrl: String?): Boolean {
        return matchesDomainSet(domainOrUrl, ADULT_DOMAINS)
    }

    /**
     * Checks if a domain or URL points to web proxies or bypass unblockers.
     */
    @JvmStatic
    fun isProxyDomain(domainOrUrl: String?): Boolean {
        return matchesDomainSet(domainOrUrl, PROXY_DOMAINS)
    }

    /**
     * Checks if a domain or URL points to piracy streaming, manga, or short dramas.
     */
    @JvmStatic
    fun isPiracyOrMediaDomain(domainOrUrl: String?): Boolean {
        if (domainOrUrl == null) return false
        val lower = domainOrUrl.lowercase(Locale.US)
        if (lower.contains("kisskh") || lower.contains("kissasian")) return true
        return matchesDomainSet(domainOrUrl, PIRACY_AND_MEDIA_DOMAINS)
    }

    /**
     * Checks if a domain points to dating or random video cam chat.
     */
    @JvmStatic
    fun isDatingDomain(domainOrUrl: String?): Boolean {
        return matchesDomainSet(domainOrUrl, DATING_AND_CHAT_DOMAINS)
    }

    /**
     * Checks if a domain points to time-wasting viral gossip.
     */
    @JvmStatic
    fun isTimeWasterDomain(domainOrUrl: String?): Boolean {
        return matchesDomainSet(domainOrUrl, TIME_WASTER_DOMAINS)
    }

    /**
     * Checks if a domain points to crypto meme coin gambling/speculation.
     */
    @JvmStatic
    fun isCryptoSpeculationDomain(domainOrUrl: String?): Boolean {
        return matchesDomainSet(domainOrUrl, CRYPTO_SPECULATION_DOMAINS)
    }
}
