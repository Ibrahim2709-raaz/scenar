package com.example.scenar.ui.responder;

import com.example.scenar.model.IncidentReport;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.nio.charset.StandardCharsets;

/**
 * A preview of the compact JSON a future offline BLE mode could carry. It is shown on
 * screen only; nothing is transmitted and Bluetooth is not used.
 *
 * The JSON is built with Gson, so quotes, backslashes, newlines and other characters in
 * user text are escaped. Unknown values are left out rather than guessed, and location
 * is included only when the filtered record has real GPS coordinates.
 */
public final class BlePayloadPreview {
    static final int MAX_SUMMARY_CODE_POINTS = 64;
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private final String json;
    private final boolean includesLocation;

    private BlePayloadPreview(String json, boolean includesLocation) {
        this.json = json;
        this.includesLocation = includesLocation;
    }

    public static BlePayloadPreview from(String incidentId, IncidentReport report) {
        JsonObject payload = new JsonObject();
        payload.addProperty("demo", true);
        payload.addProperty("id", incidentId);
        if (report != null) {
            if (!isBlank(report.getIncidentType())) {
                payload.addProperty("type", report.getIncidentType());
            }
            if (!isBlank(report.getSummary())) {
                payload.addProperty("s", truncate(report.getSummary(), MAX_SUMMARY_CODE_POINTS));
            }
            if (report.getPeopleAffected() != null) {
                payload.addProperty("n", report.getPeopleAffected());
            }
        }
        IncidentLocation location = IncidentLocation.of(report);
        if (location.isAvailable()) {
            payload.addProperty("lat", round4(location.getLatitude()));
            payload.addProperty("lon", round4(location.getLongitude()));
        }
        return new BlePayloadPreview(GSON.toJson(payload), location.isAvailable());
    }

    public String getJson() {
        return json;
    }

    public boolean includesLocation() {
        return includesLocation;
    }

    /** Size of the JSON in bytes when encoded as UTF-8. */
    public int getUtf8ByteSize() {
        return json.getBytes(StandardCharsets.UTF_8).length;
    }

    /** Shortens text to at most {@code max} code points without splitting a character. */
    static String truncate(String text, int max) {
        if (text.codePointCount(0, text.length()) <= max) return text;
        int end = text.offsetByCodePoints(0, max - 1);
        return text.substring(0, end) + "…";
    }

    private static double round4(double value) {
        return Math.round(value * 10_000d) / 10_000d;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
