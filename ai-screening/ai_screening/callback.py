"""Callback client: POST a screening result back to the Backend.

The Backend's internal callback endpoint is authenticated by the mutual
X-Internal-Secret shared secret (the same value the Backend sends on /screen).
"""

import logging

import httpx

from .config import settings
from .models import CallbackPayload

logger = logging.getLogger(__name__)

_TIMEOUT_SECONDS = 10.0


async def send_callback(payload: CallbackPayload) -> None:
    """POST the screening result to the configured backend callback URL.

    Best-effort: if the callback URL is not configured, logs and sends nothing
    (Requirement 15.6). Network errors are logged and swallowed — the Backend's
    timeout sweep advances the application if the result never arrives.
    """
    if not settings.backend_callback_url:
        logger.error(
            "No BACKEND_CALLBACK_URL configured; dropping result for application %s",
            payload.application_id,
        )
        return

    try:
        async with httpx.AsyncClient(timeout=_TIMEOUT_SECONDS) as client:
            response = await client.post(
                settings.backend_callback_url,
                headers={"X-Internal-Secret": settings.shared_secret},
                json=payload.model_dump(by_alias=True),
            )
        logger.info(
            "Callback for application %s returned HTTP %s",
            payload.application_id,
            response.status_code,
        )
    except httpx.HTTPError as exc:
        logger.warning(
            "Callback POST failed for application %s: %s",
            payload.application_id,
            exc,
        )
