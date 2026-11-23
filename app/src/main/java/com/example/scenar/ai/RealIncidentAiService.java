package com.example.scenar.ai;

import com.example.scenar.model.IncidentReport;

public class RealIncidentAiService
        implements IncidentAiService {

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
        this.gateway = gateway;
        this.promptBuilder = promptBuilder;
        this.parser = parser;
        this.validator = validator;
        this.fallbackService = fallbackService;
    }

    @Override
    public void structureIncident(
            String description,
            IncidentCallback callback
    ) {
        if (description == null
                || description.trim().isEmpty()) {
            callback.onFailure(
                    "Describe what happened before continuing."
            );
            return;
        }

        String prompt = promptBuilder.build(
                description.trim()
        );

        gateway.generate(
                prompt,
                new AiGateway.GatewayCallback() {
                    @Override
                    public void onSuccess(String jsonResponse) {
                        processResponse(
                                description,
                                jsonResponse,
                                callback
                        );
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        useFallback(description, callback);
                    }
                }
        );
    }

    private void processResponse(
            String originalDescription,
            String jsonResponse,
            IncidentCallback callback
    ) {
        try {
            IncidentReport report =
                    parser.parse(jsonResponse);

            IncidentReportValidator.ValidationResult result =
                    validator.validate(report);

            if (!result.isValid()) {
                useFallback(
                        originalDescription,
                        callback
                );
                return;
            }

            callback.onSuccess(report);

        } catch (IncidentParsingException exception) {
            useFallback(originalDescription, callback);
        }
    }

    private void useFallback(
            String originalDescription,
            IncidentCallback callback
    ) {
        fallbackService.structureIncident(
                originalDescription,
                callback
        );
    }
}