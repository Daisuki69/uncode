# Append Unified Policy Flow Realignment & CDN Sinkhole Resolution entry below Phase 3A in README.md
import sys

readme_path = "README.md"
with open(readme_path, "r", encoding="utf-8") as f:
    content = f.read()

target = """      6. **User Rule 2 Compliance Check**:
         - *Action*: Confirm no APK build commands (`assembleDebug`, `assembleRelease`, `build.bat`) were executed.
         - *Expected Result*: Verified; APK compilation left entirely to user.

---"""

entry = """      6. **User Rule 2 Compliance Check**:
         - *Action*: Confirm no APK build commands (`assembleDebug`, `assembleRelease`, `build.bat`) were executed.
         - *Expected Result*: Verified; APK compilation left entirely to user.

- **Unified Policy Flow Realignment & CDN Sinkhole Resolution (Patch 28 Follow-Up)**:
  - **Why It Was Mandated (Resolving Gating Disconnect & Restoring Ground Truth Flowchart)**:
    - *Forensic Diagnosis of Policy Failure*:
      - Live on-device testing revealed that toggling on "Videos (YouTube & Baidu Video)" or "AI (ChatGPT, Gemini, Claude)" in App Settings failed to allow these services during focus lockdown or consequence mode.
      - Deep architectural investigation identified two root causes:
        1. **Native Flow Inversion**: [AppClassifier.java](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/android/app/src/main/java/com/uncode/app/AppClassifier.java) line 1133 bundled `KnownDistracting.isKnownDistracting(pkg, appLabel)` directly into `isStage1Vetoed(...)`. Because YouTube and AI assistants are classified as distracting packages, they were vetoed at Stage 1 (Master Veto Gate). Both [LockAccessibilityService.java](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/android/app/src/main/java/com/uncode/app/LockAccessibilityService.java) and [AppClassifier.java](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/android/app/src/main/java/com/uncode/app/AppClassifier.java) short-circuited at Stage 1, rendering Stage 2's Dual Gate Truth Table (`YES KnownDistracting, YES KnownSafe -> ALLOW`) unreachable dead code.
        2. **Web CDN Sinkholing**: `UnifiedPolicyRegistry.SERVICES["youtube"].domains` and [WebClassifier.kt](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/android/app/src/main/java/com/uncode/app/WebClassifier.kt) included `youtube.com` and `m.youtube.com` but omitted video streaming chunk domains (`googlevideo.com`) and asset/thumbnail CDNs (`ytimg.com`, `youtube-nocookie.com`). When users navigated to YouTube in a browser, the initial page loaded, but streaming chunks and thumbnails were classified as distracting without being in `effectiveAllowed`. Consequently, [LocalDnsVpnService.kt](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/android/app/src/main/java/com/uncode/app/LocalDnsVpnService.kt) sinkholed them to `NXDOMAIN` (0.0.0.0), crashing video playback.
    - *Realignment with Ground Truth 3-Stage Pipeline*:
      - Realigned the native gating logic with the authoritative architectural flowchart (`README.md:2102-2125`):
        - **Stage 1 (Master Veto Gate)** evaluates strictly: (1) Anti-Tamper (`isSettingsOrDeviceManager`: Settings, Device Care, Phone Managers, Setup Wizards, Provisioners), (2) Hardware Task Killers (`isStage1Bloat`: Joyose, GameSpace, Glance, PalmStore), and (3) Hostile Circumvention / Evasion Proxies (`isHostileEvasionProxy`: Tor, Psiphon, bypass VPN tunnels, multilingual proxy tools). Consumer distractions (TikTok, games, YouTube, AI) pass Stage 1.
        - **Stage 2 (Dual Gate Truth Table)** evaluates `KnownDistracting` against `KnownSafe` (incorporating active Unified Policy services). Unchecked distractions (games, social media) are blocked, while user-allowed services (YouTube, AI) are granted execution (`YES KnownDistracting, YES KnownSafe -> ALLOW`).
        - **Stage 3 (Universal System Gateway)** evaluates system partition OEM apps (`SYSALLOW`).
      - Preserved Home launcher vetting in [InstalledLauncherDetector.kt](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/android/app/src/main/java/com/uncode/app/InstalledLauncherDetector.kt) and [LauncherStateManager.kt](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/android/app/src/main/java/com/uncode/app/LauncherStateManager.kt) by explicitly enforcing `isStage1Vetoed(pkg) || KnownDistracting.isKnownDistracting(pkg)`.
  - **Concrete Architectural Implementations**:
    - **`AppClassifier.java` (`android/app/src/main/java/com/uncode/app/AppClassifier.java`)**:
      - Restructured `isStage1Vetoed(pkg, appLabel, activityCls)`: Removed `KnownDistracting.isKnownDistracting(pkg, appLabel)` and replaced with `isHostileEvasionProxy(pkg, appLabel)`.
      - Preserved Master Veto protection against settings, provisioners, device managers, task-killing bloatware, and evasion proxies.
      - Re-enabled Stage 2 Dual Gate Truth Table execution in `isPackageBlocked(Context, String, Set<String>)`.
    - **`KnownDistracting.kt` (`android/app/src/main/java/com/uncode/app/KnownDistracting.kt`)**:
      - Added `HOSTILE_EVASION_PACKAGES` set containing 33 known circumvention tools, onion browsers, cloud renderers, and bypass VPNs.
      - Added `@JvmStatic fun isHostileEvasionProxy(packageName: String?, appLabel: String?): Boolean`.
      - Added `@JvmStatic fun hasEvasionPackageSignature(lower: String): Boolean` and `@JvmStatic fun hasEvasionLabelSignature(lowerLabel: String): Boolean` matching multilingual evasion keywords across Russian, Chinese, Japanese, Arabic, and Spanish.
    - **`UnifiedPolicyRegistry.kt` (`android/app/src/main/java/com/uncode/app/UnifiedPolicyRegistry.kt`)**:
      - Added `"googlevideo.com"`, `"ytimg.com"`, and `"youtube-nocookie.com"` to `SERVICES["youtube"].domains`.
    - **`WebClassifier.kt` (`android/app/src/main/java/com/uncode/app/WebClassifier.kt`)**:
      - Updated `buildEffectiveAllowed(allowYoutube, allowedDomains)` to dynamically include all domains from `UnifiedPolicyRegistry.SERVICES["youtube"]` when `allowYoutube == true`.
      - Delegated `classifyDomain(domain, allowYoutube, allowedDomains)` directly to `buildEffectiveAllowed` to ensure single-source parity between DNS sinkhole and URL inspection.
    - **`InstalledLauncherDetector.kt` & `LauncherStateManager.kt`**:
      - Enforced dual veto `AppClassifier.isStage1Vetoed(pkg, null) || KnownDistracting.isKnownDistracting(pkg)` in launcher candidate validation and preference persistence.
    - **Unit Test Synchronization**:
      - Synchronized [HomeHandlerTest.kt](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/android/app/src/test/java/com/uncode/app/HomeHandlerTest.kt), [WebTruthTableTest.kt](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/android/app/src/test/java/com/uncode/app/WebTruthTableTest.kt), and [UnifiedPolicyTest.kt](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/android/app/src/test/java/com/uncode/app/UnifiedPolicyTest.kt) to validate evasion proxies, DNS CDN allowance, and Truth Table policy overrides.
  - **Comprehensive Verification Plan & Matrix (User Rule 3)**:
    - *Affected Files*:
      - [`android/app/src/main/java/com/uncode/app/AppClassifier.java`](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/android/app/src/main/java/com/uncode/app/AppClassifier.java)
      - [`android/app/src/main/java/com/uncode/app/KnownDistracting.kt`](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/android/app/src/main/java/com/uncode/app/KnownDistracting.kt)
      - [`android/app/src/main/java/com/uncode/app/UnifiedPolicyRegistry.kt`](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/android/app/src/main/java/com/uncode/app/UnifiedPolicyRegistry.kt)
      - [`android/app/src/main/java/com/uncode/app/WebClassifier.kt`](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/android/app/src/main/java/com/uncode/app/WebClassifier.kt)
      - [`android/app/src/main/java/com/uncode/app/InstalledLauncherDetector.kt`](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/android/app/src/main/java/com/uncode/app/InstalledLauncherDetector.kt)
      - [`android/app/src/main/java/com/uncode/app/LauncherStateManager.kt`](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/android/app/src/main/java/com/uncode/app/LauncherStateManager.kt)
      - [`android/app/src/test/java/com/uncode/app/HomeHandlerTest.kt`](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/android/app/src/test/java/com/uncode/app/HomeHandlerTest.kt)
      - [`android/app/src/test/java/com/uncode/app/WebTruthTableTest.kt`](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/android/app/src/test/java/com/uncode/app/WebTruthTableTest.kt)
      - [`android/app/src/test/java/com/uncode/app/UnifiedPolicyTest.kt`](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/android/app/src/test/java/com/uncode/app/UnifiedPolicyTest.kt)
      - [`README.md`](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/README.md)
    - *Known Dependents & Callers*:
      - `LockAccessibilityService.java`: Direct Caller (Foreground Window Interceptor) querying `AppClassifier.isStage1Vetoed(...)` and `AppClassifier.isPackageBlocked(this, pkg, whitelist)`.
      - `LocalDnsVpnService.kt`: Direct Caller (Network DNS) querying `WebClassifier.classifyDomain(domain, allowYoutube, allowedDomains)`.
      - `KnownSafe.kt`: Direct Caller querying `UnifiedPolicyRegistry.isPackageAllowedByService(...)` to populate `KnownSafe`.
      - `LockPlugin.kt`: Direct Caller synchronizing `uncode_lock.xml` preferences and invalidating decision caches.
      - `InstalledLauncherDetector.kt` & `LauncherStateManager.kt`: Direct Callers vetting launchers against Stage 1 veto and distractions.
    - *Step-by-Step On-Device Verification Instructions (Human-Facing Sensory Protocol - No ADB)*:
      1. **Native YouTube App with Unified Policy Enabled**:
         - *Action*: In App Settings, toggle **Videos (YouTube & Baidu Video)** to **ON**. Start an active focus lockdown session. From home screen or app drawer, launch native YouTube (`com.google.android.youtube`) or YouTube ReVanced.
         - *Expected Result*: YouTube opens immediately and remains active without lock overlay or auto-back eviction. Videos can be searched and watched while floating timer bubble remains visible.
      2. **Native YouTube App with Unified Policy Disabled**:
         - *Action*: In App Settings, toggle **Videos (YouTube & Baidu Video)** to **OFF**. Start an active focus lockdown session. Launch native YouTube.
         - *Expected Result*: Within 200ms, YouTube is evicted back to the launcher with a lock warning toast (*"Focus Mode Active"*).
      3. **Web YouTube Browsing & Video Streaming (CDN Verification)**:
         - *Action*: With YouTube toggle **ON** during lockdown, open Chrome or Samsung Internet and navigate to `https://m.youtube.com`. Tap on an educational video.
         - *Expected Result*: Home feed video thumbnails render crisply (`ytimg.com` allowed). Video stream immediately plays without infinite buffering spinners or network errors (`googlevideo.com` allowed).
         - *Action (Toggle OFF)*: With YouTube toggle **OFF**, navigate to `https://m.youtube.com`.
         - *Expected Result*: Browser tab is immediately auto-backed or closed with a warning toast (*"YouTube is blocked during focus mode"*).
      4. **AI Assistant App & Web Gating (Gemini / ChatGPT)**:
         - *Action*: In App Settings, toggle **AI** to **ON**. Start lockdown. Launch ChatGPT or Gemini app; visit `https://chatgpt.com` or `https://gemini.google.com` in browser.
         - *Expected Result*: Native apps and web endpoints open and interact normally.
         - *Action (Toggle OFF)*: Toggle **AI** to **OFF**. Launch ChatGPT or Gemini.
         - *Expected Result*: App launch triggers instant eviction; web navigation triggers immediate auto-back.
      5. **Anti-Tamper Shield & Bloatware Isolation (Stage 1 Master Veto Gate Integrity)**:
         - *Action*: While in lockdown (with YouTube or AI toggled ON), attempt to open Android **Settings** (`com.android.settings`), App Info for QIEZKA, or Game Turbo/Joyose bloatware.
         - *Expected Result*: Instantly evicted by Stage 1 Master Veto with zero delay, preserving anti-tamper security.
      6. **Consumer Distraction Eviction (Games & Social Feeds)**:
         - *Action*: While in lockdown (with YouTube or AI toggled ON), attempt to launch **TikTok** (`com.zhiliaoapp.musically`), **Instagram**, **Roblox**, or a mobile game.
         - *Expected Result*: Instantly evicted back to the launcher within 200ms. Enabling YouTube policy does not leak access to games or social media feeds.
      7. **Home Launcher Navigation & Floating Bubble**:
         - *Action*: From YouTube or ChatGPT, press the Home gesture or button.
         - *Expected Result*: Returns smoothly to the default launcher. Floating countdown bubble remains visible and interactive.
      8. **Automated Gradle & TypeScript Validation**:
         - *Action*: Run `.\android\gradlew.bat -p android compileDebugKotlin compileDebugJavaWithJavac testDebugUnitTest` and `npx tsc --noEmit`.
         - *Expected Result*: Kotlin and Java compilation succeed (`BUILD SUCCESSFUL in 36s`), all 14 unit test suites pass with 0 failures, and TypeScript compiler reports 0 errors.
      9. **User Rule 2 Compliance Check**:
         - *Action*: Confirm no APK build commands (`assembleDebug`, `assembleRelease`, `build.bat`) were executed.
         - *Expected Result*: Verified; APK compilation left entirely to user.

---"""

if target in content:
    new_content = content.replace(target, entry, 1)
    with open(readme_path, "w", encoding="utf-8") as f:
        f.write(new_content)
    print("SUCCESS: Appended follow-up entry to README.md")
else:
    print("ERROR: Target string not found in README.md")
    sys.exit(1)
