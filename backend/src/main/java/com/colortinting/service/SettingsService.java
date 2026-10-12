package com.colortinting.service;

import com.colortinting.api.ApiException;
import com.colortinting.config.AppProperties;
import com.colortinting.domain.Quantities;
import com.colortinting.persistence.OperatorSettingsEntity;
import com.colortinting.persistence.OperatorSettingsRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

@Service
public class SettingsService {
    private final OperatorSettingsRepository settings;
    private final AppProperties properties;
    private final Clock clock;

    public SettingsService(OperatorSettingsRepository settings, AppProperties properties, Clock clock) {
        this.settings = settings;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public OperatorSettingsEntity get() {
        return settings.findById((short) 1).orElseThrow(() -> new ApiException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "SETTINGS_MISSING",
                "Operator settings were not initialized."
        ));
    }

    @Transactional
    public OperatorSettingsEntity update(String displayName, int refreshIntervalMs, boolean showTimestamps, boolean compact) {
        List<Quantities.FieldViolation> errors = new java.util.ArrayList<>();
        if (displayName == null || displayName.isBlank()) {
            errors.add(new Quantities.FieldViolation("displayName", "Display name is required."));
        } else if (displayName.trim().length() > 120) {
            errors.add(new Quantities.FieldViolation("displayName", "Display name must be 120 characters or fewer."));
        }
        if (refreshIntervalMs < 1000 || refreshIntervalMs > 30000) {
            errors.add(new Quantities.FieldViolation(
                    "telemetryRefreshIntervalMs",
                    "Refresh interval must be between 1000 and 30000 milliseconds."
            ));
        }
        if (!errors.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Settings are invalid.", errors);
        }
        OperatorSettingsEntity entity = get();
        entity.setDisplayName(displayName.trim());
        entity.setTelemetryRefreshIntervalMs(refreshIntervalMs);
        entity.setShowReadingTimestamps(showTimestamps);
        entity.setCompactLayout(compact);
        entity.setUpdatedAt(clock.instant());
        return settings.save(entity);
    }

    public AppProperties.Machine machine() {
        return properties.getMachine();
    }
}
