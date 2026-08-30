"""Verify the active Colab runtime before uploading training secrets."""

import json
from pathlib import Path

import torch


details = {
    "torch": torch.__version__,
    "cuda_available": torch.cuda.is_available(),
    "cuda_version": torch.version.cuda,
    "gpu": torch.cuda.get_device_name(0) if torch.cuda.is_available() else None,
    "drive_mounted": Path("/content/drive/MyDrive").is_dir(),
}
print(json.dumps(details, indent=2))
if not details["cuda_available"]:
    raise RuntimeError("CUDA is unavailable")
if "T4" not in str(details["gpu"]):
    raise RuntimeError(f"expected an NVIDIA T4, received {details['gpu']!r}")
if not details["drive_mounted"]:
    raise RuntimeError("Google Drive is not mounted at /content/drive")
