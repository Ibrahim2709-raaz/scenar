package com.example.scenar.ui.responder;

import com.example.scenar.model.DispatchStatus;

/**
 * Display helpers for the shared {@link DispatchStatus}. Holds colours and the order
 * the "advance" button follows; the allowed transitions come from DispatchStatus itself.
 */
public final class DispatchStatusStyle {
    private DispatchStatusStyle() {
    }

    /** Statuses a responder moves through, in order. DRAFT never reaches the dashboard. */
    public static final DispatchStatus[] RESPONDER_STEPS = {
            DispatchStatus.SUBMITTED,
            DispatchStatus.REVIEWING,
            DispatchStatus.DISPATCHED_SIMULATION,
            DispatchStatus.CLOSED
    };

    /** The next step for the advance button, or null when the incident is closed. */
    public static DispatchStatus next(DispatchStatus status) {
        if (status == null) return null;
        switch (status) {
            case DRAFT:
                return DispatchStatus.SUBMITTED;
            case SUBMITTED:
                return DispatchStatus.REVIEWING;
            case REVIEWING:
                return DispatchStatus.DISPATCHED_SIMULATION;
            case DISPATCHED_SIMULATION:
                return DispatchStatus.CLOSED;
            case CLOSED:
            default:
                return null;
        }
    }

    public static int color(DispatchStatus status) {
        if (status == null) return 0xFF475569;
        switch (status) {
            case SUBMITTED:
                return 0xFF0277BD;
            case REVIEWING:
                return 0xFF6A1B9A;
            case DISPATCHED_SIMULATION:
                return 0xFFE65100;
            case CLOSED:
                return 0xFF1B5E20;
            case DRAFT:
            default:
                return 0xFF475569;
        }
    }

    public static int backgroundColor(DispatchStatus status) {
        if (status == null) return 0xFFF1F5F9;
        switch (status) {
            case SUBMITTED:
                return 0xFFE1F5FE;
            case REVIEWING:
                return 0xFFF3E5F5;
            case DISPATCHED_SIMULATION:
                return 0xFFFFF3E0;
            case CLOSED:
                return 0xFFE8F5E9;
            case DRAFT:
            default:
                return 0xFFF1F5F9;
        }
    }
}
