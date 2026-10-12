package com.colortinting.machine;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Provisional ESP32 telemetry document. Field names are not a confirmed firmware contract.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Esp32TelemetryPayload(
        String deviceId,
        String firmwareVersion,
        Instant observedAt,
        String connectionState,
        String operatingState,
        String message,
        String activeBatchId,
        String reportedBatchId,
        String batchState,
        String actualTotalGrams,
        Map<String, String> actualByChannel,
        ColorPayload color,
        LoadPayload loadCell,
        List<PumpPayload> pumps,
        List<FaultPayload> faults
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ColorPayload(
            boolean available,
            String calibrationState,
            Integer red,
            Integer green,
            Integer blue
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LoadPayload(
            boolean available,
            String weightGrams,
            String tareState
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PumpPayload(
            String channel,
            String state,
            String dispensedGrams,
            String targetGrams,
            String fault
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FaultPayload(String code, String message, String severity) {
    }
}
