# llm-pet-project

Proof-of-concept app to chat with local LLMs via Ollama.

Architecture:

React Frontend -> Spring Boot Backend -> Ollama REST API -> Local LLM

## Project Structure

```text
llm-pet-project/
├── frontend/
├── backend/
├── docker-compose.yml
└── README.md
```

## Prerequisites

- macOS with Ollama installed and running
- Java 21+
- Maven 3.9+
- Node 20+
- Docker + Docker Compose (optional)

## 1. Pull and Run a Model in Ollama

```bash
ollama pull codellama:70b
ollama run codellama:70b
```

Ollama API must be available at `http://localhost:11434`.

## 2. Run Backend (Spring Boot)

```bash
cd backend
mvn spring-boot:run
```

Backend runs on `http://localhost:8080`.

### Backend API

`POST /api/chat`

Request:

```json
{
  "message": "Explain recursion",
  "model": "codellama:70b"
}
```

Response:

```json
{
  "response": "...",
  "model": "codellama:70b"
}
```

Streaming endpoint: `POST /api/chat/stream` (SSE).

## 3. Run Frontend (React)

```bash
cd frontend
npm install
npm run dev
```

Frontend runs on `http://localhost:5173`.

## 4. Run with Docker Compose

Keep Ollama running on the host machine first. Then:

```bash
docker compose up --build
```

- Frontend: `http://localhost:3000`
- Backend: `http://localhost:8080`

Compose uses `host.docker.internal:11434` so backend container can reach host Ollama.

## Environment Variables (Backend)

- `OLLAMA_BASE_URL` (default: `http://localhost:11434`)
- `OLLAMA_DEFAULT_MODEL` (default: `codellama:70b`)
- `OLLAMA_CONNECT_TIMEOUT_SECONDS` (default: `10`)
- `OLLAMA_READ_TIMEOUT_SECONDS` (default: `240`)

## Notes for 70B Models

- 70B 4-bit models can be memory intensive.
- Keep other heavy apps closed while testing.
- Increase backend read timeout if generation is slow.

## Next Enhancements

- Persist conversation history on backend
- Add authentication
- Add prompt templates/system prompts
- Add metrics and tracing
- Support multiple model providers
