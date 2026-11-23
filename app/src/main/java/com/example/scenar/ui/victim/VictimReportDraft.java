package com.example.scenar.ui.victim;

import com.example.scenar.ai.IncidentAiService;
import com.example.scenar.model.IncidentReport;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

final class VictimReportDraft {

    enum GenerationSource {
        AI,
        LOCAL_FALLBACK
    }

    record Coordinates(
            double latitude,
            double longitude,
            Float accuracyMeters
    ) {}

    record MedicalProfile(
            String allergies,
            String medications,
            String conditions
    ) {}

    record Input(
            String category,
            String description,
            String address,
            Coordinates coordinates,
            MedicalProfile medicalProfile
    ) {}

    record Report(
            Input input,
            IncidentReport structuredIncident,
            Instant generatedAt,
            GenerationSource generationSource,
            String generationMessage
    ) {
        Report {
            if (generationSource == null) {
                generationSource = GenerationSource.AI;
            }

            if (generationMessage == null) {
                generationMessage = "";
            }
        }

        String summary() {
            return structuredIncident.getSummary();
        }

        boolean usedLocalFallback() {
            return generationSource
                    == GenerationSource.LOCAL_FALLBACK;
        }

        IncidentReport toReviewedIncident(
                String categoryLabel,
                String correctedSummary
        ) {
            IncidentReport reviewed =
                    new IncidentReport(structuredIncident);

            if (!"unspecified".equals(input.category())) {
                reviewed.setIncidentType(categoryLabel);
            }

            reviewed.setSummary(correctedSummary);

            if (!input.address().isEmpty()) {
                reviewed.setLocationDescription(
                        input.address()
                );
            }

            if (input.coordinates() != null) {
                reviewed.setLatitude(
                        input.coordinates().latitude()
                );
                reviewed.setLongitude(
                        input.coordinates().longitude()
                );
            }

            List<String> medicalFlags =
                    reviewed.getMedicalFlags();

            addMedicalFlag(
                    medicalFlags,
                    "Allergies",
                    input.medicalProfile().allergies()
            );
            addMedicalFlag(
                    medicalFlags,
                    "Medications",
                    input.medicalProfile().medications()
            );
            addMedicalFlag(
                    medicalFlags,
                    "Conditions or access needs",
                    input.medicalProfile().conditions()
            );

            return reviewed;
        }

        JSONObject toJson(
                IncidentReport reviewed
        ) throws JSONException {
            Coordinates point = input.coordinates();

            boolean originalCoordinates =
                    point != null
                            && Double.valueOf(
                            point.latitude()
                    ).equals(
                            reviewed.getLatitude()
                    )
                            && Double.valueOf(
                            point.longitude()
                    ).equals(
                            reviewed.getLongitude()
                    );

            Object coordinates =
                    reviewed.getLatitude() == null
                            ? JSONObject.NULL
                            : new JSONObject()
                            .put(
                                    "latitude",
                                    reviewed.getLatitude()
                            )
                            .put(
                                    "longitude",
                                    reviewed.getLongitude()
                            )
                            .put(
                                    "accuracyMeters",
                                    originalCoordinates
                                            ? jsonValue(
                                            point.accuracyMeters()
                                    )
                                            : JSONObject.NULL
                            );

            MedicalProfile profile =
                    input.medicalProfile();

            return new JSONObject()
                    .put("schemaVersion", 1)
                    .put("practiceMode", true)
                    .put(
                            "generatedAt",
                            generatedAt.toString()
                    )
                    .put(
                            "generationSource",
                            generationSource.name()
                    )
                    .put(
                            "generationMessage",
                            generationMessage
                    )
                    .put(
                            "category",
                            input.category()
                    )
                    .put(
                            "description",
                            input.description()
                    )
                    .put(
                            "summary",
                            reviewed.getSummary()
                    )
                    .put(
                            "location",
                            new JSONObject()
                                    .put(
                                            "address",
                                            reviewed
                                                    .getLocationDescription()
                                                    == null
                                                    ? ""
                                                    : reviewed
                                                    .getLocationDescription()
                                    )
                                    .put(
                                            "coordinates",
                                            coordinates
                                    )
                    )
                    .put(
                            "medicalProfile",
                            new JSONObject()
                                    .put(
                                            "allergies",
                                            profile.allergies()
                                    )
                                    .put(
                                            "medications",
                                            profile.medications()
                                    )
                                    .put(
                                            "conditions",
                                            profile.conditions()
                                    )
                    )
                    .put(
                            "structuredReport",
                            new JSONObject()
                                    .put(
                                            "incidentType",
                                            jsonValue(
                                                    reviewed.getIncidentType()
                                            )
                                    )
                                    .put(
                                            "peopleAffected",
                                            jsonValue(
                                                    reviewed.getPeopleAffected()
                                            )
                                    )
                                    .put(
                                            "criticalConditions",
                                            new JSONArray(
                                                    reviewed
                                                            .getCriticalConditions()
                                            )
                                    )
                                    .put(
                                            "hazards",
                                            new JSONArray(
                                                    reviewed.getHazards()
                                            )
                                    )
                                    .put(
                                            "locationDescription",
                                            jsonValue(
                                                    reviewed
                                                            .getLocationDescription()
                                            )
                                    )
                                    .put(
                                            "latitude",
                                            jsonValue(
                                                    reviewed.getLatitude()
                                            )
                                    )
                                    .put(
                                            "longitude",
                                            jsonValue(
                                                    reviewed.getLongitude()
                                            )
                                    )
                                    .put(
                                            "medicalFlags",
                                            new JSONArray(
                                                    reviewed.getMedicalFlags()
                                            )
                                    )
                                    .put(
                                            "missingInformation",
                                            new JSONArray(
                                                    reviewed
                                                            .getMissingInformation()
                                            )
                                    )
                                    .put(
                                            "uncertainties",
                                            new JSONArray(
                                                    reviewed.getUncertainties()
                                            )
                                    )
                    );
        }
    }

