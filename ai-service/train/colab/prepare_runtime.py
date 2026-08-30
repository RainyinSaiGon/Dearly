"""Create the private runtime input directory before Colab CLI uploads."""

from pathlib import Path


input_directory = Path("/content/dearly-input")
input_directory.mkdir(mode=0o700, parents=True, exist_ok=True)
input_directory.chmod(0o700)
print(f"ready: {input_directory}")
