# Smart Hospital Backend

Backend for the **Smart Hospital Patient Queue and Appointment Management System**.

The project combines a Spring Boot API with a Python machine-learning sidecar to provide:

- Patient self-registration and JWT authentication
- Role-based patient, doctor, and administrator access
- Doctor and department administration
- Fixed appointment slots and conflict-safe booking
- Digital queue tokens
- Tiered emergency prioritization with FIFO within each tier
- Doctor queue controls and auditable manual overrides
- Waiting-time prediction through a Python ML service, with a deterministic Java fallback
- OpenAPI documentation and Swagger UI
- Automated tests and continuous integration

## Repository layout

```text
backend/                 Spring Boot REST API
ml-service/              FastAPI waiting-time prediction service
docs/                    Architecture and implementation documentation
docker-compose.yml       Local MySQL and optional full-stack services
```

## Technology

- Java 21, Spring Boot, Maven Wrapper
- MySQL 8
- Python 3.11+, FastAPI, scikit-learn
- JWT access and refresh tokens
- Flyway database migrations
- OpenAPI / Swagger UI

Detailed setup instructions are added as the implementation progresses.
