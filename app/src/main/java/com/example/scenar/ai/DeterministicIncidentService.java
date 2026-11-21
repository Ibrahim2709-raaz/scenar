package com.example.scenar.ai;

import com.example.scenar.model.IncidentReport;

public class DeterministicIncidentService
        implements IncidentAiService {

    @Override
    public void structureIncident(
            String description,
            IncidentCallback callback
    ) {
        if (description == null || description.trim().isEmpty()) {
            callback.onFailure(
                    "Describe what happened before continuing."
            );
            return;
        }

        IncidentReport report = new IncidentReport();
        report.setSummary(description.trim());

        report.getMissingInformation().add(
                "Confirm the incident type"
        );
        report.getMissingInformation().add(
                "Confirm the exact location"
        );
        report.getMissingInformation().add(
                "Confirm how many people are affected"
        );
        report.getMissingInformation().add(
                "Confirm injuries and immediate hazards"
        );

        callback.onSuccess(report);
    }
}