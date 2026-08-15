# Dearly backend

The backend is the only public application API. It contains two Go processes:

```text
Android
   │ Firebase ID token + HTTPS/REST
   ▼
Go API ───────► PostgreSQL
   │           Redis (refresh-token revocation)
   │
   ├── Kafka: dearly.domain-events ──► notification-worker ──► FCM
   │
   └── private REST ──► Python ai-service
```

The Android application never calls PostgreSQL, Redis, Kafka, or the Python service directly.

## Processes

- `go run ./cmd/server`: public Gin API on port `8080`
- `go run ./cmd/notification-worker`: Kafka consumer and medication reminder dispatcher

The API applies SQL migrations from `MIGRATIONS_PATH` at startup.

## Authentication

Phone OTP and Google credential collection happen through the Firebase Android SDK. After Firebase authentication, Android sends the Firebase ID token to:

```http
POST /api/v1/auth/session
Content-Type: application/json

{
  "firebase_id_token": "...",
  "role": "ELDER"
}
```

The API verifies the token with Firebase Admin, upserts the user, and returns a 15-minute access token and rotating seven-day refresh token. Protected endpoints require `Authorization: Bearer <access_token>`.

## Public routes

- `/api/v1/auth/session`, `/refresh`, `/logout`
- `/api/v1/users/me`, `/users/me/elders`
- `/api/v1/contacts`
- `/api/v1/medications`, `/:id/taken`, `/:id/snooze`
- `/api/v1/notifications`
- `/api/v1/voice/enroll`, `/verify`, `/query`

Contacts and medications accept an optional `elder_id`. Elders may only use their own ID. Caregivers must have a row in `caregiver_elder_links`.

## Kafka contract

All records use topic `dearly.domain-events` and this envelope:

```json
{
  "id": "uuid",
  "type": "medication.taken",
  "version": 1,
  "occurred_at": "RFC3339 timestamp",
  "aggregate_id": "uuid",
  "data": {}
}
```

Events currently cover sessions, contacts, medications, and voice operations. The notification worker consumes medication events with the consumer group `notification-worker`; processed event IDs are recorded for idempotency.

Kafka is intentionally asynchronous. REST commands commit their authoritative state to PostgreSQL first, so Android receives a deterministic result even when notification delivery is delayed.

## Run

1. Copy `.env.example` to `.env`.
2. Add `backend/firebase-sa.json`.
3. Add `android/app/google-services.json`.
4. Run `docker compose up --build`.

The emulator uses `http://10.0.2.2:8080/api/v1/`. Override it when building for a physical device:

```powershell
.\gradlew.bat assembleDebug -PDEARLY_API_BASE_URL=http://192.168.1.10:8080/api/v1/
```

## Trade-offs

- This is a pragmatic microservice split: API, notification worker, and AI service. Splitting every domain into its own deployable service would add distributed transactions and operational overhead without helping the current project scale.
- Domain events are published directly after database commits. For production-grade guaranteed delivery, replace this with a transactional outbox and a Kafka relay.
- The demo uses one Kafka broker. Production should use at least three brokers, replication, TLS/SASL, schema compatibility checks, metrics, and dead-letter handling.
