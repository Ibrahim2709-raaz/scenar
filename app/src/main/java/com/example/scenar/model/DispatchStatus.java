package com.example.scenar.model;

public enum DispatchStatus {
    DRAFT,
    SUBMITTED,
    REVIEWING,
    DISPATCHED_SIMULATION,
    CLOSED;

    public boolean canTransitionTo(DispatchStatus next) {
        if (next == null) return false;
        if (this == next) return true;
        return switch (this) {
            case DRAFT -> next == SUBMITTED;
            case SUBMITTED -> next == REVIEWING || next == CLOSED;
            case REVIEWING -> next == DISPATCHED_SIMULATION || next == CLOSED;
            case DISPATCHED_SIMULATION -> next == CLOSED;
            case CLOSED -> false;
        };
    }
}
