# Append Phase 3B Kotlin Migration entry to README.md under Patch 28 follow-ups
readme_path = "README.md"
with open(readme_path, "r", encoding="utf-8") as f:
    content = f.read()

target = "\n---\n\n## 🔮 Future Roadmap & Ecosystem Forks"

phase3b_entry = r"""

- **Phase 3B Kotlin Migration: On-Device App Intelligence Classifier (AppClassifier) (Patch 28 Follow-Up)**:
  - **Why It Was Mandated (Autonomous App Intelligence Engine & 5-Layer Classifier Hierarchy Modernization)**:
    - *Progressing the 6-Phase Master Kotlin Migration*:
      - Following the successful migration of Phase 1 (leaf managers), Phase 2 (receivers & content provider), Phase 4 (auxiliary background & network services), Phase 5 (native activity root & bridge layer), and Phase 3A (on-device web intelligence engine `WebClassifier.kt`), Phase 3B migrated the central on-device application classification intelligence engine: [AppClassifier.java](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/android/app/src/main/java/com/uncode/app/AppClassifier.java) -> [AppClassifier.kt](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/android/app/src/main/java/com/uncode/app/AppClassifier.kt).
      - `AppClassifier` serves as the authoritative on-device evaluator classifying candidate applications in $<5\text{ms}$ through Android ApplicationInfo metadata, package signatures, and semantic label heuristics, preserving QIEZKA's client-side BYOK architecture with zero token overhead and zero external server dependencies.
      - Migrating `AppClassifier` to idiomatic Kotlin consolidates null-safety across complex Android PackageManager queries, replaces legacy thread locks with concurrent lock-free collections, modernizes compatibility with Android 13–15 (API 33–35) package querying APIs, and provides `@JvmStatic` companion endpoints and `@JvmField` constants ensuring 100% binary bytecode compatibility with remaining Java services ([LockAccessibilityService.java](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/android/app/src/main/java/com/uncode/app/LockAccessibilityService.java)) while keeping the React/TypeScript frontend completely intact.
  - **Concrete Architectural Implementations**:
    - **`AppClassifier.kt` (`android/app/src/main/java/com/uncode/app/AppClassifier.kt`)**:
      - Converted core intelligence engine to Kotlin singleton `object AppClassifier`.
      - *Modern Android 13–15 (API 33–35) Compatibility*: Implemented backward-compatible helper methods `getApplicationInfoCompat`, `getPackageInfoCompat`, `queryIntentActivitiesCompat`, and `queryIntentServicesCompat` utilizing `PackageManager.ApplicationInfoFlags.of(flags)` and `PackageManager.PackageInfoFlags.of(flags)` on Android 13+ with `@Suppress("DEPRECATION")` fallbacks for Android 10–12 (API 29–32), eliminating compiler deprecation warnings and future-proofing against 64-bit flag requirements.
      - *Thread-Safe Lock-Free Concurrency*: Maintained `decisionCache` via `ConcurrentHashMap<String, Boolean>()` and `dynamicBrowserPackages` via `ConcurrentHashMap.newKeySet<String>()` providing lock-free concurrency across background accessibility scraping threads, network VPN threads, and UI/IPC threads.
      - *3-Stage Pipeline & Flowchart Realignment Preservation*:
        - Stage 1 Master Veto Gate (`isStage1Vetoed`): Evaluates anti-tamper protections (Settings, Device Care, Phone Managers, Setup Wizards, Provisioners), hardware bloatware/game boosters (Joyose, GameSpace, Glance, PalmStore), and hostile circumvention/evasion proxies (Tor, Psiphon, bypass VPNs).
        - Stage 2 Dual Gate Truth Table (`isPackageBlocked`): Evaluates `KnownDistracting` against `KnownSafe` (incorporating active `UnifiedPolicyRegistry` services). User-enabled services (YouTube, AI) are granted execution (`YES KnownDistracting, YES KnownSafe -> ALLOW`), unmanaged distractions are blocked (`YES KnownDistracting, NO KnownSafe -> BLOCK`), and safe academic tools are permitted.
        - Stage 2 Special Branches: Web browsers (`isBrowserPackage`) permitted at package level for in-depth URL/DOM inspection by `WebClassifier`; Home launchers (`KnownSafe.isLauncherApp`) permitted unconditionally.
        - Stage 3 Universal System Gateway (`isPassedStage3SystemAllow`): Automatically permits legitimate system partition OEM utilities (`FLAG_SYSTEM`) while excluding bloatware, games, or social feeds.
      - *5-Layer Local Heuristics Hierarchy (`evaluatePackage`)*:
        - Layer 1: Permanent Exemptions & SIM/Carrier Alerts (`isSimOrCarrierService`: STK, Cell Broadcast, Globe, Smart, DITO emergency services).
        - Layer 2: Fake Calculator Vaults (`isFakeCalculatorVault`: inspects genuine vs fake calculator permission requests including Camera, Storage, and System Alert Window).
        - Layer 4a: Negative Distraction Heuristics (`hasNegativeDistractionSignals`: games, gambling, rhythm games, remote desktop/VNC, virtual cloners/sandboxes, dating, short-dramas, manga, shopping).
        - Layer 3: Declared Android OS Category (`ApplicationInfo.category`): blocks `CATEGORY_GAME`, `CATEGORY_SOCIAL` (unless messaging/conferencing), `CATEGORY_VIDEO` (unless conferencing), `CATEGORY_NEWS`; allows `CATEGORY_ACCESSIBILITY`, `MAPS`, `AUDIO`, `IMAGE`, `PRODUCTIVITY` (unless remote desktop).
        - Layer 4b: Positive Academic Promotion (`hasPositiveAcademicSignals`: promotes uncategorized apps with educational keywords to ALLOW with green `Auto` badge).
        - Layer 5: Conservative Fallback (blocks unknown third-party apps).
      - *Portal Super-App Gating*: Permitted Baidu (`com.baidu.searchbox`) and NAVER (`com.nhn.android.search`) for academic search while vetoing internal video reels, feed flows, and web novels (`isDistractingSubActivity`, `isPortalSuperAppVideoViewId`); disallowed Phoenix Browser and UC Browser unconditionally (`isDisallowedPortalSuperApp`).
      - *Direct Messaging vs Social Feeds*: Permitted direct communication apps (`isMessagingApp`: Telegram, Messenger, WhatsApp, Signal) and online lecture platforms (`isMeetingOrVideoConferenceApp`: Meet, Zoom, Teams, Webex) while strictly isolating social algorithmic feeds (Facebook, Instagram, TikTok, Reddit).
    - **Safe Backup & Purge of Obsolete Java Source**:
      - Master backup copy preserved in `scratch/backup_phase3b/AppClassifier.java`.
      - Safely removed legacy Java file `AppClassifier.java` from active sources.
  - **Comprehensive Verification Plan & Matrix (User Rule 3)**:
    - *Affected Files*:
      - [`android/app/src/main/java/com/uncode/app/AppClassifier.kt`](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/android/app/src/main/java/com/uncode/app/AppClassifier.kt)
      - [`scratch/backup_phase3b/AppClassifier.java`](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/scratch/backup_phase3b/AppClassifier.java) (backup preserved)
      - [`README.md`](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/README.md)
    - *Known Dependents & Callers*:
      - `LockAccessibilityService.java`: Direct Caller (Foreground Window Interceptor) querying 14 methods including `isStage1Vetoed`, `isPackageBlocked`, `isBrowserPackage`, `isSettingsOrDeviceManager`, `isStage1Bloat`, `isPortalSuperApp`, `isDistractingSubActivity`, `isPortalSuperAppVideoViewId`, `isSimOrCarrierService`, and `refreshBrowserCache`.
      - `InstalledLauncherDetector.kt`: Direct Caller querying `AppClassifier.isStage1Vetoed(pkg, null)`.
      - `LauncherStateManager.kt`: Direct Caller querying `AppClassifier.isStage1Vetoed(pkg, null)`.
      - `KnownSafe.kt`: Direct Caller querying `AppClassifier.isBrowserPackage(pkg)` and `AppClassifier.isStage1Vetoed(pkg, null)`.
      - `BlacklistConstants.kt`: Direct Caller querying `AppClassifier.isSettingsOrDeviceManager` and `AppClassifier.isStage1Bloat`.
      - `LockPlugin.kt`: Direct Caller invalidating decision caches via `AppClassifier.clearCache()` upon whitelist changes.
      - `HomeHandlerTest.kt`: Unit test suite testing Portal Super-App video view IDs, Baidu/Naver components, and launcher vetting.
      - `WebTruthTableTest.kt`: Unit test suite executing 80+ assertions testing super-apps, browser detection, distracting sub-activities, shopping app blocks, and Stage 1 vetoes.
      - `UnifiedPolicyTest.kt`: Unit test suite verifying Truth Table overrides and active unified service propagation.
    - *Step-by-Step On-Device Verification Instructions (Human-Facing Sensory Protocol - No ADB)*:
      1. **Stage 1 Master Veto Gate (Anti-Tamper & Settings Eviction)**:
         - *Action*: Start an active focus lockdown session in QIEZKA (Dashboard -> Start Lock). Attempt to open Android **Settings** (`com.android.settings`), App Info for QIEZKA, or OEM Security Center / Device Care.
         - *Expected Result*: Settings is evicted immediately within $\le 200\text{ms}$. QIEZKA Lock Screen re-takes the foreground cleanly without oscillations. Access to force-stop or permissions revocation is completely blocked.
      2. **Stage 2 Dual Gate with Unified Policy (YouTube & AI Assistants)**:
         - *Action*: In QIEZKA Settings, toggle **Videos (YouTube)** to **ON**. Start lockdown. From the home launcher, launch the native **YouTube** app.
         - *Expected Result*: YouTube opens smoothly and remains in the foreground (`YES Distracting, YES Safe -> ALLOW`). The floating countdown bubble remains visible and responsive.
         - *Action (Toggle OFF)*: In Settings, toggle **Videos (YouTube)** to **OFF**. Re-launch YouTube.
         - *Expected Result*: YouTube is immediately evicted back to the lock screen with a warning toast (`YES Distracting, NO Safe -> BLOCK`).
      3. **Consumer Distraction Eviction (Games, Social Feeds & E-Commerce)**:
         - *Action*: While in active lockdown, attempt to launch **TikTok**, **Instagram**, **Roblox**, or an online shopping app (**Shopee**, **Lazada**, **Amazon**).
         - *Expected Result*: Each app is instantly intercepted and evicted to QIEZKA Lock Screen within 200ms. Apps claiming productivity categories but containing shopping/gaming keywords are reliably blocked by Layer 4a heuristics.
      4. **Web Browser & Portal Super-App Gating**:
         - *Action*: While in lockdown, open **Chrome** or **Samsung Internet** -> Browser opens and functions normally (URL/DOM audited by `WebClassifier`).
         - *Action*: Open **Baidu** (`com.baidu.searchbox`) or **NAVER** (`com.nhn.android.search`) -> General search functions normally; tapping the "Video" / "Shorts" / "Reels" tab triggers immediate eviction or auto-collapse.
         - *Action*: Attempt to launch **Phoenix Browser** or **UC Browser** -> Instantly blocked at the package level (`isDisallowedPortalSuperApp`).
      5. **Academic App Promotion & Secret Calculator Vaults**:
         - *Action*: Launch an unlisted offline scientific calculator or textbook reader (e.g. Desmos, unlisted school portal) -> App is promoted and allowed with the green `Auto` badge.
         - *Action*: Launch a known "Calculator Vault / Secret Vault" -> Detected and blocked immediately by `isFakeCalculatorVault`.
      6. **Dynamic Whitelist Cache Synchronization**:
         - *Action*: Add an unlisted study app to "Allowed Apps" on the Dashboard during lockdown.
         - *Expected Result*: `decisionCache` is cleared. Launching the newly whitelisted app opens immediately without being blocked.
      7. **Automated Gradle & TypeScript Validation**:
         - *Action*: Run `.\android\gradlew.bat -p android compileDebugKotlin compileDebugJavaWithJavac testDebugUnitTest` and `npx tsc --noEmit`.
         - *Expected Result*: Kotlin and Java compilation succeed (`BUILD SUCCESSFUL in 1m 59s`), all unit test suites pass with 0 failures, and TypeScript compiler reports 0 errors.
      8. **User Rule 2 Compliance Check**:
         - *Action*: Confirm no APK build commands (`assembleDebug`, `assembleRelease`, `build.bat`) were executed.
         - *Expected Result*: Verified; APK compilation left entirely to user.
""" + target

if target in content:
    new_content = content.replace(target, phase3b_entry, 1)
    with open(readme_path, "w", encoding="utf-8") as f:
        f.write(new_content)
    print("SUCCESS: Appended Phase 3B entry to README.md")
else:
    print("ERROR: Target anchor not found in README.md")
