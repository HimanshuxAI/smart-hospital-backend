# Smart Hospital Backend Design

**Date:** 2026-11-15  
**Status:** Approved for implementation by project owner  
**Repository:** `HimanshuxAI/smart-hospital-backend` (private)

## 1. Purpose and scope

The Smart Hospital backend is a complete academic MVP for hospital appointment and queue management. It allows patients to register, book fixed doctor slots, receive a digital token, track queue position and estimated waiting time, while doctors and administrators manage schedules, queues, emergency cases, and users.

The release includes:

- A Java 21 Spring Boot REST API backed by MySQL.
- A Python FastAPI machine-learning sidecar for waiting-time prediction.
- A responsive React single-page application for patient, doctor, and administrator workflows.
- JWT authentication with patient, doctor, and administrator roles.
- Patient, doctor, appointment, queue, emergency-priority, and dashboard workflows.
- Deterministic Java fallback when the ML sidecar is unavailable.
- OpenAPI documentation with Swagger UI.
- Automated backend, frontend, and Python tests plus deterministic seed data.

Explicitly outside this release: patient medical-record management, payments, SMS/push notifications, online consultation, and multi-hospital tenancy.

## 2. Architecture

### 2.1 Architectural style

Use a modular monolith for the Java application and a small Python prediction service.

The Java service owns all authoritative hospital, identity, appointment, and queue state. The Python service is deliberately stateless: it accepts prediction features and returns an estimate plus model metadata. It cannot mutate hospital data.

This approach provides a simple local setup and clear transaction boundaries while satisfying the project's explicit Java-and-Python requirement. A fully distributed microservice design is unnecessary for the expected scale and would make the academic MVP harder to run and test.

### 2.2 Components

1. **Identity module**
   - Patient self-registration, login, token refresh, logout, and profile access.
   - Admin-managed doctor accounts.
   - Seeded administrator account.
   - Role-based authorization at endpoint and service boundaries.

2. **Hospital directory module**
   - Departments and doctor profiles.
   - Admin CRUD operations; safe read-only access for authenticated patients.

3. **Scheduling module**
   - Doctors own availability templates and concrete schedule slots.
   - Admin creates fixed slots for doctors.
   - Patients browse open slots and book one slot.
   - A unique database constraint prevents double-booking.

4. **Appointment module**
   - Appointment lifecycle from booking through completion or cancellation.
   - State transitions are validated rather than freely editable.
   - Appointment history is available to the patient and assigned doctor.

5. **Queue module**
   - One queue entry per checked-in appointment.
   - Sequential daily token numbers are allocated per doctor.
   - Queue ordering uses emergency tier first and arrival time second.
   - Doctors call, serve, complete, skip, or cancel entries.
   - Emergency classification is restricted to doctor/admin roles.
   - Manual reordering requires a reason and creates an audit record.

6. **Waiting-time module**
   - Java builds a prediction request from current queue state.
   - FastAPI calls a scikit-learn model trained from seeded historical records.
   - Java falls back to a deterministic queue-based estimate if the sidecar is unavailable or invalid.
   - Every response identifies the prediction source and confidence.

7. **Dashboard module**
   - Admin dashboard: users, departments, doctors, appointments, and queue totals.
   - Doctor dashboard: today's appointments and active queue.
   - Patient dashboard: upcoming appointments and current queue state.

8. **Frontend application module**
   - A route-split React application presents patient, doctor, and administrator workspaces.
   - The browser never owns clinical state; TanStack Query caches typed API responses and mutations invalidate affected views.
   - Access tokens remain in memory, refresh tokens remain in `sessionStorage`, and a failed session returns to public authentication routes.
   - A deterministic demo adapter can preview incomplete screens without changing the production HTTP behavior.

### 2.3 Runtime topology

For local-first development:

- MySQL runs natively.
- The Spring Boot API runs from the Maven wrapper.
- The Python service runs in a Python virtual environment.
- The frontend runs through Vite and targets the `/api/v1` Spring Boot origin.
- Backend integration tests use isolated MySQL instances where required.

