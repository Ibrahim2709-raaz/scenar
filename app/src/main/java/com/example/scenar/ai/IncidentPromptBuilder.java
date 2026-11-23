package com.example.scenar.ai;

public class IncidentPromptBuilder {

    private static final String JSON_FORMAT =
            "{\n"
                    + "  \"incidentType\": null,\n"
                    + "  \"summary\": \"\",\n"
                    + "  \"peopleAffected\": null,\n"
                    + "  \"criticalConditions\": [],\n"
                    + "  \"hazards\": [],\n"
                    + "  \"locationDescription\": null,\n"
                    + "  \"latitude\": null,\n"
                    + "  \"longitude\": null,\n"
                    + "  \"medicalFlags\": [],\n"
                    + "  \"missingInformation\": [],\n"
                    + "  \"uncertainties\": []\n"
                    + "}";

    public String build(String description) {
        return "You organize emergency information for human review.\n"
                + "Do not diagnose or recommend treatment.\n"
                + "Do not invent any information.\n"
                + "Use null for unknown single values.\n"
                + "Use empty arrays for unknown lists.\n"
                + "Put contradictory details under uncertainties.\n"
                + "Put information responders still need under "
                + "missingInformation.\n"
                + "Return valid JSON only. Do not use Markdown.\n\n"
                + "Required JSON format:\n"
                + JSON_FORMAT
                + "\n\nUser description:\n"
                + description;
    }
}