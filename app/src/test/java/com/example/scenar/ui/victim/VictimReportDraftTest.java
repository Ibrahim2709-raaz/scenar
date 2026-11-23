package com.example.scenar.ui.victim;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThrows;

import com.example.scenar.ai.FakeIncidentAiService;
import com.example.scenar.model.IncidentReport;

import org.junit.Test;

import java.util.Collections;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class VictimReportDraftTest {
    @Test
    public void reviewReplacesStructuredValuesAndCanClearAiGuesses() {
        IncidentReport ai = new IncidentReport();
        ai.setSummary("AI summary");
        ai.setIncidentType("Fire");
        ai.setPeopleAffected(2);
        ai.setCriticalConditions(Collections.singletonList("Possible injuries"));
        ai.setHazards(Collections.singletonList("Smoke"));
        ai.setLocationDescription("Incorrect location");
        ai.setLatitude(25.0);
        ai.setLongitude(55.0);
        ai.setMedicalFlags(Collections.singletonList("Incorrect flag"));
        ai.setMissingInformation(Collections.singletonList("Exact location"));
        ai.setUncertainties(Collections.singletonList("People count"));
        IncidentReview review = new IncidentReview(ai);
        review.set(IncidentReview.Field.SUMMARY, " Corrected summary ");
        review.set(IncidentReview.Field.TYPE, "Other");
        review.set(IncidentReview.Field.PEOPLE, "0");
        review.set(IncidentReview.Field.CONDITIONS, "");
        review.set(IncidentReview.Field.HAZARDS, " Wet floor\n\nBlocked exit ");
        review.set(IncidentReview.Field.LOCATION, "Practice Hall");
        review.set(IncidentReview.Field.LATITUDE, "");
        review.set(IncidentReview.Field.LONGITUDE, "");
        review.set(IncidentReview.Field.MEDICAL, "");
        review.set(IncidentReview.Field.MISSING, "Time of incident");
        review.set(IncidentReview.Field.UNCERTAINTIES, "Cause unknown");

        IncidentReport corrected = review.toReport();
        assertEquals("Corrected summary", corrected.getSummary());
        assertEquals("Other", corrected.getIncidentType());
        assertEquals(Integer.valueOf(0), corrected.getPeopleAffected());
        assertTrue(corrected.getCriticalConditions().isEmpty());
        assertEquals(Arrays.asList("Wet floor", "Blocked exit"), corrected.getHazards());
        assertEquals("Practice Hall", corrected.getLocationDescription());
        assertNull(corrected.getLatitude());
        assertNull(corrected.getLongitude());
        assertTrue(corrected.getMedicalFlags().isEmpty());
        assertEquals(Collections.singletonList("Time of incident"), corrected.getMissingInformation());
        assertEquals(Collections.singletonList("Cause unknown"), corrected.getUncertainties());
        assertEquals(Integer.valueOf(2), ai.getPeopleAffected());
        assertEquals(Collections.singletonList("Smoke"), ai.getHazards());

        review.set(IncidentReview.Field.PEOPLE, "");
        assertNull(review.toReport().getPeopleAffected());
        assertEquals(Integer.valueOf(0), corrected.getPeopleAffected());
    }

    @Test
    public void reviewRejectsInvalidNumbersBeforeExportOrHandoff() {
        IncidentReport ai = new IncidentReport();
        ai.setSummary("Practice report");
        IncidentReview review = new IncidentReview(ai);
        for (String invalidCount : Arrays.asList("-1", "1.5", "2147483648")) {
            review.set(IncidentReview.Field.PEOPLE, invalidCount);
            assertEquals(IncidentReview.Field.PEOPLE,
                    assertThrows(IncidentReview.InvalidField.class, review::toReport).field);
        }
        review.set(IncidentReview.Field.PEOPLE, "1");
        review.set(IncidentReview.Field.LATITUDE, "25.5");
        assertEquals(IncidentReview.Field.LONGITUDE,
                assertThrows(IncidentReview.InvalidField.class, review::toReport).field);
        review.set(IncidentReview.Field.LONGITUDE, "181");
        assertEquals(IncidentReview.Field.LONGITUDE,
                assertThrows(IncidentReview.InvalidField.class, review::toReport).field);
        review.set(IncidentReview.Field.LONGITUDE, "-120.5");
        assertEquals(Double.valueOf(-120.5), review.toReport().getLongitude());
    }

    @Test
    public void handoffPreservesStructuredSimulationAndUserCorrections() throws Exception {
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
        assertEquals(Integer.valueOf(2), reviewed.getPeopleAffected());
        assertEquals(Collections.singletonList("Smoke reported nearby"), reviewed.getHazards());
        assertEquals(Collections.singletonList("One person may be unresponsive"), reviewed.getCriticalConditions());
        assertEquals(Collections.singletonList("Confirm the exact location"), reviewed.getMissingInformation());
        assertEquals(Collections.singletonList("Number of affected people requires confirmation"),
                reviewed.getUncertainties());
    }

    @Test
    public void handoffLeavesOmittedFieldsUnknown() throws Exception {
        VictimReportDraft.Input input = new VictimReportDraft.Input(
                "unspecified", "Details unavailable.", "", null,
                new VictimReportDraft.MedicalProfile("", "", ""));
        VictimReportDraft.Report draft = VictimReportDraft
                .generateIncidentReport(input, (description, callback) -> {
                    IncidentReport unstructured = new IncidentReport();
                    unstructured.setSummary(description);
                    callback.onSuccess(unstructured);
                })
                .get(5, TimeUnit.SECONDS);

        IncidentReport reviewed = draft.toReviewedIncident("Unsure or not specified", draft.summary());
        assertNull(reviewed.getIncidentType());
        assertNull(reviewed.getLocationDescription());
        assertNull(reviewed.getLatitude());
        assertNull(reviewed.getLongitude());
        assertTrue(reviewed.getMedicalFlags().isEmpty());
    }

    @Test
    public void handoffKeepsAiLocationAndMedicalFlagsWhenUserDidNotOverrideThem() throws Exception {
        IncidentReport ai = new IncidentReport();
        ai.setSummary("Initial summary.");
        ai.setIncidentType("Fire");
        ai.setLocationDescription("East loading bay");
        ai.setLatitude(25.5);
        ai.setLongitude(55.5);
        ai.setMedicalFlags(Collections.singletonList("Possible smoke exposure"));
        VictimReportDraft.Input input = new VictimReportDraft.Input(
                "unspecified", "Smoke at the east loading bay.", "", null,
                new VictimReportDraft.MedicalProfile("Latex", "", ""));

        VictimReportDraft.Report draft = VictimReportDraft
                .generateIncidentReport(input, (description, callback) -> callback.onSuccess(ai))
                .get(5, TimeUnit.SECONDS);
        ai.getMedicalFlags().clear();

        IncidentReport reviewed = draft.toReviewedIncident("Unsure or not specified", "Reviewed summary.");
        assertEquals("Fire", reviewed.getIncidentType());
        assertEquals("East loading bay", reviewed.getLocationDescription());
        assertEquals(Double.valueOf(25.5), reviewed.getLatitude());
        assertEquals(Double.valueOf(55.5), reviewed.getLongitude());
        assertEquals(Arrays.asList("Possible smoke exposure", "Allergies: Latex"), reviewed.getMedicalFlags());
        reviewed.getMedicalFlags().clear();
        assertEquals(2, draft.toReviewedIncident("Unsure or not specified", "Reviewed summary.")
                .getMedicalFlags().size());
        assertEquals("Initial summary.", draft.summary());
    }
}
