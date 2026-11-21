package com.example.scenar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import com.example.scenar.ai.FakeIncidentAiService;
import com.example.scenar.ai.IncidentAiService;
import com.example.scenar.model.IncidentReport;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicReference;

public class FakeIncidentAiServiceTest {
    @Test
    public void validDescriptionCreatesEditableReport() {
        AtomicReference<IncidentReport> result = new AtomicReference<>();

        new FakeIncidentAiService().structureIncident(
                "Two people are near smoke after part of a ceiling collapsed.",
                new IncidentAiService.IncidentCallback() {
                    @Override
                    public void onSuccess(IncidentReport report) {
                        result.set(report);
                    }

                    @Override
                    public void onFailure(String message) {
                        fail(message);
                    }
                }
        );

        assertNotNull(result.get());
        assertEquals(Integer.valueOf(2), result.get().getPeopleAffected());
        assertFalse(result.get().getHazards().isEmpty());
        assertFalse(result.get().getMissingInformation().isEmpty());
    }

    @Test
    public void blankDescriptionReturnsClearError() {
        AtomicReference<String> error = new AtomicReference<>();

        new FakeIncidentAiService().structureIncident(
                "   ",
                new IncidentAiService.IncidentCallback() {
                    @Override
                    public void onSuccess(IncidentReport report) {
                        fail("Blank input must not create a report.");
                    }

                    @Override
                    public void onFailure(String message) {
                        error.set(message);
                    }
                }
        );

        assertEquals("Describe what happened before continuing.", error.get());
    }
}
