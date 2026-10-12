# Safety and fault handling

The dashboard is not a safety system. A physical emergency stop, and firmware that can stop every pump without Wi-Fi, the browser, the backend, or the database, are mandatory before the machine dispenses colorant. The software emergency-stop button only sends a request when hardware integration is verified. It never reports that pumps have stopped unless the machine says so. If the device cannot be reached, the button stays disabled and the screen tells the operator to use the physical stop.

## What the software will not do

- It will not show simulated readings as live readings. The simulated adapter is labeled, and `hardwareVerified` remains false.
- It will not turn a missing weight or RGB value into zero.
- It will not mark a batch `COMPLETED` because a timer elapsed or a start command returned.
- It will not retry a start, cancel, tare, calibration, pump test, or emergency-stop command after a timeout.
- It will not let display settings raise firmware limits. The limits shown in Settings are server configuration for request validation only.
- It will not expose database passwords, stack traces, or the ESP32 address in API error bodies.

## Faults the interface is built to show

Sensor unavailable, load-cell faults, ESP32 timeouts, pump faults, invalid recipes, rejected commands, cancellation, emergency-stop requests, calibration failure, and malformed telemetry each keep their own code and message. Recent events are stored in `machine_events`. Ordinary telemetry polls are not stored.

Malformed telemetry marks the bad section unavailable and adds a `MALFORMED_TELEMETRY` fault. An unknown pump state becomes `UNKNOWN` rather than `OFF`.

## Duplicate and uncertain commands

Each command carries an idempotency key. Repeating the same key returns the original command and does not dispatch again. A second key is rejected while a batch is active or while a previous command still needs reconciliation.

After a timeout, the operator can record the outcome as failed. That action is explicit, stores the note, and does not invent dispensed quantities or a successful batch.

## Security limitations of this prototype

There is no login. Anyone who can reach the API can read recipes and, if the integration flags are turned on, request machine commands. Keep the API and the ESP32 on the machine network. Do not publish them. Add authentication before a multi-user or remote deployment.

CORS allows only the configured dashboard origins. Database access uses JPA bind parameters. Secrets belong in environment variables, and the example files contain only local development values.
