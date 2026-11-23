import assert from "node:assert/strict";
import test from "node:test";

import { generateIncidentReport } from "../src/openAiIncidentService.js";

test("requests strict structured output and returns parsed JSON", async () => {
  let captured;
  const client = {
    responses: {
      create: async request => {
        captured = request;
        return {
          output_text: JSON.stringify({
            incidentType: "Fire",
            summary: "Smoke reported.",
            peopleAffected: null,
            criticalConditions: [],
            hazards: ["Smoke"],
            locationDescription: null,
            latitude: null,
            longitude: null,
            medicalFlags: [],
            missingInformation: ["Number of people affected"],
            uncertainties: []
          })
        };
      }
    }
  };

  const report = await generateIncidentReport({
    client,
    prompt: "Structure this incident",
    model: "test-model"
  });

  assert.equal(captured.store, false);
  assert.equal(captured.text.format.type, "json_schema");
  assert.equal(captured.text.format.strict, true);
  assert.equal(report.summary, "Smoke reported.");
  assert.deepEqual(report.hazards, ["Smoke"]);
});

test("rejects an empty structured response", async () => {
  const client = {
    responses: {
      create: async () => ({ output_text: "" })
    }
  };

  await assert.rejects(
    generateIncidentReport({ client, prompt: "Prompt", model: "test-model" }),
    /no structured incident report/i
  );
});
