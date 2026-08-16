---
title: "feat: integrate backend and Android data layer"
base: main
head: codex/backend-integration
issue: null
labels:
  - enhancement
draft: true
---

## Summary

- Complete the Go backend runtime for authentication, access control, contacts, medications, notifications, users, and voice features.
- Add database migration execution, event publishing, a notification worker, JWT coverage, and medication service tests.
- Add the Android Room, Retrofit, repository, session, Hilt, Firebase Auth, and push-messaging layers needed to connect the completed UI to backend services.
- Update Docker, CI, environment examples, and project documentation for the integrated runtime.

## Related References

No linked issue.

## What Changed

- [x] Backend services and handlers
- [x] Database migrations and runtime migration support
- [x] Notification worker and event infrastructure
- [x] Android data, repository, session, dependency-injection, and messaging layers
- [x] Docker and CI configuration
- [x] Documentation and environment examples

## Verification

- [x] `cd backend && go test ./...`
- [x] `cd android && .\gradlew.bat testDebugUnitTest`
- [x] Confirmed the branch was based on the latest `main` UI commits with no unresolved merge conflicts

## Breaking Changes

Existing deployments should review the updated database migrations and environment variables before upgrading.

## Follow-Up

- Provide the environment-specific Firebase configuration and backend secrets before deployment.
- Add reviewers, assignees, and a milestone in GitHub as appropriate.
