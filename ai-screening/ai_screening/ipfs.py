"""Document_Fetch — resolve an IPFS Documents_Cid through the Pinata gateway.

The AI screening graph optionally cross-references uploaded supporting documents
against an application. Those documents live in IPFS and are addressed by a
single Content Identifier (CID). A CID can point at two very different things:

Single-file CID
    The gateway responds with the file bytes directly and a concrete media
    content-type (e.g. ``application/pdf`` or ``image/png``). We return that as
    one :class:`FetchedDocument`.

Directory CID
    The gateway responds with a *listing* of the directory's children rather
    than file bytes. Kubo/Pinata gateways render this as an HTML index
    (``text/html``) and can also emit a JSON listing (``application/json``).
    We parse the listing for child entry names and fetch each child from
    ``<gateway>/<cid>/<name>``.

    NOTE: the exact shape of a directory listing is gateway-specific. The HTML
    and JSON parsers here are best-effort and should be verified against the
    real Pinata gateway response before relying on directory CIDs in production.

Caps and guards (best-effort, never fatal)
    * At most :data:`settings.max_document_files` child files are fetched from a
      directory CID.
    * At most :data:`settings.max_document_bytes` bytes are read per file; a file
      whose ``Content-Length`` (or streamed size) exceeds the cap is skipped.
    * Only document/image content-types in :data:`ALLOWED_DOC_TYPES` are kept;
      anything else (HTML, octet-stream, video, ...) is skipped.
    * Any failure — a 404, a timeout, a connection error, an unparseable
      listing, or simply nothing usable — yields an EMPTY list rather than an
      exception. The caller treats an empty result as "documents not evaluated"
      (Requirement 4.5) and must never let it drive a FAIL verdict on its own
      (Requirement 4.6).

Requirements: 4.1, 4.5, 4.7.
"""

from __future__ import annotations

import json
import logging
import re
from dataclasses import dataclass
from urllib.parse import unquote, urlsplit

import httpx

from .config import settings

logger = logging.getLogger(__name__)

# Total request timeout (connect + read) for every gateway call.
_TIMEOUT_SECONDS = 15.0

# Content-types we are willing to keep as documents. Anything else is skipped.
ALLOWED_DOC_TYPES: set[str] = {
    "application/pdf",
    "image/jpeg",
    "image/png",
    "image/webp",
    "text/plain",
}

# Content-types that indicate a directory *listing* rather than a file body.
_DIRECTORY_TYPES: set[str] = {"text/html", "application/json"}

# Best-effort extension map for naming single-file CIDs that carry no filename.
_EXTENSION_BY_TYPE: dict[str, str] = {
    "application/pdf": ".pdf",
    "image/jpeg": ".jpg",
    "image/png": ".png",
    "image/webp": ".webp",
    "text/plain": ".txt",
}

# Matches href="..." attributes in an HTML directory listing.
_HREF_RE = re.compile(r'href\s*=\s*["\']([^"\']+)["\']', re.IGNORECASE)


@dataclass
class FetchedDocument:
    """A single document fetched from IPFS.

    Attributes:
        filename: The child entry name, or a synthesized ``<cid><ext>`` name for
            a single-file CID that carries no filename.
        content_type: The normalized (lowercased, parameters stripped) media type.
        data: The raw file bytes, never larger than ``settings.max_document_bytes``.
    """

    filename: str
    content_type: str
    data: bytes


def _normalize_content_type(raw: str | None) -> str:
    """Lowercase a content-type and drop any ``; charset=...`` parameters."""
    if not raw:
        return ""
    return raw.split(";", 1)[0].strip().lower()


def _join(base: str, *parts: str) -> str:
    """Join a gateway base with path parts using exactly one slash between each."""
    url = base.rstrip("/")
    for part in parts:
        url = f"{url}/{part.strip('/')}"
    return url


def _filename_for_single_file(cid: str, content_type: str) -> str:
    """Synthesize a filename for a single-file CID using a type-based extension."""
    return f"{cid}{_EXTENSION_BY_TYPE.get(content_type, '')}"


async def _read_capped(response: httpx.Response, limit: int) -> bytes | None:
    """Stream a response body, returning ``None`` if it exceeds ``limit`` bytes.

    Reading incrementally means an oversized file is abandoned without ever
    buffering more than ``limit`` (+ one chunk) bytes in memory.
    """
    chunks: list[bytes] = []
    total = 0
    async for chunk in response.aiter_bytes():
        total += len(chunk)
        if total > limit:
            return None
        chunks.append(chunk)
    return b"".join(chunks)


def _content_length_exceeds(response: httpx.Response, limit: int) -> bool:
    """Return True when a declared ``Content-Length`` already blows the cap."""
    raw = response.headers.get("content-length")
    if raw is None:
        return False
    try:
        return int(raw) > limit
    except ValueError:
        return False


def _names_from_html(body: str) -> list[str]:
    """Extract child entry names from an HTML gateway directory listing.

    The gateway renders each child as an anchor whose href points at the child
    path (e.g. ``/ipfs/<cid>/<name>`` or a relative ``<name>``). We take the last
    path segment of each href, url-decode it, and discard navigation links.
    """
    names: list[str] = []
    seen: set[str] = set()
    for href in _HREF_RE.findall(body):
        # Drop query/fragment, then take the final non-empty path segment.
        path = urlsplit(href).path
        segment = path.rstrip("/").rsplit("/", 1)[-1]
        name = unquote(segment).strip()
        if not name or name in {".", ".."}:
            continue
        if name in seen:
            continue
        seen.add(name)
        names.append(name)
    return names