The repository intentionally omits deployment automation and hosted workflow definitions. Local setup and release verification are documented as direct host commands so the project stays lightweight and portable.

## 3. Technology decisions

| Concern | Decision | Reason |
|---|---|---|
| Java runtime | Java 21 | Current LTS target and broad compatibility |
| Web framework | Spring Boot 3.x | Matches the synopsis, supports standard enterprise patterns |
| Build | Maven Wrapper | Avoids requiring a globally installed Maven |
| Persistence | Spring Data JPA + Hibernate | Clear domain mapping and transaction support |
| Database | MySQL 8 | Explicit project requirement |
| Migrations | Flyway | Versioned, reproducible schema changes |
| Authentication | Spring Security + JWT | Suitable for independent web/mobile clients |
| API docs | springdoc-openapi + Swagger UI | Interactive, generated contract documentation |
| Validation | Jakarta Bean Validation | Standard request validation |
| Error format | RFC 9457 problem details | Consistent machine-readable API errors |
| ML API | FastAPI | Lightweight, typed Python service |
| ML model | scikit-learn | Transparent baseline suitable for an academic project |
| Frontend | React 19 + TypeScript 7 + Vite 8 | Fast, typed, route-split browser application |
| Frontend state | TanStack Query + React Router | Server-state caching and route-aware navigation without a global store |
| Frontend styling | Tailwind CSS 4 + CSS tokens | Small responsive design system with no external font or chart dependency |
| Tests | JUnit 5, Mockito, Spring Boot tests, pytest, Vitest, Testing Library | Covers backend, Python, and critical frontend behavior |

Dependencies are pinned to compatible versions in Maven and Python lock/requirements files.

## 4. Domain model

The principal entities are:

- `User`: identity, password hash, role, contact details, activation state.
- `PatientProfile`: patient-specific details linked to a patient user.
- `Department`: hospital department.
- `DoctorProfile`: specialty and department linked to a doctor user.
- `AvailabilityTemplate`: recurring weekly availability owned by a doctor.
- `ScheduleSlot`: concrete doctor appointment slot with capacity one.
- `Appointment`: patient, doctor, slot, reason, and controlled status.
- `QueueEntry`: checked-in appointment, token, emergency tier, and queue status.
- `QueueAudit`: manual priority override or other privileged queue operation.
- `WaitingTimeSample`: historical features and observed waiting duration used for ML.
- `RefreshToken`: hashed rotating refresh token metadata.

Important constraints:

- User email addresses are unique.
- A schedule slot can have at most one active appointment.
- A patient cannot hold overlapping active appointments.
- A doctor cannot have overlapping active schedule slots.
- An appointment can have only one non-cancelled queue entry.
- Queue token number is unique for a doctor and queue date.
- Only one active appointment may be served by a doctor at a time.
- Emergency tiers use `NORMAL`, `MEDIUM`, `HIGH`, and `CRITICAL`.

## 5. Roles and permissions

### Patient

- Register, log in, refresh tokens, log out, read/update own profile.
- Browse departments, doctors, and open slots.
- Book and cancel own appointments.
- Check in to an eligible appointment.
- View own appointment, token, queue position, triage level, and waiting estimate.
- View personal dashboard.

### Doctor

- Do everything applicable to the authenticated doctor profile.
- View own schedule, appointments, and queue.
- Check patients in at the point of care.
- Classify emergency priority and manage queue entry state.
- Call the next patient, start consultation, complete, skip, or cancel an entry.
- Override manual queue order with a mandatory reason.
- View own dashboard.

### Administrator

- Manage departments and doctor accounts.
- Create and manage schedules for all doctors.
- View and manage all appointments and queues.
- Activate or deactivate users.
- View audit records and aggregate dashboard data.

Authorization is enforced server-side. Hiding an endpoint in client code is not considered access control.

## 6. API design

All endpoints are rooted at `/api/v1` and return JSON unless noted.

### Authentication

