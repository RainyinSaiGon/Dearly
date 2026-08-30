# Kaggle training handoff (legacy)

Kaggle GPU execution is not the final Dearly path because the account could not
complete Kaggle phone verification. The old `kaggle_train.py` remains for
reference only and must not supply final report metrics.

The canonical final pipeline is **VIVOS on Google Colab T4**. See
[../colab/README.md](../colab/README.md). It uses the prepared Kaggle datasets
only as private inputs, validates the exact 41/5/19 speaker-disjoint VIVOS
split, backs up every newly best checkpoint to Drive, and produces the final
deployment artifact and report metrics. The canonical run completed as
`20260829-183455-ecapa-vivos-r4`; see `models/training/` and
`../model_guide.md` for the evidence-backed result.
