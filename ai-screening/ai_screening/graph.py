"""Research_Graph — the LangGraph screening pipeline.

This module implements the ``Research_Graph`` described in the design document's
"AI Service — Research_Graph (LangGraph)" section. It performs best-effort,
stateless research for a single screening request and emits a structured verdict.

Node flow (see the design's mermaid diagram)::

    START
      → extract_facts
      → research_existence
      → research_email_link
      → (conditional) read_documents      # only when documentsCid is present
      → synthesize
      → structured_verdict
      → END

Key handling
------------
The Gemini API key is **per-request**: it arrives in ``state["request"].gemini_api_key``
and is used only to construct the LLM for that run. It is NEVER read from the
environment and NEVER logged. The Tavily key is platform **infrastructure**
(``settings.tavily_api_key``) and likewise never crosses the wire.

Fail-open / best-effort discipline
-----------------------------------
* Web search (Tavily) calls are wrapped defensively — a flaky search degrades to
  empty findings rather than aborting the run.
* Document evaluation is optional. A blank/unresolvable/unreadable ``documentsCid``
  records ``documents_evaluated=False`` and never drives a ``FAIL`` on its own
  (Requirements 4.5, 4.6). Document findings are framed as "consistent with the
  application"; the prompt explicitly excludes authenticity/forgery judgements
  (Requirements 4.3, 4.4).
* The ``synthesize`` and ``structured_verdict`` LLM calls are core: if they raise
  (including ``with_structured_output`` rejecting an out-of-range/invalid verdict —
  Property 4), the exception propagates out of :func:`run_research` so ``main.py``
  can decline to send a callback.

Library-version note
---------------------
The exact LangGraph / LangChain Gemini APIs (``StateGraph`` construction, the
multimodal message-content shape, and ``with_structured_output``) should be
verified by the user against the installed library versions. This workspace has
no Python toolchain, so the module is authored but not executed here.

Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 4.2, 4.3, 4.4, 4.5, 4.6.
"""

from __future__ import annotations

import asyncio
import base64
import logging
from dataclasses import dataclass

from langchain_core.messages import HumanMessage
from langchain_google_genai import ChatGoogleGenerativeAI
from langgraph.graph import END, START, StateGraph
from tavily import TavilyClient

from .config import settings
from .ipfs import FetchedDocument, fetch_documents
from .models import ResearchState, ScreenRequest, StructuredVerdict

logger = logging.getLogger(__name__)

# Number of web-search results to fold into each research node's findings.
_MAX_SEARCH_RESULTS = 5

# Note recorded whenever documents are not evaluated. Must never push a FAIL on
# its own (Requirement 4.6).
_DOCS_NOT_EVALUATED_NOTE = (
    "Supporting documents were not evaluated (none provided, or they could not be "
    "resolved/read). This absence is not itself a negative signal."
)


@dataclass
class ResearchResult:
    """Outcome of a completed research run.

    Attributes:
        summary: The human-readable Research_Summary produced by ``synthesize``.
        verdict: The validated :class:`StructuredVerdict` (score + PASS/FAIL).
    """

    summary: str
    verdict: StructuredVerdict


# --------------------------------------------------------------------------- #
# LLM / search helpers
# --------------------------------------------------------------------------- #
def _build_llm(request: ScreenRequest) -> ChatGoogleGenerativeAI:
    """Construct a Gemini chat model bound to the per-request API key.

    The key comes exclusively from the request body and is held only for the
    lifetime of this object. It is never logged or persisted.

    NOTE: the ``google_api_key`` keyword should be verified against the installed
    ``langchain-google-genai`` version.
    """
    return ChatGoogleGenerativeAI(
        model=settings.gemini_model,
        google_api_key=request.gemini_api_key,
        temperature=0.0,
    )


def _run_tavily(query: str) -> dict:
    """Execute a single (blocking) Tavily search. Wrapped by :func:`_search_text`."""
    client = TavilyClient(api_key=settings.tavily_api_key)
    return client.search(query=query, max_results=_MAX_SEARCH_RESULTS)


