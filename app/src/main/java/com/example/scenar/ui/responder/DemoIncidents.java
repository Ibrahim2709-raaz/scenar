package com.example.scenar.ui.responder;

import com.example.scenar.model.DispatchStatus;
import com.example.scenar.model.IncidentReport;
import com.example.scenar.model.SharingConsent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Fictional incidents that fill the dashboard for demos. Each one goes through
 * SimulatedDispatchRepository with its own SharingConsent, so the dashboard shows
 * only what IncidentSharingPolicy lets through.
 */
final class DemoIncidents {
    private DemoIncidents() {
    }

    static final class Demo {
        final IncidentReport report;
        final SharingConsent consent;
        final List<DispatchStatus> advanceTo;

        Demo(IncidentReport report, SharingConsent consent, DispatchStatus... advanceTo) {
            this.report = report;
            this.consent = consent;
            this.advanceTo = Collections.unmodifiableList(Arrays.asList(advanceTo));
        }
    }

    static List<Demo> all() {
        List<Demo> demos = new ArrayList<>();

        // Shares location, medical details and allows the notification simulation.
        IncidentReport collapse = new IncidentReport();
        collapse.setIncidentType("Structural emergency");
        collapse.setSummary("Part of a ceiling collapsed in Building B, room 204. Two people"
                + " affected: one conscious with a leg injury, one unresponsive. Smoke in"
                + " the hallway.");
        collapse.setPeopleAffected(2);
        collapse.setCriticalConditions(Arrays.asList(
                "One person is unresponsive",
                "One person conscious with a severe leg injury"));
        collapse.setHazards(Arrays.asList("Smoke in hallway", "Unstable ceiling structure"));
        collapse.setLocationDescription("Building B, Room 204 (fictional demo location)");
        collapse.setLatitude(37.77492);
        collapse.setLongitude(-122.41941);
        collapse.setMedicalFlags(Arrays.asList("Penicillin allergy", "Asthma (inhaler in backpack)"));
        collapse.setMissingInformation(Arrays.asList("Floor access from stairwell"));
        collapse.setUncertainties(Arrays.asList("Exact number of people in room 204"));
        demos.add(new Demo(collapse, new SharingConsent(true, true, true),
                DispatchStatus.REVIEWING));

        // Shares location only.
        IncidentReport chemical = new IncidentReport();
        chemical.setIncidentType("Chemical hazard");
        chemical.setSummary("Strong chemical odour from Chemistry Lab 301. Corridor evacuated.");
        chemical.setHazards(Arrays.asList("Unknown chemical vapour"));
        chemical.setLocationDescription("Chemistry Hall, Room 301 (fictional demo location)");
        chemical.setLatitude(37.77580);
        chemical.setLongitude(-122.41820);
        chemical.setMissingInformation(Arrays.asList("Number of people affected",
                "Chemical container label"));
        chemical.setUncertainties(Arrays.asList("Spill volume unknown"));
        demos.add(new Demo(chemical, new SharingConsent(true, false, false),
                DispatchStatus.REVIEWING, DispatchStatus.DISPATCHED_SIMULATION));

        // Has medical details, but the reporter withheld medical consent.
        IncidentReport medical = new IncidentReport();
        medical.setIncidentType("Medical incident");
        medical.setSummary("Person collapsed on the recreation lawn. A bystander is giving CPR.");
        medical.setPeopleAffected(1);
        medical.setCriticalConditions(Arrays.asList("Unresponsive, CPR in progress"));
        medical.setLocationDescription("Recreation lawn, west side (fictional demo location)");
        medical.setLatitude(37.77350);
        medical.setLongitude(-122.42010);
        medical.setMedicalFlags(Arrays.asList("Pacemaker"));
        demos.add(new Demo(medical, new SharingConsent(true, false, true)));

        // No GPS location at all.
        IncidentReport noGps = new IncidentReport();
        noGps.setIncidentType("Fire or smoke");
        noGps.setSummary("Caller smells smoke near a stairwell but could not share a location.");
        noGps.setLocationDescription("Near the north stairwell (no address given)");
        noGps.setHazards(Arrays.asList("Smoke"));
        noGps.setMissingInformation(Arrays.asList("GPS location", "Building name",
                "Number of people affected"));
        demos.add(new Demo(noGps, new SharingConsent(true, true, false)));

        return demos;
    }
}
