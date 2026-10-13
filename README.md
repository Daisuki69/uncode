# 🛡️ QIEZKA (uncode)

> **The Hardcore, AI-Enforced Academic Lockdown & Homework Accountability Platform for Android.**

[![Android Native](https://img.shields.io/badge/Android-100%25%20Kotlin-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![React](https://img.shields.io/badge/Frontend-React%2018%20%2B%20TypeScript-61DAFB?logo=react&logoColor=black)](https://react.dev/)
[![Capacitor](https://img.shields.io/badge/Bridge-Capacitor%207-119EFF?logo=capacitor&logoColor=white)](https://capacitorjs.com/)
[![Gemini AI](https://img.shields.io/badge/AI%20Engine-Google%20Gemini-4285F4?logo=google&logoColor=white)](https://deepmind.google/technologies/gemini/)
[![License](https://img.shields.io/badge/License-Proprietary-red)](#)

---

## 📖 Overview

**QIEZKA** (also known as *uncode*) is an unapologetically rigorous Android focus and accountability system engineered for students who cannot afford distractions. 

Unlike standard "screen time" apps that can be easily dismissed, bypassed, force-stopped, or worked around, QIEZKA combines **military-grade Android OS-level enforcement** with **real-time AI homework evaluation**. It physically locks distracting apps, games, social feeds, and web browsers behind strict academic obligations.

To regain access to your device, you must do one thing: **complete and submit your assigned homework**.

```
                           ┌────────────────────────────────────────┐
                           │            QIEZKA SYSTEM               │
                           └──────────────────┬─────────────────────┘
                                              │
                    ┌─────────────────────────┴─────────────────────────┐
                    ▼                                                   ▼
       ┌────────────────────────┐                          ┌────────────────────────┐
       │   STAGE 1: APP GATE    │                          │   STAGE 2: WEB GATE    │
       │  • SIREN Role Assertion│                          │  • Local DNS Sinkhole  │
       │  • Launcher Eviction   │                          │  • Real-time DOM Audit │
       │  • Anti-Tamper Shield  │                          │  • Link-Shim Forwarding│
       └────────────────────────┘                          └────────────────────────┘
                    │                                                   │
                    └─────────────────────────┬─────────────────────────┘
                                              ▼
                           ┌────────────────────────────────────────┐
                           │          AI HOMEWORK ENGINE            │
                           │   • Physical Camera Document Scan      │
                           │   • OCR Text & Rubric Analysis         │
                           │   • Gemini Multimodal Grading & Audit  │
                           └──────────────────┬─────────────────────┘
                                              │
                                 ┌────────────┴────────────┐
                                 ▼                         ▼
                         [ PASS (≥ Threshold) ]    [ FAIL / EXPIRED ]
                                 │                         │
                                 ▼                         ▼
                         UNRESTRICTED ACCESS       CONSEQUENCE LOCKDOWN
```

---

## ⚡ Core Pillars

### 1. 🔒 Dual-Gate Lockdown & Enforcement Engine

QIEZKA operates a layered, multi-tier defense architecture to ensure that no digital dopamine loops slip through:

#### Stage 1: Application & OS-Level Isolation
- **SIREN HomeProxy**: Intercepts home gestures and default launcher transitions. Ensures that during lockdown, unauthorized launchers or app drawers cannot be accessed.
- **Language-Agnostic Launcher Eviction**: Dynamically detects launcher surfaces across all device locales and languages without hardcoded strings.
- **Anti-Fast-Click Shield**: Neutralizes high-frequency spam clicks and race conditions designed to bypass system dialogs and permission gates.
- **Android 14+ BAL Compliance**: Strict Background Activity Launch (BAL) conformance, ensuring rock-solid service stability on modern Android versions (Android 12 through 15+).
- **Intelligent App Whitelisting**: Allows permitted study utilities (calculators, dictionary, LMS platforms like Canvas/Google Classroom, note-taking apps) while isolating blacklisted apps. Newly installed apps passing detection filters are automatically added to the whitelist.

#### Stage 2: Web & In-App Browser Intelligence
- **Local DNS Sinkhole VPN (`LocalDnsVpnService`)**: Intercepts DNS queries locally on device without external proxies. Instantly sinkholes blacklisted domains (TikTok, Instagram, adult, gambling, piracy, gaming arenas) to `0.0.0.0` / NXDOMAIN.
- **Accessibility DOM Audit (`LockAccessibilityService`)**: Real-time inspection of browser address bars, in-app WebViews (`BrowserLiteActivity`, Custom Tabs), and DOM trees. Evaluates against comprehensive multi-genre blocklists and academic exemption rules.
- **Universal Link-Shim & Redirection Forwarding**: Platform redirect wrappers (e.g., `l.facebook.com`, `lm.facebook.com`, `google.com/url`, `t.co`) are permitted through DNS to allow HTTP 302 redirects to resolve. The accessibility engine applies progressive settling delays (300ms–2000ms), auditing the **settled destination website** rather than being tricked by redirect shims or leftover query parameters (such as `dest=chatgpt.com`).

---

### 2. 🧠 AI-Powered Homework Evaluation

- **Physical Document Camera Capture**: Capture high-resolution multi-page photos of handwritten or printed homework.
- **OCR Text Extraction**: Extracts text, equations, and problem sets from document images.
- **Customizable Rubrics**: Instructors or students can configure custom rubrics with specific grading criteria, question weights, passing score thresholds, and difficulty levels.
- **Gemini Multimodal Grading**: Analyzes homework submissions against the assigned rubric, evaluating conceptual accuracy, completeness, and effort. Returns a numerical score, granular per-criterion breakdown, and constructive critique.
- **Automatic ID Guarantees**: Every homework item is assigned a globally unique, sanitized identifier (`hw_<timestamp>_<random>`) to prevent state corruption, orphaned consequence sessions, or deduplication errors.

---

### 3. ⏳ Study Schedules & Consequence Mode

- **Flexible Study Schedules**: Define recurring or one-off focus windows (e.g., Weekday study blocks from 4:00 PM to 7:00 PM).
- **Automated Deadline Checks**: Homework must be submitted and scored above the passing threshold before the schedule begins.
- **Consequence Mode**: If homework is unsubmitted or receives a failing score, the device enters strict Consequence Lockdown.
- **PIN-Protected Admin Override**: Parents or students can set a secure PIN to adjust settings, emergency overrides, or calibrate sensitivity.

---

## 🛠️ Technology Stack

| Layer | Technologies & Frameworks | Description |
|---|---|---|
| **Android Native Engine** | **100% Kotlin**, AndroidX, Coroutines | Core accessibility service, VPN DNS sinkhole, role management, broadcast receivers, and system overlays. All 24 native components are 100% Kotlin. |
| **Hybrid Bridge** | **Capacitor 7** (`@capacitor/core`) | High-performance asynchronous bridge connecting the React frontend to native Kotlin capabilities (`LockPlugin.kt`). |
| **Frontend UI** | **React 18**, **TypeScript**, **Vite** | Modern, responsive, dark-mode-first user interface with smooth transitions and modular component design. |
| **Styling & Icons** | **Tailwind CSS**, **Lucide React** | Sleek glassmorphism aesthetics, dynamic status indicators, and clean typography. |
| **AI & Intelligence** | **Google Gemini API**, **OCR.space** | Multimodal homework grading, rubric compliance scoring, and OCR text recognition. |

---

## 📂 Project Architecture

```
uncode/
├── android/                             # Android Native Project (100% Kotlin)
│   └── app/src/
│       ├── main/
│       │   ├── java/com/uncode/app/     # Core Native Kotlin Components (24 Files)
│       │   │   ├── LockAccessibilityService.kt # Real-time DOM audit & app eviction
│       │   │   ├── LocalDnsVpnService.kt       # Local DNS packet filter & sinkhole
│       │   │   ├── LockPlugin.kt               # Capacitor native bridge
│       │   │   ├── WebClassifier.kt            # On-device web classification brain
│       │   │   ├── AppClassifier.kt            # Package & in-app browser detector
│       │   │   ├── WebBlocklistConstants.kt    # Domain sets & link-shim definitions
│       │   │   ├── KnownDistractingWeb.kt      # Distraction domains & regex patterns
│       │   │   ├── KnownSafeWeb.kt             # Academic safe-list & search VIPs
│       │   │   ├── HomeRoleTruthTable.kt       # State machine for launcher roles
│       │   │   ├── FloatingOverlayService.kt   # System-level lock overlay
│       │   │   ├── ScheduleManager.kt          # Alarm & focus schedule dispatcher
│       │   │   └── MainActivity.kt             # Android entry point & Capacitor host
│       │   ├── AndroidManifest.xml
│       │   └── res/
│       └── test/java/com/uncode/app/    # Native JVM Unit Tests
│           ├── WebTruthTableTest.kt            # Web classifier & link-shim tests
│           ├── HomeHandlerTest.kt              # Launcher & in-app browser tests
│           └── UnifiedPolicyTest.kt            # Unified service policy tests
├── src/                                 # Frontend UI (React + TypeScript)
│   ├── App.tsx                          # Main application state & navigation
│   ├── types.ts                         # Type definitions & homework sanitizers
│   ├── components/                      # UI Components
│   │   ├── Dashboard.tsx                # Status dashboard & lockdown indicators
│   │   ├── HomeworksPage.tsx            # Homework lists, rubric cards & camera
│   │   ├── CreateSchedule.tsx           # Focus session scheduler
│   │   ├── SettingsOverlay.tsx          # Whitelist & system configuration
│   │   └── PermissionWalkthrough.tsx    # Step-by-step Android permission wizard
│   ├── services/                        # AI & API services
│   └── index.css                        # Design system & global styles
├── public/                              # Static web assets & audio effects
├── HISTORY.md                           # Complete chronological patch changelog
└── README.md                            # General project overview & documentation
```

---

## 🚀 Getting Started

### Prerequisites

- **Node.js**: `v18+` or `v20+`
- **npm**: `v9+`
- **Java Development Kit (JDK)**: `JDK 17` or `JDK 21`
- **Android SDK**: Android 12 (API 31) through Android 15 (API 35)
- **Android Device**: Physical Android device with Developer Options and USB/Wireless Debugging enabled.

### Installation & Build Workflow

1. **Clone the repository**:
   ```bash
   git clone https://github.com/Daisuki69/uncode.git
   cd uncode
   ```

2. **Install frontend dependencies**:
   ```bash
   npm install
   ```

3. **Build web assets & synchronize Capacitor**:
   ```bash
   npm run build
   npx cap sync android
   ```

4. **Run Native Unit Tests**:
   ```bash
   cd android
   .\gradlew.bat testDebugUnitTest
   ```

5. **Build the Android APK**:
   ```bash
   cd android
   .\gradlew.bat assembleDebug
   ```
   *(Or open `android/` directly in **Android Studio** and select **Build > Build Bundle(s) / APK(s) > Build APK(s)**).*

---

## 📜 Patch History & Technical Documentation

For the complete, unabridged chronological patch history, forensic bug investigations, and architectural decision logs (from **Patch 1** through **Patch 28 Follow-Up 7**), refer to:

👉 **[HISTORY.md](HISTORY.md)**

---

## 🔒 Security & Privacy

- **All DNS filtering is performed strictly on-device** using `LocalDnsVpnService`. No browsing logs or DNS queries are transmitted to third-party tracking servers.
- **Homework photos are processed exclusively for grading**. When using the Gemini API, only relevant homework images and rubrics are sent via secure HTTPS to Google's API endpoints.
- **Zero data selling or behavioral advertising**. QIEZKA is built strictly as a focus utility.
