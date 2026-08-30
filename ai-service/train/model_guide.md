# Dearly ECAPA-TDNN: Training, Evaluation, and Deployment

This document describes the completed Dearly speaker-recognition experiment and
the exact artifacts used by the application. The canonical run is
`20260829-183455-ecapa-vivos-r4`; it completed on 2026-08-29. Do not use the
older VoxVietnam/Kaggle notes as final-report evidence.

## 1. Model

- Architecture: SpeechBrain ECAPA-TDNN (`speechbrain/spkrec-ecapa-voxceleb`)
- Pretraining: VoxCeleb speaker data supplied by the SpeechBrain checkpoint
- Fine-tuning/evaluation corpus: VIVOS Vietnamese speech corpus
- Embedding: 192 dimensions
- Runtime: `ai-service/app/models/ecapa.py`
- Deployment checkpoint: `models/ecapa_dearly.ckpt` (SHA-256:
  `2BE5AF68398612BBDD06657197069D7F56D557B54528565E96015874FD1E690F`)

The runtime performs 1-to-1 cosine speaker verification and exposes a 1-to-N
identification primitive. A checkpoint contains fine-tuned embedding-model
weights and is loaded on top of the SpeechBrain base-model configuration.

## 2. Dataset and split

VIVOS was obtained from the prepared private Kaggle input
`kynthesis/vivos-vietnamese-speech-corpus-for-asr` and used for this academic,
non-commercial project under **CC BY-NC-SA 4.0**. It is not committed to Git.

| Split | Speakers | Utterances | Purpose |
|---|---:|---:|---|
| Train | 41 | 10,310 | Fine-tuning |
| Validation | 5 | 1,350 | Best-checkpoint selection |
| Test | 19 | 760 | Final evaluation only |

The split is speaker-disjoint: a speaker belongs to exactly one split. The VIVOS
tree fingerprint for the completed run is
`b04db214fd18653909b26f1c7d732823e99e3476bf851b0414bb42dd97c11d0b`.

## 3. Completed training protocol

The run used Google Colab T4 through the Colab CLI, not the local CPU runtime.

| Setting | Value |
|---|---|
| GPU | Tesla T4, CUDA 12.8 |
| Python / SpeechBrain | 3.13.15 / 1.1.0 |
| Torch / Torchaudio | 2.11.0+cu128 |
| Baseline | Untouched SpeechBrain ECAPA checkpoint |
| Smoke run | 1 epoch from the original pretrained source |
| Final run | 20 epochs from the original pretrained source |
| Batch size / crop | 32 / 3 seconds, mono 16 kHz |
| Loss | AAM-Softmax, margin 0.2, scale 30 |
| Optimizer / schedule | AdamW, 1e-4 to 1e-6 cosine decay |
| Backbone schedule | Frozen epochs 1-5; unfrozen epochs 6-20 |
| Best validation loss | 0.2644617259502411 |

The runner evaluated the untouched baseline, completed a smoke run, then ran the
final training from the original pretrained checkpoint. It copied improved
checkpoints to Google Drive and validated that the exported checkpoint produced a
finite 192-element embedding through the production runtime.

## 4. Evaluation results

The baseline and final model used the same held-out VIVOS test speakers and
10,000 deterministic balanced trials (5,000 genuine and 5,000 impostor). SID
Top-1 used five enrollment recordings per test speaker and 665 queries across
19 speakers.

| System | EER | minDCF | SID Top-1 |
|---|---:|---:|---:|
| Untouched VoxCeleb ECAPA baseline | 4.40% | 0.002978 | 100.00% |
| One-epoch smoke model | 1.58% | 0.001570 | 100.00% |
| VIVOS fine-tuned ECAPA | 1.28% | 0.001628 | 100.00% |

Fine-tuning reduced EER by 3.12 percentage points (70.9% relative) against the
baseline. The final EER operating threshold was 0.34559793992808. The production
threshold stays at 0.80; it must be calibrated separately on representative
elderly-user recordings and must not be silently replaced by the research EER
threshold.

## 5. Artifacts and reproducibility

The local artifact directory is:

```text
models/training/20260829-183455-ecapa-vivos-r4/
```

It contains `run_manifest.json`, baseline/smoke/final evaluations, training
summaries, the execution log, a reproducible notebook, and
`dearly-training-artifacts.zip`. The short evidence report is `run_report.md`.
Keep the dataset and model artifacts in Drive and include their submission link
in the required student-ID text file.

For a future rerun, follow [colab/README.md](colab/README.md). It requires a
fresh Kaggle token uploaded only as a temporary Colab input. The legacy Kaggle
runner is documented only in [kaggle/README.md](kaggle/README.md).

## 6. Runtime integration

Docker mounts `./models` at `/models`. Use:

```text
ECAPA_MODEL_PATH=/models/ecapa_dearly.ckpt
ECAPA_MODEL_SOURCE=speechbrain/spkrec-ecapa-voxceleb
SV_COSINE_THRESHOLD=0.80
SID_COSINE_THRESHOLD=0.80
```

`/ready` validates model availability plus enrollment, verification,
identification, and query routes. It does not require a Google Cloud TTS key:
the Android client reads the AI response text aloud locally with `vi-VN`.

## 7. Report checklist

- [x] Dataset, license, and speaker-disjoint split
- [x] Model, 192-dimensional embedding, and training protocol
- [x] Baseline, smoke, and final metrics with a held-out test set
- [x] Runtime checkpoint and finite-embedding validation
- [ ] Multi-user SID personalization demonstration in the application
- [ ] Real-device Vietnamese TTS demonstration and screenshots
- [ ] Final report screenshots, archive, and Drive-link text file
