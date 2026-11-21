package com.example.scenar.repository;

import com.example.scenar.model.IncidentReport;

/**
 * In-memory state for the prototype flow. Replace or extend this behind the
 * same API when persistence or a real dispatch backend is introduced.
 */
public final class IncidentRepository {
    private static final IncidentRepository INSTANCE = new IncidentRepository();

    private IncidentReport currentReport;

    private IncidentRepository() {
    }

    public static IncidentRepository getInstance() {
        return INSTANCE;
    }

    public IncidentReport getCurrentReport() {
        return currentReport;
    }

    public void setCurrentReport(IncidentReport currentReport) {
        this.currentReport = currentReport;
    }

    public void clear() {
        currentReport = null;
    }
}
