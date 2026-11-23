package com.example.scenar.ui.responder;

import com.example.scenar.model.IncidentReport;

import java.io.Serializable;
import java.util.Locale;

public class BleRescuePacket implements Serializable {
    public static final String SCENAR_SERVICE_UUID = "0000FEAA-0000-1000-8000-00805F9B34FB";

    private final String beaconId;
    private final String incidentSummary;
    private final int peopleCount;
    private final double latitude;
    private final double longitude;
    private final int rssi;
    private final int meshHop;
    private boolean isBroadcasting;

    public BleRescuePacket(String beaconId, String incidentSummary, int peopleCount,
                           double latitude, double longitude, int rssi, int meshHop) {
        this.beaconId = beaconId;
        this.incidentSummary = incidentSummary;
        this.peopleCount = peopleCount;
        this.latitude = latitude;
        this.longitude = longitude;
        this.rssi = rssi;
        this.meshHop = meshHop;
        this.isBroadcasting = false;
    }

    public static BleRescuePacket fromReport(String id, IncidentReport report) {
        String summary = report.getSummary() != null ? report.getSummary() : "Distress beacon";
        if (summary.length() > 64) {
            summary = summary.substring(0, 61) + "...";
        }
        int people = report.getPeopleAffected() != null ? report.getPeopleAffected() : 1;
        double lat = report.getLatitude() != null ? report.getLatitude() : 37.77492;
        double lng = report.getLongitude() != null ? report.getLongitude() : -122.41941;
        return new BleRescuePacket("SCENAR-BLE-" + id, summary, people, lat, lng, -68, 1);
    }

    public String getBeaconId() {
        return beaconId;
    }

    public String getIncidentSummary() {
        return incidentSummary;
    }

    public int getPeopleCount() {
        return peopleCount;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public int getRssi() {
        return rssi;
    }

    public int getMeshHop() {
        return meshHop;
    }

    public boolean isBroadcasting() {
        return isBroadcasting;
    }

    public void setBroadcasting(boolean broadcasting) {
        isBroadcasting = broadcasting;
    }

    public String getCompactPayload() {
        return String.format(Locale.ROOT,
                "{\"b\":\"%s\",\"s\":\"%s\",\"n\":%d,\"lat\":%.4f,\"lon\":%.4f,\"h\":%d}",
                beaconId, incidentSummary, peopleCount, latitude, longitude, meshHop);
    }

    public int getPayloadByteSize() {
        return getCompactPayload().getBytes().length;
    }
}
