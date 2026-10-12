package com.colortinting.api;

import com.colortinting.persistence.MachineEventRepository;
import com.colortinting.service.MachineObservationService;
import com.colortinting.service.SettingsService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class MachineController {
    private final MachineObservationService observations;
    private final SettingsService settings;
    private final MachineEventRepository events;
    private final ApiPresenter presenter;

    public MachineController(
            MachineObservationService observations,
            SettingsService settings,
            MachineEventRepository events,
            ApiPresenter presenter
    ) {
        this.observations = observations;
        this.settings = settings;
        this.events = events;
        this.presenter = presenter;
    }

    @GetMapping("/machine/status")
    public ApiPresenter.StatusResponse status() {
        return presenter.status(observations.picture(), settings.get().getDisplayName());
    }

    @GetMapping("/machine/telemetry")
    public ApiPresenter.TelemetryResponse telemetry() {
        return presenter.telemetry(observations.picture());
    }

    @PostMapping("/machine/refresh")
    public ApiPresenter.StatusResponse refresh() {
        observations.refresh();
        return presenter.status(observations.picture(), settings.get().getDisplayName());
    }

    @GetMapping("/events")
    public List<ApiPresenter.EventResponse> events() {
        return events.findTop50ByOrderByOccurredAtDesc().stream().map(presenter::event).toList();
    }

    @GetMapping("/settings")
    public ApiPresenter.SettingsResponse getSettings() {
        return presenter.settings(settings.get(), observations.picture().hardwareVerified());
    }

    @PutMapping("/settings")
    public ApiPresenter.SettingsResponse updateSettings(@Valid @RequestBody Requests.SettingsBody body) {
        return presenter.settings(
                settings.update(
                        body.displayName(),
                        body.telemetryRefreshIntervalMs(),
                        body.showReadingTimestamps(),
                        body.compactLayout()
                ),
                observations.picture().hardwareVerified()
        );
    }
}
