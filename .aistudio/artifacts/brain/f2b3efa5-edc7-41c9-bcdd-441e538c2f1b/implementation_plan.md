# AI Study Guard Backend & Intelligent Screen Monitor

Integrate Google Gemini 3.5 Flash backend for real-time study vs distraction screen content classification during active study sessions. Enforce a 1-warning followed by 3-hour app lock progression for non-study activity across YouTube, AI chatbots, and browsers, while strictly whitelisting and never monitoring essential apps (Phone, Contacts, Camera, Gallery, Settings, and Payment apps).

## User Review & Critical Decisions

> [!IMPORTANT]
> The following design decisions were confirmed via interactive clarification:
> - **Infraction Progression**: Exactly 1 warning on first distraction detection, followed by an immediate 3-hour lock on subsequent non-study usage.
> - **Strict App Whitelist**: Phone, Contacts, Camera, Gallery, Settings, and all Payment apps (PhonePe, Paytm, PayPal, FamPay, UPI) are strictly excluded from AI Guard monitoring and will never be blocked.

- **Confirmed Decision 1**: 1 warning then 3-hour lock on next distraction.
- **Confirmed Decision 2**: Strict whitelist for Phone, Contacts, Camera, Gallery, Settings, and Payment apps.
- **API Model Upgrade**: Upgrading from deprecated `gemini-2.0-flash` to official `gemini-3.5-flash` REST endpoint as per Google AI Studio guidelines.

---

## 1. Overview & Core Concept

When a user initiates an active study session (timer or auto-schedule), the AI Study Guard Sentinel actively monitors user screen context for distractions without burdening the user during legitimate study:
1. **Intelligent Screen Analysis**: Screen text and video/chat content are evaluated in real time via Gemini 3.5 Flash and local semantic caches.
2. **YouTube & Shorts**: Detects Shorts/Reels or entertainment video content.
3. **AI Chatbots & Web Browsers**: Detects casual/non-study chatting or entertainment browsing in apps like ChatGPT, Claude, DeepSeek, and mobile browsers.
4. **Progression Mechanism**:
   - **1st Infraction**: High-priority warning heads-up alert and overlay prompt explaining the distraction.
   - **2nd Infraction (Ignoring Warning)**: Enforces a strict 3-hour punishment lock on the distracting app.
5. **Absolute Whitelist Guarantee**: Dialer, Contacts, Camera, Gallery, Device Settings, and Payment apps (PhonePe, Paytm, PayPal, FamPay, Google Pay) are immediately bypassed without inspection or blocking.

---

## 2. Technical Architecture & Component Flow

```
┌────────────────────────────────────────────────────────┐
│             Active Study Session (Timer/Schedule)      │
└──────────────────────────┬─────────────────────────────┘
                           │ Accessibility Event
                           ▼
┌────────────────────────────────────────────────────────┐
│       AiStudyAccessibilityService Gatekeeper           │
│ 1. Is self (Focivo)? ──> Skip                          │
│ 2. Is in Strict Whitelist?                             │
│    (Phone, Contacts, Camera, Gallery, Settings,        │
│     Payment apps: PhonePe, Paytm, PayPal, FamPay)      │
│    ──> IMMEDIATELY SKIP & NEVER MONITOR                │
│ 3. Is study period active? ──> If NO, Skip             │
└──────────────────────────┬─────────────────────────────┘
                           │ Monitored App (YouTube, AI Chat, Browser)
                           ▼
┌────────────────────────────────────────────────────────┐
│             Content Extraction & Classification        │
│  - YouTube: Shorts screen check OR video title check   │
│  - AI Chat / Browser: Message / screen text extraction │
│  - Evaluator: StudyGuardKnowledge + Gemini 3.5 Flash   │
└──────────────────────────┬─────────────────────────────┘
                           │ Verdict: NON_STUDY / DISTRACTION
                           ▼
┌────────────────────────────────────────────────────────┐
│             AiStudyGuardManager Progression            │
│  - Offense 1: Stage 1 Warning Alert + Grace Overlay    │
│  - Offense 2 (Ignored): Stage 2 3-Hour Lockout        │
│    ──> BlockedAppLockActivity (180 mins remaining)     │
└────────────────────────────────────────────────────────┘
```

---

## 3. Key Implementation Steps

### Step 1: Upgrading Gemini Content Classifier
- Update model endpoint from deprecated `gemini-2.0-flash` to `gemini-3.5-flash` in `GeminiContentClassifier.kt`.
- Refine prompt instructions and JSON parsing to accurately classify screen and chat text into STUDY vs NON_STUDY with clear reasons.
- Ensure proper error-handling and fallback to on-device rules if network or API keys are unavailable.

### Step 2: Strict App Whitelist in EssentialAppsGuard
- Expand `EssentialAppsGuard.kt` to guarantee coverage of:
  - Phone / Dialer: `com.google.android.dialer`, `com.android.dialer`, `com.samsung.android.dialer`, and all dialer intents.
  - Contacts: `com.google.android.contacts`, `com.android.contacts`, etc.
  - Camera: `com.google.android.GoogleCamera`, `com.android.camera`, etc.
  - Gallery: `com.google.android.apps.photos`, `com.sec.android.gallery3d`, etc.
  - Device Settings: `com.android.settings`, `com.miui.securitycenter`, `com.coloros.settings`, etc.
  - Payment Apps: `com.phonepe.app`, `net.one97.paytm`, `com.paypal.android.p2pmobile`, `com.fampay.in`, `com.google.android.apps.nbu.paisa.user`, and banking apps.
- Remove YouTube from `PERMANENTLY_PROTECTED_PACKAGES` in `EssentialAppsGuard` so YouTube Shorts and video content can be evaluated during study sessions.

### Step 3: Screen & Chat Activity Monitoring in Service
- In `AiStudyAccessibilityService.kt`:
  - Verify that whitelisted apps immediately return without inspection.
  - YouTube: Detect Shorts screens and entertainment video titles using Gemini 3.5 Flash.
  - AI Apps & Browsers: Extract chat messages or active webpage content; evaluate with Gemini 3.5 Flash.
  - Connect non-study detections to `AiStudyGuardManager.onNonStudyDetected(context, pkg, text, reason)`.

### Step 4: 1-Warning-Then-3-Hour-Block Progression
- Verify that `AiStudyGuardManager`:
  - Records offense count per package during study sessions.
  - Count 1 triggers `triggerStage1Warning` (in-app warning overlay + heads-up alert).
  - Count 2+ triggers `triggerStage2Block` (sets 180-minute punishment lock in preferences and launches `BlockedAppLockActivity`).

### Step 5: Verification & Compilation
- Run `compile_applet` to ensure the build compiles cleanly without syntax or library errors.
