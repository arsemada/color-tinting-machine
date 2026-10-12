package com.colortinting.persistence;

import com.colortinting.domain.ModelEnums.CommandLifecycle;
import com.colortinting.domain.ModelEnums.CommandType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "machine_commands")
public class MachineCommandEntity {
    @Id
    private UUID id;

    @Column(name = "idempotency_key", nullable = false, length = 80)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "command_type", nullable = false, length = 40)
    private CommandType commandType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CommandLifecycle lifecycle;

    @Column(name = "batch_id")
    private UUID batchId;

    @Column(name = "machine_id", nullable = false, length = 80)
    private String machineId;

    @Column(nullable = false, length = 2000)
    private String message;

    @Column(name = "dispatched_to_hardware", nullable = false)
    private boolean dispatchedToHardware;

    @Column(nullable = false)
    private boolean simulated;

    @Column(name = "reconciliation_required", nullable = false)
    private boolean reconciliationRequired;

    @Column(name = "reconciliation_resolved", nullable = false)
    private boolean reconciliationResolved;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public CommandType getCommandType() {
        return commandType;
    }

    public void setCommandType(CommandType commandType) {
        this.commandType = commandType;
    }

    public CommandLifecycle getLifecycle() {
        return lifecycle;
    }

    public void setLifecycle(CommandLifecycle lifecycle) {
        this.lifecycle = lifecycle;
    }

    public UUID getBatchId() {
        return batchId;
    }

    public void setBatchId(UUID batchId) {
        this.batchId = batchId;
    }

    public String getMachineId() {
        return machineId;
    }

    public void setMachineId(String machineId) {
        this.machineId = machineId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isDispatchedToHardware() {
        return dispatchedToHardware;
    }

    public void setDispatchedToHardware(boolean dispatchedToHardware) {
        this.dispatchedToHardware = dispatchedToHardware;
    }

    public boolean isSimulated() {
        return simulated;
    }

    public void setSimulated(boolean simulated) {
        this.simulated = simulated;
    }

    public boolean isReconciliationRequired() {
        return reconciliationRequired;
    }

    public void setReconciliationRequired(boolean reconciliationRequired) {
        this.reconciliationRequired = reconciliationRequired;
    }

    public boolean isReconciliationResolved() {
        return reconciliationResolved;
    }

    public void setReconciliationResolved(boolean reconciliationResolved) {
        this.reconciliationResolved = reconciliationResolved;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
