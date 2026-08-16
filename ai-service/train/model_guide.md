# ECAPA-TDNN — Model Training & Integration Guide

> Covers W4 work (Aug 15–18) and the academic Requirement 1.

---

## 1. The Plan at a Glance

```
VoxCeleb2 (pretrain)          VoxVietnam / Vietnam-Celeb (fine-tune + eval)
      │                                    │
      ▼                                    ▼
SpeechBrain ECAPA-TDNN checkpoint   Fine-tune on Vietnamese speakers
      │                                    │
      └──────────────┬─────────────────────┘
                     ▼
            ecapa_dearly.ckpt
                     │
                     ▼
          ai-service/models/ecapa.py
                     │
           ┌─────────┴──────────┐
           ▼                    ▼
    Speaker Verification  Speaker Identification
       (SV, 1-to-1)          (SID, 1-to-N)
```

---

## 2. Datasets

### 2.1 Pretraining — VoxCeleb2
- **Do NOT download or train from scratch** — use the SpeechBrain pretrained checkpoint directly.
- **Source**: [HuggingFace – speechbrain/spkrec-ecapa-voxceleb](https://huggingface.co/speechbrain/spkrec-ecapa-voxceleb)
- **What it gives you**: A 192-dim speaker embedding model already trained on 5,994 speakers, 1M+ utterances.
- **How to download** (one line, goes into `./models/`):

```python
from speechbrain.inference.speaker import EncoderClassifier
model = EncoderClassifier.from_hparams(
    source="speechbrain/spkrec-ecapa-voxceleb",
    savedir="pretrained_models/spkrec-ecapa-voxceleb"
)
```

### 2.2 Fine-tuning + Evaluation — Vietnamese Dataset

Pick **one** (priority order):

| Dataset | Speakers | Utterances | Notes |
|---------|----------|------------|-------|
| **VoxVietnam** | ~100 | ~10k | Most accessible, downloadable from HuggingFace, has official splits |
| **Vietnam-Celeb** | ~1,000 | ~100k | Larger/more diverse, better results |
| VLSP 2021-SV | ~200 | ~20k | Directly citable benchmark with published baselines |

**Recommendation**: Use **VoxVietnam** — it's on HuggingFace, has official train/test splits, and published EER baselines exist to compare against in the report.

```bash
# HuggingFace download
huggingface-cli download voxvietnam/voxvietnam --repo-type dataset --local-dir data/voxvietnam
# Or: https://github.com/v-nhandt21/VoxVietnam
```

### 2.3 Train / Val / Test Split

```
Training:   80% of speakers  — used to fine-tune the model
Validation: 10% of speakers  — used to pick the best checkpoint
Test:       10% of speakers  — used ONLY for final EER/minDCF numbers

CRITICAL: splits must be SPEAKER-DISJOINT.
A speaker must appear in exactly ONE split.
Never split by utterance — that leaks speaker identity.
```

---

## 3. Fine-Tuning with SpeechBrain

### 3.1 Why Fine-Tune?
VoxCeleb2 is English-dominant. Vietnamese has different phonetics, tone patterns, and prosody. Fine-tuning adapts the embedding space to Vietnamese speaker characteristics.

### 3.2 Environment Setup

```bash
# In ai-service/ or a separate training environment
pip install speechbrain torch torchaudio

# Verify GPU
python -c "import torch; print(torch.cuda.is_available())"
```

### 3.3 Directory Structure Expected

```
data/voxvietnam/
├── train/
│   ├── speaker_001/
│   │   ├── utt_001.wav
│   │   └── utt_002.wav
│   └── speaker_002/
│       └── ...
├── val/
│   └── speaker_xxx/
└── test/
    └── speaker_yyy/
```

### 3.4 Key Hyperparameters

```yaml
# hparams/train_ecapa.yaml
seed: 42
lr: 0.0001
lr_final: 0.000001
n_epochs: 20
batch_size: 32

# AAM-Softmax loss (best for speaker verification)
margin: 0.2
scale: 30

# Number of speakers in training split
out_n_neurons: 80   # 80% of VoxVietnam speakers

# Freeze backbone for first N epochs, then unfreeze
freeze_until_epoch: 5
```

### 3.5 Training Command

```bash
# From ai-service/. Generate manifests and train from the YAML configuration.
python train/finetune_ecapa.py train/hparams/train_ecapa.yaml --prepare

# Best checkpoint saved to results/ECAPA/best_model.ckpt
cp results/ECAPA/best_model.ckpt ../models/ecapa_dearly.ckpt

# Evaluate the speaker-disjoint test split.
python -m train.eval data/voxvietnam/test --trials 10000 \
    --output results/ECAPA/evaluation.json
```

### 3.6 Google Colab Option (No Local GPU)

```python
# In Colab (T4 = free, A100 = Colab Pro — ~2–3h for 20 epochs on VoxVietnam)
!pip install speechbrain

from google.colab import drive
drive.mount('/content/drive')

# After training, save checkpoint to Drive:
# !cp results/ECAPA/.../best_model.ckpt /content/drive/MyDrive/dearly/ecapa_dearly.ckpt
```

---

## 4. Evaluation Metrics (Report — Requirement 1)

### 4.1 Equal Error Rate (EER)
- **Definition**: Point where False Acceptance Rate (FAR) = False Rejection Rate (FRR).
- **Lower = better**. SOTA on VoxCeleb1 = ~0.8% EER.
- **Realistic target for VoxVietnam fine-tune**: EER ≤ 5% is solid for the report.

### 4.2 minDCF
- Minimum Detection Cost Function — weighted combination of FA + FR errors.
- Always reported alongside EER in speaker verification papers.

### 4.3 SID Top-1 Accuracy
- Enroll up to five recordings per test speaker, then identify every remaining recording.
- Report the fraction whose highest cosine-scoring profile is the correct speaker.
- `train/eval.py` computes this together with EER and minDCF using one embedding cache.

### 4.3 Computing Metrics with SpeechBrain

```python
# Generate trial pairs from test set:
# Pair = (audio_path_1, audio_path_2, label)  where label=1 → same speaker
# Recommended: 10,000 pairs (5,000 genuine + 5,000 impostor)

from speechbrain.utils.metric_stats import EER, minDCF

# positive_scores: similarity scores for genuine pairs
# negative_scores: similarity scores for impostor pairs

eer, threshold = EER(positive_scores, negative_scores)
min_dcf, _ = minDCF(positive_scores, negative_scores)

print(f"EER:       {eer * 100:.2f}%")
print(f"minDCF:    {min_dcf:.4f}")
print(f"Threshold: {threshold:.4f}")  # compare to our chosen 0.80
```

---

## 5. Model File — Where It Lives

```
Dearly/
└── models/                        ← gitignored, mounted into Docker as /models
    └── ecapa_dearly.ckpt          ← put your fine-tuned checkpoint here
```

**Already wired in docker-compose.yml:**
```yaml
ai-service:
  volumes:
    - ./models:/models
```

**Already wired in .env.example:**
```
ECAPA_MODEL_PATH=/models/ecapa_dearly.ckpt
ECAPA_MODEL_SOURCE=speechbrain/spkrec-ecapa-voxceleb
SV_COSINE_THRESHOLD=0.80
```

`ECAPA_MODEL_PATH` is optional. Leave it empty to run the pretrained SpeechBrain source;
set it to the fine-tuned checkpoint to layer the Vietnamese backbone weights on that source.

> Upload `ecapa_dearly.ckpt` to Google Drive (too large for git).
> Submit the Drive link with your report ZIP as per submission guidelines.

---

## 6. Runtime Integration

The production wrapper is implemented in `app/models/ecapa.py`. It lazily loads one
SpeechBrain classifier, converts recordings to mono 16 kHz audio, validates finite embedding
dimensions, and provides cosine-based 1-to-1 SV and 1-to-N SID. A file in
`ECAPA_MODEL_PATH` loads the fine-tuned `embedding_model` state; otherwise the configured
SpeechBrain source is used directly. The HTTP routes use bounded temporary uploads and clean
them after inference.

The following excerpt documents the core inference shape:

```python
import os
import torch
import torchaudio
import numpy as np
from speechbrain.inference.speaker import EncoderClassifier

SV_THRESHOLD = float(os.getenv("SV_COSINE_THRESHOLD", "0.80"))


class EcapaTDNN:
    """
    Singleton ECAPA-TDNN wrapper.
    Model is loaded once at first use and reused for all requests.
    """
    _instance = None

    def __new__(cls):
        if cls._instance is None:
            cls._instance = super().__new__(cls)
            model_path = os.getenv("ECAPA_MODEL_PATH", "/models/ecapa_dearly.ckpt")
            device = "cuda" if torch.cuda.is_available() else "cpu"
            cls._instance.model = EncoderClassifier.from_hparams(
                source=model_path,
                savedir="/tmp/ecapa_cache",
                run_opts={"device": device},
            )
            cls._instance.device = device
        return cls._instance

    def extract_embedding(self, audio_path: str) -> list[float]:
        """Load audio, resample to 16kHz mono, return 192-dim embedding."""
        signal, sr = torchaudio.load(audio_path)
        if signal.shape[0] > 1:                        # stereo → mono
            signal = signal.mean(dim=0, keepdim=True)
        if sr != 16000:                                 # resample if needed
            signal = torchaudio.transforms.Resample(sr, 16000)(signal)
        with torch.no_grad():
            embedding = self.model.encode_batch(signal)
        return embedding.squeeze().cpu().tolist()       # 192-dim list[float]

    def cosine_similarity(self, a: list[float], b: list[float]) -> float:
        va, vb = np.array(a), np.array(b)
        return float(np.dot(va, vb) / (np.linalg.norm(va) * np.linalg.norm(vb)))

    def verify(self, audio_path: str, stored_embedding: list[float]) -> tuple[bool, float]:
        """1-to-1 speaker verification. Returns (passed, similarity_score)."""
        incoming = self.extract_embedding(audio_path)
        score = self.cosine_similarity(incoming, stored_embedding)
        return score >= SV_THRESHOLD, round(score, 4)

    def identify(self, audio_path: str, enrolled: list[dict]) -> tuple[str | None, float]:
        """
        1-to-N speaker identification.
        enrolled = [{"user_id": str, "embedding": list[float]}, ...]
        Returns (best_user_id, best_score) or (None, best_score) if below threshold.
        """
        incoming = self.extract_embedding(audio_path)
        scores = [
            (s["user_id"], self.cosine_similarity(incoming, s["embedding"]))
            for s in enrolled
        ]
        best_user_id, best_score = max(scores, key=lambda x: x[1])
        if best_score >= SV_THRESHOLD:
            return best_user_id, round(best_score, 4)
        return None, round(best_score, 4)
```

---

## 7. W4 Checklist

```
[ ] Download VoxVietnam from HuggingFace → data/voxvietnam/
[ ] Split by speaker: 80% train, 10% val, 10% test (speaker-disjoint)
[ ] Download SpeechBrain VoxCeleb2 checkpoint (one Python line)
[ ] Set up training env (Colab T4/A100, or local GPU)
[ ] Run fine-tuning (20 epochs, ~2–3h)
[ ] Generate 10,000 trial pairs from test set
[ ] Compute EER + minDCF, record numbers for report
[ ] Copy best_model.ckpt → Dearly/models/ecapa_dearly.ckpt
[x] Implement ecapa.py with singleton pattern above
[x] Add enrollment, verification, identification, and query contract tests with injected models
[ ] Test a real checkpoint locally: python -c "from app.models.ecapa import EcapaTDNN; print(EcapaTDNN.shared())"
[ ] Test end-to-end via Docker: enroll 5 phrases → verify same speaker → verify different speaker
```

---

## 8. Report Section Template (Requirement 1)

```markdown
### 2.1 Dataset

| Split | Speakers | Utterances |
|-------|----------|------------|
| Train | XX       | XX,XXX     |
| Val   | XX       | X,XXX      |
| Test  | XX       | X,XXX      |

Pretraining: VoxCeleb2 via SpeechBrain pretrained checkpoint
Fine-tuning & evaluation: VoxVietnam (Vietnamese, collected from YouTube)
Splits: speaker-disjoint (a speaker appears in exactly one split).

### 2.2 Model — ECAPA-TDNN
Architecture: Emphasized Channel Attention, Propagation and Aggregation TDNN
Embedding dimension: 192
Backbone: speechbrain/spkrec-ecapa-voxceleb (pretrained on VoxCeleb2)

### 2.3 Training Procedure
Loss: AAM-Softmax (margin=0.2, scale=30)
Optimizer: Adam (lr=1e-4 → 1e-6 cosine decay)
Epochs: 20 (backbone frozen for first 5)
Batch size: 32
Hardware: [your Colab/GPU info]

### 2.4 Evaluation
Protocol: 10,000 trial pairs (5,000 genuine + 5,000 impostor) from test set
Metrics: EER, minDCF

| System                    | EER (%) | minDCF | SID Top-1 (%) |
|---------------------------|---------|--------|-----------------|
| ECAPA-TDNN (VoxCeleb2)    | X.XX    | X.XXXX | XX.XX           |
| + Fine-tuned (VoxVietnam) | X.XX    | X.XXXX | XX.XX           |

Deployment threshold: cosine similarity ≥ 0.80
(Tuned lower than EER threshold to reduce false rejections for elderly users)
```
