package com.example.scenar.ui.victim;

import com.example.scenar.model.IncidentReport;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

final class IncidentReview {
    record Location(String address, Double latitude, Double longitude) {}

    enum Field {
        SUMMARY("Report summary", "Enter a summary", 4),
        TYPE("Incident type", "Leave blank if unknown", 1),
        PEOPLE("People affected", "Number of people, or blank", 1),
        CONDITIONS("Critical conditions", "One condition per line", 2),
        HAZARDS("Hazards", "One hazard per line", 2),
        LOCATION("Address or landmark", "Leave blank if location is unknown", 1),
        LATITUDE("Latitude", "Decimal latitude, or blank", 1),
        LONGITUDE("Longitude", "Decimal longitude, or blank", 1),
        MEDICAL("Medical flags", "One medical flag per line", 2),
        MISSING("Missing information", "One missing detail per line", 2),
        UNCERTAINTIES("Uncertainties", "One uncertainty per line", 2);

        final String label;
        final String hint;
        final int lines;

        Field(String label, String hint, int lines) {
            this.label = label;
            this.hint = hint;
            this.lines = lines;
        }
    }

    static final class InvalidField extends IllegalArgumentException {
        final Field field;

        InvalidField(Field field, String message) {
            super(message);
            this.field = field;
        }
    }

    private final EnumMap<Field, String> edits = new EnumMap<>(Field.class);

    IncidentReview(IncidentReport initial) {
        setInitial(Field.SUMMARY, initial.getSummary());
        setInitial(Field.TYPE, initial.getIncidentType());
        setInitial(Field.PEOPLE, initial.getPeopleAffected());
        setInitial(Field.CONDITIONS, String.join("\n", initial.getCriticalConditions()));
        setInitial(Field.HAZARDS, String.join("\n", initial.getHazards()));
        setInitial(Field.LOCATION, initial.getLocationDescription());
        setInitial(Field.LATITUDE, initial.getLatitude());
        setInitial(Field.LONGITUDE, initial.getLongitude());
        setInitial(Field.MEDICAL, String.join("\n", initial.getMedicalFlags()));
        setInitial(Field.MISSING, String.join("\n", initial.getMissingInformation()));
        setInitial(Field.UNCERTAINTIES, String.join("\n", initial.getUncertainties()));
    }

    private void setInitial(Field field, Object value) {
        set(field, value == null ? "" : value.toString());
    }

    String get(Field field) {
        return edits.get(field);
    }

    void set(Field field, String value) {
        edits.put(field, value);
    }

    IncidentReport toReport() {
        IncidentReport result = new IncidentReport();
        String summary = value(Field.SUMMARY);
        if (summary == null) throw new InvalidField(Field.SUMMARY, "Enter a summary before continuing.");
        result.setSummary(summary);
        result.setIncidentType(value(Field.TYPE));
        result.setPeopleAffected(peopleAffected());
        result.setCriticalConditions(lines(Field.CONDITIONS));
        result.setHazards(lines(Field.HAZARDS));
        Location location = location();
        result.setLocationDescription(location.address());
        result.setLatitude(location.latitude());
        result.setLongitude(location.longitude());
        result.setMedicalFlags(lines(Field.MEDICAL));
        result.setMissingInformation(lines(Field.MISSING));
        result.setUncertainties(lines(Field.UNCERTAINTIES));
        return result;
    }

    Location location() {
        Double latitude = coordinate(Field.LATITUDE, 90);
        Double longitude = coordinate(Field.LONGITUDE, 180);
        if ((latitude == null) != (longitude == null)) {
            throw new InvalidField(latitude == null ? Field.LATITUDE : Field.LONGITUDE,
                    "Enter both coordinates or clear both.");
        }
        return new Location(value(Field.LOCATION), latitude, longitude);
    }

    private Integer peopleAffected() {
        String value = value(Field.PEOPLE);
        if (value == null) return null;
        try {
            int count = Integer.parseInt(value);
            if (count >= 0) return count;
        } catch (NumberFormatException ignored) {
            // Use the same message for malformed and out-of-range numbers.
        }
        throw new InvalidField(Field.PEOPLE, "Enter a non-negative whole number or leave it blank.");
    }

    private Double coordinate(Field field, int bound) {
        String value = value(field);
        if (value == null) return null;
        try {
            double coordinate = Double.parseDouble(value);
            if (Math.abs(coordinate) <= bound) return coordinate;
        } catch (NumberFormatException ignored) {
            // Report the expected range below.
        }
        throw new InvalidField(field, "Enter a decimal number from -" + bound + " to " + bound + ".");
    }

    private List<String> lines(Field field) {
        List<String> values = new ArrayList<>();
        for (String line : get(field).split("\\R")) {
            String value = line.trim();
            if (!value.isEmpty()) values.add(value);
        }
        return values;
    }

    private String value(Field field) {
        String value = get(field).trim();
        return value.isEmpty() ? null : value;
    }
}
