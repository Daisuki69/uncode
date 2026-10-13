import sys

readme_path = r"c:\Users\CxAdmin\Desktop\qiezka\uncode\README.md"

with open(readme_path, "rb") as f:
    raw = f.read()

text = raw.decode("utf-8")

anchor = "5. **Automated Verification**:\n         - *Action*: Run `.\\android\\gradlew.bat -p android compileDebugKotlin compileDebugJavaWithJavac testDebugUnitTest` and `npx tsc --noEmit`.\n         - *Expected Result*: Kotlin and Java compilation succeed (`BUILD SUCCESSFUL in 56s`), unit tests pass (`BUILD SUCCESSFUL in 32s`, 0 failures), and TypeScript typecheck reports 0 errors.\n      6. **User Rule 2 Compliance Check**:\n         - *Action*: Confirm no APK build commands (`assembleDebug`, `assembleRelease`, `build.bat`) were executed.\n         - *Expected Result*: Verified; APK compilation left entirely to user.\n"

# Let's search for the position of anchor
pos = text.find("APK compilation left entirely to user.")
if pos == -1:
    print("Could not find anchor!")
    sys.exit(1)

# Find the next '---' after this position
dash_pos = text.find("---", pos)
if dash_pos == -1:
    print("Could not find --- after anchor!")
    sys.exit(1)

