package com.colortinting.service;

import com.colortinting.config.AppProperties;
import com.colortinting.domain.BatchStateMachine;
import com.colortinting.domain.ModelEnums.BatchState;
import com.colortinting.domain.ModelEnums.ColorChannel;
import com.colortinting.domain.ModelEnums.ConnectionState;
import com.colortinting.domain.ModelEnums.DataSource;
import com.colortinting.domain.ModelEnums.EventSeverity;
import com.colortinting.domain.Quantities;
import com.colortinting.machine.Esp32MachineClient;
import com.colortinting.machine.Observation;
import com.colortinting.persistence.BatchEntity;
import com.colortinting.persistence.BatchPumpResultEntity;
import com.colortinting.persistence.BatchRepository;
import com.colortinting.persistence.MachineCommandEntity;
import com.colortinting.persistence.MachineCommandRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class MachineObservationService {
    private static final Logger log = LoggerFactory.getLogger(MachineObservationService.class);
    private static final Set<BatchState> ACTIVE = EnumSet.of(
            BatchState.STARTING,
            BatchState.DISPENSING,
            BatchState.PAUSED,
            BatchState.CANCELLING
    );

    private final Esp32MachineClient client;
    private final AppProperties properties;
    private final BatchRepository batches;
    private final MachineCommandRepository commands;
    private final EventService events;
    private final Clock clock;
    private final TransactionTemplate transactions;
    private final AtomicReference<CachedObservation> cache = new AtomicReference<>(CachedObservation.initial());
    private ConnectionState publishedConnection = ConnectionState.CONNECTING;
    private Set<String> publishedFaults = Set.of();

    public MachineObservationService(
            Esp32MachineClient client,
            AppProperties properties,
            BatchRepository batches,
            MachineCommandRepository commands,
            EventService events,
            Clock clock,
            PlatformTransactionManager transactionManager
    ) {
        this.client = client;
        this.properties = properties;
        this.batches = batches;
        this.commands = commands;
        this.events = events;
        this.clock = clock;
        this.transactions = new TransactionTemplate(transactionManager);
    }

    @Scheduled(fixedDelayString = "${app.machine.poll-interval-ms:2000}")
    public void scheduledPoll() {
        if (!properties.getMachine().isPollingEnabled()) {
            return;
        }
        refresh();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void probeOnStartup() {
        refresh();
    }

    public synchronized void refresh() {
        Observation observation = client.probe();
        CachedObservation previous = cache.get();
        boolean stale = false;
        Observation published = observation;
        if (observation.success() && observation.observedAt() != null) {
            Duration age = Duration.between(observation.observedAt(), clock.instant());
            if (age.toMillis() > properties.getMachine().getStaleAfterMs()) {
                stale = true;
                published = withConnection(observation, ConnectionState.STALE);
            }
        }
        Observation lastSuccess = observation.success() ? published : previous.lastSuccess();
        Observation publishedObservation = published;
        cache.set(new CachedObservation(publishedObservation, lastSuccess, stale, true));
        publishChanges(publishedObservation);
        if (observation.success()) {
            transactions.executeWithoutResult(status -> reconcile(publishedObservation));
        }
    }

    public MachinePicture picture() {
        return transactions.execute(status -> buildPicture());
    }

    private MachinePicture buildPicture() {
        CachedObservation cached = cache.get();
        Observation current = cached.current();
        String machineId = properties.getMachine().getId();
        BatchEntity tracked = batches.findFirstByMachineIdAndStateIn(machineId, ACTIVE).orElse(null);
        boolean unresolved = commands.existsByReconciliationRequiredTrueAndReconciliationResolvedFalse();
        boolean hardwareVerified = properties.getMachine().realCommandsPermitted()
                && current.success()
                && current.dataSource() == DataSource.LIVE
                && current.connectionState() == ConnectionState.CONNECTED
                && !cached.stale();
        List<String> blocked = new ArrayList<>();
        if (!hardwareVerified) {
            blocked.add("Real hardware is not connected and the ESP32 contract is not confirmed.");
        }
        if (unresolved) {
            blocked.add("A previous command timed out or failed after it may have reached the machine. Wait for an authoritative batch report before starting another operation.");
        }
        if (tracked != null) {
            blocked.add("A batch is already active (" + tracked.getState() + ").");
        }
        if (current.success()) {
            boolean seriousFault = current.faults().stream().anyMatch(fault ->
                    "ERROR".equals(fault.severity()) || "CRITICAL".equals(fault.severity()));
            if (seriousFault) {
                blocked.add("The machine is reporting a fault.");
            }
        } else if (cached.probed()) {
            blocked.add(current.message());
        } else {
            blocked.add("Machine status has not been probed yet.");
        }
        boolean startAllowed = hardwareVerified && blocked.stream().noneMatch(reason ->
                reason.startsWith("A batch") || reason.startsWith("A previous") || reason.startsWith("The machine is reporting"));
        if (!hardwareVerified) {
            startAllowed = false;
        }
        boolean cancelAllowed = hardwareVerified && tracked != null && BatchStateMachine.canRequestCancel(tracked.getState());
        boolean maintenanceAllowed = hardwareVerified && tracked == null && !unresolved && current.success() && !cached.stale();
        return new MachinePicture(
                machineId,
                current,
                cached.lastSuccess(),
                cached.stale(),
                cached.probed(),
                hardwareVerified,
                client.simulated(),
                properties.getMachine().isCommandsEnabled(),
                properties.getMachine().isIntegrationConfirmed(),
                properties.getMachine().getBaseUrl() != null && !properties.getMachine().getBaseUrl().isBlank(),
                unresolved,
                toTracked(tracked),
                new Controls(
                        startAllowed,
                        cancelAllowed,
                        maintenanceAllowed,
                        maintenanceAllowed,
                        maintenanceAllowed,
                        hardwareVerified && current.success(),
                        List.copyOf(blocked)
                )
        );
    }

    private void reconcile(Observation observation) {
        UUID reportedId = observation.reportedBatchId();
        if (reportedId == null || observation.reportedBatchState() == null) {
            return;
        }
        batches.findById(reportedId).ifPresent(batch -> applyReport(batch, observation));
    }

    private void applyReport(BatchEntity batch, Observation observation) {
        BatchState before = batch.getState();
        BatchState next = BatchStateMachine.applyReported(before, observation.reportedBatchState());
        boolean changed = next != before;
        if (changed) {
            batch.setState(next);
            if (next == BatchState.CANCELLED) {
                batch.setCancellationDetail("The machine reported the batch as cancelled.");
            }
            if (next == BatchState.FAILED) {
                batch.setFailureDetail("The machine reported the batch as failed.");
            }
            if (BatchStateMachine.isTerminal(next)) {
                batch.setCompletedAt(clock.instant());
                batch.setOutcomeUnknown(false);
                resolveCommands(batch.getId());
            }
        }
        if (observation.reportedActualTotal() != null) {
            batch.setActualTotalQuantity(observation.reportedActualTotal());
        }
        for (BatchPumpResultEntity pump : batch.getPumpResults()) {
            BigDecimal actual = observation.reportedActualByChannel().get(pump.getChannel());
            if (actual != null) {
                pump.setActualQuantity(actual);
            }
        }
        if (changed || observation.reportedActualTotal() != null) {
            batch.setUpdatedAt(clock.instant());
            batches.save(batch);
        }
        if (changed) {
            events.record(
                    terminalSeverity(next),
                    "BATCH_" + next.name(),
                    "Batch " + batch.getId() + " moved from " + before + " to " + next + " because the machine reported that state.",
                    batch.getId(),
                    null,
                    observation.simulated()
            );
        }
    }

    private void resolveCommands(UUID batchId) {
        for (MachineCommandEntity command : commands.findByReconciliationRequiredTrueAndReconciliationResolvedFalse()) {
            if (batchId.equals(command.getBatchId())) {
                command.setReconciliationResolved(true);
                command.setUpdatedAt(clock.instant());
                commands.save(command);
            }
        }
    }

    private void publishChanges(Observation observation) {
        if (publishedConnection != observation.connectionState()) {
            ConnectionState previous = publishedConnection;
            publishedConnection = observation.connectionState();
            if (observation.connectionState() == ConnectionState.COMMUNICATION_ERROR
                    || observation.connectionState() == ConnectionState.DISCONNECTED) {
                events.record(
                        EventSeverity.ERROR,
                        "ESP32_CONNECTION",
                        "Machine connection changed from " + previous + " to " + observation.connectionState() + ". " + observation.message(),
                        null,
                        null,
                        observation.simulated()
                );
            }
        }
        Set<String> codes = new HashSet<>();
        for (Observation.FaultSignal fault : observation.faults()) {
            codes.add(fault.code());
            if (!publishedFaults.contains(fault.code())) {
                events.record(parseSeverity(fault.severity()), fault.code(), fault.message(), null, null, observation.simulated());
            }
        }
        publishedFaults = Set.copyOf(codes);
    }

    private static EventSeverity terminalSeverity(BatchState state) {
        return switch (state) {
            case FAILED -> EventSeverity.ERROR;
            case CANCELLED -> EventSeverity.WARNING;
            default -> EventSeverity.INFO;
        };
    }

    private static EventSeverity parseSeverity(String severity) {
        try {
            return EventSeverity.valueOf(severity);
        } catch (IllegalArgumentException ex) {
            return EventSeverity.WARNING;
        }
    }

    private static Observation withConnection(Observation observation, ConnectionState state) {
        return new Observation(
                observation.success(),
                state,
                observation.dataSource(),
                observation.simulated(),
                observation.observedAt(),
                observation.operatingState(),
                observation.firmwareVersion(),
                observation.color(),
                observation.load(),
                observation.pumps(),
                observation.activeBatchId(),
                observation.reportedBatchId(),
                observation.reportedBatchState(),
                observation.reportedActualTotal(),
                observation.reportedActualByChannel(),
                observation.faults(),
                observation.message()
        );
    }

    private static TrackedBatch toTracked(BatchEntity batch) {
        if (batch == null) {
            return null;
        }
        List<TrackedPump> pumps = new ArrayList<>();
        for (ColorChannel channel : ColorChannel.values()) {
            batch.getPumpResults().stream()
                    .filter(pump -> pump.getChannel() == channel)
                    .findFirst()
                    .ifPresent(pump -> pumps.add(new TrackedPump(
                            channel,
                            Quantities.format(pump.getTargetQuantity()),
                            Quantities.format(pump.getActualQuantity())
                    )));
        }
        return new TrackedBatch(
                batch.getId(),
                batch.getRecipe() == null ? null : batch.getRecipe().getId(),
                batch.getRecipeName(),
                batch.getTargetColor(),
                batch.getState(),
                Quantities.format(batch.getTargetTotalQuantity()),
                Quantities.format(batch.getActualTotalQuantity()),
                batch.isOutcomeUnknown(),
                batch.getFailureDetail(),
                batch.getCancellationDetail(),
                batch.getStartedAt(),
                batch.getCompletedAt(),
                pumps
        );
    }

    public record CachedObservation(Observation current, Observation lastSuccess, boolean stale, boolean probed) {
        static CachedObservation initial() {
            return new CachedObservation(
                    Observation.unavailable(
                            ConnectionState.CONNECTING,
                            "Waiting for the first machine probe. No readings are available yet.",
                            List.of()
                    ),
                    null,
                    false,
                    false
            );
        }
    }

    public record TrackedPump(ColorChannel channel, String targetGrams, String actualGrams) {
    }

    public record TrackedBatch(
            UUID id,
            UUID recipeId,
            String recipeName,
            String targetColor,
            BatchState state,
            String targetTotalGrams,
            String actualTotalGrams,
            boolean outcomeUnknown,
            String failureDetail,
            String cancellationDetail,
            Instant startedAt,
            Instant completedAt,
            List<TrackedPump> pumps
    ) {
    }

    public record Controls(
            boolean startBatch,
            boolean cancelBatch,
            boolean tare,
            boolean calibrateSensor,
            boolean pumpTest,
            boolean emergencyStop,
            List<String> reasons
    ) {
    }

    public record MachinePicture(
            String machineId,
            Observation current,
            Observation lastKnown,
            boolean stale,
            boolean probed,
            boolean hardwareVerified,
            boolean simulatedAdapter,
            boolean commandsEnabled,
            boolean integrationConfirmed,
            boolean baseUrlConfigured,
            boolean reconciliationRequired,
            TrackedBatch trackedBatch,
            Controls controls
    ) {
    }
}
