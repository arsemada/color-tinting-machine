package com.colortinting.persistence;

import com.colortinting.domain.ModelEnums.DoseUnit;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "recipes")
public class RecipeEntity {
    @Id
    private UUID id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 2000)
    private String description;

    @Column(name = "target_color", nullable = false, length = 7)
    private String targetColor;

    @Column(name = "cyan_dose", nullable = false, precision = 12, scale = 3)
    private BigDecimal cyanDose;

    @Column(name = "magenta_dose", nullable = false, precision = 12, scale = 3)
    private BigDecimal magentaDose;

    @Column(name = "yellow_dose", nullable = false, precision = 12, scale = 3)
    private BigDecimal yellowDose;

    @Column(name = "black_dose", nullable = false, precision = 12, scale = 3)
    private BigDecimal blackDose;

    @Enumerated(EnumType.STRING)
    @Column(name = "dose_unit", nullable = false, length = 16)
    private DoseUnit doseUnit;

    @Column(name = "base_paint_quantity", precision = 12, scale = 3)
    private BigDecimal basePaintQuantity;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTargetColor() {
        return targetColor;
    }

    public void setTargetColor(String targetColor) {
        this.targetColor = targetColor;
    }

    public BigDecimal getCyanDose() {
        return cyanDose;
    }

    public void setCyanDose(BigDecimal cyanDose) {
        this.cyanDose = cyanDose;
    }

    public BigDecimal getMagentaDose() {
        return magentaDose;
    }

    public void setMagentaDose(BigDecimal magentaDose) {
        this.magentaDose = magentaDose;
    }

    public BigDecimal getYellowDose() {
        return yellowDose;
    }

    public void setYellowDose(BigDecimal yellowDose) {
        this.yellowDose = yellowDose;
    }

    public BigDecimal getBlackDose() {
        return blackDose;
    }

    public void setBlackDose(BigDecimal blackDose) {
        this.blackDose = blackDose;
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
