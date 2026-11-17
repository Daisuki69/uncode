# ⚡ QIEZKA

### What is Qiezka?
Qiezka is a normal quiz-based lockdown app for Android.
It's like any other lockdown app: it is supposed to force you to do what you promised to do.

### Let us establish first a taxonomy

* **I** - Enforcing the Core goal (Integrity in our Case)
* **U** - User Driven Allowlist Blocklist
* **E** - Enforcement against bypass

`U` cannot exist exactly at the same time as `I`.
User decision makes Integrity in jeopardy; a user can lie that an obvious distracting app is not distracting.

1. `U + E` is a combination where usually the majority of focus apps exist, from weak to ultra-strong focus apps.
2. `I + E` is where real Integrity exists.

The only time a `U` can exist at the same time as `I` is if it is bounded by `I`, such as:

1. `I (U) + E`
2. `I (U)`

The only catch is:
You can only restrict further than what `I` already allowed in the first place.

Choice 1 is the **IDEAL** Focus app because it makes Integrity and Enforcement a priority, but it can only be as strong IF it can be granted using standard Android permissions.

By forcing the user to learn ADB or root their phone just to install the app, those tools accidentally train the user on how to defeat them (e.g., Device-Owner, Shizuku, Root-Based).

Device Owner is included even if the only way out is flashing stock firmware via recovery/fastboot, because via the steps they did, the user learned what a normal user wouldn't. Also, a bugged app running as Device Owner or Root is inherently more destructive.

By keeping the setup strictly on standard permissions, this architecture creates **asymmetric friction**:
* **The Entry Floor (Zero Technical Skill):** Any normal user can install two APKs and tap "Allow" on Android's native UI prompts for Accessibility, Device Admin, Battery Optimization, and Default Home App (`RoleManager.ROLE_HOME`). Even Android 13+'s "Restricted Settings" block is guided step-by-step right inside `PermissionWalkthrough.tsx`. You don't need a PC, terminal commands, or developer options to lock yourself in.
* **The Escape Ceiling:** Once those standard toggles are flipped, the two APKs interlock. To break out during an active lock or Post Lockdown, that same non-technical user suddenly has to figure out:
  * Why Force Stopping the app fails.
  * Why rebooting into Safe Mode hides the app icon.
  * Why importing a doctored backup JSON fails.

To escape, a user who only knew how to tap "Allow" on a screen now has to understand Safe Mode and ways to boot into it at the same time failing to do it, hunt down disguised packages in Apps which can be frustrating, or learn how to set up ADB on a computer from scratch while their phone is actively fighting them.

Though reality still holds for the 1% of power users: if you *already* have ADB set up on your PC or know how Device Admin lists work in Safe Mode before you install the app, the ceiling isn't high enough to trap you.

---

## How to properly use QIEZKA:

1. Go to **Releases** and install the latest app.
   *(It is recommended to also install the Companion App **SIREN**)*
2. Open Qiezka and initiate the app.
3. Grant each permission directly from within the app via in-app buttons.

> **Note:** On Android 13+: if Accessibility appears greyed out, tap **App Info** inside the onboarding — then tap the three-dot menu → *Allow restricted settings* — then return and grant it.

### On the Dashboard (Creating Decks)
1. Tap **Create Decks**
2. Tap **'+'** button
3. Select either **Manual**, **Import**, or **Custom Import**
4. Tap **Save**
*(Do this as much as you like)*

### How to set a Session
1. Tap **Add** under Focus Sessions
2. Select a deck
3. Set time and duration
4. Tap **Finalize**

Now just wait till due.
> **Note:** While there is a scheduled session, if the time remaining reaches 60 mins, **Stage 1** activates.

### In an Active Focus Session
* You can tap the selected deck to review, open other apps, or browse the internet—only if it isn't blocked while The Enforcement Engine is active.
* You can tap **'Take Quiz Now'** to exit early.
* After the duration ends, you will have to pass the mandatory **Post-Lockdown Quiz**.

### Grading & Passing Criteria
In order to pass, accumulated points must be **80%** of total.

