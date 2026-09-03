# Dearly — Technical Specification

> **Version**: 0.4 — implementation and evidence update
> **Created**: 2026-08-01 · **Updated**: 2026-08-30
> **Stack**: Android (Kotlin + Jetpack Compose) · Backend (Golang) · AI (ECAPA-TDNN SV/SID + OpenAI LLM)  
> **Deadline**: Friday, 28 August 2026

---

## 1. Project Overview

**Dearly** is an elder-care mobile application designed to support elderly users and their caregivers. The app simplifies daily life for seniors through large-button UI, voice interaction, medication reminders, call management, and caregiver coordination — all secured by speaker verification.

The app satisfies the academic project requirements:
- **Requirement 1**: Train/evaluate a speaker verification (SV) or speaker identification (SID) model.
- **Requirement 2**: Integrate that model into a complete, voice-driven virtual assistant with SV-protected and SID-personalized functions.

---

## 2. Target Users

| Role | Description |
|------|-------------|
| **Elder (Primary User)** | 60+ years old. Minimal tech literacy. Uses large-text UI, voice commands, and simplified navigation. |
| **Caregiver** | Family member or professional. Sets up the elder's profile, manages contacts, medications, and monitors activity. |

---

## 3. Tech Stack

### 3.1 Mobile (Android)

| Layer | Technology |
|-------|-----------|
| Language | Kotlin |
| UI Framework | Jetpack Compose |
| Navigation | Navigation Compose |
| State Management | ViewModel + StateFlow / UiState pattern |
| Dependency Injection | Hilt |
| Networking | Retrofit + OkHttp |
| Local Storage | Room (offline cache) |
| Audio Recording | Android AudioRecord / MediaRecorder API |
| Authentication | Firebase Auth (Phone OTP + Google Sign-In) |
| Push Notifications | Firebase Cloud Messaging (FCM) |

### 3.2 Backend (Golang)

| Layer | Technology |
|-------|-----------|
| Language | Go 1.25+ |
| Framework | Gin or Fiber |
| Database | PostgreSQL (primary) |
| Cache | Redis |
| Auth | JWT + Firebase Admin SDK |
| OTP | Firebase Phone Auth (server-side validation) |
| Voice Processing | Python microservice (called via gRPC or REST) |
| AI Orchestration | LLM-based intent + task routing |
| File Storage | Google Cloud Storage (voice samples, profile images) |
| Notification | FCM via Firebase Admin SDK |

### 3.3 AI / Voice Pipeline

| Component | Detail |
|-----------|--------|
| Speaker Verification (SV) | **ECAPA-TDNN** — SpeechBrain VoxCeleb checkpoint, fine-tuned/evaluated on speaker-disjoint **VIVOS** |
| Speaker Identification (SID) | Same ECAPA-TDNN embedding + cosine similarity matching across enrolled profiles |
| ASR | Local **Whisper** configured for Vietnamese |
| NLP / Intent | OpenAI `gpt-4o-mini` with deterministic local intent fallback |
| TTS | Android on-device Text-to-Speech using a Vietnamese (`vi-VN`) voice |
| Wake Word | On-device wake-word engine (e.g., Porcupine or custom) — triggers outside the app |

---

## 4. System Architecture

```
+-------------------------------------+
|          Android App (Kotlin)        |
|  +--------+ +--------+ +---------+  |
|  | Auth   | | Elder  | |Caregiver|  |
|  | Module | | UI     | | Setup   |  |
|  +--------+ +--------+ +---------+  |
|  | Local Vietnamese TextToSpeech     |
+------------------+------------------+
                   | HTTPS / REST
                   v
+-------------------------------------+
|        Golang API Server             |
|  +-------------------------------+  |
|  |  Auth  | User  | Caregiver   |  |
|  |  API   | API   | API         |  |
|  +-------------------------------+  |
|  |  Voice | Med   | Notification|  |
|  |  API   | API   | API         |  |
|  +-------------------------------+  |
|         | gRPC/REST                  |
|         v                            |
|  +-----------------------------+    |
|  |    Python AI Microservice    |    |
|  |  +------+ +-----+ +------+ |    |
|  |  |  SV  | | SID | | ASR  | |    |
|  |  +------+ +-----+ +------+ |    |
|  |  +----------------------+   |    |
|  |  |  LLM Orchestrator    |   |    |
|  |  |  (Intent + Response) |   |    |
|  |  +----------------------+   |    |
|  +-----------------------------+    |
|         |                            |
|  +--------------+  +-------------+  |
|  |  PostgreSQL  |  |    Redis    |  |
|  +--------------+  +-------------+  |
+-------------------------------------+
```

