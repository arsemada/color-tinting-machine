package com.colortinting.api;

import com.colortinting.api.ApiException;
import com.colortinting.domain.ModelEnums.BatchState;
import com.colortinting.domain.ModelEnums.ColorChannel;
import com.colortinting.domain.Quantities;
import com.colortinting.machine.SimulatedEsp32MachineClient;
import com.colortinting.service.MachineObservationService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Test fixture controls. This controller is absent unless the simulated adapter is selected.
 */
@RestController
@RequestMapping("/api/v1/machine/fixture")
@ConditionalOnProperty(name = "app.machine.adapter", havingValue = "simulated")
public class SimulatedFixtureController {
    private final SimulatedEsp32MachineClient fixture;
    private final MachineObservationService observations;

    public SimulatedFixtureController(SimulatedEsp32MachineClient fixture, MachineObservationService observations) {
        this.fixture = fixture;
        this.observations = observations;
    }

    @PostMapping("/scenario")
    public Map<String, String> scenario(@RequestBody Requests.ScenarioBody body) {
        try {
            fixture.setScenario(SimulatedEsp32MachineClient.Scenario.valueOf(body.scenario().trim()));
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Unknown simulated scenario.");
        }
        observations.refresh();
        return Map.of(
                "scenario", fixture.scenario().name(),
                "simulated", "true",
                "note", "This fixture is not an ESP32."
        );
    }

    @PostMapping("/batch-report")
    public Map<String, String> report(@RequestBody Requests.BatchReportBody body) {
        BatchState state;
        try {
            state = BatchState.valueOf(body.state().trim());
        } catch (RuntimeException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Unknown batch state.");
        }
        List<Quantities.FieldViolation> errors = new java.util.ArrayList<>();
        BigDecimal total = Quantities.parseOptional(body.actualTotalGrams(), "actualTotalGrams", errors);
        Map<ColorChannel, BigDecimal> actuals = new EnumMap<>(ColorChannel.class);
        putActual(actuals, ColorChannel.CYAN, body.cyanActualGrams(), errors);
        putActual(actuals, ColorChannel.MAGENTA, body.magentaActualGrams(), errors);
        putActual(actuals, ColorChannel.YELLOW, body.yellowActualGrams(), errors);
        putActual(actuals, ColorChannel.BLACK, body.blackActualGrams(), errors);
        if (!errors.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "The fixture report is invalid.", errors);
        }
        fixture.reportBatch(state, total, actuals);
        observations.refresh();
        return Map.of("reported", "true", "simulated", "true", "state", state.name());
    }

    private static void putActual(
            Map<ColorChannel, BigDecimal> actuals,
            ColorChannel channel,
            String raw,
            List<Quantities.FieldViolation> errors
    ) {
        BigDecimal value = Quantities.parseOptional(raw, channel.name().toLowerCase() + "ActualGrams", errors);
        if (value != null) {
            actuals.put(channel, value);
        }
    }
}
