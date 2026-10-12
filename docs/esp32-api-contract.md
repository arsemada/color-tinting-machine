# Provisional ESP32 API contract

This document is a proposal for the hardware developer. It is not a description of firmware that exists today. The backend HTTP adapter calls these paths only as configured, and it does not send pump, tare, calibration, cancel, or emergency-stop commands unless both `APP_MACHINE_COMMANDS_ENABLED` and `APP_MACHINE_INTEGRATION_CONFIRMED` are true.

Do not enable those flags against a device that has not implemented this contract.

## Transport

- The machine controller and the ESP32 should be on the same local network.
- The browser does not call the ESP32. The Spring Boot process does.
- Do not expose the ESP32 to the public internet.
- Proposed protocol: HTTP/JSON over the device base URL.
- Polling uses one telemetry GET. The default interval is 2 seconds and is configurable. Safe reads may retry once. Commands are never retried automatically.
- Default timeouts: 2 seconds connect, 3 seconds read.
- Timestamps are ISO-8601 UTC.
- Quantities are decimal strings in grams, with at most 3 decimal places. Missing values are JSON `null` or omitted. They must not be sent as `0` unless the scale or pump actually measured zero.

## Proposed device endpoints

| Method | Path | Purpose |
| --- | --- | --- |
| GET | `/api/v1/telemetry` | Status and measurements in one document |
| GET | `/api/v1/status` | Optional short status. The backend does not poll this by default |
| POST | `/api/v1/commands/batch/start` | Request a batch |
| POST | `/api/v1/commands/batch/cancel` | Request cancellation |
| POST | `/api/v1/commands/tare` | Request a load-cell tare |
| POST | `/api/v1/commands/sensor/calibrate` | Request color-sensor calibration |
| POST | `/api/v1/commands/pump/test` | Request one pump test |
| POST | `/api/v1/commands/emergency-stop` | Supplementary software stop request |

Paths are configurable under `app.machine.paths` so they can change without scattering literals through the services.

## Telemetry example

```json
{
  "deviceId": "esp32-01",
  "firmwareVersion": "0.0.0",
  "observedAt": "2026-10-12T18:00:00Z",
  "connectionState": "CONNECTED",
  "operatingState": "IDLE",
  "message": "",
  "activeBatchId": null,
  "reportedBatchId": null,
  "batchState": null,
  "actualTotalGrams": null,
  "actualByChannel": {},
  "color": {
    "available": false,
    "calibrationState": "UNCALIBRATED",
    "red": null,
    "green": null,
    "blue": null
  },
  "loadCell": {
    "available": false,
    "weightGrams": null,
    "tareState": "UNKNOWN"
  },
  "pumps": [
    { "channel": "CYAN", "state": "OFF", "dispensedGrams": null, "targetGrams": null, "fault": null }
  ],
  "faults": []
}
```

`operatingState`: `IDLE`, `STARTING`, `DISPENSING`, `PAUSED`, `CANCELLING`, `FAULT`, `UNKNOWN`.

`batchState`: `STARTING`, `DISPENSING`, `PAUSED`, `CANCELLING`, `COMPLETED`, `CANCELLED`, `FAILED`, `UNKNOWN`.

`connectionState` from the device may be replaced by the backend with `STALE` when `observedAt` is older than the configured threshold.

Pump `state`: `ON`, `OFF`, `STARTING`, `STOPPING`, `FAULT`, `UNKNOWN`.

Color `calibrationState`: `UNKNOWN`, `UNCALIBRATED`, `CALIBRATING`, `CALIBRATED`, `FAILED`.

Load `tareState`: `UNKNOWN`, `NOT_TARED`, `TARING`, `TARED`, `FAILED`.

RGB channels, when `color.available` is true, must be integers from 0 through 255. The backend rejects out-of-range values instead of displaying them. This contract does not define a TCS3200 frequency-to-RGB formula. The firmware must document the conversion it uses. The backend will not invent one.

When a sensor is unavailable, omit its readings. Do not send zeros.

`reportedBatchId` is required when a batch reaches a terminal state and is no longer the active batch. The backend will not mark a batch complete from an empty active-batch field.

## Command request

```json
{
  "commandId": "6d1f0b0e-8c0a-4e3e-9b8a-0d5e2f6a7b11",
  "type": "START_BATCH",
  "batchId": "2b6c1a4e-1d2f-4a55-9c1e-8f0a6d4b3c21",
  "channel": "",
  "quantityGrams": "",
  "durationMs": 0,
  "targetTotalGrams": "13.750"
}
```

The same `commandId` must be idempotent on the device. A repeated start with the same id must not dispense a second batch.

## Command response

```json
{
  "commandId": "6d1f0b0e-8c0a-4e3e-9b8a-0d5e2f6a7b11",
  "accepted": true,
  "state": "ACKNOWLEDGED",
  "message": "Start accepted. Dispensing has not finished."
}
```

`state` should be `ACKNOWLEDGED`, `EXECUTING`, `SUCCEEDED`, or `REJECTED`.

`accepted: true` with `SUCCEEDED` means the command itself finished, not that a batch is complete. Batch completion belongs in telemetry.

`accepted: false` is a rejection. Use HTTP 4xx for an invalid command and HTTP 5xx for a device fault if a body cannot be returned. The backend does not retry either case.

If the backend times out, it assumes the device might have received the command and blocks another start until telemetry identifies the batch outcome.

## Safety requirements the firmware must enforce

These are independent of the dashboard and database:

- A physical emergency stop must remove power or otherwise stop all pumps without Wi-Fi, the browser, or the backend.
- Firmware must reject unsafe doses, durations, and simultaneous pump tests even if the backend sends them.
- Firmware must refuse a new batch while one is active.
- Pump-test mode must run one pump at a time unless a later contract explicitly allows more.
- Calibration success must be a firmware decision. The backend will not calculate coefficients.
- Tare success must be a new valid weight report, not a display-side zero.

## Checklist for the hardware developer

Confirm or correct each item:

1. Base URL, port, and whether HTTP or another protocol will be used.
2. Whether telemetry and status are one document or two.
3. Exact paths and whether they match the table above.
4. Units for weight and dispensed volume or mass. This proposal uses grams.
5. The TCS3200-to-RGB conversion and what `CALIBRATED` means.
6. Load-cell interface, tare procedure, and the meaning of a reported `0`.
7. Whether batch pause exists.
8. How cancellation stops pumps and what state is reported if some colorant was already dispensed.
9. Whether `reportedBatchId` is sent after completion.
10. Command idempotency behavior for each POST.
11. Timeout budget the device can sustain at the chosen poll interval.
12. Emergency-stop behavior of the software command versus the physical stop.
13. Maximum safe dose per channel, total dose, pump-test quantity, and pump-test duration.
14. Firmware version field and fault code list.
15. Whether any command may run while a batch is dispensing.

Until those answers are written down, keep `APP_MACHINE_INTEGRATION_CONFIRMED=false`.
