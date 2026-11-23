package com.example.scenar.ui.victim;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.scenar.model.IncidentReport;

import org.junit.Test;

import java.util.Collections;

public class PracticeDispatchTest {
    @Test
    public void uncheckedConsentKeepsMedicalFlagsPrivateWithoutChangingReviewedReport() {
        IncidentReport reviewed = new IncidentReport();
        reviewed.setSummary("Reviewed practice report");
        reviewed.setHazards(Collections.singletonList("Wet floor"));
        reviewed.setMedicalFlags(Collections.singletonList("Allergies: Latex"));

        PracticeDispatch dispatch = new PracticeDispatch(reviewed, false, false);

        assertFalse(dispatch.includesMedicalFlags());
        assertFalse(dispatch.contactNotificationSimulated());
        assertEquals(reviewed.getSummary(), dispatch.sharedReport().getSummary());
        assertEquals(reviewed.getHazards(), dispatch.sharedReport().getHazards());
        assertEquals(1, reviewed.getMedicalFlags().size());
    }

    @Test
    public void consentSharesSnapshotAndNotificationChoiceDoesNotChangeIt() {
        IncidentReport reviewed = new IncidentReport();
        reviewed.setMedicalFlags(Collections.singletonList("Allergies: Latex"));
        PracticeDispatch dispatch = new PracticeDispatch(reviewed, true, true);

        reviewed.getMedicalFlags().clear();
        dispatch.sharedReport().getMedicalFlags().clear();

        assertEquals(Collections.singletonList("Allergies: Latex"), dispatch.sharedReport().getMedicalFlags());
        assertTrue(dispatch.includesMedicalFlags());
        assertTrue(dispatch.contactNotificationSimulated());
    }
}
