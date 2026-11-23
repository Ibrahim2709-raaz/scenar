export const incidentReportSchema = {
  type: "object",
  additionalProperties: false,
  properties: {
    incidentType: { type: ["string", "null"] },
    summary: { type: "string" },
    peopleAffected: { type: ["integer", "null"] },
    criticalConditions: { type: "array", items: { type: "string" } },
    hazards: { type: "array", items: { type: "string" } },
    locationDescription: { type: ["string", "null"] },
    latitude: { type: ["number", "null"] },
    longitude: { type: ["number", "null"] },
    medicalFlags: { type: "array", items: { type: "string" } },
    missingInformation: { type: "array", items: { type: "string" } },
    uncertainties: { type: "array", items: { type: "string" } }
  },
  required: [
    "incidentType",
    "summary",
    "peopleAffected",
    "criticalConditions",
    "hazards",
    "locationDescription",
    "latitude",
    "longitude",
    "medicalFlags",
    "missingInformation",
    "uncertainties"
  ]
};
