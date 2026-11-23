package com.example.scenar.ui.victim;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.example.scenar.model.IncidentReport;

import org.junit.Test;

public class VictimMapSearchTest {
    @Test
    public void routingUsesCorrectedLocationWithoutRequiringTheRestOfTheReport() {
        IncidentReport report = new IncidentReport();
        report.setLocationDescription("Unconfirmed location");
        report.setLatitude(25.0);
        report.setLongitude(55.0);
        IncidentReview review = new IncidentReview(report);
        review.set(IncidentReview.Field.LATITUDE, "25.25");
        review.set(IncidentReview.Field.LONGITUDE, "55.35");

        VictimMapSearch hospitals = VictimMapSearch.forHospitals(review.location());
        VictimMapSearch incident = VictimMapSearch.forIncident(review.location());

        assertEquals("25.25,55.35", hospitals.center());
        assertEquals("hospital", hospitals.query());
        assertEquals("25.25,55.35", incident.query());
        assertTrue(hospitals.usesIncidentLocation());
    }

    @Test
    public void usesAddressOrGeneralSearchWhenCoordinatesAreCleared() {
        IncidentReview review = new IncidentReview(new IncidentReport());
        review.set(IncidentReview.Field.LOCATION, "Practice Hall & South Gate");

        assertEquals("hospital near Practice Hall & South Gate",
                VictimMapSearch.forHospitals(review.location()).query());
        assertEquals("Practice Hall & South Gate", VictimMapSearch.forIncident(review.location()).query());

        review.set(IncidentReview.Field.LOCATION, "");
        VictimMapSearch hospitals = VictimMapSearch.forHospitals(review.location());
        assertEquals("hospital", hospitals.query());
        assertFalse(hospitals.usesIncidentLocation());
        assertNull(VictimMapSearch.forIncident(review.location()));
    }

    @Test
    public void rejectsPartialCoordinatesBeforeRouting() {
        IncidentReview review = new IncidentReview(new IncidentReport());
        review.set(IncidentReview.Field.LATITUDE, "25.25");
        assertEquals(IncidentReview.Field.LONGITUDE,
                assertThrows(IncidentReview.InvalidField.class, review::location).field);
    }
}
