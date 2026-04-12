# Frontend

React + Vite client for the local chat UI.

## Run

```bash
npm install
npm run dev
```

Default local URL:

- `http://localhost:5173`

## What It Does

- sends normal and streaming chat requests to the backend
- keeps the active `sessionId` across turns
- shows a session sidebar with search and filters
- supports session import, export, delete, and resume
- renders structured MCP report cards for supported tool results
- previews local report artifacts in-app through the backend artifact endpoints
- shows provider metadata and technical details behind the UI debug toggle

## Backend Integration

The frontend expects the Spring Boot backend to be running at:

- `http://localhost:8080`

It uses:

- `/api/chat`
- `/api/chat/stream`
- `/api/sessions`
- `/api/artifacts/*`
- `/api/tools/*`

## Notes

- the UI is session-oriented; reopened sessions restore saved messages and metadata
- technical details are hidden by default and can be enabled from the UI
- streamed replies can enrich the final assistant message with provider metadata when the backend emits completion metadata
