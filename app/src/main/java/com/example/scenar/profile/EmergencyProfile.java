package com.example.scenar.profile;

import java.util.Objects;

public record EmergencyProfile(
        String preferredName,
        String dateOfBirth,
        String bloodType,
        String allergies,
        String medications,
        String medicalConditions,
        String accessibilityNeeds,
        String emergencyContactName,
        String emergencyContactRelationship,
        String emergencyContactPhone
) {
    private static final int PROFILE_FIELD_COUNT = 10;

    public EmergencyProfile {
        preferredName = normalize(preferredName);
        dateOfBirth = normalize(dateOfBirth);
        bloodType = normalize(bloodType);
        allergies = normalize(allergies);
        medications = normalize(medications);
        medicalConditions = normalize(medicalConditions);
        accessibilityNeeds = normalize(accessibilityNeeds);
        emergencyContactName = normalize(emergencyContactName);
        emergencyContactRelationship =
                normalize(emergencyContactRelationship);
        emergencyContactPhone =
                normalize(emergencyContactPhone);
    }

    public static EmergencyProfile empty() {
        return new EmergencyProfile(
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                ""
        );
    }

    public int completionPercentage() {
        int completed = 0;

        for (String value : new String[]{
                preferredName,
                dateOfBirth,
                bloodType,
                allergies,
                medications,
                medicalConditions,
                accessibilityNeeds,
                emergencyContactName,
                emergencyContactRelationship,
                emergencyContactPhone
        }) {
            if (!value.isEmpty()) {
                completed++;
            }
        }

        return completed * 100 / PROFILE_FIELD_COUNT;
    }

    public boolean isEmpty() {
        return completionPercentage() == 0;
    }

    public boolean hasEmergencyContact() {
        return !emergencyContactName.isEmpty()
                && !emergencyContactPhone.isEmpty();
    }

    public boolean hasMedicalInformation() {
        return !bloodType.isEmpty()
                || !allergies.isEmpty()
                || !medications.isEmpty()
                || !medicalConditions.isEmpty();
    }

    private static String normalize(String value) {
        return Objects.requireNonNullElse(value, "").trim();
    }

    @Override
    public String toString() {
        /*
         * Never expose medical or contact details through logs,
         * crash messages or debugger-generated strings.
         */
        return "EmergencyProfile{completion="
                + completionPercentage()
                + "%}";
    }
}