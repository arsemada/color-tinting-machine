package com.colortinting.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class Quantities {
    public static final int SCALE = 3;
    private static final Pattern DECIMAL = Pattern.compile("^(?:0|[1-9]\\d{0,8})(?:\\.\\d{1,3})?$");

    private Quantities() {
    }

    public static BigDecimal parseRequired(String raw, String field, List<FieldViolation> errors) {
        if (raw == null || raw.isBlank()) {
            errors.add(new FieldViolation(field, "Enter a quantity."));
            return null;
        }
        String trimmed = raw.trim();
        if (!DECIMAL.matcher(trimmed).matches()) {
            errors.add(new FieldViolation(field, "Use a nonnegative number with at most 3 decimal places."));
            return null;
        }
        return new BigDecimal(trimmed).setScale(SCALE, RoundingMode.UNNECESSARY);
    }

    public static BigDecimal parseOptional(String raw, String field, List<FieldViolation> errors) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return parseRequired(raw, field, errors);
    }

    public static String format(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.setScale(SCALE, RoundingMode.UNNECESSARY).toPlainString();
    }

    public static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(SCALE, RoundingMode.UNNECESSARY);
    }

    public static BigDecimal sum(BigDecimal... values) {
        BigDecimal total = zero();
        for (BigDecimal value : values) {
            total = total.add(value == null ? zero() : value);
        }
        return total.setScale(SCALE, RoundingMode.UNNECESSARY);
    }

    public static boolean isPositive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }

    public static List<FieldViolation> limitViolations(
            BigDecimal cyan,
            BigDecimal magenta,
            BigDecimal yellow,
            BigDecimal black,
            BigDecimal maxChannel,
            BigDecimal maxTotal
    ) {
        List<FieldViolation> errors = new ArrayList<>();
        checkChannel("cyanDose", cyan, maxChannel, errors);
        checkChannel("magentaDose", magenta, maxChannel, errors);
        checkChannel("yellowDose", yellow, maxChannel, errors);
        checkChannel("blackDose", black, maxChannel, errors);
        BigDecimal total = sum(cyan, magenta, yellow, black);
        if (maxTotal != null && total.compareTo(maxTotal) > 0) {
            errors.add(new FieldViolation(
                    "totalDose",
                    "Total colorant dose exceeds the configured software limit of " + format(maxTotal) + " g."
            ));
        }
        return errors;
    }

    private static void checkChannel(String field, BigDecimal value, BigDecimal maxChannel, List<FieldViolation> errors) {
        if (value != null && maxChannel != null && value.compareTo(maxChannel) > 0) {
            errors.add(new FieldViolation(
                    field,
                    "Dose exceeds the configured software limit of " + format(maxChannel) + " g."
            ));
        }
    }

    public record FieldViolation(String field, String message) {
    }
}
