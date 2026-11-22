package com.example.scenar.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.example.scenar.model.IncidentReport;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicReference;

public class RealIncidentAiServiceTest {

    private IncidentPromptBuilder promptBuilder;
    private IncidentReportJsonParser parser;
    private IncidentReportValidator validator;
    private IncidentAiService fallback;

    @Before
    public void setUp() {
        promptBuilder = new IncidentPromptBuilder();
        parser = new IncidentReportJsonParser();
        validator = new IncidentReportValidator();
        fallback = new DeterministicIncidentService();
    }

    @Test
    public void validAiResponseReturnsStructuredReport() {
        AiGateway gateway = new SuccessfulGateway(
                validJson()
        );

        RealIncidentAiService service =
                createService(gateway);

        AtomicReference<IncidentReport> result =
                new AtomicReference<>();

        service.structureIncident(
                TestIncidentScenarios.COMPLETE_INCIDENT,
                callbackFor(result)
        );

        assertEquals(
                "Structural emergency",
                result.get().getIncidentType()
        );
        assertEquals(
                Integer.valueOf(2),
                result.get().getPeopleAffected()
        );
        assertTrue(
                result.get().getHazards().contains("Smoke")
        );
    }

    @Test
    public void gatewayFailureUsesDeterministicFallback() {
        RealIncidentAiService service =
                createService(new FailingGateway());

        AtomicReference<IncidentReport> result =
                new AtomicReference<>();

        service.structureIncident(
                TestIncidentScenarios.INCOMPLETE_INCIDENT,
                callbackFor(result)
        );

        assertEquals(
                TestIncidentScenarios.INCOMPLETE_INCIDENT,
                result.get().getSummary()
        );
        assertNull(result.get().getPeopleAffected());
        assertFalse(
                result.get()
                        .getMissingInformation()
                        .isEmpty()
        );
    }

    @Test
    public void malformedJsonUsesFallback() {
        AiGateway gateway =
                new SuccessfulGateway("{bad json");

        RealIncidentAiService service =
                createService(gateway);

        AtomicReference<IncidentReport> result =
                new AtomicReference<>();

        service.structureIncident(
                TestIncidentScenarios.CONTRADICTORY_INCIDENT,
                callbackFor(result)
        );

        assertEquals(
                TestIncidentScenarios.CONTRADICTORY_INCIDENT,
                result.get().getSummary()
        );
        assertNull(result.get().getPeopleAffected());
    }

    @Test
    public void invalidReportUsesFallback() {
        String invalidJson =
                "{"
                        + "\"summary\":\"Emergency\","
                        + "\"peopleAffected\":-4,"
                        + "\"criticalConditions\":[],"
                        + "\"hazards\":[],"
                        + "\"medicalFlags\":[],"
                        + "\"missingInformation\":[],"
                        + "\"uncertainties\":[]"
                        + "}";

        RealIncidentAiService service =
                createService(
                        new SuccessfulGateway(invalidJson)
                );

        AtomicReference<IncidentReport> result =
                new AtomicReference<>();

        service.structureIncident(
                "Emergency reported.",
                callbackFor(result)
        );

        assertNull(result.get().getPeopleAffected());
        assertFalse(
                result.get()
                        .getMissingInformation()
                        .isEmpty()
        );
    }

    private RealIncidentAiService createService(
            AiGateway gateway
    ) {
        return new RealIncidentAiService(
                gateway,
                promptBuilder,
                parser,
                validator,
                fallback
        );
    }

    private IncidentAiService.IncidentCallback callbackFor(
            AtomicReference<IncidentReport> result
    ) {
        return new IncidentAiService.IncidentCallback() {
            @Override
            public void onSuccess(IncidentReport report) {
                result.set(report);
            }

            @Override
            public void onFailure(String message) {
                fail(message);
            }
        };
    }

    private String validJson() {
        return "{"
                + "\"incidentType\":\"Structural emergency\","
                + "\"summary\":\"Ceiling collapsed.\","
                + "\"peopleAffected\":2,"
                + "\"criticalConditions\":["
                + "\"One person is unresponsive\""
                + "],"
                + "\"hazards\":[\"Smoke\"],"
                + "\"locationDescription\":\"Building B\","
                + "\"latitude\":null,"
                + "\"longitude\":null,"
                + "\"medicalFlags\":[],"
                + "\"missingInformation\":[],"
                + "\"uncertainties\":[]"
                + "}";
    }

    private static class SuccessfulGateway
            implements AiGateway {

        private final String response;

        SuccessfulGateway(String response) {
            this.response = response;
        }

        @Override
        public void generate(
                String prompt,
                GatewayCallback callback
        ) {
            callback.onSuccess(response);
        }
    }

    private static class FailingGateway
            implements AiGateway {

        @Override
        public void generate(
                String prompt,
                GatewayCallback callback
        ) {
            callback.onFailure("Network unavailable");
        }
    }
}