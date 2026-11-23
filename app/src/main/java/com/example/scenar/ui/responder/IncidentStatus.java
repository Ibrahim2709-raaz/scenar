package com.example.scenar.ui.responder;

public enum IncidentStatus {
    RECEIVED("Received", 0xFF0277BD, 0xFFE1F5FE),
    DISPATCHED("Dispatched", 0xFFE65100, 0xFFFFF3E0),
    EN_ROUTE("En Route", 0xFF6A1B9A, 0xFFF3E5F5),
    ON_SCENE("On Scene", 0xFFB00020, 0xFFFFEBEE),
    RESOLVED("Resolved", 0xFF1B5E20, 0xFFE8F5E9);

    private final String label;
    private final int color;
    private final int backgroundColor;

    IncidentStatus(String label, int color, int backgroundColor) {
        this.label = label;
        this.color = color;
        this.backgroundColor = backgroundColor;
    }

    public String getLabel() {
        return label;
    }

    public int getColor() {
        return color;
    }

    public int getBackgroundColor() {
        return backgroundColor;
    }

    public IncidentStatus next() {
        switch (this) {
            case RECEIVED:
                return DISPATCHED;
            case DISPATCHED:
                return EN_ROUTE;
            case EN_ROUTE:
                return ON_SCENE;
            case ON_SCENE:
                return RESOLVED;
            case RESOLVED:
            default:
                return RESOLVED;
        }
    }
}
