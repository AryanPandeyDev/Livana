"""Pydantic models mirroring the backend JSON contract.

camelCase JSON aliases match the Java DTOs (ScreeningRequest / ScreeningCallbackRequest).
"""

from typing import Literal, TypedDict

from pydantic import BaseModel, ConfigDict, Field


class ScreenRequest(BaseModel):
    """Inbound POST /screen body. Mirrors the backend ScreeningRequest DTO."""

    model_config = ConfigDict(populate_by_name=True)

    application_id: str = Field(alias="applicationId")
    org_name: str = Field(alias="orgName")
    registration_number: str = Field(alias="registrationNumber")
    description: str
    official_email: str = Field(alias="officialEmail")
    documents_cid: str | None = Field(default=None, alias="documentsCid")
    gemini_api_key: str = Field(alias="geminiApiKey")


class StructuredVerdict(BaseModel):
    """Gemini structured-output target. Out-of-range/invalid values raise ValidationError."""

    confidence_score: float = Field(ge=0.0, le=100.0)
    verdict: Literal["PASS", "FAIL"]


class CallbackPayload(BaseModel):
    """Outbound callback body. Mirrors the backend ScreeningCallbackRequest DTO."""

    model_config = ConfigDict(populate_by_name=True)

    application_id: str = Field(alias="applicationId")
    confidence_score: float = Field(ge=0.0, le=100.0, alias="confidenceScore")
    research_summary: str = Field(alias="researchSummary")
    verdict: Literal["PASS", "FAIL"]


class ResearchState(TypedDict, total=False):
    """Shared LangGraph state accumulated across research nodes."""

    request: ScreenRequest
    email_domain: str
    existence_findings: str
    email_link_findings: str
    documents_evaluated: bool
    document_findings: str
    research_summary: str
    verdict: StructuredVerdict


def round_score(score: float) -> float:
    """Round a confidence score to two decimal places (matches DECIMAL(5,2))."""
    return round(score, 2)
