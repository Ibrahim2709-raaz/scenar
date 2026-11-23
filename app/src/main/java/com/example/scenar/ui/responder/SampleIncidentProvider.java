package com.example.scenar.ui.responder;

import com.example.scenar.model.IncidentReport;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
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

    public static List<ResponderIncident> createDemoIncidents(IncidentReport currentReport) {
        List<ResponderIncident> list = new ArrayList<>();

        if (currentReport != null) {
            ResponderIncident live = new ResponderIncident("INC-2026-LIVE", currentReport, "Just now");
            live.addTimeline("Submitted from Victim App", "Validated report transmitted via SCENAR pipeline.");
            list.add(live);
        }

        // Canonical ceiling collapse scenario
        IncidentReport r1 = getCanonicalSampleReport();
        ResponderIncident inc1 = new ResponderIncident("INC-2026-8842", r1, "16:42");
        inc1.setStatus(IncidentStatus.DISPATCHED);
        list.add(inc1);

        // Hazardous chemical scenario
        IncidentReport r2 = new IncidentReport();
        r2.setIncidentType("Chemical hazard");
        r2.setSummary("Strong chemical odor and vapor emitting from Chemistry Lab 301. Students evacuated corridor.");
        r2.setPeopleAffected(0);
        r2.getHazards().addAll(Arrays.asList("Unknown toxic gas / chemical vapor", "Active ventilation shutoff required"));
        r2.setLocationDescription("Chemistry Hall, Room 301, North Campus");
        r2.setLatitude(37.77580);
        r2.setLongitude(-122.41820);
        r2.getMissingInformation().add("Identify chemical container label");
        r2.getUncertainties().add("Spill volume unknown");
        ResponderIncident inc2 = new ResponderIncident("INC-2026-8839", r2, "16:30");
        inc2.setStatus(IncidentStatus.EN_ROUTE);
        list.add(inc2);

        // Medical trauma scenario
        IncidentReport r3 = new IncidentReport();
        r3.setIncidentType("Medical trauma");
        r3.setSummary("Individual collapsed near Campus Recreation quad. Bystander CPR in progress, public AED retrieved.");
        r3.setPeopleAffected(1);
        r3.getCriticalConditions().add("Cardiac event / unresponsive, CPR in progress");
        r3.getMedicalFlags().add("Consented: Cardiac history / pacemaker");
        r3.setLocationDescription("Campus Recreation Quad, West Lawn");
        r3.setLatitude(37.77350);
        r3.setLongitude(-122.42010);
        ResponderIncident inc3 = new ResponderIncident("INC-2026-8825", r3, "16:15");
        inc3.setStatus(IncidentStatus.ON_SCENE);
        list.add(inc3);

        return list;
    }

    public static List<NearbyFacility> getNearbyFacilities() {
        List<NearbyFacility> facilities = new ArrayList<>();
        facilities.add(new NearbyFacility(
                "Metropolitan Trauma Center & ER",
                "Level 1 Trauma Center",
                37.78120, -122.41150,
                "(415) 555-0144",
                "750 Emergency Way"
        ));
        facilities.add(new NearbyFacility(
                "Station 4 Fire & Rescue",
                "Heavy Rescue & HazMat",
                37.77210, -122.42500,
                "(415) 555-0199",
                "220 Civic Center Blvd"
        ));
        facilities.add(new NearbyFacility(
                "St. Jude Urgent Care",
                "Urgent Care Clinic",
                37.77800, -122.42800,
                "(415) 555-0122",
                "410 Mission Ave"
        ));
        return facilities;
    }
}
