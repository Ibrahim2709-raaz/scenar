package com.example.scenar.ui.responder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.scenar.model.EmergencyFacility;
import com.example.scenar.model.IncidentReport;
import com.example.scenar.model.SharingConsent;
import com.example.scenar.repository.SimulatedDispatchRepository;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class MissingCoordinatesTest {
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
    public void noCoordinatesMeansLocationUnavailableAndEveryActionOff() {
        IncidentLocation location = IncidentLocation.of(reportWithAddressOnly());

        assertFalse(location.isAvailable());
        assertNull(location.getLatitude());
        assertNull(location.getLongitude());
        assertEquals("Location unavailable", location.coordinatesText());
        assertFalse(location.canNavigate());
        assertFalse(location.canCopyCoordinates());
        assertFalse(location.canCalculateFacilityDistance());
    }

    @Test
    public void facilityDistanceAndEtaAreUnavailableWithoutCoordinates() {
        IncidentLocation location = IncidentLocation.of(reportWithAddressOnly());

        for (EmergencyFacility facility : DemoFacilities.all()) {
            assertNull(location.distanceKmTo(facility));
            assertEquals(ResponderCopy.FACILITY_DISTANCE_UNAVAILABLE,
                    location.facilityDistanceText(facility));
            assertNull(location.etaMinutesTo(facility));
            assertEquals(ResponderCopy.FACILITY_ETA_UNAVAILABLE,
                    location.facilityEtaText(facility));
        }
    }

    @Test
    public void halfACoordinateOrAnInvalidOneIsTreatedAsMissing() {
        IncidentReport latitudeOnly = new IncidentReport();
        latitudeOnly.setLatitude(49.28);
        assertFalse(IncidentLocation.of(latitudeOnly).isAvailable());

        IncidentReport outOfRange = new IncidentReport();
        outOfRange.setLatitude(123.0);
        outOfRange.setLongitude(10.0);
        assertFalse(IncidentLocation.of(outOfRange).isAvailable());

        IncidentReport notANumber = new IncidentReport();
        notANumber.setLatitude(Double.NaN);
        notANumber.setLongitude(10.0);
        assertFalse(IncidentLocation.of(notANumber).isAvailable());

        assertFalse(IncidentLocation.of(null).isAvailable());
    }

    @Test
    public void noSanFranciscoOrOtherFallbackCoordinatesAreUsed() {
        IncidentLocation location = IncidentLocation.of(reportWithAddressOnly());
        EmergencyFacility nearest = DemoFacilities.all().get(0);

        // The old code fell back to 37.77492, -122.41941. Nothing may be computed now.
        assertNull(location.distanceKmTo(nearest));
        assertNull(location.etaMinutesTo(nearest));
        assertFalse(location.coordinatesText().contains("37.7"));
        assertFalse(BlePayloadPreview.from("INC-TEST", reportWithAddressOnly())
                .getJson().contains("37.7"));
    }

    @Test
    public void submittedIncidentWithoutGpsKeepsNoCoordinates() {
        ResponderIncident incident = queue.submit(reportWithAddressOnly(),
                SharingConsent.shareReportDetails(), repository);

        assertNull(incident.getReport().getLatitude());
        assertNull(incident.getReport().getLongitude());
        assertFalse(incident.getLocation().isAvailable());
    }

    @Test
    public void withheldLocationConsentRemovesCoordinates() {
        IncidentReport report = reportWithAddressOnly();
        report.setLatitude(49.2827);
        report.setLongitude(-123.1207);

        ResponderIncident incident = queue.submit(report,
                new SharingConsent(false, true, false), repository);

        assertFalse(incident.getLocation().isAvailable());
        assertEquals("Location unavailable", incident.getLocation().coordinatesText());
    }

    @Test
    public void demoIncidentWithoutGpsStaysWithoutCoordinates() {
        queue.ensureDemoIncidents(repository);

        boolean foundNoGpsDemo = false;
        for (ResponderIncident incident : queue.sync(repository)) {
            if (incident.getReport().getMissingInformation().contains("GPS location")) {
                foundNoGpsDemo = true;
                assertFalse(incident.getLocation().isAvailable());
            }
        }
        assertTrue("The demo set should include an incident without GPS", foundNoGpsDemo);
    }

    @Test
    public void blePreviewLeavesOutLocationWhenCoordinatesAreMissing() {
        BlePayloadPreview preview = BlePayloadPreview.from("INC-TEST", reportWithAddressOnly());

        assertFalse(preview.includesLocation());
        assertFalse(preview.getJson().contains("\"lat\""));
        assertFalse(preview.getJson().contains("\"lon\""));
    }

    @Test
    public void coordinatesAreShownWhenPresent() {
        IncidentReport report = reportWithAddressOnly();
        report.setLatitude(49.2827);
        report.setLongitude(-123.1207);
        IncidentLocation location = IncidentLocation.of(report);

        assertTrue(location.canNavigate());
        assertTrue(location.canCopyCoordinates());
        assertEquals("49.28270, -123.12070", location.coordinatesText());
        assertTrue(BlePayloadPreview.from("INC-TEST", report).includesLocation());
    }

    private static IncidentReport reportWithAddressOnly() {
        IncidentReport report = new IncidentReport();
        report.setIncidentType("Fire or smoke");
        report.setSummary("Smoke near a stairwell.");
        report.setLocationDescription("Near the north stairwell");
        return report;
    }
}
