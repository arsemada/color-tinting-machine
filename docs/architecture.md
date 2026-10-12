# Architecture

The system is one Next.js application and one Spring Boot application for a single machine. There is no microservice split.

## Boundaries

- React components render status and collect input. They do not decide that a pump is on because a button was clicked.
- `frontend/src/lib/api` is the only browser HTTP client. It calls the backend.
- Spring controllers validate requests and map DTOs. Services own transactions and state changes.
- `Esp32MachineClient` is the only hardware port. `HttpEsp32MachineClient` speaks the provisional HTTP contract. `SimulatedEsp32MachineClient` is a test fixture and is not loaded unless `app.machine.adapter=simulated`.
- PostgreSQL stores recipes, batch snapshots, pump targets and reported actuals, commands, events, and display settings. Telemetry samples are not stored.

## Time and numbers

Persisted timestamps are `timestamptz` and are written from a UTC clock. Quantities are `numeric(12,3)` grams in the database and decimal strings in JSON so they are not binary floating-point values.

## Batch transitions

Active states: `STARTING`, `DISPENSING`, `PAUSED`, `CANCELLING`.

Terminal states: `COMPLETED`, `CANCELLED`, `FAILED`. Terminal states do not change again.

A missing or `UNKNOWN` machine report does not change the current state. `COMPLETED` is applied only from a machine report that names the batch. A start acknowledgement leaves the batch in `STARTING`.

One active batch per machine is enforced by a partial unique index. Command writes lock the operator-settings row so two starts cannot pass the check together.

## Command lifecycle

`PENDING` means the backend accepted the request and is about to dispatch it.

`ACKNOWLEDGED` or `EXECUTING` means the adapter returned that result. For a batch start, `SUCCEEDED` from a device is stored as `ACKNOWLEDGED` so a command response is not treated as batch completion.

`REJECTED` means the device or the backend policy refused the command. If it was refused before dispatch, `dispatchedToHardware` is false.

`TIMED_OUT` means the device did not answer in time. The command is not retried. The batch stays non-terminal, `outcomeUnknown` is set, and further starts are blocked until an authoritative report arrives or an operator records the outcome as failed. That recovery path cannot mark a batch completed.

## Connection truth

Before the first probe the state is `CONNECTING`. An empty ESP32 URL is `DISCONNECTED`. A failed probe is `COMMUNICATION_ERROR` and current readings are unavailable. A previous success may be returned separately as `lastKnown` and must be labeled as last known. A device timestamp older than `app.machine.stale-after-ms` is `STALE`.

`hardwareVerified` is true only when the adapter is the HTTP adapter, both confirmation flags are true, the latest probe succeeded, the data source is `LIVE`, the connection state is `CONNECTED`, and the reading is not stale. The operator controls use that flag. The simulated fixture can accept commands on the API for tests, but `hardwareVerified` stays false, so the dashboard does not enable those controls.

## Configuration

Machine identity, adapter choice, base URL, timeouts, poll interval, software dose limits, and provisional ESP32 paths live in `backend/src/main/resources/application.yml` and environment variables. CORS allows the configured dashboard origin only, defaulting to `http://localhost:3000`.

## Future expansion

Authentication should be added before any multi-user or non-local deployment. A second machine would need the active-batch uniqueness and command lock to stay scoped by machine id, which they already are. A formulation service, if added later, must sit behind a separate interface and must not treat an sRGB hex color as a paint recipe.
