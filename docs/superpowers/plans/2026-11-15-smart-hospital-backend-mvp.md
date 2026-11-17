# Smart Hospital Backend MVP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver a private, production-quality academic MVP that manages hospital accounts, doctors, fixed appointments, digital queue tokens, emergency prioritization, queue progression, dashboards, and ML-backed waiting-time estimates.

**Architecture:** A Java 21 Spring Boot modular monolith owns identity, scheduling, appointment, and queue state in MySQL. A stateless Python FastAPI sidecar owns waiting-time inference; the Java API calls it through a typed HTTP client and returns a deterministic fallback when the sidecar is unavailable.

**Tech Stack:** Java 21, Spring Boot 3.5, Spring Security, Spring Data JPA, Flyway, MySQL 8, Maven Wrapper, JWT, Python 3.12, FastAPI, scikit-learn, pytest, Docker Compose, GitHub Actions.

---

## File map

### Backend

```text
backend/pom.xml
backend/mvnw
backend/mvnw.cmd
backend/.mvn/wrapper/maven-wrapper.properties
backend/src/main/java/com/smarthospital/SmartHospitalApplication.java
backend/src/main/java/com/smarthospital/config/{SecurityConfig,JwtConfig,OpenApiConfig,ClockConfig}.java
backend/src/main/java/com/smarthospital/common/{ApiException,GlobalExceptionHandler,PageResponse}.java
backend/src/main/java/com/smarthospital/auth/{AuthController,AuthService,JwtService,RefreshTokenService,TokenProperties}.java
backend/src/main/java/com/smarthospital/user/{User,UserRole,UserRepository,UserService,UserController,AdminUserController}.java
backend/src/main/java/com/smarthospital/patient/{PatientProfile,PatientProfileRepository}.java
backend/src/main/java/com/smarthospital/department/{Department,DepartmentRepository,DepartmentService,DepartmentController,AdminDepartmentController}.java
backend/src/main/java/com/smarthospital/doctor/{DoctorProfile,DoctorProfileRepository,DoctorService,DoctorController,AdminDoctorController}.java
backend/src/main/java/com/smarthospital/schedule/{ScheduleSlot,ScheduleSlotRepository,ScheduleService,ScheduleController,AdminScheduleController}.java
backend/src/main/java/com/smarthospital/appointment/{Appointment,AppointmentStatus,AppointmentRepository,AppointmentService,AppointmentController}.java
backend/src/main/java/com/smarthospital/queue/{QueueEntry,QueueStatus,EmergencyTier,QueueEntryRepository,QueueAudit,QueueAuditRepository,QueueService,QueueController}.java
backend/src/main/java/com/smarthospital/waiting/{WaitingTimeService,WaitingTimeClient,WaitingTimeFallback,WaitingTimeController,PredictionSource}.java
backend/src/main/java/com/smarthospital/dashboard/{DashboardService,DashboardController}.java
backend/src/main/resources/application.yml
backend/src/main/resources/db/migration/V1__create_core_schema.sql
backend/src/test/java/com/smarthospital/...
```

### ML service

```text
ml-service/pyproject.toml
ml-service/app/__init__.py
ml-service/app/main.py
ml-service/app/config.py
ml-service/app/schemas.py
ml-service/app/model.py
ml-service/app/training.py
ml-service/tests/test_api.py
ml-service/tests/test_model.py
```

### Operations and documentation

```text
.env.example
docker-compose.yml
backend/Dockerfile
ml-service/Dockerfile
.github/workflows/ci.yml
docs/api-demo.md
docs/architecture.md
```

## Task 1: Scaffold both runnable services

**Files:**
- Create: `backend/pom.xml`
- Create: `backend/src/main/java/com/smarthospital/SmartHospitalApplication.java`
- Create: `backend/src/main/resources/application.yml`
- Create: `backend/src/test/java/com/smarthospital/SmartHospitalApplicationTests.java`
- Create: `ml-service/pyproject.toml`
- Create: `ml-service/app/__init__.py`
- Create: `ml-service/app/main.py`
- Create: `ml-service/tests/test_api.py`

