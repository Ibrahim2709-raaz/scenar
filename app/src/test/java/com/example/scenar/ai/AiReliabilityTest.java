package com.example.scenar.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.example.scenar.model.IncidentReport;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public final class AiReliabilityTest {

    @Test
    public void gatewayFailureIsReportedAsLocalFallback() {
        AiGateway failingGateway =
                (prompt, callback) ->
                        callback.onFailure(
                                "Network unavailable"
                        );

        IncidentAiService fallback =
                successfulFallback();

        RealIncidentAiService service =
                createService(
                        failingGateway,
                        fallback
                );

        AtomicBoolean normalSuccess =
                new AtomicBoolean(false);
        AtomicBoolean fallbackSuccess =
                new AtomicBoolean(false);
        AtomicReference<IncidentReport> result =
                new AtomicReference<>();
        AtomicReference<String> message =
                new AtomicReference<>();

        service.structureIncident(
                "A person fell near the library.",
                new IncidentAiService.IncidentCallback() {
                    @Override
                    public void onSuccess(
                            IncidentReport report
                    ) {
                        normalSuccess.set(true);
                    }

                    @Override
                    public void onFallback(
                            IncidentReport report,
                            String userMessage
                    ) {
                        fallbackSuccess.set(true);
                        result.set(report);
                        message.set(userMessage);
                    }

                    @Override
                    public void onFailure(
                            String errorMessage
                    ) {
                        fail(errorMessage);
                    }
                }
        );

        assertFalse(normalSuccess.get());
        assertTrue(fallbackSuccess.get());
        assertNotNull(result.get());
        assertEquals(
                "A person fell near the library.",
                result.get().getSummary()
        );
        assertTrue(
                message.get().contains(
                        "created this report locally"
                )
        );
    }

    @Test
    public void invalidAiResponseIsReportedAsLocalFallback() {
        AiGateway malformedGateway =
                (prompt, callback) ->
                        callback.onSuccess(
                                "{invalid-json"
                        );

        RealIncidentAiService service =
                createService(
                        malformedGateway,
                        successfulFallback()
                );

        AtomicBoolean fallbackSuccess =
                new AtomicBoolean(false);
        AtomicReference<String> message =
                new AtomicReference<>();

        service.structureIncident(
                "Smoke is visible in the hallway.",
                new IncidentAiService.IncidentCallback() {
                    @Override
                    public void onSuccess(
                            IncidentReport report
                    ) {
                        fail(
                                "Malformed AI output must not "
                                        + "be reported as AI success."
                        );
                    }

                    @Override
                    public void onFallback(
                            IncidentReport report,
                            String userMessage
                    ) {
                        fallbackSuccess.set(true);
                        message.set(userMessage);
                    }

                    @Override
                    public void onFailure(
                            String errorMessage
                    ) {
                        fail(errorMessage);
                    }
                }
        );

        assertTrue(fallbackSuccess.get());
        assertTrue(
                message.get().contains(
                        "could not be safely verified"
                )
        );
    }

    @Test
    public void failureIsReturnedWhenAiAndFallbackBothFail() {
        AiGateway failingGateway =
                (prompt, callback) ->
                        callback.onFailure(
                                "Network unavailable"
                        );

        IncidentAiService failingFallback =
                (description, callback) ->
                        callback.onFailure(
                                "Local generation failed"
                        );

        RealIncidentAiService service =
                createService(
                        failingGateway,
                        failingFallback
                );

        AtomicBoolean success =
                new AtomicBoolean(false);
        AtomicReference<String> failure =
                new AtomicReference<>();

        service.structureIncident(
                "Emergency reported.",
                new IncidentAiService.IncidentCallback() {
                    @Override
                    public void onSuccess(
                            IncidentReport report
                    ) {
                        success.set(true);
                    }

                    @Override
                    public void onFailure(
                            String message
                    ) {
                        failure.set(message);
                    }
                }
        );

        assertFalse(success.get());
        assertNotNull(failure.get());
        assertTrue(
                failure.get().contains(
                        "could not create the report"
                )
        );
    }

    @Test
    public void blankDescriptionDoesNotCallGateway() {
        AtomicBoolean gatewayCalled =
                new AtomicBoolean(false);

        AiGateway gateway =
                (prompt, callback) ->
                        gatewayCalled.set(true);

        RealIncidentAiService service =
                createService(
                        gateway,
                        successfulFallback()
                );

        AtomicReference<String> failure =
                new AtomicReference<>();

        service.structureIncident(
                "   ",
                new IncidentAiService.IncidentCallback() {
                    @Override
                    public void onSuccess(
                            IncidentReport report
                    ) {
                        fail(
                                "Blank input must not succeed."
                        );
                    }

                    @Override
                    public void onFailure(
                            String message
                    ) {
                        failure.set(message);
                    }
                }
        );

        assertFalse(gatewayCalled.get());
        assertEquals(
                "Describe what happened before continuing.",
                failure.get()
        );
    }

    private RealIncidentAiService createService(
            AiGateway gateway,
            IncidentAiService fallback
    ) {
        return new RealIncidentAiService(
                gateway,
                new IncidentPromptBuilder(),
                new IncidentReportJsonParser(),
                new IncidentReportValidator(),
                fallback
        );
    }

    private IncidentAiService successfulFallback() {
        return (description, callback) -> {
            IncidentReport report =
                    new IncidentReport();

            report.setIncidentType(
                    "Unspecified emergency"
            );
            report.setSummary(description);
            report.getMissingInformation().add(
                    "Confirm incident details"
            );

            callback.onSuccess(report);
        };
    }
}