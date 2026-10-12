package com.colortinting.domain;

import com.colortinting.config.AppProperties;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecipeRulesTest {
    private final AppProperties.Limits limits = new AppProperties.Limits();

    @Test
    void rejectsBlankNameAndInvalidColor() {
        List<Quantities.FieldViolation> errors = RecipeRules.validate(
                " ",
                null,
                "blue",
                "1",
                "0",
                "0",
                "0",
                "GRAMS",
                null,
                limits,
                true
        );
        assertTrue(errors.stream().anyMatch(error -> error.field().equals("name")));
        assertTrue(errors.stream().anyMatch(error -> error.field().equals("targetColor")));
    }

    @Test
    void rejectsAllZeroDosesWhenDispensing() {
        List<Quantities.FieldViolation> errors = RecipeRules.validate(
                "Empty",
                null,
                "#112233",
                "0",
                "0",
                "0",
                "0",
                "GRAMS",
                null,
                limits,
                true
        );
        assertTrue(errors.stream().anyMatch(error -> error.field().equals("doses")));
    }

    @Test
    void rejectsQuantitiesAboveConfiguredSoftwareLimits() {
        limits.setMaxChannelDoseGrams(new BigDecimal("10.000"));
        List<Quantities.FieldViolation> errors = RecipeRules.validate(
                "Heavy",
                null,
                "#112233",
                "10.001",
                "0",
                "0",
                "0",
                "GRAMS",
                null,
                limits,
                true
        );
        assertTrue(errors.stream().anyMatch(error -> error.field().equals("cyanDose")));
    }

    @Test
    void parsesDecimalQuantitiesWithoutTurningBlankIntoZero() {
        List<Quantities.FieldViolation> errors = new java.util.ArrayList<>();
        assertNull(Quantities.parseOptional(" ", "basePaintQuantity", errors));
        assertTrue(errors.isEmpty());
        assertEquals("1.500", Quantities.format(Quantities.parseRequired("1.5", "cyanDose", errors)));
        assertFalse(errors.isEmpty() == false && Quantities.parseRequired("0", "cyanDose", errors).signum() != 0);
    }
}