---

## 5. Voice Assistant Pipeline

```
Speech Input (AudioRecord)
        |
        v
  Upload audio to backend
        |
        v
  ASR (Speech -> Text)
        |
        v
  Request Analysis & Task Orchestration (LLM)
        |
        +--- General task (no auth required)
        |         +---> Execute -> response text -> Android TTS -> Voice Response
        |
        +--- Protected task -> Speaker Verification (SV)
        |         +-- PASS -> Execute -> response text -> Android TTS -> Voice Response
        |         +-- FAIL -> "Xac minh giong noi that bai" -> Android TTS
        |
        +--- Personalized task -> Speaker Identification (SID)
                  +-- Match user -> Personalized Execute -> response text -> Android TTS -> Voice Response
```

---

## 6. Feature Modules

### 6.1 Authentication & Onboarding

#### 6.1.1 Sign Up with Phone Number
- Enter phone number (Vietnamese format: 0xxxxxxxxx)
- Receive OTP via SMS (Firebase Phone Auth)
- Enter OTP to verify
- Create profile (name, age, avatar)
- Select role: **Elder (Nguoi duoc cham soc)** or **Caregiver (Nguoi cham soc)**

#### 6.1.2 Sign Up with Google
- OAuth 2.0 via Google Sign-In SDK
- On first login: collect phone number → OTP verification
- Select role (same as above)

#### 6.1.3 Sign In
- Phone number + OTP
- Google Sign-In
- Persistent session via JWT stored in Encrypted SharedPreferences

#### 6.1.4 OTP Verification Screen
- 6-digit OTP input (large digits, elder-friendly)
- Auto-fill support
- Resend OTP timer (60s countdown)
- Error state with clear message

---

### 6.2 Caregiver Setup Service

The caregiver configures the elder's environment through a dedicated setup flow:

#### 6.2.1 Elder Profile Setup
- Assign elder name, age, city
- Health status indicator (Normal / Warning / Critical)

#### 6.2.2 Contact Management (Danh sach goi)
- Add contacts with nickname (e.g., "con Lan", "thang Ti")
- Assign relationship tag (Con trai, Con gai, Ban be, Hang xom, etc.)
- Assign call method (Zalo Video Call / Phone)
- Large-format contact cards for elder readability

#### 6.2.3 Medication Schedule (Lich thuoc)
- Add medications with dose frequency (2x/day, 3x/day, etc.)
- Set specific time slots per dose
- Push notification reminder sent to elder's device at medication time
- Elder marks doses as taken (Done / Not yet)

#### 6.2.4 Activity Dashboard (Hoat dong)
- Call log: incoming/outgoing with duration and contact name
- Medication compliance summary
- Health status badge

---

### 6.3 Voice Assistant Service

> Core academic requirement: integrates SV + SID + ASR + LLM + TTS

#### 6.3.1 Invocation — Dual Mode

**Mode A — In-App Tap:**
- Elder taps large **"Speak" button** on main screen
- App records audio (up to 30s, auto-stop on silence)
- Waveform animation shown during recording

**Mode B — Wake Word ("Hey Dearly"):**
- Always-on background service listens for wake word
- Works even when app is minimized or screen is off
- On detection: screen lights up, mic activates, full-screen overlay appears
- Uses on-device wake-word engine (Porcupine or Android SpeechRecognizer hotword) — no audio sent to server until wake word confirmed

#### 6.3.2 General Functions (No Authentication Required)
- **Public Ask Dearly** is available from the sign-in screen without a Firebase or Dearly session.
- It safely answers the current time, current date, greeting, and usage guidance.
- It never loads contacts, medication schedules, call history, voice enrollments, or any other private information.

#### 6.3.3 Protected Functions (Require Speaker Verification)
- Initiate a phone/video call to a contact
- Confirm medication as taken
- Change personal settings (volume, language, theme)

**SV Flow**:
1. System detects a protected intent via request analysis.
2. For medication confirmation, Dearly resolves the named unpaid dose and asks, for example, *“Bác xác nhận đã uống Amlodipine lúc 08:30 phải không?”*, then asks the elder to say *“Đúng rồi”*.
3. The elder speaks the confirmation close to the microphone.
4. ECAPA-TDNN extracts an embedding and compares it with the stored enrollment vector by cosine similarity.
5. At threshold **≥ 0.80**, a pass issues a one-use grant and executes the update; a failure leaves the dose unchanged and offers a retry.

