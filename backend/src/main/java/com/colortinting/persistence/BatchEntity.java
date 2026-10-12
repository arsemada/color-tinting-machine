package com.colortinting.persistence;

import com.colortinting.domain.ModelEnums.BatchState;
import com.colortinting.domain.ModelEnums.DoseUnit;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "batches")
public class BatchEntity {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id")
    private RecipeEntity recipe;

    @Column(name = "machine_id", nullable = false, length = 80)
    private String machineId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private BatchState state;

    @Column(name = "recipe_name", nullable = false, length = 120)
    private String recipeName;

    @Column(name = "recipe_description", length = 2000)
    private String recipeDescription;

    @Column(name = "target_color", nullable = false, length = 7)
    private String targetColor;

    @Column(name = "cyan_target", nullable = false, precision = 12, scale = 3)
    private BigDecimal cyanTarget;

    @Column(name = "magenta_target", nullable = false, precision = 12, scale = 3)
    private BigDecimal magentaTarget;

    @Column(name = "yellow_target", nullable = false, precision = 12, scale = 3)
    private BigDecimal yellowTarget;

    @Column(name = "black_target", nullable = false, precision = 12, scale = 3)
    private BigDecimal blackTarget;

    @Enumerated(EnumType.STRING)
    @Column(name = "dose_unit", nullable = false, length = 16)
    private DoseUnit doseUnit;

    @Column(name = "base_paint_quantity", precision = 12, scale = 3)
    private BigDecimal basePaintQuantity;

    @Column(name = "target_total_quantity", nullable = false, precision = 12, scale = 3)
    private BigDecimal targetTotalQuantity;

    @Column(name = "actual_total_quantity", precision = 12, scale = 3)
    private BigDecimal actualTotalQuantity;

    @Column(name = "outcome_unknown", nullable = false)
    private boolean outcomeUnknown;

    @Column(name = "failure_detail", length = 2000)
    private String failureDetail;

    @Column(name = "cancellation_detail", length = 2000)
    private String cancellationDetail;

    @Column(name = "start_command_id")
    private UUID startCommandId;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "batch", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BatchPumpResultEntity> pumpResults = new ArrayList<>();

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public RecipeEntity getRecipe() {
        return recipe;
    }

    public void setRecipe(RecipeEntity recipe) {
        this.recipe = recipe;
    }

    public String getMachineId() {
        return machineId;
    }

    public void setMachineId(String machineId) {
        this.machineId = machineId;
    }

    public BatchState getState() {
        return state;
    }

    public void setState(BatchState state) {
        this.state = state;
    }

    public String getRecipeName() {
        return recipeName;
    }

    public void setRecipeName(String recipeName) {
        this.recipeName = recipeName;
    }

    public String getRecipeDescription() {
        return recipeDescription;
    }

    public void setRecipeDescription(String recipeDescription) {
        this.recipeDescription = recipeDescription;
    }

    public String getTargetColor() {
        return targetColor;
    }

    public void setTargetColor(String targetColor) {
        this.targetColor = targetColor;
    }

    public BigDecimal getCyanTarget() {
        return cyanTarget;
    }

    public void setCyanTarget(BigDecimal cyanTarget) {
        this.cyanTarget = cyanTarget;
    }

    public BigDecimal getMagentaTarget() {
        return magentaTarget;
    }

    public void setMagentaTarget(BigDecimal magentaTarget) {
        this.magentaTarget = magentaTarget;
    }

    public BigDecimal getYellowTarget() {
        return yellowTarget;
    }

    public void setYellowTarget(BigDecimal yellowTarget) {
        this.yellowTarget = yellowTarget;
    }

    public BigDecimal getBlackTarget() {
        return blackTarget;
    }

    public void setBlackTarget(BigDecimal blackTarget) {
        this.blackTarget = blackTarget;
    }

    public DoseUnit getDoseUnit() {
        return doseUnit;
    }

    public void setDoseUnit(DoseUnit doseUnit) {
        this.doseUnit = doseUnit;
    }

    public BigDecimal getBasePaintQuantity() {
        return basePaintQuantity;
    }

    public void setBasePaintQuantity(BigDecimal basePaintQuantity) {
        this.basePaintQuantity = basePaintQuantity;
    }

    public BigDecimal getTargetTotalQuantity() {
        return targetTotalQuantity;
    }

    public void setTargetTotalQuantity(BigDecimal targetTotalQuantity) {
        this.targetTotalQuantity = targetTotalQuantity;
    }

    public BigDecimal getActualTotalQuantity() {
        return actualTotalQuantity;
    }

    public void setActualTotalQuantity(BigDecimal actualTotalQuantity) {
        this.actualTotalQuantity = actualTotalQuantity;
    }

    public boolean isOutcomeUnknown() {
        return outcomeUnknown;
    }

    public void setOutcomeUnknown(boolean outcomeUnknown) {
        this.outcomeUnknown = outcomeUnknown;
    }

    public String getFailureDetail() {
        return failureDetail;
    }

    public void setFailureDetail(String failureDetail) {
        this.failureDetail = failureDetail;
    }

    public String getCancellationDetail() {
        return cancellationDetail;
    }

    public void setCancellationDetail(String cancellationDetail) {
        this.cancellationDetail = cancellationDetail;
    }

    public UUID getStartCommandId() {
        return startCommandId;
    }

    public void setStartCommandId(UUID startCommandId) {
        this.startCommandId = startCommandId;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
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

    public List<BatchPumpResultEntity> getPumpResults() {
        return pumpResults;
    }

    public void setPumpResults(List<BatchPumpResultEntity> pumpResults) {
        this.pumpResults = pumpResults;
    }

    public void addPumpResult(BatchPumpResultEntity result) {
        result.setBatch(this);
        pumpResults.add(result);
    }
}
