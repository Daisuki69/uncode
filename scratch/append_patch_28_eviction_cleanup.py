# Append Native App Eviction Clean-Up entry below previous follow-up entry in README.md
import sys

readme_path = "README.md"
with open(readme_path, "r", encoding="utf-8") as f:
    content = f.read()

target = """      9. **User Rule 2 Compliance Check**:
         - *Action*: Confirm no APK build commands (`assembleDebug`, `assembleRelease`, `build.bat`) were executed.
         - *Expected Result*: Verified; APK compilation left entirely to user.

---"""

entry = r"""      9. **User Rule 2 Compliance Check**:
         - *Action*: Confirm no APK build commands (`assembleDebug`, `assembleRelease`, `build.bat`) were executed.
         - *Expected Result*: Verified; APK compilation left entirely to user.

- **Native App Eviction Clean-Up: Eliminating Conflicting Back Presses in enforceBlock (Patch 28 Follow-Up)**:
  - **Why It Was Mandated (Resolving the Self-Destructive App Thrashing Loop)**:
    - *Forensic Diagnosis of the Fighting Loop*:
      - During focus lockdown, when a student attempted to open a blocked native application (e.g. mobile games, TikTok, or unapproved apps), the device entered an intense, flickering oscillation loop between the QIEZKA Lock Screen and the home launcher.
      - Investigation traced this to a fundamental collision between synthetic Back actions in `enforceBlock()` and React's hardware back handling:
        1. When a distracting app opened, `enforceBlock()` in [LockAccessibilityService.java](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/android/app/src/main/java/com/uncode/app/LockAccessibilityService.java) called `launchLockOverlay()` (to summon QIEZKA Lock Screen) while simultaneously firing `performGlobalAction(GLOBAL_ACTION_BACK)` at 0ms, 100ms, and 250ms.
        2. In [MainActivity.kt](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/android/app/src/main/java/com/uncode/app/MainActivity.kt), `onBackPressedDispatcher` caught this synthetic Back event and passed it to React via `LockPlugin.triggerBackPressed()`.
        3. In [App.tsx](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/src/App.tsx) line 178, the hardware back handler specifies: `if (appState === 'locked') { exitToHome(); return; }`. This was designed so students could intentionally press Back to access allowed study apps while the Floating Overlay Ball remains pinned.
        4. However, because `enforceBlock()` bombarded the system with synthetic Back presses upon summoning the overlay, React immediately executed `exitToHome()`, commanding the Android OS to minimize QIEZKA (`moveTaskToBack(true)`).
        5. The delayed ticker checks in `enforceBlock()` (at 100ms and 250ms) saw that QIEZKA was no longer in the foreground, firing `launchLockOverlay()` and `GLOBAL_ACTION_BACK` again—causing QIEZKA to repeatedly launch itself and minimize itself in a continuous fight.
    - *Strict Flowchart Realignment (`README.md:2104-2114`)*:
      - **Native Apps**: Flowchart mandates `BLOCK["🚫 BLOCK AND EVICT TO QIEZKA (not route EVICT TO HOMESCREEN)"]`. Native app blocks must strictly summon the QIEZKA Lock overlay and **never** send synthetic Back key events.
      - **Websites**: Flowchart mandates `BLOCKS["Auto Back Until Safe Site, If theres nothing to back on then go to HomePage"]`. Distracting websites inside browsers stay inside the browser and execute in-browser Auto-Back (`remediateBlockedBrowserTab`), **never** summoning QIEZKA.
  - **Concrete Architectural Implementations**:
    - **`LockAccessibilityService.java` (`android/app/src/main/java/com/uncode/app/LockAccessibilityService.java`)**:
      - In `enforceBlock(String pkg)`: Completely purged all three calls to `performGlobalAction(GLOBAL_ACTION_BACK)` (immediate call at line 2508, 100ms burst at line 2518, and 250ms burst at line 2528). Native app blocking now exclusively and cleanly executes `launchLockOverlay()`.
      - In `evictHomeLauncherChange(AccessibilityNodeInfo dialogRoot)`: Retained dialog cancellation via `clickCancelOnNode` and purged delayed `performGlobalAction(GLOBAL_ACTION_BACK)` bursts at 25ms, 60ms, and 120ms that were bleeding into `launchLockOverlay()`.
      - Preserved `remediateBlockedBrowserTab()`: In-browser Auto-Back for websites remains 100% intact, maintaining complete separation of concerns between web remediation and native app eviction.
    - **Safe Backup**:
      - Master backup preserved in `scratch/backup_eviction_cleanup/LockAccessibilityService.java`.
  - **Comprehensive Verification Plan & Matrix (User Rule 3)**:
    - *Affected Files*:
      - [`android/app/src/main/java/com/uncode/app/LockAccessibilityService.java`](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/android/app/src/main/java/com/uncode/app/LockAccessibilityService.java)
      - [`scratch/backup_eviction_cleanup/LockAccessibilityService.java`](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/scratch/backup_eviction_cleanup/LockAccessibilityService.java) (backup preserved)
      - [`README.md`](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/README.md)
    - *Known Dependents & Callers*:
      - `LockAccessibilityService.java`: Evaluates all foreground package transitions (`onAccessibilityEvent`, `TYPE_WINDOWS_CHANGED`, background ticker) against `isStage1Vetoed` and `isPackageBlocked`. All blocked native apps cleanly route to `launchLockOverlay()`.
      - `MainActivity.kt`: Target activity receiving `launchLockOverlay()` intents. Will no longer receive unintended `onBackPressed()` key events triggered by its own background service.
      - `App.tsx`: Hardware back listener on the `'locked'` screen will now only fire when the student physically presses the hardware Back button/gesture to minimize QIEZKA for whitelisted study apps.
      - `FloatingOverlayService.kt`: Overlay countdown ball remains completely stable with zero jitter from task-switching loops.
    - *Step-by-Step On-Device Verification Instructions (Human-Facing Sensory Protocol - No ADB)*:
      1. **Native App Block Stability (Eliminating Self-Fighting Loop)**:
         - *Action*: Start an active focus lockdown session. From the home screen or app drawer, attempt to launch a known distracting native app (e.g. TikTok, Instagram, Roblox, or YouTube when disallowed).
         - *Expected Result*: The fullscreen QIEZKA Lock Screen instantly takes the foreground. The screen **remains solidly on the QIEZKA Lock Screen** without flickering, flashing, stuttering, or bouncing back to the home launcher.
      2. **Intentional Hardware Back Press for Allowed Apps**:
         - *Action*: While on the QIEZKA Lock Screen during an active lockdown session, physically tap the device Back button or perform the Back edge gesture.
         - *Expected Result*: QIEZKA smoothly minimizes to the home screen. The Floating Countdown Ball remains visible and responsive at the edge of the screen. Tapping the floating ball returns immediately to the Lock Screen.
      3. **Website Auto-Back Isolation (Browser Retains In-Place Remediation)**:
         - *Action*: While in active lockdown, open Chrome or Samsung Internet and visit a distracting website (e.g. `https://poki.com` or `https://y8.com`).
         - *Expected Result*: A brief toast appears (*"⚠️ Distracting website. Returning to safe page."*). The browser automatically executes Back navigation, returning to the previous safe page or New Tab Page. **QIEZKA DOES NOT summon its overlay over the browser.**
      4. **Stage 1 Master Veto Gate (Anti-Tamper Settings Eviction)**:
         - *Action*: While in active lockdown, attempt to open Android **Settings** (`com.android.settings`) or App Info.
         - *Expected Result*: Settings is instantly evicted, and QIEZKA Lock Screen takes the foreground cleanly without rapid oscillations.
      5. **Automated Gradle & TypeScript Validation**:
         - *Action*: Run `.\android\gradlew.bat -p android compileDebugKotlin compileDebugJavaWithJavac testDebugUnitTest` and `npx tsc --noEmit`.
         - *Expected Result*: Kotlin and Java compilation succeed (`BUILD SUCCESSFUL`), all 14 unit test suites pass with 0 failures, and TypeScript compiler reports 0 errors.
      6. **User Rule 2 Compliance Check**:
         - *Action*: Confirm no APK build commands (`assembleDebug`, `assembleRelease`, `build.bat`) were executed.
         - *Expected Result*: Verified; APK compilation left entirely to user.

---"""

if target in content:
    new_content = content.replace(target, entry, 1)
    with open(readme_path, "w", encoding="utf-8") as f:
        f.write(new_content)
    print("SUCCESS: Appended eviction clean-up entry to README.md")
else:
    print("ERROR: Target string not found in README.md")
    sys.exit(1)
