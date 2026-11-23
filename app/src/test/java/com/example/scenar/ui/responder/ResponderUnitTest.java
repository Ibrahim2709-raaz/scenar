package com.example.scenar.ui.responder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.example.scenar.model.IncidentReport;

import org.json.JSONException;
import org.junit.Test;

import java.util.List;

public class ResponderUnitTest {

    @Test
    public void testIncidentStatusProgression() {
        IncidentStatus status = IncidentStatus.RECEIVED;
        status = status.next();
        assertEquals(IncidentStatus.DISPATCHED, status);

        status = status.next();
        assertEquals(IncidentStatus.EN_ROUTE, status);

        status = status.next();
        assertEquals(IncidentStatus.ON_SCENE, status);

        status = status.next();
        assertEquals(IncidentStatus.RESOLVED, status);

        // Terminal state remains resolved
        status = status.next();
        assertEquals(IncidentStatus.RESOLVED, status);
    }

    @Test
    public void testPriorityCalculation() {
        IncidentReport report = new IncidentReport();
        report.getCriticalConditions().add("Unresponsive victim");
        assertEquals(PriorityLevel.CRITICAL, PriorityLevel.calculate(report));

        IncidentReport highReport = new IncidentReport();
        highReport.getHazards().add("Active structure fire");
        assertEquals(PriorityLevel.HIGH, PriorityLevel.calculate(highReport));

        IncidentReport mediumReport = new IncidentReport();
        mediumReport.setPeopleAffected(1);
        assertEquals(PriorityLevel.MEDIUM, PriorityLevel.calculate(mediumReport));
    }

    @Test
    public void testCanonicalSampleJsonParsing() throws JSONException {
        IncidentReport report = SampleIncidentProvider.fromJson(SampleIncidentProvider.SAMPLE_REPORT_JSON);

        assertNotNull(report);
        assertEquals("Structural emergency", report.getIncidentType());
        assertEquals(Integer.valueOf(2), report.getPeopleAffected());
        assertEquals(2, report.getCriticalConditions().size());
        assertEquals(2, report.getHazards().size());
        assertEquals("Building B, Room 204, Campus Science Complex", report.getLocationDescription());
        assertEquals(Double.valueOf(37.77492), report.getLatitude());
        assertEquals(Double.valueOf(-122.41941), report.getLongitude());
        assertEquals(2, report.getMedicalFlags().size());
        assertEquals(2, report.getMissingInformation().size());
        assertEquals(1, report.getUncertainties().size());
    }

    @Test
    public void testJsonRoundTrip() throws JSONException {
        IncidentReport original = SampleIncidentProvider.getCanonicalSampleReport();
        String json = SampleIncidentProvider.toJson(original);
        IncidentReport parsed = SampleIncidentProvider.fromJson(json);

        assertEquals(original.getIncidentType(), parsed.getIncidentType());
        assertEquals(original.getSummary(), parsed.getSummary());
        assertEquals(original.getPeopleAffected(), parsed.getPeopleAffected());
        assertEquals(original.getLocationDescription(), parsed.getLocationDescription());
        assertEquals(original.getCriticalConditions().size(), parsed.getCriticalConditions().size());
        assertEquals(original.getHazards().size(), parsed.getHazards().size());
        assertEquals(original.getMedicalFlags().size(), parsed.getMedicalFlags().size());
    }

    @Test
    public void testResponderIncidentLifecycleAndTimeline() {
        IncidentReport report = SampleIncidentProvider.getCanonicalSampleReport();
        ResponderIncident incident = new ResponderIncident("INC-TEST-001", report, "12:00");

        assertEquals(IncidentStatus.RECEIVED, incident.getStatus());
        assertEquals(PriorityLevel.CRITICAL, incident.getPriority());
        assertEquals(1, incident.getTimeline().size());

        // Advance status
        incident.advanceStatus();
        assertEquals(IncidentStatus.DISPATCHED, incident.getStatus());
        assertEquals(2, incident.getTimeline().size());

        // Add note
        incident.addNote("Medic 4 arrived on perimeter.");
        assertEquals(1, incident.getResponderNotes().size());
        assertEquals(3, incident.getTimeline().size());

        // Record notification
        assertFalse(incident.isContactNotificationSent());
        incident.recordNotification("Jane Doe", "Alert dispatched.");
        assertTrue(incident.isContactNotificationSent());
        assertEquals(1, incident.getNotificationLog().size());
        assertEquals(4, incident.getTimeline().size());
    }

    @Test
    public void testNearbyFacilityHaversineDistanceAndEta() {
        NearbyFacility facility = new NearbyFacility(
                "Trauma Center", "Hospital", 37.78120, -122.41150, "555-0100", "123 Main"
        );
        double distKm = facility.distanceKm(37.77492, -122.41941);
        assertTrue("Distance should be approximately 0.5 - 2.0 km", distKm > 0.5 && distKm < 2.0);

        int eta = facility.etaMinutes(37.77492, -122.41941);
        assertTrue("ETA should be between 1 and 15 mins", eta >= 1 && eta <= 15);
    }

    @Test
    public void testBleRescuePacket() {
        IncidentReport report = SampleIncidentProvider.getCanonicalSampleReport();
        BleRescuePacket packet = BleRescuePacket.fromReport("8842", report);

        assertNotNull(packet.getBeaconId());
        assertTrue(packet.getBeaconId().contains("8842"));
        assertFalse(packet.isBroadcasting());

        packet.setBroadcasting(true);
        assertTrue(packet.isBroadcasting());

        String compact = packet.getCompactPayload();
        assertTrue(compact.contains("\"b\":"));
        assertTrue(packet.getPayloadByteSize() > 0 && packet.getPayloadByteSize() < 512);
    }
}
