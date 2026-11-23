import path from "node:path";
import { fileURLToPath } from "node:url";

import dotenv from "dotenv";
import express from "express";

import {
  createOpenAiClient,
  generateIncidentReport
} from "./openAiIncidentService.js";

const currentDirectory = path.dirname(fileURLToPath(import.meta.url));
dotenv.config({ path: path.resolve(currentDirectory, "../../.env.local") });

const port = Number.parseInt(process.env.PORT ?? "3000", 10);
const model = process.env.OPENAI_MODEL?.trim() || "gpt-4o-mini";
const client = createOpenAiClient(process.env.OPENAI_API_KEY);
const app = express();

app.disable("x-powered-by");
app.use(express.json({ limit: "64kb" }));

app.get("/health", (request, response) => {
  response.json({ status: "ok", model });
});

app.post("/api/incident", async (request, response) => {
  const prompt = request.body?.prompt;
  if (typeof prompt !== "string" || !prompt.trim()) {
    response.status(400).json({ error: "A non-empty prompt is required." });
    return;
  }
  if (prompt.length > 16_000) {
    response.status(413).json({ error: "The incident description is too long." });
    return;
  }

  try {
    const report = await generateIncidentReport({
      client,
      prompt: prompt.trim(),
      model
    });
    response.json(report);
  } catch (error) {
    const upstreamStatus = Number(error?.status);
    const status = upstreamStatus === 429 ? 503 : 502;
    response.status(status).json({
      error: "The AI service could not generate a report. Use the offline fallback."
    });
  }
});

app.use((error, request, response, next) => {
  if (error instanceof SyntaxError) {
    response.status(400).json({ error: "Invalid JSON request." });
    return;
  }
  next(error);
});

app.listen(port, "127.0.0.1", () => {
  console.log(`SCENAR AI backend listening on http://127.0.0.1:${port}`);
});
