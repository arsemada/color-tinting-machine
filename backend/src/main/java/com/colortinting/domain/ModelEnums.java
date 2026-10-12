package com.colortinting.domain;

public final class ModelEnums {
    private ModelEnums() {
    }

    public enum ConnectionState {
        CONNECTED,
        CONNECTING,
        DISCONNECTED,
        STALE,
        COMMUNICATION_ERROR
    }

    public enum DataSource {
        LIVE,
        SIMULATED,
        UNAVAILABLE
    }

    public enum OperatingState {
        IDLE,
        STARTING,
        DISPENSING,
        PAUSED,
        CANCELLING,
        FAULT,
        UNKNOWN
    }

    public enum BatchState {
        STARTING,
        DISPENSING,
        PAUSED,
        CANCELLING,
        COMPLETED,
        CANCELLED,
        FAILED,
        UNKNOWN
    }

    public enum PumpState {
        ON,
        OFF,
        STARTING,
        STOPPING,
        FAULT,
        UNKNOWN
    }

    public enum ColorChannel {
        CYAN,
        MAGENTA,
        YELLOW,
        BLACK
    }

    public enum DoseUnit {
        GRAMS
    }

    public enum CommandType {
        START_BATCH,
        CANCEL_BATCH,
        TARE,
        CALIBRATE_SENSOR,
        PUMP_TEST,
        EMERGENCY_STOP
    }

    public enum CommandLifecycle {
        PENDING,
        ACKNOWLEDGED,
        EXECUTING,
        SUCCEEDED,
        REJECTED,
        TIMED_OUT,
        FAILED
    }

    public enum EventSeverity {
        INFO,
        WARNING,
        ERROR,
        CRITICAL
    }

    public enum CalibrationState {
        UNKNOWN,
        UNCALIBRATED,
        CALIBRATING,
        CALIBRATED,
        FAILED
    }

    public enum TareState {
        UNKNOWN,
        NOT_TARED,
        TARING,
        TARED,
        FAILED
    }

    public enum DeviceCallStatus {
        ACKNOWLEDGED,
        EXECUTING,
        SUCCEEDED,
        REJECTED,
        TIMED_OUT,
        FAILED,
        NOT_SENT
    }
}
