# Kotlin Implementation Plan & Execution Record

This document records the architectural blueprint, file-by-file roles, hybrid interoperability mechanics, and the step-by-step migration of QIEZKA's native Android codebase from Java to Kotlin.

---

## 1. Architectural Strategy: Phased Hybrid Migration

Kotlin and Java have **100% bidirectional interoperability**. Both compile to JVM `.class` bytecode and link into the same DEX files inside the final APK.

### Interoperability Mechanics:
- **`@JvmStatic` & `@JvmField`**: Kotlin objects and companion functions are annotated so that existing Java callers access them with zero syntax changes.
- **Compilation Pipeline**:
  1. `:app:compileDebugKotlin` compiles `.kt` files.
  2. `:app:compileDebugJavaWithJavac` compiles `.java` files against the compiled Kotlin classes.
  3. Hybrid builds execute concurrently without breaking changes.

---

## 2. File-by-File Inventory: What Every Native File Does

There are **15 native Android source files** in `android/app/src/main/java/com/uncode/app/`:

| File | Status | Size | Architectural Role & Responsibilities |
| :--- | :---: | :---: | :--- |
| **`AdminReceiver`** | **Kotlin (.kt)** | ~0.4 KB | **Device Admin Privileges**: Subclasses `DeviceAdminReceiver` to prevent students from uninstalling QIEZKA while an active lockdown session is ongoing. |
| **`BootReceiver`** | **Kotlin (.kt)** | ~2.2 KB | **Reboot Persistence**: Intercepts `BOOT_COMPLETED` to immediately restore active schedules, reset alarms, and re-launch lockdown if the device was rebooted during a lock period. |
| **`BlacklistConstants`** | **Kotlin (.kt)** | ~5.8 KB | **Stage 1 Anti-Tamper & Hostile Signatures**: Stores hardcoded anti-tamper packages (Settings, MIUI Security, SafeCenter) and `hasHostileSubstring` for detecting PWA and window title bypasses. |
| **`SystemUadAllowlist`** | **Kotlin (.kt)** | ~32 KB | **UAD-NG Ground Truth Allowlist**: Brand-partitioned sets (AOSP, Samsung, Xiaomi, Oppo, Vivo, Motorola, Huawei, Transsion) protecting emergency SOS, screenshot markup, intent resolvers, and captive portals. |
| **`WebBlocklistConstants`** | **Kotlin (.kt)** | ~34 KB | **Web Domain Blacklist & Academic Whitelist**: Stores 400+ gaming, social, adult, and piracy domains alongside worldwide academic immunity (.edu, .ac.*, .gov) and host extraction utilities. |
| **`AppClassifier`** | Java (.java) | ~33 KB | **3-Stage Dynamic App Engine**: Evaluates app packages during lockdown. Stage 1 (anti-tamper/bloat veto), Stage 2 (category distraction detection & unified whitelist), Stage 3 (universal system gateway), Layer 5 (fallback). |
| **`WebClassifier`** | Java (.java) | ~38 KB | **Browser DOM & URL Inspector**: Detects distracting URLs in Chrome/Edge/Firefox address bars and accessibility nodes, driving multi-step auto-back remediation. |
| **`ScheduleManager`** | Java (.java) | ~11 KB | **Schedule Coordinator**: Calculates recurring schedules (daily, weekly, custom days) and schedules alarms with `AlarmManager`. |
| **`PunishmentManager`** | Java (.java) | ~17 KB | **Penalties & Escalation Engine**: Tracks violation counts, extends lock durations, and logs bypass infractions. |
| **`AlarmReceiver`** | Java (.java) | ~24 KB | **RTC Alarm Lifecycle Receiver**: Receives exact wake-up alarms from `AlarmManager` to start/stop locks, trigger penalties, and display notifications. |
| **`LocalDnsVpnService`** | Java (.java) | ~23 KB | **DNS Sinkhole VPN Service**: Local Android `VpnService` that intercepts UDP port 53 packets to sinkhole distracting domains without an external server. |
| **`FloatingOverlayService`** | Java (.java) | ~18 KB | **System Alert Window Overlay**: Draws the floating lock screen, break countdown, and warning banner on top of distracting apps. |
| **`MainActivity`** | Java (.java) | ~1.1 KB | **Capacitor Host Activity**: Boots Capacitor web runtime and registers native bridge plugins. |
| **`LockPlugin`** | Java (.java) | ~63 KB | **Capacitor Native Bridge**: Bridges JavaScript/React to native Android calls (`startLock`, `getLockStatus`, `setWhitelist`, etc.). |
| **`LockAccessibilityService`** | Java (.java) | ~101 KB | **The Core Sentinel Service**: Listens for window/app changes, blocks apps, coordinates overlays, and intercepts browsers. |

