package com.colortinting.api;

import com.colortinting.domain.ModelEnums.ColorChannel;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public final class Requests {
    private Requests() {
    }

    public record RecipeBody(
            @NotBlank @Size(max = 120) String name,
            @Size(max = 2000) String description,
            @NotBlank String targetColor,
            @NotBlank String cyanDose,
            @NotBlank String magentaDose,
            @NotBlank String yellowDose,
            @NotBlank String blackDose,
            @NotBlank String doseUnit,
            String basePaintQuantity
    ) {
    }

    public record IdempotentBody(@NotBlank String idempotencyKey) {
    }

    public record StartBatchBody(@NotNull UUID recipeId, @NotBlank String idempotencyKey) {
    }

    public record PumpTestBody(
            @NotNull ColorChannel channel,
            @NotBlank String quantityGrams,
            @NotNull @Min(1) @Max(60000) Integer durationMs,
            @NotBlank String idempotencyKey
    ) {
    }

    public record ResolveUnknownBody(@NotBlank @Size(max = 500) String note) {
    }

    public record SettingsBody(
            @NotBlank @Size(max = 120) String displayName,
            @NotNull Integer telemetryRefreshIntervalMs,
            @NotNull Boolean showReadingTimestamps,
            @NotNull Boolean compactLayout
    ) {
    }

    public record ScenarioBody(@NotBlank String scenario) {
    }

    public record BatchReportBody(
            @NotBlank String state,
            String actualTotalGrams,
            String cyanActualGrams,
            String magentaActualGrams,
            String yellowActualGrams,
            String blackActualGrams
    ) {
    }
}
