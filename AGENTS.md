# Repository Guidelines

## Project Structure & Module Organization

- `android/` contains the Kotlin/Jetpack Compose app. Production code lives under `android/app/src/main/java/com/dearly/app/`, grouped into `ui/`, `navigation/`, `domain/`, `data/`, `di/`, and `service/`. Images and XML resources belong in `src/main/assets/` and `src/main/res/`.
- `backend/` contains the Go API and notification worker. Entrypoints are in `cmd/server/` and `cmd/notification-worker/`; domain logic is in `internal/`, shared infrastructure in `pkg/`, and ordered SQL changes in `migrations/`.
- `ai-service/` contains the FastAPI voice service. Keep routes in `app/routers/`, integrations in `app/services/`, and training utilities in `train/`.
- Root Compose files orchestrate local services. Architecture and requirements are documented in `README.md`, `backend/README.md`, `specs.md`, and `project_requirements.md`.

## Build, Test, and Development Commands

Run the full local stack from the repository root:

```sh
docker compose up --build
```

Run component checks before opening a PR:

```sh
cd backend && go vet ./... && go test ./...
cd android && ./gradlew lintDebug testDebugUnitTest assembleDebug
cd ai-service && ruff check . && pytest -v
```

On Windows, use `gradlew.bat`. Start services independently with `go run ./cmd/server`, `go run ./cmd/notification-worker`, or `uvicorn app.main:app --reload --port 5000`.

## Coding Style & Naming Conventions

Use four-space indentation for Kotlin and Python; run `gofmt` on Go files. Kotlin types and composables use `PascalCase`, while functions and properties use `camelCase`. Go tests follow `TestName`; Python functions and modules use `snake_case`. Keep handlers thin and place business rules in service or repository layers.

## Testing Guidelines

Place Go tests beside code as `*_test.go`, Android unit tests in `app/src/test/`, instrumentation tests in `app/src/androidTest/`, and Python tests as `test_*.py`. No numeric coverage threshold is enforced, but new validation, authorization, and data-mapping behavior should include focused tests.

## Commit & Pull Request Guidelines

Prefer concise Conventional Commit subjects such as `feat: add medication reminders` or `fix: validate refresh tokens`. Keep commits scoped to one coherent change. PRs should target `main`, summarize affected modules, list verification commands, link relevant issues, note migrations or environment changes, and include screenshots for UI changes.

## Security & Configuration

Copy `.env.example` to `.env`; never commit real secrets, `firebase-sa.json`, `google-services.json`, build outputs, or JVM crash logs. Use environment-specific Firebase and API configuration for deployments.