- [ ] **Step 1: Generate the Maven Wrapper and Spring skeleton**

Use Spring Boot `3.5.16`, Java `21`, group `com.smarthospital`, artifact `backend`. Add starters for web, validation, data JPA, security, actuator, Flyway, and MySQL; add springdoc `2.8.17`, Testcontainers `1.21.4`, and a Java JWT-compatible security implementation.

Run:

```bash
cd backend
curl -fsSL "https://start.spring.io/starter.zip?type=maven-project&language=java&bootVersion=3.5.16&baseDir=backend&groupId=com.smarthospital&artifactId=backend&name=backend&description=Smart%20Hospital%20Backend&packageName=com.smarthospital&packaging=jar&javaVersion=21&dependencies=web,validation,data-jpa,security,actuator,flyway,mysql" -o /tmp/smart-hospital.zip
unzip -q /tmp/smart-hospital.zip -d /tmp/smart-hospital-starter
cp -R /tmp/smart-hospital-starter/backend/. .
```

Expected: `backend/mvnw`, `backend/pom.xml`, and `backend/src/main/java/com/smarthospital/SmartHospitalApplication.java` exist.

- [ ] **Step 2: Write the backend context test**

```java
package com.smarthospital;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:smarthospital;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false"
})
class SmartHospitalApplicationTests {
    @Test
    void contextLoads() {
    }
}
```

Add H2 as a test dependency. Run `./mvnw test`; expected: one passing test.

- [ ] **Step 3: Write and run the Python health API test first**

```python
from fastapi.testclient import TestClient
from app.main import app

client = TestClient(app)


def test_health() -> None:
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json() == {"status": "UP", "service": "smart-hospital-ml"}
```

- [ ] **Step 4: Implement the Python health endpoint**

```python
from fastapi import FastAPI

app = FastAPI(title="Smart Hospital ML Service", version="1.0.0")


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "UP", "service": "smart-hospital-ml"}
```

Run `python3.12 -m venv ml-service/.venv`, install the project, and run `pytest`; expected: one passing test.

- [ ] **Step 5: Commit and push the scaffold**

```bash
git add backend ml-service
git commit -m "chore: scaffold backend and ml services"
git push -u origin feature/backend-mvp
```

## Task 2: Establish schema, entities, and seed data

**Files:**
- Create: `backend/src/main/resources/db/migration/V1__create_core_schema.sql`
- Create: `backend/src/main/java/com/smarthospital/user/{User,UserRole,UserRepository}.java`
- Create: `backend/src/main/java/com/smarthospital/patient/{PatientProfile,PatientProfileRepository}.java`
- Create: `backend/src/main/java/com/smarthospital/department/{Department,DepartmentRepository}.java`
- Create: `backend/src/main/java/com/smarthospital/doctor/{DoctorProfile,DoctorProfileRepository}.java`
- Create: `backend/src/main/java/com/smarthospital/schedule/{ScheduleSlot,ScheduleSlotRepository}.java`
- Create: `backend/src/main/java/com/smarthospital/appointment/{Appointment,AppointmentStatus,AppointmentRepository}.java`
- Create: `backend/src/main/java/com/smarthospital/queue/{QueueEntry,QueueStatus,EmergencyTier,QueueEntryRepository,QueueAudit,QueueAuditRepository}.java`
- Create: `backend/src/main/java/com/smarthospital/auth/RefreshToken.java`
- Create: `backend/src/main/java/com/smarthospital/auth/RefreshTokenRepository.java`
- Create: `backend/src/main/java/com/smarthospital/config/DataSeeder.java`

- [ ] **Step 1: Write a Flyway migration test**

Create `MigrationSmokeTest` using Testcontainers MySQL `8.4`, start the Spring context, query `flyway_schema_history`, and assert version `1` is successful.

