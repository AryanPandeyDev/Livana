"""Environment-driven configuration for the AI screening service.

Settings are loaded once from the process environment (and an optional local
``.env`` file) via pydantic-settings.

Key handling note
-----------------
The **Gemini API key is intentionally NOT part of this configuration**. It is
operational config that an admin rotates without a redeploy, so the Backend
keeps it encrypted and forwards it per request in the ``/screen`` body. Each
screening run uses that per-request key as a local variable and then discards
it.

The **Tavily API key IS infrastructure config** and lives here: it is a static
platform credential, never admin-managed and never sent over the wire.
"""

from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """Infrastructure configuration read from the environment.

    All values are environment-backed. ``backend_callback_url`` and
    ``shared_secret`` have no defaults because the service cannot operate
    securely without them; the remaining values carry sensible defaults.
    """

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        case_sensitive=False,
        extra="ignore",
    )

    # Where to POST screening results (the Backend internal callback endpoint).
    backend_callback_url: str = ""

    # Mutual X-Internal-Secret value used in both directions.
    shared_secret: str = ""

    # IPFS gateway base used to fetch application documents.
    ipfs_gateway_url: str = "https://gateway.pinata.cloud/ipfs/"

    # Cap on the number of files fetched from a directory CID.
    max_document_files: int = 5

    # Cap on bytes fetched per document file (10 MB).
    max_document_bytes: int = 10_485_760

    # Gemini model id used for synthesis and structured output.
    gemini_model: str = "gemini-2.0-flash"

    # Platform infrastructure Tavily key (NOT admin-swappable, never sent over the wire).
    tavily_api_key: str = ""


@lru_cache
def get_settings() -> Settings:
    """Return a cached :class:`Settings` instance.

    Using an ``lru_cache`` makes the settings a process-wide singleton while
    keeping construction lazy so tests can override the environment before the
    first access.
    """

    return Settings()


# Module-level convenience handle for application code.
settings = get_settings()
