# Secure Virtual Assistant with Speaker Recognition

**Project:** Dearly - Vietnamese elder-care virtual assistant  
**Student ID:** 23127144  
**Report status:** evidence-backed draft, updated 2026-08-30

## Abstract

Dearly is an Android virtual assistant for elderly users and caregivers. It
accepts recorded voice commands, transcribes Vietnamese speech, classifies the
request, uses speaker verification before sensitive actions, and returns a
spoken Vietnamese response. The system uses a fine-tuned ECAPA-TDNN embedding
model for speaker verification (SV) and speaker identification (SID), Whisper
for ASR, a constrained LLM/local intent layer for orchestration, a Go backend
for authorization and medication state, and Android's local TextToSpeech engine
for response audio. This avoids a paid cloud TTS dependency while retaining
Vietnamese speech output on devices with a `vi-VN` voice installed.

## 1. Use cases and security policy

| Category | Example | Required control | Current implementation status |
|---|---|---|---|
| General | Greeting, asking the time/date, checking medication schedule | No SV | Intent classification and Vietnamese guidance response are implemented; the data-action adapter for voice-triggered schedule lookup is still pending. |
| Important | Mark a medication dose as taken | SV before mutation | Implemented: verification issues a short-lived, one-use grant consumed by the medication endpoint. |
| Personalized | Identify a registered speaker and personalize information | SID | ECAPA 1-to-N matching and its evaluation are implemented; a multi-user app personalization flow remains pending. |

The separation is deliberate: an initial voice query can classify a protected
request but cannot authorize an action. The user records a second verification
phrase. Only a successful SV result produces an `X-Voice-Grant` that is bound to
the user and protected intent, expires after two minutes, is usable once, and is
stored only as a SHA-256 hash.

## 2. System architecture and flow

```text
Android app
  record voice -> Go API -> FastAPI AI service
                                 |- Whisper ASR (Vietnamese)
                                 |- ECAPA-TDNN SV/SID embeddings
                                 |- intent classification and response text
  show response text <- Go API <- AI service
  speak response text locally with Android TextToSpeech (vi-VN)

Protected medication action:
  query -> protected intent -> second recording -> SV -> one-use voice grant
  -> POST medication/taken with X-Voice-Grant -> PostgreSQL + Kafka event
```

Five enrollment recordings are collected in Android Settings. The backend sends
each recording to ECAPA, stores five serialized embeddings, and stores their
average as the user profile. Raw enrollment audio is not persisted by this
workflow. The AI service provides both 1-to-1 cosine verification and a 1-to-N
identification primitive.

## 3. Speaker-recognition model and dataset

The selected model is **ECAPA-TDNN**, using the SpeechBrain
`speechbrain/spkrec-ecapa-voxceleb` checkpoint as the pretrained base. It
returns a 192-dimensional embedding. The model was fine-tuned and evaluated on
VIVOS, a Vietnamese speech corpus obtained from
`kynthesis/vivos-vietnamese-speech-corpus-for-asr`. VIVOS is used under the
CC BY-NC-SA 4.0 license for this academic, non-commercial project.

| Split | Speakers | Utterances | Use |
|---|---:|---:|---|
| Train | 41 | 10,310 | Fine-tuning |
| Validation | 5 | 1,350 | Best-checkpoint selection |
| Test | 19 | 760 | Final evaluation only |

The split is speaker-disjoint: no speaker occurs in more than one split. This
prevents speaker-identity leakage from train to validation or test data.

## 4. Training procedure

The final experiment ran on a **Tesla T4** Google Colab runtime. It first
evaluated the untouched pretrained model, then completed a one-epoch smoke run,
then trained for 20 epochs from the original pretrained source rather than from
the smoke checkpoint.

| Parameter | Value |
|---|---|
| Batch size | 32 |
| Input | Random 3-second, 16 kHz mono crops |
| Loss | AAM-Softmax, margin 0.2, scale 30 |
| Optimizer | AdamW |
| Learning rate | 1e-4 to 1e-6 cosine schedule |
| Backbone | Frozen for epochs 1-5; unfrozen for epochs 6-20 |
| Best validation loss | 0.2644617259502411 |
| Evaluation protocol | 10,000 deterministic balanced trials: 5,000 genuine and 5,000 impostor |

The source revision was `736a118b6dc50fd7700bf1218b74d946f14d214b-dirty`.
The complete manifest, execution log, notebook, checkpoints, and evaluation
JSON files are preserved in `models/training/20260829-183455-ecapa-vivos-r4/`.

## 5. Experimental results

The untouched baseline and fine-tuned model were evaluated on the same held-out
VIVOS test speakers. SID Top-1 used five enrollment recordings per test speaker
and 665 query recordings across 19 speakers.

| System | EER | minDCF | SID Top-1 |
|---|---:|---:|---:|
| Untouched VoxCeleb ECAPA baseline | 4.40% | 0.002978 | 100.00% |
| One-epoch smoke model | 1.58% | 0.001570 | 100.00% |
| VIVOS fine-tuned ECAPA | **1.28%** | **0.001628** | **100.00%** |

Fine-tuning reduced EER by 3.12 percentage points, a 70.9% relative reduction
from the baseline. The final EER operating threshold was 0.34559793992808. The
application's SV threshold remains 0.80: it is a safety-oriented deployment
choice and should be recalibrated only with representative elderly-user data.

The final deployment checkpoint is `models/ecapa_dearly.ckpt` with SHA-256
`2BE5AF68398612BBDD06657197069D7F56D557B54528565E96015874FD1E690F`. It was
loaded through the production ECAPA wrapper on local CPU and returned a finite
192-element embedding.

## 6. Runtime validation

The following checks were completed after the local-TTS change:

- AI service lint and focused unit tests: 12 passed.
- Docker Compose configuration: valid.
- AI service Docker container: `/ready` returned healthy using the trained
  checkpoint and no `gcp-sa.json` mount.
- Android debug APK: built successfully.

Android local TTS selects `Locale("vi", "VN")`, requests a speech audio usage,
and reports a visible Vietnamese message if the device lacks a compatible voice.
The user must download/select a Vietnamese voice in Android's Text-to-speech
settings before the live demo.

## 7. Limitations and remaining integration work

This report does not claim unfinished behavior as completed.

1. The current general voice intents provide safe guidance text but do not yet
   execute the related time/date/medication lookup in the voice-query flow.
2. The AI service has a tested 1-to-N SID primitive, but the backend currently
   submits only the current user's enrollment profile to the query route. A
   multi-user personalization feature must be wired and demonstrated.
3. A full physical-device demonstration is still required for microphone,
   Vietnamese TTS playback, Firebase authentication, real enrollment, SV, and
   protected medication marking.
4. The current measurements are VIVOS test-set metrics, not a calibration study
   on the elderly target population.

## 8. Submission materials

Package source code, this report, and the final checkpoint. Because VIVOS and
the trained-model archive are large, upload them to Google Drive and include the
link in `23127144.txt` (or the underscore-separated team-ID filename required
for the actual team). The final ZIP must use the corresponding student-ID name.

Before submission, follow [REPORT_READINESS.md](REPORT_READINESS.md) to record
the three required demonstrations and create the missing ZIP/link artifacts.
