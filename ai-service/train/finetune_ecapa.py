"""
Fine-tune ECAPA-TDNN on VoxVietnam (or Vietnam-Celeb).

Usage:
    python train/finetune_ecapa.py train/hparams/train_ecapa.yaml

Requirements:
    pip install speechbrain torch torchaudio

Folder structure expected (see hparams/train_ecapa.yaml for paths):
    data/voxvietnam/
        train/  speaker_id/  *.wav
        val/    speaker_id/  *.wav
        test/   speaker_id/  *.wav
"""

import os
import sys
import torch
import torchaudio
import logging
import hyperpyyaml
import speechbrain as sb
from pathlib import Path
from torch.utils.data import DataLoader
from speechbrain.inference.speaker import EncoderClassifier

logger = logging.getLogger(__name__)


# ──────────────────────────────────────────────────────────────────
# TODO(W4) Step 1: Prepare data manifest CSV
# ──────────────────────────────────────────────────────────────────
# Each CSV row: utt_id, audio_path, speaker_id, duration
#
# Run this once to generate manifests from your data folder:
#
# def make_manifest(split_dir: str, out_csv: str):
#     rows = []
#     for spk_dir in Path(split_dir).iterdir():
#         for wav in spk_dir.glob("*.wav"):
#             info = torchaudio.info(str(wav))
#             duration = info.num_frames / info.sample_rate
#             rows.append(f"{wav.stem},{wav},{spk_dir.name},{duration:.2f}")
#     with open(out_csv, "w") as f:
#         f.write("utt_id,audio_path,speaker_id,duration\n")
#         f.write("\n".join(rows))
#
# make_manifest("data/voxvietnam/train", "data/train.csv")
# make_manifest("data/voxvietnam/val",   "data/val.csv")
# make_manifest("data/voxvietnam/test",  "data/test.csv")


# ──────────────────────────────────────────────────────────────────
# TODO(W4) Step 2: Define the SpeechBrain Brain class
# ──────────────────────────────────────────────────────────────────
# SpeechBrain uses a "Brain" class that defines forward + training steps.
# See: https://speechbrain.readthedocs.io/en/latest/API/speechbrain.core.html
#
# class SpeakerBrain(sb.Brain):
#
#     def compute_forward(self, batch, stage):
#         """Extract embeddings from audio."""
#         wavs, lens = batch.sig
#         wavs = wavs.to(self.device)
#         # Feature extraction: FBANK or raw waveform
#         feats = self.modules.compute_features(wavs)
#         feats = self.modules.mean_var_norm(feats, lens)
#         # ECAPA-TDNN embedding
#         embeddings = self.modules.embedding_model(feats)
#         # AAM-Softmax output (only during training)
#         if stage == sb.Stage.TRAIN:
#             outputs = self.modules.classifier(embeddings)
#             return outputs, lens
#         return embeddings
#
#     def compute_objectives(self, predictions, batch, stage):
#         """Compute AAM-Softmax loss."""
#         predictions, lens = predictions
#         spkids, _ = batch.spk_id_encoded
#         loss = self.hparams.compute_cost(predictions, spkids, lens)
#         if stage != sb.Stage.TRAIN:
#             self.error_metrics.append(batch.id, predictions, spkids)
#         return loss
#
#     def on_stage_end(self, stage, stage_loss, epoch):
#         if stage == sb.Stage.VALID:
#             self.hparams.lr_annealing(stage_loss)
#             self.checkpointer.save_and_keep_only(
#                 meta={"loss": stage_loss}, min_keys=["loss"]
#             )


# ──────────────────────────────────────────────────────────────────
# TODO(W4) Step 3: Load pretrained VoxCeleb2 checkpoint
# ──────────────────────────────────────────────────────────────────
# pretrained = EncoderClassifier.from_hparams(
#     source="speechbrain/spkrec-ecapa-voxceleb",
#     savedir="pretrained_models/spkrec-ecapa-voxceleb",
# )
# # Transfer weights into your Brain's embedding_model:
# brain.modules.embedding_model.load_state_dict(
#     pretrained.mods.embedding_model.state_dict()
# )


# ──────────────────────────────────────────────────────────────────
# TODO(W4) Step 4: Run training
# ──────────────────────────────────────────────────────────────────
# brain.fit(
#     epoch_counter=brain.hparams.epoch_counter,
#     train_set=train_data,
#     valid_set=valid_data,
#     train_loader_kwargs=hparams["train_dataloader_options"],
#     valid_loader_kwargs=hparams["valid_dataloader_options"],
# )
# Best checkpoint auto-saved by SpeechBrain checkpointer.


# ──────────────────────────────────────────────────────────────────
# TODO(W4) Step 5: Export best checkpoint
# ──────────────────────────────────────────────────────────────────
# import shutil
# best = Path("results/ECAPA/") / hparams["seed"] / "save" / "best_model.ckpt"
# shutil.copy(best, "../../models/ecapa_dearly.ckpt")
# print("Saved to models/ecapa_dearly.ckpt")


if __name__ == "__main__":
    # TODO(W4): Wire up the steps above using the hparams YAML
    print("Training script skeleton — implement TODO steps above.")
    print("Reference: https://github.com/speechbrain/speechbrain/tree/develop/recipes/VoxCeleb/SpeakerRec")