The current grading system is speed-based:
* `1.5s` for **Unreal** — worth `1.5 points`
* `3.0s` for **Instant** — worth `1.0 points`
* `4.5s` for **Sharp** — worth `0.75 points`
* `6.0s` for **Deliberate** — worth `0.5 points`
* `>7.5s` for **Hesitant** — worth `0.25 points`

*If the initial tap is wrong, then the whole question is worth `0 points`.*

**If the user fails to reach 80%:**
* **In a 'Take Quiz Now' scenario:** A user can tap the back button to return to the Focus Session.
* **In a Mandatory Quiz Session:** User will be directed to Dashboard with the Post-Lockdown banner active. The Enforcement Engine is still active. In this state, you can take a Standard Quiz or schedule another Focus Session.

**To fully remove the Enforcement:**
You have to reschedule the failed session and have to pass the Quiz.

---

## What is MathX?

### **QIEZKA MathX**
MathX is a procedural math quiz engine designed to eliminate answer memorization.
Instead of static questions with fixed answers, you build formulas using the integrated scientific calculator keypad. Every quiz attempt randomizes variables, constants, operators, and coefficients, computing fresh solutions to force you to actually solve the math every single time.

#### **1. Integrated SciCalc Formula Creation**
Type a base formula directly using the built-in scientific calculator keypad:
* **User-created formula:** `ax² + bx + c = 0`

You only need to define the algebraic structure once.

#### **2. Dynamic Instance & Answer Generation**
On every quiz run, the engine generates a valid instance by randomizing variables, constants, operators, and coefficients. It computes the exact mathematical solution on the fly and builds fresh answer choices.

* **Base Formula 1:** `ax² + bx + c = 0`
  * **Quiz Attempt 1:** `3x² - 12x + 9 = 0` → `x = 1, 3`
  * **Quiz Attempt 2:** `2x² - 10x + 12 = 0` → `x = 2, 3`
* **Base Formula 2:** `a(x [op] b) = y`
  * **Quiz Attempt 1:** `2(x + 3) = 14` → `x = 4`
  * **Quiz Attempt 2:** `5(5 - 2) = x` → `x = 15`
* **Base Formula 3:** `(ax² [op1] b) / c [op2] d = z`
  * **Quiz Attempt 1:** `(3y² + 15) / 3 - 2 = 12` → `y = 3, -3`
  * **Quiz Attempt 2:** `(2z² - 8) / 4 + 5 = 11` → `z = 4, -4`

Because the numbers and answers shift every time, memorizing previous runs is useless.

#### **3. Accuracy Scoring & Overall Timer**
Math Decks drop per-question reaction speed scoring in favor of pure accuracy under an overall quiz timer.
* Full credit for every correct solution—no points lost for taking a few extra seconds to calculate.
* Bank time on simpler equations to spend on heavier calculations.
* You must actually work through the steps to pass before the overall clock runs out.

#### **4. Anti-Cheat**
Every time the app detects it is not in the foreground or the user switches apps, the quiz variables, constants, operators, and coefficients get re-randomized on the fly without restarting the quiz from zero. It is recommended to own a scientific calculator and have it ready whenever.

#### **5. Standard Quiz Apps vs. Math Deck**

| Standard Quiz Deck | QIEZKA Math Deck |
| :--- | :--- |
| Static text questions | SciCalc keypad formulas |
| Fixed, repeatable answers | Randomized variables, constants, operators & coefficients |
| Per-question speed scoring | Accuracy-based + overall timer |
| Beaten by memorization | Requires real computation |

### How to use MathX
In the Dashboard:
1. Tap **'Add'**
2. Type a valid formula using the integrated keyboard
3. Tap **Save**
4. Then set a Schedule

---

## The Enforcement Engine

### 1. 3-Stage App Classifier 🧠
* **Stage 1 — Master Veto Gate:**
  Android Settings, all OEM Phone Managers (MIUI Security Center, Samsung Device Care, ColorOS SafeCenter), and hardware bloatware (Joyose, GameCenter, PalmStore, Glance) are unconditionally blocked. No exceptions, no appeal.