async def _search_text(query: str) -> str:
    """Run a Tavily web search and concatenate the top result snippets.

    Best-effort: any failure (network, auth, unexpected shape) is logged and
    yields an empty string so the run can still complete. The Tavily client call
    is synchronous, so it runs in a worker thread to avoid blocking the event
    loop. The query text is safe to log; API keys are not included in it.
    """
    try:
        response = await asyncio.to_thread(_run_tavily, query)
    except Exception as exc:  # noqa: BLE001 - search is best-effort, never fatal
        logger.warning("Tavily search failed for query %r: %s", query, exc)
        return ""

    results = response.get("results", []) if isinstance(response, dict) else []
    snippets: list[str] = []
    for result in results[:_MAX_SEARCH_RESULTS]:
        if not isinstance(result, dict):
            continue
        title = (result.get("title") or "").strip()
        url = (result.get("url") or "").strip()
        content = (result.get("content") or "").strip()
        snippets.append(f"- {title} ({url}): {content}")
    return "\n".join(snippets)


# --------------------------------------------------------------------------- #
# Graph nodes
# --------------------------------------------------------------------------- #
async def extract_facts(state: ResearchState) -> ResearchState:
    """Derive the email domain from ``officialEmail`` (no LLM call).

    Splits on the final ``@`` and stores the lowercased domain in the state so
    later nodes can assess whether it matches the organization's web presence.
    """
    request = state["request"]
    email = (request.official_email or "").strip()
    domain = email.rsplit("@", 1)[1].strip().lower() if "@" in email else ""
    logger.info("extract_facts: derived email domain %r", domain)
    return {"email_domain": domain}


async def research_existence(state: ResearchState) -> ResearchState:
    """Search the web for a real, registered NGO matching name + reg number.

    Implements Requirement 3.1. Stores the concatenated top result snippets as
    ``existence_findings``.
    """
    request = state["request"]
    query = (
        f'"{request.org_name}" "{request.registration_number}" '
        f"NGO OR nonprofit registered"
    )
    search_text = await _search_text(query)
    if search_text:
        findings = (
            f"Existence search for organization '{request.org_name}' "
            f"(registration number '{request.registration_number}'):\n{search_text}"
        )
    else:
        findings = (
            f"No conclusive web evidence was found for organization "
            f"'{request.org_name}' with registration number "
            f"'{request.registration_number}'."
        )
    return {"existence_findings": findings}


async def research_email_link(state: ResearchState) -> ResearchState:
    """Search for the org's web presence and assess email-domain association.

    Implements Requirement 3.2. Looks for the organization's official site and
    checks whether the official-email domain appears in the identified web
    presence, recording the assessment in ``email_link_findings``.
    """
    request = state["request"]
    domain = state.get("email_domain", "")
    query = f'"{request.org_name}" official website contact'
    search_text = await _search_text(query)

    domain_present = bool(domain) and domain in search_text.lower()
    if not domain:
        assessment = (
            "No email domain could be derived from the supplied official email, "
            "so domain association could not be assessed."
        )
    elif domain_present:
        assessment = (
            f"The official email domain '{domain}' appears within the "
            f"organization's identified web presence, which is consistent with "
            f"the email being associated with the organization."
        )
    else:
        assessment = (
            f"The official email domain '{domain}' was not clearly found within "
            f"the organization's identified web presence."
        )

    findings = (
        f"{assessment}\n\nWeb-presence search results:\n"
        f"{search_text or '(no results)'}"
    )
    return {"email_link_findings": findings}


def _build_document_message(prompt: str, documents: list[FetchedDocument]) -> HumanMessage:
    """Build a Gemini multimodal message: a text instruction plus inline files.

    Each document is attached as an inline ``media`` content part carrying its
    base64-encoded bytes and media type.

    NOTE: the multimodal content-block shape (``{"type": "media", "mime_type":
    ..., "data": <base64>}``) should be verified against the installed
    ``langchain-google-genai`` version; older/newer releases use slightly
    different keys.
    """
    content: list[dict] = [{"type": "text", "text": prompt}]
    for doc in documents:
        content.append(
            {
                "type": "media",
                "mime_type": doc.content_type,
                "data": base64.b64encode(doc.data).decode("ascii"),
            }
        )
    return HumanMessage(content=content)


