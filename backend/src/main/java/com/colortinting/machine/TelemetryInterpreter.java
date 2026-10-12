package com.colortinting.machine;

import com.colortinting.domain.ModelEnums.BatchState;
import com.colortinting.domain.ModelEnums.CalibrationState;
import com.colortinting.domain.ModelEnums.ColorChannel;
import com.colortinting.domain.ModelEnums.ConnectionState;
import com.colortinting.domain.ModelEnums.DataSource;
import com.colortinting.domain.ModelEnums.OperatingState;
import com.colortinting.domain.ModelEnums.PumpState;
import com.colortinting.domain.ModelEnums.TareState;
import com.colortinting.domain.Quantities;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Interprets a provisional telemetry document. Invalid sections become unavailable
 * rather than being coerced to zero or to a guessed color.
 */
public final class TelemetryInterpreter {
    private TelemetryInterpreter() {
    }

    public static Observation interpret(Esp32TelemetryPayload payload, boolean simulated, Instant receivedAt) {
        if (payload == null) {
            return Observation.unavailable(
                    ConnectionState.COMMUNICATION_ERROR,
                    "The device response was empty.",
                    List.of(new Observation.FaultSignal("MALFORMED_TELEMETRY", "Empty telemetry document.", "ERROR"))
            );
        }
        List<Observation.FaultSignal> faults = new ArrayList<>();
        if (payload.faults() != null) {
            for (Esp32TelemetryPayload.FaultPayload fault : payload.faults()) {
                if (fault == null || fault.code() == null || fault.message() == null) {
                    faults.add(new Observation.FaultSignal("MALFORMED_TELEMETRY", "A fault entry was incomplete.", "ERROR"));
                } else {
                    faults.add(new Observation.FaultSignal(fault.code(), fault.message(), normalizeSeverity(fault.severity())));
                }
            }
        }

        Instant observedAt = payload.observedAt() == null ? receivedAt : payload.observedAt();
        Observation.ColorReading color = interpretColor(payload.color(), observedAt, faults);
        Observation.LoadReading load = interpretLoad(payload.loadCell(), observedAt, faults);
        List<Observation.PumpReading> pumps = interpretPumps(payload.pumps(), faults);
        UUID activeBatchId = parseUuid(payload.activeBatchId(), "activeBatchId", faults);
        UUID reportedBatchId = parseUuid(payload.reportedBatchId(), "reportedBatchId", faults);
        if (reportedBatchId == null) {
            reportedBatchId = activeBatchId;
        }
        BatchState reportedBatchState = parseEnum(BatchState.class, payload.batchState(), true);
        if (payload.batchState() != null && reportedBatchState == null) {
            faults.add(new Observation.FaultSignal("MALFORMED_TELEMETRY", "Unrecognised batch state.", "ERROR"));
        }
        OperatingState operatingState = parseEnum(OperatingState.class, payload.operatingState(), true);
        if (operatingState == null) {
            operatingState = OperatingState.UNKNOWN;
            if (payload.operatingState() != null) {
                faults.add(new Observation.FaultSignal("MALFORMED_TELEMETRY", "Unrecognised operating state.", "WARNING"));
            }
        }
        ConnectionState connectionState = parseEnum(ConnectionState.class, payload.connectionState(), true);
        if (connectionState == null) {
            connectionState = ConnectionState.CONNECTED;
        }

        BigDecimal actualTotal = parseQuantity(payload.actualTotalGrams(), "actualTotalGrams", faults);
        Map<ColorChannel, BigDecimal> actuals = new EnumMap<>(ColorChannel.class);
        if (payload.actualByChannel() != null) {
            payload.actualByChannel().forEach((key, value) -> {
                ColorChannel channel = parseEnum(ColorChannel.class, key, false);
                BigDecimal quantity = parseQuantity(value, "actualByChannel." + key, faults);
                if (channel != null && quantity != null) {
                    actuals.put(channel, quantity);
                }
            });
        }

        DataSource source = simulated ? DataSource.SIMULATED : DataSource.LIVE;
        return new Observation(
                true,
                connectionState,
                source,
                simulated,
                observedAt,
                operatingState,
                payload.firmwareVersion(),
                color,
                load,
                pumps,
                activeBatchId,
                reportedBatchId,
                reportedBatchState,
                actualTotal,
                actuals,
                List.copyOf(faults),
                payload.message() == null ? "" : payload.message()
        );
    }

