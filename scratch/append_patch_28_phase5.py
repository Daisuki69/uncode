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

    phase5_block = """- **Phase 5 Kotlin Migration: Native Bridge Layer (MainActivity, LockPlugin) (Patch 28 Follow-Up)**:
  - **Why It Was Mandated (Native Activity Root & Capacitor Two-Way IPC Modernization)**:
    - *Transitioning the Core Application Gateway & Two-Way IPC Engine*:
      - Following Phase 1 (leaf managers), Phase 2 (receivers & content provider), and Phase 4 (auxiliary background & network services), Phase 5 migrated the central native coordination engine: [MainActivity.java](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/android/app/src/main/java/com/uncode/app/MainActivity.java) and [LockPlugin.java](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/android/app/src/main/java/com/uncode/app/LockPlugin.java).
      - `MainActivity` acts as the root entry point extending Capacitor's `BridgeActivity`, controlling application lifecycle state, hardware back-press dispatching, and portrait orientation locking.
      - `LockPlugin` serves as the primary two-way IPC bridge with 36 distinct `@PluginMethod` endpoints connecting the React/TypeScript frontend (`systemBridge.ts`) to all native Android subsystems (session lifecycle, consequence states, web protection, unified service policies, app inventory extraction, system settings deep links, SAF document streaming, and SIREN launcher discovery).
      - Migrating these components to idiomatic Kotlin enforces null safety, coroutine-friendly architecture, and clean `@JvmStatic` companion interfaces ensuring 100% binary and source compatibility across remaining Java services (`LockAccessibilityService.java`) while preserving the React/TypeScript frontend completely intact.
  - **Concrete Architectural Implementations**:
    - **`MainActivity.kt` (`android/app/src/main/java/com/uncode/app/MainActivity.kt`)**:
      - Converted root activity to Kotlin class `class MainActivity : BridgeActivity()`.
      - Exposes `@JvmStatic fun isRunning(): Boolean` and `@JvmStatic fun getInstance(): MainActivity?` via `companion object`.
      - Enforces portrait orientation lock: `requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT`.
      - Registers native plugin via `registerPlugin(LockPlugin::class.java)`.
      - Wires hardware `onBackPressedDispatcher`: intercepts Back button presses, queries `LockPlugin.hasBackListeners()`, delegates to `LockPlugin.triggerBackPressed()` if active, and falls back to `moveTaskToBack(true)`.
      - Maintains thread-safe lifecycle tracking (`sIsRunning` and `sInstance`) across `onCreate()`, `onResume()`, and `onDestroy()`.
    - **`LockPlugin.kt` (`android/app/src/main/java/com/uncode/app/LockPlugin.kt`)**:
      - Converted core IPC bridge to Kotlin class `class LockPlugin : Plugin()`, annotated with `@CapacitorPlugin(name = "LockPlugin")`.
      - Fully ported all 36 exact `@PluginMethod` endpoints:
        1. *Session Lifecycle*: `startLockdown(call)` (with silent IME/keyboard whitelisting, 90-min clamp, SharedPreferences synchronization, exact alarm scheduling, and overlay dispatch), `endLockdown(call)`, `getLockStatus(call)`.
        2. *Consequence Enforcement*: `setConsequenceActive(call)` (initiates full consequence enforcement, starts floating indicator, posts status notification).
        3. *Operating & Web Protection Governance*: `setOperatingMode(call)` (`safemode` vs `hardcore`), `setWebProtectionMode(call)` (`accessibility` vs `dual_hybrid`), `setAllowYoutube(call)`, `setServicePolicy(call)`, `getActiveServices(call)`, `getRegisteredServices(call)`, `setBlockWebGames(call)`, `setDnsFilterProfile(call)`, `setEnforceSafeSearch(call)`, `requestVpnPermission(call)` (with `@ActivityCallback private fun vpnPermissionCallback(...)`).
        4. *Schedules & Clocks*: `syncTimeOffset(call)`, `syncSchedules(call)`.
        5. *App Inventory & Security Sanitization*: `getInstalledApps(call)` (background query of launcher, browser, music, and camera apps with bloatware/anti-tamper filtering, Stage 3 system gateway bypass, and 96x96 Base64 PNG icon generation), `sanitizeImportApps(call)`.
        6. *Permissions & Settings Deep Links*: `override fun checkPermissions(call)` (evaluates Device Admin, Accessibility, Battery Optimization, Notifications, and Exact Alarm), `requestBatteryOptimization(call)`, `requestNotificationPermission(call)` (with `@PermissionCallback private fun notificationPermCallback(...)`), `openNotificationSettings(call)`, `openAccessibilitySettings(call)`, `openAppInfo(call)`, `openDeviceAdminSettings(call)` (with `@ActivityCallback private fun deviceAdminResult(...)`), `openDeviceAdminList(call)`.
        7. *Data Management (SAF Stream)*: `exportBackup(call)` (with `@ActivityCallback private fun exportBackupResult(...)` streaming JSON from cache to SAF URI), `importBackup(call)` (with `@ActivityCallback private fun importBackupResult(...)` reading UTF-8 JSON from SAF URI).
        8. *Launcher & SIREN Bridge*: `getInstalledLaunchers(call)`, `setSelectedLauncher(call)`, `clearSelectedLauncher(call)`, `getSelectedLauncher(call)`, `getSirenStatus(call)`, `requestSirenHomeRole(call)`.
        9. *System UI*: `exitToHome(call)` (executes `moveTaskToBack(true)` on UI thread), `showToast(call)`.
      - Exposes `@JvmStatic fun clearLockdownState(context: Context?)` and `@JvmStatic fun startConsequenceState(context: Context?, scheduleId: String?, whitelist: Set<String>?)` in `companion object`.
      - Preserves hardware Back listeners bridge: `fun hasBackListeners(): Boolean = hasListeners("backPressed")` and `fun triggerBackPressed() = notifyListeners("backPressed", JSObject())`.
    - **Safe Backup & Purge of Obsolete Java Sources**:
      - Master backup copies preserved in `scratch/backup_phase5/MainActivity.java` and `scratch/backup_phase5/LockPlugin.java`.
      - Safely removed legacy Java files `MainActivity.java` and `LockPlugin.java` from active sources.
  - **Comprehensive Verification Plan & Matrix (User Rule 3)**:
    - *Affected Files*:
      - [`android/app/src/main/java/com/uncode/app/MainActivity.kt`](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/android/app/src/main/java/com/uncode/app/MainActivity.kt)
      - [`android/app/src/main/java/com/uncode/app/LockPlugin.kt`](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/android/app/src/main/java/com/uncode/app/LockPlugin.kt)
      - [`README.md`](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/README.md)
    - *Known Dependents & Callers*:
      - `AndroidManifest.xml`: Declares `MainActivity` as the primary launcher activity.
      - `src/systemBridge.ts`: Primary TypeScript IPC client invoking all 36 `@PluginMethod` bindings.
      - `src/App.tsx`: Hardware back navigation, session state lifecycle, schedule sync, consequence mode, exitToHome.
      - `src/components/Dashboard.tsx`: Allowed apps modal via `getInstalledApps()`.
      - `src/components/SettingsOverlay.tsx`: Launcher selection, SIREN bridge, backup export/import, unified service policies, mode toggles.
      - `src/components/PermissionWalkthrough.tsx` & `src/components/Onboarding.tsx`: System permission status checks and deep link setting navigation.
      - `FloatingOverlayService.kt`: Tapping floating countdown ball launches `MainActivity`; started and stopped by `LockPlugin`.
      - `AlarmReceiver.kt`: Fires alarms and launches `MainActivity`; managed by `LockPlugin`.
      - `LocalDnsVpnService.kt`: Started and stopped by `LockPlugin`.
      - `ScheduleManager.kt`: Rescheduled and synchronized by `LockPlugin`.
      - `HomeHandlerTest.kt`: Unit test suite testing `MainActivity.isRunning()` and `MainActivity.getInstance()`.
    - *Step-by-Step On-Device Verification Instructions (Human-Facing Sensory Protocol - No ADB)*:
      1. **Authentic Hardware Back-Button Handling Across Screens**:
         - *Sub-Screen Navigation*: Tap Settings (or Logs, or Create Schedule) -> Press the Android Back button once -> The screen smoothly navigates backward returning to the Dashboard.
         - *Dashboard Double-Back Exit*: On the Dashboard, press the Android Back button once -> A toast notification appears at the bottom: *"Press back again to exit"*. Press Back a second time within 2 seconds -> QIEZKA cleanly minimizes to the Android launcher (`moveTaskToBack(true)`).
         - *Lockdown Minimization for Allowed Apps*: Start a lockdown session (Dashboard -> Start Lock) -> Press the Back button -> QIEZKA cleanly minimizes to the home screen so you can use your whitelisted apps while the Floating Countdown Ball remains visible and active on screen. Tap the floating ball: QIEZKA opens back up to the Lock Screen immediately.
      2. **Installed Apps Discovery & Whitelist Picker**:
         - Tap "Allowed Apps" on Dashboard (or in Create Schedule) -> A modal dialog appears displaying user-launchable apps installed on the device with their authentic native Base64 PNG icons and readable labels. Distracting apps (games, social media), system settings, device managers, and keyboards are filtered out and do not appear in the selectable whitelist.
      3. **Live Permissions & Deep Linking**:
         - Open Settings -> Permissions (or Onboarding / Permission Walkthrough) -> Status indicators (green checkmarks / red warnings) accurately reflect current system states for Accessibility, Device Admin, Battery Optimization, and Notifications. Tapping each setting opens the appropriate system settings screen.
      4. **Full Lockdown Session Lifecycle (Start, Float Ball, End)**:
         - Start a test lock session -> Fullscreen Lock Screen engages with the large countdown timer ticking down, floating timer ball overlay appears on screen edge, and persistent status notification appears in the notification shade. On emergency release or 00:00 timer, the floating countdown ball disappears, notifications clear, and app returns to Dashboard.
      5. **Consequence Mode State Engagement**:
         - Engage a consequence state -> Persistent Android notification appears: *"⚠️ QIEZKA Consequence Mode - Distracting apps are restricted..."*, and floating overlay appears indicating Consequence Mode. Completing or rescheduling the task dismisses both.
      6. **Mode & Policy Toggles Persistence**:
         - In Settings, toggle Operating Mode between Safe Mode and Hardcore Mode; toggle Web Protection Mode (prompts system VPN permission on Dual Hybrid); toggle Unified Services (YouTube, AI). Close and re-open Settings to confirm state persistence.
      7. **Launcher Bridge & SIREN Status**:
         - In Settings -> Launcher Configuration -> See installed launchers listed (e.g. System Launcher, KISS, Nova); select a launcher to mark it as active. SIREN HomeProxy section renders showing installation status and default home status. Tapping "Set as Default Home" triggers the system default home chooser.
      8. **Data Management SAF Export & Import**:
         - In Settings -> Data Management, tap Export Backup -> System document picker saves `backup.json` and displays success toast. Tap Import Backup -> System document picker loads file, sanitizes packages, and restores settings.
      9. **Automated Gradle & TypeScript Validation**:
         - *Action*: Run `.\\android\\gradlew.bat -p android compileDebugKotlin compileDebugJavaWithJavac testDebugUnitTest` and `npx tsc --noEmit`.
         - *Expected Result*: Kotlin and Java compilation succeed (`BUILD SUCCESSFUL in 32s`), unit tests pass (`BUILD SUCCESSFUL`), and TypeScript compiler reports 0 errors.
      10. **User Rule 2 Compliance Check**:
          - *Action*: Confirm no APK build commands (`assembleDebug`, `assembleRelease`, `build.bat`) were executed.
          - *Expected Result*: Verified; APK compilation left entirely to user.

"""
    new_content = before + phase5_block + after
    with open(readme_path, 'w', encoding='utf-8') as out_f:
        out_f.write(new_content)
    print("Successfully updated README.md")
