public RealIncidentAiService(
        AiGateway gateway,
        IncidentPromptBuilder promptBuilder,
        IncidentReportJsonParser parser,
        IncidentReportValidator validator,
        IncidentAiService fallback
)