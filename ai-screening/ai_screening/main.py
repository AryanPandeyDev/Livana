"""FastAPI app for the AI screening service.

POST /screen accepts a screening request (authenticated by the mutual
X-Internal-Secret shared secret), returns 202 immediately, and runs the research
graph as a background task. On success it POSTs the result to the Backend
callback; on ANY failure it logs and sends nothing, relying on the Backend's
timeout sweep to advance the application (fail-open).
"""

import hmac
import logging

from fastapi import BackgroundTasks, Depends, FastAPI, Header, HTTPException, status

from .callback import send_callback
from .config import settings
from .graph import run_research
from .models import CallbackPayload, ScreenRequest, round_score

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(title="Livana AI Screening Service")


def verify_internal_secret(x_internal_secret: str | None = Header(default=None)) -> None:
    """Constant-time check of the X-Internal-Secret header against the shared secret."""
    expected = settings.shared_secret or ""
    provided = x_internal_secret or ""
    if not hmac.compare_digest(provided, expected):
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED,
                            detail="Invalid or missing internal secret")


@app.get("/health")
async def health() -> dict:
    return {"status": "ok"}


@app.post("/screen", status_code=status.HTTP_202_ACCEPTED)
async def screen(
    req: ScreenRequest,
    background_tasks: BackgroundTasks,
    _: None = Depends(verify_internal_secret),
) -> dict:
    """Accept a screening request and run research in the background."""
    background_tasks.add_task(run_screening, req)
    return {"status": "accepted", "applicationId": req.application_id}


async def run_screening(req: ScreenRequest) -> None:
    """Run the research graph and post the result back. Fail-open on any error.

    The per-request Gemini key lives only on `req` for the duration of this call.
    On any exception (research failure, invalid structured output, etc.) we log
    and send NOTHING — the Backend timeout sweep advances the application.
    """
    try:
        result = await run_research(req)
    except Exception as exc:  # research failure / invalid verdict — fail-open
        logger.warning("Screening failed for application %s: %s", req.application_id, exc)
        return

    if result is None:
        logger.warning("Screening produced no verdict for application %s", req.application_id)
        return

    payload = CallbackPayload(
        application_id=req.application_id,
        confidence_score=round_score(result.verdict.confidence_score),
        research_summary=result.summary,
        verdict=result.verdict.verdict,
    )
    await send_callback(payload)
