# ⚡ QIEZKA

<div align="center">

```
   ██████╗ ██╗███████╗███████╗██╗  ██╗ █████╗ 
  ██╔═══██╗██║██╔════╝╚══███╔╝██║ ██╔╝██╔══██╗
  ██║   ██║██║█████╗    ███╔╝ █████╔╝ ███████║
  ██║▄▄ ██║██║██╔══╝   ███╔╝  ██╔═██╗ ██╔══██║
  ╚██████╔╝██║███████╗███████╗██║  ██╗██║  ██║
   ╚══▀▀═╝ ╚═╝╚══════╝╚══════╝╚═╝  ╚═╝╚═╝  ╚═╝
```

### *This app is specifically made for me, do not blame me for bricked phones*

[![Android](https://img.shields.io/badge/Platform-Android%2010%20to%2016%20(API%2029--36)-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com/)
[![React](https://img.shields.io/badge/Frontend-React%2019%20%7C%20TypeScript-61DAFB?style=for-the-badge&logo=react&logoColor=black)](https://react.dev/)
[![Capacitor](https://img.shields.io/badge/Bridge-Capacitor%207-119EFF?style=for-the-badge&logo=capacitor&logoColor=white)](https://capacitorjs.com/)
[![Vite](https://img.shields.io/badge/Bundler-Vite%206-646CFF?style=for-the-badge&logo=vite&logoColor=white)](https://vitejs.dev/)
[![Tailwind](https://img.shields.io/badge/Styling-Tailwind%20CSS%204-06B6D4?style=for-the-badge&logo=tailwindcss&logoColor=white)](https://tailwindcss.com/)
[![Google Gemini](https://img.shields.io/badge/AI%20Evaluation-Google%20Gemini%202.0%20%2F%202.5-8E75B2?style=for-the-badge&logo=googlegemini&logoColor=white)](https://aistudio.google.com/)
[![Security](https://img.shields.io/badge/Security-Device%20Admin%20%2B%20Accessibility-E91E63?style=for-the-badge)](https://developer.android.com/guide/topics/admin/device-admin)
[![Overlay](https://img.shields.io/badge/Overlay-Floating%20Ball%20Timer-FF5722?style=for-the-badge)](https://developer.android.com/reference/android/view/WindowManager.LayoutParams#TYPE_APPLICATION_OVERLAY)
[![API Model](https://img.shields.io/badge/Privacy-100%25%20BYOK%20Zero--Telemetry-00C853?style=for-the-badge)](#-online-architecture--offline-resources-byok)
[![License](https://img.shields.io/badge/License-MIT-green?style=for-the-badge)](LICENSE)

<br/>

**[⚡ Philosophy](#-philosophy-procrastination-prevention)** • 
**[⚖️ Comparison Matrix](#%EF%B8%8F-qiezka-vs-traditional-app-blockers)** • 
**[🏗️ Architecture](#%EF%B8%8F-system-architecture)** • 
**[🧠 Local App Classifier](#-local-app-classifier-engine-on-device-5-layer-heuristics)** • 
**[🚨 Consequence Lockdown](#-consequence-lockdown--operating-hours-700-pm--300-am)** • 
**[⚡ Operating Modes: Safemode vs Hardcore](#-operating-modes-safemode-vs-hardcore-247)** • 
**[🌐 Web & Browser Protection](#-web--browser-protection-accessibility-guard--local-dns-sinkhole)** • 
**[📰 Case Studies](#-case-studies--article-declutter-system)** • 
**[🔮 Floating Ball Overlay](#-floating-assistive-timer-ball)** • 
**[📋 App Directory](#-complete-application-reference-blocked--allowed)** • 
**[🛡️ Anti-Cheat & Security](#%EF%B8%8F-security--anti-cheat-architecture)** • 
**[🚀 Setup & ADB](#-setup-guide)** • 
**[📱 OEM Guides](#-oem-rom-optimization-guide)** • 
**[📂 Codebase Tour](#-codebase-architecture--tour)** • 
**[📜 Engineering Changelog](#-engineering-changelog--architecture-evolution-log)** • 
**[🔮 Future Roadmap & Forks](#-future-roadmap--ecosystem-forks)** • 
**[❓ FAQ](#-frequently-asked-questions-faq)**

</div>

Most productivity apps fail because they operate at one of two counterproductive extremes:

1. **The "Honor System" (Too Soft)**: Apps like *Forest*, *StayFree*, or standard Pomodoro timers show polite reminder dialogs, draw virtual trees, or set soft timers. When a student feels cognitive friction or boredom, they dismiss the dialog with a single swipe, disable the timer, and plunge right back into endless scrolling.
2. **The "Dumb Phone" Brick (Blindly Restrictive)**: Traditional kiosk and extreme lockdown tools disable the entire phone or block every app except phone calls. This completely ruins modern academic workflows: students can't access lecture slides in Google Drive, consult PDF textbooks in Adobe Acrobat, write down thoughts in Obsidian or Samsung Notes, translate Latin or German homework in DeepL, or ask Google Gemini to explain a complex physics derivation.

**QIEZKA rejects both extremes.**

Our design directive is uncompromising: **Obliterate the algorithmic dopamine pits of wasted time, while keeping the full homework, research, and AI accessible.**

---

## 🏗️ System Architecture

### 1. Multi-Layer Component Flow

```mermaid
flowchart TD
    subgraph NativeOS ["Android 10 - 16 Native Operating System Layer"]
        LAS["LockAccessibilityService<br/>• Window Event Interceptor<br/>• Quick Settings Collapser<br/>• Consequence Operating Hours (7PM-3AM)"]
        AC["AppClassifier<br/>• On-Device 5-Layer Classifier<br/>• Android Category Inspector<br/>• Semantic Label & Pkg Heuristics<br/>• Telephony Safety Infrastructure"]
        FOS["FloatingOverlayService<br/>• WindowManager Overlay Ball<br/>• Physics Edge-Snap & Retry Badge<br/>• Special-Use Foreground Daemon"]
        AM["AlarmManager & BootReceiver<br/>• Exact RTC Wall-Clock Alarms<br/>• Reboot Auto-Recovery<br/>• Consequence Alert Notifications"]
        DPM["DevicePolicyManager<br/>• Device Administrator Admin<br/>• Anti-Uninstall Enforcement"]
    end

    subgraph Bridge ["Capacitor 7 Native Two-Way IPC Bridge"]
        LP["LockPlugin.java<br/>• Two-Way Native IPC<br/>• SharedPreferences Sync<br/>• Consequence State Manager<br/>• Overlay & Service Controller"]
    end

    subgraph WebApp ["React 19 + TypeScript Application Core"]
        DASH["Dashboard & Schedule Hub<br/>• Live Countdown Cards<br/>• Consequence Active Alert Banner<br/>• Auto-Allowed Apps Display"]
        CREATE["Step-by-Step Schedule Creator<br/>• OCR Extraction & Inline Editor<br/>• Case Study Detection & Gating<br/>• Strict 90-Min Ultradian Cap"]
        LOCK["Full-Screen LockScreen View<br/>• Native Camera Document Capture<br/>• Real-Time Timer Synchronization<br/>• Auto-Detected Accessible Apps"]
        LOGS["Failed Homeworks Log<br/>• Strict Failure Accountability<br/>• Original Schedule Window Details<br/>• Add Again (Reschedule & Retake)"]
        RES["Study Resource Library<br/>• Course Notes & Flashcards<br/>• Case Studies & Article Declutter"]
    end

    subgraph CloudServices ["Client-Side BYOK Cloud Services (Direct HTTPS)"]
        GEMINI["Google Gemini 2.0 / 2.5 API<br/>• Multimodal Handwritten Analysis<br/>• Dynamic Rubric Generator<br/>• Resource & Article Decluttering"]
        OCR["OCR.space REST API<br/>• Handwritten Mode Transcription<br/>• Document Optical Character Recognition"]
    end

    LAS -->|Blocks Hostile Apps & Settings| LAS
    LAS <-->|Queries Local Classifier| AC
    LAS -->|Dispatches Lock Intent| LOCK
    AM -->|Triggers Exact Start / Consequence| FOS
    AM -->|Auto-Relaunch on Alarm| LOCK
    DPM -->|Blocks App Uninstall in Settings| LAS

    LP <-->|Synchronizes Whitelist & Consequence Mode| LAS
    LP <-->|Starts / Stops Floating Ball View| FOS
    DASH <-->|Capacitor JavaScript Bridge| LP
    LOCK <-->|Capacitor JavaScript Bridge| LP

    LOCK -->|Dispatches Photo Proof| OCR
    OCR -->|Returns Raw Transcribed Text| GEMINI
    GEMINI -->|Returns JSON Score & Feedback| LOCK
    LOCK -->|Passed: Harvests AI Research| RES
    LOCK -->|Timeout: Enters Consequence Mode| LOGS
    RES -->|Declutters via Gemini| RES
```

---

### 2. Lockdown Lifecycle State Machine

```mermaid
stateDiagram-v2
    [*] --> Idle: App Installed & Permissions Granted
    Idle --> Scheduled: User Creates Schedule (Max 90 Mins)
    Idle --> ActiveLockdown: User Taps "Start Lockdown Now"
    Scheduled --> ActiveLockdown: Wall-Clock Alarm Triggers (AlarmManager)
    
    state ActiveLockdown {
        [*] --> InitializeLock
        InitializeLock --> SpawnOverlay: Start FloatingOverlayService
        InitializeLock --> EngageAccessibility: Set lockdown_active = true
        
        state PermittedAppWorkflow {
            [*] --> AllowedAppOpened
            AllowedAppOpened --> FloatingBallVisible: Draggable & Snaps to Edge
            FloatingBallVisible --> AllowedAppOpened: Research in Gemini, Docs, or Notes
            FloatingBallVisible --> ReturnToQiezka: Tap Ball to Submit Proof
        }
        
        state BlockedAppWorkflow {
            [*] --> HostileAppOpened
            HostileAppOpened --> DetectPackage: Accessibility Event Fired
            DetectPackage --> ClassifyApp: Local 5-Layer AppClassifier
            ClassifyApp --> CollapseQuickSettings: Notification Shade Pulled
            ClassifyApp --> RouteHome: Block Hostile / Distraction
            RouteHome --> ForceQiezkaFront: Re-open Qiezka Lock Screen
        }
        
        ReturnToQiezka --> CaptureHomework: Camera Photo Proof
        CaptureHomework --> CloudEvaluation: OCR.space + Gemini Evaluation
    }
    
    CloudEvaluation --> PassUnlocked: AI Score Meets Passing Threshold
    CloudEvaluation --> ActiveLockdown: Score Below Passing Threshold (Try Again)
    ActiveLockdown --> ConsequenceMode: Timer Reaches 00:00 (Timeout)
    
    state ConsequenceMode {
        [*] --> CheckOperatingHours: Check Clock Time
        CheckOperatingHours --> EnforceRestrictions: 7:00 PM – 3:00 AM (Operating Window)
        CheckOperatingHours --> PauseRestrictions: 3:00 AM – 7:00 PM (School / Sleep)
        PauseRestrictions --> EnforceRestrictions: Clock Hits 7:00 PM
        EnforceRestrictions --> RescheduleFlow: Tap "View Failed Homeworks" on Banner
        RescheduleFlow --> ExpandFailedCard: View Original Window & Click "Add Again"
        ExpandFailedCard --> SetNewTime: Set Retake Time & Cascade Schedules
    }
    
    SetNewTime --> ActiveLockdown: Retake Session Initiated
    PassUnlocked --> Idle: Consequence Cleared, Floating Ball Removed & Full Device Unlocked
```

---

## 🧠 Local App Classifier Engine (On-Device 5-Layer Heuristics)

To solve the limitations of static lists without requiring central servers or consuming personal API keys on every app launch, QIEZKA includes an **On-Device Local App Classifier (`AppClassifier.java`)**.

### The 3-Tier Classification Model

```
APP LAUNCH
    │
    ▼
Is package hardcoded?
   / \
 YES  NO
  │    │
  ▼    ▼
Known ALLOWED       UNKNOWN APP
  │                     │
  ▼                     ▼
ALLOW             LOCAL 5-LAYER CLASSIFIER
                        │
          ┌─────────────┼─────────────┐
          ▼             ▼             ▼
        SAFE         UNKNOWN       HOSTILE
          │             │             │
          ▼             ▼             ▼
        ALLOW         BLOCK         BLOCK
```

1. **Known Allowed Apps**: Chrome, Google Docs, ChatGPT, Claude, Keep, OpenCamera, Gboard, etc. $\to$ **ALLOW** immediately.
2. **Known Hostile Apps**: TikTok, YouTube, Roblox, Netflix, Instagram, GameGuardian, ReVanced, VMOS $\to$ **BLOCK** immediately.
3. **Unknown Apps**: Analyzed locally in $<5\text{ms}$ using Android's native `PackageManager` and `ApplicationInfo`. Zero network requests, preserving BYOK.

### 5-Layer Local Decision Hierarchy

| Layer | Component Evaluated | Decision / Criteria |
|---|---|---|
| **Layer 1: Permanent Exemptions** | Core Framework & Telephony | Always allows OS (`android`, `com.android.systemui`), dialers (`com.android.phone`, `com.google.android.dialer`, `com.android.incallui`), keyboards/IMEs, launchers, and user-configured whitelist. |
| **Layer 2: Tampering & Blacklist** | Settings & Hostile Signatures | Blocks `com.android.settings` (prevents disabling Accessibility) and all `BlacklistConstants` signatures. |
| **Layer 3: Declared Android OS Category** | `ApplicationInfo.category` | • `CATEGORY_GAME` (0) $\to$ **BLOCK**<br/>• `CATEGORY_SOCIAL` (4) $\to$ **BLOCK**<br/>• `CATEGORY_VIDEO` (2) $\to$ **BLOCK**<br/>• `CATEGORY_NEWS` (5) $\to$ **BLOCK**<br/>• `CATEGORY_ACCESSIBILITY` (8) $\to$ **ALLOW**<br/>• `CATEGORY_MAPS` (6) $\to$ **ALLOW**<br/>• `CATEGORY_AUDIO` (1) & `IMAGE` (3) $\to$ **ALLOW** (if clean of distraction signals)<br/>• `CATEGORY_PRODUCTIVITY` (7) $\to$ Verified against negative signals |
| **Layer 4a: Negative Distraction Filter** | App Label & Package Substrings | Overrides declared categories. If the title contains gaming, gambling, dating, modding, or comics (`game`, `casino`, `poker`, `betting`, `dating`, `cloner`, `parallel`, `manga`, `webtoon`), it is strictly **BLOCKED**. |
| **Layer 4b: Positive Academic Promotion** | Educational Keywords | Promotes unlisted `CATEGORY_UNDEFINED` apps to **ALLOW** if the title/package contains educational terms (`study`, `school`, `university`, `college`, `campus`, `academy`, `reader`, `pdf`, `dictionary`, `formula`, `calculator`, `desmos`, `geometry`, `science`, `flashcard`, `homework`, `portal`). |
| **Layer 5: Conservative Fallback** | Unclassified / Ambiguous Apps | Defaults to **BLOCK**. Unknown apps without positive academic signals are never granted a free pass. |

### UI Recognition: "Auto" Allowed Badge
Unlisted apps recognized by the local classifier (e.g. regional school portals like *"CEU Study Hub"*, campus portals, or unlisted scientific calculators) appear in the **Allowed Applications** section on both the **Dashboard** and the **LockScreen** with a dedicated green **`Auto`** badge, confirming to the student that the app is permitted.

---

## 🚨 Consequence Lockdown & Operating Hours (7:00 PM – 3:00 AM)

### Eliminating the "Wait-It-Out" Loophole
Previous app blockers have an obvious vulnerability: a student can simply wait out the countdown timer (25–40 minutes) without doing any homework, knowing the device will unlock when the clock reaches 0:00.

**In QIEZKA, waiting out the clock has severe consequences:**
- When the timer expires without submitting passing homework proof, **the device does not auto-unlock**.
- **Consequence Mode (`consequenceActive`)** activates natively in `SharedPreferences` and in React state.
- Distracting apps remain restricted, while essential study tools (notes, research, camera, messaging, QIEZKA) stay accessible.
- The restriction persists continuously until the student reschedules the failed homework and **passes AI evaluation**.

### Operating Hours Window (7:00 PM – 3:00 AM)
QIEZKA strictly respects human circadian needs and daytime academic schedules:
- **7:00 PM to 3:00 AM**: Primary homework and study hours. Consequence failure restrictions are actively enforced.
- **3:00 AM to 7:00 PM**: QIEZKA pauses restrictions so the student can sleep and use their phone normally for daytime school and commuting.
- **At 7:00 PM sharp**: Consequence failure restrictions **automatically resume** if the failed homework has not yet been rescheduled and passed.

### Strict Retake Flow
1. **Dashboard Banner**: When consequence mode is active, a persistent red alert banner appears with a single action: **"View Failed Homeworks"** (no bypass shortcuts).
2. **Failed Homeworks Hub**: Expanding the failed session displays the original scheduled window, allotted duration, and an alert card.
3. **Reschedule & Retake**: User clicks **"Add Again (Reschedule)"** $\to$ sets a new start time with automatic cascading $\to$ completes the session $\to$ submits answers $\to$ **passing AI evaluation completely clears Consequence Mode and restores full phone access**.

---

## ⚡ Operating Modes: Safemode vs. Hardcore (24/7)

QIEZKA provides two distinct operating modes configured in **Settings > General > Operating Mode**:

```
┌─────────────────────────────────────────────────────────────┐
│                       OPERATING MODES                       │
├──────────────────────────────┬──────────────────────────────┤
│ 🛡️ SAFEMODE (Default)        │ ⚡ HARDCORE (24/7)           │
├──────────────────────────────┼──────────────────────────────┤
│ • 7:00 PM – 3:00 AM window   │ • 24/7 round-the-clock       │
│ • Daytime pause (3 AM - 7 PM)│ • Lockdown anytime (24h)     │
│ • Strict 3:00 AM curfew cap  │ • No 3:00 AM curfew cap      │
│ • School & sleep protection  │ • Consequence persists 24/7  │
└──────────────────────────────┴──────────────────────────────┘
```

### 1. Safemode (7:00 PM – 3:00 AM) — *Default*
- **Circadian & Academic Balance**: Focus sessions and consequence restrictions operate strictly during evening and night study hours (7:00 PM – 3:00 AM).
- **Daytime Truce (3:00 AM – 7:00 PM)**: Consequence app blocking pauses automatically during daytime hours so students can attend classes, commute, access banking/work portals, and rest without obstruction.
- **Automated 3:00 AM Curfew**: Cascaded schedule calculations enforce a 3:00 AM curfew cap. Any schedule series or break intervals that would spill past 3:00 AM are flagged as invalid, preventing unhealthy late-night study cycles.
- **Smart Rescheduling**: When retaking a failed assignment during daytime, the scheduler defaults to a 7:00 PM start time.

### 2. Hardcore Mode (24/7 Round-the-Clock)
- **Uncompromising Discipline**: Designed for intense study marathons, weekends, bar review, or users needing strict accountability all day long.
- **Anytime Scheduling**: Sessions can be scheduled and locked down at **any hour of the day or night (00:00 – 23:59)**.
- **No Curfew Cap**: Cascade schedules have no 3:00 AM cutoff; sessions can ripple across the morning and afternoon.
- **24/7 Consequence Enforcement**: If a session expires without passing submission, distracting apps remain blocked **continuously 24/7** without any daytime truce at 3:00 AM.
- **Native OS Persistence**: Capacitor bridge passes `operating_mode: "hardcore"` directly into Android `SharedPreferences`. `LockAccessibilityService` and `FloatingOverlayService` continuously intercept and block hostile packages 24 hours a day, 7 days a week.

### 🛡️ Safety Modal & Daytime Schedule Anti-Cheat Guard

To eliminate cheat vectors and prevent impulsive lockouts:

1. **High-Contrast Warning Modal**: Switching from Safemode to Hardcore triggers an explicit confirmation dialog warning that consequence restrictions will persist round-the-clock without daytime relief.
2. **Active Lockdown Guard**: Operating mode **cannot be changed** while a lockdown session is active or while Consequence Mode is active (`isLockedOrConsequence`).
3. **Daytime Schedule Downgrade Guard**: A user **cannot switch from Hardcore back to Safemode if an active schedule exists during daytime hours (03:01 AM – 06:59 PM)**. 
   - *Anti-Cheat Rationale*: Prevents a user from scheduling a daytime lock session in Hardcore mode, realizing they want to play games during the day, and downgrading to Safemode to escape the restriction.
   - *Requirement*: The user must complete or delete all daytime schedules first before Safemode can be restored.

---

## 🌐 Web & Browser Protection (Accessibility Guard & Local DNS Sinkhole)

While QIEZKA strictly blocks distracting apps (TikTok, Instagram, Reddit, Twitter, Netflix, etc.), students facing high academic friction often attempt to bypass restrictions using **mobile Chrome or alternative web browsers**. 

To close this backdoor without breaking legitimate web research (e.g. Wikipedia, Google Docs, AI assistants, or university portals), QIEZKA provides a multi-layer web defense system configured in **Settings > General > Web & Browser Protection**:

```
┌─────────────────────────────────────────────────────────────────────────┐
│                      WEB PROTECTION ARCHITECTURE                        │
├────────────────────────────────────┬────────────────────────────────────┤
│ 🛡️ ACCESSIBILITY URL GUARD         │ ⚡ LOCAL DNS SINKHOLE (VPN)        │
├────────────────────────────────────┼────────────────────────────────────┤
│ • Real-time browser URL inspection │ • On-device UDP port 53 loopback   │
│ • Immediate visual eviction (Back) │ • Sinkholes domains to 0.0.0.0     │
│ • Leaves VPN slot FREE             │ • Blocks all browsers & WebViews   │
│ • Zero battery / latency overhead  │ • Zero proxy delay for HTTPS       │
└────────────────────────────────────┴────────────────────────────────────┘
```

### 1. Protection Modes

| Mode | Technology | Best For | VPN Slot Used? |
|---|---|---|:---:|
| **🛡️ Accessibility URL Guard** *(Default)* | Inspects the address bar in Chrome, Samsung Internet, Firefox, Edge, and Brave using `LockAccessibilityService`. If a blacklisted domain is navigated to, QIEZKA triggers `GLOBAL_ACTION_BACK` and flashes an informative block toast. | Students who need external VPNs (university campus VPN, WireGuard, Tailscale, Cloudflare WARP) for research. | **No** (Slot free) |
| **⚡ DNS Sinkhole (Local VPN)** | Runs an on-device `LocalDnsVpnService` that routes virtual DNS queries (`10.111.222.1/32`). Blacklisted domains resolve to `0.0.0.0`, while legitimate research queries are relayed to upstream DNS (`1.1.1.1` / `8.8.8.8`) via protected sockets. | Maximum packet-level blocking across all obscure browsers, private tabs, and in-app WebViews. | **Yes** (1 VPN slot) |
| **🔒 Dual-Layer Hybrid** | Runs both Accessibility URL Guard and DNS Sinkhole concurrently for zero-tolerance discipline. | High-stakes exam periods, bar exams, and extreme focus sprints. | **Yes** (1 VPN slot) |
| **⚪ Off (Unrestricted)** | Web filtering is disabled; allowed browsers can navigate to any URL. | Users with high self-discipline who only want app blocking. | **No** |

### 2. YouTube Policy (Default Blocked & Academic Web Only)

To prevent procrastination loops while allowing legitimate lecture research:

1. **Native YouTube App Permanently Blocked**: 
   - The native YouTube application (`com.google.android.youtube`, YouTube Kids, YouTube Studio, ReVanced, NewPipe, etc.) is **strictly hardcoded in the distraction blacklist and can NEVER be launched or whitelisted**.
   - The only supported route to YouTube is through an allowed web browser.

2. **Blocked by Default**:
   - By default, YouTube web domains (`youtube.com`, `youtu.be`, `googlevideo.com`) are **completely blocked** both in the Accessibility URL Guard and the Local DNS Sinkhole.

3. **"Allow YouTube (Academic Only)" Option**:
   - In **Settings > General > Web & Browser Protection**, users can toggle **"Allow YouTube (Academic Only)"** (disabled by default).
   - **When Enabled**:
     - Long-form educational videos, documentaries, and academic searches on `youtube.com` are permitted in allowed browsers.
     - **YouTube Shorts (`youtube.com/shorts/*` or `#shorts`) are strictly blocked** in real-time, instantly issuing a `GLOBAL_ACTION_BACK` and displaying an alert toast.
     - The native YouTube app remains 100% blocked.

### 3. Web-Based Game Armor (250+ Domains, Dynamic Heuristics & Search Game Blocking)

When native games (such as *Coxeta*, *Genshin Impact*, or *Roblox*) are blocked by QIEZKA's hierarchical classifier, students frequently turn to allowed web browsers (Google Chrome, Samsung Internet, Firefox, Brave, Microsoft Edge) to play browser-based games. 

To eliminate this evasion vector without breaking legitimate academic research, QIEZKA implements an uncompromising **Web-Based Game Armor Protocol**:

#### A. Comprehensive 250+ Gaming Domain Database
Integrated into both the **Local DNS Sinkhole (`LocalDnsVpnService`)** and the **Real-Time Accessibility URL Guard (`LockAccessibilityService`)**:
- **Mega Portals & Hubs (60+ Domains)**: Poki (`poki.com`), CrazyGames (`crazygames.com`), Coolmath Games (`coolmathgames.com`), Kongregate, Armor Games, Newgrounds, Y8, Friv, Miniplay, Silvergames, Kizi, Agame, Snokido, Playhop, 1001 Games, etc.
- **Viral .IO Multiplayer Arena Games (50+ Domains)**: Slither.io, Agar.io, Diep.io, Krunker.io, 1v1.lol, Paper.io, Hole.io, Surviv.io, Skribbl.io, Gartic, Shell Shockers, Bloxd.io, Voxiom, Ev.io, Zombs, Bonk.io, Narrow.one, etc.
- **Cloud Gaming & Web APK Streaming Backdoors (15+ Domains)**: `now.gg` (the primary platform used by students to stream native Android APKs like Roblox and Coxeta inside web browsers), GeForce NOW, Xbox Cloud Gaming, Amazon Luna, Boosteroid.
- **Indie Web Runtimes & CDNs (15+ Domains)**: `*.itch.zone` (sandboxed iframe runtime hosting all 100,000+ HTML5 games on itch.io), GameJolt, Opera GX (`gx.games`), Simmer.io, Pico-8 (`lexaloffle.com`).
- **Web Emulators & Retro Gaming (25+ Domains)**: EmulatorOnline, RetroGames.cc, PlayRetroGames, Vimm's Lair, EmuPedia, Afterplay.io, EclipseEmu, PlayClassic.games, DosGames.
- **Dedicated School "Unblocked Games" Networks (40+ Domains)**: `unblocked-games.com`, `unblockedgames66/76/77/99/500/24h`, `classroom6x.com`, `slope-game.com`, `hoodamath.com`, `mathplayground.com`, `abcya.com`, etc.
- **Casual, Board & Puzzle Sites (30+ Domains)**: Chess.com, Lichess, GeoGuessr, 2048, Sudoku.com, Wordle clones, Tetr.io, Cookie Clicker.

#### B. Dynamic Heuristic Defense Engine
1. **Browser Internal Game Schemes**:
   - Blocks `chrome://dino` and `chrome://network-error/-106` (Chrome Dinosaur runner)
   - Blocks `edge://surf` and `opera://game`
   - *Action*: Automatically triggers `GLOBAL_ACTION_BACK` with alert toast: *"⚠️ Browser mini-games are blocked during study sessions"*.
2. **Google & Bing Search-Embedded Canvas Games**:
   - Detects interactive HTML5 canvas game triggers inside search result queries:
     `snake`, `play snake`, `tic tac toe`, `pacman`, `minesweeper`, `solitaire`, `atari breakout`, `dreidel`, `fidget spinner`, `earth day quiz`, `memory game`, `play game`
   - *Action*: Backs out of the search result with alert toast: *"⚠️ Search-embedded mini-games are blocked during focus mode"*.
3. **Unblocked Game Mirror & Proxy Heuristics**:
   - Detects unblocked game mirror signatures in URL host and path:
     - Host tokens: `unblocked`, `classroom6x`, `slope-game`, `slopegame`, `retrobowl`, `moto-x3m`
     - Hosting mirrors: `sites.google.com/view/*unblocked*`, `*.github.io/*slope*`, `*.github.io/*unblocked*`
4. **Sub-Path Game Blocking on Portal & News Sites**:
   - Blocks `/games/`, `/crosswords/`, `/puzzles/` on general news portals (`nytimes.com/games`, `washingtonpost.com/crossword`, `theguardian.com/crosswords`, `zone.msn.com`, `yandex.com/games`) while leaving legitimate news and research articles fully accessible.

#### C. Ironclad Academic Safe-List Immunity
Legitimate educational and documentation resources are explicitly exempted from keyword heuristics so research is never hindered:
- **Academic Platforms**: `wikipedia.org`, `wikimedia.org`, all university `.edu` domains (Stanford, MIT, Harvard, Berkeley, etc.), `khanacademy.org`, `coursera.org`, `edx.org`, `udemy.com`, `quizlet.com`, `brainly.com`, `chegg.com`, `duolingo.com`
- **Developer Documentation**: `developer.mozilla.org` (MDN), `github.com` (source repositories), `stackoverflow.com`, `stackexchange.com`, `docs.unity3d.com`, `docs.godotengine.org`, `unrealengine.com`
- **Scientific Journals**: `arxiv.org`, `researchgate.net`, `jstor.org`, `nature.com`, `sciencedirect.com`

#### D. Settings Toggle
In **Settings > General > Web & Browser Protection**, students can toggle **"Block Web-Based Games"** (enabled by default). Locked during active lockdown and consequence mode.

### 4. Multi-Genre On-Device Web Classification Engine (`WebClassifier`)

Instead of relying on third-party commercial DNS providers, QIEZKA features **`WebClassifier`**—an on-device semantic and heuristic web intelligence engine that operates as the unified decision brain across both **Accessibility Guard** (real-time DOM & browser address bar) and **DNS Sinkhole** (socket-level packet sinkhole).

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                        ON-DEVICE WEB CLASSIFIER ARCHITECTURE                           │
├──────────────────────────┬─────────────────────────────┬───────────────────────────────┤
│ Layer 1: Search Gate     │ Layer 2: Threat Categories  │ Layer 3: Dual Enforcement     │
├──────────────────────────┼─────────────────────────────┼───────────────────────────────┤
│ Typing in address bar &  │ 10 major distraction genres │ Accessibility Guard (0 VPN)   │
│ search results (Google,  │ evaluated on-device with zero│ OR DNS Sinkhole (0.0.0.0)    │
│ Bing) are 100% immune.   │ external latency overhead.  │ OR Dual-Layer Hybrid Armor.   │
└──────────────────────────┴─────────────────────────────┴───────────────────────────────┘
```

The engine classifies websites across 10 distinct digital distraction categories:
1. **Web Games & Cloud Gaming**: 300+ game portals, .io arenas, unblocked mirrors, cloud APKs (`now.gg`).
2. **Online Gambling & Sportsbooks**: Casinos, sports betting, crypto slots, skin/case gambling.
3. **Adult, Explicit & NSFW**: Adult streaming, cams, adult subscriptions, erotica.
4. **Web Proxies & Unblockers**: CGI/PHP/Node web proxies and firewall bypass tunnels.
5. **Piracy Streaming & Manga**: Free movie/anime streaming, manga/webtoons, vertical short-dramas.
6. **Social Media Feeds**: TikTok, Instagram, Twitter/X, Reddit, Threads web interfaces.
7. **Dating & Random Cam Chat**: Dating platforms and random video chat services.
8. **Time-Wasters & Gossip**: Viral clickbait and gossip portals.
9. **Crypto Meme Coin Speculation**: Speculative DEX and meme coin casino portals.
10. **Suspicious gTLDs**: Direct blocking for `.casino`, `.bet`, `.poker`, `.adult`, `.porn`, `.xxx`, `.sex`, `.cam`.

### 5. Academic Safe-List Immunity
Critical educational platforms enjoy 100% hardcoded immunity with zero false positives:
- Google Classroom, Docs, Drive, Sheets, Slides, Scholar
- Wikipedia, Wikimedia, Khan Academy, Coursera, edX, Udemy
- Desmos, GeoGebra, WolframAlpha, Overleaf
- Quizlet, Brainly, Chegg, Duolingo, MDN, GitHub, StackOverflow
- All educational institutions ending in `.edu`, `.edu.*`, `.ac.uk`, and `.gov`

### 6. Anti-Cheat Lockout Guard
Just like Operating Mode settings, **Web & Browser Protection modes and settings cannot be changed or disabled during an active lockdown session or during Consequence Mode**. The student must complete and pass their homework submission first.

---

## 📰 Case Studies & Article Declutter System

Assignments requiring students to evaluate real-world issues (news clips, documentary transcripts, journal articles, or court cases) using foundational course concepts are supported through a specialized architecture:

### 1. Dedicated `articleDeclutter` Parser
Standard study decluttering compresses text into flashcard pairs (`[Trigger] | [Variable]`). Feeding news articles or legal briefs into a flashcard prompt destroys narrative continuity.
- **`articleDeclutter`** acts as a diagnostic research analyst, extracting:
  1. *Artifact & Source* (Title, author, publication, date).
  2. *Core Issue & Background* (Central operational failure or controversy).
  3. *Key Parties & Actors* (Entities, systems, individuals involved).
  4. *Timeline & Chronology* (Factual sequence of events).
  5. *Technical & Ethical Dilemmas* (Specific engineering or procedural dilemmas).
  6. *Outcomes & Impact* (Consequences, regulatory penalties, societal fallout).
  7. *Empirical Data & Quotes* (Exact measurements and key citations).

### 2. Step 2 Case Study Gating
When creating a schedule, if the AI detects the prompt is an external case study:
- Displays a dedicated **Case Study / Article Selection** section.
- Lets the user pick from their saved Case Studies library or click **"+ Add Case Study / Article"** to paste and clean up a document on the spot.
- **Gated Progression**: The "Next" button is disabled until an article is selected or General AI Knowledge is authorized.

---

## 🔮 Floating Assistive Timer Ball

When a lockdown session is initiated, QIEZKA summons a **Floating Assistive Ball Timer** that stays with you across all permitted apps:

```
          ┌────────────────────────────────────────────────────────┐
          │  Allowed Application (Google Docs / Keep / Gemini)     │
          │                                                        │
          │                                                        │
          │                   ┌──────────────┐                     │
          │                   │  ⏱️ 24:18    │ ◀─── Assistive Ball │
          │                   └──────────────┘                     │
          │                                                        │
          │  • Floats above all permitted applications             │
          │  • Drag anywhere & smoothly snaps to nearest edge      │
          │  • Real-time countdown synced with native bridge       │
          │  • Single tap instantly returns to QIEZKA submission   │
          └────────────────────────────────────────────────────────┘
```

### Engineering Specifications of the Floating Ball
* **System-Wide Overlay**: Rendered via native Android `WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY` with `FLAG_NOT_FOCUSABLE`. It never steals keyboard focus when you are typing essays in Google Docs or prompts in Gemini.
* **Physics-Based Edge Snapping**: You can drag the ball anywhere across the screen. On touch release (`ACTION_UP`), the service calculates the screen width and uses a smooth `ValueAnimator` with cubic deceleration to glide and snap the ball against the nearest left or right border.
* **Flicker-Free Debounce**: Digits are updated using a dedicated equality check (`if (!newText.equals(tvTimer.getText().toString()))`) before calling `setText()`. Rapid window switches or app taps never cause digit flashing or erratic frame stutter.
* **Foreground Service Persistence**: Supported by Android's `FOREGROUND_SERVICE_TYPE_SPECIAL_USE` daemon. Even under aggressive OEM memory pressure on Android 14, 15, and 16, the system will not kill the overlay timer.
* **One-Tap Quick Return**: Tapping the floating ball automatically fires a high-priority `Intent` with `FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_REORDER_TO_FRONT` to summon QIEZKA's homework submission screen in milliseconds.

---

## 📋 Complete Application Reference (Blocked & Allowed)

QIEZKA utilizes dual-layer enforcement: **Exact Package ID Matching** and **Dynamic Substring / Signature Heuristics**. Below is the complete catalog of all hardcoded applications recognized by the frontend UI and the native Android Accessibility engine ([`blacklistedApps.ts`](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/src/constants/blacklistedApps.ts), [`BlacklistConstants.java`](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/android/app/src/main/java/com/uncode/app/BlacklistConstants.java), and [`LockPlugin.java`](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/android/app/src/main/java/com/uncode/app/LockPlugin.java)).

---

### 🟢 Hardcoded Allowed Applications ("ALWAYS" Accessible)

These applications are permanently whitelisted during lockdown. They appear in the app drawer and are never routed Home or blocked.

<details open>
<summary><b>🤖 1. AI Study Assistants (11 Packages)</b></summary>

| Application | Android Package ID | Academic Role |
|---|---|---|
| **Google Gemini** | `com.google.android.apps.bard` | Native Google multimodal AI assistant |
| **Google Gemini (Direct)** | `com.google.android.apps.gemini` | Standalone Google Gemini package |
| **Google Assistant / Gemini** | `com.google.android.apps.googleassistant` | Android voice & assistant trigger |
| **ChatGPT** | `com.openai.chatgpt` | OpenAI homework & concept tutor |
| **Claude** | `com.anthropic.claude` | Anthropic reasoning & writing tutor |
| **Microsoft Copilot** | `com.microsoft.copilot` | Microsoft GPT-4 & Bing search assistant |
| **Perplexity AI** | `ai.perplexity.app.android` | Citation-backed research engine |
| **DeepSeek** | `com.deepseek.chat` | DeepSeek AI assistant & math reasoning |
| **Poe by Quora** | `com.poe.android`, `com.quora.poe.android` | Multi-bot AI ecosystem by Quora |
| **Pi AI** | `ai.inflection.pi` | Conversational study coach |

</details>

<details open>
<summary><b>📂 2. System Document Pickers & Media Providers (Hidden Exemptions - 13 Packages)</b></summary>

*These packages are internal OS file providers. They are hardcoded as completely exempt so homework photo uploads and document attachments never get blocked, but are hidden from the user-facing Allowed Apps UI:*

| Component / Provider | Package ID | Role |
|---|---|---|
| **Google DocumentsUI** | `com.google.android.documentsui` | Stock Android file and storage picker |
| **AOSP DocumentsUI** | `com.android.documentsui` | Core Android system document provider |
| **Google Media Provider** | `com.google.android.providers.media.module` | Internal Android photo picker provider |
| **AOSP Media Provider** | `com.android.providers.media` | System media indexing & SAF provider |
| **Samsung My Files** | `com.sec.android.app.myfiles` | Samsung OneUI file manager |
| **Files by Google** | `com.google.android.apps.nbu.files` | Google official file explorer |
| **Xiaomi File Explorer** | `com.mi.android.globalFileexplorer` | MIUI / HyperOS file selector |
| **Oppo File Manager** | `com.coloros.filemanager` | ColorOS file manager |
| **OnePlus File Manager** | `com.oneplus.filemanager` | OxygenOS file selector |
| **Huawei Files** | `com.huawei.filemanager` | EMUI / HarmonyOS file browser |
| **Vivo File Manager** | `com.vivo.FileManager` | FuntouchOS file selector |
| **Motorola Files** | `com.motorola.filemanager` | Moto file manager |
| **Asus File Manager** | `com.asus.filemanager` | ZenUI storage browser |

</details>

<details open>
<summary><b>📄 3. Document Scanners & PDF Worksheets (8 Packages)</b></summary>

| Application | Android Package ID | Academic Role |
|---|---|---|
| **Adobe Acrobat Reader** | `com.adobe.reader` | PDF viewing, annotations, and worksheets |
| **Adobe Scan** | `com.adobe.scan.android` | Camera document scanning with OCR |
| **CamScanner** | `com.intsig.camscanner` | Document capture & homework PDF export |
| **WPS Office** | `cn.wps.moffice_eng` | Complete office suite (Word, Excel, PPT, PDF) |
| **Microsoft Lens** | `com.microsoft.office.officelens` | Whiteboard & document photo scanning |
| **ReadEra** | `org.readera` | Offline PDF, EPUB, and textbook reader |
| **Xodo PDF Reader** | `com.xodo.pdf.reader` | PDF worksheet form-filling & drawing |
| **Foxit PDF Reader** | `com.foxit.mobile.pdf.lite` | Lightweight PDF reading and annotating |

</details>

<details open>
<summary><b>🌐 4. Translation & Language Dictionaries (6 Packages)</b></summary>

| Application | Android Package ID | Academic Role |
|---|---|---|
| **Google Translate** | `com.google.android.apps.translate` | Multilingual text, speech, and camera translation |
| **DeepL Translate** | `com.deepl.mobiletranslator` | High-accuracy neural translation |
| **Duolingo** | `com.duolingo` | Foreign language vocabulary & grammar practice |
| **Merriam-Webster Dictionary** | `com.merriamwebster` | English vocabulary, definitions, and thesaurus |
| **Oxford Dictionary of English** | `com.mobisystems.msdict.embedded.wireless.oxford.dictionaryofenglish` | Academic English reference |
| **Cambridge Dictionary** | `org.cambridge.cclae` | Learner vocabulary and collocations |

</details>

<details open>
<summary><b>📝 5. Note-Taking & Brainstorming (15 Packages)</b></summary>

| Application | Android Package ID | Academic Role |
|---|---|---|
| **Google Keep** | `com.google.android.keep` | Quick notes, lists, and audio memos |
| **Samsung Notes** | `com.samsung.android.app.notes` | Handwritten stylus notes and lecture scribbles |
| **Microsoft OneNote** | `com.microsoft.office.onenote` | Multi-subject binder & notebook organizer |
| **Notion** | `notion.id` | Project planning, lecture databases, and wikis |
| **Obsidian** | `md.obsidian` | Markdown knowledge base & networked thought |
| **Evernote** | `com.evernote` | Cross-platform note archive |
| **Simplenote** | `com.automattic.simplenote` | Clean, distraction-free markdown notes |
| **ColorNote** | `com.socialnmobile.dictapps.notepad.color.note` | Simple sticky notes and checklists |
| **Zoho Notebook** | `com.zoho.notebook` | Visual card-based notebook system |
| **Squid** | `com.steadfastinnovation.android.pencilbook` | Handwritten PDF markup & vector paper |
| **Goodnotes** | `com.goodnotes.goodnotes` | Digital notebook and handwriting suite |
| **Nebo** | `com.myscript.nebo` | Handwriting-to-digital-text conversion |
| **UpNote** | `com.upnote.app` | Elegant rich-text note editor |
| **Standard Notes** | `com.standardnotes` | Encrypted, future-proof notes |
| **Noteshelf** | `com.fluidtouch.noteshelf3` | Digital planner & handwriting |

</details>

<details open>
<summary><b>🎓 6. Student Platforms, LMS & Office Suites (21 Packages)</b></summary>

| Application | Android Package ID | Academic Role |
|---|---|---|
| **Google Classroom** | `com.google.android.apps.classroom` | School assignments, announcements, and submissions |
| **Google Drive** | `com.google.android.apps.docs` | Cloud file management and lecture slides |
| **Google Docs** | `com.google.android.apps.docs.editors.docs` | Word processing and essay drafting |
| **Google Sheets** | `com.google.android.apps.docs.editors.sheets` | Spreadsheets, data analysis, and graphing |
| **Google Slides** | `com.google.android.apps.docs.editors.slides` | Presentation creation and lecture decks |
| **Google PDF Viewer** | `com.google.android.apps.pdfviewer` | Direct PDF viewing utility |
| **Canvas Student** | `com.instructure.cstudent` | University/school LMS portal and quiz hub |
| **Canvas Teacher** | `com.instructure.cancan` | Instructor grading and module inspection |
| **Blackboard Learn** | `com.blackboard.android.bbstudent` | University LMS portal & course syllabus |
| **Schoology** | `com.schoology.app` | K-12 and collegiate learning management |
| **Quizlet** | `com.quizlet.quizletandroid` | Flashcards, study sets, and memorization |
| **AnkiDroid** | `com.ichi2.anki` | Spaced repetition flashcard engine |
| **Brainly** | `co.brainly` | Peer-to-peer homework question & answer |
| **Photomath** | `com.photomath.android` | Step-by-step math solver with camera scanning |
| **Desmos Graphing Calculator** | `com.desmos.calculator` | Interactive graphing and Cartesian curves |
| **GeoGebra** | `org.geogebra.android` | Geometry, 3D graphing, and calculus models |
| **WolframAlpha** | `com.wolfram.android.alpha` | Computational knowledge & symbolic math engine |
| **Microsoft 365 (Office)** | `com.microsoft.office.officehubrow` | Integrated Word, Excel, and PowerPoint suite |
| **Microsoft Word** | `com.microsoft.office.word` | Academic essay and report editing |
| **Microsoft Excel** | `com.microsoft.office.excel` | Advanced formulas, tables, and statistics |
| **Microsoft PowerPoint** | `com.microsoft.office.powerpoint` | Slide deck review and presentation prep |

</details>

<details open>
<summary><b>🧮 7. STEM Solvers & Learning Hubs (7 Packages)</b></summary>

| Application | Android Package ID | Academic Role |
|---|---|---|
| **Khan Academy** | `org.khanacademy.android` | Free comprehensive courses in math, science, history |
| **Symbolab** | `com.devsense.symbolab` | Step-by-step calculus, algebra, and matrix solver |
| **Mathway** | `com.bagatrix.mathway.android` | Instant math problem step-by-step solver |
| **Chegg Study** | `com.chegg` | Textbook solutions and expert Q&A |
| **Brilliant** | `org.brilliant.android` | Interactive STEM, logic, and CS problem solving |
| **Periodic Table 2024** | `mendeleev.redlime` | Chemistry element data, isotopes, and properties |
| **Cymath** | `com.cymath.cymath` | Math problem solver with step derivations |

</details>

<details open>
<summary><b>☁️ 8. Cloud Storage & Sync (3 Packages)</b></summary>

| Application | Android Package ID | Academic Role |
|---|---|---|
| **Microsoft OneDrive** | `com.microsoft.skydrive` | University 365 cloud file storage |
| **Dropbox** | `com.dropbox.android` | Cloud file sharing and document archive |
| **Box** | `net.box.android` | Enterprise and collegiate cloud storage |

</details>

<details open>
<summary><b>💻 9. CS & Coding Environments (4 Packages)</b></summary>

| Application | Android Package ID | Academic Role |
|---|---|---|
| **Termux** | `com.termux` | Full-fledged Linux terminal emulator and package manager |
| **Acode** | `com.foxdebug.acode` | Powerful mobile code editor for web, JS, and Python |
| **Pydroid 3** | `ru.iiec.pydroid3` | Offline educational Python 3 IDE with pip |
| **GitHub** | `com.github.android` | Code repository review, issues, and pull requests |

</details>

<details open>
<summary><b>🛡️ 10. 2FA Authenticators (10 Packages)</b></summary>

| Application | Android Package ID | Security Role |
|---|---|---|
| **Google Authenticator** | `com.google.android.apps.authenticator2` | 2FA OTP codes for Google and academic portals |
| **Microsoft Authenticator** | `com.azure.authenticator` | University Single Sign-On (SSO) and 2FA |
| **Duo Mobile** | `com.duosecurity.duomobile` | Campus two-factor authentication push prompts |
| **Twilio Authy** | `com.authy.authy` | Multi-device cloud 2FA tokens |
| **2FAS Authenticator** | `com.twofasapp` | Open-source privacy-focused 2FA |
| **Aegis Authenticator** | `com.beemdevelopment.aegis` | Secure, open-source Android 2FA client |
| **Bitwarden Authenticator** | `com.bitwarden.authenticator` | Integrated password & TOTP generator |
| **LastPass Authenticator** | `com.lastpass.authenticator` | Two-factor verification tool |
| **FreeOTP** | `org.fedorahosted.freeotp` | Red Hat open-source 2FA |
| **Yubico Authenticator** | `com.yubico.yubioath` | Hardware security key companion |

</details>

<details open>
<summary><b>🌐 11. Web Browsers (8 Packages)</b></summary>

| Application | Android Package ID | Function |
|---|---|---|
| **Google Chrome** | `com.android.chrome`, `com.google.chrome` | General web research & portal access |
| **Mozilla Firefox** | `org.mozilla.firefox` | Independent web browser |
| **Samsung Internet** | `com.sec.android.app.sbrowser` | Optimized Samsung browser |
| **Microsoft Edge** | `com.microsoft.emmx` | Web research & Copilot sync |
| **Opera Browser** | `com.opera.browser` | Web browser |
| **Opera Mini** | `com.opera.mini.native` | Data-saving web browser |
| **Brave Browser** | `com.brave.browser` | Privacy-centric web browser |
| **DuckDuckGo** | `com.duckduckgo.mobile.android` | Tracker-free search browser |

</details>

<details open>
<summary><b>🎵 12. Deep Focus Music & Audio (9 Packages)</b></summary>

| Application | Android Package ID | Function |
|---|---|---|
| **Spotify** | `com.spotify.music` | Lo-Fi study beats, white noise, and focus playlists |
| **YouTube Music** | `com.google.android.apps.youtube.music` | Background study music streaming |
| **Apple Music** | `com.apple.android.music` | High-fidelity focus audio streaming |
| **Amazon Music** | `com.amazon.mp3` | Audio streaming |
| **Tidal** | `com.aspiro.tidal` | Lossless focus audio |
| **Deezer** | `deezer.android.app` | Music & podcast streaming |
| **SoundCloud** | `com.soundcloud.android` | Independent artist tracks and study mixes |
| **Samsung Music** | `com.sec.android.app.music` | Offline local audio player |
| **MIUI Music Player** | `com.miui.player` | Offline Xiaomi audio player |

</details>

<details open>
<summary><b>📸 13. System Camera Applications (12 Packages)</b></summary>

| Vendor / ROM | Android Package ID |
|---|---|
| **AOSP / Generic Camera** | `com.android.camera`, `com.android.camera2` |
| **Google Pixel Camera** | `com.google.android.GoogleCamera` |
| **Samsung Camera** | `com.sec.android.app.camera`, `com.samsung.android.camera` |
| **Huawei Camera** | `com.huawei.camera` |
| **Oppo / Realme Camera** | `com.oppo.camera` |
| **OnePlus Camera** | `com.oneplus.camera` |
| **Xiaomi / Redmi / POCO Camera** | `com.xiaomi.camera` |
| **Motorola Camera** | `com.motorola.camera`, `com.motorola.camera2` |
| **Sony Xperia Camera** | `com.sonyericsson.android.camera` |
| **LineageOS Snap** | `org.lineageos.snap` |

</details>

<details>
<summary><b>⚙️ 14. System Keyboards & IMEs (Under-the-Hood Exemptions - 18 Packages)</b></summary>

*These packages are native input-method infrastructure. They are automatically permitted so students can type in search boxes and notes, but are hidden from the UI app drawer:*

`com.google.android.inputmethod.latin` (Gboard), `com.samsung.android.honeyboard` (Samsung Keyboard), `com.touchtype.swiftkey` & `com.touchtype.swiftkey.beta` (SwiftKey), `com.android.inputmethod.latin` (AOSP), `com.syntellia.fleksy.keyboard` (Fleksy), `org.dslul.openboard.inputmethod.latin` (OpenBoard), `com.menny.android.anysoftkeyboard` (AnySoftKeyboard), `com.grammarly.android.keyboard` (Grammarly), `com.oppo.keyboard`, `com.coloros.keyboard`, `com.vivo.keyboard`, `com.huawei.ohos.inputmethod`, `com.sohu.inputmethod.sogou`, `com.baidu.input`, `com.google.android.tts`.

</details>

---

### 🔴 Hardcoded Blacklisted Applications (Strictly Blocked)

These applications represent high-dopamine distractions, bypass vectors, or cracking utilities. Any attempt to open them immediately triggers the native accessibility service to route the user Home in milliseconds.

<details open>
<summary><b>🍿 1. Asian Dramas, Anime & Streaming (26 Packages)</b></summary>

| Application | Android Package ID | Category |
|---|---|---|
| **KissKH TWA** | `id.kisskh.twa` | Pirated drama streaming PWA/TWA wrapper |
| **KissKH App** | `com.kisskh.app` | Asian drama streaming client |
| **KissAsian** | `com.kissasian.app` | Asian drama streaming portal |
| **Bilibili (Mainland China)** | `tv.danmaku.bili` | Video streaming & Danmaku community |
| **Bilibili Global / SEA** | `com.bstar.intl` | International Bilibili anime platform |
| **Bilibili India** | `com.bilibili.app.in` | Bilibili regional client |
| **Bilibili HD** | `tv.danmaku.bilibilihd` | Bilibili tablet edition |
| **Bilibili Comics** | `com.bilibili.comic`, `com.bilibili.comic.intl` | Manga & comic reader |
| **Bilibili Studio** | `com.bilibili.studio` | Content creator studio |
| **iQIYI / iQIYI International** | `com.iqiyi`, `com.iqiyi.i18n`, `com.qiyi.video` | Chinese drama & variety shows |
| **WeTV / Tencent Video** | `com.tencent.qqlivei18n`, `com.tencent.qqlive` | Asian drama streaming |
| **Viu** | `com.vuclip.viu` | Korean & Asian drama streaming |
| **Viki (Rakuten)** | `com.viki.android` | Asian drama streaming & community subs |
| **Youku / Youku International** | `com.youku.phone`, `com.youku.international` | Chinese video streaming |
| **Mango TV / MGTV** | `com.hunantv.imgo.activity`, `com.mgtv.tv` | Variety & entertainment streaming |
| **Crunchyroll** | `com.crunchyroll.crunchyroid` | Anime streaming service |
| **Funimation** | `com.funimation.funimationnow` | Anime streaming platform |
| **HIDIVE** | `com.hidive.android` | Anime streaming service |
| **RetroCrush** | `tv.cinedigm.retrocrush` | Classic retro anime streaming |

</details>

<details open>
<summary><b>🏴‍☠️ 2. Unofficial / Piracy / Third-Party Streaming (10 Packages)</b></summary>

| Application | Android Package ID | Threat Type |
|---|---|---|
| **Loklok** | `com.artem.scotepio`, `com.darmiu.folasia`, `com.tarparos.phigaea` | Pirated movie & drama aggregator |
| **CloudStream 3** | `com.lagradost.cloudstream3` | Open-source modular piracy streaming client |
| **MovieBoxPro** | `com.movieboxpro.android` | Third-party movie & series streaming |
| **Anilab** | `com.anilab.android`, `com.anilab.animtvappr` | Unofficial anime streaming app |
| **Stremio** | `com.stremio.one` | Torrent-based media streaming aggregator |
| **OnStream** | `com.onstream.app` | Third-party film & TV streaming client |
| **Popcorn Time** | `com.popcorntime` | Peer-to-peer torrent video player |

</details>

<details open>
<summary><b>📖 3. Webtoons, Manga & Light Novels (10 Packages)</b></summary>

| Application | Android Package ID | Description |
|---|---|---|
| **Line Webtoon** | `com.naver.linewebtoon` | Infinite-scroll Korean comic reader |
| **Tapas** | `com.tapastic` | Web comics and novel app |
| **KakaoPage / Kakao Webtoon** | `com.kakao.page`, `com.kakaowebtoon.app` | Webtoon reading portal |
| **Manga Plus by Shueisha** | `jp.co.shueisha.mangaplus` | Official Shonen Jump manga reader |
| **Tappytoon** | `com.contentsfirst.tappytoon` | Comics and light novel reader |
| **Tachiyomi** | `eu.kanade.tachiyomi` | Modular open-source manga reader |
| **Mihon** | `app.mihon` | Modern successor to Tachiyomi |
| **Aniyomi** | `eu.kanade.tachiyomi.anime`, `app.aniyomi` | Manga & anime fork of Tachiyomi |

</details>

<details open>
<summary><b>🎬 4. Micro-Drama & Vertical Short Series (6 Packages)</b></summary>

| Application | Android Package ID | Description |
|---|---|---|
| **ReelShort** | `com.newleaf.app.android.victor` | 1-minute episodic addictive drama reels |
| **DramaBox** | `com.storymatrix.drama` | Micro-drama vertical streaming |
| **ShortMax** | `live.shorttv.apps` | Short TV series and dramatic reels |
| **GoodShort** | `com.goodreels.app` | Micro-episodes & romance video drama |
| **MoboReels** | `com.chao.novel.moboreels` | Vertical mini-drama player |
| **TopShort** | `com.topshort.video` | Short drama entertainment |

</details>

<details open>
<summary><b>📺 5. Global Video Streaming & OTT (20 Packages)</b></summary>

| Application | Android Package ID |
|---|---|
| **Netflix** | `com.netflix.mediaclient`, `com.netflix.ninja` |
| **Disney+ / Hotstar** | `com.disney.disneyplus`, `in.startv.hotstar` |
| **Twitch** | `tv.twitch.android.app` |
| **Kick Streaming** | `com.kick.app` |
| **Amazon Prime Video** | `com.amazon.avod.thirdpartyclient`, `com.amazon.amazonvideo.livingroom` |
| **Hulu** | `com.hulu.plus` |
| **Max / HBO** | `com.wbd.stream`, `com.hbo.hbonow`, `com.hbo.gobro` |
| **Peacock TV** | `com.peacocktv.peacockandroid` |
| **Paramount+** | `com.cbs.app`, `com.cbs.ott` |
| **Apple TV** | `com.apple.atve.androidtv.appletv` |
| **Tubi TV** | `com.tubitv` |
| **Pluto TV** | `tv.pluto.android` |
| **Dailymotion** | `com.dailymotion.videoplayer` |
| **Vimeo** | `com.vimeo.android.videoapp` |
| **Plex** | `com.plexapp.android` |
| **JioCinema** | `com.jio.media.ondemand` |
| **Zee5** | `com.graymatrix.did` |
| **SonyLIV** | `com.sonyliv` |
| **MX Player** | `com.mxtech.videoplayer.ad` |

</details>

<details open>
<summary><b>💉 6. Game Modding, Memory Editors & Cracking (10 Packages)</b></summary>

| Application | Android Package ID | Bypass / Threat Vector |
|---|---|---|
| **Lucky Patcher** | `com.chelpus.luckypatcher`, `ru.chelpus.patcher`, `com.forpda.lp`, `com.dimonvideo.luckypatcher` | APK patching, in-app billing spoofing, signature removal |
| **GameGuardian** | `catch_.me_.if_.you_.can_`, `com.gameguardian` | Live process memory modification & speedhack |
| **SB Game Hacker** | `org.sbtools.gamehack` | Memory value scanner & trainer |
| **Creehack** | `org.cree.creehack` | Offline in-app purchase emulation |
| **Freedom APK** | `madkite.freedom`, `cc.madkite.freedom` | Google Play billing bypass daemon |

</details>

<details open>
<summary><b>📦 7. Virtual OS, Containers & Sandbox Bypass Tools (13 Packages)</b></summary>

| Application | Android Package ID | Bypass Mechanism |
|---|---|---|
| **VMOS / VMOS Pro** | `com.vmos.app`, `com.vmos.pro`, `com.vmos.vmospro` | Complete guest Android OS with independent root & zygote |
| **F1 VM** | `com.f1player.f1vm`, `com.f1vm.android` | Virtual machine sandbox bypassing system services |
| **VPhoneGaGa** | `com.vphonegaga.titan` | Parallel virtual Android runtime container |
| **X8 Sandbox** | `com.x8zs.sandbox` | Sandboxed Android environment with speed hacks |
| **Island** | `com.oasisfeng.island` | Managed work profile container |
| **Shelter** | `net.typeblog.shelter` | Work profile sandbox isolator |
| **MT Manager** | `bin.mt.plus` | Advanced APK editor, dex decompiler, signature cloner |
| **NP Manager** | `com.mcal.np` | Dex string decryptor, APK modding tool |
| **Apktool M** | `ru.maximoff.apktool` | Mobile decompilation & resource re-packager |
| **APK Editor** | `com.google.android.apps.apkeditor` | On-device manifest and byte modification |

</details>

<details open>
<summary><b>🛒 8. Modded App Stores & Cloners (11 Packages)</b></summary>

| Application | Android Package ID |
|---|---|
| **HappyMod** | `com.happymod.apk` |
| **Aptoide** | `cm.aptoide.pt` |
| **ACMarket** | `net.appx.acmarket` |
| **Mobilism** | `org.mobilism.android` |
| **Androeed** | `com.androeed` |
| **TutuApp** | `com.tutuapp.android` |
| **Parallel Space** | `com.lbe.parallel.intl` |
| **Dual Space** | `com.excelliance.dualaid`, `clone.app.dualspace` |
| **Super Clone** | `com.polestar.super.clone` |
| **App Cloner** | `com.applisto.appcloner` |

</details>

<details open>
<summary><b>✨ 9. Modded Social Media & Third-Party Clients (14 Packages)</b></summary>

| Application | Android Package ID | Base App / Target |
|---|---|---|
| **InstaPrime** | `com.instaprime.android` | Modded Instagram with media downloaders |
| **Instander** | `com.instander.android` | Modded Instagram client |
| **AeroInsta** | `com.aeroinsta.android` | Modded Instagram suite |
| **Honista** | `com.honista.app` | Modded Instagram client |
| **YouTube ReVanced** | `app.revanced.android.youtube` | Modded YouTube client |
| **ReVanced Extended** | `app.rvx.android.youtube` | Extended YouTube ReVanced fork |
| **NewPipe** | `org.schabi.newpipe` | Lightweight third-party YouTube player |
| **SmartTube** | `com.teamsmart.videomanager.tv` | Unofficial YouTube TV client |
| **YouTube Vanced (Legacy)** | `com.vanced.android.youtube` | Discontinued modded YouTube client |
| **GBWhatsApp** | `com.gbwhatsapp` | Modded WhatsApp client |
| **FMWhatsApp** | `com.fmwhatsapp` | Modded WhatsApp client |
| **YoWhatsApp** | `com.yowhatsapp` | Modded WhatsApp client |
| **Aero WhatsApp** | `com.aerowhatsapp` | Modded WhatsApp client |
| **Plus Messenger** | `org.telegram.plus` | Modded Telegram client |

</details>

<details open>
<summary><b>📱 10. Social Media & Short-Form Video (35 Packages)</b></summary>

| Application | Android Package ID |
|---|---|
| **TikTok** | `com.zhiliaoapp.musically`, `com.ss.android.ugc.trill` |
| **Douyin / Douyin Lite** | `com.ss.android.ugc.aweme`, `com.ss.android.ugc.aweme.lite` |
| **Kwai / Kuaishou** | `com.kwai.video`, `com.smile.gifmaker`, `com.kuaishou.nebula` |
| **Likee** | `video.like` |
| **CapCut (Video Editor)** | `com.lemon.lvoverseas`, `com.lemon.lv` |
| **Instagram / Instagram Lite** | `com.instagram.android`, `com.instagram.lite` |
| **Threads** | `com.instagram.barcelona` |
| **YouTube** | `com.google.android.youtube`, `com.google.android.apps.youtube.kids`, `com.google.android.apps.youtube.creator` |
| **Snapchat** | `com.snapchat.android` |
| **Facebook / Facebook Lite** | `com.facebook.katana`, `com.facebook.lite` |
| **X / Twitter / Twitter Lite** | `com.twitter.android`, `com.twitter.android.lite` |
| **Reddit** | `com.reddit.frontpage`, `com.rubenmayayo.reddit` (Infinity), `me.ccrama.redditslide` (Slide) |
| **Pinterest** | `com.pinterest` |
| **Tumblr** | `com.tumblr` |
| **BeReal** | `com.bereal.ft` |
| **Bluesky** | `xyz.blueskyweb.app` |
| **Mastodon** | `org.joinmastodon.android` |
| **Sina Weibo** | `com.sina.weibo` |
| **Xiaohongshu (RED)** | `com.xingin.xhs` |
| **Lemon8** | `com.bd.nproject` |
| **Amino** | `com.narvii.amino.master` |
| **9GAG** | `com.ninegag.android.app` |
| **Imgur** | `com.imgur.mobile` |
| **iFunny** | `mobi.ifunny` |

</details>

<details open>
<summary><b>🎮 11. Games, Gachas & Platforms (48 Packages)</b></summary>

| Application / Studio | Android Package ID |
|---|---|
| **Roblox** | `com.roblox.client` |
| **Discord** | `com.discord` |
| **Steam** | `com.valvesoftware.android.steam.community` |
| **HoYoverse Titles** | `com.mihoyo.genshinimpact`, `com.cognosphere.genshinimpact.oversea` (Genshin), `com.hoyoverse.hkrpgoversea`, `com.mihoyo.hkrpg` (Star Rail), `com.hoyoverse.nap`, `com.cognosphere.nap.oversea` (Zenless Zone Zero) |
| **Shooters & Battle Royales** | `com.tencent.ig`, `com.pubg.krmobile`, `com.pubg.imobile`, `com.vng.pubgmobile` (PUBG), `com.dts.freefireth`, `com.dts.freefiremax` (Free Fire), `com.activision.callofduty.shooter`, `com.activision.callofduty.warzone` (Call of Duty) |
| **MOBAs** | `com.mobile.legends` (Mobile Legends: Bang Bang), `com.riotgames.league.wildrift` (Wild Rift), `com.riotgames.league.teamfighttactics` (TFT), `com.garena.game.kgid` (Arena of Valor) |
| **Supercell Titles** | `com.supercell.brawlstars`, `com.supercell.clashofclans`, `com.supercell.clashroyale`, `com.supercell.squad`, `com.supercell.hayday`, `com.supercell.boombeach` |
| **Casual & Runners** | `com.king.candycrushsaga`, `com.king.candycrushsodasaga`, `com.king.farmheroessaga`, `com.kiloo.subwaysurf`, `com.imangi.templerun`, `com.imangi.templerun2`, `com.innersloth.spacemafia` (Among Us), `com.mojang.minecraftpe` (Minecraft) |
| **Sports & AR** | `com.ea.gp.fifamobile` (FC Mobile), `jp.konami.pesam` (eFootball), `com.nianticlabs.pokemongo`, `jp.pokemon.pokemonunite` |
| **Stores & Emulators** | `com.taptap`, `com.taptap.global`, `com.epicgames.portal`, `org.ppsspp.ppsspp`, `org.ppsspp.ppssppgold`, `com.retroarch`, `org.dolphinemu.dolphinemu`, `xyz.aethersx2.android` |

</details>

<details open>
<summary><b>🛍️ 12. Shopping & E-Commerce (19 Packages)</b></summary>

| Store | Android Package ID |
|---|---|
| **Shopee** | `com.shopee.ph`, `com.shopee.id`, `com.shopee.my`, `com.shopee.sg`, `com.shopee.th`, `com.shopee.vn`, `com.shopee.tw`, `com.shopee.br` |
| **Lazada** | `com.lazada.android` |
| **Amazon Shopping** | `com.amazon.mshop.android.shopping` |
| **Shein** | `com.zzkko` |
| **Temu** | `com.einnovation.temu` |
| **AliExpress / Taobao** | `com.alibaba.aliexpresshd`, `com.taobao.taobao` |
| **eBay** | `com.ebay.mobile` |
| **Tokopedia / Bukalapak** | `com.tokopedia.tkpd`, `com.bukalapak.android` |
| **Mercado Libre** | `com.mercadolibre` |

</details>

<details open>
<summary><b>📚 13. Fanfic, Web Novels & Light Novels (6 Packages)</b></summary>

| Application | Android Package ID | Description |
|---|---|---|
| **Wattpad** | `wp.wattpad` | Social storytelling and fanfiction platform |
| **Webnovel** | `com.qidian.Int.reader` | Serialized web novels and light novels |
| **MangaToon / NovelToon** | `mobi.mangatoon.novel`, `mobi.mangatoon.noveltoon` | Comics and light fiction reading |
| **Wuxiaworld** | `com.wuxiaworld.mobile` | Chinese/Korean martial arts & cultivation web novels |
| **Shosetsu** | `com.shosetsu.android` | Open-source novel aggregator |

</details>

<details open>
<summary><b>📹 14. Live Video Feeds & Stranger Chats (6 Packages)</b></summary>

| Application | Android Package ID | Description |
|---|---|---|
| **OmeTV** | `video.chat.ometv`, `com.duoduo.ometv` | Live video roulette with random strangers |
| **Bigo Live** | `sg.bigo.live` | Live streaming and virtual gifting broadcasts |
| **Tango** | `com.sgiggle.production` | Interactive live streaming and stranger chat |
| **17LIVE** | `com.machipopo.media17` | Live video social platform |
| **Yubo** | `co.yubo.mobile` | Social live streaming and friend discovery |

</details>

<details open>
<summary><b>💬 15. Dating & Swiping Apps (6 Packages)</b></summary>

| Application | Android Package ID | Description |
|---|---|---|
| **Tinder** | `com.tinder` | Swiping & dating app |
| **Bumble** | `com.bumble.app` | Dating, friends, and networking |
| **Hinge** | `co.hinge.app` | Relationship-oriented dating app |
| **Badoo** | `com.badoo.mobile` | Global dating community |
| **Omi** | `com.zenmen.omi` | Asian social dating app |
| **Grindr** | `com.grindrapp.android` | Location-based dating and chat |

</details>

<details open>
<summary><b>🎲 16. Gambling & Sports Betting (6 Packages)</b></summary>

| Application | Android Package ID | Description |
|---|---|---|
| **DraftKings** | `com.draftkings.dknative` | Sportsbook, casino, and daily fantasy |
| **FanDuel** | `com.fanduel.sportsbook` | Sportsbook & racing app |
| **PokerStars** | `com.pokerstars.android` | Real money poker and card tournaments |
| **Bet365** | `com.bet365Wrapper.Bet365` | Sports betting and live match odds |
| **1xBet** | `com.xbet.android` | Online sports betting client |
| **Stake** | `com.stake.mobile` | Crypto casino and sports betting |

</details>

<details open>
<summary><b>🏷️ 17. Resale & Second-Hand Bidding (4 Packages)</b></summary>

| Application | Android Package ID | Description |
|---|---|---|
| **Carousell** | `com.thecarousell.Carousell` | Classifieds marketplace for preloved goods |
| **Vinted** | `fr.vinted` | Second-hand clothes shopping & resale |
| **Depop** | `io.depop` | Vintage fashion marketplace |
| **Pinduoduo** | `com.xunmeng.pinduoduo` | Group-buying social shopping app |

</details>

<details open>
<summary><b>🌀 18. Hyper-Casual, .IO & Satisfying Time-Wasters (46 Packages)</b></summary>

| Application | Android Package ID | Publisher / Type |
|---|---|---|
| **Hole.io** | `io.voodoo.holeio` | VOODOO physics black-hole devourer |
| **Woodturning 3D** | `com.furunwang.woodturning`, `com.voodoo.woodturning` | VOODOO ASMR lathe carving simulator |
| **Paper.io & Paper.io 2** | `io.voodoo.paperio`, `io.voodoo.paper2` | VOODOO territory claiming .io game |
| **Helix Jump** | `com.h8games.helixjump` | VOODOO endless bouncy ball drop |
| **Crowd City** | `io.voodoo.crowdcity` | VOODOO crowd gathering .io battle |
| **Aquapark.io** | `com.cassette.aquapark` | VOODOO water slide racing .io |
| **Flappy Dunk** | `io.voodoo.flappydunk` | VOODOO winged basketball tapper |
| **Slither.io** | `air.com.hypah.io.slither` | Original snake multiplayer .io |
| **Agar.io / Diep.io** | `com.miniclip.agar.io`, `com.miniclip.diep.io` | Miniclip classic cellular & tank .io |
| **Snake.io** | `com.amelosinteractive.snake` | Modern arcade worm battle .io |
| **Survivor!.io** | `com.gorilla.boltrend.survivorio`, `com.habby.survivorio` | Habby roguelite zombie wave horde |
| **Archero** | `com.habby.archero` | Habby dungeon crawler shooter |
| **Join Clash 3D** | `com.freeplay.clash3d`, `com.superpow.snake` | Supersonic crowd runner battle |
| **Bridge Race** | `com.garawell.bridgerace` | Garawell brick collecting runner |
| **Going Balls** | `com.pronetis.ironball` | Supersonic rolling ball obstacle platformer |
| **Tall Man Run** | `com.supersonic.tallmanrun` | Supersonic scaling runner game |
| **Stumble Guys** | `com.kitkagames.fallbuddies` | Knockout multiplayer party royale |
| **Tiles Hop: EDM Rush!** | `com.amanotes.pamadiscodancing` | Amanotes rhythm ball hopper |
| **Magic Tiles 3** | `com.youmusic.magictiles` | Amanotes piano tile rhythm tapper |
| **Color Switch** | `com.fortafygames.colorswitch` | Color matching geometry reflex tapper |
| **Crossy Road** | `com.yodo1.crossyroad` | Endless road crossing arcade |
| **Fruit Ninja** | `com.halfbrick.fruitninjafree` | Halfbrick blade slashing reflex arcade |
| **Cut the Rope** | `com.zeptolab.ctr.ads` | ZeptoLab physics puzzle |
| **Blockudoku** | `com.easybrain.block.puzzle.game` | Easybrain spatial grid time sink |
| **2048** | `com.androbaby.game2048` | Sliding tile math puzzle |
| **Stack / Knife Hit / Rider** | `com.ketchapp.stack`, `com.ketchapp.knifehit`, `com.ketchapp.rider` | Ketchapp minimalist reflex games |
| **Sand Balls** | `com.mhappsgaming.sandballs` | SayGames sand digging physics puzzle |
| **Johnny Trigger** | `com.saygames.johnnytrigger` | SayGames slow-motion platform shooter |
| **My Perfect Hotel** | `com.redfox.myhotel` | SayGames idle hotel tycoon runner |
| **Race Master 3D** | `com.saygames.racemaster` | SayGames high-octane mini racer |
| **Hair Challenge / High Heels!** | `com.rolllic.hairchallenge`, `com.zynga.highheels` | Rollic / Zynga endless fashion runners |
| **Tie Dye / ASMR Slicing / Soap Cutting** | `com.crazylabs.tiedye`, `com.crazylabs.asmr.slicing`, `com.crazylabs.soapcutting` | CrazyLabs satisfying ASMR simulations |
| **Happy Glass / Save The Girl / Mr Bullet** | `com.lionstudios.happyglass`, `com.lionstudios.savethegirl`, `com.lionstudios.mrbullet`, `com.lionstudios.pullhimout` | Lion Studios puzzle & pin-pulling teasers |

</details>

---

### 🛡️ Dynamic Substring & Heuristic Defense

In addition to exact package ID matching, QIEZKA runs real-time **substring signature evaluation** on every foreground window change event in [`blacklistedApps.ts`](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/src/constants/blacklistedApps.ts) and [`BlacklistConstants.java`](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/android/app/src/main/java/com/uncode/app/BlacklistConstants.java).

Any application whose package identifier contains any of the following substrings is **automatically recognized as hostile and blocked**, instantly neutralizing rebranded clones, third-party APK forks, and modded sideloads:

```
kisskh • bilibili • danmaku.bili • chelpus • luckypatcher • instaprime • instander • aeroinsta • honista • gameguardian • happymod • acmarket • dramabox • reelshort • shortmax • goodshort • moboreels • loklok • cloudstream • movieboxpro • anilab • stremio • onstream • revanced • gbwhatsapp • fmwhatsapp • yowhatsapp • vmos • f1vm • vphonegaga • x8zs • wattpad • webnovel • wuxiaworld • ometv • bigo • tinder • bumble • pokerstars • bet365 • draftkings • fanduel • carousell • vinted • depop • holeio • woodturning • paperio • helixjump • crowdcity • aquapark • slither • agar.io • snake.io • survivorio • bridgerace • stumbleguys • voodoo • saygames • lionstudios • crazylabs • ketchapp
```

---

## 📖 Feature Deep Dive

### 1. Step-by-Step Schedule Creator with Editable OCR (Step 2)
Creating a schedule is intuitive and friction-free:
1. **Step 1: Select Attached Resources**: Choose from your local library or toggle 🌐 **General AI Knowledge**.
2. **Step 2: Homework Content & Requirements**: Upload an image, `.docx`, or text file to extract instructions via OCR. **The text is fully interactive and editable in a dedicated editor**, allowing students to fix typos, add notes, or write custom problem sets from scratch.
3. **Step 3: Define Grading Rubric**: Generate a criteria rubric using Google Gemini AI or write custom instructions.
4. **Step 4: Timing & Duration (Max 90 Minutes)**: Set activation time (between 7:00 PM and 3:00 AM) and duration. **Duration is strictly capped at 90 minutes** to enforce scientifically optimal deep work cycles (ultradian rhythms) and prevent burnout.

---

### 2. Active Schedules: Clickable Read-Only Homework Viewer
On the main Dashboard, active schedule cards feature a clickable **Homework Content** section:
- Provides a neat 3-line preview with an interactive **"View Full"** action.
- Tapping it summons an instant, distraction-free modal viewer displaying the complete homework prompt in full-fidelity read-only text.

---

### 3. Homeworks Log & Timeout Failure Tracking
All study activity is permanently tracked under the **Homeworks Log** (accessible via the file icon in the dashboard header):
- **Passed Sessions**: Displays green verification badges, OCR transcriptions, and AI feedback.
- **Failed / Expired Sessions**: If a timer expires before homework is submitted and passed, QIEZKA logs the attempt with a red `Failed / Expired` badge and timestamp to keep students accountable.
- Includes a 1-click **Clear All** action when desired.

---

### 4. AI General Knowledge Auto-Harvesting with `declutterResource`
When a student completes a homework session where 🌐 **General AI Knowledge** was enabled alongside a notebook resource:
- The AI answer and research are merged with the existing resource.
- The combined text is automatically processed by `declutterResource` using Google Gemini in the background.
- Redundant greetings, conversational filler, and markdown fluff are stripped away, leaving a clean, highly structured reference document in your offline resource library.

---

## 🌐 Online Architecture & Offline Resources (BYOK)

QIEZKA is designed as a **hybrid online/offline system** with a strict **Bring Your Own Key (BYOK)** privacy model:

```
                                  QIEZKA SYSTEM
                                        │
        ┌───────────────────────────────┴───────────────────────────────┐
        ▼                                                               ▼
[ ONLINE FEATURES ]                                             [ OFFLINE FEATURES ]
Requires Internet + Personal API Key                            Works 100% Without Internet
 • Handwritten Homework AI Evaluation (Gemini)                   • Study Resource Library & Notes Editor
 • Real-time Photo OCR Transcription (OCR.space)                 • Local Document Parsing (.docx, .txt, .md)
 • AI Prompt Refinement & Dynamic Rubrics                        • Focus Session Timers & Floating Ball
 • Cloud Model Selection (Gemini 2.5/2.0/Flash/Pro)              • Selective App Blocking (Accessibility)
 • Automatic Knowledge Harvesting & Decluttering                 • Quick Settings Shield & Recents Guard
                                                                 • Device Admin Anti-Uninstall Protection
                                                                 • Full JSON Data Backup & Restore
```

### 1. What Works Offline (Resources & Lockdown Only)
- **Local Study Resources**: Create, edit, search, organize, and review all notes, syllabi, and study materials with zero internet connection.
- **Client-Side Document Parsing**: Ingestion of `.txt`, `.md`, and Microsoft Word `.docx` documents is processed directly inside your browser/WebView using local in-memory engines ([Mammoth](https://github.com/mwilliamson/mammoth.js)).
- **Focus Enforcement & Lockdown Engine**: Android Accessibility service app-blocking, Quick Settings tile collapse, Device Administrator uninstall prevention, and boot recovery operate strictly on-device via native Android OS APIs.
- **Data Backups**: Export and import complete JSON backups of your settings, resources, and schedules offline.

### 2. What Requires an Online Connection & Your Own API Key (BYOK)
- **AI Homework Evaluation**: Grading your handwritten homework photo against your study rubric requires connecting to the Google Gemini API.
- **Photo OCR Transcription**: Extracting text from photographed papers requires connecting to the OCR.space API.
- **Users Must Provide Their Own API Key (BYOK)**:
  - **Zero Central Servers**: QIEZKA has no middleman servers, no proxy backends, and no proprietary accounts.
  - **100% Private**: Your API keys and homework photos travel directly from your phone to Google / OCR.space over encrypted HTTPS.
  - **Always Free**: Both Google Gemini and OCR.space provide generous **free tiers** that require no payment.

---

## 🔑 How to Get Your Free API Keys

QIEZKA takes 1 minute to configure with free keys:

### 1. Google Gemini API Key (Required for AI Evaluation)
1. Visit [Google AI Studio](https://aistudio.google.com/app/apikey).
2. Sign in with any Google account.
3. Click **"Create API Key"** and copy the generated key (starts with `AIzaSy...`).
4. In QIEZKA, tap the **Settings (gear icon)** at the top right, paste it into **Gemini API Key**, and tap **Save**.

### 2. OCR.space API Key (Required for Image Transcription)
1. Visit [OCR.space Free API Registration](https://ocr.space/ocrapi/freekey).
2. Enter your email and name; your free API key will be delivered instantly.
3. In QIEZKA, paste it into **Simple OCR API Key** or **Formatted OCR API Key** under Settings and tap **Save**.

---

## 🚀 Setup Guide

QIEZKA offers two distinct setup paths. Both yield the exact same security and app-blocking capabilities:

```
                            Choose Your Setup Path
                                      │
              ┌───────────────────────┴───────────────────────┐
              ▼                                               ▼
      [ Method A: On-Device ]                     [ Method B: PC ADB Script ]
       • 100% on your phone                        • Automated via qiezka.bat
       • No PC or USB cable needed                 • Takes 5 seconds
       • Step-by-step guided UI                    • Pre-grants all permissions
```

---

### Method A: On-Device Setup (No PC Required)

Ideal for everyday use. Complete all 4 steps inside the in-app **Permission Walkthrough** screen:

1. **Step 1: Enable Accessibility Service (Required)**
   - Tap **Open Accessibility Settings**.
   - Navigate to *Installed Apps* / *Downloaded Services*.
   - Tap **QIEZKA** and toggle it **ON**.
2. **Step 2: Activate Device Administrator (Recommended)**
   - Tap **Activate Device Admin**.
   - Confirm the system prompt to prevent uninstallation during a lockdown session.
   - *(If the direct prompt does not open on your OEM ROM, tap "Open Device Admin Apps list" and toggle QIEZKA on manually).*
3. **Step 3: Unrestricted Battery / Background (Recommended)**
   - Tap **Allow Unrestricted Background Usage**.
   - Tap **Allow** on Android's native battery optimization exemption prompt (`ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`) to stop OEM task killers from freezing timers.
4. **Step 4: Allow Notifications (Recommended)**
   - Tap **Allow Notifications** to enable persistent timer alerts and lock completion alerts.
5. **Tap "Proceed to Dashboard"** to finish setup!

> [!TIP]
> #### Android 13/14/15/16 "Restricted setting" Bypass Guide
> Modern Android automatically marks sideloaded apps with a *"Restricted setting"* warning for Accessibility and Device Admin. You can unlock it in 10 seconds:
> 1. In the QIEZKA walkthrough, tap **Step 1** or **Step 2** once (this triggers Android to register the restriction attempt).
> 2. Tap the **Open App Info** button inside the walkthrough banner.
> 3. In the top-right corner of QIEZKA's App Info page, tap the **3 dots (⋮)** menu.
> 4. Tap **Allow restricted settings** and confirm with your device PIN or fingerprint.
> 5. Return to QIEZKA and complete the steps normally!

---

### Method B: PC Automated Setup (`qiezka.bat`)

Ideal for power users, developers, or anyone with a PC who wants an instant, 1-click setup:

1. Enable **Developer Options** on your Android phone (Go to *Settings > About Phone* and tap *Build Number* 7 times).
2. Go to *Settings > Developer Options* and turn on **USB Debugging**.
3. Connect your phone to your PC via USB cable and allow the USB Debugging authorization prompt on your phone screen.
4. Double-click **`qiezka.bat`** (or execute it in Command Prompt / PowerShell).
5. The script automatically verifies your device, checks if QIEZKA is installed (or installs a local APK), unlocks restricted settings, whitelists battery, enables accessibility, activates device admin, and launches QIEZKA!

---

## ⚙️ Customizing `qiezka.bat`

[`qiezka.bat`](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/qiezka.bat) includes a **User Configuration Section** at the very top. You can open `qiezka.bat` in any text editor to customize every function using simple `true` or `false` flags:

```bat
:: ============================================================================
::                     USER CONFIGURATION / PREFERENCES
::  Edit the values below (true or false) to tailor the setup to your needs.
:: ============================================================================

:: 1. Force re-install local APK even if already installed on device (default: false)
set "FORCE_REINSTALL_APK=false"

:: 2. Unlock Android 13/14+ Restricted Settings automatically via ADB
set "BYPASS_RESTRICTED_SETTINGS=true"

:: 3. Grant elevated system permissions (WRITE_SECURE_SETTINGS, DUMP, POST_NOTIFICATIONS)
set "GRANT_SECURE_PERMISSIONS=true"

:: 4. Whitelist QIEZKA from aggressive OS battery savers (Samsung, Xiaomi, etc.)
set "WHITELIST_BATTERY=true"

:: 5. Automatically enable QIEZKA's Accessibility Service via ADB
set "ENABLE_ACCESSIBILITY=true"

:: 6. Activate Device Administrator to prevent uninstallation during lockdown
set "ACTIVATE_DEVICE_ADMIN=true"

:: 7. Attempt Enterprise Device Owner mode (DEFAULT: false)
set "TRY_DEVICE_OWNER=false"

:: 8. Automatically launch QIEZKA on your phone after setup completes
set "LAUNCH_APP_ON_FINISH=true"
```

---

## 🛡️ Security & Anti-Cheat Architecture

```
                            ┌────────────────────────┐
                            │   Active Lock Session  │
                            └───────────┬────────────┘
                                        │
          ┌─────────────────────────────┼─────────────────────────────┐
          ▼                             ▼                             ▼
  [ App Interception ]          [ SystemUI Defense ]          [ Anti-Bypass Guard ]
   • Window events monitored     • Quick Settings collapsed    • Device Admin prevents
   • Whitelisted: Allowed          in real time                  uninstallation
   • Blacklisted: Routed Home    • Notification shade stays    • Settings app: Blocked
   • Camera & File Pickers:        accessible for alerts       • BootReceiver: Resumes
     Exempt for homework         • Recents overview: Auto        lock after reboot
     submission without kick       re-launches if swiped
```

### 1. Canonical State Machine & Teardown Architecture
To eliminate race conditions, state drift, and lingering lock states, QIEZKA routes all session transitions through two centralized routines in `LockPlugin.java`:

- **`clearLockdownState(Context context)` [Authoritative Teardown]**:
  - Clears `lockdown_active`, `consequence_mode`, `consequence_schedule_id`, and `scheduled_end_time` in `SharedPreferences`.
  - Cancels all pending `AlarmManager` alarms.
  - Stops `FloatingOverlayService` (removes floating ball).
  - Stops `LocalDnsVpnService` (restores normal DNS resolution).
  - Cancels ongoing foreground and alert notifications.
  - Unblocks Device Owner uninstall blocks via `DevicePolicyManager.setUninstallBlocked(admin, pkg, false)`.
  - *All exit points* (`endLockdown()`, `setConsequenceActive(false)`, and `getLockStatus()` automatic timeout expiration) invoke this authoritative routine.

- **`startConsequenceState(Context context, String scheduleId, Set<String> whitelist)` [Authoritative Engagement]**:
  - Persists consequence flags and whitelisted package IDs.
  - Activates anti-uninstall protection via Device Policy Manager.
  - Launches `LocalDnsVpnService` (DNS sinkhole for distracting domains).
  - Starts `FloatingOverlayService` with the "⚠️ RETRY" state badge.
  - Displays persistent high-priority notification with quick-reschedule actions.
  - Immediately triggers `enforceBlock()` via `LockAccessibilityService` to evict any active hostile app.

### 2. Operating Hours Gate: Safemode vs. Hardcore Mode
During **Consequence Mode**, enforcement depends on your active operating mode:
- **Safemode (Default)**: Consequence enforcement is restricted to **7:00 PM – 3:00 AM**. During daytime hours (3:00 AM – 7:00 PM), enforcement is suspended so university students can freely attend daytime lectures, consult campus portals, and navigate public transit. If tested at 1:00 PM under Safemode, the app will pause blocking until 7:00 PM.
- **Hardcore Mode**: Enforcement is **24/7 continuous**. Once Consequence Mode is triggered, all distracting apps and blacklisted domains remain blocked around the clock until the student reschedules and successfully passes the homework submission.

### 3. Emergency ADB Recovery Procedures
If Device Owner or Device Admin lock states ever need to be manually reset from a host PC:
```bash
# Force-remove Device Administrator / Device Owner privileges:
adb shell dpm remove-active-admin com.uncode.app/.AdminReceiver

# Force-stop the app process and background services:
adb shell am force-stop com.uncode.app

# (Optional) Clear app data if a clean slate is desired:
adb shell pm clear com.uncode.app
```

### 4. Sandbox Isolation & Anti-Tampering
- **Restricted FileProvider**: Configured strictly to app-private cache and files directories (`Pictures/` and `cache/`), preventing host path traversal or external storage leaks.
- **Backup Disabled**: `android:allowBackup="false"` in `AndroidManifest.xml` prevents users or malicious tools from exporting or restoring stale SharedPreferences to bypass active lockdown sessions.

### 5. App Installation & Update Policy (Manual APKs & Google Play Store)
- **Non-Interrupted Updates & Installs**: Official OEM package installers (`com.google.android.packageinstaller`, `com.android.packageinstaller`, `com.samsung.android.packageinstaller`, etc.) and the Google Play Store (`com.android.vending`) are declared as infrastructure exemptions. Sideloading APKs, accepting system updates, and updating study tools through Google Play Store will never be aborted or kicked to the home screen.
- **Real-Time Defense on Launch**: Even if a student downloads or updates a distracting app (e.g. TikTok, a game, or a streaming app), QIEZKA's real-time Accessibility interceptor evaluates the package the moment it attempts to open. Any blacklisted or non-whitelisted app is instantly intercepted and routed back to the Home Screen / Lock overlay within 50ms.
- **Clean UI**: Package installers and store components are marked as internal infrastructure exemptions (`isHiddenSystemExemptApp`), ensuring they never clutter the user's study app launcher grid on the Dashboard or Lock Screen.
---

## 📱 OEM ROM Optimization Guide

Aggressive OEM background managers can prematurely terminate background accessibility monitors. Follow your manufacturer's checklist below:

<details>
<summary><b>🔵 Samsung (One UI)</b></summary>

1. **Battery Exemption**: Go to *Settings > Apps > QIEZKA > Battery* and choose **Unrestricted**.
2. **Never Sleeping Apps**: Go to *Settings > Battery > Background usage limits > Never auto-sleeping apps* and tap **+** to add QIEZKA.
3. **Lock in Recents**: Open the App Switcher (Recents), tap the QIEZKA app icon, and tap **Lock this app**.

</details>

<details>
<summary><b>🟠 Xiaomi / Redmi / POCO (MIUI / HyperOS)</b></summary>

1. **Autostart**: Go to *Settings > Apps > Manage Apps > QIEZKA* and toggle **Autostart** to **ON**.
2. **Battery Saver**: In the same menu, tap **Battery Saver** and select **No restrictions**.
3. **Lock in Task Switcher**: Open Recents, long-press the QIEZKA card, and tap the **Padlock icon**.
4. **Display Pop-up Windows**: Enable *Display pop-up windows while running in the background*.

</details>

<details>
<summary><b>🟢 Oppo / Realme / OnePlus (ColorOS / OxygenOS)</b></summary>

1. **Allow Background Activity**: Go to *Settings > Apps > App Management > QIEZKA > Battery Usage* and enable **Allow background activity** and **Allow auto-launch**.
2. **App Lock**: In Recents, tap the **3 dots** next to QIEZKA and select **Lock**.

</details>

<details>
<summary><b>🔴 Vivo / iQOO (Funtouch OS / OriginOS)</b></summary>

1. **High Background Power Consumption**: Go to *Settings > Battery > Background power consumption management* and set QIEZKA to **Don't restrict background power use**.
2. **Autostart**: Go to *Settings > Applications and Permissions > Autostart* and turn QIEZKA **ON**.

</details>

<details>
<summary><b>⚪ Google Pixel & Stock Android</b></summary>

1. Go to *Settings > Apps > See all apps > QIEZKA > App battery usage*.
2. Select **Unrestricted**.

</details>

---

## 📂 Codebase Architecture & Tour

```
qiezka/
├── android/app/src/main/java/com/uncode/app/
│   ├── AdminReceiver.java            # DeviceAdminReceiver: intercepts & prevents uninstall attempts
│   ├── AlarmReceiver.java            # BroadcastReceiver: triggers exact wall-clock schedule alarms
│   ├── AppClassifier.java            # On-device 5-layer heuristic application classifier
│   ├── BlacklistConstants.java       # Native blacklist database: package IDs & heuristic regexes
│   ├── BootReceiver.java             # Auto-resumes active lockdown sessions after device reboot
│   ├── FloatingOverlayService.java   # Draggable WindowManager floating ball timer with edge-snapping
│   ├── LocalDnsVpnService.java       # On-device loopback DNS sinkhole blocking distracting web domains
│   ├── LockAccessibilityService.java # Core Accessibility engine: window monitoring & app interception
│   ├── LockPlugin.java               # Capacitor two-way IPC bridge & canonical lockdown state machine
│   ├── MainActivity.java             # Android activity entry point & Capacitor bridge initializer
│   ├── PunishmentManager.java        # Consequence manager & accountability tracking
│   └── ScheduleManager.java          # Native schedule persistence & AlarmManager coordinator
├── src/
│   ├── api/
│   │   ├── buildRubric.ts            # AI rubric generation from homework prompts
│   │   ├── declutterResource.ts      # Gemini pipeline to strip chat fluff & clean study notes
│   │   ├── evaluate.ts               # Gemini multimodal handwritten homework grading
│   │   ├── generateAnswer.ts         # Generates reference answers & scoring rubrics
│   │   ├── parseResource.ts          # Client-side .docx (Mammoth), .txt, and .md document parsing
│   │   └── validateHomework.ts       # Validates photographed homework against rubric criteria
│   ├── components/
│   │   ├── CreateSchedule.tsx        # 4-step schedule creator with editable OCR text & 90-min cap
│   │   ├── Dashboard.tsx             # Active schedule countdown cards & modal homework viewer
│   │   ├── EditRubric.tsx            # Interactive grading criteria editor
│   │   ├── EvaluationResult.tsx      # Visual score breakdown & feedback card
│   │   ├── HomeworksPage.tsx         # Homeworks Log archive (passed & failed/expired records)
│   │   ├── LockScreen.tsx            # Full-screen lock enforcement & camera submission
│   │   ├── Onboarding.tsx            # First-run welcome & mission statement
│   │   ├── PermissionWalkthrough.tsx # In-app permission setup wizard with direct intent links
│   │   └── SettingsOverlay.tsx       # BYOK API key configuration & system preferences
│   ├── constants/
│   │   ├── allowedApps.ts            # Hardcoded whitelisted apps & hidden system file pickers
│   │   └── blacklistedApps.ts        # Hardcoded blacklisted apps & signature substring lists
│   ├── App.tsx                       # Root application coordinator, state management & routing
│   └── systemBridge.ts               # TypeScript wrapper for native LockPlugin Capacitor calls
├── qiezka.bat                        # Automated Windows ADB setup, permission grant & launch script
└── package.json                      # Project metadata, scripts, and dependencies
```

---

## 📜 Engineering Changelog & Architecture Evolution Log

This section details every major engineering revision, architectural refinement, and bug fix implemented in QIEZKA, documenting **why** specific mechanisms were introduced (the underlying problem, bypass vector, or usability trap) and **what** exact classes, components, and logic were modified.

---

### 1. Milestone 1: The Core Selective Focus Protocol
- **Why It Was Added**:
  - Existing app blockers failed at two extremes: soft "honor system" timers (easily bypassed during cognitive fatigue) or blunt "dumb phone" locks (disabling academic tools like lecture PDFs, notes, and AI explanation).
  - Modern academic workflows require active access to reference materials, course slide viewers, and AI tutors while completely cutting off algorithmic dopamine feeds (reels, games, social feeds).
- **What Was Implemented**:
  - **90-Minute Ultradian Limit**: Hard ceiling on scheduled sessions matching human biological attention spans to prevent burnout.
  - **BYOK Gemini Multimodal Evaluation**: Client-side direct HTTPS grading of photographed handwritten homework against custom rubrics without proprietary backends.
  - **Device Administrator Anti-Uninstall**: Blocks premature uninstallation or app removal via Android's `DevicePolicyManager`.
  - **Draggable WindowManager Overlay Ball**: Custom Android `TYPE_APPLICATION_OVERLAY` service with physics edge-snapping and timer countdown, keeping students oriented across allowed apps.

---

### 2. Milestone 2: Phantom Alarm Elimination & Exact Alarm Cancellation
- **Why It Was Changed**:
  - Students reported alarms triggering upcoming schedule notifications and starting lockdowns even after schedules were deleted, disabled, or updated to a different time.
  - When opened, no schedule was active for that time, causing the app to cancel the lock and return to the dashboard without blocking apps.
- **Root Cause & Technical Fix**:
  - In `ScheduleManager.java`, `cancelAllScheduledAlarms()` previously used a bare `Intent` with `action = null`. Because the scheduled alarms used `ACTION_SCHEDULE_START` and `ACTION_SCHEDULE_WARNING`, Android's `Intent.filterEquals()` failed to match them, leaving orphaned pending intents active in `AlarmManager`.
  - In `AlarmReceiver.java`, alarms were executed blindly without verifying whether the schedule still existed in persistent storage.
- **Modifications**:
  - [ScheduleManager.java](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/android/app/src/main/java/com/uncode/app/ScheduleManager.java): Systematically cancels pending intents matching explicit action strings (`ACTION_SCHEDULE_START`, `ACTION_SCHEDULE_WARNING`, and legacy `null`). Clamps `targetEndSim` so it cannot exceed `effectiveNow + durationMs`.
  - [AlarmReceiver.java](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/android/app/src/main/java/com/uncode/app/AlarmReceiver.java): Added verification that `scheduleId` exists and is marked `isActive: true` in `schedules_json` before firing notifications or launching lock sessions.

---

### 3. Milestone 3: Floating Ball Timer Clamping & LockScreen Countdown Synchronization
- **Why It Was Changed**:
  - The floating overlay ball timer frequently started at `1:00:00` (1 hour) regardless of whether the schedule duration was set to 25, 40, or 60 minutes.
  - On the full-screen `LockScreen.tsx`, the timer remained frozen at `40:00` and would not tick down until 20 minutes later (when 1 hour dropped below 40 minutes).
- **Root Cause & Technical Fix**:
  - `FloatingOverlayService` had no knowledge of `durationMinutes` and blindly computed `(lockEndTime - effectiveNow) / 1000L`. If `lock_end_time` in `SharedPreferences` had a stale 1-hour timestamp from a previous session, it displayed `1:00:00`.
  - In `LockScreen.tsx`, `const remaining = Math.min(maxAllowedSec, rawRemaining)` clamped `timeLeft` to 2400 seconds (40 min) while `rawRemaining` was 3600 seconds, freezing the visual display until `rawRemaining` caught up.
- **Modifications**:
  - [FloatingOverlayService.java](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/android/app/src/main/java/com/uncode/app/FloatingOverlayService.java): Added `getActiveScheduleDurationMinutes(prefs)` to inspect `active_schedule_id` and clamp `remainingSec = maxDurationMinutes * 60L` and adjust `lock_end_time` in `SharedPreferences`.
  - [LockScreen.tsx](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/src/components/LockScreen.tsx): Added `effectiveEndTime` clamping against `initialNow + maxAllowedSec * 1000` so countdown starts immediately (39:59, 39:58...) without delay.
  - [LockPlugin.java](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/android/app/src/main/java/com/uncode/app/LockPlugin.java): Added strict clamping in `startLockdown()` so `lockEndTime` cannot exceed `effectiveNow + (durationMinutes * 60L * 1000L)`.

---

### 4. Milestone 4: Evaluation Completion Dismissal & State Deactivation
- **Why It Was Changed**:
  - When students submitted homework and passed AI evaluation, the floating timer ball remained on screen, continuing to count down. Tapping or exiting returned the student back into the locked schedule.
- **Root Cause & Technical Fix**:
  - In `FloatingOverlayService.java`, `updateTimerDisplay()` required `if (!isLockdownActive && lockEndTime == 0L)` to dismiss the ball. When `endLockdown()` removed `lock_end_time` from preferences, `storedEnd` was 0, so `this.lockEndTime` was not reset and remained > 0, bypassing dismissal.
  - In `App.tsx`, `handleSubmitHomework()` called `endLockdown()` but did not mark the schedule as inactive (`schedule.isActive = false`). Because the current wall-clock time was still within the schedule window, the native background accessibility ticker saw `isActive: true` and re-locked the device.
- **Modifications**:
  - [FloatingOverlayService.java](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/android/app/src/main/java/com/uncode/app/FloatingOverlayService.java): Resets `lockEndTime = 0L` when `storedEnd <= 0`. If `!isLockdownActive`, immediately removes the floating view from `WindowManager`, stops the foreground service, and calls `stopSelf()`.
  - [LockPlugin.java](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/android/app/src/main/java/com/uncode/app/LockPlugin.java): In `endLockdown()`, writes `last_completed_window_end_<scheduleId>` to `SharedPreferences`.
  - [LockAccessibilityService.java](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/android/app/src/main/java/com/uncode/app/LockAccessibilityService.java) & [ScheduleManager.java](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/android/app/src/main/java/com/uncode/app/ScheduleManager.java): Skips lock engagement if `windowEnd <= lastCompletedEnd`.
  - [App.tsx](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/src/App.tsx): In `handleSubmitHomework()`, when evaluation passes, immediately sets `isActive: false`, syncs via `syncSchedules()`, and invokes `endLockdown()`.

---

### 5. Milestone 5: Intelligent Resource & AI General Knowledge Validation
- **Why It Was Changed**:
  - When students selected both course notes and "AI General Knowledge", Step 2 in the Schedule Creator previously skipped validation entirely.
  - If an assignment was completely answerable using course notes alone (e.g. Student Grade Calculator), leaving AI General Knowledge enabled caused the auto-harvester to harvest scenario-specific details (like quiz weights and formulas) into clean resource notes upon passing.
  - If a student didn't select AI knowledge and validation failed, they were stuck on Step 2 with no clear progression path.
- **Modifications**:
  - [defaultPrompts.ts](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/defaultPrompts.ts): Added Rule 3 to `validateHomework`: self-contained problem scenarios, formulas, or parameters where core principles exist in notes are explicitly validated as complete without needing outside AI knowledge.
  - [CreateSchedule.tsx](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/src/components/CreateSchedule.tsx): Validates homework against course notes first. If notes are sufficient, displays the **"AI General Knowledge May Not Be Needed"** modal offering **"Turn Off AI Knowledge & Continue" (Recommended)** to prevent note pollution. If notes are insufficient and AI wasn't selected, provides an inline **"Enable AI General Knowledge & Proceed"** one-click shortcut.

---

### 6. Milestone 6: Homeworks Log "Add Again" Cascade Rescheduling (Option B)
- **Why It Was Added**:
  - Students with failed or expired homework sessions in the Homeworks Log needed a way to re-attempt their assignments without causing schedule overlap conflicts.
- **What Was Implemented**:
  - In [HomeworksPage.tsx](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/src/components/HomeworksPage.tsx):
    - **Midpoint Preemption**:
      - If requested emergency time is **below midpoint (< 50%)** of an existing schedule: the Emergency schedule preempts the conflicting schedule at the chosen time; the conflicting schedule starts after the emergency session + 30-minute buffer.
      - If requested emergency time is **at or above midpoint (≥ 50%)**: the conflicting schedule finishes its duration; the Emergency schedule starts after it + 30-minute buffer.
    - **30-Minute Break Buffer**: Enforces mandatory 30-minute rest intervals between consecutive sessions (no buffer trailing the final session).
    - **3:00 AM Curfew Enforcement**: Rejects schedule cascades exceeding 3:00 AM with `"Too many schedules / exceeds 3:00 AM curfew"`.

---

### 7. Milestone 7: Simulated Time Bridge & Clock Synchronization
- **Why It Was Added**:
  - Testing schedule triggers, countdown transitions, and consequence operating windows required waiting hours for real wall-clock time.
  - Simulating time purely in React caused severe desynchronization with native Android `AlarmManager`, `FloatingOverlayService`, and `LockAccessibilityService`.
- **What Was Implemented**:
  - In [Dashboard.tsx](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/src/components/Dashboard.tsx): Added simulated time header with live clock, time override picker, and quick reset.
  - In [systemBridge.ts](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/src/systemBridge.ts) & [LockPlugin.java](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/android/app/src/main/java/com/uncode/app/LockPlugin.java): Implemented `syncTimeOffset(offsetMs)` persisting `simulated_time_offset` into `SharedPreferences`.
  - In [FloatingOverlayService.java](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/android/app/src/main/java/com/uncode/app/FloatingOverlayService.java) & [LockAccessibilityService.java](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/android/app/src/main/java/com/uncode/app/LockAccessibilityService.java): All calculations use `effectiveNow = System.currentTimeMillis() + timeOffset`.

---

### 8. Milestone 8: Accessibility Service Self-Exemption & Safe Schedule Start
- **Why It Was Changed**:
  - Under certain race conditions when lockdown engaged, opening QIEZKA to view instructions or capture homework photos caused QIEZKA itself to be intercepted or minimized by its own accessibility service.
- **What Was Implemented**:
  - In [AppClassifier.java](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/android/app/src/main/java/com/uncode/app/AppClassifier.java) & [LockAccessibilityService.java](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/android/app/src/main/java/com/uncode/app/LockAccessibilityService.java): Added explicit self-package exemption (`pkg.equals(context.getPackageName())`) at Tier 1 before any heuristic checks.

---

### 9. Milestone 9: Consequence Mode Accountability & Failed Homework Archiving
- **Why It Was Changed**:
  - When a student abandoned a session or let the timer expire without submitting, the app previously closed itself and left the failed schedule in limbo, failing to enforce accountability.
- **What Was Implemented**:
  - In [App.tsx](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/src/App.tsx): When timer expires, `handleTimeout()` logs the failed session into `completedHomeworks` with detailed diagnostic feedback and transitions smoothly to Dashboard.
  - Distracting apps remain locked during operating hours (7 PM – 3 AM in Safemode, or 24/7 in Hardcore) until the failed homework is rescheduled and passed.
  - In [Dashboard.tsx](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/src/components/Dashboard.tsx): Prominently displays the red **"⚠️ Consequence Active: Homework Failed"** banner with a direct link to the Homeworks Log.

---

### 10. Milestone 10: Preserving App Installation & Play Store Package Installers
- **Why It Was Changed**:
  - Updating or installing apps (via Google Play Store, Samsung Galaxy Store, or manual APK installation) was blocked by the accessibility window monitor during study hours because package installer activities had unknown or system category.
- **What Was Implemented**:
  - In [AppClassifier.java](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/android/app/src/main/java/com/uncode/app/AppClassifier.java) & [LockAccessibilityService.java](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/android/app/src/main/java/com/uncode/app/LockAccessibilityService.java):
    - Added `INSTALLER_AND_STORE_PACKAGES` whitelist containing Google Play Store, Google Package Installer, AOSP Package Installer, Samsung Galaxy Store, MIUI Package Installer, ColorOS Package Installer, and Huawei AppGallery.
    - Implemented `isInstallerOrStorePackage(pkg)` checking package name signatures (`.packageinstaller`, `.installer`).
    - Added package installer exemption to Tier 1 of the classification engine so installations and updates run uninterrupted.

---

### 11. Milestone 11: Operating Modes (Safemode vs Hardcore 24/7) & Local DNS Sinkhole VPN
- **Why It Was Added**:
  - Standard Consequence Mode operates only during study hours (7:00 PM – 3:00 AM). University students requested a "Hardcore" mode enforcing 24/7 round-the-clock restrictions.
  - In-app browser tabs and WebViews allowed bypassing app-level blocks to access distracting websites.
- **What Was Implemented**:
  - In [SettingsOverlay.tsx](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/src/components/SettingsOverlay.tsx) & [App.tsx](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/src/App.tsx): Added Operating Mode toggle (`safemode` vs `hardcore`) with a dedicated Hardcore confirmation dialog.
  - In [LocalDnsVpnService.java](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/android/app/src/main/java/com/uncode/app/LocalDnsVpnService.java): Integrated on-device DNS loopback sinkhole intercepting UDP port 53 DNS queries and returning `0.0.0.0` for blacklisted domains (TikTok, Instagram, Netflix, KissKH, etc.).
  - Added Web Protection Mode selector (`accessibility`, `dns_vpn`, `dual_hybrid`, `off`) with native permission triggers.

---

### 12. Milestone 12: YouTube Academic Browsing Mode
- **Why It Was Added**:
  - STEM students require access to YouTube coding tutorials, physics lectures, and 3Blue1Brown explanations, but the native YouTube app is an extreme distraction vector with Shorts and algorithm feeds.
- **What Was Implemented**:
  - In [SettingsOverlay.tsx](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/src/components/SettingsOverlay.tsx), [LockAccessibilityService.java](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/android/app/src/main/java/com/uncode/app/LockAccessibilityService.java), and [LocalDnsVpnService.java](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/android/app/src/main/java/com/uncode/app/LocalDnsVpnService.java):
    - Added **"Allow YouTube in Browser"** toggle.
    - **Native App Strictly Blocked**: `com.google.android.youtube` remains strictly blocked under all circumstances.
    - **Browser Filtering**: Web browsers are permitted to load `m.youtube.com` and `youtube.com/watch`, but URL keywords matching `shorts`, `feed/explore`, and `gaming` are immediately intercepted by Accessibility Guard.

---

### 13. Milestone 13: 5-Step Interactive Onboarding Wizard & Device Permissions Setup
- **Why It Was Changed**:
  - First-run setup was too brief, missing interactive explanations of selective focus, proof-of-work expectations (photographing/screenshotting handwritten work or code), terms and conditions, and system permission verification.
- **What Was Implemented**:
  - In [Onboarding.tsx](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/src/components/Onboarding.tsx):
    - Re-architected into a 5-step interactive wizard with full vertical touch scrolling (`overflow-y-auto`, `overscroll-contain`):
      1. **Step 1: The Protocol**: Explains selective focus, floating timer ball, unlocking via photograph/screenshot of completed homework, and consequence mode.
      2. **Step 2: Nuclear Agreement**: Transparent Terms & Conditions with individual acknowledgment cards and "Accept All Terms" shortcut.
      3. **Step 3: Device Permissions**: In-app permission manager with live status badges and direct intent launchers for Accessibility Service (with Android 13+ restricted settings shortcut), Post Notifications, Battery Optimization, and Device Administrator.
      4. **Step 4: Optional Quick Setup**: Front-loads BYOK Gemini API key, Free OCR key, Operating Mode (with Hardcore confirmation modal), and Web Protection (with Android system VPN prompt). Includes **"Skip for Now"** button.
      5. **Step 5: Ready to Focus**: Summary overview card and direct launch into the Dashboard.

---

### 14. Milestone 14: Consequence Mode Import Protection & Orphan Auto-Recovery
- **Why It Was Changed**:
  - A user attempted to reset the app by importing a blank JSON backup (with empty schedules, empty completedHomeworks, and `consequenceActive: false`).
  - However, native Android SharedPreferences (`uncode_lock.xml`) still had `consequence_active = true`. On boot, `getLockStatus()` queried native preferences and reinstated `consequenceActive: true`, leaving the user deadlocked in Consequence Mode with 0 failed homeworks available to reschedule and pass.
  - Furthermore, allowing students to import backups during active lockdown or consequence mode would undermine the anti-cheat protocol.
- **What Was Implemented**:
  - In [SettingsOverlay.tsx](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/src/components/SettingsOverlay.tsx):
    - "Import Backup" is disabled during active lockdown or consequence mode (`isLockedOrConsequence`) with a lock badge and tooltip: `"🔒 Backup import is disabled during active lockdown or consequence mode. Complete and pass your session first."`.
    - "Export Backup" remains 100% functional at all times so students never lose their data.
    - On valid import outside lockdown/consequence, native preferences are synchronized via `endLockdown()`, `setConsequenceActive(false)`, and `syncSchedules(...)`.
  - In [Dashboard.tsx](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/src/components/Dashboard.tsx):
    - Consequence banner checks if `completedHomeworks` has any failed sessions (`completedHomeworks.some(h => !h.passed)`).
    - If false (orphaned state), renders an amber card: **"⚠️ Consequence Mode Active (No Failed Homeworks Found)"** with a direct **"Clear Orphaned Consequence"** button.
    - Added **"Reset All Locks"** quick-action button in the simulated time debug header.
  - In [App.tsx](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/src/App.tsx):
    - In `loadAll()`, auto-heals orphaned consequence states if native consequence is active but failed homework count is 0.
    - In `handleVisibilityChange()`, checks `completedHomeworksRef` before reinstating consequence mode.

---

### 15. Milestone 15: Web-Based Game Armor & Browser Gaming Defense Protocol
- **Why It Was Added**:
  - When native game apps (like *Coxeta*, *Genshin*, or *Roblox*) were blocked, students bypassed restrictions by opening allowed browsers (Chrome, Samsung Internet, Firefox, Brave, Edge) to play web-based games, viral .IO multiplayer games, retro emulators, cloud-streamed APKs (`now.gg`), or search-embedded mini-games (Chrome Dino, Google Snake).
- **What Was Implemented**:
  - In [LocalDnsVpnService.java](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/android/app/src/main/java/com/uncode/app/LocalDnsVpnService.java):
    - Integrated a comprehensive **250+ gaming domain database** covering web game mega-portals (Poki, CrazyGames, Coolmath Games, Kongregate, Armor Games, Newgrounds, Y8, Friv), viral .IO arenas (Slither, Agar, Krunker, 1v1.lol, Paper.io, Hole.io, Surviv, Bloxd, Ev.io), cloud APK streaming (`now.gg`, GeForce NOW, Luna), indie iframe runtimes (`*.itch.zone`), web emulators, and unblocked game networks.
    - Sinkholes UDP port 53 DNS lookups for gaming domains to RFC-standard `NXDOMAIN` responses.
  - In [LockAccessibilityService.java](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/android/app/src/main/java/com/uncode/app/LockAccessibilityService.java):
    - **Real-Time Address Bar Interception**: Synchronized with the 250+ web gaming domain set.
    - **Browser Internal Mini-Game Scheme Defense**: Intercepts `chrome://dino`, `chrome://network-error/-106`, `edge://surf`, and `opera://game`, immediately executing `GLOBAL_ACTION_BACK` with alert toast: *"⚠️ Browser mini-games are blocked during study sessions"*.
    - **Search-Embedded Interactive Canvas Game Defense**: Detects Google Search and Bing query parameters launching embedded HTML5 games (`snake`, `play snake`, `tic tac toe`, `pacman`, `minesweeper`, `solitaire`, `atari breakout`, `dreidel`, `fidget spinner`, `earth day quiz`, `memory game`), backing out before the game canvas renders.
    - **Unblocked Game Mirror & Proxy Heuristics**: Detects unblocked game mirror signatures across `sites.google.com/view/*`, `*.github.io/*`, and keywords (`unblocked-games`, `classroom6x`, `slope-game`, `retrobowl`, `moto-x3m`).
    - **Sub-Path Game Blocking**: Blocks `/games/`, `/crosswords/`, `/puzzles/` on general news portals (`nytimes.com/games`, `zone.msn.com`, `yandex.com/games`) while keeping legitimate news articles readable.
    - **Ironclad Academic Safe-List Immunity**: Explicitly exempts Wikipedia, university `.edu` domains, MDN Web Docs, Khan Academy, Coursera, GitHub repositories, and game engine documentation from heuristic filters to guarantee zero false positives during research.
  - In [LockPlugin.java](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/android/app/src/main/java/com/uncode/app/LockPlugin.java) & [systemBridge.ts](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/src/systemBridge.ts):
    - Added `setBlockWebGames(boolean)` persisting `block_web_games` into `SharedPreferences`.
  - In [SettingsOverlay.tsx](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/src/components/SettingsOverlay.tsx):
    - Added dedicated **"Block Web-Based Games"** card with toggle switch (enabled by default) with badge `250+ Sites • Portals, .IO & Cloud APKs Protected`. Disabled during active lockdown and consequence mode.
  - In [App.tsx](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/src/App.tsx):
    - Synchronizes `block_web_games` preference to native bridge on boot and upon saving settings.

---
### 16. Milestone 16: Unified Tri-Mode Web Protection Architecture (Accessibility, DNS Sinkhole & Dual Hybrid)
- **Why It Was Added**:
  - Manually updating blocklists or relying on inflexible, external upstream family filters proved either too narrow or created unwanted network side-effects. Students require reliable, device-wide web defense that seamlessly integrates with their study environment while preserving battery life and device compatibility.
  - To accommodate diverse student needs and device environments, QIEZKA introduced a unified tri-mode web protection architecture:
    1. **`accessibility` (Default — Zero VPN Overhead)**: Uses Android's native Accessibility Service to continuously inspect active browser address bars and page structures in real time. Requires zero VPN slots, leaving the device's VPN slot completely open for school, university, or work VPNs.
    2. **`dns` (Local DNS Sinkhole VPN)**: Runs an on-device local UDP port 53 DNS sinkhole engine that intercepts network-level DNS queries phone-wide across all applications, synthesizing RFC-compliant `NXDOMAIN` responses for blocked distraction destinations in <1ms without routing external data through third parties.
    3. **`dual` (Dual-Layer Hybrid)**: Combines local DNS packet sinkholing with real-time accessibility DOM/URL eviction for defense-in-depth protection.
- **Architectural Implementation**:
  - In [LocalDnsVpnService.java](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/android/app/src/main/java/com/uncode/app/LocalDnsVpnService.java):
    - All incoming DNS queries are evaluated on-device directly against `WebClassifier.classifyDomain(lowerDomain, allowYoutube)`.
    - Blocked domains receive instant local `NXDOMAIN` (RCODE 3) responses.
    - Legitimate academic and research lookups are forwarded to high-speed upstream DNS resolvers (Cloudflare `1.1.1.1` & Google `8.8.8.8`) with built-in failover redundancy.
    - DoH canary endpoints (`use-application-dns.net`, `cloudflare-dns.com`, `dns.google`) are sinkholed to enforce system-level resolution in Chromium and Firefox.
  - In [SettingsOverlay.tsx](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/src/components/SettingsOverlay.tsx) & [App.tsx](file:///c:/Users/CxAdmin/Desktop/qiezka/uncode/src/App.tsx):
    - Added dedicated **"Web Protection Engine"** visual card selector (`accessibility`, `dns`, `dual`) with clear status indicators and explanations.
    - Fully locked and immutable during active lock sessions and consequence mode to prevent evasion.

---

### 17. Milestone 17: Multi-User Anti-Evasion, App Cloner Elimination & Wide Recognition Engine
- **Why It Must Be Implemented**:
  - Traditional Android app blockers operate on a naive single-user, single-instance model: they inspect package IDs on the primary user account (`UserHandle 0`).
  - When students experience high cognitive friction during difficult study sessions, their brain actively searches for technical escape routes. In modern Android, the OS provides virtualization and multi-tenancy frameworks that circumvent basic package checks:
    1. **App Cloners & Virtual Spaces (`Parallel Space`, `Dual Apps`, `Multi Space`, `Island`, `Shelter`)**: Apps running in secondary containers or cloned packages (e.g. `com.lbe.parallel.intl`, `com.lbe.parallel.intl.arm64`, `com.dualspace.multispace.android`) bypass exact package equality checks.
    2. **Multi-User Environments & Second Space**: Xiaomi "Second Space", Android 15 "Private Space", and Android guest accounts run isolated user profiles (`u10_a123`). Switching users leaves QIEZKA dormant on User 0 while the secondary space is unrestricted.
    3. **Samsung Secure Folder & Knox Containers**: Encrypted enterprise sandboxes with separate package registries.
    4. **Fake "Calculator" Vaults**: Apps disguised as simple arithmetic calculators that reveal media vaults, social media wrappers, or games upon typing a secret PIN (e.g. `1234=`).
    5. **Virtual Android Environments (VMOS, VPhoneGaGa, F1VM)**: Complete guest Android virtual machines running inside an app container that conceal all distraction activity from host accessibility services.
    6. **PWA & WebAPK Shortcuts**: Web apps running under `org.chromium.chrome.browser.webapps.SameTaskWebApkActivity` masquerading as standalone apps.
- **Architectural Implementation**:
  - **Comprehensive Parallel Space & Cloner Runtime Blacklist**:
    - Hardcoded all major variations of Parallel Space (`com.lbe.parallel.intl`, `com.lbe.parallel.intl.arm64`, `com.lbe.parallel.intl.arm32`, `com.lbe.parallel.parallel`, `com.lbe.parallel.pro`, `com.lbe.doubleinstance`, `com.parallel.space.lite`, `com.trendmicro.tpacket.parallellite`), Dual Space, Multi Space, Super Clone, 2Accounts, Clone App, App Cloner, Island, Shelter, VMOS Pro, VPhoneGaGa, F1VM, GSpace, and X8 Sandbox into `BlacklistConstants.java`.
    - Added heuristic signature filters in `isBlacklisted()` for `.parallel.`, `.dualspace.`, `.multispace.`, `.superclone.`, `.appcloner.`, `.2accounts.`, and `.cloner.`.
  - **Vastly Widened App Recognition Engine (`AppClassifier.java`)**:
    - Broadened recognition across all distraction genres: comprehensive gaming keywords (action, RPG, MMO, battle royale, idle clicker, gacha, visual novel, hypercasual, simulator, card game, board game, puzzle, tower defense, crafting), gambling, short-dramas (ReelShort, DramaBox, ShortMax, GoodShort), webtoons/manga, web novels (Wattpad, Wuxia), dating apps, and live-streaming platforms.
    - Android Category Guard: Evaluates `CATEGORY_GAME`, `CATEGORY_SOCIAL`, `CATEGORY_VIDEO`, and `FLAG_IS_GAME` to block distractors regardless of package naming.
  - **Fake Calculator & Secret Vault Detection**: Inspects apps declared as "Calculator" or "Calc" requesting `READ/MANAGE_EXTERNAL_STORAGE`, `CAMERA`, or `INTERNET`, or containing vault activity signatures (`*vault*`, `*gallerylock*`, `*secret*`, `*hidestore*`).
  - **Explicit Allowance of Package Installers**:
    - Per user configuration, Android System Package Installers (`com.google.android.packageinstaller`, `com.android.packageinstaller`, and system installer components) are **explicitly allowed** across both lockdown and consequence modes, ensuring seamless app installation, sideloading, and updates.

---

### 18. Milestone 18: Massive Multi-Genre WebClassifier & Autocomplete Typing Immunity Guard
- **Why It Was Mandated**:
  - Narrow, hardcoded game lists proved insufficient against modern digital distractions: students easily wandered onto web-based gambling, adult content, proxy tunnels, piracy streaming, vertical short-dramas, viral gossip, and crypto speculation.
  - Crucially, previous URL evaluation mechanisms suffered from an **autocomplete eviction trap**: when a student typed the letter `y` in Chrome to search or navigate to `yale.edu`, Chrome's inline autocomplete pre-filled `y8.com`. The accessibility service extracted `y8.com` on the keystroke text-change event and fired `GLOBAL_ACTION_BACK`, booting the student out mid-keystroke. The student was unable to type any word starting with `y`, `p`, `r`, or `t`.
- **Architectural Enhancements**:
  - **Unified On-Device Intelligence Engine (`WebClassifier.java` & `WebBlocklistConstants.java`)**:
    - Operates as the single authoritative brain across both `LockAccessibilityService` (DOM & address bar) and `LocalDnsVpnService` (DNS socket layer).
    - **600+ Distraction Domains across 10 Categories**:
      1. *Web Games & Cloud APKs (400+ Domains)*: Poki, CrazyGames, CoolMath, Y8, Friv, ArmorGames, Newgrounds, .IO arenas (Slither, Agar, Krunker, 1v1.lol, Smash Karts), retro emulators (vimm, retrogames), unblocked networks (classroom6x, 3kh0, slope, retro bowl, moto-x3m), and cloud APK runtimes (`now.gg`).
      2. *Online Gambling & Sportsbooks (100+ Domains)*: Stake, Roobet, Bet365, Bovada, DraftKings, FanDuel, BetMGM, Caesars, PokerStars, 1xBet, 888casino, CSGO gambling, skin betting.
      3. *Adult & Explicit NSFW (100+ Domains)*: Major adult tube networks, live cam portals, OnlyFans, Fansly, nHentai, Rule34, adult webcomics, and erotica.
      4. *Web Proxies & School Unblockers (50+ Domains)*: CroxyProxy, ProxySite, 4everproxy, BlockAway, Rammerhead, Ultraviolet, Womginx, Ludicrous, Interstellar, and CGI bypass tunnels.
      5. *Piracy Streaming, Manga & Short-Dramas (100+ Domains)*: 123movies, Fmovies, Putlocker, Soap2Day, Aniwave, MangaDex, AsuraScans, ReelShort, DramaBox, ShortMax, GoodShort, ThePirateBay, FitGirl.
      6. *Social Media & Algorithmic Feeds (40+ Domains)*: TikTok, Instagram, Twitter/X, Reddit, Threads, Snapchat, Discord, Twitch, Kick, Bilibili, Pinterest, Tumblr.
      7. *Casual Dating & Random Video Cam Chat (50+ Domains)*: Tinder, Bumble, Hinge, Badoo, OkCupid, Match, Grindr, Omegle, OmeTV, Chatroulette, Emerald Chat, Monkey App.
      8. *Viral Gossip & Clickbait Time-Wasters (30+ Domains)*: BuzzFeed, BoredPanda, TheChive, DailyMail, TMZ, HollywoodLife, E! Online, Cracked, Ranker.
      9. *Crypto Meme Coin Speculation (10+ Domains)*: Pump.fun, DexScreener, Birdeye, Raydium, PancakeSwap, Uniswap, SushiSwap.
      10. *Suspicious gTLDs*: `.casino`, `.bet`, `.poker`, `.adult`, `.porn`, `.xxx`, `.sex`, `.cam`, `.dating`, `.vodka`, `.bingo`.
  - **Vast Tier 1 Academic & Developer Safe-List (100+ Domains & Worldwide TLDs)**:
    - *Reference & Encyclopedias*: Wikipedia, Wiktionary, Britannica, Merriam-Webster, Oxford, Cambridge, Stanford Encyclopedia of Philosophy.
    - *Learning Management Systems (LMS)*: Canvas, Blackboard, Schoology, Moodle, Google Classroom, PowerSchool, Infinite Campus, College Board, ACT.
    - *Science, Mathematics & STEM Tools*: Desmos, GeoGebra, WolframAlpha, Symbolab, Mathway, Integral/Derivative calculators, Khan Academy, Brilliant, PhET Interactive Simulations, ChemGuide, PTable, HyperPhysics.
    - *Scholarly Journals & Scientific Repositories*: arXiv, bioRxiv, medRxiv, ResearchGate, JSTOR, Nature, Science, ScienceDirect, Springer, Wiley, IEEE Xplore, ACM Digital Library, PubMed, NCBI, Google Scholar, NASA, NOAA, USGS.
    - *Courseware & Study Guides*: Coursera, edX, Udemy, Quizlet, Brainly, Chegg, Gutenberg, Internet Archive, SparkNotes.
    - *Language Learning*: Duolingo, Babbel, Memrise, Busuu, AnkiWeb, DeepL, Google Translate.
    - *Computer Science & Developer Docs*: MDN Web Docs, W3Schools, GeeksforGeeks, StackOverflow, GitHub, GitLab, LeetCode, HackerRank, FreeCodeCamp, Oracle Java Docs, Python Docs, Node.js, React, Angular, Vue, Android Developers, Apple Developer, Microsoft Learn, AWS, Google Cloud, Overleaf, LaTeX.
    - *Student Productivity Suites*: Google Docs, Drive, Sheets, Slides, Forms, Notion, Obsidian, Trello, Miro, Figma, Canva, Microsoft Office 365, OneDrive.
    - *Worldwide TLD Immunity*: Hardcoded whitelist for `.edu`, `.ac.uk`, `.ac.jp`, `.ac.in`, `.edu.au`, `.edu.sg`, `.edu.ph`, `.edu.cn`, `.edu.br`, `.gov`, `.gov.uk`, `.gov.au`, `.mil`.
  - **Browser Autocomplete Typing Immunity & Address Bar Focus Guard (`LockAccessibilityService.java`)**:
    - **Keystroke & Selection Event Filtering**: `TYPE_VIEW_TEXT_CHANGED` and `TYPE_VIEW_TEXT_SELECTION_CHANGED` events from browser packages are immediately ignored in `onAccessibilityEvent`. Typing a character never triggers mid-keystroke URL evaluation.
    - **Address Bar Focus Guard**: Before evaluating candidate address bar view IDs, checks `root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)` and `node.isFocused()`. If an address bar, search box, or text field is actively focused, URL extraction immediately returns `null`. Autocomplete suggestions are never evaluated.
    - **Dropdown Suggestion Isolation**: Excluded autocomplete suggestion list view IDs (such as `com.android.chrome:id/line_1`) from address bar targets, ensuring suggestion lists are never mistaken for destination URLs.
    - **Committed Navigation Transition**: Only when the student commits navigation (presses Enter or taps a search suggestion) does the address bar lose focus (`node.isFocused() == false`), the keyboard dismisses, and the page renders. `WebClassifier` then evaluates the loaded destination with zero false positives during typing and zero bypasses upon page load.
---

### 19. Milestone 19: Notification Sound/Vibration & Background Notification Guard
- **Why It Was Mandated**:
  - **Silent / Non-Vibrating Alerts**: Students need unambiguous audio and tactile feedback for time-sensitive study warnings (30m, 15m, 5m, 1m, 30s before lockdown), lockdown activation, homework expiration, and unlock completion. Missing `VIBRATE` permission in `AndroidManifest.xml` and default channel settings caused alerts to be silent on several devices.
  - **Background Notification Hijacking**: Whenever a blocked app (e.g., WhatsApp, Discord, YouTube) received a background notification or performed background sync, Android dispatched an `AccessibilityEvent`. `LockAccessibilityService` was checking `isPackageBlocked(pkg)` on all event types, causing `enforceBlock()` to immediately launch QIEZKA full-screen over the user's active study session (e.g. Google Docs, Anki, Classroom), even though the user never touched or opened the notification.
- **What Was Implemented**:
  - **Hardware Haptic Feedback & Audio Notification Engine (`AlarmReceiver.java`, `AndroidManifest.xml`)**:
    - Added `<uses-permission android:name="android.permission.VIBRATE" />` to `AndroidManifest.xml`.
    - Configured notification channels (`qiezka_alerts_v2`, `qiezka_status_v2`) with `AudioAttributes.USAGE_NOTIFICATION_EVENT`, `NotificationManager.IMPORTANCE_HIGH`, lights, system notification sound, and a double-pulse vibration pattern (`new long[]{0, 350, 200, 350}`).
    - Added direct hardware `Vibrator` / `VibrationEffect` triggering in `showNotificationStatic()` to guarantee tactile vibration across aggressive OEM skins (OneUI, MIUI, ColorOS).
  - **Background Notification Interception Guard (`LockAccessibilityService.java`)**:
    - Restricted `lastForegroundPackage` updates: ONLY updates on `AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED` where `event.getPackageName()` is not `com.android.systemui`.
    - In `onAccessibilityEvent()`, `enforceBlock(pkg)` is now strictly guarded: it is ONLY triggered if the blocked app is actively entering the foreground (`TYPE_WINDOW_STATE_CHANGED`) or is confirmed as the active focused foreground window (`pkg.equals(detectCurrentForegroundPackage())`).
    - Background notifications alone (`TYPE_NOTIFICATION_STATE_CHANGED`, background `TYPE_WINDOW_CONTENT_CHANGED`) remain peacefully in the notification shade without yanking the user to QIEZKA.
    - If the user taps the notification from the shade to launch the blocked app, Android triggers a foreground `TYPE_WINDOW_STATE_CHANGED`, which QIEZKA immediately intercepts and blocks.

---

### 20. Milestone 20: PWA (WebAPK) Deep Inspection & Recents Task-Switching Guard
- **Why It Was Mandated**:
  - **PWA (WebAPK) & Trusted Web Activity (TWA) Reality**:
    - Progressive Web Apps and TWAs (such as KissKH, Loklok, KissAsian, DramaBox, etc.) appear on the home screen as WebAPK or TWA stubs (e.g. `id.kisskh.twa`, `org.chromium.webapk.*`).
    - *On Initial Launch*: The launcher briefly triggers the stub APK, which was caught by static package checks.
    - *On Android Recents Resume (Android 16 QPR1 AOSP & Galaxy A30 Android 11 One UI 2.x)*: The running task attaches directly to the Chromium host wrapper: `com.android.chrome/org.chromium.chrome.browser.customtabs.CustomTabActivity` (or `SameTaskWebApkActivity` / `WebappActivity`). When resumed from Recents, Android OS attaches directly to the running Chrome window. The package reported by WindowManager is `com.android.chrome`—NOT `id.kisskh.twa` or `org.chromium.webapk.*`.
    - Because `com.android.chrome` is an allowed browser tool, static package blacklisting returned `false` and routed the event to browser inspection.
  - **The Null Address Bar & The Single Gate Flaw**:
    - Standalone PWAs and TWAs run in fullscreen / standalone display mode with **no omnibox or address bar**.
    - `extractUrlFromBrowser()` always returned `null`.
    - Prior `WebClassifier.classify()` logic gated execution on `if (!isDestinationUrl(cleanUrl)) return allowed();`. When `cleanUrl` was null/empty, it immediately granted an automatic pass without ever checking the DOM, giving KissKH an automatic pass.
  - **Chromium View Nesting & `MAX_DOM_DEPTH` Barrier**:
    - Live forensic dumping via ADB on device revealed that in Chromium-based activities (`CustomTabActivity`, `WebView`), native view groups (`action_bar_root`, `edge_to_edge_base_layout`, `coordinator`, `compositor_view_holder`) consume the first 8-9 levels of the view hierarchy.
    - Web page elements (such as `"kisskh"`, video titles, `"continue watching"`, and episode tags) reside between depths 10 and 22.
    - Because `MAX_DOM_DEPTH` was previously capped at 8, DOM traversal terminated prematurely before reaching any HTML/web content.
  - **`CustomTabActivity` Exemption in `isStandalonePwa()`**:
    - `isStandalonePwa()` previously only audited `WebappActivity` and `WebApk`. It failed to identify `CustomTabActivity` used by TWAs (`id.kisskh.twa`) and standalone web wrappers.
  - **WindowManager Window Title Blindness on Android 11+ Custom Tabs**:
    - On Android 11 One UI and Android 16 AOSP, `AccessibilityWindowInfo.getTitle()` returns `null` or empty for `CustomTabActivity` windows. Relying solely on window title checks caused the engine to skip classification.
- **Architectural Enhancements**:
  - **Z-Order Window Hierarchy Audit for Blocked Tasks (`detectCurrentForegroundPackage()`)**:
    - Audits all application windows in `getWindows()`. If any application window in the active task belongs to a blacklisted package (e.g. `id.kisskh.twa`), `detectCurrentForegroundPackage()` returns that blocked package directly, bypassing Chrome's foreground wrapper.
  - **Deep DOM Traversal with Safety Budget (`WebClassifier.java`)**:
    - Increased `MAX_DOM_DEPTH` from 8 to 25 to penetrate Chromium compositor view holders and inspect live web elements.
    - Added `MAX_NODE_SCAN_COUNT = 350` to guarantee high-performance execution (<2ms) without UI thread lag.
  - **CustomTab & Standalone WebApp Interception (`LockAccessibilityService.java`)**:
    - Expanded `isStandalonePwa()` to include `customtab`, `customtabs`, and `customtabactivity`.
    - Guaranteed fallback: In any browser package where `url == null` (no address bar present), the engine **always** audits the DOM via `WebClassifier.classifyStandalonePwa()` instead of granting an automatic pass.
  - **Streaming & Drama Token Signatures (`WebClassifier.java`)**:
    - Expanded `DOM_PIRACY_TOKENS` with live tokens extracted from device dumps: `"continue watching"`, `"lastest update"`, `"latest update"`, `"top k-drama"`, `"top c-drama"`, `"k-drama"`, `"c-drama"`, `"watch history"`, `"sign in now to save your watch history"`.
    - Integrated direct matching against `BlacklistConstants.isBlacklisted("", val)`.
  - **Continuous Ticker Hardening**:
    - Updated `inspectBrowserForBlockedPwaOrContent()`: when hostile web content or blocked URLs are caught by the background ticker, the engine invokes `enforceBlock(pkg)` (minimizing to Home + bringing QIEZKA Lock to front with retry post-delays) rather than merely pressing Back.
  - **Runtime Event Subscription Integrity**:
    - Maintained programmatic subscription to `TYPE_WINDOW_STATE_CHANGED`, `TYPE_WINDOWS_CHANGED`, `TYPE_WINDOW_CONTENT_CHANGED`, `TYPE_VIEW_CLICKED`, `TYPE_VIEW_FOCUSED`, and `TYPE_VIEW_SCROLLED`.
  - **Direct Lock Screen Redirection (`enforceBlock()`) — Zero Home Redirection**:
    - Completely eliminated `goHome()` / `GLOBAL_ACTION_HOME` from the eviction pathway. Previously, simultaneously dispatching `goHome()` and `launchLockOverlay()` created an OS animation race condition where the launcher and QIEZKA fought each other in a visible stutter loop until one side won.
    - All blocked, unwanted, or distracting apps/PWAs now point directly and instantly to QIEZKA Lock (`MainActivity`) via `FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TOP | FLAG_ACTIVITY_SINGLE_TOP | FLAG_ACTIVITY_REORDER_TO_FRONT`, immediately locking the device without ever bouncing through the Home launcher.

---


## 🔮 Future Roadmap & Ecosystem Forks

This section outlines features, architectural milestones, modular ecosystem forks, and cross-fork synchronization strategies planned for the QIEZKA platform, explaining **why** each component is needed, **how** it will be engineered, and the **feasibility analysis** behind experimental concepts.

---

### 1. Core Roadmap Enhancements (QIEZKA Base)

#### 1.1 Fully Offline On-Device OCR (Google ML Kit Text Recognition)
- **Why It Is Needed**:
  - Currently, extracting text from photographed homework relies on OCR.space's REST API. If the student has poor internet connectivity, rate-limit issues, or an expired API key, OCR extraction fails.
- **Proposed Implementation**:
  - Integrate `@google-mlkit/text-recognition` into Android native code as a Capacitor plugin method (`recognizeTextOffline(base64Image)`).
  - Runs fully on-device on the Neural Processing Unit (NPU/CPU) via Google Play Services in under 300ms with zero network traffic.
  - Uses OCR.space as an optional secondary fallback for complex multi-column handwritten equations.

#### 1.2 Fine-Grained Allowed App Time Quotas
- **Why It Is Needed**:
  - Whitelisting essential communication apps (like WhatsApp or Telegram) or web browsers is necessary for asking classmates questions or researching, but students can easily get sidetracked and spend 45 minutes chatting instead of studying.
- **Proposed Implementation**:
  - Add a configurable per-session time quota (e.g. max 10 minutes total for messaging apps per 90-minute study session).
  - In `LockAccessibilityService.java`, track cumulative foreground time for secondary whitelisted apps. Once the quota is exhausted, the accessibility service displays a floating toast `"Messaging quota reached for this study session"` and minimizes the app back to QIEZKA.

#### 1.3 Encrypted Cloud Backup & Multi-Device Synchronization
- **Why It Is Needed**:
  - Currently, backups are exported as plaintext JSON files via Android's Storage Access Framework (SAF). Students who switch devices or reinstall the OS must manually transfer JSON files, and storing raw Gemini API keys in plaintext presents a security concern.
- **Proposed Implementation**:
  - Implement client-side AES-256-GCM encryption with a user-defined password before writing backup files or uploading to Google Drive / WebDAV.
  - Decrypts only on the target device when the correct master passphrase is provided.

#### 1.4 Academic Performance Analytics & Study Streak Tracking
- **Why It Is Needed**:
  - Students want positive reinforcement and visual feedback on their long-term focus habits.
- **Proposed Implementation**:
  - In `Dashboard.tsx`, add an "Academic Insights" analytics card showing:
    - Weekly total focus minutes.
    - Ratio of passed homeworks vs failed consequence triggers.
    - Consecutive day study streak counter.
    - Visual timeline of daily focus sessions.

---

### 2. Planned Ecosystem Forks & Specialized Sister Apps

QIEZKA's hardcore accessibility lockdown engine, foreground window interceptor, and anti-tamper architecture can be adapted into focused sister applications for distinct behavioral challenges:

```mermaid
flowchart TD
    CORE["⚡ Base QIEZKA<br/>(Homework Verification Engine)"]
    
    FORK1["🧠 Quiz<br/>• In-App Quiz & Drills<br/>• Language Learning<br/>• Reading Enforcement"]
    FORK2["🔗 Bridge<br/>• External App Gating<br/>• Duolingo / Anki / Khan<br/>• Score Threshold Release"]
    FORK3["🌊 Flow<br/>• Zero-Schedule Engine<br/>• Target App Interceptor<br/>• Autonomous Anti-Doomscrolling"]

    CORE -.->|Modular Fork| FORK1
    CORE -.->|Modular Fork| FORK2
    CORE -.->|Modular Fork| FORK3
```

#### 2.1 Quiz: Active Recall & Reading Enforcement Fork
- **Core Concept**:
  - Replaces physical homework camera grading with **instant interactive knowledge verification**. To unlock your phone or exit lockdown, you must actively pass a rigorous quiz generated by QIEZKA's AI engine or curated question banks.
- **Target Workflows & Integrations**:
  - **Language Learning Drills**: Daily vocabulary memorization, kanji/hanzi character recognition, verb conjugations, and grammar drills. The phone remains locked until the user scores ≥90% on their daily language set.
  - **Reading Enforcement & Comprehension Gate**: Students often claim to have "read" assigned chapters while merely daydreaming or skimming. In this mode, the user assigns an article, textbook chapter, or PDF; QIEZKA analyzes the text via Gemini and generates specific conceptual questions that can only be answered if the text was genuinely read and understood. Skim-and-cheat is impossible.
- **Unlock Condition**: High-accuracy quiz completion (e.g. 5/5 correct answers with randomized question ordering and timer limits).

#### 2.2 Bridge: External Study & Quiz App Enforcement Fork
- **Core Concept**:
  - Instead of taking quizzes inside QIEZKA, device unlocking is tied directly to **external third-party learning applications**.
- **How It Operates**:
  - The student selects their target external learning app (e.g., Duolingo, Anki, Quizlet, Khan Academy, LeetCode, or a university study portal).
  - QIEZKA places the device in lockdown, whitelisting *only* the chosen educational app and emergency dialers.
  - **Score / Milestone Verification**: The student must achieve a designated score, complete a set number of flashcards, or earn a target XP count in the external app.
  - **Verification Engine**: Android Accessibility inspection scans the external app's active UI tree (detecting "Lesson Complete", "Score: 100%", "Deck Finished") or queries Android `UsageStatsManager` / Intent result callbacks to confirm genuine task completion before lifting the system lockdown.

#### 2.3 Flow: Autonomous Zero-Schedule Anti-Doomscrolling Fork
- **Core Concept**:
  - In current QIEZKA, lockdown requires explicit scheduling or active task creation. **Flow eliminates schedules and tasks entirely.** It operates silently in the background as a permanent algorithmic circuit breaker.
- **Autonomous Detection Engine**:
  - Continuously monitors foreground activity across identified short-form dopamine pit apps: Instagram Reels, TikTok, YouTube Shorts, X/Twitter, Reddit, Facebook Reels.
  - Uses `LockAccessibilityService` scroll rate detection and foreground session timers to recognize doomscrolling behavior in real-time (e.g., >7 rapid vertical swipes within 30 seconds, or uninterrupted continuous consumption exceeding 10 minutes).
- **Autonomous Interventional Lock**:
  - When doomscrolling is detected, Flow automatically yanks the foreground, displays a full-screen cool-off lockdown or floating intervention banner, and enforces a mandatory dopamine detox cooldown (e.g. 15–30 minutes) without the user ever having had to pre-plan a study session.

---

### 3. Cross-Fork Classifier & Blocklist Management Architecture

A major engineering challenge across multiple forked apps is maintaining blocklists, categorizations, and link filters: *if a student discovers a new distraction domain or updates a classifier rule, modifying and recompiling every single fork independently is tedious and unsustainable.*

QIEZKA plans two viable architectural solutions to this distribution problem:

```mermaid
flowchart LR
    subgraph ModelA ["Model A: Mandatory Offline Companion Hub (Central / Base QIEZKA)"]
        HUB["📱 Central / Base QIEZKA<br/>(Master Classifier & Rule Hub)<br/>• 100% Offline<br/>• Sole App Updated via APK"]
        F1["🧠 Quiz"]
        F2["🔗 Bridge"]
        F3["🌊 Flow"]
        HUB -.->|Mandatory Runtime Gate<br/>(Forks refuse to run if Central missing)| F1
        HUB -.->|Mandatory Runtime Gate<br/>(Forks refuse to run if Central missing)| F2
        HUB -.->|Mandatory Runtime Gate<br/>(Forks refuse to run if Central missing)| F3
        HUB ==>|Local IPC: Blocklists & Classifier| F1
        HUB ==>|Local IPC: Blocklists & Classifier| F2
        HUB ==>|Local IPC: Blocklists & Classifier| F3
    end

    subgraph ModelB ["Model B: Standalone Dynamic Remote Pull (Online Model)"]
        GH["🌐 Hardcoded Remote Resource<br/>(GitHub Raw / CDN Manifest)"]
        B1["🧠 Quiz"]
        B2["🔗 Bridge"]
        B3["🌊 Flow"]
        GH -.->|Direct Async HTTPS Pull| B1
        GH -.->|Direct Async HTTPS Pull| B2
        GH -.->|Direct Async HTTPS Pull| B3
    end
```

#### Model A: Mandatory Offline Companion App (`Central` - Companion Hub)
- **Mandatory Runtime Gating (Required Prerequisite)**:
  - Every fork (`Quiz`, `Bridge`, `Flow`) is **required to run ONLY if the companion app (`Central` / Base QIEZKA) is installed, working, and actively running in the background**.
  - If the companion app is missing, stopped, or disabled, the forked apps immediately refuse to execute or enforce lockdown.
- **Single-App Update Architecture (Zero-Fork Recompilation)**:
  - **The Problem It Solves**: In an offline ecosystem, if you want to add a specific unlisted link or an updated classifier heuristic, updating every single fork would be exhausting.
  - **The Solution**: You **only download/update ONE app: an updated QIEZKA companion app APK**.
  - **Zero Touching of Forked Apps**: Because every fork queries `Central` via local Android IPC (`ContentProvider` / AIDL Binder) for real-time decisions on what to block and classify, downloading an updated QIEZKA companion app immediately updates the classification and blocking rules across all forked apps—**without having to update, recompile, or reinstall any of the forks!**

#### Model B: Standalone Dynamic Remote Pull (Online Model)
- **Role & Philosophy**: If the apps are allowed to run online without a BYOK Model, the need for a secondary companion app is eliminated.
- **Hardcoded Remote Source Endpoints**:
  - Each fork operates completely standalone and contains hardcoded links/endpoints to trusted remote resources (e.g. version-controlled GitHub Raw JSON manifests or remote rulebooks).
  - The forks always pull and update their classifiers, unlisted links, and blocklists directly from the hardcoded resources on launch or network availability.
  - Operates completely standalone with local caching and offline fallback.

---

### 4. Feasibility Analysis: Speculative & High-Risk Forks ("On-Hold / Not Recommended")

Several potential forks have been proposed but are currently deemed **impractical or fundamentally vulnerable to anti-cheat bypasses** under QIEZKA's "Zero Honor System" doctrine:

| Proposed Fork | Concept | Fatal Vulnerabilities & Anti-Cheat Failure Modes | Feasibility Verdict |
| :--- | :--- | :--- | :---: |
| **🏋️ QIEZKA Fitness** | Unlock device by completing physical workout (running, lifting, pushups). | • **Camera Spoofing**: User points camera at a laptop screen playing an online YouTube workout video or holds up a static photo.<br/>• **Smartwatch Shaking**: Smartwatch accelerometers and step counters can be faked by shaking the wrist or attaching the watch to a household fan/pet.<br/>• **Hardware Barrier**: Requires expensive smartwatches with real-time PPG heart-rate telemetry to even approximate effort. | ⚠️ **On Hold** (High bypass risk; unfeasible without tamper-proof biometric heart-rate variance). |
| **🧹 QIEZKA Chores** | Unlock device by doing dishes, cleaning bedroom, or folding laundry. | • **Static Photo Re-use**: Taking a photo of an already clean corner of the room or reusing an old photograph.<br/>• **Zero Objective Ground Truth**: AI cannot determine if a bed was made today or last month, or if the user was the person who cleaned it.<br/>• **Stageability**: Placing two plates in a dish rack to simulate a full chore. | ❌ **Rejected / Impractical** (Fundamentally relies on the honor system). |
| **🎨 QIEZKA Creative** | Unlock device by drawing, painting, or writing poetry/fiction. | • **Plagiarism & Image Download**: Downloading artwork or text from Pinterest, Google Images, or AI generators and photographing the monitor.<br/>• **Subjective Effort**: No algorithmic metric can verify whether a 5-minute sketch represents honest effort or deliberate evasion. | ❌ **Rejected / Impractical** (Impossible to verify effort objectively without continuous surveillance). |

---

## 💻 Tech Stack

- **Core Framework**: [React 19](https://react.dev/), [TypeScript 5.8](https://www.typescriptlang.org/), [Vite 6](https://vitejs.dev/)
- **Mobile Native Bridge**: [Capacitor 7](https://capacitorjs.com/) (`@capacitor/android`, `@capacitor/filesystem`)
- **Native Android Engine**: Java 17 (Accessibility Service, Floating WindowManager Overlay, DevicePolicyManager, BroadcastReceiver, ContentResolver SAF)
- **AI & Evaluation**: [@google/genai](https://www.npmjs.com/package/@google/genai) (Google Gemini 2.0 / 2.5 Flash & Pro) + OCR.space
- **Document Ingestion**: [Mammoth.js](https://github.com/mwilliamson/mammoth.js) (Offline `.docx` to HTML/Text conversion)
- **Styling & UI**: [Tailwind CSS 4](https://tailwindcss.com/), [Motion / Framer Motion](https://motion.dev/), [Lucide React](https://lucide.dev/)

---

## 🛠️ Development & Building

### Prerequisites
- Node.js 18+ & npm
- Android Studio (Ladybug or newer) & Android SDK 34+
- Android platform-tools (ADB) in your system PATH

### Commands
```bash
# 1. Install dependencies
npm install

# 2. Run web dev server
npm run dev

# 3. Build web distribution bundle
npm run build

# 4. Sync web bundle and native plugins to Android
npx cap sync android

# 5. Open Android project in Android Studio
npx cap open android
```

> [!IMPORTANT]
> **Compilation Notice**: Always run `npm run build` followed by `npx cap sync android` whenever you modify frontend components in `src/` or bridge definitions to keep the Android assets in sync.

---

## ❓ Frequently Asked Questions (FAQ)

<details>
<summary><b>1. Why is schedule duration capped at 90 minutes?</b></summary>
Cognitive science and chronobiology demonstrate that sustained human focus operates on **ultradian rhythms** (natural biological cycles lasting approximately 90 minutes). Setting multi-hour lockdown sessions leads to diminishing cognitive returns, mental exhaustion, and burnout. 90 minutes represents the gold standard for high-intensity, undistracted academic productivity.
</details>

<details>
<summary><b>2. What if there is an urgent phone call or family emergency?</b></summary>
Emergency dialer functions and incoming phone calls (`com.android.phone`, `com.google.android.dialer`, `com.samsung.android.dialer`) are completely exempt and will never be blocked. You can always answer incoming phone calls and make emergency calls.
</details>

<details>
<summary><b>3. Why can't I access Android Settings during a lockdown?</b></summary>
If the Android Settings application were accessible, a student facing academic frustration could simply open Settings, revoke Device Administrator or Accessibility permissions, and abort the lockdown. Blocking the Settings app seals this bypass vector completely.
</details>

<details>
<summary><b>4. Does QIEZKA require root or bootloader unlocking?</b></summary>
No. QIEZKA operates 100% within the standard Android framework using official APIs: `AccessibilityService`, `DeviceAdminReceiver`, `AlarmManager`, and `WindowManager`. No root, no Magisk/KernelSU, and no bootloader unlocking are required.
</details>

<details>
<summary><b>5. Are my homework photos and notes stored on any server?</b></summary>
Dawg No 😭🙏. QIEZKA has zero central servers, zero accounts, and zero proprietary backends. Your homework photographs and study notes stay on your device. During grading, photos are transmitted directly from your phone to Google Gemini / OCR.space over encrypted HTTPS using your personal API key.
</details>

---

## 📄 License

This project is licensed under the [MIT License](LICENSE).

</div>
