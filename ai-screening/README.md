# AI Screening Service

A stateless [FastAPI](https://fastapi.tiangolo.com/) + [LangGraph](https://langchain-ai.github.io/langgraph/)
service that screens NGO applications for the Livana platform. For a single
application it researches whether the organization actually exists as a
registered NGO, whether the supplied official email is associated with that
organization, and — optionally — whether any uploaded supporting documents are
*consistent with* the application. It runs that research with a LangGraph graph,
synthesizes a confidence score and a `PASS`/`FAIL` verdict, and posts the result
back to the Livana backend through a webhook callback. The service holds no
database and remembers nothing between requests: each run is self-contained and
everything it learns is discarded when the run ends.

## How it fits into Livana

Screening is asynchronous and best-effort. The backend owns the NGO application
status machine; this service is a fire-and-forget research worker:

1. An NGO submits an application. The backend verifies the official email, moves
   the application to `AI_SCREENING`, and fires an HTTP `POST /screen` at this
   service. The request carries the application fields, a **per-request Gemini
   API key** (decrypted and forwarded by the backend), and the shared secret in
   the `X-Internal-Secret` header.
2. This service authenticates the secret and immediately answers `202 Accepted`,
   then runs the LangGraph research graph in a background task. The backend does
   not block on the research.
3. On success — and only with a well-formed verdict — the service `POST`s the
   result back to the backend's internal callback endpoint
   (`POST /api/v1/internal/screening-callback`), again carrying the shared
   secret. The backend stores the score, summary, and verdict and advances the
   application to `PENDING_REVIEW`.

```
NGO submit ─▶ Backend (DRAFT → AI_SCREENING)
                 │  POST /screen  (Gemini key + X-Internal-Secret)
                 ▼
        AI Screening Service ──▶ 202 Accepted (returns immediately)
                 │  background LangGraph research
                 ▼
        POST /api/v1/internal/screening-callback  (X-Internal-Secret)
                 │
                 ▼
        Backend stores result → PENDING_REVIEW
```

## Architecture

The service lives in the `ai_screening/` package:

| Module | Responsibility |
|---|---|
| `config.py` | Environment-driven settings (callback URL, shared secret, IPFS gateway, document caps, Gemini model id, Tavily key) loaded once via pydantic-settings. |
| `models.py` | Pydantic models mirroring the backend JSON contract (`ScreenRequest`, `StructuredVerdict`, `CallbackPayload`) plus the `ResearchState` graph state. |
| `ipfs.py` | `fetch_documents` — best-effort document retrieval from the IPFS gateway for single-file and directory CIDs, with per-file byte and file-count caps. |
| `graph.py` | The LangGraph `Research_Graph`: `extract_facts → research_existence → research_email_link → (conditional) read_documents → synthesize → structured_verdict`. |
| `callback.py` | `send_callback` — posts the `CallbackPayload` back to the backend's callback URL with the shared-secret header. |
| `main.py` | The FastAPI app: `GET /health`, `POST /screen` (202 + background task), the `X-Internal-Secret` auth dependency, and the `run_screening` orchestration. |

## Setup and run

This service is managed with [`uv`](https://docs.astral.dev/uv/). All commands run
from the `ai-screening/` directory.

```bash
# Install dependencies (creates/uses .venv from pyproject.toml + uv.lock)
uv sync

# Run the test suite (unit + property tests)
uv run pytest

# Run the dev server with hot reload on port 8000
uv run uvicorn ai_screening.main:app --port 8000 --reload
```

Copy `.env.example` to `.env` and fill in real values before running locally.

## Environment variables

All service configuration is read from the environment (or a local `.env`
file). These are **infrastructure** config — set once per deployment.

| Variable | Description | Scope |
|---|---|---|
| `BACKEND_CALLBACK_URL` | Backend internal callback endpoint where screening results are `POST`ed (e.g. `http://localhost:8080/api/v1/internal/screening-callback`). If unset, results are dropped rather than sent. | infra |
| `SHARED_SECRET` | Mutual `X-Internal-Secret` value used in **both** directions. Must match the backend's `ai-screening.shared-secret`. | infra |
| `IPFS_GATEWAY_URL` | Base URL (trailing slash) used to fetch application documents, e.g. `https://gateway.pinata.cloud/ipfs/`. | infra |
| `MAX_DOCUMENT_FILES` | Cap on how many files are fetched from a directory CID (default `5`). | infra |
| `MAX_DOCUMENT_BYTES` | Cap on bytes read per document file (default `10485760`, i.e. 10 MB). | infra |
| `GEMINI_MODEL` | Gemini model id used for synthesis and structured output (e.g. `gemini-2.0-flash`). | infra |
| `TAVILY_API_KEY` | Platform Tavily key for web search. **Not** admin-managed and **never** sent over the wire. | infra |

### Note: the Gemini API key is not an environment variable

The Gemini API key is intentionally **not** configured here. It is operational
config an admin rotates without a redeploy, so the backend keeps it encrypted
and forwards it **per request** in the `/screen` body (`geminiApiKey`). Each
screening run uses that per-request key as a local value and discards it when
the run ends. The Tavily key, by contrast, is static platform infrastructure and
does live in the environment.

## Authentication

Both internal hops are authenticated with a single mutual shared secret carried
in the `X-Internal-Secret` header:

- **Inbound** `POST /screen` — the service rejects any request whose header does
  not match `SHARED_SECRET` (constant-time compare) with `401 Unauthorized`.
- **Outbound** callback — the service sends the same secret to the backend's
  callback endpoint.

`SHARED_SECRET` here must be identical to the backend's `ai-screening.shared-secret`
value, or the two services cannot talk to each other.

## Endpoints

| Method | Path | Description |
|---|---|---|
| `GET` | `/health` | Liveness check. |
| `POST` | `/screen` | Accepts a screening request, returns `202 Accepted`, and runs research in the background. Missing/invalid body fields yield `422`; a bad/absent secret yields `401`. |

## Fail-open behavior

Screening is **best-effort and never blocking**. Any failure during a run — an
invalid Gemini key, a research exception, a malformed or out-of-range verdict, a
timeout, or an unresolvable callback URL — results in the service sending
**nothing** back to the backend. The backend's scheduled timeout sweep then
advances the stale `AI_SCREENING` application to `PENDING_REVIEW` for manual
review. A flaky or unavailable AI service can therefore never strand a
legitimate NGO; in the worst case the application simply lands in the human
review queue without an AI verdict.

## Out of scope: document authenticity

This service does **not** judge whether documents are authentic, genuine,
forged, or fake. Document authenticity and forgery detection are explicitly out
of scope. When documents are evaluated, the service only checks whether their
contents are **consistent with** the application (organization name,
registration number, email domain), and that check never drives a `FAIL` on its
own.

## Verification

This service is authored in the Livana repository but **not run or verified**
there — that environment has no Python toolchain. Install dependencies and run
the test suite and a smoke run on your own machine:

```bash
uv sync
uv run pytest
uv run uvicorn ai_screening.main:app --port 8000
```
