package com.example.scenar.ui.victim;

import com.example.scenar.model.DispatchRecord;
import com.example.scenar.model.IncidentReport;
import com.example.scenar.model.SharingConsent;
import com.example.scenar.repository.SimulatedDispatchRepository;

final class PracticeDispatch {
    private final IncidentReport reviewed;
    private final SharingConsent consent;

    PracticeDispatch(IncidentReport reviewed, SharingConsent consent) {
        this.reviewed = new IncidentReport(reviewed);
        this.consent = consent;
    }

    DispatchRecord submit(SimulatedDispatchRepository repository) {
        return repository.submit(reviewed, consent);
    }

    boolean includesLocation() {
        return consent.shareLocation()
                && (reviewed.getLocationDescription() != null || reviewed.getLatitude() != null);
    }

    boolean includesMedicalFlags() {
        return consent.shareMedicalFlags() && !reviewed.getMedicalFlags().isEmpty();
    }

    boolean allowsContactNotification() {
        return consent.notifyEmergencyContacts();
    }
}
