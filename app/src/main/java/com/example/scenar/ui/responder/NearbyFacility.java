package com.example.scenar.ui.responder;

import java.io.Serializable;
import java.util.Locale;

public class NearbyFacility implements Serializable {
    private final String name;
    private final String type;
    private final double latitude;
    private final double longitude;
    private final String phone;
    private final String address;

    public NearbyFacility(String name, String type, double latitude, double longitude, String phone, String address) {
        this.name = name;
        this.type = type;
        this.latitude = latitude;
        this.longitude = longitude;
        this.phone = phone;
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getPhone() {
        return phone;
    }

    public String getAddress() {
        return address;
    }

    public double distanceKm(double fromLat, double fromLng) {
        double lat1 = Math.toRadians(fromLat);
        double lon1 = Math.toRadians(fromLng);
        double lat2 = Math.toRadians(latitude);
        double lon2 = Math.toRadians(longitude);

        double dlat = lat2 - lat1;
        double dlon = lon2 - lon1;

        double a = Math.sin(dlat / 2) * Math.sin(dlat / 2)
                + Math.cos(lat1) * Math.cos(lat2)
                * Math.sin(dlon / 2) * Math.sin(dlon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double r = 6371.0; // Earth radius in km
        return r * c;
    }

    public int etaMinutes(double fromLat, double fromLng) {
        double distKm = distanceKm(fromLat, fromLng);
        // Assume emergency vehicle speed ~35 km/h in urban grid + 1 min dispatch response
        int minutes = (int) Math.round((distKm / 35.0) * 60.0) + 1;
        return Math.max(1, minutes);
    }

    public String getFormattedDistance(double fromLat, double fromLng) {
        double km = distanceKm(fromLat, fromLng);
        return String.format(Locale.ROOT, "%.1f km", km);
    }
}
