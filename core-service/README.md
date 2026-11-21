# core-service

REST API and PostgreSQL persistence layer of **Laundry Booking**. It is the **only service that writes to the database** (single source of truth); the other services build their own copies from Kafka events (see `docs/events.md`).

- Java 21, Spring Boot 4.1.2, Spring Data JPA, Flyway, PostgreSQL 16
- Package: `ro.upt.laundry.core`
- Port: `8081`

## What it does

| Area | Behavior |
|---|---|
| Machines | List machines, see the active bookings of a machine, close a maintenance period |
| Bookings | Create, cancel, start ("Am început"); no overlapping bookings per machine |
| No-show | A scheduled job marks bookings as `NO_SHOW` when they were not started within 10 minutes of `startTime` |
| Faults | Students report faults; the server sets `reportedAt` |
| Feedback | Students submit feedback; the admin lists it |
| Proposals | Created by the AI agent (through the MCP server), approved/rejected by an admin |

Out of scope (per the project proposal): payments, real machine integration, real authentication.

## Run it (Windows / PowerShell)

Prerequisites: JDK 21, Docker Desktop. Maven is not needed (the Maven Wrapper is included).

```powershell
cd core-service
Copy-Item ..\.env.example .env        # first time only; set POSTGRES_PASSWORD
docker compose up -d postgres         # from the repo root or core-service/ (Compose finds docker-compose.yml)
.\mvnw.cmd spring-boot:run
```

Stop: `Ctrl + C`, then `docker compose down` (data is kept in the volume; `down -v` deletes it).

Notes:
- Spring Boot does not read `.env` by itself. `application.yml` imports it with
  `spring.config.import: optional:file:.env[.properties]`, relative to the **working directory**, so run the app from `core-service/`.
- In `.env`, write `POSTGRES_PASSWORD=value` without quotes or trailing spaces (Compose and Spring parse the file differently).
- A local PostgreSQL service on port 5432 conflicts with the container. Stop it or change the port mapping.
- Swagger UI: http://localhost:8081/swagger-ui.html, OpenAPI JSON/YAML: `/v3/api-docs` and `/v3/api-docs.yaml`.

### Configuration

| Property | Default | Meaning |
|---|---|---|
| `laundry.booking.noshow-grace` | `10m` | Time after `startTime` in which a booking can still be started |
| `laundry.booking.noshow-check-ms` | `60000` | Delay between no-show job runs |
| `laundry.cors.allowed-origins` | `http://localhost:5173,http://localhost:3000` | Allowed browser origins |

Example for manual no-show testing:
`.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--laundry.booking.noshow-grace=30s --laundry.booking.noshow-check-ms=10000"`

## Architecture

```
Controller  ->  Service  ->  Repository  ->  PostgreSQL
 (HTTP/JSON)   (rules,       (Spring Data     (schema via Flyway)
  DTOs          transactions)  JPA)
```

- Entities are never returned directly; controllers return DTO records.
- Errors are `ApiException(status, message)` translated by `GlobalExceptionHandler` into RFC 9457 `ProblemDetail` JSON.
- Time is always UTC (`Instant`, `timestamptz`), taken from an injected `Clock`.

### Layout

```
core-service/src/main/java/ro/upt/laundry/core/
├── config/        Clock, scheduling, CORS, OpenAPI
├── controller/    Machine, Booking, Fault, Feedback, Proposal
├── dto/           request/response records
├── exception/     ApiException, GlobalExceptionHandler
├── model/         entities and enums
├── repository/    Spring Data repositories
├── scheduler/     NoShowScheduler
└── service/       business logic
core-service/src/main/resources/
├── application.yml
└── db/migration/  V1..V5 (Flyway)
```

## Database

Schema is versioned with Flyway. **Never edit an applied migration; add a new `V<n>__name.sql`.** Hibernate only validates (`ddl-auto: validate`).

| Migration | Content |
|---|---|
| `V1__init.sql` | `users`, `machines` (+ 4 seed machines) |
| `V2__bookings.sql` | `bookings`, overlap constraint, seed users (1 = ADMIN, 2 = STUDENT) |
| `V3__faults_feedback.sql` | `faults`, `feedback` |
| `V4__proposals.sql` | `proposals`, unique partial index for PENDING |
| `V5__booking_version.sql` | `bookings.version` (optimistic locking) |

Rules enforced **in the database** (cannot be bypassed by a bug or a race):
- No two `CREATED`/`STARTED` bookings overlap on the same machine (`EXCLUDE USING gist` with `tstzrange`; adjacent slots are allowed, cancelled/no-show bookings free the slot).
- One `PENDING` proposal per `(kind, target_id)`.
- A proposal has `decided_by`/`decided_at` if and only if it is not `PENDING`.
- Enumerations are `CHECK` constraints; foreign keys on users and machines.

## API

Base URL `http://localhost:8081`. Full contract: Swagger UI.

| Method | Path | Success | Errors |
|---|---|---|---|
| GET | `/machines` | 200 | |
| GET | `/machines/{id}/bookings` | 200 | 404 |
| POST | `/machines/{id}/maintenance/close` | 200 | 404, 409 |
| POST | `/bookings` | 201 | 400, 404, 409 |
| GET | `/bookings/{id}` | 200 | 404 |
| GET | `/bookings?userId=` | 200 | |
| POST | `/bookings/{id}/cancel` | 200 | 404, 409 |
| POST | `/bookings/{id}/start` | 200 | 404, 409 |
| POST | `/faults` | 201 | 400, 404 |
| GET | `/faults?machineId=` | 200 | 404 |
| POST | `/feedback` | 201 | 400, 404 |
| GET | `/feedback` | 200 | |
| POST | `/proposals` | 201 | 400, 404, 409 |
| GET | `/proposals?status=` | 200 | 400 |
| GET | `/proposals/{id}` | 200 | 404 |
| POST | `/proposals/{id}/approve` | 200 | 403, 404, 409 |
| POST | `/proposals/{id}/reject` | 200 | 403, 404, 409 |

