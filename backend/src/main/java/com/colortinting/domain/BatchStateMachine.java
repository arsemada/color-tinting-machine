package com.colortinting.domain;

import java.util.EnumSet;
import java.util.Set;

import static com.colortinting.domain.ModelEnums.BatchState;

/**
 * Permitted batch transitions. Terminal states are sticky.
 * A batch becomes COMPLETED, CANCELLED, or FAILED only when the machine reports that state.
 */
public final class BatchStateMachine {
    private static final Set<BatchState> TERMINAL = EnumSet.of(
            BatchState.COMPLETED,
            BatchState.CANCELLED,
            BatchState.FAILED
    );
    private static final Set<BatchState> ACTIVE = EnumSet.of(
            BatchState.STARTING,
            BatchState.DISPENSING,
            BatchState.PAUSED,
            BatchState.CANCELLING
    );

    private BatchStateMachine() {
    }

    public static boolean isTerminal(BatchState state) {
        return TERMINAL.contains(state);
    }

    public static boolean isActive(BatchState state) {
        return ACTIVE.contains(state);
    }

    public static boolean canRequestCancel(BatchState state) {
        return state == BatchState.STARTING
                || state == BatchState.DISPENSING
                || state == BatchState.PAUSED;
    }

    /**
     * Applies an authoritative machine-reported state.
     * IDLE is not a batch state. UNKNOWN and null reports leave the current state unchanged.
     */
    public static BatchState applyReported(BatchState current, BatchState reported) {
        if (current == null) {
            throw new IllegalArgumentException("current batch state is required");
        }
        if (reported == null || reported == BatchState.UNKNOWN || isTerminal(current)) {
            return current;
        }
        return switch (current) {
            case STARTING -> switch (reported) {
                case STARTING, DISPENSING, PAUSED, CANCELLING, COMPLETED, CANCELLED, FAILED -> reported;
                default -> current;
            };
            case DISPENSING -> switch (reported) {
                case DISPENSING, PAUSED, CANCELLING, COMPLETED, CANCELLED, FAILED -> reported;
                default -> current;
            };
            case PAUSED -> switch (reported) {
                case PAUSED, DISPENSING, CANCELLING, COMPLETED, CANCELLED, FAILED -> reported;
                default -> current;
            };
            case CANCELLING -> switch (reported) {
                case CANCELLING, CANCELLED, FAILED, COMPLETED -> reported;
                default -> current;
            };
            default -> current;
        };
    }
}