Run `./mvnw -Dtest=MigrationSmokeTest test`; expected: failure because no migration exists.

- [ ] **Step 2: Write `V1__create_core_schema.sql`**

Create normalized tables for `users`, `patient_profiles`, `departments`, `doctor_profiles`, `availability_templates`, `schedule_slots`, `appointments`, `queue_entries`, `queue_audits`, `refresh_tokens`, and `waiting_time_samples` with these invariants:

```sql
ALTER TABLE users ADD CONSTRAINT uk_users_email UNIQUE (email);
ALTER TABLE schedule_slots ADD CONSTRAINT uk_slot_doctor_start UNIQUE (doctor_id, starts_at);
ALTER TABLE appointments ADD CONSTRAINT uk_appointment_active_slot UNIQUE (slot_id, active_marker);
ALTER TABLE queue_entries ADD CONSTRAINT uk_queue_token UNIQUE (doctor_id, queue_date, token_number);
ALTER TABLE queue_entries ADD CONSTRAINT uk_queue_appointment UNIQUE (appointment_id);
```

Use `active_marker` as nullable `1` for active appointments and `NULL` for cancelled/completed appointments so MySQL permits reuse where appropriate. Add indexes for user email, doctor/date slots, patient/date appointments, doctor/date/status queue entries, and refresh-token lookup.

- [ ] **Step 3: Map JPA entities to the migration**

Use `GenerationType.IDENTITY` for primary keys, explicit table/column names, enum `@Enumerated(EnumType.STRING)`, `Instant` timestamps, entity-level `@Version` on appointments and queue entries, and lazy relationships. Keep domain behavior out of JPA entities; repositories expose intent-specific query methods.

- [ ] **Step 4: Add deterministic development seed data**

`DataSeeder` runs only when `app.seed.enabled=true`. It creates:

- `admin@smarthospital.local` / `Admin@123`
- `doctor@smarthospital.local` / `Doctor@123`
- `patient@smarthospital.local` / `Patient@123`
- General Medicine and Cardiology departments
- One doctor profile and slots for today and tomorrow

Use `PasswordEncoder` for every seeded password and make seeding idempotent by email.

- [ ] **Step 5: Run migration and entity tests**

Run `./mvnw -Dtest=MigrationSmokeTest,DataSeederTest test`; expected: all tests pass and Flyway reports version `1`.

- [ ] **Step 6: Commit and push the persistence layer**

```bash
git add backend/src/main backend/src/test
git commit -m "feat: add hospital database schema and seed data"
git push
```

## Task 3: Implement authentication, refresh rotation, and authorization

**Files:**
- Create: `backend/src/main/java/com/smarthospital/auth/{AuthController,AuthService,JwtService,RefreshTokenService,TokenProperties}.java`
- Create: `backend/src/main/java/com/smarthospital/config/{SecurityConfig,JwtConfig}.java`
- Create: `backend/src/main/java/com/smarthospital/common/{ApiException,GlobalExceptionHandler}.java`
- Create: `backend/src/main/java/com/smarthospital/user/{UserService,UserController,AdminUserController}.java`
- Modify: `backend/src/main/resources/application.yml`
- Test: `backend/src/test/java/com/smarthospital/auth/{AuthServiceTest,AuthControllerTest,RefreshTokenServiceTest}.java`

- [ ] **Step 1: Write failing registration and login tests**

Cover duplicate email, BCrypt password storage, patient self-registration, invalid login, admin-only doctor creation, patient denial for admin routes, inactive-user denial, and `/api/v1/auth/me`.

Expected contract:

```json
{
  "accessToken": "eyJ...",
  "refreshToken": "base64-url-secret",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "user": {"id": 1, "email": "patient@example.com", "role": "PATIENT"}
}
```

- [ ] **Step 2: Implement opaque refresh-token storage and rotation**

