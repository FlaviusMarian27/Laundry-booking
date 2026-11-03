# Project structure

Detailed layout of the repository. The top-level folders are stable; the packages inside each service may change as the code evolves, so keep this file up to date when you add or move something.

```
Laundry-booking/
├── core-service/              # Java + Spring Boot
│   ├── src/main/java/.../
│   │   ├── controller/        # REST endpoints (bookings, faults, proposals)
│   │   ├── service/           # business logic
│   │   ├── repository/        # PostgreSQL access
│   │   ├── model/             # entities (Booking, Machine, Fault, Proposal)
│   │   └── kafka/             # event producer
│   ├── src/main/resources/application.yml
│   ├── pom.xml
│   └── Dockerfile
│
├── notification-service/      # Java + Spring Boot
│   ├── src/main/java/.../
│   │   ├── kafka/             # event consumer
│   │   ├── store/             # its own copy of the data
│   │   └── notifier/          # sends notifications
│   ├── pom.xml
│   └── Dockerfile
│
├── mcp-server/                # Java
│   ├── src/main/java/.../
│   │   ├── tools/             # get_device_errors, propose_maintenance, ...
│   │   ├── kafka/             # consumer that builds the read copy
│   │   └── client/            # REST calls to core-service (propose_*)
│   ├── pom.xml
│   └── Dockerfile
│
├── agent/                     # Python
│   ├── agent.py               # agent loop (LLM + MCP tools)
│   ├── prompts/               # system prompt
│   ├── requirements.txt
│   └── Dockerfile
│
├── web-client/                # React
│   ├── src/
│   │   ├── pages/             # Student, Admin
│   │   ├── components/
│   │   └── api/               # calls to core-service
│   ├── package.json
│   └── Dockerfile
│
├── docs/
│   ├── architecture.png       # architecture diagram
│   ├── Laundry_Booking_Prezentare.pdf
│   ├── structure.md           # this file
│   ├── openapi.yaml           # core-service API documentation
│   └── events.md              # JSON format of every Kafka event
│
├── docker-compose.yml         # Kafka, PostgreSQL and all services
├── .env.example               # environment variables template (no real secrets)
├── .gitignore
└── README.md
```

## Services

| Folder | Role | Talks to |
|---|---|---|
| `core-service` | REST API, the only service that writes to PostgreSQL, publishes events | PostgreSQL, Kafka (produce) |
| `notification-service` | Builds its own copy of bookings from events and sends notifications | Kafka (consume) |
| `mcp-server` | Remote MCP server with read and propose tools | Kafka (consume), core-service (REST) |
| `agent` | Runs daily, uses the MCP tools and an LLM to create proposals | mcp-server, LLM API |
| `web-client` | UI for students and the admin | core-service (REST) |

## Conventions

- Each service has its own `Dockerfile`; `docker-compose.yml` at the root starts everything with `docker compose up`.
- Event formats are defined in `docs/events.md` before they are used in code.
- Secrets go only in `.env` (ignored by Git); `.env.example` lists the variable names without values.