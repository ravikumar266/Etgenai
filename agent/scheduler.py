"""
agent/scheduler.py
──────────────────
Background APScheduler job that checks Gmail every 10 minutes
and sends a notification email if important items are found.

Usage (from main.py lifespan):
  from agent.scheduler import start_scheduler, stop_scheduler
"""

import os

from apscheduler.schedulers.background import BackgroundScheduler

from agent.config import logger
from agent.tools_email import _filter_important, _read_emails_raw, _send_email_direct

_scheduler: BackgroundScheduler | None = None


def _scheduled_check_updates() -> None:
    """
    Background job: read unread emails, filter important ones,
    and send a notification to EMAIL_USER if anything is found.
    Runs silently — results appear in server logs only.
    """
    try:
        emails_text = _read_emails_raw()

        if emails_text.startswith("No unread") or emails_text.startswith("Error"):
            logger.info(f"[Scheduler] {emails_text}")
            return

        summary = _filter_important(emails_text)

        if "No important updates" in summary:
            logger.info("[Scheduler] No important updates found")
            return

        logger.info(f"[Scheduler] Important updates:\n{summary[:500]}")

        notify_email = os.getenv("EMAIL_USER")
        if notify_email:
            _send_email_direct(
                to=notify_email,
                subject="Important Email Updates",
                body=f"Here are your important updates:\n\n{summary}",
            )

    except Exception as e:
        logger.error(f"[Scheduler] Job failed: {e}")


def start_scheduler() -> None:
    """
    Initialise and start the background scheduler.
    Call once from main.py lifespan on app startup.
    Safe to call multiple times — won't start a second instance.
    """
    global _scheduler
    if _scheduler and _scheduler.running:
        return

    _scheduler = BackgroundScheduler()
    _scheduler.add_job(
        _scheduled_check_updates,
        "interval",
        minutes=720,
        id="email_check",
    )
    _scheduler.start()
    logger.info("Background scheduler started (email check every 10 min)")


def stop_scheduler() -> None:
    """Gracefully stop the scheduler. Call from main.py lifespan shutdown."""
    global _scheduler
    if _scheduler and _scheduler.running:
        _scheduler.shutdown(wait=False)
        logger.info("Background scheduler stopped")


def get_scheduler() -> BackgroundScheduler | None:
    """Return the scheduler instance (used by main.py status endpoint)."""
    return _scheduler