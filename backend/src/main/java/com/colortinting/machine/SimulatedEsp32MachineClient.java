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
import com.colortinting.domain.Quantities;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * In-process fixture for automated tests and explicitly labeled development use.
 * It is not an ESP32 and must not be presented as live hardware.
 */
public class SimulatedEsp32MachineClient implements Esp32MachineClient {
    public enum Scenario {
        NOMINAL,
        COMMUNICATION_ERROR,
        SENSOR_UNAVAILABLE,
        LOAD_CELL_FAULT,
        PUMP_FAULT
    }

    private final Clock clock;
    private final Object monitor = new Object();
    private Scenario scenario = Scenario.NOMINAL;
    private OperatingState operatingState = OperatingState.IDLE;
    private CalibrationState calibrationState = CalibrationState.UNCALIBRATED;
    private TareState tareState = TareState.NOT_TARED;
    private BigDecimal weightGrams = new BigDecimal("250.000");
    private UUID activeBatchId;
    private UUID reportedBatchId;
    private BatchState reportedBatchState;
    private BigDecimal reportedActualTotal;
    private final Map<ColorChannel, BigDecimal> reportedActuals = new EnumMap<>(ColorChannel.class);
    private boolean emergencyRequested;

    public SimulatedEsp32MachineClient(Clock clock) {
        this.clock = clock;
    }

    public void setScenario(Scenario scenario) {
        synchronized (monitor) {
            this.scenario = scenario;
        }
    }

    public Scenario scenario() {
        synchronized (monitor) {
            return scenario;
        }
    }

    public void reportBatch(BatchState state, BigDecimal actualTotal, Map<ColorChannel, BigDecimal> actuals) {
        synchronized (monitor) {
            this.reportedBatchId = this.activeBatchId;
            this.reportedBatchState = state;
            this.reportedActualTotal = actualTotal;
            this.reportedActuals.clear();
            if (actuals != null) {
                this.reportedActuals.putAll(actuals);
            }
            if (state == BatchState.COMPLETED || state == BatchState.CANCELLED || state == BatchState.FAILED) {
                this.operatingState = OperatingState.IDLE;
                this.activeBatchId = null;
            } else if (state == BatchState.DISPENSING) {
                this.operatingState = OperatingState.DISPENSING;
            } else if (state == BatchState.PAUSED) {
                this.operatingState = OperatingState.PAUSED;
            } else if (state == BatchState.CANCELLING) {
                this.operatingState = OperatingState.CANCELLING;
            } else if (state == BatchState.STARTING) {
                this.operatingState = OperatingState.STARTING;
            }
        }
    }

    @Override
    public Observation probe() {
        synchronized (monitor) {
            if (scenario == Scenario.COMMUNICATION_ERROR) {
                return Observation.unavailable(
                        ConnectionState.COMMUNICATION_ERROR,
                        "Simulated communication error. No readings are available.",
                        List.of(new Observation.FaultSignal(
                                "ESP32_COMMUNICATION",
                                "Simulated fixture is reporting a communication error.",
                                "ERROR"
                        ))
                );
            }
            Instant now = clock.instant();
            boolean sensorAvailable = scenario != Scenario.SENSOR_UNAVAILABLE;
            boolean loadAvailable = scenario != Scenario.LOAD_CELL_FAULT;
            List<Observation.FaultSignal> faults = new java.util.ArrayList<>();
            if (!sensorAvailable) {
                faults.add(new Observation.FaultSignal("COLOR_SENSOR_UNAVAILABLE", "Simulated color sensor is unavailable.", "ERROR"));
            }
            if (!loadAvailable) {
                faults.add(new Observation.FaultSignal("LOAD_CELL_FAULT", "Simulated load cell fault.", "ERROR"));
            }
            if (scenario == Scenario.PUMP_FAULT) {
                faults.add(new Observation.FaultSignal("PUMP_FAULT", "Simulated cyan pump fault.", "ERROR"));
            }
            if (emergencyRequested) {
                faults.add(new Observation.FaultSignal(
                        "EMERGENCY_STOP_REQUESTED",
                        "Simulated emergency-stop request was acknowledged. Pump stop is not confirmed.",
                        "CRITICAL"
                ));
            }
            Observation.ColorReading color = sensorAvailable
                    ? new Observation.ColorReading(true, calibrationState, 32, 128, 148, now)
                    : new Observation.ColorReading(false, calibrationState, null, null, null, null);
            Observation.LoadReading load = loadAvailable
                    ? new Observation.LoadReading(true, weightGrams, tareState, now)
                    : Observation.LoadReading.unavailable();
            return new Observation(
                    true,
                    ConnectionState.CONNECTED,
                    DataSource.SIMULATED,
                    true,
                    now,
                    operatingState,
                    "simulated-fixture",
                    color,
                    load,
                    pumps(),
                    activeBatchId,
                    reportedBatchId,
                    reportedBatchState,
                    reportedActualTotal,
                    Map.copyOf(reportedActuals),
                    List.copyOf(faults),
                    "Simulated fixture. These values are not readings from an ESP32."
            );
        }
    }

