package com.colortinting.api;

import com.colortinting.config.AppProperties;
import com.colortinting.domain.ModelEnums.ColorChannel;
import com.colortinting.domain.ModelEnums.CommandLifecycle;
import com.colortinting.domain.Quantities;
import com.colortinting.machine.Observation;
import com.colortinting.persistence.BatchEntity;
import com.colortinting.persistence.BatchPumpResultEntity;
import com.colortinting.persistence.MachineCommandEntity;
import com.colortinting.persistence.MachineEventEntity;
import com.colortinting.persistence.OperatorSettingsEntity;
import com.colortinting.persistence.RecipeEntity;
import com.colortinting.service.MachineObservationService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class ApiPresenter {
    private final AppProperties properties;

    public ApiPresenter(AppProperties properties) {
        this.properties = properties;
    }

    public RecipeResponse recipe(RecipeEntity recipe) {
        return new RecipeResponse(
                recipe.getId(),
                recipe.getName(),
                recipe.getDescription(),
                recipe.getTargetColor(),
                Quantities.format(recipe.getCyanDose()),
                Quantities.format(recipe.getMagentaDose()),
                Quantities.format(recipe.getYellowDose()),
                Quantities.format(recipe.getBlackDose()),
                recipe.getDoseUnit().name(),
                Quantities.format(recipe.getBasePaintQuantity()),
                Quantities.format(Quantities.sum(
                        recipe.getCyanDose(),
                        recipe.getMagentaDose(),
                        recipe.getYellowDose(),
                        recipe.getBlackDose()
                )),
                recipe.getCreatedAt(),
                recipe.getUpdatedAt()
        );
    }

    public BatchResponse batch(BatchEntity batch) {
        List<PumpResultResponse> pumps = new ArrayList<>();
        List<BatchPumpResultEntity> rows = new ArrayList<>(batch.getPumpResults());
        rows.sort(Comparator.comparing(row -> row.getChannel().ordinal()));
        for (BatchPumpResultEntity row : rows) {
            pumps.add(new PumpResultResponse(
                    row.getChannel().name(),
                    Quantities.format(row.getTargetQuantity()),
                    Quantities.format(row.getActualQuantity()),
                    row.getDoseUnit().name()
            ));
        }
        return new BatchResponse(
                batch.getId(),
                batch.getRecipe() == null ? null : batch.getRecipe().getId(),
                batch.getMachineId(),
                batch.getState().name(),
                batch.getRecipeName(),
                batch.getRecipeDescription(),
                batch.getTargetColor(),
                Quantities.format(batch.getCyanTarget()),
                Quantities.format(batch.getMagentaTarget()),
                Quantities.format(batch.getYellowTarget()),
                Quantities.format(batch.getBlackTarget()),
                batch.getDoseUnit().name(),
                Quantities.format(batch.getBasePaintQuantity()),
                Quantities.format(batch.getTargetTotalQuantity()),
                Quantities.format(batch.getActualTotalQuantity()),
                batch.isOutcomeUnknown(),
                batch.getFailureDetail(),
                batch.getCancellationDetail(),
                batch.getStartCommandId(),
                batch.getStartedAt(),
                batch.getCompletedAt(),
                pumps
        );
    }

    public CommandResponse command(MachineCommandEntity command) {
        return new CommandResponse(
                command.getId(),
                command.getIdempotencyKey(),
                command.getCommandType().name(),
                command.getLifecycle().name(),
                command.getBatchId(),
                command.getMessage(),
                command.isDispatchedToHardware(),
                command.isSimulated(),
                command.isReconciliationRequired(),
                command.isReconciliationResolved(),
                command.getCreatedAt(),
                command.getUpdatedAt()
        );
    }

    public HttpStatus commandStatus(CommandLifecycle lifecycle) {
        return switch (lifecycle) {
            case PENDING, ACKNOWLEDGED, EXECUTING -> HttpStatus.ACCEPTED;
            case SUCCEEDED -> HttpStatus.OK;
            case REJECTED -> HttpStatus.CONFLICT;
            case TIMED_OUT -> HttpStatus.GATEWAY_TIMEOUT;
            case FAILED -> HttpStatus.BAD_GATEWAY;
        };
    }

    public StatusResponse status(MachineObservationService.MachinePicture picture, String displayName) {
        Observation current = picture.current();
        return new StatusResponse(
                picture.machineId(),
                displayName,
                current.connectionState().name(),
                current.dataSource().name(),
                picture.simulatedAdapter() || current.simulated(),
                picture.hardwareVerified(),
                picture.commandsEnabled(),
                picture.integrationConfirmed(),
                picture.baseUrlConfigured(),
                picture.reconciliationRequired(),
                current.operatingState().name(),
                current.firmwareVersion(),
                picture.lastKnown() == null ? null : picture.lastKnown().observedAt(),
                picture.stale(),
                picture.probed(),
                current.message(),
                faults(current),
                picture.trackedBatch(),
                picture.controls()
        );
    }

    public TelemetryResponse telemetry(MachineObservationService.MachinePicture picture) {
        Observation current = picture.current();
        ReadingBlock currentReading = picture.current().success()
                ? reading(current)
                : new ReadingBlock(false, null, null, null, null, null, false, null, null, Observation.PumpReading.unknownPumps());
        ReadingBlock lastKnown = picture.lastKnown() == null ? null : reading(picture.lastKnown());
        return new TelemetryResponse(
                current.dataSource().name(),
                picture.simulatedAdapter() || current.simulated(),
                picture.stale(),
                current.success() ? current.observedAt() : null,
                picture.hardwareVerified(),
                current.operatingState().name(),
                current.message(),
                current.success() ? current.activeBatchId() : null,
                color(currentReading),
                load(currentReading),
                pumps(currentReading.pumps()),
                lastKnown == null ? null : new LastKnownTelemetry(
                        picture.lastKnown().observedAt(),
                        picture.lastKnown().dataSource().name(),
                        picture.lastKnown().simulated(),
                        color(lastKnown),
                        load(lastKnown),
                        pumps(lastKnown.pumps()),
                        picture.lastKnown().message()
                )
        );
    }

    public SettingsResponse settings(OperatorSettingsEntity settings, boolean hardwareVerified) {
        AppProperties.Machine machine = properties.getMachine();
        AppProperties.Limits limits = machine.getLimits();
        return new SettingsResponse(
                settings.getDisplayName(),
                machine.getId(),
                settings.getTelemetryRefreshIntervalMs(),
                settings.isShowReadingTimestamps(),
                settings.isCompactLayout(),
                "GRAMS",
                List.of(ColorChannel.CYAN.name(), ColorChannel.MAGENTA.name(), ColorChannel.YELLOW.name(), ColorChannel.BLACK.name()),
                new SoftwareLimits(
                        Quantities.format(limits.getMaxChannelDoseGrams()),
                        Quantities.format(limits.getMaxTotalDoseGrams()),
                        Quantities.format(limits.getMaxPumpTestGrams()),
                        limits.getMinPumpTestDurationMs(),
                        limits.getMaxPumpTestDurationMs(),
                        "These are provisional software validation limits. They do not replace firmware safety limits and are not confirmed by the hardware developer."
                ),
                new HardwareInfo(
                        machine.getAdapter(),
                        machine.isCommandsEnabled(),
                        machine.isIntegrationConfirmed(),
                        machine.getBaseUrl() != null && !machine.getBaseUrl().isBlank(),
                        hardwareVerified
                )
        );
    }

    public EventResponse event(MachineEventEntity event) {
        return new EventResponse(
                event.getId(),
                event.getOccurredAt(),
                event.getSeverity().name(),
                event.getCode(),
                event.getMessage(),
                event.getBatchId(),
                event.getCommandId(),
                event.isSimulated()
        );
    }

    private static List<FaultResponse> faults(Observation observation) {
        return observation.faults().stream()
                .map(fault -> new FaultResponse(fault.code(), fault.message(), fault.severity()))
                .toList();
    }

    private static ReadingBlock reading(Observation observation) {
        return new ReadingBlock(
                observation.color().available(),
                observation.color().calibrationState().name(),
                observation.color().red(),
                observation.color().green(),
                observation.color().blue(),
                observation.color().readAt(),
                observation.load().available(),
                Quantities.format(observation.load().weightGrams()),
                observation.load().tareState().name(),
                observation.pumps()
        );
    }

    private static ColorResponse color(ReadingBlock reading) {
        return new ColorResponse(
                reading.colorAvailable(),
                reading.calibrationState(),
                reading.red(),
                reading.green(),
                reading.blue(),
                reading.colorReadAt()
        );
    }

    private static LoadResponse load(ReadingBlock reading) {
        return new LoadResponse(reading.loadAvailable(), reading.weightGrams(), reading.tareState(), reading.colorReadAt());
    }

    private static List<PumpStatusResponse> pumps(List<Observation.PumpReading> pumps) {
        return pumps.stream()
                .map(pump -> new PumpStatusResponse(
                        pump.channel().name(),
                        pump.state().name(),
                        Quantities.format(pump.dispensedGrams()),
                        Quantities.format(pump.targetGrams()),
                        pump.fault()
                ))
                .toList();
    }

    private record ReadingBlock(
            boolean colorAvailable,
            String calibrationState,
            Integer red,
            Integer green,
            Integer blue,
            Instant colorReadAt,
            boolean loadAvailable,
            String weightGrams,
            String tareState,
            List<Observation.PumpReading> pumps
    ) {
    }

    public record RecipeResponse(
            java.util.UUID id,
            String name,
            String description,
            String targetColor,
            String cyanDose,
            String magentaDose,
            String yellowDose,
            String blackDose,
            String doseUnit,
            String basePaintQuantity,
            String totalColorant,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record BatchResponse(
            java.util.UUID id,
            java.util.UUID recipeId,
            String machineId,
            String state,
            String recipeName,
            String recipeDescription,
            String targetColor,
            String cyanTarget,
            String magentaTarget,
            String yellowTarget,
            String blackTarget,
            String doseUnit,
            String basePaintQuantity,
            String targetTotalGrams,
            String actualTotalGrams,
            boolean outcomeUnknown,
            String failureDetail,
            String cancellationDetail,
            java.util.UUID startCommandId,
            Instant startedAt,
            Instant completedAt,
            List<PumpResultResponse> pumps
    ) {
    }

    public record PumpResultResponse(String channel, String targetGrams, String actualGrams, String doseUnit) {
    }

    public record CommandResponse(
            java.util.UUID commandId,
            String idempotencyKey,
            String type,
            String lifecycle,
            java.util.UUID batchId,
            String message,
            boolean dispatchedToHardware,
            boolean simulated,
            boolean reconciliationRequired,
            boolean reconciliationResolved,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record StatusResponse(
            String machineId,
            String displayName,
            String connectionState,
            String dataSource,
            boolean simulated,
            boolean hardwareVerified,
            boolean commandsEnabled,
            boolean integrationConfirmed,
            boolean baseUrlConfigured,
            boolean reconciliationRequired,
            String operatingState,
            String firmwareVersion,
            Instant lastSuccessfulTelemetryAt,
            boolean stale,
            boolean probed,
            String message,
            List<FaultResponse> faults,
            MachineObservationService.TrackedBatch trackedBatch,
            MachineObservationService.Controls controls
    ) {
    }

    public record TelemetryResponse(
            String dataSource,
            boolean simulated,
            boolean stale,
            Instant observedAt,
            boolean hardwareVerified,
            String operatingState,
            String message,
            java.util.UUID activeBatchId,
            ColorResponse color,
            LoadResponse loadCell,
            List<PumpStatusResponse> pumps,
            LastKnownTelemetry lastKnown
    ) {
    }

    public record LastKnownTelemetry(
            Instant observedAt,
            String dataSource,
            boolean simulated,
            ColorResponse color,
            LoadResponse loadCell,
            List<PumpStatusResponse> pumps,
            String message
    ) {
    }

    public record ColorResponse(
            boolean available,
            String calibrationState,
            Integer red,
            Integer green,
            Integer blue,
            Instant readAt
    ) {
    }

    public record LoadResponse(boolean available, String weightGrams, String tareState, Instant readAt) {
    }

    public record PumpStatusResponse(
            String channel,
            String state,
            String dispensedGrams,
            String targetGrams,
            String fault
    ) {
    }

    public record FaultResponse(String code, String message, String severity) {
    }

    public record SettingsResponse(
            String displayName,
            String machineId,
            int telemetryRefreshIntervalMs,
            boolean showReadingTimestamps,
            boolean compactLayout,
            String doseUnit,
            List<String> pumps,
            SoftwareLimits softwareLimits,
            HardwareInfo hardware
    ) {
    }

    public record SoftwareLimits(
            String maxChannelDoseGrams,
            String maxTotalDoseGrams,
            String maxPumpTestGrams,
            int minPumpTestDurationMs,
            int maxPumpTestDurationMs,
            String note
    ) {
    }

    public record HardwareInfo(
            String adapter,
            boolean commandsEnabled,
            boolean integrationConfirmed,
            boolean baseUrlConfigured,
            boolean hardwareVerified
    ) {
    }

    public record EventResponse(
            java.util.UUID id,
            Instant occurredAt,
            String severity,
            String code,
            String message,
            java.util.UUID batchId,
            java.util.UUID commandId,
            boolean simulated
    ) {
    }

    public record PageResponse<T>(List<T> items, int page, int size, long totalItems, int totalPages) {
    }
}
