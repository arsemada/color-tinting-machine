# Setup

Commands below use PowerShell from the repository root unless noted.

## PostgreSQL with Docker

```powershell
docker compose up -d
docker compose ps
```

The compose file publishes PostgreSQL on `127.0.0.1:5434` so it does not collide with a PostgreSQL instance already installed on port 5432. The database name, user, and password are `color_tinting`. That password is a local development default. Change it before any shared or production deployment. The JDBC URL uses `127.0.0.1` rather than `localhost` so Windows does not prefer a different IPv6 server.

Flyway runs on backend startup. Hibernate validates the schema and does not create tables itself.

To stop the database:

```powershell
docker compose down
```

`docker compose down -v` deletes the data volume.

## PostgreSQL without Docker

Install PostgreSQL 16, create the same database and user, and set:

```text
SPRING_DATASOURCE_URL=jdbc:postgresql://127.0.0.1:5434/color_tinting
SPRING_DATASOURCE_USERNAME=color_tinting
SPRING_DATASOURCE_PASSWORD=your-password
```

Integration tests still need Docker because they use Testcontainers. Unit tests do not.

## Backend

From `backend`, with JDK 25 and Maven 3.9:

```powershell
mvn spring-boot:run
```

Optional environment variables are listed in `backend/.env.example`. Spring Boot reads them from the process environment. A local `.env` file is not loaded automatically unless you export the variables yourself.

Useful checks:

- `http://localhost:8080/api/v1/health` should report `database: UP`
- `http://localhost:8080/actuator/health` is the process probe and does not include secrets
- `http://localhost:8080/swagger-ui.html` documents `/api/v1`

## Frontend

From `frontend`:

```powershell
npm install
copy .env.example .env.local
npm run dev
```

`NEXT_PUBLIC_API_BASE_URL` defaults to `http://localhost:8080` when unset. Change it if the API is on another origin, and add that origin to `APP_CORS_ALLOWED_ORIGINS` or `app.cors.allowed-origins`. Production CORS is an explicit list, not `*`.

## Simulated fixture

Only for software tests:

```powershell
$env:APP_MACHINE_ADAPTER = "simulated"
mvn spring-boot:run
```

The dashboard shows a simulated banner. Pump controls stay disabled because the fixture is not verified hardware. Test code may still post commands to the fixture. `POST /api/v1/machine/fixture/scenario` and `POST /api/v1/machine/fixture/batch-report` exist only in this mode.

## Troubleshooting

- Backend fails on startup with a database connection or password error: this machine may already have PostgreSQL on port 5432. The compose file uses host port 5434. Confirm `docker compose ps` shows `0.0.0.0:5434->5432/tcp`, and that `SPRING_DATASOURCE_URL` points at `127.0.0.1:5434`.
- Port 8080 is already in use: start the API with `$env:SERVER_PORT = "8090"; mvn spring-boot:run` and set `NEXT_PUBLIC_API_BASE_URL` to `http://127.0.0.1:8090`. Add that frontend origin to `APP_CORS_ALLOWED_ORIGINS` if it is not already listed. Do not stop an unrelated process that owns port 8080.
- Dashboard says the backend is unavailable: confirm the API port and that the frontend origin is allowed by CORS.
- Dashboard never says Connected: that is expected until an ESP32 answers the telemetry URL and the integration flags are confirmed. An empty base URL is reported as disconnected.
- A batch stays `STARTING`: the machine has not reported `DISPENSING` or a terminal state. Do not treat the start response as completion.
- A timed-out command blocks the next start: query telemetry, or record the unknown outcome as failed from `POST /api/v1/commands/{id}/resolve-unknown`. That path cannot mark the batch completed.
- Maven prints Java native-access warnings on JDK 25: the build can still succeed. They come from Maven's console library.
- Frontend production build requires the Next.js 16 cache-component rules. `usePathname` is wrapped in `Suspense` in the root layout for that reason.