Business rules:
- **Booking create:** `endTime > startTime`, start in the future, user and machine exist, machine is `AVAILABLE`, no overlap (409).
- **Cancel:** only `CREATED` bookings.
- **Start:** only `CREATED`, and only between `startTime` and `startTime + 10 min` (capped at `endTime`).
- **No-show:** the scheduler marks `CREATED` bookings with `startTime < now - 10 min` as `NO_SHOW`. Optimistic locking (`@Version`) makes a concurrent `start` and no-show update safe: the loser gets 409.
- **Proposal create:** `MAINTENANCE` (target = machineId) requires `severity` and an `AVAILABLE` machine; `PENALTY` (target = userId) defaults `severity` to `MEDIUM`.
- **Approve/reject:** the row is locked (`SELECT ... FOR UPDATE`); `decidedBy` must be an `ADMIN` (403). Approving a `MAINTENANCE` proposal, in one transaction: marks the proposal approved, sets the machine to `MAINTENANCE`, cancels its upcoming `CREATED` bookings. A started cycle is not interrupted.
- **Reporting a fault does not change the machine status**; maintenance opens only through an approved proposal.

## Kafka integration points

core-service publishes the events from `docs/events.md` through the `events` package:

- `EventRecorder.record(topic, key, type, payload)` is called inside the `@Transactional` service methods. It only raises an in-memory Spring event.
- `KafkaEventRelay` (`@TransactionalEventListener(AFTER_COMMIT)`) wraps it in the common envelope (`eventId`, `type`, `version`, `occurredAt`, `payload`) and sends it to Kafka **only after the transaction commits**. On rollback nothing is sent.
- The message key is the entity id (`bookingId`, `machineId`, `proposalId`, `feedbackId`), so events of one entity stay in order.
- Producer: `acks=all`, idempotence enabled. Kafka address: `KAFKA_BOOTSTRAP_SERVERS` (default `localhost:9092`).

Where each event is recorded:

| Where | Event(s) |
|---|---|
| `BookingService.create` | `booking.created` |
| `BookingService.cancel` | `booking.cancelled` (`reason: user_cancelled`) |
| `BookingService.start` | `booking.started` |
| `BookingService.markNoShows` (called by `NoShowScheduler`) | `booking.noshow`, one per returned booking |
| `FaultService.report` | `fault.reported` |
| `FeedbackService.submit` | `feedback.submitted` |
| `ProposalService.create` | `proposal.created` |
| `ProposalService.approve` | `proposal.approved`; for `MAINTENANCE` also `maintenance.opened` (`proposalId`, `openedAt` = `decidedAt`) and one `booking.cancelled` per booking returned by `cancelUpcomingForMachine` (`reason: maintenance`) |
| `ProposalService.reject` | `proposal.rejected` |
| `MachineService.closeMaintenance` | `maintenance.closed` |

Keys: `bookingId`, `machineId`, `feedbackId`, `proposalId` as in `docs/events.md`.

## Testing

- `scripts/smoke-test.ps1` runs the HTTP scenarios (status codes) against the running app:
  `powershell -ExecutionPolicy Bypass -File .\scripts\smoke-test.ps1`
- `.\mvnw.cmd test` runs the integration tests with Testcontainers (a real PostgreSQL in Docker, so Docker Desktop must be running). A build inside a Docker image without Docker must use `-DskipTests`.
- Not covered by the script: no-show (needs real time, use the shortened grace period) and cancellation of bookings on maintenance approval.

## Open decisions (for the team)

1. **Authentication:** none yet. `userId`, `reportedBy`, `decidedBy` come from the request body, and `POST /proposals` is open to anyone, not only the MCP server. Decide on a simple scheme (token per user, a shared key for the MCP server).
2. **Effect of an approved `PENALTY`:** currently only the status changes. There is no penalty table or event. Decide what a penalty means (blocked bookings for N days? a counter?).
3. **`severity` for `PENALTY`:** `propose_penalty(user_id, reason)` has no severity, but `proposal.created` requires it. Default `MEDIUM` is provisional.
4. **`events.md`:** add `reason: "maintenance"` for `booking.cancelled`, and confirm that no event is needed for penalties.
5. **OpenAPI:** keep `docs/openapi.yaml` generated from the code (current snapshot) or write the contract first?
6. **Missing endpoints for the frontend:** list all bookings for the admin, list users / simple login, create and edit machines (the proposal says the admin manages machines).
7. **CORS and Docker:** how `web-client` reaches core-service in `docker-compose` (origin, proxy), and a `Dockerfile` for core-service (`-DskipTests`).
8. **Scheduler in several instances:** the no-show job is safe with optimistic locking but would run twice; use a lock (e.g. ShedLock) if core-service is ever scaled.
9. **Seed data:** only 2 users and 4 machines exist; decide whether a shared `V6__seed.sql` with more test data is wanted.
10. **Event publishing guarantees:** publishing after commit can lose an event if the app crashes in between; an outbox table is the robust alternative.
