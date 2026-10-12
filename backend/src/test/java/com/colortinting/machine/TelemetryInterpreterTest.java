package com.colortinting.machine;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TelemetryInterpreterTest {
    @Test
    void doesNotConvertMissingWeightOrRgbIntoZero() {
        Esp32TelemetryPayload payload = new Esp32TelemetryPayload(
                "device",
                "0",
                Instant.parse("2026-10-12T12:00:00Z"),
                "CONNECTED",
                "IDLE",
                null,
                null,
                null,
                null,
                null,
                null,
                new Esp32TelemetryPayload.ColorPayload(false, "UNCALIBRATED", null, null, null),
                new Esp32TelemetryPayload.LoadPayload(false, null, "UNKNOWN"),
                List.of(),
                List.of()
        );
        Observation observation = TelemetryInterpreter.interpret(payload, false, Instant.parse("2026-10-12T12:00:01Z"));
        assertFalse(observation.color().available());
        assertNull(observation.color().red());
        assertFalse(observation.load().available());
        assertNull(observation.load().weightGrams());
        assertEquals(ModelEnumsPump.UNKNOWN, observation.pumps().getFirst().state().name());
    }

    @Test
    void rejectsRgbOutsideTheByteRange() {
        Esp32TelemetryPayload payload = new Esp32TelemetryPayload(
                null,
                null,
                null,
                "CONNECTED",
                "IDLE",
                null,
                null,
                null,
                null,
                null,
                null,
                new Esp32TelemetryPayload.ColorPayload(true, "CALIBRATED", 999, 0, 0),
                new Esp32TelemetryPayload.LoadPayload(true, "-1", "TARED"),
                List.of(new Esp32TelemetryPayload.PumpPayload("CYAN", "BANANA", null, null, null)),
                List.of()
        );
        Observation observation = TelemetryInterpreter.interpret(payload, false, Instant.parse("2026-10-12T12:00:00Z"));
        assertFalse(observation.color().available());
        assertNull(observation.color().red());
        assertFalse(observation.load().available());
        assertNull(observation.load().weightGrams());
        assertEquals("UNKNOWN", observation.pumps().getFirst().state().name());
        assertTrue(observation.faults().stream().anyMatch(fault -> fault.code().equals("MALFORMED_TELEMETRY")));
    }

    private static final class ModelEnumsPump {
        private static final String UNKNOWN = "UNKNOWN";
    }
}
