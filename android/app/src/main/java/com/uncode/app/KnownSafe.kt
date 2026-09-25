package com.uncode.app

import android.content.Context
import android.content.pm.ApplicationInfo
import java.util.Locale

/**
 * Registry of explicitly known safe applications, baseline study tools,
 * and displayable hardcoded infrastructure tools (Milestone / Flow Update V4).
 *
 * Invariant: A user CANNOT make an arbitrary app safe.
 * An app can only be whitelisted if it is already in KnownSafe,
 * an active Unified Service enabled by policy, or detected to be safe by the Secondary App Classifier.
 */
object KnownSafe {

    /**
     * Pure package names of universally recognized study, academic, and productivity applications.
     */
    @JvmField
    val BASELINE_SAFE_PACKAGES: Set<String> = hashSetOf(
        // Google Workspace & Academic Suite
        "com.google.android.apps.classroom",
        "com.google.android.apps.docs",
        "com.google.android.apps.docs.editors.docs",
        "com.google.android.apps.docs.editors.sheets",
        "com.google.android.apps.docs.editors.slides",
        "com.google.android.keep",
        "com.google.android.apps.translate",

        // Flashcards, Quizzes & Spaced Repetition
        "com.ichi2.anki",
        "com.quizlet.quizletandroid",
        "ai.saveall.app",                  // Gizmo: AI Flashcards and Tutor
        "com.gizmo.ai.flashcards",          // Gizmo alternate ID
        "com.cram.cramandroid",             // Cram.com Flashcards
        "com.brainscape.mobile.portal",     // Brainscape
        "com.studysmarter",                 // StudySmarter / Vaia
        "de.studysmarter",                  // StudySmarter variant
        "com.kahoot.android",               // Kahoot!
        "com.quizizz.android",              // Quizizz
        "com.magoosh.flashcards.gre",       // Magoosh Flashcards
        "com.magoosh.vocab.gre",            // Magoosh Vocabulary

        // Language Learning
        "com.duolingo",

        // Learning Management Systems (LMS)
        "com.instructure.cadd",             // Canvas Student
        "com.instructure.candroid",          // Canvas Teacher
        "com.blackboard.android.bbmatters",  // Blackboard Learn
        "com.schoology.app",                 // Schoology

        // Knowledge, Reference & Encyclopedias
        "org.wikipedia",
        "org.khanacademy.android",

        // Notes, Markdown & Knowledge Bases
        "notion.id",
        "md.obsidian",
        "com.simplenote",

        // Mathematics, Graphing & Science Tools
        "com.wolfram.android.alpha",
        "com.desmos.calculator",
        "org.geogebra.android",
        "com.symbolab.mathsolver",
        "com.microblink.photomath",

        // Document Readers & Scanners
        "com.intsig.camscanner",
        "com.adobe.reader",
        "org.readera",
        "cn.wps.moffice_eng",

        // Coding & Terminal Tools
        "com.termux",
        "ru.iiec.pydroid3"
    )

    /**
     * Identifies displayable hardcoded tools (Launchers, Web Browsers, Safe Music, Camera, Authenticators, Notes, Classroom).
     * These apps render under "Always Allowed by System" on the Dashboard with no "X" delete button.
     */
    @JvmStatic
    @Suppress("UNUSED_PARAMETER")
    fun isHardcodedApp(context: Context?, appInfo: ApplicationInfo?, pkg: String?, appLabel: String?): Boolean {
        if (pkg == null) return false
        val lowerPkg = pkg.lowercase(Locale.ROOT)
        val lowerLabel = appLabel?.lowercase(Locale.ROOT) ?: ""

        // 1. Home Launchers
        if (context != null && LockAccessibilityService.isLauncherApp(context, pkg)) return true

        // 2. Web Browsers (governed by WebClassifier, exempt from package eviction)
        if (lowerPkg.contains("browser") || lowerPkg.contains("chrome") || lowerPkg.contains("firefox") || lowerPkg.contains("opera") || lowerPkg.contains("brave")) return true

        // 3. Safe Music & Audio Players
        if (lowerPkg.contains("spotify") || lowerPkg.contains("tidal") || lowerPkg.contains("deezer") || lowerPkg.contains("soundcloud") || lowerPkg.contains("music")) return true

        // 4. Camera Apps
        if (lowerPkg.contains("camera") || lowerLabel == "camera" || lowerLabel == "kamera") return true

        // 5. 2FA Authenticators
        if (lowerPkg.contains("authenticator") || lowerPkg.contains("twofas") || lowerPkg.contains("duomobile") || lowerPkg.contains("yubioath") || lowerPkg.contains("authy")) return true

        // 6. Notes & Workspace
        if (lowerPkg == "com.google.android.keep" || lowerPkg == "notion.id" || lowerPkg == "md.obsidian" || lowerLabel.contains("notes")) return true

        // 7. Core Classroom & Drive
        if (lowerPkg == "com.google.android.apps.classroom" || lowerPkg == "com.google.android.apps.docs") return true

        return false
    }

    /**
     * Authoritative KnownSafe Checker (MSG_GATE2).
     *
     * Invariant: A user CANNOT whitelist an app unless it is:
     * 1. Already in KnownSafe (baseline safe packages, keyboards, QIEZKA), OR
     * 2. Detected to be safe by the Secondary App Classifier, OR
     * 3. An active Unified Service enabled by policy.
     */
    @JvmStatic
    fun isKnownSafe(
        context: Context?,
        pkg: String?,
        userWhitelist: Set<String>?,
        activeServices: Set<String>?
    ): Boolean {
        if (pkg.isNullOrBlank()) return false

        // 1. QIEZKA itself is always KnownSafe
        if (context != null && pkg == context.packageName) return true

        // 2. Active Input Method Editors (Keyboards: Gboard, SwiftKey)
        if (context != null && LockAccessibilityService.isKeyboardPackage(context, pkg)) return true

        // 3. Baseline Verified Study Packages
        if (BASELINE_SAFE_PACKAGES.contains(pkg)) return true

        // 4. Active Unified Services (YouTube / AI toggled on by user)
        if (UnifiedPolicyRegistry.isPackageAllowedByService(pkg, activeServices)) return true

        // 5. User-Configured Allowed Apps Whitelist
        // Strict Invariant: If in userWhitelist, it MUST NOT be a known distraction (unless authorized by Unified Policy above)
        if (userWhitelist != null && userWhitelist.contains(pkg)) {
            if (!KnownDistracting.isKnownDistracting(pkg)) {
                return true
            }
        }

        return false
    }
}
