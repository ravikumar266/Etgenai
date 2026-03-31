# ETGenAI

An autonomous multi-agent AI backend — chat, document Q&A, email, GitHub reviews, and scheduled intelligence through a single API.

**Live Demo:** [https://etgenainew.onrender.com/docs](https://etgenainew.onrender.com/docs)

---

## 🛠 Stack

`FastAPI` · `LangGraph` · `Gemini 2.5 Flash` · `Groq Llama 3.3` · `ChromaDB` · `Gmail API` · `GitHub API` · `SQLite` · `Render`

---

## 🚀 Core Features

* **Chat with Memory** – Sliding-window summarization keeps context deep without hitting token limits.
* **PDF Q&A** – Upload any PDF and ask questions in natural language via ChromaDB RAG.
* **Email Integration** – Gmail API support with a human-in-the-loop approval gate for security.
* **GitHub Agent** – List PRs, review diffs, read files, and search code autonomously.
* **Google Docs** – Create and update documents directly from the chat interface.
* **Intelligence Scheduler** – Daily morning briefing (news + weather + Gmail + GitHub) delivered at 8 AM.
* **Resilient Key Rotation** – Automatically rotates through 3 Gemini keys on 429 errors, with Groq as a final fallback.

---

## 📑 API Reference

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| **POST** | `/chat/with-pdf` | Chat + optional PDF upload (main endpoint) |
| **POST** | `/chat` | JSON-only chat interaction |
| **POST** | `/session/new` | Initialize a new session |
| **GET** | `/history/{session_id}` | Retrieve chat history and auto-summary |
| **POST** | `/email/approve` | Approve or deny a pending email draft |
| **GET** | `/health` | Check system status and active LLM config |

---

## ⚡ Quick Start

### Backend Setup

```bash
# Clone the repository
git clone https://github.com/ravikumar266/etgenainew.git
cd etgenainew

# Install dependencies
pip install -r requirements.txt

# Run the server
uvicorn main:app --reload

# Documentation available at: http://localhost:8000/docs
```

### Frontend (Android App)

The Android client for ETGenAI is developed separately and connects to this backend API.

* **📱 Repo:** [https://github.com/Kishan8548/Axiom](https://github.com/Kishan8548/Axiom)
* Built for mobile interaction with ETGenAI agents.
* Supports chat, document queries, and API-based workflows.

---

## 💻 Example Usage

```javascript
const form = new FormData();
form.append("message", "Summarize this PDF");
form.append("session_id", sessionId);
if (file) form.append("file", file);

const { reply, session_id } = await fetch("/chat/with-pdf", {
  method: "POST",
  body: form
}).then(r => r.json());
```

---

## 🔑 Environment Variables

To run this project, you will need to add the following variables to your `.env` file:

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

## ⚖️ License

This project is licensed under the **MIT License**.
