package com.example.scenar.ai;

import com.example.scenar.model.IncidentReport;

import java.util.Objects;

public final class RealIncidentAiService
        implements IncidentAiService {

    private static final String NETWORK_FALLBACK_MESSAGE =
            "The AI service could not be reached. "
                    + "SCENAR created this report locally. "
                    + "Please review it carefully.";

    private static final String INVALID_RESPONSE_FALLBACK_MESSAGE =
            "The AI response could not be safely verified. "
                    + "SCENAR created this report locally. "
                    + "Please review it carefully.";

    private static final String COMPLETE_FAILURE_MESSAGE =
            "SCENAR could not create the report. "
                    + "Your details are still saved. Please try again.";

    private final AiGateway gateway;
    private final IncidentPromptBuilder promptBuilder;
    private final IncidentReportJsonParser parser;
    private final IncidentReportValidator validator;
    private final IncidentAiService fallbackService;

    public RealIncidentAiService(
            AiGateway gateway,
            IncidentPromptBuilder promptBuilder,
            IncidentReportJsonParser parser,
            IncidentReportValidator validator,
            IncidentAiService fallbackService
    ) {
        this.gateway = Objects.requireNonNull(
                gateway,
                "gateway"
        );
        this.promptBuilder = Objects.requireNonNull(
                promptBuilder,
                "promptBuilder"
        );
        this.parser = Objects.requireNonNull(
                parser,
                "parser"
        );
        this.validator = Objects.requireNonNull(
                validator,
                "validator"
        );
        this.fallbackService = Objects.requireNonNull(
                fallbackService,
                "fallbackService"
        );
    }

    @Override
    public void structureIncident(
            String description,
            IncidentCallback callback
    ) {
        Objects.requireNonNull(callback, "callback");

        if (description == null
                || description.trim().isEmpty()) {
            callback.onFailure(
                    "Describe what happened before continuing."
            );
            return;
        }

        String normalizedDescription =
                description.trim();

        String prompt;

        try {
            prompt = promptBuilder.build(
                    normalizedDescription
            );
        } catch (RuntimeException exception) {
            callback.onFailure(COMPLETE_FAILURE_MESSAGE);
            return;
        }

        try {
            gateway.generate(
                    prompt,
                    new AiGateway.GatewayCallback() {
                        @Override
                        public void onSuccess(
                                String jsonResponse
                        ) {
                            processResponse(
                                    normalizedDescription,
                                    jsonResponse,
                                    callback
                            );
                        }

                        @Override
                        public void onFailure(
                                String errorMessage
                        ) {
                            useFallback(
                                    normalizedDescription,
                                    NETWORK_FALLBACK_MESSAGE,
                                    callback
                            );
                        }
                    }
            );
        } catch (RuntimeException exception) {
            useFallback(
                    normalizedDescription,
                    NETWORK_FALLBACK_MESSAGE,
                    callback
            );
        }
    }

    private void processResponse(
            String originalDescription,
            String jsonResponse,
            IncidentCallback callback
    ) {
        if (jsonResponse == null
                || jsonResponse.trim().isEmpty()) {
            useFallback(
                    originalDescription,
                    INVALID_RESPONSE_FALLBACK_MESSAGE,
                    callback
            );
            return;
        }

        try {
            IncidentReport report =
                    parser.parse(jsonResponse);

            IncidentReportValidator.ValidationResult result =
                    validator.validate(report);

            if (!result.isValid()) {
                useFallback(
                        originalDescription,
                        INVALID_RESPONSE_FALLBACK_MESSAGE,
                        callback
                );
                return;
            }

            callback.onSuccess(
                    new IncidentReport(report)
            );

        } catch (IncidentParsingException
                 | RuntimeException exception) {
            useFallback(
                    originalDescription,
                    INVALID_RESPONSE_FALLBACK_MESSAGE,
                    callback
            );
        }
    }

    private void useFallback(
            String originalDescription,
            String userMessage,
            IncidentCallback callback
    ) {
        try {
            fallbackService.structureIncident(
                    originalDescription,
                    new IncidentCallback() {
                        @Override
                        public void onSuccess(
                                IncidentReport report
                        ) {
                            deliverFallback(
                                    report,
                                    userMessage,
                                    callback
                            );
                        }

                        @Override
                        public void onFallback(
                                IncidentReport report,
                                String ignoredMessage
                        ) {
                            deliverFallback(
                                    report,
                                    userMessage,
                                    callback
                            );
                        }

                        @Override
                        public void onFailure(
                                String ignoredMessage
                        ) {
                            callback.onFailure(
                                    COMPLETE_FAILURE_MESSAGE
                            );
                        }
                    }
            );
        } catch (RuntimeException exception) {
            callback.onFailure(
                    COMPLETE_FAILURE_MESSAGE
            );
        }
    }

    private void deliverFallback(
            IncidentReport report,
            String userMessage,
            IncidentCallback callback
    ) {
        if (report == null) {
            callback.onFailure(
                    COMPLETE_FAILURE_MESSAGE
            );
            return;
        }

        callback.onFallback(
                new IncidentReport(report),
                userMessage
        );
    }
}