insertion = """
- **Phase 4 Kotlin Migration: Auxiliary Services & Network Filter (EnforcementCoreService, FloatingOverlayService, LocalDnsVpnService) (Patch 28 Follow-Up)**:
  - **Why It Was Mandated (Decoupled Background Services & Network Filter Layer)**:
    - *Progressing the Master Kotlin Migration by Focusing on Lifecycle Components*:
      - Following Phase 2 (Receivers and Content Provider), Phase 3 (Classifier engines) was deferred to prioritize the self-contained, low-risk auxiliary services and network filter.
      - Historically, `EnforcementCoreService.java`, `FloatingOverlayService.java`, and `LocalDnsVpnService.java` managed critical Android service lifecycles: headless background watchdogs, draggable WindowManager floating countdown badges, and on-device loopback DNS packet filtering (sinkholing distracting domains to NXDOMAIN).
      - Migrating these services to Kotlin consolidates coroutine-ready lifecycle management, strict null safety, and clean companion object `@JvmStatic` interfaces while preserving 100% binary compatibility with remaining Java enforcement services (`LockPlugin.java`, `LockAccessibilityService.java`).
  - **Concrete Architectural Implementations**:
    - **`EnforcementCoreService.kt` (`android/app/src/main/java/com/uncode/app/EnforcementCoreService.kt`)**:
      - Converted headless background watchdog to Kotlin.
      - Preserves `@JvmStatic fun ensureRunning(context: Context)` companion helper.
      - Employs `START_STICKY` lifecycle pulse to keep Accessibility services and floating overlay active without launching UI activities.
    - **`FloatingOverlayService.kt` (`android/app/src/main/java/com/uncode/app/FloatingOverlayService.kt`)**:
      - Converted draggable WindowManager floating countdown timer service to Kotlin.
      - Preserves `@JvmStatic` helpers: `startService(context, lockEndTime, title)` and `stopService(context)`.
      - Preserves `WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY`, boundary clamping, drag touch listener, 250ms high-precision wall-clock ticker, and tap-to-foreground `MainActivity` intent routing.
    - **`LocalDnsVpnService.kt` (`android/app/src/main/java/com/uncode/app/LocalDnsVpnService.kt`)**:
      - Converted ultra-lightweight loopback Android `VpnService` to Kotlin.
      - Preserves `@JvmStatic` companion endpoints: `startVpn(context)`, `stopVpn(context)`, `updateNotification(context)`, and `@JvmField @Volatile var isRunning: Boolean`.
      - Maintains virtual DNS IPs (`10.111.222.1` & `fd00:111:222::1`), multi-threaded socket worker pool, packet parsing, NXDOMAIN sinkhole responses, and protected upstream DNS socket forwarding.
    - **Safe Backup & Purge of Obsolete Java Sources**:
      - Master backup copies preserved in `scratch/backup_phase4/`.
      - Safely removed legacy files `EnforcementCoreService.java`, `FloatingOverlayService.java`, and `LocalDnsVpnService.java` from the active source tree.
  - **Comprehensive Verification Plan & Matrix (User Rule 3)**:
    - *Affected Files*:
      - [`android/app/src/main/java/com/uncode/app/EnforcementCoreService.kt`](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/android/app/src/main/java/com/uncode/app/EnforcementCoreService.kt)
      - [`android/app/src/main/java/com/uncode/app/FloatingOverlayService.kt`](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/android/app/src/main/java/com/uncode/app/FloatingOverlayService.kt)
      - [`android/app/src/main/java/com/uncode/app/LocalDnsVpnService.kt`](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/android/app/src/main/java/com/uncode/app/LocalDnsVpnService.kt)
      - [`README.md`](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/README.md)
    - *Known Dependents & Callers*:
      - `LockPlugin.java`: Calls `FloatingOverlayService.startService`/`stopService`, `LocalDnsVpnService.startVpn`/`stopVpn`/`updateNotification`.
      - `LockAccessibilityService.java`: Calls `FloatingOverlayService.startService`, `LocalDnsVpnService.startVpn`/`stopVpn`, inspects `LocalDnsVpnService.isRunning`.
      - `AlarmReceiver.kt`: Calls `FloatingOverlayService.startService`/`stopService` and `EnforcementCoreService.ensureRunning`.
      - `ScheduleManager.kt`: Calls `FloatingOverlayService.stopService`.
      - `BootReceiver.kt`: Calls `EnforcementCoreService.ensureRunning` and `FloatingOverlayService.startService`/`stopService`.
      - `SirenReviveReceiver.kt`: Calls `EnforcementCoreService.ensureRunning`.
    - *Step-by-Step On-Device Verification Instructions (Human-Facing Sensory Protocol - No ADB)*:
      1. **Floating Countdown Overlay & Edge Dragging Verification**:
         - *Action*: In QIEZKA, tap "Test Lock Now" (or trigger a scheduled study session). Minimize QIEZKA or switch to the Home screen / allowed app (e.g. Calculator).
         - *Expected Result*: A floating timer badge/pill appears on the screen showing the active countdown clock (e.g., `29:55`). Dragging the pill moves it smoothly across the display and snaps cleanly to the nearest screen edge upon release. Tapping the pill immediately brings QIEZKA to the foreground. When the lock session completes, the floating overlay cleanly vanishes.
      2. **Local DNS VPN Status Bar Key & Sinkhole Activation**:
         - *Action*: When a lock session starts (or Consequence Mode is engaged), look at the Android top status bar.
         - *Expected Result*: The Android VPN key icon (🔑) appears in the status bar alongside a persistent notification ("WebClassifier On-Device Protection Active"). When the lockdown ends (or homework is submitted), the VPN key icon cleanly disappears from the status bar.
      3. **Headless Background Watchdog & Task Switcher Survival**:
         - *Action*: While in active lockdown, open Android Recent Apps and swipe QIEZKA away to dismiss the app card.
         - *Expected Result*: Lockdown remains fully operational without crashing or releasing the phone; `EnforcementCoreService` preserves the background sticky pulse.
      4. **Automated Verification**:
         - *Action*: Run `.\\android\\gradlew.bat -p android compileDebugKotlin compileDebugJavaWithJavac testDebugUnitTest` and `npx tsc --noEmit`.
         - *Expected Result*: Kotlin and Java compilation succeed (`BUILD SUCCESSFUL in 27s`), unit tests pass (`BUILD SUCCESSFUL in 11s`, 0 failures), and TypeScript typecheck reports 0 errors.
      5. **User Rule 2 Compliance Check**:
         - *Action*: Confirm no APK build commands (`assembleDebug`, `assembleRelease`, `build.bat`) were executed.
         - *Expected Result*: Verified; APK compilation left entirely to user.

"""

new_text = text[:dash_pos] + insertion + text[dash_pos:]

with open(readme_path, "w", encoding="utf-8", newline="") as f:
    f.write(new_text)

print("SUCCESSFULLY INSERTED PHASE 4")
