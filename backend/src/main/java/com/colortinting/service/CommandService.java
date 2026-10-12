package com.colortinting.service;

import com.colortinting.api.ApiException;
import com.colortinting.config.AppProperties;
import com.colortinting.domain.BatchStateMachine;
import com.colortinting.domain.ModelEnums.BatchState;
import com.colortinting.domain.ModelEnums.ColorChannel;
import com.colortinting.domain.ModelEnums.CommandLifecycle;
import com.colortinting.domain.ModelEnums.CommandType;
import com.colortinting.domain.ModelEnums.DeviceCallStatus;
import com.colortinting.domain.ModelEnums.DoseUnit;
import com.colortinting.domain.ModelEnums.EventSeverity;
import com.colortinting.domain.Quantities;
import com.colortinting.machine.Esp32MachineClient;
import com.colortinting.machine.Observation;
import com.colortinting.persistence.BatchEntity;
import com.colortinting.persistence.BatchPumpResultEntity;
import com.colortinting.persistence.BatchRepository;
import com.colortinting.persistence.MachineCommandEntity;
import com.colortinting.persistence.MachineCommandRepository;
import com.colortinting.persistence.OperatorSettingsRepository;
import com.colortinting.persistence.RecipeEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class CommandService {
    private static final Set<BatchState> ACTIVE = EnumSet.of(
            BatchState.STARTING, BatchState.DISPENSING, BatchState.PAUSED, BatchState.CANCELLING
    );

    private final MachineCommandRepository commands;
    private final BatchRepository batches;
    private final OperatorSettingsRepository settings;
    private final RecipeService recipes;
    private final EventService events;
    private final Esp32MachineClient client;
    private final MachineObservationService observations;
    private final AppProperties properties;
    private final Clock clock;
    private final TransactionTemplate transactions;

    public CommandService(
            MachineCommandRepository commands,
            BatchRepository batches,
            OperatorSettingsRepository settings,
            RecipeService recipes,
            EventService events,
            Esp32MachineClient client,
            MachineObservationService observations,
            AppProperties properties,
            Clock clock,
            PlatformTransactionManager transactionManager
    ) {
        this.commands = commands;
        this.batches = batches;
        this.settings = settings;
        this.recipes = recipes;
        this.events = events;
        this.client = client;
        this.observations = observations;
        this.properties = properties;
        this.clock = clock;
        this.transactions = new TransactionTemplate(transactionManager);
    }

    public MachineCommandEntity startBatch(UUID recipeId, String idempotencyKey) {
        requireKey(idempotencyKey);
        Optional<MachineCommandEntity> existing = commands.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return existing.get();
        }
        Prepared prepared = prepare(() -> prepareStart(recipeId, idempotencyKey), idempotencyKey);
        if (prepared.skipDispatch()) {
            return reload(prepared.commandId());
        }
        Observation.DeviceAck ack = client.dispatch(prepared.deviceCommand());
        MachineCommandEntity saved = transactions.execute(status -> applyStart(prepared, ack));
        observations.refresh();
        return saved;
    }

    public MachineCommandEntity cancelBatch(UUID batchId, String idempotencyKey) {
        requireKey(idempotencyKey);
        Optional<MachineCommandEntity> existing = commands.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return existing.get();
        }
        Prepared prepared = prepare(() -> prepareCancel(batchId, idempotencyKey), idempotencyKey);
        if (prepared.skipDispatch()) {
            return reload(prepared.commandId());
        }
        Observation.DeviceAck ack = client.dispatch(prepared.deviceCommand());
        MachineCommandEntity saved = transactions.execute(status -> applySimple(prepared.commandId(), ack, batch -> {
            if (ack.status() == DeviceCallStatus.ACKNOWLEDGED || ack.status() == DeviceCallStatus.EXECUTING) {
                if (BatchStateMachine.canRequestCancel(batch.getState())) {
                    batch.setState(BatchState.CANCELLING);
                    batch.setUpdatedAt(clock.instant());
                }
            }
        }));
        observations.refresh();
        return saved;
    }

    public MachineCommandEntity tare(String idempotencyKey) {
        return simpleCommand(CommandType.TARE, idempotencyKey, null, null, null);
    }

    public MachineCommandEntity calibrate(String idempotencyKey) {
        return simpleCommand(CommandType.CALIBRATE_SENSOR, idempotencyKey, null, null, null);
    }

    public MachineCommandEntity emergencyStop(String idempotencyKey) {
        return simpleCommand(CommandType.EMERGENCY_STOP, idempotencyKey, null, null, null);
    }

    public MachineCommandEntity pumpTest(ColorChannel channel, String quantityGrams, int durationMs, String idempotencyKey) {
        requireKey(idempotencyKey);
        Optional<MachineCommandEntity> existing = commands.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return existing.get();
        }
        Prepared prepared = prepare(() -> preparePump(channel, quantityGrams, durationMs, idempotencyKey), idempotencyKey);
        if (prepared.skipDispatch()) {
            return reload(prepared.commandId());
        }
        Observation.DeviceAck ack = client.dispatch(prepared.deviceCommand());
        MachineCommandEntity saved = transactions.execute(status -> applySimple(prepared.commandId(), ack, batch -> {
        }));
        observations.refresh();
        return saved;
    }

    public MachineCommandEntity resolveUnknown(UUID commandId, String note) {
        if (note == null || note.isBlank() || note.trim().length() > 500) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "Describe why the unknown outcome is being recorded as failed.",
                    List.of(new Quantities.FieldViolation("note", "Enter a note of 1 to 500 characters."))
            );
        }
        return transactions.execute(status -> {
            settings.lockSingleton();
            MachineCommandEntity command = commands.findById(commandId).orElseThrow(() -> new ApiException(
                    HttpStatus.NOT_FOUND,
                    "COMMAND_NOT_FOUND",
                    "Command " + commandId + " was not found."
            ));
            if (!command.isReconciliationRequired() || command.isReconciliationResolved()) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "NOT_RECONCILABLE",
                        "This command does not have an unresolved machine outcome."
                );
            }
            Instant now = clock.instant();
            command.setReconciliationResolved(true);
            command.setUpdatedAt(now);
            command.setMessage(trim(command.getMessage() + " Operator recorded the unknown outcome as failed: " + note.trim()));
            if (command.getBatchId() != null) {
                batches.findById(command.getBatchId()).ifPresent(batch -> {
                    if (!BatchStateMachine.isTerminal(batch.getState())) {
                        batch.setState(BatchState.FAILED);
                        batch.setOutcomeUnknown(false);
                        batch.setFailureDetail(trim(
                                "Operator recorded the outcome as failed after an unconfirmed command. Actual dispensed quantities were not invented. " + note.trim()
                        ));
                        batch.setCompletedAt(now);
                        batch.setUpdatedAt(now);
                    }
                });
            }
            events.record(
                    EventSeverity.WARNING,
                    "OUTCOME_RECORDED_FAILED",
                    "Unresolved command " + commandId + " was recorded as failed by the operator. " + note.trim(),
                    command.getBatchId(),
                    commandId,
                    command.isSimulated()
            );
            return commands.save(command);
        });
    }

    public MachineCommandEntity get(UUID id) {
        return commands.findById(id).orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND,
                "COMMAND_NOT_FOUND",
                "Command " + id + " was not found."
        ));
    }

    private MachineCommandEntity simpleCommand(
            CommandType type,
            String idempotencyKey,
            ColorChannel channel,
            BigDecimal quantity,
            Integer durationMs
    ) {
        requireKey(idempotencyKey);
        Optional<MachineCommandEntity> existing = commands.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return existing.get();
        }
        Prepared prepared = prepare(() -> prepareGeneric(type, idempotencyKey, channel, quantity, durationMs), idempotencyKey);
        if (prepared.skipDispatch()) {
            return reload(prepared.commandId());
        }
        Observation.DeviceAck ack = client.dispatch(prepared.deviceCommand());
        MachineCommandEntity saved = transactions.execute(status -> applySimple(prepared.commandId(), ack, batch -> {
        }));
        observations.refresh();
        return saved;
    }

    private Prepared prepare(java.util.function.Supplier<Prepared> supplier, String idempotencyKey) {
        try {
            return transactions.execute(status -> supplier.get());
        } catch (DataIntegrityViolationException ex) {
            return commands.findByIdempotencyKey(idempotencyKey)
                    .map(command -> new Prepared(command.getId(), command.getBatchId(), null, true))
                    .orElseThrow(() -> ex);
        }
    }

    private Prepared prepareStart(UUID recipeId, String idempotencyKey) {
        settings.lockSingleton();
        RecipeEntity recipe = recipes.get(recipeId);
        RecipeService.ParsedRecipe parsed = recipes.parseForDispense(recipe);
        Optional<String> block = blockingReason(true);
        if (block.isPresent()) {
            return rejected(CommandType.START_BATCH, idempotencyKey, null, block.get());
        }
        if (!mayDispatch()) {
            return rejected(
                    CommandType.START_BATCH,
                    idempotencyKey,
                    null,
                    "The start command was not sent. Hardware integration is not confirmed, so the batch was not started."
            );
        }
        Instant now = clock.instant();
        UUID commandId = UUID.randomUUID();
        UUID batchId = UUID.randomUUID();
        BatchEntity batch = new BatchEntity();
        batch.setId(batchId);
        batch.setRecipe(recipe);
        batch.setMachineId(properties.getMachine().getId());
        batch.setState(BatchState.STARTING);
        batch.setRecipeName(parsed.name());
        batch.setRecipeDescription(parsed.description());
        batch.setTargetColor(parsed.targetColor());
        batch.setCyanTarget(parsed.cyan());
        batch.setMagentaTarget(parsed.magenta());
        batch.setYellowTarget(parsed.yellow());
        batch.setBlackTarget(parsed.black());
        batch.setDoseUnit(DoseUnit.GRAMS);
        batch.setBasePaintQuantity(parsed.basePaintQuantity());
        batch.setTargetTotalQuantity(parsed.totalColorant());
        batch.setOutcomeUnknown(false);
        batch.setStartCommandId(commandId);
        batch.setCreatedAt(now);
        batch.setUpdatedAt(now);
        addPump(batch, ColorChannel.CYAN, parsed.cyan());
        addPump(batch, ColorChannel.MAGENTA, parsed.magenta());
        addPump(batch, ColorChannel.YELLOW, parsed.yellow());
        addPump(batch, ColorChannel.BLACK, parsed.black());
        batches.save(batch);
        savePending(commandId, CommandType.START_BATCH, idempotencyKey, batchId, "Start request is pending dispatch.");
        Map<ColorChannel, BigDecimal> targets = new EnumMap<>(ColorChannel.class);
        targets.put(ColorChannel.CYAN, parsed.cyan());
        targets.put(ColorChannel.MAGENTA, parsed.magenta());
        targets.put(ColorChannel.YELLOW, parsed.yellow());
        targets.put(ColorChannel.BLACK, parsed.black());
        return new Prepared(commandId, batchId, new Observation.DeviceCommand(
                commandId,
                CommandType.START_BATCH.name(),
                batchId,
                parsed.name(),
                targets,
                parsed.totalColorant(),
                null,
                null,
                null
        ), false);
    }

    private Prepared prepareCancel(UUID batchId, String idempotencyKey) {
        settings.lockSingleton();
        BatchEntity batch = batches.findById(batchId).orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND,
                "BATCH_NOT_FOUND",
                "Batch " + batchId + " was not found."
        ));
        if (!BatchStateMachine.canRequestCancel(batch.getState())) {
            return rejected(CommandType.CANCEL_BATCH, idempotencyKey, batchId,
                    "Cancellation was not sent. Batch state is " + batch.getState() + ".");
        }
        if (!mayDispatch()) {
            return rejected(CommandType.CANCEL_BATCH, idempotencyKey, batchId,
                    "The cancel command was not sent. Hardware integration is not confirmed, so the batch was not cancelled.");
        }
        UUID commandId = UUID.randomUUID();
        savePending(commandId, CommandType.CANCEL_BATCH, idempotencyKey, batchId, "Cancel request is pending dispatch.");
        return new Prepared(commandId, batchId, new Observation.DeviceCommand(
                commandId, CommandType.CANCEL_BATCH.name(), batchId, null, Map.of(), null, null, null, null
        ), false);
    }

    private Prepared prepareGeneric(
            CommandType type,
            String idempotencyKey,
            ColorChannel channel,
            BigDecimal quantity,
            Integer durationMs
    ) {
        settings.lockSingleton();
        Optional<String> block = blockingReason(type != CommandType.EMERGENCY_STOP);
        if (type != CommandType.EMERGENCY_STOP && block.isPresent()) {
            return rejected(type, idempotencyKey, null, block.get());
        }
        if (!mayDispatch()) {
            String message = switch (type) {
                case EMERGENCY_STOP -> "Software emergency stop was not sent. Use the physical emergency stop. Hardware integration is not confirmed, and this dashboard cannot stop the pumps by itself.";
                case TARE -> "Tare was not sent. The displayed weight was not changed.";
                case CALIBRATE_SENSOR -> "Sensor calibration was not sent. No calibration result was invented.";
                default -> "The command was not sent because hardware integration is not confirmed.";
            };
            return rejected(type, idempotencyKey, null, message);
        }
        UUID commandId = UUID.randomUUID();
        savePending(commandId, type, idempotencyKey, null, type + " request is pending dispatch.");
        return new Prepared(commandId, null, new Observation.DeviceCommand(
                commandId,
                type.name(),
                null,
                null,
                Map.of(),
                null,
                channel,
                quantity,
                durationMs
        ), false);
    }

    private Prepared preparePump(ColorChannel channel, String quantityGrams, int durationMs, String idempotencyKey) {
        settings.lockSingleton();
        if (channel == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Choose a pump.",
                    List.of(new Quantities.FieldViolation("channel", "Choose a pump.")));
        }
        List<Quantities.FieldViolation> errors = new java.util.ArrayList<>();
        BigDecimal quantity = Quantities.parseRequired(quantityGrams, "quantityGrams", errors);
        AppProperties.Limits limits = properties.getMachine().getLimits();
        if (quantity != null && (quantity.signum() <= 0 || quantity.compareTo(limits.getMaxPumpTestGrams()) > 0)) {
            errors.add(new Quantities.FieldViolation(
                    "quantityGrams",
                    "Pump test quantity must be greater than 0 and at most " + Quantities.format(limits.getMaxPumpTestGrams()) + " g."
            ));
        }
        if (durationMs < limits.getMinPumpTestDurationMs() || durationMs > limits.getMaxPumpTestDurationMs()) {
            errors.add(new Quantities.FieldViolation(
                    "durationMs",
                    "Pump test duration must be between " + limits.getMinPumpTestDurationMs() + " and " + limits.getMaxPumpTestDurationMs() + " ms."
            ));
        }
        if (!errors.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "The pump test is invalid.", errors);
        }
        Optional<BatchEntity> active = batches.findFirstByMachineIdAndStateIn(properties.getMachine().getId(), ACTIVE);
        if (active.isPresent()) {
            return rejected(CommandType.PUMP_TEST, idempotencyKey, null, "Pump test was not sent because a batch is active.");
        }
        if (commands.existsByReconciliationRequiredTrueAndReconciliationResolvedFalse()) {
            return rejected(CommandType.PUMP_TEST, idempotencyKey, null, "Pump test was not sent because a previous command still needs reconciliation.");
        }
        if (!mayDispatch()) {
            return rejected(CommandType.PUMP_TEST, idempotencyKey, null, "Pump test was not sent. Hardware integration is not confirmed.");
        }
        UUID commandId = UUID.randomUUID();
        savePending(commandId, CommandType.PUMP_TEST, idempotencyKey, null, "Pump test is pending dispatch.");
        return new Prepared(commandId, null, new Observation.DeviceCommand(
                commandId,
                CommandType.PUMP_TEST.name(),
                null,
                null,
                Map.of(),
                null,
                channel,
                quantity,
                durationMs
        ), false);
    }

    private MachineCommandEntity applyStart(Prepared prepared, Observation.DeviceAck ack) {
        MachineCommandEntity command = reload(prepared.commandId());
        BatchEntity batch = batches.findById(prepared.batchId()).orElseThrow();
        Instant now = clock.instant();
        command.setDispatchedToHardware(ack.dispatched());
        command.setMessage(trim(ack.message()));
        command.setUpdatedAt(now);
        switch (ack.status()) {
            case ACKNOWLEDGED, SUCCEEDED, EXECUTING -> {
                command.setLifecycle(ack.status() == DeviceCallStatus.EXECUTING
                        ? CommandLifecycle.EXECUTING
                        : CommandLifecycle.ACKNOWLEDGED);
                if (batch.getStartedAt() == null) {
                    batch.setStartedAt(now);
                }
                batch.setUpdatedAt(now);
                events.record(EventSeverity.INFO, "BATCH_START_" + command.getLifecycle(), command.getMessage(), batch.getId(), command.getId(), command.isSimulated());
            }
            case TIMED_OUT -> uncertain(command, batch, CommandLifecycle.TIMED_OUT, ack.message());
            case FAILED -> {
                if (ack.dispatched()) {
                    uncertain(command, batch, CommandLifecycle.FAILED, ack.message());
                } else {
                    command.setLifecycle(CommandLifecycle.FAILED);
                    failBatch(batch, ack.message());
                    events.record(EventSeverity.ERROR, "COMMAND_FAILED", ack.message(), batch.getId(), command.getId(), command.isSimulated());
                }
            }
            case REJECTED, NOT_SENT -> {
                command.setLifecycle(CommandLifecycle.REJECTED);
                failBatch(batch, ack.message());
                events.record(EventSeverity.WARNING, "COMMAND_REJECTED", ack.message(), batch.getId(), command.getId(), command.isSimulated());
            }
        }
        return commands.save(command);
    }

    private MachineCommandEntity applySimple(UUID commandId, Observation.DeviceAck ack, java.util.function.Consumer<BatchEntity> onAck) {
        MachineCommandEntity command = reload(commandId);
        Instant now = clock.instant();
        command.setDispatchedToHardware(ack.dispatched());
        command.setMessage(trim(ack.message()));
        command.setUpdatedAt(now);
        CommandLifecycle lifecycle = switch (ack.status()) {
            case ACKNOWLEDGED, SUCCEEDED -> CommandLifecycle.ACKNOWLEDGED;
            case EXECUTING -> CommandLifecycle.EXECUTING;
            case REJECTED, NOT_SENT -> CommandLifecycle.REJECTED;
            case TIMED_OUT -> CommandLifecycle.TIMED_OUT;
            case FAILED -> CommandLifecycle.FAILED;
        };
        command.setLifecycle(lifecycle);
        if (lifecycle == CommandLifecycle.TIMED_OUT || (lifecycle == CommandLifecycle.FAILED && ack.dispatched())) {
            command.setReconciliationRequired(true);
            command.setReconciliationResolved(false);
        }
        if ((lifecycle == CommandLifecycle.ACKNOWLEDGED || lifecycle == CommandLifecycle.EXECUTING) && command.getBatchId() != null) {
            batches.findById(command.getBatchId()).ifPresent(onAck);
        }
        EventSeverity severity = switch (lifecycle) {
            case REJECTED -> EventSeverity.WARNING;
            case TIMED_OUT, FAILED -> EventSeverity.ERROR;
            default -> EventSeverity.INFO;
        };
        events.record(severity, command.getCommandType().name() + "_" + lifecycle, command.getMessage(), command.getBatchId(), command.getId(), command.isSimulated());
        return commands.save(command);
    }

    private void uncertain(MachineCommandEntity command, BatchEntity batch, CommandLifecycle lifecycle, String message) {
        command.setLifecycle(lifecycle);
        command.setReconciliationRequired(true);
        command.setReconciliationResolved(false);
        batch.setOutcomeUnknown(true);
        batch.setFailureDetail(trim("Command outcome is unknown. The batch was not marked completed. " + message));
        batch.setUpdatedAt(clock.instant());
        events.record(EventSeverity.ERROR, "COMMAND_" + lifecycle, command.getMessage(), batch.getId(), command.getId(), command.isSimulated());
    }

    private void failBatch(BatchEntity batch, String message) {
        Instant now = clock.instant();
        batch.setState(BatchState.FAILED);
        batch.setOutcomeUnknown(false);
        batch.setFailureDetail(trim(message));
        batch.setCompletedAt(now);
        batch.setUpdatedAt(now);
    }

    private Prepared rejected(CommandType type, String idempotencyKey, UUID batchId, String message) {
        UUID commandId = UUID.randomUUID();
        MachineCommandEntity command = baseCommand(commandId, type, idempotencyKey, batchId, message);
        command.setLifecycle(CommandLifecycle.REJECTED);
        command.setDispatchedToHardware(false);
        commands.save(command);
        events.record(EventSeverity.WARNING, "COMMAND_REJECTED", message, batchId, commandId, client.simulated());
        return new Prepared(commandId, batchId, null, true);
    }

    private void savePending(UUID commandId, CommandType type, String idempotencyKey, UUID batchId, String message) {
        MachineCommandEntity command = baseCommand(commandId, type, idempotencyKey, batchId, message);
        command.setLifecycle(CommandLifecycle.PENDING);
        command.setDispatchedToHardware(false);
        commands.save(command);
    }

    private MachineCommandEntity baseCommand(UUID id, CommandType type, String key, UUID batchId, String message) {
        Instant now = clock.instant();
        MachineCommandEntity command = new MachineCommandEntity();
        command.setId(id);
        command.setIdempotencyKey(key);
        command.setCommandType(type);
        command.setBatchId(batchId);
        command.setMachineId(properties.getMachine().getId());
        command.setMessage(trim(message));
        command.setSimulated(client.simulated());
        command.setReconciliationRequired(false);
        command.setReconciliationResolved(false);
        command.setCreatedAt(now);
        command.setUpdatedAt(now);
        return command;
    }

    private Optional<String> blockingReason(boolean includeActiveBatch) {
        if (commands.existsByReconciliationRequiredTrueAndReconciliationResolvedFalse()) {
            return Optional.of("The command was not sent. A previous command needs reconciliation before another machine operation.");
        }
        if (includeActiveBatch) {
            Optional<BatchEntity> active = batches.findFirstByMachineIdAndStateIn(properties.getMachine().getId(), ACTIVE);
            if (active.isPresent()) {
                return Optional.of("The command was not sent because batch " + active.get().getId() + " is " + active.get().getState() + ".");
            }
        }
        return Optional.empty();
    }

    private boolean mayDispatch() {
        return properties.getMachine().simulatedAdapter() || properties.getMachine().realCommandsPermitted();
    }

    private MachineCommandEntity reload(UUID id) {
        return commands.findById(id).orElseThrow();
    }

    private void requireKey(String key) {
        if (key == null || !key.matches("^[A-Za-z0-9._:-]{8,80}$")) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "The idempotency key is invalid.",
                    List.of(new Quantities.FieldViolation(
                            "idempotencyKey",
                            "Use 8 to 80 characters: letters, numbers, dot, underscore, colon, or hyphen."
                    ))
            );
        }
    }

    private void addPump(BatchEntity batch, ColorChannel channel, BigDecimal target) {
        BatchPumpResultEntity pump = new BatchPumpResultEntity();
        pump.setId(UUID.randomUUID());
        pump.setChannel(channel);
        pump.setTargetQuantity(target);
        pump.setDoseUnit(DoseUnit.GRAMS);
        batch.addPumpResult(pump);
    }

    private static String trim(String message) {
        if (message == null) {
            return "";
        }
        return message.length() <= 2000 ? message : message.substring(0, 2000);
    }

    private record Prepared(UUID commandId, UUID batchId, Observation.DeviceCommand deviceCommand, boolean skipDispatch) {
    }
}