async def read_documents(state: ResearchState) -> ResearchState:
    """(Conditional) Cross-reference supporting documents against the application.

    Only reached when ``documentsCid`` is non-blank (see :func:`_should_read_documents`).
    Fetches documents through the IPFS gateway and, when any are readable, sends
    them to Gemini multimodal to extract the organization name, registration
    number, and email domain and compare them with the application fields.

    Document findings are framed strictly as *consistency with the application*;
    the prompt forbids authenticity/forgery/genuineness judgements
    (Requirements 4.2, 4.3, 4.4). A missing/empty/unreadable fetch — or any error
    talking to Gemini — records ``documents_evaluated=False`` with a neutral note
    and never produces a negative signal on its own (Requirements 4.5, 4.6).
    """
    request = state["request"]
    cid = (request.documents_cid or "").strip()
    if not cid:
        # Defensive: the conditional edge should already have skipped this node.
        return {"documents_evaluated": False, "document_findings": _DOCS_NOT_EVALUATED_NOTE}

    documents = await fetch_documents(cid)
    if not documents:
        logger.info("read_documents: CID %s yielded no readable documents", cid)
        return {"documents_evaluated": False, "document_findings": _DOCS_NOT_EVALUATED_NOTE}

    prompt = (
        "You are reviewing supporting documents an NGO submitted with its "
        "application. Read the attached document(s) and extract any organization "
        "name, registration/identification number, and email domain you can find. "
        "Then compare those extracted facts against the application details below "
        "and report whether the documents are CONSISTENT WITH THE APPLICATION.\n\n"
        "IMPORTANT FRAMING RULES:\n"
        "- Describe findings only in terms of consistency with the application "
        "(e.g. 'consistent with the application' or 'not consistent with the "
        "application').\n"
        "- Do NOT assess, claim, or imply that documents are 'authentic', "
        "'genuine', real, forged, or fake. Document authenticity and forgery "
        "detection are explicitly out of scope.\n\n"
        f"Application organization name: {request.org_name}\n"
        f"Application registration number: {request.registration_number}\n"
        f"Application email domain: {state.get('email_domain', '')}\n"
    )

    try:
        llm = _build_llm(request)
        message = _build_document_message(prompt, documents)
        response = await llm.ainvoke([message])
        findings = response.content if isinstance(response.content, str) else str(response.content)
        return {"documents_evaluated": True, "document_findings": findings}
    except Exception as exc:  # noqa: BLE001 - document reading is best-effort
        logger.warning("read_documents: Gemini multimodal read failed for CID %s: %s", cid, exc)
        return {"documents_evaluated": False, "document_findings": _DOCS_NOT_EVALUATED_NOTE}


async def synthesize(state: ResearchState) -> ResearchState:
    """Combine all findings into a human-readable Research_Summary (LLM call).

    Implements Requirement 3.3. The summary must describe the existence findings
    and the email-association findings, and (when evaluated) the document
    consistency findings. This is a core step: a failure here propagates.
    """
    request = state["request"]
    documents_evaluated = state.get("documents_evaluated", False)
    document_findings = state.get("document_findings") or _DOCS_NOT_EVALUATED_NOTE

    prompt = (
        "Write a concise research summary (a few short paragraphs) evaluating an "
        "NGO application for an admin reviewer. Base it ONLY on the findings "
        "below. Cover, in order: (1) whether the organization appears to exist as "
        "a registered NGO, (2) whether the official email is associated with the "
        "organization, and (3) if documents were evaluated, whether they are "
        "consistent with the application. Do not describe documents as authentic, "
        "genuine, forged, or fake. If documents were not evaluated, say so neutrally "
        "and do not treat their absence as a negative signal.\n\n"
        f"Application organization name: {request.org_name}\n"
        f"Application registration number: {request.registration_number}\n"
        f"Application official email: {request.official_email}\n"
        f"Application description: {request.description}\n\n"
        f"=== Existence findings ===\n{state.get('existence_findings', '(none)')}\n\n"
        f"=== Email-association findings ===\n{state.get('email_link_findings', '(none)')}\n\n"
        f"=== Document findings (documents_evaluated={documents_evaluated}) ===\n"
        f"{document_findings}\n"
    )

    llm = _build_llm(request)
    response = await llm.ainvoke(prompt)
    summary = response.content if isinstance(response.content, str) else str(response.content)
    return {"research_summary": summary}


