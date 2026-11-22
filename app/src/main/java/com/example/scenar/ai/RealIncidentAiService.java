package com.example.scenar.ai;

/**
 * Placeholder for the real AI implementation.
 * Replace its failure response after the gateway, parser,
 * validation and fallback pipeline are complete.
 */
public class RealIncidentAiService implements IncidentAiService {

    public RealIncidentAiService() {
        // The AI gateway will be injected here later.
    }

    @Override
    public void structureIncident(
            String description,
            IncidentCallback callback
    ) {
        callback.onFailure(
                "The real AI service has not been configured yet."
        );
    }
}