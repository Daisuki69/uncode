readme_path = r'c:\Users\CxAdmin\Desktop\qiezka\uncode\README.md'

with open(readme_path, 'r', encoding='utf-8') as f:
    content = f.read()

target_str = "## 🔮 Future Roadmap & Ecosystem Forks"
idx = content.find(target_str)
if idx == -1:
    print("target_str not found")
else:
    # Find the '---' right before this header
    dashes_idx = content.rfind("---", 0, idx)
    print(f"Found at {idx}, dashes at {dashes_idx}")
    before = content[:dashes_idx]
    after = content[dashes_idx:]

    phase6_block = """- **Phase 6 Kotlin Migration: Core Sensory Enforcement & Live Remediation Engine (LockAccessibilityService) (Patch 28 Follow-Up)**:
  - **Why It Was Mandated (The Final Piece of 100% Native Kotlin Migration)**:
    - *Modernizing the Core Sensory & Real-Time Accessibility Engine*:
      - Prior phases successfully migrated leaf utilities & state managers (Phase 1), receivers & content provider (Phase 2), classification intelligences (Phase 3A/3B), auxiliary background services (Phase 4), and native bridge/activity roots (Phase 5).
      - [LockAccessibilityService.java](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/android/app/src/main/java/com/uncode/app/LockAccessibilityService.java) (3,269 lines) stood as the final remaining legacy Java source in the entire Android project. Migrating it to pure idiomatic Kotlin marks the completion of the 100% Kotlin Android codebase modernization.
      - As the sensory nervous system of QIEZKA, `LockAccessibilityService` runs continuously as an active Android `AccessibilityService` bound to window events (`TYPE_WINDOW_STATE_CHANGED`, `TYPE_VIEW_CLICKED`, `TYPE_WINDOW_CONTENT_CHANGED`), orchestrating:
        1. *Stage 1 Master Veto Gate*: Intercepts illicit foreground packages within milliseconds during active lockdown and consequence sessions.
        2. *Stage 2 Web Classifier & Progressive DOM Scraper*: Asynchronously inspects browser address bars, detects link context menus, and evaluates full URLs against `WebClassifier` using progressive debounce intervals.
        3. *Stage 3 Launcher Protection*: Safeguards default launcher status, prevents tampering with Android home settings, and integrates with SIREN HomeProxy.
        4. *Multi-Tier Smart Tab Remediation*: Enforces 3-tier remediation (Auto-Back debounce -> Browser Home button click -> Reusable safe tab intent) to neutralize illicit web navigation without creating tab clutter.
      - Migrating this monumental service to Kotlin enforces total null safety across volatile accessibility node hierarchies (`AccessibilityNodeInfo?`), eliminates runtime NPE crashes during rapid window transitions, safely bounds concurrency via `HandlerThread` worker offloading, and provides seamless `@JvmStatic` and `@JvmField` binary compatibility for callers and unit test suites.
  - **Concrete Architectural Implementations**:
    - **`LockAccessibilityService.kt` (`android/app/src/main/java/com/uncode/app/LockAccessibilityService.kt`)**:
      - Converted monolithic service into pure idiomatic Kotlin class `class LockAccessibilityService : AccessibilityService()`.
      - **Thread Architecture & Concurrency**:
        - Retains dedicated `auditThread = HandlerThread("Qiezka-AuditWorker")` and worker `auditHandler` to offload heavy accessibility node tree traversals, URL parsing, and package manager queries entirely from the Android UI thread.
        - Preserves Main Looper 1000ms `tickerHandler` + `tickerRunnable` powering countdown clocks, advance audio/haptic warnings, and `LocalDnsVpnService` session synchronization.
        - Progressive DOM polling delays `{ 300L, 700L, 1200L, 2000L }` with token cancellation (`auditStep0Token`) ensuring responsive, battery-friendly URL extraction.
      - **Complete Companion Object Public API Surface**:
        - Exposes `@JvmField var instance: LockAccessibilityService? = null`.
        - Exposes `@JvmField var dynamicLauncherPackages: MutableSet<String>`.
        - Exposes `@JvmField var dynamicLauncherLabels: MutableSet<String>`.
        - Preserves 18 exact `@JvmStatic` companion endpoints ensuring 100% binary interoperability with all background services and unit test suites:
          1. `getInstance(): LockAccessibilityService?`
          2. `isInOperatingHours(context: Context): Boolean`
          3. `isBrowserPackage(pkg: String?): Boolean`
          4. `isValidUrlCandidate(text: String?): Boolean`
          5. `isLinkContextMenu(root: AccessibilityNodeInfo?): Boolean`
          6. `isInstalledLauncherCandidate(packageName: String): Boolean`
          7. `isHomeAppSelectionTitle(title: String?): Boolean`
          8. `isHomeChangeInterceptorActive(): Boolean`
          9. `setCachedIsSirenDefaultForTesting(value: Boolean)`
          10. `isSimOrCarrierService(pkg: String?): Boolean`
          11. `hasActiveSchedules(): Boolean`
          12. `isAddressBarNode(node: AccessibilityNodeInfo?): Boolean`
          13. `isBrowserNewTabPage(urlOrTitle: String?): Boolean`
          14. `checkDynamicLauncherStatus(pkg: String?): Boolean`
          15. `clearWhitelistedAppCaches()`
          16. `checkAndFireTickerWarning(context: Context, remainingSeconds: Long, totalSeconds: Long, prefKeyPrefix: String)`
          17. `checkAndFireLockdownTimerWarning(context: Context, remainingSeconds: Long, totalSeconds: Long)`
          18. `checkAndFireConsequenceTimerWarning(context: Context, remainingSeconds: Long, totalSeconds: Long)`
      - **Multi-Tier Smart Tab Remediation Protocol**:
        - *Tier 1 (Hits 1-2)*: Executes in-place `performGlobalAction(GLOBAL_ACTION_BACK)` with 2200ms debounce to cleanly bounce away from illicit web pages.
        - *Tier 2 (Hit 3)*: Dispatches `clickBrowserHomeButton(root)` to navigate the existing browser tab back to its default start page in-place, preventing tab accumulation.
        - *Tier 3 (Fallback)*: Reusable single safe tab Intent with `Browser.EXTRA_APPLICATION_ID = packageName`.
      - **Null Safety & Node Lifecycle**:
        - Safe-calls and null checks across all accessibility node operations preventing lifecycle leaks and recycling errors across Android versions.
      - **Caller Safe-Call Optimization**:
        - Updated [AlarmReceiver.kt](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/android/app/src/main/java/com/uncode/app/AlarmReceiver.kt) with safe calls `LockAccessibilityService.instance?.onScheduleStartTriggered()`.
    - **Safe Backup & Deprecation Purge**:
      - Master backup preserved in `scratch/backup_phase6/LockAccessibilityService.java` and `LockAccessibilityService.java.bak`.
      - Removed legacy Java file `android/app/src/main/java/com/uncode/app/LockAccessibilityService.java` from active sources.
  - **Comprehensive Verification Plan & Matrix (User Rule 3)**:
    - *Affected Files*:
      - [`android/app/src/main/java/com/uncode/app/LockAccessibilityService.kt`](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/android/app/src/main/java/com/uncode/app/LockAccessibilityService.kt)
      - [`android/app/src/main/java/com/uncode/app/AlarmReceiver.kt`](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/android/app/src/main/java/com/uncode/app/AlarmReceiver.kt)
      - [`README.md`](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/README.md)
    - *Known Dependents & Callers*:
      - `AndroidManifest.xml`: Declares `LockAccessibilityService` with `android.permission.BIND_ACCESSIBILITY_SERVICE`.
      - `res/xml/accessibility_config.xml`: Configures service capabilities (`canRetrieveWindowContent`, `accessibilityFeedbackType`, flags).
      - `AlarmReceiver.kt`: Invokes `LockAccessibilityService.instance?.onScheduleStartTriggered()`.
      - `EnforcementCoreService.kt`: Interacts with accessibility state and lockdown enforcement.
      - `LockPlugin.kt`: Checks accessibility service enabled state; interacts with launcher caches and operating modes.
      - `LocalDnsVpnService.kt`: Synchronizes active session status with accessibility ticker loop.
      - `ScheduleManager.kt`: Interacts with active schedules checked by `hasActiveSchedules()`.
      - `HomeHandlerTest.kt`: Unit test suite testing launcher detection and home change interception logic.
      - `WebTruthTableTest.kt`: Validates URL validation and candidate filtering patterns.
      - `UnifiedPolicyTest.kt`: Validates policy interactions and browser classification.
    - *Step-by-Step On-Device Verification Instructions (Human-Facing Sensory Protocol - No ADB)*:
      1. **Active Lockdown Stage 1 Master Veto Gate**:
         - *Action*: Start a lock session from the Dashboard. Attempt to launch an un-whitelisted application (e.g. Android System Settings, YouTube, Social Media).
         - *Expected Result*: The accessibility service immediately detects the illicit window package, executes `GLOBAL_ACTION_HOME`, and redirects back to the Lock Screen / allowed app within milliseconds.
      2. **Browser Real-Time DOM URL Inspection & Tier Remediation**:
         - *Action*: During active lockdown with a browser whitelisted (e.g. Chrome), navigate to a prohibited domain (e.g. `tiktok.com` or adult/gaming site).
         - *Expected Result*: The asynchronous DOM scraper detects the URL in the address bar. Hits 1-2 trigger automatic Back navigation returning to the previous search or safe page. Persistent hits trigger the browser Home button redirecting to the blank start tab.
      3. **Ticker Countdown Loop & Synchronization**:
         - *Action*: Observe the lock session countdown timer on the fullscreen lock interface and on the floating countdown ball overlay.
         - *Expected Result*: The ticker decrements reliably every second with 0 lag or stutter. When the timer hits 00:00, the session cleanly transitions to finished state.
      4. **Advance Audio/Haptic Warnings**:
         - *Action*: Let a scheduled lock or countdown run through a milestone boundary (e.g., 5m, 1m).
         - *Expected Result*: Haptic vibration pulse and audible notification alert fire cleanly without crashing the background service.
      5. **Stage 3 Launcher Protection & SIREN Invariant**:
         - *Action*: Attempt to access Android Default Home App settings or change the default launcher during active lockdown.
         - *Expected Result*: The service intercepts the Settings screen title (`isHomeAppSelectionTitle`), immediately dispatches Back/Home, preventing bypassing of the lock via third-party launcher installation.
      6. **Consequence Mode Enforcement**:
         - *Action*: Trigger or enter Consequence Mode after a missed homework deadline.
         - *Expected Result*: The master veto gate remains active, restricting all non-essential and distracting apps until the consequence task is satisfied.
      7. **Automated Gradle & TypeScript Validation**:
         - *Action*: Run `.\\android\\gradlew.bat -p android compileDebugKotlin compileDebugJavaWithJavac testDebugUnitTest` and `npx tsc --noEmit`.
         - *Expected Result*: Pure Kotlin and Java compilation succeed (`BUILD SUCCESSFUL`), all unit test suites pass (`BUILD SUCCESSFUL`), and TypeScript compiler reports 0 errors.
      8. **User Rule 2 Compliance Check**:
         - *Action*: Confirm no APK build commands (`assembleDebug`, `assembleRelease`, `build.bat`) were executed.
         - *Expected Result*: Verified; APK compilation left entirely to user.

"""
    new_content = before + phase6_block + after
    with open(readme_path, 'w', encoding='utf-8') as out_f:
        out_f.write(new_content)
    print("Successfully updated README.md with Phase 6")
