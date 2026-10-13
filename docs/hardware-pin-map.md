# Hardware pin map

Received from the hardware developer on 13 October 2026. This is their wiring plan. It has not been checked against a built machine, and it is not an HTTP API.

The developer’s source note refers to `HARDWARE_WIRING_PINMAP.md`. This file is the copy kept with the software.

Commands stay disabled. `APP_MACHINE_INTEGRATION_CONFIRMED` remains `false` until the firmware API in `docs/esp32-api-contract.md` is confirmed.

## Pins

All of these assignments are the developer’s. They stated that the pins avoid ESP32 boot-strapping conflicts and that input-only pins are reserved for sensor feedback.

| Peripheral | Signal | ESP32 GPIO | Direction | Their electrical note |
| --- | --- | --- | --- | --- |
| Pump 1, Cyan | Relay IN 1 | 25 | Output | Active-low optocoupled relay. Driven high on boot. |
| Pump 2, Magenta | Relay IN 2 | 26 | Output | Active-low optocoupled relay. Driven high on boot. |
| Pump 3, Yellow | Relay IN 3 | 27 | Output | Active-low optocoupled relay. Driven high on boot. |
| Pump 4, Key/Base | Relay IN 4 | 33 | Output | Active-low optocoupled relay. Driven high on boot. |
| TCS3200 | S0, S1 | 18, 19 | Output | Frequency scaling set to 20%. |
| TCS3200 | S2, S3 | 21, 22 | Output | Photodiode filter selection: red, green, blue, clear. |
| TCS3200 | OUT | 35 | Input | Frequency output on an input-only pin. |
| TCS3200 | OE, LED | 5, 4 | Output | Output enable is active low. LED is the illumination control. |
| HX711 | DOUT | 34 | Input | 24-bit load-cell data, with timeout guards. |
| HX711 | SCK | 32 | Output | Serial clock. |
| Physical emergency stop | Switch | 39 | Input | External pull-up. Active low. |

## Power

Their caution, recorded here unchanged in meaning:

- Do not power the 12 V pumps from the ESP32 3.3 V or 5 V rail.
- Use a separate regulated 12 V, 5 A supply with an in-line 5 A fuse.
- Put a 1N4007 or 1N5819 flyback diode across each pump motor, reverse-biased, to absorb the switch-off spike.

## What this confirms

- The scale interface they plan to use is an HX711, not an unspecified weighing chip.
- The color sensor is a TCS3200, read as a frequency on GPIO 35. Scaling is 20%. Filters are selected with S2 and S3.
- Pump relays are active low, so a high output is the off state. They intend to drive the outputs high during boot.
- A physical emergency-stop switch is wired as an input on GPIO 39.

## What this does not confirm

- No base URL, HTTP paths, JSON fields, or command acknowledgements.
- No TCS3200 frequency-to-RGB conversion, and no definition of calibrated.
- No tare procedure, and no statement of whether a reported weight of 0 g is a real measurement.
- No dose limits, pump timing, or dispensed-quantity measurement.
- Pump 4 is labeled **Key/Base**. The software channel is still **Black**. Key can mean black in CMYK, or this pump may dispense base paint. That name must be confirmed before the channel is renamed.
- GPIO 39 shows that firmware can read the stop button. The diagram does not show the button cutting the 12 V pump supply by itself. Whether the physical stop halts the pumps with the ESP32, Wi-Fi, and this software all off is still untested.

Until those points are answered, the dashboard keeps reporting the machine as unverified and leaves the controls locked.
