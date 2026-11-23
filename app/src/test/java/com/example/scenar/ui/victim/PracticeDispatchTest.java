package com.example.scenar.ui.victim;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.scenar.model.IncidentReport;
import com.example.scenar.model.SharingConsent;
import com.example.scenar.repository.SimulatedDispatchRepository;
import com.example.scenar.ui.responder.ResponderQueue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

public class PracticeDispatchTest {
    private final SimulatedDispatchRepository repository = SimulatedDispatchRepository.getInstance();
    private final ResponderQueue responder = ResponderQueue.getInstance();

    @Before
    @After
    public void clearDispatch() {
        repository.clear();
        responder.reset();
    }

    @Test
    public void uncheckedConsentWithholdsLocationAndMedicalFlagsAndDisablesNotification() {
        IncidentReport reviewed = new IncidentReport();
        reviewed.setSummary("Reviewed practice report");
        reviewed.setHazards(Collections.singletonList("Wet floor"));
        reviewed.setMedicalFlags(Collections.singletonList("Allergies: Latex"));
        reviewed.setLocationDescription("Practice Hall");
        reviewed.setLatitude(50.0);
        reviewed.setLongitude(-120.0);

        PracticeDispatch dispatch = new PracticeDispatch(reviewed, SharingConsent.shareNothing());
        var record = dispatch.submit(repository);
        var incident = responder.sync(repository).get(0);

        assertFalse(dispatch.includesMedicalFlags());
        assertEquals(record.getIncidentId(), incident.getIncidentId());
        assertEquals(SharingConsent.shareNothing(), incident.getConsent());
        assertTrue(incident.getMedicalFlagsToShow().isEmpty());
        assertFalse(incident.canSimulateNotification());
        assertNull(incident.getReport().getLocationDescription());
        assertNull(incident.getReport().getLatitude());
        assertNull(incident.getReport().getLongitude());
        assertEquals(reviewed.getSummary(), incident.getReport().getSummary());
        assertEquals(reviewed.getHazards(), incident.getReport().getHazards());
        assertEquals(1, reviewed.getMedicalFlags().size());
    }

    @Test
    public void selectedConsentSharesReviewedSnapshotAndEnablesResponderNotification() {
        IncidentReport reviewed = new IncidentReport();
        reviewed.setMedicalFlags(Collections.singletonList("Allergies: Latex"));
        reviewed.setLocationDescription("Practice Hall");
        reviewed.setLatitude(50.0);
        reviewed.setLongitude(-120.0);
        SharingConsent consent = new SharingConsent(true, true, true);
        PracticeDispatch dispatch = new PracticeDispatch(reviewed, consent);

        reviewed.getMedicalFlags().clear();
        var record = dispatch.submit(repository);
        var incident = responder.sync(repository).get(0);

        assertTrue(dispatch.includesMedicalFlags());
        assertEquals(record.getIncidentId(), incident.getIncidentId());
        assertEquals(consent, incident.getConsent());
        assertEquals(Collections.singletonList("Allergies: Latex"), incident.getMedicalFlagsToShow());
        assertEquals("Practice Hall", incident.getReport().getLocationDescription());
        assertEquals(Double.valueOf(50.0), incident.getReport().getLatitude());
        assertEquals(Double.valueOf(-120.0), incident.getReport().getLongitude());
        assertTrue(incident.canSimulateNotification());
        assertFalse(incident.hasSimulatedNotifications());
        assertEquals(1, responder.sync(repository).size());
    }

    @Test
    public void contactConsentIsIndependentOfMedicalConsent() {
        IncidentReport reviewed = new IncidentReport();
        reviewed.setMedicalFlags(Collections.singletonList("Allergies: Latex"));

        var contactOnly = new PracticeDispatch(reviewed, new SharingConsent(false, false, true))
                .submit(repository);
        var medicalOnly = new PracticeDispatch(reviewed, new SharingConsent(false, true, false))
                .submit(repository);
        var incidents = responder.sync(repository);

        assertEquals(medicalOnly.getIncidentId(), incidents.get(0).getIncidentId());
        assertFalse(incidents.get(0).canSimulateNotification());
        assertEquals(reviewed.getMedicalFlags(), incidents.get(0).getMedicalFlagsToShow());
        assertEquals(contactOnly.getIncidentId(), incidents.get(1).getIncidentId());
        assertTrue(incidents.get(1).canSimulateNotification());
        assertTrue(incidents.get(1).getMedicalFlagsToShow().isEmpty());
    }
}
