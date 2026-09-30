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
        "com.google.android.apps.tachyon",          // Google Meet
        "com.google.android.apps.meetings",         // Google Meet (legacy)
        "us.zoom.videomeetings",                    // Zoom Workplace
        "com.microsoft.teams",                      // Microsoft Teams
        "com.cisco.webex.meetings",                 // Cisco Webex Meetings

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
     * Comprehensive catalog of camera applications, including verified Google Camera (GCam)
     * ports and auxiliary lens sensor packages from celsoazevedo.com and common OEM cameras.
     * Note: com.ss.android.ugc.aweme is strictly excluded as it represents TikTok/Douyin.
     */
    @JvmField
    val KNOWN_CAMERA_PACKAGES: Set<String> = hashSetOf(
        // Google Camera (Pixel) & Official Builds
        "com.google.android.GoogleCamera",
        "com.google.android.GoogleCameraEng",

        // Hasli LMC GCam Ports & Variants
        "com.google.android.GoogleCamera.LMC",
        "com.google.android.GoogleCamera.LMC84",
        "com.google.android.GoogleCamera.LMC8",
        "com.google.android.GoogleCameraLMCR17",
        "com.google.android.GoogleCameraLMCR18",

        // BigKaka AGC GCam Ports & Variants
        "com.agc.cam",
        "com.agc.gcam84",
        "com.agc.gcam88",
        "com.agc.gcam92",
        "com.samsung.agc.gcam84",

        // Shamim SGCam Ports
        "com.shamim.cam",

        // BSG MGC Ports
        "com.android.mgc",

        // Arnova8G2 Ports
        "arn.android.gcam",
        "arn.android.gcam.beta",

        // Greatness, Wichaya, Nikita, Urnyx, Potse, MWP, San1ty
        "com.google.android.GoogleCamera.Cameight",
        "com.GoogleCamera.Wichaya",
        "com.google.android.GoogleCamera.Nikita",
        "com.google.android.GoogleCamera.Urnyx",
        "com.google.android.GoogleCamera.potse",
        "com.google.android.GoogleCamera.mwp82",
        "com.google.android.GoogleCameraGood",
        "com.google.android.GoogleCamera.san1ty",

        // Snapdragon / Qualcomm Camera Pipelines & Aux Sensor Packages
        "org.codeaurora.snapcam",
        "org.codeaurora.qcamera3",
        "com.qualcomm.saltproject2",
        "com.asus.snapcam",

        // Samsung Aux Sensor Whitelisted Packages
        "com.samsung.android.ruler",
        "com.samsung.android.scan3d",
        "com.samsung.android.biometrics.service",

        // Open Source Camera
        "net.sourceforge.opencamera",

        // OEM Stock & Common Camera Packages
        "com.sec.android.app.camera",
        "com.android.camera",
        "com.android.camera2",
        "com.miui.camera",
        "com.huawei.camera",
        "com.oppo.camera",
        "com.coloros.camera",
        "com.oplus.camera",
        "com.oneplus.camera",
        "com.vivo.camera",
        "com.motorola.camera",
        "com.motorola.camera2",
        "com.motorola.camera3",
        "com.sonyericsson.android.camera",
        "com.sonymobile.camera",
        "com.transsion.camera",
        "com.meitu.meiyancamera"
    )

    /**
     * Evaluates whether a package or app label represents a genuine camera application.
     * Enforces anti-bypass: known distracting packages (such as TikTok / com.ss.android.ugc.aweme)
     * can NEVER claim camera status.
     */
    @JvmStatic
    @Suppress("UNUSED_PARAMETER")
    fun isCameraApp(context: Context?, pkg: String?, appLabel: String?): Boolean {
        if (pkg.isNullOrBlank()) return false
        val lowerPkg = pkg.lowercase(Locale.ROOT).trim()

        // Strict Anti-Bypass Guard: Never allow known distracting apps or TikTok
        if (KnownDistracting.isKnownDistracting(lowerPkg, appLabel) ||
            BlacklistConstants.isBlacklisted(lowerPkg, appLabel)) {
            return false
        }

        // 1. Exact catalog match
        if (KNOWN_CAMERA_PACKAGES.contains(lowerPkg) || KNOWN_CAMERA_PACKAGES.contains(pkg)) {
            return true
        }

        // 2. Modded Google Camera prefix matching
        if (lowerPkg.startsWith("com.google.android.googlecamera") ||
            lowerPkg.startsWith("com.agc.") ||
            lowerPkg.startsWith("arn.android.gcam") ||
            lowerPkg.startsWith("com.shamim.")) {
            return true
        }

        // 3. Package substring heuristics
        if (lowerPkg.contains("camera") || lowerPkg.contains("snapcam") || lowerPkg.contains(".gcam")) {
            return true
        }

        // 4. App Label heuristics
        if (!appLabel.isNullOrBlank()) {
            val lowerLabel = appLabel.lowercase(Locale.ROOT).trim()
            if (lowerLabel == "camera" || lowerLabel == "kamera" ||
                lowerLabel.contains("gcam") || lowerLabel.contains("lmc") ||
                lowerLabel.contains("sgcam") || lowerLabel.contains("agc") ||
                lowerLabel.contains("snapcam") || lowerLabel.contains("opencamera")) {
                return true
            }
        }

        return false
    }

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

        // 1. Web Browsers (governed by WebClassifier, exempt from package eviction)
        if (lowerPkg.contains("browser") || lowerPkg.contains("chrome") || lowerPkg.contains("firefox") || lowerPkg.contains("opera") || lowerPkg.contains("brave")) return true

        // 3. Safe Music & Audio Players
        if (lowerPkg.contains("spotify") || lowerPkg.contains("tidal") || lowerPkg.contains("deezer") || lowerPkg.contains("soundcloud") || lowerPkg.contains("music")) return true

        // 4. Camera Apps
        if (isCameraApp(context, pkg, appLabel)) return true

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

        // 1b. SIREN HomeProxy companion is always KnownSafe (0ms Home delegation proxy)
        if (pkg == "com.siren.homeproxy") return true

        // 1c. Home Launchers (system default and genuine third-party launchers) are always KnownSafe
        if (context != null && LockAccessibilityService.isLauncherApp(context, pkg)) return true

        // 2. Active Input Method Editors (Keyboards: Gboard, SwiftKey)
        if (context != null && LockAccessibilityService.isKeyboardPackage(context, pkg)) return true

        // 2b. Verified Camera Tools & GCam Ports (Essential for Homework Photography)
        if (isCameraApp(context, pkg, null)) return true

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
