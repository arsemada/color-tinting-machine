package com.colortinting.machine;

import com.colortinting.domain.ModelEnums.BatchState;
import com.colortinting.domain.ModelEnums.CalibrationState;
import com.colortinting.domain.ModelEnums.ColorChannel;
import com.colortinting.domain.ModelEnums.ConnectionState;
import com.colortinting.domain.ModelEnums.DataSource;
import com.colortinting.domain.ModelEnums.DeviceCallStatus;
import com.colortinting.domain.ModelEnums.OperatingState;
import com.colortinting.domain.ModelEnums.PumpState;
import com.colortinting.domain.ModelEnums.TareState;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record Observation(
        boolean success,
        ConnectionState connectionState,
        DataSource dataSource,
        boolean simulated,
        Instant observedAt,
        OperatingState operatingState,
        String firmwareVersion,
        ColorReading color,
        LoadReading load,
        List<PumpReading> pumps,
        UUID activeBatchId,
        UUID reportedBatchId,
        BatchState reportedBatchState,
        BigDecimal reportedActualTotal,
        Map<ColorChannel, BigDecimal> reportedActualByChannel,
        List<FaultSignal> faults,
        String message
) {
    public static Observation unavailable(ConnectionState connectionState, String message, List<FaultSignal> faults) {
        return new Observation(
                false,
                connectionState,
                DataSource.UNAVAILABLE,
                false,
                null,
                OperatingState.UNKNOWN,
                null,
                ColorReading.unavailable(),
                LoadReading.unavailable(),
                PumpReading.unknownPumps(),
                null,
                null,
                null,
                null,
                Map.of(),
                faults,
                message
        );
    }

    public record ColorReading(
            boolean available,
            CalibrationState calibrationState,
            Integer red,
            Integer green,
            Integer blue,
            Instant readAt
    ) {
        public static ColorReading unavailable() {
            return new ColorReading(false, CalibrationState.UNKNOWN, null, null, null, null);
        }
    }

    public record LoadReading(
            boolean available,
            BigDecimal weightGrams,
            TareState tareState,
            Instant readAt
    ) {
        public static LoadReading unavailable() {
            return new LoadReading(false, null, TareState.UNKNOWN, null);
        }
    }

    public record PumpReading(
            ColorChannel channel,
            PumpState state,
            BigDecimal dispensedGrams,
            BigDecimal targetGrams,
            String fault
    ) {
        public static List<PumpReading> unknownPumps() {
            return List.of(
                    unknown(ColorChannel.CYAN),
                    unknown(ColorChannel.MAGENTA),
                    unknown(ColorChannel.YELLOW),
                    unknown(ColorChannel.BLACK)
            );
        }

        private static PumpReading unknown(ColorChannel channel) {
            return new PumpReading(channel, PumpState.UNKNOWN, null, null, null);
        }
    }

    public record FaultSignal(String code, String message, String severity) {
    }

    public record DeviceCommand(
            UUID commandId,
            String type,
            UUID batchId,
            String recipeName,
            Map<ColorChannel, BigDecimal> targets,
            BigDecimal targetTotal,
            ColorChannel pumpChannel,
            BigDecimal pumpQuantity,
            Integer durationMs
    ) {
    }

    public record DeviceAck(DeviceCallStatus status, String message, boolean dispatched) {
        public static DeviceAck notSent(String message) {
            return new DeviceAck(DeviceCallStatus.NOT_SENT, message, false);
        }
    }
}
