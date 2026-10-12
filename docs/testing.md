# Testing

Automated tests cover the software that is implemented. They do not verify a physical ESP32, TCS3200, load cell, or pumps.

## Frontend

From `frontend`:

```powershell
npm test
npm run typecheck
npm run lint
npm run build
```

Unit tests cover RGB conversion, recipe validation, dose totals, zero-target progress, missing telemetry, stale readings, connection labels, pump and fault rendering, and duplicate command clicks.

Playwright is configured in `playwright.config.ts` and `e2e/operator.spec.ts`. It needs the API on port 8080 and a Chromium install:

```powershell
npx playwright install chromium
npm run test:e2e
```

The browser scenario checks that start and software emergency stop are disabled when hardware is not verified, that a recipe can be saved, and that batch history opens. Run it against the default HTTP adapter, not as proof of hardware.

## Backend

From `backend`:

```powershell
mvn test
```

Unit tests cover recipe rules, batch transitions, telemetry parsing, and the HTTP adapter's refusal to send commands before integration is confirmed.

`ApiIntegrationTest` uses Testcontainers and PostgreSQL 16. It checks health, validation errors, recipe conflict, batch snapshot stability after a recipe edit, start acknowledgement without completion, idempotent replay, a simulated completion report with actual quantities, batch filtering, and a simulated communication error that does not invent RGB or weight.

If Docker is unavailable, Testcontainers skips that class. The skip is not a pass of the database tests. Run them again when Docker is available.

## Manual tests required with real hardware

These have not been done:

1. Confirm the contract in `docs/esp32-api-contract.md` and only then set both command flags.
2. With the device unplugged, confirm the dashboard stays disconnected and shows no RGB or weight.
3. With the device connected, confirm live RGB, weight, pump states, and firmware version match the device output, including a deliberately stale timestamp.
4. Disconnect the color sensor and load cell separately and confirm unavailable states rather than zeros.
5. Tare the scale and confirm the next reported weight, including whether zero is a real tare result.
6. Run a single pump test at the firmware's maximum and one above it. The firmware must reject the unsafe request even if software limits change.
7. Start a batch and confirm the dashboard stays out of `COMPLETED` until the machine says so. Edit the recipe during the batch and confirm history is unchanged.
8. Cancel a batch and confirm `CANCELLED` appears only after the machine reports it, with actual dispensed amounts if any were measured.
9. Time out a start request and confirm a second start is blocked.
10. Press the physical emergency stop with the dashboard closed and the network unplugged. Pumps must stop. Repeat with the software request and record whether the firmware acknowledges it. Neither test has been performed in this repository.