async def structured_verdict(state: ResearchState) -> ResearchState:
    """Emit a validated ``{confidence_score, verdict}`` via Gemini structured output.

    Implements Requirements 3.4 and 3.5. Uses ``llm.with_structured_output`` bound
    to the :class:`StructuredVerdict` Pydantic schema, so an out-of-range score or
    a verdict other than ``PASS``/``FAIL`` raises a validation error rather than
    being silently coerced (Property 4). That error propagates to the caller.
    """
    request = state["request"]
    research_summary = state.get("research_summary", "")

    prompt = (
        "Based strictly on the research summary below, produce a screening "
        "verdict for this NGO application. Provide a confidence_score between "
        "0.00 and 100.00 reflecting how confident you are that the organization "
        "is a legitimate, real, registered NGO whose official email is associated "
        "with it, and a verdict of exactly 'PASS' or 'FAIL'. Do not lower the "
        "score solely because supporting documents were unavailable or not "
        "evaluated.\n\n"
        f"=== Research summary ===\n{research_summary}\n"
    )

    llm = _build_llm(request)
    structured_llm = llm.with_structured_output(StructuredVerdict)
    verdict = await structured_llm.ainvoke(prompt)
    return {"verdict": verdict}


# --------------------------------------------------------------------------- #
# Graph wiring
# --------------------------------------------------------------------------- #
def _should_read_documents(state: ResearchState) -> str:
    """Conditional router after ``research_email_link``.

    Routes to ``read_documents`` when the request carries a non-blank
    ``documentsCid``; otherwise jumps straight to ``synthesize`` for web-only
    research (Requirements 2.5, 4.5).
    """
    cid = (state["request"].documents_cid or "").strip()
    return "read_documents" if cid else "synthesize"


def build_graph():
    """Build and compile the ``Research_Graph`` ``StateGraph``.

    Returns the compiled graph, whose ``ainvoke`` runs the full pipeline over a
    :class:`ResearchState`.

    NOTE: the ``StateGraph`` / ``START`` / ``END`` API should be verified against
    the installed ``langgraph`` version.
    """
    builder = StateGraph(ResearchState)

    builder.add_node("extract_facts", extract_facts)
    builder.add_node("research_existence", research_existence)
    builder.add_node("research_email_link", research_email_link)
    builder.add_node("read_documents", read_documents)
    builder.add_node("synthesize", synthesize)
    builder.add_node("structured_verdict", structured_verdict)

    builder.add_edge(START, "extract_facts")
    builder.add_edge("extract_facts", "research_existence")
    builder.add_edge("research_existence", "research_email_link")
    builder.add_conditional_edges(
        "research_email_link",
        _should_read_documents,
        {"read_documents": "read_documents", "synthesize": "synthesize"},
    )
    builder.add_edge("read_documents", "synthesize")
    builder.add_edge("synthesize", "structured_verdict")
    builder.add_edge("structured_verdict", END)

    return builder.compile()


async def run_research(request: ScreenRequest) -> ResearchResult | None:
    """Build, run, and collect the result of the ``Research_Graph``.

    Args:
        request: The inbound screening request (carries the per-request Gemini key).

    Returns:
        A :class:`ResearchResult` (summary + validated verdict) on success, or
        ``None`` if the graph completed without producing a verdict.

    Raises:
        Exception: Any exception raised inside a core node (notably the
            ``structured_verdict`` validation when the model returns an
            out-of-range/invalid value) propagates to the caller. ``main.py``
            catches it, logs, and sends no callback so the Backend timeout
            fallback advances the application (Requirements 5.4, 7.4).
    """
    graph = build_graph()
    initial_state: ResearchState = {"request": request}
    final_state = await graph.ainvoke(initial_state)

    verdict = final_state.get("verdict")
    if verdict is None:
        logger.warning(
            "Research graph produced no verdict for application %s",
            request.application_id,
        )
        return None

    return ResearchResult(
        summary=final_state.get("research_summary", ""),
        verdict=verdict,
    )
