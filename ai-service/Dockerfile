# ─── Python AI Service Dockerfile ─────────────────────────────────
# Local Docker Desktop inference runs on CPU. Model training remains on Colab GPU.
FROM python:3.11-slim

WORKDIR /app

# System dependencies for audio processing
RUN apt-get update && apt-get install -y --no-install-recommends \
    ffmpeg \
    libsndfile1 \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Pre-install pip & setuptools for build-isolation wheel builds (openai-whisper)
RUN pip install --no-cache-dir --upgrade pip setuptools

# PyPI's Linux Torch wheel pulls the CUDA runtime even in a CPU-only image.
# Install the matching official CPU wheels first so requirements.txt reuses them.
RUN pip install --no-cache-dir \
    --index-url https://download.pytorch.org/whl/cpu \
    torch==2.5.1+cpu \
    torchaudio==2.5.1+cpu

# Install Python deps
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

EXPOSE 5000

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "5000"]
