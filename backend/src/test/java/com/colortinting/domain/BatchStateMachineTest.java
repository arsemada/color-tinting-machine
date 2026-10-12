package com.colortinting.domain;

import org.junit.jupiter.api.Test;

import static com.colortinting.domain.ModelEnums.BatchState;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BatchStateMachineTest {
    @Test
    void keepsTerminalStates() {
        assertEquals(BatchState.COMPLETED, BatchStateMachine.applyReported(BatchState.COMPLETED, BatchState.DISPENSING));
        assertEquals(BatchState.CANCELLED, BatchStateMachine.applyReported(BatchState.CANCELLED, BatchState.COMPLETED));
        assertEquals(BatchState.FAILED, BatchStateMachine.applyReported(BatchState.FAILED, BatchState.COMPLETED));
    }

    @Test
    void doesNotCompleteWhenTheMachineReportIsMissing() {
        assertEquals(BatchState.STARTING, BatchStateMachine.applyReported(BatchState.STARTING, null));
        assertEquals(BatchState.DISPENSING, BatchStateMachine.applyReported(BatchState.DISPENSING, BatchState.UNKNOWN));
    }

    @Test
    void acceptsAuthoritativeCompletionAndCancellation() {
        assertEquals(BatchState.COMPLETED, BatchStateMachine.applyReported(BatchState.DISPENSING, BatchState.COMPLETED));
        assertEquals(BatchState.CANCELLING, BatchStateMachine.applyReported(BatchState.DISPENSING, BatchState.CANCELLING));
        assertEquals(BatchState.CANCELLED, BatchStateMachine.applyReported(BatchState.CANCELLING, BatchState.CANCELLED));
    }

    @Test
    void identifiesActiveAndCancellableStates() {
        assertTrue(BatchStateMachine.isActive(BatchState.PAUSED));
        assertFalse(BatchStateMachine.isActive(BatchState.COMPLETED));
        assertTrue(BatchStateMachine.canRequestCancel(BatchState.STARTING));
        assertFalse(BatchStateMachine.canRequestCancel(BatchState.CANCELLING));
    }
}