Generate 32 random bytes with `SecureRandom`, return URL-safe Base64, and store only SHA-256 hashes. Each refresh rotates the token. If a revoked token is reused, revoke every token in its family. Store expiry, family ID, revoked time, and replacement hash.

- [ ] **Step 3: Implement signed access tokens**

Use Spring Security's Nimbus JWT encoder/decoder with an HMAC secret of at least 32 bytes. Claims contain `sub`, `role`, `type=access`, issuer, issued-at, and expiry. Access lifetime is 15 minutes. Add a `JwtAuthenticationConverter` that maps the role claim to `ROLE_PATIENT`, `ROLE_DOCTOR`, or `ROLE_ADMIN`.

- [ ] **Step 4: Implement security and RFC 9457 errors**

Permit `/api/v1/auth/register`, `/api/v1/auth/login`, `/api/v1/auth/refresh`, `/actuator/health`, `/v3/api-docs/**`, and `/swagger-ui/**`. Authenticate all other API routes. Return `application/problem+json` with a stable machine-readable `code`, `message`, `path`, timestamp, and optional field errors.

- [ ] **Step 5: Add profile and activation APIs**

Patient registration creates `User` and `PatientProfile`. Admin can create a doctor with department and specialty, list users, activate/deactivate accounts, and change a doctor profile. Doctors can read/update only their own profile.

- [ ] **Step 6: Run and commit authentication**

Run `./mvnw test`; expected: all Java tests pass.

```bash
git add backend
git commit -m "feat: add jwt authentication and role authorization"
git push
```

## Task 4: Implement departments, doctors, and fixed schedules

**Files:**
- Create: `backend/src/main/java/com/smarthospital/department/{DepartmentService,DepartmentController,AdminDepartmentController}.java`
- Create: `backend/src/main/java/com/smarthospital/doctor/{DoctorService,DoctorController,AdminDoctorController}.java`
- Create: `backend/src/main/java/com/smarthospital/schedule/{ScheduleService,ScheduleController,AdminScheduleController}.java`
- Test: `backend/src/test/java/com/smarthospital/schedule/ScheduleServiceTest.java`
- Test: `backend/src/test/java/com/smarthospital/department/DepartmentControllerTest.java`

- [ ] **Step 1: Write failing directory and schedule tests**

Test department CRUD authorization, duplicate department names, doctor directory filtering, slot creation, overlapping-slot rejection, slot cancellation, and only returning unbooked future slots to patients.

- [ ] **Step 2: Implement department administration**

Admin can create, update, and deactivate departments. Authenticated clients can list active departments and active doctors. Inactive entities never appear in public directory results.

- [ ] **Step 3: Implement schedule creation and browsing**

An admin creates a doctor slot with `doctorId`, `startsAt`, `endsAt`, and optional reason label. Validate `startsAt < endsAt`, future start, doctor ownership, and no overlap for that doctor. Patients request `/api/v1/doctors/{doctorId}/slots?date=YYYY-MM-DD`.

- [ ] **Step 4: Run and commit scheduling**

Run `./mvnw test`; expected: all tests pass.

```bash
git add backend
git commit -m "feat: add departments doctors and fixed schedules"
git push
```

## Task 5: Implement appointment booking, cancellation, check-in, and tokens

**Files:**
- Create: `backend/src/main/java/com/smarthospital/appointment/{AppointmentService,AppointmentController}.java`
- Create: `backend/src/main/java/com/smarthospital/queue/TokenService.java`
- Test: `backend/src/test/java/com/smarthospital/appointment/AppointmentServiceTest.java`
- Test: `backend/src/test/java/com/smarthospital/appointment/AppointmentControllerTest.java`

- [ ] **Step 1: Write failing appointment lifecycle tests**

Test booking an open slot, rejecting a booked slot, preventing overlapping patient appointments, cancellation by owner/admin, cancellation before check-in only, check-in within the configured window, rejecting early/late check-in, rejecting duplicate check-in, and assigning sequential daily token numbers per doctor.

