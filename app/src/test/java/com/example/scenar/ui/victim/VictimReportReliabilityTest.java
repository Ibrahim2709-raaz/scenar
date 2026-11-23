package com.example.scenar.ui.victim;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.example.scenar.ai.IncidentAiService;
import com.example.scenar.model.IncidentReport;

import org.junit.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public final class VictimReportReliabilityTest {

    @Test
    public void normalSuccessIsMarkedAsAiGenerated()
            throws Exception {
        VictimReportDraft.Report result =
                VictimReportDraft
                        .generateIncidentReport(
                                input(),
                                successfulAi()
                        )
                        .get(5, TimeUnit.SECONDS);

        assertFalse(result.usedLocalFallback());
        assertEquals(
                VictimReportDraft.GenerationSource.AI,
                result.generationSource()
        );
        assertTrue(
                result.generationMessage().isEmpty()
        );
    }

    @Test
    public void fallbackSuccessIsMarkedAsLocal()
            throws Exception {
        IncidentAiService fallbackResultService =
                (description, callback) -> {
                    IncidentReport report =
                            report(description);

                    callback.onFallback(
                            report,
                            "SCENAR generated this locally."
                    );
                };

        VictimReportDraft.Report result =
                VictimReportDraft
                        .generateIncidentReport(
                                input(),
                                fallbackResultService
                        )
                        .get(5, TimeUnit.SECONDS);

        assertTrue(result.usedLocalFallback());
        assertEquals(
                VictimReportDraft
                        .GenerationSource
                        .LOCAL_FALLBACK,
                result.generationSource()
        );
        assertEquals(
                "SCENAR generated this locally.",
                result.generationMessage()
        );
    }

    @Test
    public void failureCompletesFutureExceptionally()
            throws Exception {
        IncidentAiService failingService =
                (description, callback) ->
                        callback.onFailure(
                                "Unable to generate report."
                        );

        try {
            VictimReportDraft
                    .generateIncidentReport(
                            input(),
                            failingService
                    )
                    .get(5, TimeUnit.SECONDS);

            fail("Generation should have failed.");

        } catch (ExecutionException exception) {
            assertEquals(
                    "Unable to generate report.",
                    exception.getCause().getMessage()
            );
        }
    }

    @Test
    public void nullReportIsRejected()
            throws Exception {
        IncidentAiService nullService =
                (description, callback) ->
                        callback.onSuccess(null);

        try {
            VictimReportDraft
                    .generateIncidentReport(
                            input(),
                            nullService
                    )
                    .get(5, TimeUnit.SECONDS);

            fail("A null report must be rejected.");

        } catch (ExecutionException exception) {
            assertEquals(
                    "The generated report was missing.",
                    exception.getCause().getMessage()
            );
        }
    }

    private VictimReportDraft.Input input() {
        return new VictimReportDraft.Input(
                "medical",
                "A person needs medical assistance.",
                "Campus library",
                null,
                new VictimReportDraft.MedicalProfile(
                        "",
                        "",
                        ""
                )
        );
    }

    private IncidentAiService successfulAi() {
        return (description, callback) ->
                callback.onSuccess(
                        report(description)
                );
    }

    private IncidentReport report(
            String description
    ) {
        IncidentReport report =
                new IncidentReport();

        report.setIncidentType(
                "Medical incident"
        );
        report.setSummary(description);

        return report;
    }
}