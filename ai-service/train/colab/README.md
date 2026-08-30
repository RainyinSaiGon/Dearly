# Dearly Colab T4 training

This workflow runs the prepared VIVOS experiment through the WSL Colab CLI.
It evaluates the pretrained ECAPA baseline, runs a one-epoch smoke test, and
then performs a clean 20-epoch fine-tune. Output is backed up to Google Drive
after every improved checkpoint.

## Completed reference run

The final report uses completed run `20260829-183455-ecapa-vivos-r4`, not a
planned run. It completed on a Tesla T4 with the exact 41/5/19
speaker-disjoint split. Final metrics were EER **1.28%**, minDCF **0.001628**,
and SID Top-1 **100.00%**; baseline EER was 4.40%. Its local archive is at
`models/training/20260829-183455-ecapa-vivos-r4/`.

Use the instructions below only for a future rerun. Rotate the Kaggle token
first; never reuse a token that was pasted into a chat, notebook, or log.

## One-time preparation

1. Rotate the Kaggle API token that was previously exposed. Save the fresh
   token in a temporary file outside this repository, for example
   `%TEMP%\\dearly-kaggle-token.txt`.
2. Start a T4 runtime named `dearly-ecapa` through WSL Colab CLI. Authenticate
   in the browser if prompted, then keep the terminal open until the command
   reports the session is ready:

   ```bash
   colab --auth oauth2 new -s dearly-ecapa --gpu T4
   colab status -s dearly-ecapa
   ```

   Verify CUDA and the Drive mount before uploading credentials:

   ```bash
   colab exec -s dearly-ecapa \
     -f /mnt/c/Users/Vu/schoolProject/Dearly/ai-service/train/colab/preflight.py
   ```
3. Mount Google Drive in that session:

   ```bash
   colab drivemount -s dearly-ecapa /content/drive
   ```

4. Create a non-secret run configuration locally and upload it with the token:

   ```json
   {
     "run_id": "20260829-ecapa-vivos",
     "drive_root": "/content/drive/MyDrive/Dearly/training",
     "source_revision": "<git commit plus dirty-state if applicable>",
     "colab_cli_version": "<output of colab version>"
   }
   ```

   ```bash
   colab upload -s dearly-ecapa <token-file> /content/dearly-input/kaggle_token.txt
   colab upload -s dearly-ecapa <run-config.json> /content/dearly-input/run_config.json
   ```

## Run and retrieve artifacts

From WSL, execute the local runner:

```bash
colab exec -s dearly-ecapa -f /mnt/c/Users/Vu/schoolProject/Dearly/ai-service/train/colab/colab_train.py --timeout 43200
```

The runner downloads VIVOS and the private offline bundle, removes the token
file, lets pip select only wheels compatible with the active Colab Python ABI,
validates the 41/5/19 speaker split, and writes artifacts under both
`/content/dearly-training/artifacts` and the configured Drive run folder.

After success, download the deployment checkpoint, archive, and execution log
into the local gitignored model directory:

```bash
colab download -s dearly-ecapa /content/dearly-training/artifacts/ecapa_dearly.ckpt /mnt/c/Users/Vu/schoolProject/Dearly/models/ecapa_dearly.ckpt
colab download -s dearly-ecapa /content/dearly-training/dearly-training-artifacts.zip /mnt/c/Users/Vu/schoolProject/Dearly/models/training/<run-id>/dearly-training-artifacts.zip
colab log -s dearly-ecapa -o /mnt/c/Users/Vu/schoolProject/Dearly/models/training/<run-id>/colab_execution.ipynb
```

Extract the archive into the same `<run-id>` directory. It contains
`baseline_evaluation.json`, `evaluation.json`, both training summaries, the
manifest, and the final checkpoint. The Google Drive run folder is the durable
backup if the Colab VM is released.

The final checkpoint is an embedding-model state dictionary.  Deploy it with
`ECAPA_MODEL_PATH=/models/ecapa_dearly.ckpt` while keeping
`ECAPA_MODEL_SOURCE=speechbrain/spkrec-ecapa-voxceleb` configured.