* **Stage 2 — Evaluation:**
  Apps on the `KnownSafe.kt` are allowed; apps on the `KnownDistracting.kt` are blocked unless Unified Policy says so. Qiezka uses a table in order to know if it should block an app or not. If an app is undefined, Qiezka will try to evaluate it:
  `CATEGORY_GAME` blocked. `CATEGORY_SOCIAL` blocked. `CATEGORY_VIDEO` and `CATEGORY_NEWS` blocked. `CATEGORY_PRODUCTIVITY` allowed. Combined with Positive and Negative Signals.
* **Stage 3 — Universal System Gateway:**
  Apps on the OEM system partition (`FLAG_SYSTEM`) that passed all veto gates are allowed as legitimate hardware infrastructure.
* **Layer 5 — Conservative Fallback:**
  Unknown user-installed app with no category, no academic signals, not system-partitioned → **blocked**.

### 2. 2-Stage Web and PWA/TWA Classifier 🌐
* **Stage 1 — Evaluation:**
  URLs on `KnownSafeWeb.kt` are allowed; URLs on the `KnownDistracting.kt` are blocked. Qiezka uses a table in order to know if it should block a URL or not + Unified Policy toggle. If a URL is undefined, Qiezka will try to evaluate it:
  Live Chromium DOM scan reads the page's accessibility tree — academic signals promote it, distraction signals block it.
* **Stage 2 — Conservative Fallback:**
  Unknown URL with no academic signals, with or without distraction signals, are **Blocked**.

### 3. Anti-Bypass Stack ⚔️
* **Device Admin** — blocks uninstall.
* **Accessibility Service** — blocks distracting apps.
* **Old KeepAlive Strategy:** We used Home Shell Proxy (`QiezkaHomeHandlerActivity` holds `ROLE_HOME`) — pressing Home auto-revives the enforcement process if force-stopped/killed. Works best in 3-button navigation.
* **Launcher Change Interception:** `isFastHomeRoleEvent()` detects role change dialogs sub-millisecond. 4-burst retry at `0/25/60/120ms` dismisses the picker before it finishes inflating. *(OLD KeepAlive Strategy)*
* **Current KeepAlive Strategy:** Uses the same method as HomeShell PROXY, **BUT** as a `VoiceAssistant`.
* **Boot Persistence** — `BootReceiver` + `AlarmManager` exact alarms fire on `ACTION_BOOT_COMPLETED`. If lockdown was active, it resumes. If timer expired while powered off, Post Lockdown activates immediately.
* **Sideloaded Apps Detection** — newly installed apps detected via installer/store package monitoring are immediately classified via 3-Stage App Classifier 🧠.
* **Android Safe Mode** is mitigated, not through some zero-day exploits. We won't tell you how and you shouldn't figure it out.

---

## Limitations
* Not guaranteed to be bug-free and inescapable.

## Privacy and data handling
* We don't use an online database; everything works offline. We don't ask for personal information.

## Known issues (Verbose)
1. At some bug after I finish quiz, idk what happened or what did I do, but I clicked back, suddenly hit with 18hr focus session. Clicking quiz now doesn't work; it is red, but when I click it, it dims out.
   * *Note:* This would never happen on original uncode; check why whether cuz it's impossible for timer to go beyond duration.
2. Wait too long, there's no animation at all, check why.
3. Autoquiz happened and I failed. Clicking quiz now doesn't work; it is red, but when I click it, it dims out. I pressed back, it gave me 16hrs. Investigate correlation to no. 1.
4. Recent opened apps shows `-556 minutes`.
5. A rescheduled autoquiz then tap take quiz button (maybe just quiz) gives me quiz randomized from every deck (supposed to be from a deck only).
   * After reaching `0:00` in timer, while Qiezka is in background, it didn't take me to mandatory quiz; it instead calls for homework expired.
   * I rescheduled again, but waiting for timer to `0:00` while Qiezka is in foreground, I can see notif "few seconds remain till Qiezka auto unlocks" or something. After reaches `0:00`, I can split-second see timer restarted to duration then transferred me to mandatory quiz.
   * I can't click at choices nor the back button (maybe just lag).
6. The back button in the mandatory quiz (post-lockdown quiz) should mean move user to dashboard, call for quiz not passed or expired.
   * Autoquiz happened, I tapped back or home button, but every time I open another app it auto-launches Qiezka. Idk if I like it or not, but I think if autoquiz pops up, I think I should be able to still dismiss it or open another app, but if the autoquiz timer or the allocated duration reached `0:00`, then call for quiz expired or not passed.
