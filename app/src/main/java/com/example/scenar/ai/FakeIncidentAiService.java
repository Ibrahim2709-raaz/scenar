package com.example.scenar.ai;

import com.example.scenar.model.IncidentReport;

/**
 * Deterministic offline implementation used until the real AI service is ready.
 */
public class FakeIncidentAiService implements IncidentAiService {
    @Override
    public void structureIncident(String description, IncidentCallback callback) {
        if (description == null || description.trim().isEmpty()) {
            callback.onFailure("Describe what happened before continuing.");
            return;
        }

        IncidentReport report = new IncidentReport();
        report.setIncidentType("Structural emergency");
        report.setSummary(description.trim());
        report.setPeopleAffected(2);
        report.getCriticalConditions().add("One person may be unresponsive");
        report.getHazards().add("Smoke reported nearby");
        report.getMissingInformation().add("Confirm the exact location");
        report.getUncertainties().add("Number of affected people requires confirmation");
        callback.onSuccess(report);
    }
}
