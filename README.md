# ETGenAI

An autonomous multi-agent AI backend — chat, document Q&A, email, GitHub reviews, and scheduled intelligence through a single API.

**Live →** https://etgenainew.onrender.com/docs

---

## Stack

`FastAPI` · `LangGraph` · `Gemini 2.5 Flash` · `Groq Llama 3.3` · `ChromaDB` · `Gmail API` · `GitHub API` · `SQLite` · `Render`

---

## Core features

- **Chat with memory** — sliding-window summarization keeps context without hitting token limits
- **PDF Q&A** — upload any PDF, ask questions in natural language via ChromaDB RAG
- **Email** — Gmail API send with human-in-the-loop approval gate
- **GitHub** — list PRs, review diffs, read files, search code
- **Google Docs** — create and update docs from chat
- **Scheduler** — daily morning briefing (news + weather + Gmail + GitHub) at 8 AM
- **Key rotation** — 3 Gemini keys rotate on 429, Groq as final fallback

---

## API

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/chat/with-pdf` | Chat + optional PDF upload (main endpoint) |
| `POST` | `/chat` | JSON chat only |
| `POST` | `/session/new` | Create session |
| `GET` | `/history/{session_id}` | Chat history + summary |
| `POST` | `/email/approve` | Approve / deny pending email |
| `GET` | `/health` | Status + LLM config |

---

## Quick start

```bash
git clone https://github.com/ravikumar266/etgenainew.git
cd etgenainew
pip install -r requirements.txt
uvicorn main:app --reload
# → http://localhost:8000/docs
```

---

## Frontend

```js
const form = new FormData();
form.append("message", "Summarize this PDF");
form.append("session_id", sessionId);
if (file) form.append("file", file); // optional

const { reply, session_id } = await fetch("/chat/with-pdf", {
  method: "POST", body: form
}).then(r => r.json());
```

---

## Environment

```env
GEMINI_API_KEY_1=
GEMINI_API_KEY_2=
GEMINI_API_KEY_3=
GROQ_API_KEY=
GOOGLE_API_KEY=
GOOGLE_TOKEN_JSON=
GOOGLE_CREDENTIALS_JSON=
RESEND_API_KEY=
TAVILY_API_KEY=
WEATHER_API_KEY=
EMAIL_USER=
```

---

## License
