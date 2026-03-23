"""
agent/config.py
───────────────
Single source of truth for:
  - Environment / .env loading
  - LLM with automatic model fallback + retry on 429
  - Google OAuth credentials + service builders
  - Tavily client
  - Cloud Run base URL

Model priority (set GEMINI_MODEL in .env to override):
  1. gemini-2.0-flash       — 1500 req/day free, fast, recommended default
  2. gemini-1.5-flash       — 1500 req/day free, fallback
  3. gemini-2.5-flash       — 20 req/day free (original, now last resort)

Rate limit strategy:
  - Automatic retry with exponential backoff on 429
  - Model fallback: if primary model is quota-exhausted, tries next model
  - Retry delay extracted from the API error response when available
"""

import logging
import os
import re
import time

from dotenv import load_dotenv
from google.auth.transport.requests import Request
from google.oauth2.credentials import Credentials
from google_auth_oauthlib.flow import InstalledAppFlow
from googleapiclient.discovery import build
from langchain_google_genai import ChatGoogleGenerativeAI
from tavily import TavilyClient

load_dotenv()

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
)
logger = logging.getLogger(__name__)


# ── LLM with fallback + retry ─────────────────────────────────────────────────

# Model priority list — highest free quota first
# Override primary with GEMINI_MODEL in .env
_MODEL_PRIORITY = [
    os.getenv("GEMINI_MODEL", "gemini-2.0-flash"),   # default: 1500/day free
                                                                  # 1500/day free, fallback
    "gemini-2.5-flash",                               # 20/day free, last resort
]
# Remove duplicates while preserving order
seen = set()
_MODEL_PRIORITY = [m for m in _MODEL_PRIORITY if not (m in seen or seen.add(m))]

_GOOGLE_API_KEY = os.getenv("GOOGLE_API_KEY", "")

# Track which model is currently active
_active_model_index = 0


def _extract_retry_delay(error_str: str) -> int:
    """
    Parse retry delay seconds from a 429 error message.
    Google API returns: 'retryDelay': '26s' or 'retry in 26.38s'
    Returns delay in seconds, defaults to 30 if not found.
    """
    # Match patterns like '26s', '26.38s', 'retry in 26s'
    match = re.search(r"retry[^\d]*(\d+(?:\.\d+)?)\s*s", error_str, re.IGNORECASE)
    if match:
        return min(int(float(match.group(1))) + 2, 60)  # add 2s buffer, cap at 60s
    return 30


def _build_llm(model: str) -> ChatGoogleGenerativeAI:
    return ChatGoogleGenerativeAI(
        model=model,
        google_api_key=_GOOGLE_API_KEY,
        temperature=0.3,
    )


class _FallbackLLM:
    """
    Wraps ChatGoogleGenerativeAI with:
      - Automatic retry on 429 (waits the delay the API specifies)
      - Model fallback: exhausted model → tries next in priority list
      - Max 2 retries per model before falling back
    Exposes the same .invoke() and .bind_tools() interface as LangChain LLMs.
    """

    def __init__(self):
        self._model_index = 0
        self._llm = _build_llm(_MODEL_PRIORITY[0])
        logger.info(f"[LLM] Active model: {_MODEL_PRIORITY[0]}")

    def _is_quota_error(self, error: Exception) -> bool:
        return "429" in str(error) or "RESOURCE_EXHAUSTED" in str(error)

    def _try_next_model(self):
        """Switch to the next model in the priority list."""
        self._model_index += 1
        if self._model_index >= len(_MODEL_PRIORITY):
            raise RuntimeError(
                "All Gemini models are quota-exhausted. "
                "Wait until midnight (Pacific Time) for quota reset, "
                "or add billing at https://aistudio.google.com/plan_information"
            )
        next_model = _MODEL_PRIORITY[self._model_index]
        logger.warning(f"[LLM] Quota exhausted — switching to {next_model}")
        self._llm = _build_llm(next_model)
        # Re-bind tools if there are any pending
        if self._bound_tools:
            self._llm = self._llm.bind_tools(self._bound_tools)

    def invoke(self, messages, **kwargs):
        self._bound_tools = getattr(self, "_bound_tools", [])
        max_retries = 2

        for attempt in range(max_retries + 1):
            try:
                return self._llm.invoke(messages, **kwargs)
            except Exception as e:
                if not self._is_quota_error(e):
                    raise  # non-quota errors bubble up immediately

                err_str = str(e)
                if attempt < max_retries:
                    delay = _extract_retry_delay(err_str)
                    logger.warning(
                        f"[LLM] 429 on {_MODEL_PRIORITY[self._model_index]} "
                        f"(attempt {attempt + 1}/{max_retries}) — "
                        f"waiting {delay}s then retrying..."
                    )
                    time.sleep(delay)
                else:
                    # Exhausted retries on this model — try next
                    logger.warning(
                        f"[LLM] {_MODEL_PRIORITY[self._model_index]} quota exhausted "
                        f"after {max_retries} retries — trying fallback model"
                    )
                    self._try_next_model()
                    # One more attempt with the new model
                    try:
                        return self._llm.invoke(messages, **kwargs)
                    except Exception as e2:
                        if self._is_quota_error(e2):
                            delay = _extract_retry_delay(str(e2))
                            logger.warning(f"[LLM] Fallback model also rate-limited — waiting {delay}s")
                            time.sleep(delay)
                            return self._llm.invoke(messages, **kwargs)
                        raise

    def bind_tools(self, tools):
        """Returns a bound copy — same interface as LangChain LLMs."""
        self._bound_tools = tools
        self._llm = _build_llm(_MODEL_PRIORITY[self._model_index]).bind_tools(tools)
        return self  # return self so graph.py can use _llm_with_tools = llm.bind_tools(TOOLS)

    def __getattr__(self, name):
        """Proxy any other attribute access to the underlying LLM."""
        return getattr(self._llm, name)


llm = _FallbackLLM()
logger.info(
    f"[LLM] Model priority: {' → '.join(_MODEL_PRIORITY)}\n"
    f"      Set GEMINI_MODEL in .env to change primary model."
)


# ── Google OAuth ──────────────────────────────────────────────────────────────

SCOPES = [
    "https://www.googleapis.com/auth/documents",
    "https://www.googleapis.com/auth/drive",
    "https://www.googleapis.com/auth/gmail.readonly",
]


def get_creds() -> Credentials:
    """Load saved OAuth token or open browser login on first run."""
    creds = None
    if os.path.exists("token.json"):
        creds = Credentials.from_authorized_user_file("token.json", SCOPES)
    if not creds or not creds.valid:
        if creds and creds.expired and creds.refresh_token:
            creds.refresh(Request())
        else:
            flow = InstalledAppFlow.from_client_secrets_file(
                "credentials.json", SCOPES
            )
            creds = flow.run_local_server(port=0)
        with open("token.json", "w") as f:
            f.write(creds.to_json())
        logger.info("token.json saved ✓")
    return creds


def docs_service():
    return build("docs", "v1", credentials=get_creds())


def gmail_service():
    return build("gmail", "v1", credentials=get_creds())


# ── Tavily client ─────────────────────────────────────────────────────────────

tavily_client = TavilyClient(api_key=os.getenv("TAVILY_API_KEY"))

# ── Cloud Run URL ─────────────────────────────────────────────────────────────

CLOUD_RUN_URL = os.getenv("CLOUD_RUN_URL", "")