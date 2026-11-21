package com.example.scenar.ai;

import com.example.scenar.model.IncidentReport;

public class IncidentReportValidator {

    public ValidationResult validate(IncidentReport report) {
        if (report == null) {
            return ValidationResult.invalid("AI returned no report.");
        }

        if (report.getSummary() == null
                || report.getSummary().trim().isEmpty()) {
            return ValidationResult.invalid("Report summary is missing.");
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
            return ValidationResult.invalid("Latitude is invalid.");
        }

        if (report.getLongitude() != null
                && (report.getLongitude() < -180
                || report.getLongitude() > 180)) {
            return ValidationResult.invalid("Longitude is invalid.");
        }

        return ValidationResult.valid();
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