import OpenAI from "openai";

import { incidentReportSchema } from "./incidentSchema.js";

export function createOpenAiClient(apiKey) {
  if (!apiKey) throw new Error("OPENAI_API_KEY is not configured");
  return new OpenAI({
    apiKey,
    maxRetries: 1,
    timeout: 25_000
  });
}

export async function generateIncidentReport({ client, prompt, model }) {
  const response = await client.responses.create({
    model,
    store: false,
    max_output_tokens: 1_200,
    input: prompt,
    text: {
      format: {
        type: "json_schema",
        name: "scenar_incident_report",
        description: "Emergency incident facts organized for human review",
        strict: true,
        schema: incidentReportSchema
      }
    }
  });

  if (!response.output_text || !response.output_text.trim()) {
    throw new Error("OpenAI returned no structured incident report");
  }

  return JSON.parse(response.output_text);
}