    static CompletableFuture<Report> generateIncidentReport(
            Input input,
            IncidentAiService ai
    ) {
        CompletableFuture<Report> result =
                new CompletableFuture<>();

        CompletableFuture.runAsync(
                () -> ai.structureIncident(
                        input.description(),
                        new IncidentAiService.IncidentCallback() {
                            @Override
                            public void onSuccess(
                                    IncidentReport report
                            ) {
                                completeReport(
                                        result,
                                        input,
                                        report,
                                        GenerationSource.AI,
                                        ""
                                );
                            }

                            @Override
                            public void onFallback(
                                    IncidentReport report,
                                    String userMessage
                            ) {
                                completeReport(
                                        result,
                                        input,
                                        report,
                                        GenerationSource.LOCAL_FALLBACK,
                                        userMessage
                                );
                            }

                            @Override
                            public void onFailure(
                                    String message
                            ) {
                                String safeMessage =
                                        message == null
                                                || message.isBlank()
                                                ? "The report could not be generated."
                                                : message;

                                result.completeExceptionally(
                                        new IllegalArgumentException(
                                                safeMessage
                                        )
                                );
                            }
                        }
                )
        ).exceptionally(error -> {
            result.completeExceptionally(error);
            return null;
        });

        return result;
    }

    private static void completeReport(
            CompletableFuture<Report> result,
            Input input,
            IncidentReport report,
            GenerationSource source,
            String message
    ) {
        if (report == null) {
            result.completeExceptionally(
                    new IllegalArgumentException(
                            "The generated report was missing."
                    )
            );
            return;
        }

        result.complete(
                new Report(
                        input,
                        new IncidentReport(report),
                        Instant.now(),
                        source,
                        message
                )
        );
    }

    private static void addMedicalFlag(
            List<String> flags,
            String label,
            String value
    ) {
        if (value == null || value.isEmpty()) {
            return;
        }

        String flag = label + ": " + value;

        if (!flags.contains(flag)) {
            flags.add(flag);
        }
    }

    private static Object jsonValue(
            Object value
    ) {
        return value == null
                ? JSONObject.NULL
                : value;
    }

    private VictimReportDraft() {}
}