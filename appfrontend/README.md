# Axiom Android Client

Axiom is a specialized Android application acting as the mobile interface for the **[Etgenai Agentic Backend](https://github.com/ravikumar266/Etgenai)**. It shifts the paradigm from a traditional chat application to a fully integrated **Agentic Workflow Manager**, allowing users to effectively oversee, schedule, and interact with autonomous AI agents.

## Features

- **Agentic Chat Interface**: Communicate directly with the ETGenAI backend. Includes dynamic rendering of agent workflows such as Google Docs generation, automated email drafting, and approval handling.
- **Workflow Approvals**: Review, approve, or reject AI-generated actions (like sending emails or creating plans) directly within the chat timeline.
- **Scheduler System**: Monitor the health of autonomous agents and manually trigger workflow actions like "Run Email Now" or "Run Briefing Now". 
- **History Management**: Browse through previous agent execution threads and seamlessly resume past workflows.
- **Dynamic Theming**: Adapts to system Day/Night modes with fluid, polished Material Design elements, custom floating navigation, and state-aware components.

## Tech Stack

- **Language**: Kotlin
- **Architecture**: MVVM
- **UI Toolkit**: XML layouts with Material Components (`com.google.android.material`)
- **Networking**: Retrofit2 & Gson for RESTful API communication with the backend
- **Navigation**: Jetpack Navigation Component
- **Image Loading**: Glide
- **Asynchrony**: Kotlin Coroutines

## Architecture Overview

The codebase is organized by feature for ease of maintenance:

*   `ui.chat`: Contains the primary conversational interface and specialized `ChatAdapter` for rendering rich workflow previews.
*   `ui.scheduler`: Interfaces for monitoring backend autonomy and manually triggering scheduled jobs.
*   `ui.history`: Views for recalling past interaction threads.
*   `data.api`: Retrofit service configurations mapping to the Etgenai endpoints (e.g., polling, sending messages, fetching history).
*   `data.model`: Kotlin data classes mapping to JSON payloads expected by the ETGenAI backend.

## Prerequisites

- Android Studio Giraffe | 2022.3.1 or newer.
- Android SDK level 24 or higher.

## Setup & Installation

1. Clone this repository:
   ```bash
   git clone https://github.com/Kishan8548/Axiom.git
   ```
2. Open the project in Android Studio.
3. The app is configured to communicate with the production Etgenai backend automatically (configured in `RetrofitClient.kt`). If you are running the backend locally, update the `BASE_URL` accordingly.
4. Sync Gradle and run the application on an emulator or a physical device.

## Backend Repository

The backend powering this application is built with Python and LangGraph. You can view the backend architecture and agent logic here:  
**[ravikumar266/Etgenai](https://github.com/ravikumar266/Etgenai)**
