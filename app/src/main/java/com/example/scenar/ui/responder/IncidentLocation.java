package com.example.scenar.ui.responder;

import com.example.scenar.model.EmergencyFacility;
import com.example.scenar.model.IncidentReport;

import java.util.Locale;

/**
 * The incident's GPS location exactly as it appears in the filtered dispatch record.
 * No default or placeholder coordinates are ever substituted: when the record has no
 * usable coordinates, every location-based action is unavailable.
 */
public final class IncidentLocation {
    private final Double latitude;
    private final Double longitude;

    private IncidentLocation(Double latitude, Double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public static IncidentLocation of(IncidentReport report) {
        if (report == null) return new IncidentLocation(null, null);
        Double lat = report.getLatitude();
        Double lng = report.getLongitude();
        boolean valid = lat != null && lng != null
                && Double.isFinite(lat) && Double.isFinite(lng)
                && lat >= -90 && lat <= 90
                && lng >= -180 && lng <= 180;
        return valid ? new IncidentLocation(lat, lng) : new IncidentLocation(null, null);
    }

    public boolean isAvailable() {
        return latitude != null && longitude != null;
    }

    /** Null when the location is unavailable. */
    public Double getLatitude() {
        return latitude;
    }

    /** Null when the location is unavailable. */
    public Double getLongitude() {
        return longitude;
    }

    /** "49.28270, -123.12070", or "Location unavailable". */
    public String coordinatesText() {
        if (!isAvailable()) return ResponderCopy.LOCATION_UNAVAILABLE;
        return String.format(Locale.ROOT, "%.5f, %.5f", latitude, longitude);
    }

    public boolean canNavigate() {
        return isAvailable();
    }

    public boolean canCopyCoordinates() {
        return isAvailable();
    }

    public boolean canCalculateFacilityDistance() {
        return isAvailable();
    }

    /** Straight-line distance in km, or null when the incident has no GPS location. */
    public Double distanceKmTo(EmergencyFacility facility) {
        if (!canCalculateFacilityDistance() || facility == null) return null;
        return facility.distanceKmTo(latitude, longitude);
    }

    public String facilityDistanceText(EmergencyFacility facility) {
        Double km = distanceKmTo(facility);
        if (km == null) return ResponderCopy.FACILITY_DISTANCE_UNAVAILABLE;
        return String.format(Locale.ROOT, ResponderCopy.FACILITY_DISTANCE_FORMAT, km);
    }

    /**
     * Rough travel time in minutes from the straight-line distance at
     * {@link #DEMO_TRAVEL_SPEED_KMH}, or null when the incident has no GPS location.
     */
    public Integer etaMinutesTo(EmergencyFacility facility) {
        Double km = distanceKmTo(facility);
        if (km == null) return null;
        return Math.max(1, (int) Math.round(km / DEMO_TRAVEL_SPEED_KMH * 60.0));
    }

    public String facilityEtaText(EmergencyFacility facility) {
        Integer minutes = etaMinutesTo(facility);
        if (minutes == null) return ResponderCopy.FACILITY_ETA_UNAVAILABLE;
        return String.format(Locale.ROOT, ResponderCopy.FACILITY_ETA_FORMAT, minutes);
    }

    /** Assumed speed for the demo ETA estimate. Not based on traffic or roads. */
    public static final double DEMO_TRAVEL_SPEED_KMH = 35.0;
}
