# Dearly

Elder-care mobile app · Kotlin + Jetpack Compose · Go microservices · Kafka · ECAPA-TDNN · OpenAI

---

## Repository Layout

```
Dearly/
├── .github/workflows/ ← CI/CD Pipeline (GitHub Actions)
│   ├── ci.yml         ← Continuous Integration Matrix with paths filtering
│   └── release-android.yml ← Automatic installable APK packaging on tag push
├── android/           ← Android app (Kotlin 2.1.0 + Jetpack Compose + Hilt)
├── backend/           ← Go 1.25 public API + Kafka notification worker
├── ai-service/        ← Voice AI microservice (Python 3.11 + FastAPI + ECAPA-TDNN)
├── docker-compose.yml
├── .env.example
├── specs.md
└── project_requirements.md
```

---

## CI/CD Pipeline Overview

The project includes an optimized GitHub Actions matrix with path filtering ([`.github/workflows/ci.yml`](file:///c:/Users/Vu/schoolProject/Dearly/.github/workflows/ci.yml)):

| Job | Trigger Filter | Tasks Executed | Output Artifact |
|-----|----------------|----------------|-----------------|
| **`changes`** | Push / PR | Uses `dorny/paths-filter` to detect modified component directories | Filter outputs for job gating |
| **`android-ci`** | `android/**` | Runs `./gradlew lintDebug testDebugUnitTest` and compiles `app-debug.apk` | `dearly-android-debug-apk` |
| **`backend-ci`** | `backend/**` | Runs `go vet` & `go test`, verifies Go binary compilation | Verified `dearly-api` binary |
| **`ai-service-ci`** | `ai-service/**` | Runs `ruff check .` linting + `pytest` import & unit testing | Verified Python microservice |
| **`docker-ci`** | Dockerfiles / Compose | Validates `docker compose config` and executes `docker compose build` | Built Docker image stack |
| **`release-android`** | Tag push (`v*`) | Builds installable debug-signed & release APKs | Release APK assets on GitHub Releases |

---

## Prerequisites

| Tool | Version |
|------|---------|
| Android Studio | Ladybug (2024.2) or newer |
| JDK | 21 (LTS) |
| Go | 1.25+ |
| Kafka | 3.9+ |
| Python | 3.11+ |
| Docker Desktop | 4.x |
| Docker Compose | v2 (bundled with Docker Desktop) |

---

## First-time Setup

### Step 1 — Clone & copy env

```bash
git clone <repo-url>
cd Dearly
cp .env.example .env
# Fill in your actual secrets in .env (see Step 3 and Step 4)
```

---

### Step 2 — Create Firebase Project

> Firebase is used for Phone OTP authentication and push notifications (FCM).

1. Go to [https://console.firebase.google.com](https://console.firebase.google.com)
2. Click **"Add project"** → name it `Dearly` → disable Google Analytics → **Create project**

#### 2a. Enable Phone Authentication
1. In the left sidebar: **Build → Authentication → Get started**
2. Click the **Sign-in method** tab
3. Enable **Phone** → Save
4. Also enable **Google** → provide your support email → Save

#### 2b. Add an Android App
1. Click the **Android** icon on the project overview page
2. **Android package name**: `com.dearly.app`
3. **App nickname**: `Dearly Android`
4. Click **Register app**
5. Download **`google-services.json`**
6. Place it at: `android/app/google-services.json`
7. Skip the rest of the wizard (dependencies will be in `build.gradle.kts`)

#### 2c. Download Admin SDK credentials (for backend)
1. In Firebase Console: **Project Settings** (gear icon) → **Service accounts** tab
2. Click **"Generate new private key"** → **Generate key**
3. Save the downloaded JSON as: `backend/firebase-sa.json`
4. In your `.env`, set:
   ```
   FIREBASE_SERVICE_ACCOUNT_PATH=/run/secrets/firebase-sa.json
   ```

---

### Step 3 — Get OpenAI API Key

1. Go to [https://platform.openai.com/api-keys](https://platform.openai.com/api-keys)
2. Create a new secret key
3. In your `.env`, set:
   ```
   OPENAI_API_KEY=sk-...your-key...
   ```

---

### Step 4 — Start backend services

```bash
# From repo root
docker compose up --build
```

This starts:
- **PostgreSQL** on `localhost:5432` (DB migrations run automatically)
- **Redis** on `localhost:6379`
- **Go API** on `localhost:8080`
- **AI Service** privately on the Compose network at `ai-service:5000`
- **Kafka** on the private Compose network
- **Notification worker** as a Kafka consumer

Verify everything is running:
```bash
curl http://localhost:8080/health   # → {"status":"ok"}
docker compose exec ai-service curl -f http://localhost:5000/health
```

---

### Step 5 — Open Android project

1. Open **Android Studio**
2. **File → Open** → select the `android/` folder
3. Wait for Gradle sync to complete
4. Create an emulator (API 33+) or connect a physical device
5. Run the app ▶
