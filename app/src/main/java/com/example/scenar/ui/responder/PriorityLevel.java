package com.example.scenar.ui.responder;

import com.example.scenar.model.IncidentReport;

public enum PriorityLevel {
    CRITICAL("CRITICAL PRIORITY", 0xFFB00020, 0xFFFFEBEE),
    HIGH("HIGH PRIORITY", 0xFFD84315, 0xFFFBE9E7),
    MEDIUM("MEDIUM PRIORITY", 0xFFE65100, 0xFFFFF3E0),
    LOW("LOW PRIORITY", 0xFF2E7D32, 0xFFE8F5E9);

    private final String label;
    private final int color;
    private final int backgroundColor;

    PriorityLevel(String label, int color, int backgroundColor) {
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

    public static PriorityLevel calculate(IncidentReport report) {
        if (report == null) return MEDIUM;
        if (!report.getCriticalConditions().isEmpty()) {
            return CRITICAL;
        }
        if (report.getPeopleAffected() != null && report.getPeopleAffected() >= 2) {
            return HIGH;
        }
        for (String hazard : report.getHazards()) {
            String lower = hazard.toLowerCase();
            if (lower.contains("fire") || lower.contains("smoke") || lower.contains("gas") || lower.contains("collapse")) {
                return HIGH;
            }
        }
        return MEDIUM;
    }
}
