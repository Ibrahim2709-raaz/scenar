package com.example.scenar.ai;

import com.example.scenar.model.IncidentReport;

public class DeterministicIncidentService
        implements IncidentAiService {

    @Override
    public void structureIncident(
            String description,
            IncidentCallback callback
    ) {
        if (description == null
                || description.trim().isEmpty()) {
            callback.onFailure(
                    "Describe what happened before continuing."
            );
            return;
        }

        IncidentReport report = new IncidentReport();

        // Preserve only information explicitly supplied by the user.
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

        report.getUncertainties().add(
                "AI structuring was unavailable; "
                        + "the original description was preserved."
        );

        callback.onSuccess(report);
    }
}