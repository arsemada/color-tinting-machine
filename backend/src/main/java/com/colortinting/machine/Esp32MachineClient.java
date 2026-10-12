package com.colortinting.machine;

public interface Esp32MachineClient {
    Observation probe();

    Observation.DeviceAck dispatch(Observation.DeviceCommand command);

    boolean simulated();

    String adapterName();
}