    @Override
    public Observation.DeviceAck dispatch(Observation.DeviceCommand command) {
        synchronized (monitor) {
            if (scenario == Scenario.COMMUNICATION_ERROR) {
                return new Observation.DeviceAck(
                        DeviceCallStatus.FAILED,
                        "Simulated communication error. The command was not applied.",
                        false
                );
            }
            return switch (command.type()) {
                case "START_BATCH" -> start(command);
                case "CANCEL_BATCH" -> cancel(command);
                case "TARE" -> tare();
                case "CALIBRATE_SENSOR" -> calibrate();
                case "PUMP_TEST" -> pumpTest(command);
                case "EMERGENCY_STOP" -> emergencyStop();
                default -> new Observation.DeviceAck(DeviceCallStatus.REJECTED, "Unsupported simulated command.", false);
            };
        }
    }

    private Observation.DeviceAck start(Observation.DeviceCommand command) {
        if (activeBatchId != null) {
            return new Observation.DeviceAck(DeviceCallStatus.REJECTED, "Simulated fixture already has an active batch.", true);
        }
        activeBatchId = command.batchId();
        reportedBatchId = command.batchId();
        reportedBatchState = BatchState.STARTING;
        operatingState = OperatingState.STARTING;
        reportedActualTotal = null;
        reportedActuals.clear();
        return new Observation.DeviceAck(
                DeviceCallStatus.ACKNOWLEDGED,
                "Simulated fixture acknowledged the start request. The batch is not complete.",
                true
        );
    }

    private Observation.DeviceAck cancel(Observation.DeviceCommand command) {
        if (activeBatchId == null || !activeBatchId.equals(command.batchId())) {
            return new Observation.DeviceAck(DeviceCallStatus.REJECTED, "Simulated fixture has no matching active batch.", true);
        }
        reportedBatchState = BatchState.CANCELLING;
        operatingState = OperatingState.CANCELLING;
        return new Observation.DeviceAck(
                DeviceCallStatus.ACKNOWLEDGED,
                "Simulated cancellation was acknowledged. The batch is not cancelled until the fixture reports CANCELLED.",
                true
        );
    }

    private Observation.DeviceAck tare() {
        tareState = TareState.TARED;
        weightGrams = Quantities.zero();
        return new Observation.DeviceAck(
                DeviceCallStatus.ACKNOWLEDGED,
                "Simulated tare was applied inside the fixture. This is not a load-cell confirmation.",
                true
        );
    }

    private Observation.DeviceAck calibrate() {
        calibrationState = CalibrationState.CALIBRATING;
        return new Observation.DeviceAck(
                DeviceCallStatus.ACKNOWLEDGED,
                "Simulated calibration was acknowledged. Success has not been reported.",
                true
        );
    }

    private Observation.DeviceAck pumpTest(Observation.DeviceCommand command) {
        if (activeBatchId != null) {
            return new Observation.DeviceAck(DeviceCallStatus.REJECTED, "Simulated pump test rejected while a batch is active.", true);
        }
        return new Observation.DeviceAck(
                DeviceCallStatus.ACKNOWLEDGED,
                "Simulated pump test for " + command.pumpChannel() + " was acknowledged. No physical pump ran.",
                true
        );
    }

    private Observation.DeviceAck emergencyStop() {
        emergencyRequested = true;
        operatingState = OperatingState.FAULT;
        return new Observation.DeviceAck(
                DeviceCallStatus.ACKNOWLEDGED,
                "Simulated emergency stop was acknowledged. This does not confirm that pumps have stopped.",
                true
        );
    }

    private List<Observation.PumpReading> pumps() {
        return List.of(
                pump(ColorChannel.CYAN, scenario == Scenario.PUMP_FAULT),
                pump(ColorChannel.MAGENTA, false),
                pump(ColorChannel.YELLOW, false),
                pump(ColorChannel.BLACK, false)
        );
    }

    private Observation.PumpReading pump(ColorChannel channel, boolean fault) {
        if (fault) {
            return new Observation.PumpReading(channel, PumpState.FAULT, null, null, "Simulated pump fault");
        }
        if (emergencyRequested) {
            return new Observation.PumpReading(channel, PumpState.STOPPING, null, null, null);
        }
        PumpState state = operatingState == OperatingState.DISPENSING ? PumpState.ON : PumpState.OFF;
        if (operatingState == OperatingState.STARTING) {
            state = PumpState.STARTING;
        }
        BigDecimal dispensed = reportedActuals.get(channel);
        return new Observation.PumpReading(channel, state, dispensed, null, null);
    }

    @Override
    public boolean simulated() {
        return true;
    }

    @Override
    public String adapterName() {
        return "simulated";
    }
}
