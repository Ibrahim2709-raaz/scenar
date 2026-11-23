package com.example.scenar.ui.responder;

import com.example.scenar.model.IncidentReport;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class SampleIncidentProvider {

    public static final String SAMPLE_REPORT_JSON = "{\n"
            + "  \"incidentType\": \"Structural emergency\",\n"
            + "  \"summary\": \"Part of the ceiling collapsed in Building B, room 204. Two people affected. One conscious with leg injury, one unresponsive. Smoke in hallway.\",\n"
            + "  \"peopleAffected\": 2,\n"
            + "  \"criticalConditions\": [\n"
            + "    \"One person is unresponsive\",\n"
            + "    \"One person conscious with severe leg injury\"\n"
            + "  ],\n"
            + "  \"hazards\": [\n"
            + "    \"Smoke in hallway\",\n"
            + "    \"Falling debris / unstable ceiling structure\"\n"
            + "  ],\n"
            + "  \"locationDescription\": \"Building B, Room 204, Campus Science Complex\",\n"
            + "  \"latitude\": 37.77492,\n"
            + "  \"longitude\": -122.41941,\n"
            + "  \"medicalFlags\": [\n"
            + "    \"Consented: Penicillin allergy\",\n"
            + "    \"Consented: Asthmatic (inhaler in backpack)\"\n"
            + "  ],\n"
            + "  \"missingInformation\": [\n"
            + "    \"Confirm exact floor accessibility from stairwell\",\n"
            + "    \"Verify if hallway smoke is toxic or electrical fire\"\n"
            + "  ],\n"
            + "  \"uncertainties\": [\n"
            + "    \"Exact headcount inside room 204 requires visual confirmation on scene\"\n"
            + "  ]\n"
            + "}";

    private SampleIncidentProvider() {}

    public static IncidentReport getCanonicalSampleReport() {
        try {
            return fromJson(SAMPLE_REPORT_JSON);
        } catch (JSONException e) {
            IncidentReport fallback = new IncidentReport();
            fallback.setIncidentType("Structural emergency");
            fallback.setSummary("Ceiling collapsed in Building B, room 204.");
            fallback.setPeopleAffected(2);
            fallback.getCriticalConditions().add("One person is unresponsive");
            fallback.getHazards().add("Smoke in hallway");
            fallback.setLocationDescription("Building B, Room 204");
            fallback.setLatitude(37.77492);
            fallback.setLongitude(-122.41941);
            return fallback;
        }
    }

    public static IncidentReport fromJson(String json) throws JSONException {
        if (json == null) throw new JSONException("JSON string is null");
        String trimmed = json.trim();
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        }
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        trimmed = trimmed.trim();

        JSONObject obj = new JSONObject(trimmed);
        IncidentReport report = new IncidentReport();

        if (obj.has("incidentType") && !obj.isNull("incidentType")) {
            report.setIncidentType(obj.getString("incidentType"));
        }
        if (obj.has("summary") && !obj.isNull("summary")) {
            report.setSummary(obj.getString("summary"));
        }
        if (obj.has("peopleAffected") && !obj.isNull("peopleAffected")) {
            report.setPeopleAffected(obj.getInt("peopleAffected"));
        }
        if (obj.has("locationDescription") && !obj.isNull("locationDescription")) {
            report.setLocationDescription(obj.getString("locationDescription"));
        }
        if (obj.has("latitude") && !obj.isNull("latitude")) {
            report.setLatitude(obj.getDouble("latitude"));
        }
        if (obj.has("longitude") && !obj.isNull("longitude")) {
            report.setLongitude(obj.getDouble("longitude"));
        }

        report.setCriticalConditions(jsonArrayToList(obj.optJSONArray("criticalConditions")));
        report.setHazards(jsonArrayToList(obj.optJSONArray("hazards")));
        report.setMedicalFlags(jsonArrayToList(obj.optJSONArray("medicalFlags")));
        report.setMissingInformation(jsonArrayToList(obj.optJSONArray("missingInformation")));
        report.setUncertainties(jsonArrayToList(obj.optJSONArray("uncertainties")));

        return report;
    }

    public static String toJson(IncidentReport report) {
        if (report == null) return "{}";
        try {
            JSONObject obj = new JSONObject();
            obj.put("incidentType", report.getIncidentType() != null ? report.getIncidentType() : JSONObject.NULL);
            obj.put("summary", report.getSummary() != null ? report.getSummary() : JSONObject.NULL);
            obj.put("peopleAffected", report.getPeopleAffected() != null ? report.getPeopleAffected() : JSONObject.NULL);
            obj.put("criticalConditions", new JSONArray(report.getCriticalConditions()));
            obj.put("hazards", new JSONArray(report.getHazards()));
            obj.put("locationDescription", report.getLocationDescription() != null ? report.getLocationDescription() : JSONObject.NULL);
            obj.put("latitude", report.getLatitude() != null ? report.getLatitude() : JSONObject.NULL);
            obj.put("longitude", report.getLongitude() != null ? report.getLongitude() : JSONObject.NULL);
            obj.put("medicalFlags", new JSONArray(report.getMedicalFlags()));
            obj.put("missingInformation", new JSONArray(report.getMissingInformation()));
            obj.put("uncertainties", new JSONArray(report.getUncertainties()));
            return obj.toString(2);
        } catch (JSONException e) {
            return "{}";
        }
    }

    private static List<String> jsonArrayToList(JSONArray array) {
        List<String> list = new ArrayList<>();
        if (array == null) return list;
        for (int i = 0; i < array.length(); i++) {
            try {
                list.add(array.getString(i));
            } catch (JSONException ignored) {}
        }
        return list;
    }
}
