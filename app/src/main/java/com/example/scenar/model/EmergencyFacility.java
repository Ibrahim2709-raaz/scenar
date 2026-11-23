package com.example.scenar.model;

import java.io.Serializable;
import java.time.LocalDate;

public record EmergencyFacility(
        String id,
        String name,
        String address,
        double latitude,
        double longitude,
        String source,
        LocalDate lastReviewed
) implements Serializable {
    private static final double EARTH_RADIUS_KM = 6371.0088;

    public EmergencyFacility {
        requireText(id, "id");
        requireText(name, "name");
        requireText(address, "address");
        requireText(source, "source");
        if (!Double.isFinite(latitude) || latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("latitude must be between -90 and 90");
        }
        if (!Double.isFinite(longitude) || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("longitude must be between -180 and 180");
        }
        if (lastReviewed == null) {
            throw new IllegalArgumentException("lastReviewed is required");
        }
    }

    public double distanceKmTo(double otherLatitude, double otherLongitude) {
        if (!Double.isFinite(otherLatitude) || otherLatitude < -90 || otherLatitude > 90
                || !Double.isFinite(otherLongitude) || otherLongitude < -180
                || otherLongitude > 180) {
            throw new IllegalArgumentException("valid coordinates are required");
        }

        double latitudeDelta = Math.toRadians(otherLatitude - latitude);
        double longitudeDelta = Math.toRadians(otherLongitude - longitude);
        double startLatitude = Math.toRadians(latitude);
        double endLatitude = Math.toRadians(otherLatitude);
        double a = Math.sin(latitudeDelta / 2) * Math.sin(latitudeDelta / 2)
                + Math.cos(startLatitude) * Math.cos(endLatitude)
                * Math.sin(longitudeDelta / 2) * Math.sin(longitudeDelta / 2);
        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }
}
