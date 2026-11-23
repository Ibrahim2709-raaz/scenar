package com.example.scenar.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.example.scenar.model.IncidentReport;
import com.example.scenar.model.SharingConsent;

import org.junit.Test;

import java.util.Collections;

public class IncidentSharingPolicyTest {
    @Test
    public void removesLocationAndMedicalFlagsWithoutConsent() {
        IncidentReport source = report();

        IncidentReport filtered = IncidentSharingPolicy.filterForResponder(
                source,
                SharingConsent.shareNothing()
        );

        assertNull(filtered.getLocationDescription());
        assertNull(filtered.getLatitude());
        assertNull(filtered.getLongitude());
        assertEquals(Collections.emptyList(), filtered.getMedicalFlags());
        assertEquals("Practice emergency", filtered.getSummary());

        assertEquals("Practice Hall", source.getLocationDescription());
        assertEquals(Collections.singletonList("Allergy: Latex"), source.getMedicalFlags());
    }

    @Test
    public void preservesApprovedInformation() {
        IncidentReport filtered = IncidentSharingPolicy.filterForResponder(
                report(),
                SharingConsent.shareReportDetails()
        );

        assertEquals("Practice Hall", filtered.getLocationDescription());
        assertEquals(Double.valueOf(25.25), filtered.getLatitude());
        assertEquals(Collections.singletonList("Allergy: Latex"), filtered.getMedicalFlags());
    }

    private IncidentReport report() {
        IncidentReport report = new IncidentReport();
        report.setSummary("Practice emergency");
        report.setLocationDescription("Practice Hall");
        report.setLatitude(25.25);
        report.setLongitude(55.35);
        report.setMedicalFlags(Collections.singletonList("Allergy: Latex"));
        return report;
    }
}
