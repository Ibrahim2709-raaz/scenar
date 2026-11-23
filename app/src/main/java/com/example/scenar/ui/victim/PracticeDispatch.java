package com.example.scenar.ui.victim;

import com.example.scenar.model.DispatchRecord;
import com.example.scenar.model.IncidentReport;
import com.example.scenar.model.SharingConsent;
import com.example.scenar.profile.EmergencyProfile;
import com.example.scenar.repository.IncidentSharingPolicy;
import com.example.scenar.repository.SimulatedDispatchRepository;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class PracticeDispatch {
    static final class MissingEmergencyContact extends IllegalArgumentException {
    }

    private final IncidentReport shared;
    private final SharingConsent consent;

    PracticeDispatch(IncidentReport reviewed, EmergencyProfile profile, SharingConsent consent) {
        if (consent.notifyEmergencyContacts() && (!profile.hasEmergencyContact()
                || profile.emergencyContactPhone().chars().filter(Character::isDigit).count() < 7)) {
            throw new MissingEmergencyContact();
        }
        this.consent = consent;
        shared = IncidentSharingPolicy.filterForResponder(reviewed, consent);
        if (consent.shareMedicalFlags()) {
            Set<String> flags = new LinkedHashSet<>(shared.getMedicalFlags());
            addFlag(flags, "Blood type", profile.bloodType());
            addFlag(flags, "Allergies", profile.allergies());
            addFlag(flags, "Medications", profile.medications());
            addFlag(flags, "Medical conditions", profile.medicalConditions());
            addFlag(flags, "Accessibility needs", profile.accessibilityNeeds());
            shared.setMedicalFlags(List.copyOf(flags));
        }
    }

    DispatchRecord submit(SimulatedDispatchRepository repository) {
        return repository.submit(shared, consent);
    }

    IncidentReport sharedReport() {
        return new IncidentReport(shared);
    }

    boolean allowsContactNotification() {
        return consent.notifyEmergencyContacts();
    }

    private static void addFlag(Set<String> flags, String label, String value) {
        if (!value.isEmpty()) flags.add(label + ": " + value);
    }
}