def _names_from_json(body: bytes) -> list[str]:
    """Extract child entry names from a JSON gateway directory listing.

    Handles the common shapes emitted by IPFS gateways/APIs:
      * ``{"Objects": [{"Links": [{"Name": ...}]}]}``  (``/api/v0/ls``)
      * ``{"Links": [{"Name": ...}]}``
      * ``{"Entries": [{"Name": ...}]}``               (dag-json gateway listing)
    Unknown shapes simply yield no names (best-effort).
    """
    try:
        parsed = json.loads(body)
    except (ValueError, TypeError):
        return []

    link_lists: list[list] = []
    if isinstance(parsed, dict):
        objects = parsed.get("Objects")
        if isinstance(objects, list):
            for obj in objects:
                if isinstance(obj, dict) and isinstance(obj.get("Links"), list):
                    link_lists.append(obj["Links"])
        if isinstance(parsed.get("Links"), list):
            link_lists.append(parsed["Links"])
        if isinstance(parsed.get("Entries"), list):
            link_lists.append(parsed["Entries"])

    names: list[str] = []
    seen: set[str] = set()
    for links in link_lists:
        for entry in links:
            if not isinstance(entry, dict):
                continue
            name = entry.get("Name") or entry.get("name")
            if not isinstance(name, str):
                continue
            name = name.strip()
            if not name or name in seen:
                continue
            seen.add(name)
            names.append(name)
    return names


async def _fetch_child(
    client: httpx.AsyncClient, cid: str, name: str
) -> FetchedDocument | None:
    """Fetch a single child of a directory CID, applying type and size guards.

    Returns ``None`` (and never raises) when the child is missing, the wrong
    type, too large, or fails to download.
    """
    url = _join(settings.ipfs_gateway_url, cid, name)
    try:
        async with client.stream("GET", url) as response:
            if response.status_code != 200:
                return None
            content_type = _normalize_content_type(response.headers.get("content-type"))
            if content_type not in ALLOWED_DOC_TYPES:
                return None
            if _content_length_exceeds(response, settings.max_document_bytes):
                return None
            data = await _read_capped(response, settings.max_document_bytes)
            if data is None:
                return None
            return FetchedDocument(filename=name, content_type=content_type, data=data)
    except httpx.HTTPError as exc:
        logger.warning("Failed to fetch child %s/%s: %s", cid, name, exc)
        return None


async def _fetch_single_file(
    response: httpx.Response, cid: str, content_type: str
) -> list[FetchedDocument]:
    """Read a single-file CID response (already streaming) into one document."""
    if _content_length_exceeds(response, settings.max_document_bytes):
        return []
    data = await _read_capped(response, settings.max_document_bytes)
    if data is None:
        return []
    filename = _filename_for_single_file(cid, content_type)
    return [FetchedDocument(filename=filename, content_type=content_type, data=data)]


async def fetch_documents(cid: str) -> list[FetchedDocument]:
    """Fetch documents for ``cid`` through the configured IPFS gateway.

    Resolves both single-file and directory CIDs (see the module docstring),
    applies the configured file-count and per-file byte caps, and keeps only
    document/image content-types.

    This operation is best-effort and non-fatal: it returns an EMPTY list on a
    blank CID, a 404, a timeout, a connection error, an unparseable listing, or
    when nothing usable is found. The caller treats an empty result as
    "documents not evaluated" (Requirements 4.5, 4.6).

    Args:
        cid: The IPFS Content Identifier to resolve. May be ``None``/blank.

    Returns:
        A list of :class:`FetchedDocument`, at most ``settings.max_document_files``
        entries long. Empty when the CID is unresolvable or unreadable.
    """
    cid = (cid or "").strip()
    if not cid:
        return []

    url = _join(settings.ipfs_gateway_url, cid)
    try:
        async with httpx.AsyncClient(
            timeout=_TIMEOUT_SECONDS, follow_redirects=True
        ) as client:
            # First GET inspects the top-level content-type to decide single vs directory.
            async with client.stream("GET", url) as response:
                if response.status_code != 200:
                    logger.info(
                        "IPFS CID %s returned HTTP %s; treating as unresolvable",
                        cid,
                        response.status_code,
                    )
                    return []

                content_type = _normalize_content_type(
                    response.headers.get("content-type")
                )

                if content_type in ALLOWED_DOC_TYPES:
                    return await _fetch_single_file(response, cid, content_type)

                if content_type in _DIRECTORY_TYPES:
                    listing = await _read_capped(response, settings.max_document_bytes)
                    if listing is None:
                        return []
                    if content_type == "application/json":
                        names = _names_from_json(listing)
                    else:
                        names = _names_from_html(listing.decode("utf-8", "replace"))
                else:
                    # Unknown top-level type (octet-stream, video, ...) — nothing usable.
                    logger.info(
                        "IPFS CID %s has unsupported content-type %r; skipping",
                        cid,
                        content_type,
                    )
                    return []

            # Directory path: fetch up to the configured number of children.
            documents: list[FetchedDocument] = []
            for name in names[: settings.max_document_files]:
                child = await _fetch_child(client, cid, name)
                if child is not None:
                    documents.append(child)
            return documents
    except httpx.HTTPError as exc:
        logger.warning("Document fetch failed for CID %s: %s", cid, exc)
        return []
