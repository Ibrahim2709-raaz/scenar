package com.example.scenar.ai;

import com.example.scenar.model.IncidentReport;

public class IncidentReportValidator {

    public ValidationResult validate(IncidentReport report) {
        if (report == null) {
            return ValidationResult.invalid(
                    "AI returned no incident report."
            );
        }

        if (isBlank(report.getSummary())) {
            return ValidationResult.invalid(
                    "The incident summary is missing."
            );
        }

        if (report.getPeopleAffected() != null
                && report.getPeopleAffected() < 0) {
            return ValidationResult.invalid(
                    "People affected cannot be negative."
            );
        }

        if (report.getLatitude() != null
                && (report.getLatitude() < -90
                || report.getLatitude() > 90)) {
            return ValidationResult.invalid(
                    "Latitude must be between -90 and 90."
            );
        }

        if (report.getLongitude() != null
                && (report.getLongitude() < -180
                || report.getLongitude() > 180)) {
            return ValidationResult.invalid(
                    "Longitude must be between -180 and 180."
            );
        }

        if (report.getCriticalConditions() == null
                || report.getHazards() == null
                || report.getMedicalFlags() == null
                || report.getMissingInformation() == null
                || report.getUncertainties() == null) {
            return ValidationResult.invalid(
                    "One or more report lists are missing."
            );
        }

        return ValidationResult.valid();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static class ValidationResult {
        private final boolean valid;
        private final String message;

        private ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public static ValidationResult valid() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult invalid(String message) {
            return new ValidationResult(false, message);
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }
    }
}