**Hands-free medication confirmation:** The elder says, for example, *“Tôi đã uống Amlodipine rồi.”* Dearly extracts the medication name and finds the matching unpaid dose for today, but does not update it yet. Dearly reads the exact dose and scheduled time, for example: *“Bác xác nhận đã uống Amlodipine lúc 08:30 phải không? Bác hãy nói Đúng rồi để xác nhận nhé.”* The confirmation recording must pass speaker verification before a one-use, two-minute `MARK_TAKEN` grant is issued and the dose is marked as taken. Failed, too-short, silent, replayed, or rate-limited recordings leave the dose unchanged. The optional dose-card dialog uses the same spoken confirmation and SV rule.

#### 6.3.4 Personalized Functions (Speaker Identification)
- Greet the identified user by name on voice activation.
- Read the medication schedule belonging to the identified user, after the backend confirms that identity belongs to the trusted caregiver/elder group.
- Apply the identified user's stored reminder wording (**gentle** or **direct**) and preferred Vietnamese TTS speed.
- Include the identified elder's daily medication schedule and, when configured, name the preferred caregiver/contact Dearly should prioritize for help.
- The Android response visibly states that SID personalization was applied; the preferences are never used for public, unsigned-in queries.

**SID Flow**:
1. Audio embedding extracted from voice input
2. Matched against enrolled speaker profiles in DB
3. Go API re-checks the trusted group, then loads only that user's voice preferences, preferred contact, and daily schedule before composing the response

#### 6.3.5 Speaker Enrollment
- Triggered during initial onboarding: **caregiver sets up directly on the elder's device**
- Elder repeats **5 fixed Vietnamese phrases** (shown on screen in large text with `0/5`--`5/5` progress)
- The Android VAD requires at least 0.8 seconds of sustained voice activity; silent/too-short recordings are deleted locally and the elder is asked in Vietnamese to record that phrase again
- FastAPI performs a second intelligible-speech check before ECAPA creates an enrollment embedding, so a bypassed client cannot enroll silence/noise
- Audio sent to backend → ECAPA-TDNN computes embedding per phrase → average embedding stored in DB
- Raw audio is discarded after embedding is computed
- Enrollment can be re-triggered anytime from Settings → Security
- Caregiver can manage **multiple elder accounts** on one device (switched via account selector)

---

### 6.4 Settings (Cai dat)

- Edit profile (name, phone, avatar)
- Account settings
- Notification preferences
- Security (re-enroll voice, change password)
- Language (Vietnamese / English)
- Support / Help
- Sign out

> **Medication snooze**: Elder can dismiss or snooze (10 min) a medication reminder directly from the **Android notification shade** via action buttons, without opening the app.

---

## 7. Screen Inventory

| Screen | Role | Description |
|--------|------|-------------|
| Splash / Onboarding | Both | App intro, role selection |
| Sign Up - Phone | Both | Phone number entry |
| OTP Verification | Both | 6-digit OTP input |
| Sign Up - Google | Both | Google OAuth redirect |
| Role Selection | Both | Elder / Caregiver picker |
| Profile Setup | Both | Name, age, avatar |
| Voice Enrollment | Elder | Record 5 phrases for SV/SID |
| Home - Elder (Hoat dong) | Elder | Dashboard: calls, meds, status |
| Call List (Goi dien) | Elder | Contact list with tap-to-call |
| Medication Schedule (Lich thuoc) | Elder | Today's med list |
| Settings (Cai dat) | Elder | Profile + preferences |
| Voice Assistant Overlay | Elder | Full-screen mic button + waveform |
| Caregiver Dashboard | Caregiver | Elder status overview (multi-elder account switcher) |
| Add Contact | Caregiver | Add/edit contact |
| Add Medication | Caregiver | Add/edit medication schedule |
| Elder Detail | Caregiver | Elder's profile + activity log |
| Voice Enrollment | Caregiver | Guide elder through 5-phrase enrollment on elder's device |

---

## 8. API Design (High Level)

### Auth
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /api/v1/auth/session | Firebase ID token + role -> Dearly JWT session |
| POST | /api/v1/auth/refresh | Refresh JWT |
| POST | /api/v1/auth/logout | Revoke refresh token |

### User & Profile
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /api/v1/users/me | Get current user profile |
| PUT | /api/v1/users/me | Update profile |
| GET | /api/v1/users/me/voice-preferences | Get the signed-in user's SID personalization settings |
| PUT | /api/v1/users/me/voice-preferences | Set reminder style, TTS speed, preferred contact, and daily-schedule inclusion |
| GET | /api/v1/users/:id/elders | Caregiver's elder list |

