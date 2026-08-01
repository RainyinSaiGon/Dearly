"""
Evaluation script — compute EER and minDCF on the test set.

Usage:
    python train/eval.py

Run AFTER training. Reads the best checkpoint from models/ecapa_dearly.ckpt.
Outputs numbers you copy directly into the academic report (Requirement 1).
"""

import os
import random
import torch
import torchaudio
import numpy as np
from pathlib import Path
from itertools import combinations

# TODO(W4): Uncomment once speechbrain is installed
# from speechbrain.utils.metric_stats import EER, minDCF
# from app.models.ecapa import EcapaTDNN


# ──────────────────────────────────────────────────────────────────
# TODO(W4) Step 1: Generate trial pairs from test set
# ──────────────────────────────────────────────────────────────────
# A trial pair = (utt_path_1, utt_path_2, label)
#   label = 1  → same speaker  (genuine)
#   label = 0  → different speakers (impostor)
#
# Recommended: 10,000 pairs (5,000 genuine + 5,000 impostor)
# MUST be speaker-disjoint from training set.
#
# def generate_trial_pairs(test_dir: str, n_pairs: int = 10000):
#     speakers = {spk.name: list(spk.glob("*.wav"))
#                 for spk in Path(test_dir).iterdir() if spk.is_dir()}
#
#     genuine, impostor = [], []
#
#     # Genuine pairs: two utterances from the SAME speaker
#     for spk, utts in speakers.items():
#         if len(utts) >= 2:
#             for u1, u2 in combinations(utts, 2):
#                 genuine.append((str(u1), str(u2), 1))
#
#     # Impostor pairs: one utterance each from DIFFERENT speakers
#     spk_list = list(speakers.keys())
#     for _ in range(n_pairs * 2):   # generate extra, then sample
#         s1, s2 = random.sample(spk_list, 2)
#         u1 = random.choice(speakers[s1])
#         u2 = random.choice(speakers[s2])
#         impostor.append((str(u1), str(u2), 0))
#
#     random.shuffle(genuine)
#     random.shuffle(impostor)
#     half = n_pairs // 2
#     return genuine[:half] + impostor[:half]


# ──────────────────────────────────────────────────────────────────
# TODO(W4) Step 2: Score all trial pairs
# ──────────────────────────────────────────────────────────────────
# def score_pairs(pairs, model: EcapaTDNN):
#     positive_scores, negative_scores = [], []
#     for path1, path2, label in pairs:
#         emb1 = model.extract_embedding(path1)
#         emb2 = model.extract_embedding(path2)
#         score = model.cosine_similarity(emb1, emb2)
#         if label == 1:
#             positive_scores.append(score)
#         else:
#             negative_scores.append(score)
#     return positive_scores, negative_scores


# ──────────────────────────────────────────────────────────────────
# TODO(W4) Step 3: Compute EER and minDCF
# ──────────────────────────────────────────────────────────────────
# positive_scores = torch.tensor(positive_scores)
# negative_scores = torch.tensor(negative_scores)
#
# eer, threshold = EER(positive_scores, negative_scores)
# min_dcf, _     = minDCF(positive_scores, negative_scores)
#
# print("=" * 40)
# print(f"  EER:       {eer * 100:.2f}%")
# print(f"  minDCF:    {min_dcf:.4f}")
# print(f"  Threshold: {threshold:.4f}  (our deployment threshold: 0.80)")
# print("=" * 40)
# # Copy these numbers into the academic report table.


# ──────────────────────────────────────────────────────────────────
# TODO(W4) Step 4: Baseline comparison
# ──────────────────────────────────────────────────────────────────
# Run eval TWICE:
#   1. With the raw VoxCeleb2 pretrained model (no fine-tuning)
#   2. With your fine-tuned ecapa_dearly.ckpt
#
# Report both rows in the table:
#
# | System                      | EER (%) | minDCF |
# |-----------------------------|---------|--------|
# | ECAPA-TDNN (VoxCeleb2 only) |  X.XX   | X.XXXX |
# | + Fine-tuned (VoxVietnam)   |  X.XX   | X.XXXX |
#
# The improvement proves fine-tuning on Vietnamese data helps.


if __name__ == "__main__":
    print("Evaluation script skeleton — implement TODO steps above.")
