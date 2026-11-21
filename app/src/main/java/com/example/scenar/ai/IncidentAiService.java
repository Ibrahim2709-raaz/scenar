package com.example.scenar.ai;

import com.example.scenar.model.IncidentReport;

/**
 * Boundary for incident structuring. The fake and real AI implementations use
 * the same contract so UI development does not depend on API availability.
 */
public interface IncidentAiService {
    void structureIncident(String description, IncidentCallback callback);

    interface IncidentCallback {
        void onSuccess(IncidentReport report);

        void onFailure(String message);
    }
}