### Voice Enrollment
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /api/v1/voice/enroll | Upload audio samples for enrollment |
| DELETE | /api/v1/voice/enroll/:userId | Reset enrollment |
| GET | /api/v1/voice/verification-audit | Authenticated user's recent successful, failed, replay-blocked, and rate-limited protected voice checks |

### Voice Assistant
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /api/v1/voice/query | Upload audio, returns ASR text + assistant response audio |
| POST | /api/v1/voice/verify | Upload audio for SV, returns pass/fail |

### Contacts
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /api/v1/contacts | List contacts |
| POST | /api/v1/contacts | Add contact |
| PUT | /api/v1/contacts/:id | Update contact |
| DELETE | /api/v1/contacts/:id | Delete contact |

### Medications
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /api/v1/medications | List medications |
| POST | /api/v1/medications | Add medication |
| PUT | /api/v1/medications/:id | Update medication |
| DELETE | /api/v1/medications/:id | Delete medication |
| POST | /api/v1/medications/:id/taken | Mark dose as taken |

### Notifications
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /api/v1/notifications/register | Register FCM token |
| GET | /api/v1/notifications | List notifications |

---

## 9. Data Models (Simplified)

### User
```
id, phone_number, email, name, age, city, role (ELDER/CAREGIVER),
avatar_url, fcm_token, created_at, updated_at
```

### Voice Preferences
```
user_id (PK/FK User), reminder_style (GENTLE/DIRECT), speech_rate (0.5--1.5),
preferred_contact_id (nullable FK Contact), include_daily_schedule, updated_at
```

### Elder Profile (extends User)
```
user_id (FK User),
health_status_override (NORMAL/WARNING/CRITICAL, set by caregiver — nullable),
health_status_auto (computed: NORMAL if 0-1 missed doses today,
                              WARNING if 2-4 missed doses,
                              CRITICAL if 5+ missed doses),
health_status_effective (= override if set, else auto)
```

### Caregiver-Elder Link
```
id, caregiver_id (FK User), elder_id (FK User), created_at
```

### VoiceEnrollment
```
id, user_id (FK User), embedding_vector (float[]),
phrase_index, audio_url, created_at
```

### Contact
```
id, elder_id (FK User), nickname, full_name, phone_number,
relationship, call_method (PHONE/ZALO_VIDEO), created_at
```

### Medication
```
id, elder_id (FK User), name, frequency_per_day,
time_slots (JSON array of HH:mm strings), created_at
```

### MedicationLog
```
id, medication_id (FK), scheduled_time, taken_at,
status (TAKEN/PENDING/SNOOZED), snoozed_until
```

### CallLog
```
id, elder_id (FK User), contact_id (FK Contact),
direction (IN/OUT), started_at, duration_seconds
```

---

## 10. Security Considerations

- All API calls over HTTPS/TLS
- JWT access token (15min) + refresh token (7 days) stored in Encrypted SharedPreferences
- Voice embeddings stored as non-reversible float vectors (not raw audio after enrollment)
- OTP expires in 5 minutes, max 3 attempts before lockout
- **SV threshold: cosine similarity ≥ 0.80** (lenient setting chosen for elderly voice variability)
- Rate limiting on OTP and voice endpoints; five failed protected voice checks trigger a 15-minute verification pause
- A fresh spoken medication confirmation is required after the dose/time read-back; exact raw-audio replays of a successful protected verification are blocked for 24 hours, and an authenticated verification-attempt audit endpoint records the result
- This is replay protection and an interaction check, not a dedicated anti-spoof/liveness model; re-encoded playback remains a limitation to address with an anti-spoof model in later work

---

## 11. Non-Functional Requirements

| Requirement | Target |
|-------------|--------|
| Voice response latency | < 3 seconds end-to-end (target; not yet measured end-to-end) |
| OTP delivery | < 10 seconds |
| App cold start | < 2 seconds |
| Offline support | Cached contacts + medication list readable offline |
| Accessibility | Min font size 18sp, high contrast, large tap targets (>= 48dp) |
| Language | Vietnamese primary, English secondary |

---

## 12. Academic Report Checklist (from project_requirements.md)