7. Failed autoquiz should call for failed session, not restart quiz.
   * **ALSO REMINDER:** AUTOQUIZ TIMER STARTS ONLY IF USER IS IN non-allowed apps for a specific amount of time, and this timer is accumulated across apps within a specific range of time (meaning if I opened TikTok for 13 minutes an hour or hours ago then right now).
8. Stage 1 restriction should only happen if the schedule is within range.
9. Don't you think the scheduling or the timer, whether it's from reschedule or step 2 schedule, should set time at the nearest clean 5-min time and AM or PM?

---

### **App was tested on:**
* **Redmi Note 12 4G (`tapas`)** running Android 16 QPR1 Evolution-X 11.5 ROM
* **SAMSUNG Galaxy A30** running Android 11 One UI 3.1

### **App Flow**

```mermaid
flowchart TD

    %% ----------------------------------------------------
    %% 0. UNIFIED POLICY REGISTRY & USER UI CONFIGURATION
    %% ----------------------------------------------------
    subgraph SUB_POLICY ["0. Unified Service Policy Engine (Dev Registry & UI Config)"]
        REG_DEV["<b>Developer Registry</b><br>• <b>YouTube</b>: com.google.android.youtube + youtube.com<br>• <b>Gemini</b>: com.google.android.apps.gemini + gemini.google.com<br>• <b>OpenAI</b>: com.openai.chatgpt + chatgpt.com<br>• <b>Claude</b>: com.anthropic.claude + claude.ai"]:::devNode --> UI_TOGGLE(["👤 User Toggles Service in App UI<br>(Allow Gemini / OpenAI / Claude / YouTube)"]):::uiNode
        UI_TOGGLE --> SYNC_POLICY["🔄 Synchronize uncode_lock.xml & Invalidate Caches<br>• Package IDs synced to <b>KnownSafe</b><br>• Web Domains synced to <b>KnownSafeWeb</b><br>• AppClassifier & WebClassifier caches cleared"]:::bridgeNode
    end

    %% ----------------------------------------------------
    %% 1. BOOT & STARTUP LIFECYCLE
    %% ----------------------------------------------------
    BOOT_START(["🔄 User Restarts Device<br>Device Reboots / Powers On"]):::startNode --> OS_INIT["📱 Android OS Boot Completed<br>System Server Initializes"]:::actionNode
    OS_INIT --> BROADCAST_BOOT["📡 Android Dispatches<br>ACTION_BOOT_COMPLETED"]:::actionNode
    OS_INIT --> BIND_A11Y["⚙️ Android AccessibilityManager<br>Binds LockAccessibilityService"]:::actionNode

    subgraph SUB_BOOT ["1. BootReceiver Execution"]
        BROADCAST_BOOT --> BOOT_RCV["📥 BootReceiver.kt onReceive()"]:::actionNode
        BOOT_RCV --> RESCHED_ALL["⏰ ScheduleManager.rescheduleAll()<br>Re-register AlarmManager exact alarms"]:::actionNode
        RESCHED_ALL --> READ_PREFS_BOOT["📖 Read uncode_lock.xml<br>lockdown_active, lock_end_time, schedules, policies"]:::actionNode

        READ_PREFS_BOOT --> CHECK_WAS_LOCKED{"Was lockdown active<br>before restart?"}:::gateNode
        
        CHECK_WAS_LOCKED -- "Yes" --> CHECK_TIME_EXP{"Did timer expire while<br>device was powered off?"}:::gateNode
        CHECK_TIME_EXP -- "Yes (now >= lockEndTime)" --> EXPIRE_TO_CONSEQUENCE["Clear lockdown_active<br>Set consequence_active = true"]:::blockNode
        CHECK_TIME_EXP -- "No (Lockdown still active)" --> RESCHED_ALARM["Reschedule Alarm &<br>Relaunch QIEZKA to foreground"]:::actionNode

        CHECK_WAS_LOCKED -- "No" --> CHECK_BOOT_SCHED{"Check Active Schedules<br>Status on Boot"}:::gateNode
        CHECK_BOOT_SCHED -- "Window Missed While Off<br>(now >=