- [ ] **Step 2: Implement transactional booking**

Lock the slot pessimistically, verify it is open and future, verify no overlapping active appointment for the patient, then create a `BOOKED` appointment. Map a database uniqueness violation to HTTP `409 SLOT_ALREADY_BOOKED`.

- [ ] **Step 3: Implement cancellation**

Patients may cancel their own `BOOKED` appointment; admins may cancel any active appointment. A cancelled appointment releases the slot by setting its active marker to `NULL` and records cancellation time/reason.

- [ ] **Step 4: Implement check-in and digital tokens**

Lock the appointment and any queue entry. Verify ownership or doctor/admin access, appointment date, configurable check-in window, and status. Create a `WAITING` queue entry and allocate the next token for that doctor/date under a database lock. Return token, queue status, triage level, and current position.

- [ ] **Step 5: Run and commit appointments**

Run `./mvnw test`; expected: all tests pass.

```bash
git add backend
git commit -m "feat: add appointment booking and digital queue tokens"
git push
```

## Task 6: Implement emergency triage, queue progression, and audit trail

**Files:**
- Modify: `backend/src/main/java/com/smarthospital/queue/{QueueService,QueueController}.java`
- Test: `backend/src/test/java/com/smarthospital/queue/QueueServiceTest.java`
- Test: `backend/src/test/java/com/smarthospital/queue/QueueControllerTest.java`

- [ ] **Step 1: Write failing queue-ordering tests**

Create entries with controlled arrival times and assert exact order: `CRITICAL`, `HIGH`, `MEDIUM`, `NORMAL`, then FIFO by `arrivedAt` inside each tier. Assert patients can only read their own queue entry while doctors/admins can read their doctor's queue.

- [ ] **Step 2: Implement queue queries and position**

A queue entry's position is the count of waiting entries ordered ahead by the canonical comparator. Return the full ordered doctor queue to staff and a patient-safe view to patients.

- [ ] **Step 3: Implement controlled transitions**

Allowed transitions are:

```text
WAITING -> CALLED -> IN_CONSULTATION -> COMPLETED
WAITING -> SKIPPED -> WAITING
WAITING/CALLED -> CANCELLED
```

A doctor can call the canonical next entry, serve a called entry, complete a served entry, and skip with a reason. Only one `IN_CONSULTATION` entry is allowed per doctor. Invalid transitions return HTTP `409 INVALID_QUEUE_TRANSITION`.

- [ ] **Step 4: Implement triage and audited override**

Doctor/admin can classify `NORMAL`, `MEDIUM`, `HIGH`, or `CRITICAL`. A manual override requires a nonblank reason and writes `queue_audits` with actor, entry, old priority, new priority, reason, and timestamp.

- [ ] **Step 5: Run and commit queue management**

Run `./mvnw test`; expected: all tests pass.

```bash
git add backend
git commit -m "feat: add emergency priority and queue progression"
git push
```

## Task 7: Build the Python ML service and Java prediction integration

**Files:**
- Create: `ml-service/app/{config.py,schemas.py,model.py,training.py}`
- Create: `ml-service/tests/test_model.py`
- Modify: `ml-service/app/main.py`
- Create: `backend/src/main/java/com/smarthospital/waiting/{WaitingTimeService,WaitingTimeClient,WaitingTimeFallback,WaitingTimeController,PredictionSource}.java`
- Modify: `backend/src/main/resources/application.yml`
- Test: `backend/src/test/java/com/smarthospital/waiting/{WaitingTimeServiceTest,WaitingTimeFallbackTest}.java`

- [ ] **Step 1: Write failing Python model and API tests**

Validate `/predict`, feature schema, nonnegative estimates, deterministic training, model metadata, and synthetic-data provenance. The training target must increase with queue size and emergency load but include bounded noise.

- [ ] **Step 2: Implement a reproducible baseline model**

