package com.example.scenar.ai;

import com.example.scenar.model.IncidentReport;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;

public class IncidentReportJsonParser {

    private final Gson gson;

    public IncidentReportJsonParser() {
        gson = new Gson();
    }

    public IncidentReport parse(String rawResponse)
            throws IncidentParsingException {

        if (rawResponse == null
                || rawResponse.trim().isEmpty()) {
            throw new IncidentParsingException(
                    "AI returned an empty response."
            );
        }

        String json = removeMarkdownFence(rawResponse);

        try {
            IncidentReport report =
                    gson.fromJson(json, IncidentReport.class);

            if (report == null) {
                throw new IncidentParsingException(
                        "AI returned no incident report."
                );
            }

            return report;

        } catch (JsonParseException exception) {
            throw new IncidentParsingException(
                    "AI returned malformed JSON.",
                    exception
            );
        }
    }

    private String removeMarkdownFence(String response)
            throws IncidentParsingException {

        String trimmed = response.trim();

        if (!trimmed.startsWith("```")) {
            return trimmed;
        }

        int firstNewline = trimmed.indexOf('\n');
        int closingFence = trimmed.lastIndexOf("```");

        if (firstNewline < 0
                || closingFence <= firstNewline) {
            throw new IncidentParsingException(
                    "AI returned an incomplete JSON code block."
            );
        }

        return trimmed.substring(
                firstNewline + 1,
                closingFence
        ).trim();
    }
}