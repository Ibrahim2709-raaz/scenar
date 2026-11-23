package com.example.scenar.ui.responder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.example.scenar.model.DispatchStatus;
import com.example.scenar.model.EmergencyFacility;
import com.example.scenar.model.IncidentReport;
import com.example.scenar.model.SharingConsent;
import com.example.scenar.repository.SimulatedDispatchRepository;

import org.json.JSONException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class ResponderUnitTest {
    private final SimulatedDispatchRepository repository = SimulatedDispatchRepository.getInstance();
    private ResponderQueue queue;

    @Before
    public void setUp() {
        repository.clear();
        queue = new ResponderQueue();
    }

    @After
    public void tearDown() {
        repository.clear();
    }

    @Test
    public void advanceFollowsSharedDispatchStatusOrder() {
        assertEquals(DispatchStatus.REVIEWING, DispatchStatusStyle.next(DispatchStatus.SUBMITTED));
        assertEquals(DispatchStatus.DISPATCHED_SIMULATION, DispatchStatusStyle.next(DispatchStatus.REVIEWING));
        assertEquals(DispatchStatus.CLOSED, DispatchStatusStyle.next(DispatchStatus.DISPATCHED_SIMULATION));
        assertEquals(null, DispatchStatusStyle.next(DispatchStatus.CLOSED));

        for (DispatchStatus status : DispatchStatus.values()) {
            DispatchStatus next = DispatchStatusStyle.next(status);
            if (next != null) assertTrue(status + " -> " + next, status.canTransitionTo(next));
        }
    }

    @Test
    public void statusChangesGoThroughSharedRepository() {
        ResponderIncident incident = queue.submit(
                SampleIncidentProvider.getCanonicalSampleReport(),
                SharingConsent.shareReportDetails(), repository);
        assertEquals(DispatchStatus.SUBMITTED, incident.getStatus());

        assertTrue(queue.updateStatus(repository, incident, DispatchStatus.REVIEWING));
        assertEquals(DispatchStatus.REVIEWING, incident.getStatus());
        assertEquals(DispatchStatus.REVIEWING, repository.getIncidents().get(0).getStatus());

        // Not allowed by DispatchStatus: REVIEWING cannot go back to SUBMITTED.
        assertFalse(queue.updateStatus(repository, incident, DispatchStatus.SUBMITTED));
        assertEquals(DispatchStatus.REVIEWING, incident.getStatus());

        assertTrue(queue.updateStatus(repository, incident, DispatchStatus.DISPATCHED_SIMULATION));
        assertTrue(queue.updateStatus(repository, incident, DispatchStatus.CLOSED));
        assertFalse(queue.updateStatus(repository, incident, DispatchStatus.REVIEWING));
    }

    @Test
    public void syncReturnsTrackedIncidentsNewestFirst() {
        ResponderIncident first = queue.submit(new IncidentReport(), SharingConsent.shareNothing(), repository);
        ResponderIncident second = queue.submit(new IncidentReport(), SharingConsent.shareNothing(), repository);

        List<ResponderIncident> synced = queue.sync(repository);
        assertEquals(2, synced.size());
        assertSame(second, synced.get(0));
        assertSame(first, synced.get(1));
    }

    @Test
    public void victimHandoffIsSubmittedOnlyOnce() {
        IncidentReport report = new IncidentReport();
        report.setSummary("Test handoff");

        assertNotNull(queue.ingestVictimHandoff(report, repository));
        assertEquals(null, queue.ingestVictimHandoff(report, repository));
        assertEquals(1, repository.getIncidents().size());
    }

    @Test
    public void demoIncidentsAreSeededOnceAndMarkedAsDemo() {
        queue.ensureDemoIncidents(repository);
        int seeded = repository.getIncidents().size();
        queue.ensureDemoIncidents(repository);

        assertEquals(seeded, repository.getIncidents().size());
        for (ResponderIncident incident : queue.sync(repository)) {
            assertTrue(incident.isDemo());
        }
    }

    @Test
    public void priorityCalculation() {
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
    public void notesAreAddedToTheLocalTimeline() {
        ResponderIncident incident = queue.submit(new IncidentReport(),
                SharingConsent.shareNothing(), repository);
        int before = incident.getTimeline().size();

        incident.addNote("Perimeter set up.");
        incident.addNote("   ");

        assertEquals(1, incident.getResponderNotes().size());
        assertEquals(before + 1, incident.getTimeline().size());
    }

    @Test
    public void facilityDistanceUsesSharedEmergencyFacility() {
        IncidentReport report = new IncidentReport();
        report.setLatitude(37.77492);
        report.setLongitude(-122.41941);
        IncidentLocation location = IncidentLocation.of(report);

        List<EmergencyFacility> nearby = DemoFacilities.repository()
                .findNearby(location.getLatitude(), location.getLongitude(), 3);
        assertEquals(3, nearby.size());
        Double km = location.distanceKmTo(nearby.get(0));
        assertNotNull(km);
        assertTrue("Nearest demo facility should be within 2 km", km > 0 && km < 2.0);
        assertTrue(location.facilityDistanceText(nearby.get(0)).contains("km"));
    }

    @Test
    public void canonicalSampleJsonParsing() throws JSONException {
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
    public void jsonRoundTrip() throws JSONException {
        IncidentReport original = SampleIncidentProvider.getCanonicalSampleReport();
        IncidentReport parsed = SampleIncidentProvider.fromJson(SampleIncidentProvider.toJson(original));

        assertEquals(original.getIncidentType(), parsed.getIncidentType());
        assertEquals(original.getSummary(), parsed.getSummary());
        assertEquals(original.getPeopleAffected(), parsed.getPeopleAffected());
        assertEquals(original.getLocationDescription(), parsed.getLocationDescription());
        assertEquals(original.getCriticalConditions().size(), parsed.getCriticalConditions().size());
        assertEquals(original.getHazards().size(), parsed.getHazards().size());
        assertEquals(original.getMedicalFlags().size(), parsed.getMedicalFlags().size());
    }
}
