# AI Screening Service — container image
#
# NOTE: This Dockerfile is authored in this environment but NOT built or run here
# (no Docker/Python toolchain available). Verify the build locally with:
#   docker build -t livana-ai-screening ./ai-screening
# or via docker compose (see docker-compose.yml at the repo root).

FROM python:3.11-slim

# Keep Python output unbuffered and skip writing .pyc files in the container.
ENV PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1

WORKDIR /app

# Install uv, the project package/dependency manager used by this service.
RUN pip install --no-cache-dir uv

# Install dependencies first so this layer is cached across code-only changes.
# uv.lock is optional: copy it when present so builds are reproducible.
COPY pyproject.toml ./
COPY uv.lock* ./

# Create the virtual environment and install runtime (non-dev) dependencies.
# Skip installing the project itself here since the source is copied next.
RUN uv sync --no-dev --no-install-project

# Copy the application package.
COPY ai_screening ./ai_screening

# Complete the install now that the source is present (installs the project).
RUN uv sync --no-dev

# FastAPI/uvicorn listens on 8000 (matches the compose port mapping).
EXPOSE 8000

# Run the FastAPI app via uvicorn inside the uv-managed environment.
CMD ["uv", "run", "uvicorn", "ai_screening.main:app", "--host", "0.0.0.0", "--port", "8000"]