- `POST /auth/register`
- `POST /auth/login`
- `POST /auth/refresh`
- `POST /auth/logout`
- `GET /auth/me`

### Directory

- `GET /departments`
- `GET /doctors`
- `GET /doctors/{doctorId}`
- `GET /doctors/{doctorId}/slots`
- Admin endpoints for departments, doctors, and slots

### Appointments

- `GET /appointments`
- `GET /appointments/{id}`
- `POST /appointments`
- `POST /appointments/{id}/cancel`
- `POST /appointments/{id}/check-in`

### Queue and prediction

- `GET /queue`
- `GET /queue/{entryId}`
- `GET /queue/{entryId}/waiting-time`
- `POST /queue/{entryId}/classify`
- `POST /queue/{entryId}/call`
- `POST /queue/{entryId}/serve`
- `POST /queue/{entryId}/complete`
- `POST /queue/{entryId}/skip`
- `POST /queue/{entryId}/cancel`
- `POST /queue/{entryId}/override`

### Dashboards

- `GET /dashboard/patient`
- `GET /dashboard/doctor`
- `GET /dashboard/admin`

### ML sidecar

- `GET /health`
- `POST /predict`

## 7. Core workflows

### Appointment booking

1. Patient authenticates.
2. Client requests available slots for a doctor and date.
3. Patient submits doctor, slot, and visit reason.
4. The service reloads the slot inside a transaction and verifies it is open.
5. The service checks that the patient has no overlapping active appointment.
6. The service creates the appointment and marks the slot booked.
7. A unique database constraint is the final protection against concurrent double-booking.

### Check-in and token generation

1. Patient checks in on an eligible, confirmed appointment.
2. The service verifies the appointment belongs to the patient, matches the current queue date, is within the configurable check-in window, and is not cancelled or completed.
3. A queue entry is created and a per-doctor/per-day token is allocated transactionally.
4. The patient receives the token and initial queue position.

### Emergency priority and next-patient selection

1. A doctor or admin assigns `MEDIUM`, `HIGH`, or `CRITICAL`; ordinary entries use `NORMAL`.
2. Ordering is `CRITICAL`, `HIGH`, `MEDIUM`, then `NORMAL`.
3. Within a tier, the oldest `arrivedAt` comes first.
4. Calling the next patient locks the relevant queue state so concurrent doctors cannot call two active patients unexpectedly.
5. A manual override records actor, previous value, new value, reason, and timestamp.

### Waiting-time prediction

1. Java collects queue position, people waiting, current time, historical doctor average, emergency load, triage tier, and slot context.
2. FastAPI validates the payload and invokes the trained model.
3. FastAPI returns estimated minutes and confidence metadata.
4. If the call times out or fails, Java uses `remainingPeople × averageConsultationMinutes` adjusted for current emergency load.
5. The API response states whether the estimate came from `ML` or `FALLBACK`.

## 8. Error handling and security

- Standard HTTP status codes and RFC 9457 problem-detail responses.
- Validation failures include field-level errors.
- Optimistic lock versions protect queue and appointment state from lost updates.
- Passwords use BCrypt hashing.
- Access JWTs are short-lived; refresh tokens are opaque, hashed at rest, rotated, and revocable.
- Secrets come from environment variables. Development defaults are supplied only in `.env.example` or local profile configuration.
- CORS origins are explicit and configurable.
- Request bodies and URL lengths are bounded.
- Logs avoid passwords, tokens, and sensitive clinical details.
- Rate limiting is applied to authentication endpoints in the local/production profile.
- Refresh-token reuse revokes the associated token family.

## 9. Database and consistency

Flyway owns the schema. The application uses explicit foreign keys and indexes for common lookup paths, including appointment patient/doctor/date, queue doctor/date/status, and unique token constraints.

Critical booking, check-in, token allocation, and queue transition operations run in database transactions. Pessimistic locking is used where concurrent access must be serialized; optimistic versions are retained as an additional guard.

Database migrations are forward-only and tested by starting the application against a clean MySQL database.

