package com.example.scenar.ui.victim;

import com.example.scenar.ai.IncidentAiService;
import com.example.scenar.model.IncidentReport;

import org.json.JSONException;
import org.json.JSONObject;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

final class VictimReportDraft {
    record Coordinates(double latitude, double longitude, Float accuracyMeters) {}
    record MedicalProfile(String allergies, String medications, String conditions) {}
    record Input(String category, String description, String address,
                 Coordinates coordinates, MedicalProfile medicalProfile) {}
    record Report(Input input, String summary, Instant generatedAt) {
        IncidentReport toReviewedIncident(String categoryLabel, String correctedSummary) {
            IncidentReport reviewed = new IncidentReport();
            if (!"unspecified".equals(input.category())) reviewed.setIncidentType(categoryLabel);
            reviewed.setSummary(correctedSummary);
            if (!input.address().isEmpty()) reviewed.setLocationDescription(input.address());
            if (input.coordinates() != null) {
                reviewed.setLatitude(input.coordinates().latitude());
                reviewed.setLongitude(input.coordinates().longitude());
            }
            List<String> medicalFlags = reviewed.getMedicalFlags();
            addMedicalFlag(medicalFlags, "Allergies", input.medicalProfile().allergies());
            addMedicalFlag(medicalFlags, "Medications", input.medicalProfile().medications());
            addMedicalFlag(medicalFlags, "Conditions or access needs", input.medicalProfile().conditions());
            return reviewed;
        }

        JSONObject toJson(String correctedSummary) throws JSONException {
            Coordinates point = input.coordinates();
            Object coordinates = point == null ? JSONObject.NULL : new JSONObject()
                    .put("latitude", point.latitude())
                    .put("longitude", point.longitude())
                    .put("accuracyMeters", point.accuracyMeters() == null
                            ? JSONObject.NULL : point.accuracyMeters());
            MedicalProfile profile = input.medicalProfile();
            return new JSONObject()
                    .put("schemaVersion", 1)
                    .put("practiceMode", true)
                    .put("generatedAt", generatedAt.toString())
                    .put("category", input.category())
                    .put("description", input.description())
                    .put("summary", correctedSummary)
                    .put("location", new JSONObject().put("address", input.address())
                            .put("coordinates", coordinates))
                    .put("medicalProfile", new JSONObject().put("allergies", profile.allergies())
                            .put("medications", profile.medications())
                            .put("conditions", profile.conditions()));
        }
    }

    static CompletableFuture<Report> generateIncidentReport(Input input, IncidentAiService ai) {
        CompletableFuture<Report> result = new CompletableFuture<>();
        CompletableFuture.runAsync(() -> ai.structureIncident(input.description(),
                new IncidentAiService.IncidentCallback() {
                    @Override
                    public void onSuccess(IncidentReport report) {
                        // The practice service includes illustrative facts. Use only its summary.
                        if (report == null) {
                            result.completeExceptionally(new IllegalArgumentException("Report was missing"));
                            return;
                        }
                        result.complete(new Report(input, report.getSummary(), Instant.now()));
                    }

                    @Override
                    public void onFailure(String message) {
                        result.completeExceptionally(new IllegalArgumentException(message));
                    }
                })).exceptionally(error -> {
                    result.completeExceptionally(error);
                    return null;
                });
        return result;
    }

    private static void addMedicalFlag(List<String> flags, String label, String value) {
        if (!value.isEmpty()) flags.add(label + ": " + value);
    }

    private VictimReportDraft() {}
}