---

## 3. Tier 1 Execution Record: COMPLETED & VERIFIED

### Actions Taken:
1. **Gradle Build Environment**:
   - Added `kotlinVersion = '1.9.24'` to `variables.gradle`.
   - Added `kotlin-gradle-plugin` to root `build.gradle`.
   - Applied `kotlin-android` plugin and added `kotlin-stdlib` to `app/build.gradle`.
   - Configured `org.gradle.java.home` in `gradle.properties` to target Android Studio's bundled JDK 21 LTS (`jbr`).
2. **File Conversion**:
   - `AdminReceiver.java` $\rightarrow$ `AdminReceiver.kt`
   - `BootReceiver.java` $\rightarrow$ `BootReceiver.kt`
   - `BlacklistConstants.java` $\rightarrow$ `BlacklistConstants.kt`
   - `SystemUadAllowlist.java` $\rightarrow$ `SystemUadAllowlist.kt`
   - `WebBlocklistConstants.java` $\rightarrow$ `WebBlocklistConstants.kt`
3. **Compiler Fixes**:
   - Cleaned up Java `import java.util.Set;` and `import java.util.HashSet;` in Kotlin files to let Kotlin collections resolve natively.
   - Removed duplicate leftover method body in `WebBlocklistConstants.kt`.
4. **Compilation Verification**:
   - Executed `./gradlew.bat compileDebugSources`.
   - **Result**: `BUILD SUCCESSFUL in 1m 37s`. Both `:app:compileDebugKotlin` and `:app:compileDebugJavaWithJavac` completed with 0 errors.

### Device ADB Verification Record (`f678bc48`):
- **Package Status**: Verified `com.uncode.app` updated and active with targetSdk 35.
- **Accessibility Service**: Confirmed `com.uncode.app/.LockAccessibilityService` running.
- **Anti-Tamper Eviction**: Triggered `am start -a android.settings.SETTINGS` $\rightarrow$ immediately intercepted and evicted back to QIEZKA lock screen.
- **System Share Sheet / Intent Resolver**: Triggered `android.intent.action.SEND` $\rightarrow$ `com.android.internal.app.ResolverActivity` stayed open over lock screen without being evicted or closed. Verified via device screenshot.
- **Stability**: Zero crashes, zero runtime exceptions.

---

## 4. Migration Roadmap

### 🟢 Tier 1: Data, Constants & Simple Receivers (COMPLETED ✅)
- `AdminReceiver.kt`
- `BootReceiver.kt`
- `BlacklistConstants.kt`
- `SystemUadAllowlist.kt`
- `WebBlocklistConstants.kt`

### 🟡 Tier 2: Business Logic & State Engines (NEXT UP)
- `ScheduleManager.java` $\rightarrow$ `ScheduleManager.kt`
- `PunishmentManager.java` $\rightarrow$ `PunishmentManager.kt`
- `AppClassifier.java` $\rightarrow$ `AppClassifier.kt`
- `WebClassifier.java` $\rightarrow$ `WebClassifier.kt`
- `AlarmReceiver.java` $\rightarrow$ `AlarmReceiver.kt`

### 🔴 Tier 3: Core Android Services & Capacitor Plugins (FINAL PHASE)
- `FloatingOverlayService.java` $\rightarrow$ `FloatingOverlayService.kt`
- `LocalDnsVpnService.java` $\rightarrow$ `LocalDnsVpnService.kt`
- `MainActivity.java` $\rightarrow$ `MainActivity.kt`
- `LockPlugin.java` $\rightarrow$ `LockPlugin.kt`
- `LockAccessibilityService.java` $\rightarrow$ `LockAccessibilityService.kt`