    private static Observation.ColorReading interpretColor(
            Esp32TelemetryPayload.ColorPayload color,
            Instant observedAt,
            List<Observation.FaultSignal> faults
    ) {
        if (color == null || !color.available()) {
            if (color != null && (color.red() != null || color.green() != null || color.blue() != null)) {
                faults.add(new Observation.FaultSignal(
                        "MALFORMED_TELEMETRY",
                        "Color readings were present while the sensor was marked unavailable.",
                        "WARNING"
                ));
            }
            CalibrationState calibration = color == null
                    ? CalibrationState.UNKNOWN
                    : parseEnum(CalibrationState.class, color.calibrationState(), true);
            if (calibration == null) {
                calibration = CalibrationState.UNKNOWN;
            }
            return new Observation.ColorReading(false, calibration, null, null, null, null);
        }
        if (!validChannel(color.red()) || !validChannel(color.green()) || !validChannel(color.blue())) {
            faults.add(new Observation.FaultSignal(
                    "MALFORMED_TELEMETRY",
                    "RGB readings were missing or outside 0–255.",
                    "ERROR"
            ));
            return Observation.ColorReading.unavailable();
        }
        CalibrationState calibration = parseEnum(CalibrationState.class, color.calibrationState(), true);
        if (calibration == null) {
            calibration = CalibrationState.UNKNOWN;
        }
        return new Observation.ColorReading(true, calibration, color.red(), color.green(), color.blue(), observedAt);
    }

    private static Observation.LoadReading interpretLoad(
            Esp32TelemetryPayload.LoadPayload load,
            Instant observedAt,
            List<Observation.FaultSignal> faults
    ) {
        if (load == null || !load.available()) {
            if (load != null && load.weightGrams() != null) {
                faults.add(new Observation.FaultSignal(
                        "MALFORMED_TELEMETRY",
                        "A weight was present while the load cell was marked unavailable.",
                        "WARNING"
                ));
            }
            return Observation.LoadReading.unavailable();
        }
        BigDecimal weight = parseQuantity(load.weightGrams(), "weightGrams", faults);
        if (weight == null) {
            faults.add(new Observation.FaultSignal(
                    "MALFORMED_TELEMETRY",
                    "Load cell was available but the weight was missing or invalid.",
                    "ERROR"
            ));
            return Observation.LoadReading.unavailable();
        }
        TareState tare = parseEnum(TareState.class, load.tareState(), true);
        if (tare == null) {
            tare = TareState.UNKNOWN;
        }
        return new Observation.LoadReading(true, weight, tare, observedAt);
    }

    private static List<Observation.PumpReading> interpretPumps(
            List<Esp32TelemetryPayload.PumpPayload> pumps,
            List<Observation.FaultSignal> faults
    ) {
        Map<ColorChannel, Observation.PumpReading> byChannel = new EnumMap<>(ColorChannel.class);
        if (pumps != null) {
            for (Esp32TelemetryPayload.PumpPayload pump : pumps) {
                if (pump == null) {
                    continue;
                }
                ColorChannel channel = parseEnum(ColorChannel.class, pump.channel(), false);
                if (channel == null) {
                    faults.add(new Observation.FaultSignal("MALFORMED_TELEMETRY", "Unrecognised pump channel.", "ERROR"));
                    continue;
                }
                PumpState state = parseEnum(PumpState.class, pump.state(), true);
                if (state == null) {
                    state = PumpState.UNKNOWN;
                    faults.add(new Observation.FaultSignal(
                            "MALFORMED_TELEMETRY",
                            "Unrecognised state for pump " + channel + ".",
                            "WARNING"
                    ));
                }
                BigDecimal dispensed = parseQuantity(pump.dispensedGrams(), channel.name() + ".dispensedGrams", faults);
                BigDecimal target = parseQuantity(pump.targetGrams(), channel.name() + ".targetGrams", faults);
                byChannel.put(channel, new Observation.PumpReading(channel, state, dispensed, target, pump.fault()));
            }
        }
        List<Observation.PumpReading> result = new ArrayList<>();
        for (ColorChannel channel : ColorChannel.values()) {
            result.add(byChannel.getOrDefault(channel, new Observation.PumpReading(channel, PumpState.UNKNOWN, null, null, null)));
        }
        return List.copyOf(result);
    }

    private static boolean validChannel(Integer value) {
        return value != null && value >= 0 && value <= 255;
    }

    private static BigDecimal parseQuantity(String raw, String field, List<Observation.FaultSignal> faults) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        List<Quantities.FieldViolation> errors = new ArrayList<>();
        BigDecimal parsed = Quantities.parseRequired(raw, field, errors);
        if (!errors.isEmpty() || parsed == null) {
            faults.add(new Observation.FaultSignal("MALFORMED_TELEMETRY", "Invalid quantity for " + field + ".", "ERROR"));
            return null;
        }
        return parsed;
    }

    private static UUID parseUuid(String raw, String field, List<Observation.FaultSignal> faults) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            faults.add(new Observation.FaultSignal("MALFORMED_TELEMETRY", "Invalid UUID for " + field + ".", "ERROR"));
            return null;
        }
    }

    private static String normalizeSeverity(String severity) {
        if (severity == null) {
            return "WARNING";
        }
        String upper = severity.toUpperCase(Locale.ROOT);
        return switch (upper) {
            case "INFO", "WARNING", "ERROR", "CRITICAL" -> upper;
            default -> "WARNING";
        };
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String raw, boolean allowBlankAsNull) {
        if (raw == null || raw.isBlank()) {
            return allowBlankAsNull ? null : null;
        }
        try {
            return Enum.valueOf(type, raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
