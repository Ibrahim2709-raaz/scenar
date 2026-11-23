package com.example.scenar.ui.victim;

import com.example.scenar.model.IncidentReport;

final class PracticeDispatch {
    private final IncidentReport sharedReport;
    private final boolean contactNotificationSimulated;

    PracticeDispatch(IncidentReport reviewed, boolean shareMedicalFlags, boolean simulateContactNotification) {
        sharedReport = new IncidentReport(reviewed);
        if (!shareMedicalFlags) sharedReport.setMedicalFlags(null);
        contactNotificationSimulated = simulateContactNotification;
    }

    IncidentReport sharedReport() {
        return new IncidentReport(sharedReport);
    }

    boolean includesMedicalFlags() {
        return !sharedReport.getMedicalFlags().isEmpty();
    }

    boolean contactNotificationSimulated() {
        return contactNotificationSimulated;
    }
}
