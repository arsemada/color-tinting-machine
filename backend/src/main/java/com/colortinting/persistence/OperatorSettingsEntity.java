package com.colortinting.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "operator_settings")
public class OperatorSettingsEntity {
    @Id
    private Short id;

    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;

    @Column(name = "telemetry_refresh_interval_ms", nullable = false)
    private int telemetryRefreshIntervalMs;

    @Column(name = "show_reading_timestamps", nullable = false)
    private boolean showReadingTimestamps;

    @Column(name = "compact_layout", nullable = false)
    private boolean compactLayout;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Short getId() {
        return id;
    }

    public void setId(Short id) {
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public int getTelemetryRefreshIntervalMs() {
        return telemetryRefreshIntervalMs;
    }

    public void setTelemetryRefreshIntervalMs(int telemetryRefreshIntervalMs) {
        this.telemetryRefreshIntervalMs = telemetryRefreshIntervalMs;
    }

    public boolean isShowReadingTimestamps() {
        return showReadingTimestamps;
    }

    public void setShowReadingTimestamps(boolean showReadingTimestamps) {
        this.showReadingTimestamps = showReadingTimestamps;
    }

    public boolean isCompactLayout() {
        return compactLayout;
    }

    public void setCompactLayout(boolean compactLayout) {
        this.compactLayout = compactLayout;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