Generate a deterministic seeded synthetic historical dataset. Train a scikit-learn `RandomForestRegressor` with a fixed random seed. Persist `model.joblib` plus `model-metadata.json` containing model version, feature schema version, training row count, seed, and `"synthetic-demo-data"` provenance. Retrain when the artifact is absent or incompatible.

- [ ] **Step 3: Implement the prediction contract**

Request fields:

```json
{
  "queuePosition": 3,
  "peopleWaiting": 5,
  "averageConsultationMinutes": 15.0,
  "activeEmergencyCount": 1,
  "triageTier": "HIGH",
  "hourOfDay": 10,
  "dayOfWeek": 2,
  "minutesFromScheduledStart": -5
}
```

Response fields:

```json
{
  "estimatedWaitMinutes": 24,
  "confidence": 0.82,
  "modelVersion": "waiting-time-v1",
  "featureSchemaVersion": 1,
  "trainingDataProvenance": "synthetic-demo-data"
}
```

- [ ] **Step 4: Implement Java ML client and fallback**

Use Spring `RestClient` with a 750 ms connect/read timeout. Map a successful response to source `ML`. On timeout, connection failure, invalid response, or non-2xx status, use:

```text
remainingPeople × averageConsultationMinutes
+ activeEmergencyCount × 5
```

Round to the nearest minute, clamp to zero, and mark source `FALLBACK` with confidence `0.50`.

- [ ] **Step 5: Expose the waiting-time endpoint**

`GET /api/v1/queue/{entryId}/waiting-time` returns estimate, source, confidence, model metadata when available, queue position, and calculated timestamp. Patient ownership and staff scope are enforced.

- [ ] **Step 6: Run both test suites and commit**

Run `./mvnw test` and `ml-service/.venv/bin/pytest`; expected: all tests pass.

```bash
git add backend ml-service
git commit -m "feat: add ml waiting time prediction with fallback"
git push
```

## Task 8: Add dashboards and OpenAPI contract

**Files:**
- Create: `backend/src/main/java/com/smarthospital/dashboard/{DashboardService,DashboardController}.java`
- Create: `backend/src/main/java/com/smarthospital/config/OpenApiConfig.java`
- Test: `backend/src/test/java/com/smarthospital/dashboard/DashboardServiceTest.java`

- [ ] **Step 1: Write failing dashboard tests**

Patient dashboard returns upcoming appointments and active queue entry. Doctor dashboard returns today's schedule and current queue. Admin dashboard returns user, department, doctor, appointment, and queue totals for a selected date.

- [ ] **Step 2: Implement scoped dashboard queries**

Never accept a doctor ID from a doctor client; derive it from the authenticated profile. Admin may optionally filter by date, defaulting to today in the server's configured zone.

- [ ] **Step 3: Add OpenAPI metadata and endpoint documentation**

Configure title, version, description, bearer JWT scheme, and server URL. Annotate public, patient, doctor, and admin endpoint groups. Swagger UI must be available at `/swagger-ui.html` and the JSON contract at `/v3/api-docs`.

- [ ] **Step 4: Run and commit dashboards/API docs**

Run `./mvnw test`; expected: all tests pass.

```bash
git add backend
git commit -m "feat: add role dashboards and openapi documentation"
git push
```

## Task 9: Add local operations, containers, and CI

**Files:**
- Create: `.env.example`
- Create: `docker-compose.yml`
- Create: `backend/Dockerfile`
- Create: `ml-service/Dockerfile`
- Create: `.github/workflows/ci.yml`
- Modify: `README.md`
- Modify: `.gitignore`

- [ ] **Step 1: Write and validate Docker Compose configuration**

Compose starts MySQL 8.4 with a health check. Backend and ML services live behind an `full` profile so local-first development remains simple.

Run `docker compose config`; expected: valid merged configuration with no secret values printed from `.env`.

- [ ] **Step 2: Add production-sensible container definitions**

Backend Dockerfile uses Maven build stage and Java 21 JRE runtime. ML Dockerfile uses Python 3.12 slim, installs the locked project, and starts Uvicorn. Neither image includes local secrets.

