package com.colortinting.api;

import com.colortinting.machine.Observation;
import com.colortinting.service.MachineObservationService;
import com.colortinting.service.SettingsService;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {
    private final JdbcTemplate jdbc;
    private final MachineObservationService observations;
    private final SettingsService settings;
    private final ApiPresenter presenter;

    public HealthController(
            JdbcTemplate jdbc,
            MachineObservationService observations,
            SettingsService settings,
            ApiPresenter presenter
    ) {
        this.jdbc = jdbc;
        this.observations = observations;
        this.settings = settings;
        this.presenter = presenter;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        boolean databaseUp = true;
        try {
            jdbc.queryForObject("select 1", Integer.class);
        } catch (RuntimeException ex) {
            databaseUp = false;
        }
        MachineObservationService.MachinePicture picture = observations.picture();
        ApiPresenter.StatusResponse status = presenter.status(picture, settings.get().getDisplayName());
        String overall = databaseUp ? "UP" : "DOWN";
        return ResponseEntity.status(databaseUp ? 200 : 503).body(Map.of(
                "status", overall,
                "timestamp", Instant.now().toString(),
                "database", databaseUp ? "UP" : "DOWN",
                "machineConnection", status.connectionState(),
                "machineDataSource", status.dataSource(),
                "hardwareVerified", status.hardwareVerified(),
                "simulated", status.simulated()
        ));
    }
}
