# Color Tinting Machine

Operator software for a single automatic color-tinting machine. The dashboard, API, and database are implemented. The ESP32, TCS3200, load cell, and pumps are not connected, and this repository does not claim that physical dispensing, calibration, or emergency-stop behavior has been tested.

## Features

- Responsive operator dashboard with connection state, color readings, weight, four pump cards, batch progress, and faults
- Recipe create, edit, search, validation, and delete
- Persistent batch history with a recipe snapshot and nullable actual quantities
- Command lifecycle for start, cancel, tare, sensor calibration, single-pump test, and software emergency stop
- Hardware actions stay disabled until a confirmed ESP32 integration reports a live, fresh, ready state
- PostgreSQL schema managed by Flyway
- Provisional ESP32 adapter with a separate simulated fixture for tests

## Architecture

```
Operator browser
    |
    v
Next.js dashboard  --HTTP-->  Spring Boot API  --JDBC-->  PostgreSQL
                                    |
                                    +--> Esp32MachineClient
                                              |
                                              +--> HTTP adapter (provisional, commands off)
                                              +--> Simulated fixture (tests only)
```

The backend never treats a button click, a timer, or a successful HTTP request as proof that a pump ran or a batch finished. Batch completion is applied only when the machine report includes that batch and a terminal state.

## Technology

- Frontend: Next.js 16, React 19, TypeScript, Tailwind CSS 4, TanStack Query, React Hook Form, Zod, Vitest, Playwright
- Backend: Java 25, Spring Boot 4.1.1, Spring Web MVC, Validation, Data JPA, Flyway, Actuator, springdoc OpenAPI
- Database: PostgreSQL 16

Spring Boot 4.1.1 is used because the installed JDK is Java 25.

## Repository layout

```
frontend/     Next.js operator console
backend/      Spring Boot API
docs/         Architecture, setup, tests, safety, and the ESP32 contract
docker-compose.yml
```

## Prerequisites

- Node.js 24 and npm
- JDK 25
- Maven 3.9 or newer
- Docker Desktop, for local PostgreSQL and backend integration tests

## Run locally

Start PostgreSQL:

```powershell
docker compose up -d
```

Start the API from `backend`:

```powershell
mvn spring-boot:run
```

The API listens on `http://localhost:8080`. OpenAPI is at `http://localhost:8080/swagger-ui.html`.

Start the dashboard from `frontend`:

```powershell
npm install
npm run dev
```

Open `http://localhost:3000`.

With the default configuration the ESP32 base URL is empty, hardware commands are disabled, and the dashboard shows a disconnected or communication-error state. It does not invent RGB values or weight.

## Environment

Copy the examples and change them for the machine network:

- `backend/.env.example`
- `frontend/.env.example`

Do not put the ESP32 address in `NEXT_PUBLIC_` variables. The browser talks only to the backend. Real `.env` files are gitignored.

`APP_MACHINE_COMMANDS_ENABLED` and `APP_MACHINE_INTEGRATION_CONFIRMED` must both stay `false` until the hardware developer confirms `docs/esp32-api-contract.md`. Setting only one of them does not enable pump commands.

## Tests

See `docs/testing.md`.

```powershell
cd frontend
npm test
npm run typecheck
npm run lint
npm run build

cd ../backend
mvn test
```

`mvn test` runs unit tests and, when Docker is available, PostgreSQL integration tests through Testcontainers. Those integration tests were executed successfully in this workspace.

## ESP32 status

The HTTP adapter and the provisional contract exist. The hardware developer has sent a pin map (`docs/hardware-pin-map.md`): HX711 load cell, TCS3200, four active-low pump relays, and a physical stop input. No firmware API was supplied, no device was reached, and no physical pump, sensor, load cell, or emergency stop was verified. Pump 4 is labeled Key/Base on that map; the software channel remains Black until that name is confirmed.

A simulated adapter exists only for automated tests and must be selected explicitly with `APP_MACHINE_ADAPTER=simulated`. Its readings are labeled simulated.

## Known limitations

- Single machine, no user authentication
- Software emergency stop is not a safety system
- CMYK doses are operator-entered quantities, not an automatic paint match
- Software dose limits are provisional configuration, not firmware limits
- High-frequency telemetry is not stored
- The unauthenticated local API must not be exposed beyond the machine network

## Further reading

- `docs/setup.md`
- `docs/architecture.md`
- `docs/esp32-api-contract.md`
- `docs/hardware-pin-map.md`
- `docs/safety-and-fault-handling.md`
- `docs/testing.md`