- [ ] **Step 3: Add GitHub Actions**

The workflow uses Java 21 and Python 3.12 caches. It starts a MySQL 8.4 service, runs `./mvnw verify`, and runs `pytest`. It must not require repository secrets.

- [ ] **Step 4: Document local setup**

README includes prerequisites, MySQL setup, migrations, seeded credentials, Maven commands, virtualenv commands, Python training, Swagger URL, environment variables, Docker option, and the complete API demo flow.

- [ ] **Step 5: Commit and push operations**

```bash
git add .env.example docker-compose.yml backend/Dockerfile ml-service/Dockerfile .github README.md .gitignore
git commit -m "chore: add local containers and continuous integration"
git push
```

## Task 10: Add end-to-end verification and final documentation

**Files:**
- Create: `backend/src/test/java/com/smarthospital/e2e/PatientJourneyTest.java`
- Create: `docs/api-demo.md`
- Create: `docs/architecture.md`
- Modify: `README.md`

- [ ] **Step 1: Write the full journey integration test**

Using MockMvc against a real MySQL Testcontainer, the test performs:

1. Register patient.
2. Login patient.
3. Login seeded doctor and admin.
4. Admin creates a department, doctor, and fixed slot.
5. Patient discovers and books the slot.
6. Patient checks in and receives a token.
7. Doctor classifies the entry as `HIGH`.
8. Doctor calls, serves, and completes the entry.
9. Patient requests waiting time with the ML client stubbed to fail and receives `FALLBACK`.

- [ ] **Step 2: Run the complete verification suite**

```bash
cd backend && ./mvnw clean verify
cd ../ml-service && .venv/bin/pytest
cd .. && docker compose config
```

Expected: all Java tests, Python tests, and Compose validation pass.

- [ ] **Step 3: Document architecture and a reproducible API demo**

`docs/architecture.md` describes modules, data ownership, transaction boundaries, queue ordering, token rotation, ML fallback, and deployment topology. `docs/api-demo.md` contains exact curl commands and expected response fields for the complete patient/doctor/admin flow.

- [ ] **Step 4: Review secrets and generated artifacts**

Run:

```bash
git grep -nE '(gh[opsu]_[A-Za-z0-9]{20,}|BEGIN (RSA |EC )?PRIVATE KEY|password\s*[:=]\s*[^$])' -- . ':!docs/superpowers' ':!README.md' ':!docs/api-demo.md' || true
git status --short
```

Expected: no real secrets or generated model artifacts are staged.

- [ ] **Step 5: Commit and push final verification**

```bash
git add backend docs README.md
git commit -m "test: verify complete smart hospital workflow"
git push
```

## Task 11: Review, merge, and publish

**Files:**
- Modify only files required by review findings.

- [ ] **Step 1: Request code review**

Review against `docs/superpowers/specs/2026-11-15-smart-hospital-backend-design.md`, focusing on authorization, transaction boundaries, queue ordering, token security, and fallback behavior.

- [ ] **Step 2: Run final verification from a clean checkout**

```bash
git clone --branch feature/backend-mvp https://github.com/HimanshuxAI/smart-hospital-backend.git /tmp/smart-hospital-clean
cd /tmp/smart-hospital-clean/backend && ./mvnw clean verify
cd ../ml-service && python3.12 -m venv .venv && .venv/bin/pip install -e '.[dev]' && .venv/bin/pytest
```

Expected: clean checkout builds and all tests pass.

- [ ] **Step 3: Merge without rewriting history**

Fast-forward `main` to `feature/backend-mvp`, push `main`, and verify the GitHub repository remains private.

```bash
git checkout main
git merge --ff-only feature/backend-mvp
git push origin main
gh repo view HimanshuxAI/smart-hospital-backend --json visibility,url
```

Expected: `visibility` is `PRIVATE` and the URL is the configured repository.
