package com.example.scenar.ui.victim;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.scenar.ai.FakeIncidentAiService;
import com.example.scenar.model.IncidentReport;

import org.junit.Test;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class VictimReportDraftTest {
    @Test
    public void handoffUsesReviewedFactsWithoutMockExamples() throws Exception {
        VictimReportDraft.Input input = new VictimReportDraft.Input(
                "injury", "Someone slipped.", "Practice Park",
                new VictimReportDraft.Coordinates(50.5, -120.5, 12f),
                new VictimReportDraft.MedicalProfile("", "", "Needs step-free access"));
        VictimReportDraft.Report draft = VictimReportDraft
                .generateIncidentReport(input, new FakeIncidentAiService())
                .get(5, TimeUnit.SECONDS);

        IncidentReport reviewed = draft.toReviewedIncident("Injury or accident", "Corrected summary.");
        assertEquals("Injury or accident", reviewed.getIncidentType());
        assertEquals("Corrected summary.", reviewed.getSummary());
        assertEquals("Practice Park", reviewed.getLocationDescription());
        assertEquals(Double.valueOf(50.5), reviewed.getLatitude());
        assertEquals(Double.valueOf(-120.5), reviewed.getLongitude());
        assertEquals(Collections.singletonList("Conditions or access needs: Needs step-free access"),
                reviewed.getMedicalFlags());
        assertNull(reviewed.getPeopleAffected());
        assertTrue(reviewed.getHazards().isEmpty());
        assertTrue(reviewed.getCriticalConditions().isEmpty());
    }

    @Test
    public void handoffLeavesOmittedFieldsUnknown() throws Exception {
        VictimReportDraft.Input input = new VictimReportDraft.Input(
                "unspecified", "Details unavailable.", "", null,
                new VictimReportDraft.MedicalProfile("", "", ""));
        VictimReportDraft.Report draft = VictimReportDraft
                .generateIncidentReport(input, new FakeIncidentAiService())
                .get(5, TimeUnit.SECONDS);

        IncidentReport reviewed = draft.toReviewedIncident("Unsure or not specified", draft.summary());
        assertNull(reviewed.getIncidentType());
        assertNull(reviewed.getLocationDescription());
        assertNull(reviewed.getLatitude());
        assertNull(reviewed.getLongitude());
        assertTrue(reviewed.getMedicalFlags().isEmpty());
    }
}
