package com.example.scenar.ui.responder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.scenar.model.IncidentReport;
import com.example.scenar.model.SharingConsent;
import com.example.scenar.repository.IncidentSharingPolicy;
import com.example.scenar.repository.SimulatedDispatchRepository;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

public class MedicalConsentTest {
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
    public void withheldMedicalConsentHidesMedicalFlags() {
        ResponderIncident incident = queue.submit(reportWithMedicalFlags(),
                new SharingConsent(true, false, false), repository);

        assertTrue(incident.getMedicalFlagsToShow().isEmpty());
        assertTrue(incident.getReport().getMedicalFlags().isEmpty());
        assertTrue(repository.getIncidents().get(0).getReport().getMedicalFlags().isEmpty());
        assertEquals(ResponderCopy.MEDICAL_NOT_SHARED, incident.getMedicalEmptyMessage());
    }

    @Test
    public void withheldMedicalConsentKeepsFlagsOutOfJsonAndBlePreview() {
        ResponderIncident incident = queue.submit(reportWithMedicalFlags(),
                SharingConsent.shareNothing(), repository);

        String ble = BlePayloadPreview.from(incident.getDisplayId(), incident.getReport()).getJson();
        assertFalse(ble.contains("Penicillin"));
        assertFalse(ble.contains("Asthma"));
    }

    @Test
    public void givenMedicalConsentShowsTheFilteredFlags() {
        ResponderIncident incident = queue.submit(reportWithMedicalFlags(),
                new SharingConsent(false, true, false), repository);

        assertEquals(Arrays.asList("Penicillin allergy", "Asthma"), incident.getMedicalFlagsToShow());
    }

    @Test
    public void dashboardShowsExactlyWhatIncidentSharingPolicyLetsThrough() {
        IncidentReport source = reportWithMedicalFlags();
        source.setLatitude(49.2827);
        source.setLongitude(-123.1207);
        for (SharingConsent consent : new SharingConsent[] {
                SharingConsent.shareNothing(),
                SharingConsent.shareReportDetails(),
                new SharingConsent(true, false, true),
                new SharingConsent(false, true, false)}) {
            IncidentReport expected = IncidentSharingPolicy.filterForResponder(source, consent);
            ResponderIncident incident = queue.submit(source, consent, repository);

            assertEquals(expected.getMedicalFlags(), incident.getMedicalFlagsToShow());
            assertEquals(expected.getLatitude(), incident.getLocation().getLatitude());
            assertEquals(expected.getLongitude(), incident.getLocation().getLongitude());
        }
    }

    @Test
    public void consentedButEmptyMedicalFlagsSayNoneReported() {
        ResponderIncident incident = queue.submit(new IncidentReport(),
                SharingConsent.shareReportDetails(), repository);

        assertTrue(incident.getMedicalFlagsToShow().isEmpty());
        assertEquals(ResponderCopy.MEDICAL_NONE_REPORTED, incident.getMedicalEmptyMessage());
    }

    @Test
    public void demoIncidentWithWithheldMedicalConsentShowsNoFlags() {
        queue.ensureDemoIncidents(repository);

        boolean foundWithheld = false;
        for (ResponderIncident incident : queue.sync(repository)) {
            if (!incident.getConsent().shareMedicalFlags()) {
                foundWithheld = true;
                assertTrue(incident.getMedicalFlagsToShow().isEmpty());
            }
        }
        assertTrue(foundWithheld);
    }

    private static IncidentReport reportWithMedicalFlags() {
        IncidentReport report = new IncidentReport();
        report.setIncidentType("Medical incident");
        report.setSummary("Person collapsed.");
        report.setMedicalFlags(Arrays.asList("Penicillin allergy", "Asthma"));
        return report;
    }
}