### Requirement 1 - Speaker Model
- [x] Dataset description: VIVOS Vietnamese corpus, CC BY-NC-SA 4.0
- [x] Train / validation / test split: 41 / 5 / 19 speaker-disjoint speakers
- [x] Model architecture: 192-dimensional SpeechBrain ECAPA-TDNN
- [x] Training procedure: Colab Tesla T4, 20 epochs, AdamW, AAM-Softmax
- [x] Evaluation: 10,000 balanced trials, EER, minDCF, and SID Top-1
- [x] Experimental results: baseline EER 4.40%; final VIVOS EER 1.28%

### Requirement 2 - Virtual Assistant Integration
- [x] Enrollment procedure: five recorded phrases; per-phrase embeddings plus an average profile
- [x] Overall system architecture and processing flow
- [x] On-device Vietnamese Android TTS design
- [x] General voice action implementation: live time/date and stored medication-schedule responses
- [ ] Recorded demonstration of a general voice function
- [ ] Recorded demonstration of SV-protected medication marking using a real enrollment
- [x] Multi-user SID personalization flow: matching is restricted to the caller's linked caregiver/elder group and returns the recognized name, schedule, and stored voice preferences
- [ ] Recorded multi-user SID personalization demonstration with two enrolled, linked users

The evidence-backed narrative and metric table are in
`FINAL_PROJECT_REPORT.md`. The remaining implementation, demo, and submission
work is tracked in `REPORT_READINESS.md`.

---

## 13. All Confirmed Decisions

| # | Topic | Decision |
|---|-------|----------|
| 1 | Voice dataset | SpeechBrain VoxCeleb checkpoint; fine-tune + evaluate on **VIVOS** (completed) |
| 2 | Caregiver-Elder linking | Caregiver sets up **directly on the elder's device** |
| 3 | Real calls | **Real Zalo / Android phone deeplinks** |
| 4 | LLM provider | OpenAI `gpt-4o-mini` with deterministic local fallback |
| 5 | Voice invocation | In-app tap-to-record button; wake-word remains future work |
| 6 | Multi-elder | Yes — **multiple elder accounts** switchable on one caregiver phone |
| 7 | Deadline | **Friday, 28 August 2026** |
| 8 | SV threshold | **≥ 0.80** (lenient, tuned for elderly voice variability) |
| 9 | Backend hosting | **Localhost** for demo |
| 10 | AI microservice | Separate Docker container using CPU inference locally; training ran on a Colab T4 |
| 11 | Medication snooze | **From notification shade** — Android action buttons ("Taken" / "Snooze 10 min") |
| 12 | Health status logic | **Both**: caregiver override + auto-derived from missed doses (≥2 = Warning, ≥5 = Critical) |

---

## 14. Deployment Architecture (Local Demo)

```
[Android Device / Emulator]
        |
        | HTTP (local network / USB ADB tunnel)
        v
+-------------------------------+  docker-compose up
|   docker-compose              |
|  +---------+  +-----------+  |
|  | Go API  |  | Python AI |  |
|  | :8080   |  | :5000     |  |
|  +---------+  +-----------+  |
|  +-----------+ +----------+  |
|  | PostgreSQL| |  Redis   |  |
|  | :5432     | |  :6379   |  |
|  +-----------+ +----------+  |
+-------------------------------+
        Host: localhost (demo machine)
```

- All services run via **docker-compose** on the demo laptop
- Android device connects over **USB ADB reverse tunnel** or same WiFi
- Python AI container uses CPU inference locally; GPU is required for the remote training workflow only
- Go API container communicates with Python AI via internal Docker network (`ai-service:5000`)

---

## 15. Development Timeline (Target: 28 Aug 2026)

| Week | Dates | Focus |
|------|-------|-------|
| W1 | Aug 1–3 | Project scaffolding: Android + Golang skeleton, Firebase setup, DB schema |
| W2 | Aug 4–10 | Auth: Phone OTP, Google Sign-In, role selection, onboarding |
| W3 | Aug 11–14 | Caregiver setup: contacts, medications, elder profile |
| W4 | Aug 15–18 | Voice enrollment pipeline: ECAPA-TDNN service + enrollment UI |
| W5 | Aug 19–22 | Voice assistant core: ASR + OpenAI intent + TTS + SV flow |
| W6 | Aug 23–24 | Wake-word service, SID personalization, Zalo/phone deeplinks |
| W7 | Aug 25–26 | Polish: accessibility, push notifications, medication reminders |
| W8 | Aug 27–28 | Final testing, model fine-tuning results, report writing, submission |

---

## 16. Out of Scope (v1)

- iOS version
- Smartwatch companion
- Real-time GPS / location tracking
- Emergency SOS beyond voice command
- Multi-language TTS beyond Vietnamese
- Payment / subscription features
