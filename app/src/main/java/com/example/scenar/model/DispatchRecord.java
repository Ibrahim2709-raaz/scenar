package com.example.scenar.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

public final class DispatchRecord implements Serializable {
    private final String incidentId;
    private final IncidentReport report;
    private final SharingConsent consent;
    private final DispatchStatus status;
    private final Instant submittedAt;

    public DispatchRecord(
            String incidentId,
            IncidentReport report,
            SharingConsent consent,
            DispatchStatus status,
            Instant submittedAt
    ) {
        this.incidentId = requireText(incidentId, "incidentId");
        this.report = new IncidentReport(Objects.requireNonNull(report, "report"));
        this.consent = Objects.requireNonNull(consent, "consent");
        this.status = Objects.requireNonNull(status, "status");
        this.submittedAt = Objects.requireNonNull(submittedAt, "submittedAt");
    }

    public String getIncidentId() {
        return incidentId;
    }

    public IncidentReport getReport() {
        return new IncidentReport(report);
    }

    public SharingConsent getConsent() {
        return consent;
    }

    public DispatchStatus getStatus() {
        return status;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public DispatchRecord transitionTo(DispatchStatus next) {
        if (!status.canTransitionTo(next)) {
            throw new IllegalStateException(
                    "Cannot transition dispatch from " + status + " to " + next
            );
        }
        return new DispatchRecord(incidentId, report, consent, next, submittedAt);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }
}
