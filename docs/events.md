# Kafka events

This is the contract between services. A service that publishes an event must follow this format, and a service that consumes it can rely on it. Change this file (through a PR) **before** changing the code.

## Topics

| Topic | Producer | Consumers | Message key |
|---|---|---|---|
| `bookings.events` | core-service | notification-service, mcp-server | `bookingId` |
| `devices.events` | core-service | notification-service, mcp-server | `machineId` |
| `feedback.events` | core-service | mcp-server | `feedbackId` |
| `proposals.events` | core-service | notification-service, mcp-server | `proposalId` |

Only **core-service** publishes. Other services never write to the database; they build their own copies from these events.

The message key decides the partition, so all events of the same booking (or machine, or proposal) stay in order.

## Envelope

Every event has the same outer structure:

```json
{
  "eventId": "7f1c2a9e-3b4d-4c51-9a0e-2d6f8b1c0e11",
  "type": "booking.created",
  "version": 1,
  "occurredAt": "2026-11-14T15:30:00Z",
  "payload": { }
}
```

| Field | Meaning |
|---|---|
| `eventId` | UUID, unique per event; consumers use it to ignore duplicates |
| `type` | event name, one of the types below |
| `version` | schema version of the payload; increase it when the payload changes in an incompatible way |
| `occurredAt` | when it happened, ISO-8601 in UTC |
| `payload` | event-specific data |

## bookings.events

### `booking.created`
```json
{
  "bookingId": 42,
  "userId": 7,
  "machineId": 3,
  "startTime": "2026-11-15T18:00:00Z",
  "endTime": "2026-11-15T19:30:00Z"
}
```

### `booking.cancelled`
```json
{ "bookingId": 42, "userId": 7, "machineId": 3, "reason": "user_cancelled" }
```

### `booking.started`
The student pressed "Am început".
```json
{ "bookingId": 42, "userId": 7, "machineId": 3, "startedAt": "2026-11-15T18:04:00Z" }
```

### `booking.noshow`
No start confirmation within 10 minutes of `startTime`.
```json
{ "bookingId": 42, "userId": 7, "machineId": 3, "startTime": "2026-11-15T18:00:00Z" }
```

## devices.events

### `fault.reported`
```json
{
  "faultId": 15,
  "machineId": 3,
  "reportedBy": 7,
  "description": "Water does not drain",
  "reportedAt": "2026-11-15T18:20:00Z"
}
```

### `maintenance.opened`
```json
{ "machineId": 3, "proposalId": 9, "openedAt": "2026-11-16T08:30:00Z" }
```

### `maintenance.closed`
The machine is repaired and available again.
```json
{ "machineId": 3, "closedAt": "2026-11-17T12:00:00Z" }
```

## feedback.events

### `feedback.submitted`
```json
{ "feedbackId": 21, "userId": 7, "message": "Dryer 01 takes too long", "submittedAt": "2026-11-15T20:00:00Z" }
```

## proposals.events

### `proposal.created`
Created by the AI agent through the MCP server; status is always `PENDING`.
```json
{
  "proposalId": 9,
  "kind": "MAINTENANCE",
  "targetId": 3,
  "reason": "4 faults reported in the last 24h",
  "severity": "HIGH",
  "status": "PENDING"
}
```
`kind` is `MAINTENANCE` (targetId = machineId) or `PENALTY` (targetId = userId). `severity` is `LOW`, `MEDIUM` or `HIGH`.

### `proposal.approved` / `proposal.rejected`
```json
{ "proposalId": 9, "decidedBy": 1, "decidedAt": "2026-11-16T08:30:00Z" }
```

## Rules

- Consumers must ignore unknown `type` values and unknown fields, so new events don't break old services.
- IDs are numbers from the core-service database; times are always UTC.
- Never put passwords, tokens or other personal data beyond IDs in events.
