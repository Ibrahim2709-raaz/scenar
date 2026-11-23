package com.example.scenar.ai;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class IncidentPromptBuilderTest {

    @Test
    public void promptContainsOriginalDescriptionAndJsonFields() {
        IncidentPromptBuilder builder = new IncidentPromptBuilder();

        String prompt = builder.build(
                TestIncidentScenarios.COMPLETE_INCIDENT
        );

        assertTrue(prompt.contains(
                TestIncidentScenarios.COMPLETE_INCIDENT
        ));
        assertTrue(prompt.contains("\"incidentType\""));
        assertTrue(prompt.contains("\"missingInformation\""));
        assertTrue(prompt.contains("\"uncertainties\""));
        assertTrue(prompt.contains("Do not invent"));
    }

    @Test
    public void promptPreservesContradictoryDescription() {
        IncidentPromptBuilder builder = new IncidentPromptBuilder();

        String prompt = builder.build(
                TestIncidentScenarios.CONTRADICTORY_INCIDENT
        );

        assertTrue(prompt.contains("two people, maybe three"));
        assertTrue(prompt.contains("Building A"));
        assertTrue(prompt.contains("Building C"));
    }
}