## 10. ML service design

The Python service trains a baseline scikit-learn regression model on a deterministic, seeded historical dataset generated for demonstration purposes. The data is explicitly synthetic and must not be represented as real patient data.

Features include:

- Current queue position
- Number of waiting patients
- Doctor historical average consultation minutes
- Active emergency count
- Appointment priority/triage
- Hour of day and day of week
- Whether the patient is arriving close to the scheduled slot

The trained artifact is versioned. The service exposes health and prediction endpoints and reports model version, feature schema version, and training-data provenance. The Java fallback keeps the overall application usable if Python is not running.

## 11. Testing strategy

### Java

- Unit tests for domain rules, ordering, authorization decisions, fallback estimation, and state transitions.
- MVC/API tests for validation, status codes, problem details, and role restrictions.
- Integration tests against MySQL for booking concurrency, check-in/token uniqueness, and queue transitions.
- Full application-context test for migrations and health endpoints.

### Python

- Unit tests for preprocessing and model prediction.
- API tests for validation and response schema.
- A smoke test proving a serialized model can be trained, loaded, and used.

### End-to-end verification

A deterministic demo sequence covers registration, login, booking, check-in, queue token retrieval, emergency classification, doctor queue progression, and waiting-time response.

## 12. Repository workflow

- The GitHub repository is private under `HimanshuxAI/smart-hospital-backend`.
- Work is committed in small, logically grouped commits with conventional commit-style messages.
- Each completed work unit is pushed to its feature branch; no history is rewritten or force-pushed.
- Verification runs directly through Maven, pytest, npm, TypeScript, and Vitest commands documented in the plans.
- `.env.example` documents required variables without storing secrets.
- Deployment automation and hosted workflow definitions are intentionally excluded from this release.

## 13. Implementation sequence

1. Initialize Git and private GitHub repository; add baseline documentation.
2. Scaffold the Spring Boot API and Python service with health checks and local configuration.
3. Add Flyway schema, JPA entities, and seed data.
4. Implement authentication, refresh-token rotation, and role authorization.
5. Implement departments, doctors, schedules, and slot browsing.
6. Implement appointment booking, cancellation, check-in, and digital tokens.
7. Implement queue ordering, emergency priority, doctor transitions, audits, and dashboards.
8. Implement the Python model service and Java fallback/ML integration.
9. Build the responsive frontend foundation, design system, typed API boundary, and authentication lifecycle.
10. Build patient, doctor, and administrator workspaces with route guards and complete state handling.
11. Add frontend regression tests, accessibility checks, bundle-budget enforcement, and responsive verification.
12. Run the complete local verification suite and document demo credentials and workflows.

## 14. Acceptance criteria

The release is complete when:

- A clean checkout can be configured, built, migrated, seeded, and started locally.
- The documented demo credentials can log in through the API and the frontend.
- Patients can browse departments and doctors, book and cancel appointments, check in, and track a digital queue token through the responsive frontend.
- Doctors can review today's schedule, manage the live queue, triage entries, and update their profile through the frontend.
- Administrators can review aggregate metrics, manage users and doctors, and inspect departments, schedules, appointments, and queues through the frontend.
- Check-in produces a unique digital token and queue position.
- Emergency entries outrank normal entries in the documented tier order and in the frontend queue board.
- The frontend correctly parses RFC 9457 problem details, retries one serialized refresh after a 401, and never stores an access token outside memory.
- Route guards prevent cross-role access and provide role-aware redirects and accessible 403/404 pages.
- The frontend meets WCAG AA contrast, semantic landmark, visible-focus, keyboard-operation, form-error association, and reduced-motion requirements.
- Initial route JavaScript remains below 200 KB gzip; the production build fails the repeatable bundle-budget check if it regresses.
- Swagger UI exposes the API contract.
- Automated Java, Python, and frontend tests pass; TypeScript typechecking and the frontend production build succeed.
- `git diff --check` is clean, no secrets or generated bulky artifacts are tracked, and the repository contains incremental, readable commits.
