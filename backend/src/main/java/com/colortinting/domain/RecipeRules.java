package com.colortinting.domain;

import com.colortinting.config.AppProperties;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class RecipeRules {
    public static final Pattern COLOR = Pattern.compile("^#[0-9A-Fa-f]{6}$");

    private RecipeRules() {
    }

    public static List<Quantities.FieldViolation> validate(
            String name,
            String description,
            String targetColor,
            String cyanDose,
            String magentaDose,
            String yellowDose,
            String blackDose,
            String doseUnit,
            String basePaintQuantity,
            AppProperties.Limits limits,
            boolean requirePositiveDose
    ) {
        List<Quantities.FieldViolation> errors = new ArrayList<>();
        if (name == null || name.isBlank()) {
            errors.add(new Quantities.FieldViolation("name", "Name is required."));
        } else if (name.trim().length() > 120) {
            errors.add(new Quantities.FieldViolation("name", "Name must be 120 characters or fewer."));
        }
        if (description != null && description.length() > 2000) {
            errors.add(new Quantities.FieldViolation("description", "Description must be 2000 characters or fewer."));
        }
        if (targetColor == null || targetColor.isBlank()) {
            errors.add(new Quantities.FieldViolation("targetColor", "Target color is required."));
        } else if (!COLOR.matcher(targetColor.trim()).matches()) {
            errors.add(new Quantities.FieldViolation("targetColor", "Use a color in #RRGGBB form."));
        }
        if (doseUnit == null || doseUnit.isBlank()) {
            errors.add(new Quantities.FieldViolation("doseUnit", "Dose unit is required."));
        } else if (!"GRAMS".equals(doseUnit.trim())) {
            errors.add(new Quantities.FieldViolation("doseUnit", "Only GRAMS is supported."));
        }

        BigDecimal cyan = Quantities.parseRequired(cyanDose, "cyanDose", errors);
        BigDecimal magenta = Quantities.parseRequired(magentaDose, "magentaDose", errors);
        BigDecimal yellow = Quantities.parseRequired(yellowDose, "yellowDose", errors);
        BigDecimal black = Quantities.parseRequired(blackDose, "blackDose", errors);
        Quantities.parseOptional(basePaintQuantity, "basePaintQuantity", errors);

        if (cyan != null && magenta != null && yellow != null && black != null) {
            errors.addAll(Quantities.limitViolations(
                    cyan,
                    magenta,
                    yellow,
                    black,
                    limits.getMaxChannelDoseGrams(),
                    limits.getMaxTotalDoseGrams()
            ));
            if (requirePositiveDose && !Quantities.isPositive(cyan)
                    && !Quantities.isPositive(magenta)
                    && !Quantities.isPositive(yellow)
                    && !Quantities.isPositive(black)) {
                errors.add(new Quantities.FieldViolation(
                        "doses",
                        "At least one colorant dose must be greater than zero before dispensing."
                ));
            }
        }
        return errors;
    }
}
