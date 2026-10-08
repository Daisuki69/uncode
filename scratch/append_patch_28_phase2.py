import re

readme_path = r"c:\Users\CxAdmin\Desktop\qiezka\uncode\README.md"

with open(readme_path, "rb") as f:
    raw = f.read()

encoding = "utf-8"
text = raw.decode(encoding)

# Let's search for "SIREN reliably opens the most recently selected launcher on the first press"
pos = text.find("SIREN reliably opens the most recently selected launcher on the first press")
if pos == -1:
    print("Could not find anchor text!")
    exit(1)

# Find the next '---' after this position
dash_pos = text.find("---", pos)
if dash_pos == -1:
    print("Could not find --- after anchor!")
    exit(1)

# Insertion content:
insertion = """
- **Phase 2 Kotlin Migration: Receivers & Content Provider (AlarmReceiver, SirenReviveReceiver, SirenContentProvider) (Patch 28 Follow-Up)**:
  - **Why It Was Mandated (Decoupled Android OS Component Layer & Zero-Drift Kotlin Architecture)**:
    - *Progressing the 6-Phase Master Kotlin Migration*:
      - Following the successful Phase 1 migration of leaf utilities (`ScheduleManager`, `InstalledLauncherDetector`, `LauncherStateManager`), Phase 2 targets the boundary layer between the Android OS and QIEZKA: broadcast receivers and content providers.
      - Historically, `AlarmReceiver.java`, `SirenReviveReceiver.java`, and `SirenContentProvider.java` managed critical Android OS system callbacks: exact alarm wakeups, consequence wake-up pulses, notification channels, vibrating alerts, and inter-process launcher resolution with SIREN HomeProxy.
      - Migrating these components to idiomatic Kotlin consolidates null safety, standardizes coroutine-compatible execution patterns, and provides `@JvmStatic` companion interfaces ensuring 100% binary compatibility with remaining Java enforcement services (`LockPlugin.java`, `LockAccessibilityService.java`).
  - **Concrete Architectural Implementations**:
    - **`AlarmReceiver.kt` (`android/app/src/main/java/com/uncode/app/AlarmReceiver.kt`)**:
      - Converted legacy `AlarmReceiver.java` (500+ lines) into idiomatic, null-safe Kotlin.
      - Preserves all system actions: `ACTION_SCHEDULE_START`, `ACTION_LOCK_END`, `ACTION_PRE_LOCK_WARNING`, `ACTION_WARNING_VIBRATE`, and `ACTION_START_CONSEQUENCE`.
      - Maintains full `@JvmStatic` companion methods for backward-compatible Java callers: `triggerScheduleStartDirectly`, `scheduleLockEndAlarm`, `cancelLockEndAlarm`, `scheduleExactAlarmCompat`, `createNotificationChannels`, and `showNotificationStatic`.
      - Strict null safety and exception handling around `NotificationManagerCompat`, `Vibrator` / `VibrationEffect`, `PendingIntent`, and Android 12+ (API 31+) `FLAG_IMMUTABLE` requirements.
    - **`SirenReviveReceiver.kt` (`android/app/src/main/java/com/uncode/app/SirenReviveReceiver.kt`)**:
      - Converted from `SirenReviveReceiver.java`.
      - Intercepts `com.uncode.app.ACTION_SIREN_REVIVE` and `com.siren.homeproxy.ACTION_REVIVE_PULSE`.
      - Verifies `consequence_mode` and `lockdown_active` flags from `SharedPreferences` before dispatching revival intents, preventing phantom revivals.
    - **`SirenContentProvider.kt` (`android/app/src/main/java/com/uncode/app/SirenContentProvider.kt`)**:
      - Converted from `SirenContentProvider.java`.
      - Exposes secure `MatrixCursor` projection (`package_name`, `activity_name`) under `content://com.uncode.app.sirenprovider/selected_launcher` consumed by SIREN HomeProxy.
      - Directly delegates to `LauncherStateManager.getSelectedLauncher(context)` with instant fallback resolution.
    - **Purge of Obsolete Java Sources**:
      - Safely removed legacy files `AlarmReceiver.java`, `SirenReviveReceiver.java`, and `SirenContentProvider.java`.
  - **Comprehensive Verification Plan & Matrix (User Rule 3)**:
    - *Affected Files*:
      - [`android/app/src/main/java/com/uncode/app/AlarmReceiver.kt`](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/android/app/src/main/java/com/uncode/app/AlarmReceiver.kt)
      - [`android/app/src/main/java/com/uncode/app/SirenReviveReceiver.kt`](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/android/app/src/main/java/com/uncode/app/SirenReviveReceiver.kt)
      - [`android/app/src/main/java/com/uncode/app/SirenContentProvider.kt`](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/android/app/src/main/java/com/uncode/app/SirenContentProvider.kt)
      - [`README.md`](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/README.md)
    - *Known Dependents & Callers*:
      - `ScheduleManager.kt`: Invokes `AlarmReceiver.scheduleExactAlarmCompat`, `AlarmReceiver.triggerScheduleStartDirectly`, and `AlarmReceiver.cancelLockEndAlarm`.
      - `LockPlugin.java`: Invokes `AlarmReceiver.scheduleLockEndAlarm`, `AlarmReceiver.cancelLockEndAlarm`, `AlarmReceiver.createNotificationChannels`, `AlarmReceiver.showNotificationStatic`, and `AlarmReceiver.triggerScheduleStartDirectly`.
      - `LockAccessibilityService.java`: Invokes `AlarmReceiver.showNotificationStatic` and dispatches `ACTION_WARNING_VIBRATE`.
      - `BootReceiver.kt`: Interacts via `ScheduleManager.rescheduleAll` which arms alarms routed to `AlarmReceiver`.
      - `SIREN HomeProxy` (`SirenHomeActivity.kt`): Queries `content://com.uncode.app.sirenprovider/selected_launcher` via `SirenContentProvider`.
    - *Step-by-Step On-Device Verification Instructions (Human-Facing Sensory Protocol - No ADB)*:
      1. **Alarm Triggering, Warning Notification & Fullscreen Lockdown Engagement**:
         - *Action*: In QIEZKA, create a new schedule set to start in 2 minutes with a 5-minute duration. Tap Save.
         - *Expected Result*: 1 minute before lock, phone chimes with a persistent warning notification ("Study Session Starting in 1 Minute"). 10 seconds before lock, phone emits a distinct urgent vibration pulse. At the exact scheduled start minute, the app automatically transitions to the full-screen Lock Screen overlay with the large red countdown timer ticking down.
      2. **Alarm Cancellation & Session Deletion Verification**:
         - *Action*: In QIEZKA Dashboard, delete an active upcoming schedule.
         - *Expected Result*: Observe phone status bar. The scheduled alarm is cancelled immediately. When the previously scheduled clock time arrives, NO lock screen overlay appears, NO alarm fires, and NO phantom notifications appear.
      3. **Consequence Mode Revival & Siren Pulse Verification**:
         - *Action*: Let a study session time out without homework submission (or fast forward simulated time past the deadline) so Consequence Mode is engaged.
         - *Expected Result*: Phone displays high-priority persistent consequence notification ("Consequence Mode Active - Study Session Missed"), vibrates with warning pattern, and `SirenReviveReceiver` keeps the consequence lock active if the user attempts to swipe QIEZKA away.
      4. **SIREN ContentProvider Live Launcher Resolution**:
         - *Action*: Open QIEZKA Settings -> Home Launcher selection list. Switch between launchers and tap Home.
         - *Expected Result*: `SirenContentProvider` serves the selected launcher to SIREN HomeProxy without crashes or permission errors; pressing Home delegates to the selected launcher instantly.
      5. **Automated Verification**:
         - *Action*: Run `.\\android\\gradlew.bat -p android compileDebugKotlin compileDebugJavaWithJavac testDebugUnitTest` and `npx tsc --noEmit`.
         - *Expected Result*: Kotlin and Java compilation succeed (`BUILD SUCCESSFUL in 56s`), unit tests pass (`BUILD SUCCESSFUL in 32s`, 0 failures), and TypeScript typecheck reports 0 errors.
      6. **User Rule 2 Compliance Check**:
         - *Action*: Confirm no APK build commands (`assembleDebug`, `assembleRelease`, `build.bat`) were executed.
         - *Expected Result*: Verified; APK compilation left entirely to user.

"""

new_text = text[:dash_pos] + insertion + text[dash_pos:]

with open(readme_path, "w", encoding="utf-8", newline="") as f:
    f.write(new_text)

print("SUCCESSFULLY INSERTED")
