# Smart Hospital

Full-stack MVP of the **Smart Hospital Patient Queue and Appointment Management System**.

The project combines a Spring Boot API, a Python machine-learning sidecar, and a responsive React frontend to provide:

- Patient self-registration and JWT authentication
- Role-based patient, doctor, and administrator workspaces
- Doctor and department administration
- Fixed appointment slots and conflict-safe booking
- Digital queue tokens
- Tiered emergency prioritization with FIFO within each tier
- Doctor queue controls and auditable manual overrides
- Waiting-time prediction through a Python ML service, with a deterministic Java fallback
- OpenAPI documentation and Swagger UI
- Automated backend, Python, and frontend tests

## Repository layout

```text
backend/                 Spring Boot REST API
frontend/                React, TypeScript, and Vite application
ml-service/              FastAPI waiting-time prediction service
docs/                    Architecture and implementation documentation
```

## Technology

- Java 21, Spring Boot, Maven Wrapper
- MySQL 8
- Python 3.11+, FastAPI, scikit-learn
- React 19, TypeScript 7, Vite 8, Tailwind CSS 4
- React Router, TanStack Query, and Lucide React
- JWT access and refresh tokens
- Flyway database migrations
- OpenAPI / Swagger UI

## Frontend development

The frontend uses the real Spring Boot API by default. The API base URL is `/api/v1`; override it with `VITE_API_BASE_URL` when the API runs on another origin.

```bash
cd frontend
npm ci
npm run dev
```

A deterministic, fictional-data preview is available only when explicitly enabled:

```bash
cd frontend
VITE_API_MODE=demo npm run dev
```

Demo mode is labeled in the application and never sends requests to the backend.

## Local verification

Run frontend tests, strict TypeScript checking, a production build, and the initial-JavaScript gzip budget from `frontend/`:

```bash
cd frontend
npm test -- --run
npm run typecheck
npm run build
npm run check:bundle-size
```

The frontend architecture and implementation steps are documented in `docs/superpowers/specs/2026-11-15-smart-hospital-frontend-design.md` and `docs/superpowers/plans/2026-11-15-smart-hospital-frontend.md`.
