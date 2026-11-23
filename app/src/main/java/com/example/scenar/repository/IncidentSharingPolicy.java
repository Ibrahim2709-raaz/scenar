package com.example.scenar.repository;

import com.example.scenar.model.IncidentReport;
import com.example.scenar.model.SharingConsent;

import java.util.Collections;
import java.util.Objects;

public final class IncidentSharingPolicy {
    private IncidentSharingPolicy() {
    }

    public static IncidentReport filterForResponder(
            IncidentReport source,
            SharingConsent consent
    ) {
        IncidentReport filtered = new IncidentReport(
                Objects.requireNonNull(source, "source")
        );
        Objects.requireNonNull(consent, "consent");

        if (!consent.shareLocation()) {
            filtered.setLocationDescription(null);
            filtered.setLatitude(null);
            filtered.setLongitude(null);
        }
        if (!consent.shareMedicalFlags()) {
            filtered.setMedicalFlags(Collections.emptyList());
        }
        return filtered;
    }
}
