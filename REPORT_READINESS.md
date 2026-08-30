# Dearly Final-Project Readiness Audit

**Audit date:** 2026-08-30  
**Rubric source:** `project_requirements.md`  
**Training evidence:** `models/training/20260829-183455-ecapa-vivos-r4/`

## Completed evidence

| Rubric item | Status | Evidence |
|---|---|---|
| Representative SV/SID model | Complete | SpeechBrain ECAPA-TDNN, 192-dimensional embeddings. |
| Suitable dataset and split | Complete | VIVOS, speaker-disjoint 41 train / 5 validation / 19 test speakers. |
| Training procedure | Complete | Tesla T4, baseline + smoke + independent 20-epoch final run; manifest and logs saved. |
| Evaluation | Complete | 10,000 balanced held-out trials: final EER 1.28%, minDCF 0.001628, SID Top-1 100%. |
| Model integration | Complete | `models/ecapa_dearly.ckpt` loads through the production runtime and returns a finite 192-element embedding. |
| Voice interaction and enrollment | Code complete | Android recorder; five phrase embeddings and an average profile are stored by the backend. |
| General voice actions | Code complete | `ASK_TIME` and `ASK_DATE` return the configured local time/date; `CHECK_MEDICATIONS` reads the authenticated elder's stored schedule. |
| Important protected function | Code complete | Medication marking requires an intent-bound, two-minute, one-use SV grant. |
| SID personalization | Code complete | The query compares the caller plus directly linked caregiver/elder enrollment profiles, then returns the matched person's name in the spoken response. |
| Vietnamese TTS | Code complete | Android uses local `vi-VN` TextToSpeech; no Cloud TTS account or key is required. |

## Still required before a defensible final submission

### 1. Record a true general voice-action demonstration

**Implemented:** `ASK_TIME` and `ASK_DATE` read the configured local clock;
`CHECK_MEDICATIONS` reads the stored medication schedule. The query route is
covered by focused backend tests and has been rebuilt into the Docker API.

**Required work:** record at least one of those read-only actions in the demo.

### 2. Record multi-user SID personalization

**Implemented:** the backend sends profiles only from the caller's trusted
caregiver/elder relationship group to `/query/`, resolves the recognized user,
and prefixes the spoken response with that person's name.

**Required work:** record a demo with at least two enrolled, linked speakers.

### 3. Record the required real-device demo evidence

The emulator was not connected during the latest build audit, so the following
must be recorded on an Android device/emulator with a Vietnamese voice installed:

1. Open Android Text-to-speech settings and show a selected/downloaded `vi-VN`
   voice.
2. Enroll five voice phrases and show the completion state.
3. Demonstrate a general function (time, date, or medication schedule).
4. Demonstrate medication marking: request -> second verification phrase ->
   successful action.
5. Demonstrate a failed verification attempt with a different speaker.
6. Demonstrate SID personalization with two enrolled, linked users.

Capture screenshots or a short video and reference the figures in the final
report. Do not claim this as tested until the real recordings exist.

### 4. Verify the full Docker/Firebase stack once

The AI container is healthy, but this audit did not verify an end-to-end Go API
session, Firebase ID-token verification, FCM delivery, database migration, and
voice request together. Start the whole stack using the real
`backend/firebase-sa.json`, then run the device flow above.

### 5. Create the final submission package

No final student-ID ZIP or Drive-link text file was found in the repository.
Before submitting:

- Create the final report PDF/DOCX from `FINAL_PROJECT_REPORT.md` and add the
  real screenshots.
- Upload the large VIVOS/model artifacts to Drive if they cannot fit in the ZIP.
- Create `23127144.txt` with the Drive link, or use the underscore-separated
  team-ID filename if this is a team submission.
- Create `23127144.zip` containing source code, report, and required metadata;
  exclude secrets such as `.env`, `firebase-sa.json`, and any API keys.
- Inspect the ZIP contents before upload.

## Non-blocking notes

- The production cosine threshold remains 0.80. The measured EER threshold
  (0.3456) is reported for research, not automatically deployed.
- VIVOS is CC BY-NC-SA 4.0; retain attribution and academic/non-commercial use
  context in the final report and Drive material.
- A previously exposed Kaggle token should be rotated before any future rerun.
