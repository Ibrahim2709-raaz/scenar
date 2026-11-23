package com.example.scenar.ai;

import com.example.scenar.BuildConfig;

public final class IncidentAiServiceFactory {

    private IncidentAiServiceFactory() {
        // Prevent this utility class from being instantiated.
    }

    /**
     * Stable demo version that works without internet.
     */
    public static IncidentAiService createDemoService() {
        return new FakeIncidentAiService();
    }

    /**
     * Uses the configured backend when present. Release builds without a
     * deployed endpoint remain functional through the deterministic fallback.
     */
    public static IncidentAiService createConfiguredService() {
        String endpoint = BuildConfig.SCENAR_AI_ENDPOINT;
        if (endpoint == null || endpoint.isBlank()) {
            return new DeterministicIncidentService();
        }
        return createRealService(endpoint);
    }

    /**
     * Real AI version. If the network or AI response fails,
     * RealIncidentAiService automatically uses the
     * deterministic fallback.
     */
    public static IncidentAiService createRealService(
            String endpointUrl
    ) {
        AiGateway gateway =
                new HttpAiGateway(endpointUrl);

        return new RealIncidentAiService(
                gateway,
                new IncidentPromptBuilder(),
                new IncidentReportJsonParser(),
                new IncidentReportValidator(),
                new DeterministicIncidentService()
        );
    }
}
