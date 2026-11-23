package com.example.scenar.ui.responder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.scenar.model.IncidentReport;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.Test;

import java.nio.charset.StandardCharsets;

public class BlePayloadPreviewTest {

    @Test
    public void quotesNewlinesAndBackslashesAreEscaped() {
        IncidentReport report = new IncidentReport();
        report.setIncidentType("Fire \"or\" smoke");
        report.setSummary("He said \"help\"\nthen C:\\exit\t{ok}");

        String json = BlePayloadPreview.from("INC-\"1\"", report).getJson();

        assertFalse("Raw newlines must not appear in the payload", json.contains("\n"));
        assertFalse("Raw tabs must not appear in the payload", json.contains("\t"));
        assertTrue(json.contains("\\\"help\\\""));
        assertTrue(json.contains("\\n"));
        assertTrue(json.contains("C:\\\\exit"));

        JsonObject parsed = JsonParser.parseString(json).getAsJsonObject();
        assertEquals("INC-\"1\"", parsed.get("id").getAsString());
        assertEquals("Fire \"or\" smoke", parsed.get("type").getAsString());
        assertEquals("He said \"help\"\nthen C:\\exit\t{ok}", parsed.get("s").getAsString());
    }

    @Test
    public void textThatLooksLikeJsonCannotAddFields() {
        IncidentReport report = new IncidentReport();
        report.setSummary("x\",\"lat\":0,\"lon\":0,\"z\":\"");

        BlePayloadPreview preview = BlePayloadPreview.from("INC-1", report);
        JsonObject parsed = JsonParser.parseString(preview.getJson()).getAsJsonObject();

        assertFalse(parsed.has("lat"));
        assertFalse(parsed.has("lon"));
        assertFalse(parsed.has("z"));
        assertFalse(preview.includesLocation());
    }

    @Test
    public void byteSizeIsMeasuredInUtf8() {
        IncidentReport report = new IncidentReport();
        report.setSummary("Café fire — help \uD83D\uDE92");

        BlePayloadPreview preview = BlePayloadPreview.from("INC-1", report);
        int utf8 = preview.getJson().getBytes(StandardCharsets.UTF_8).length;

        assertEquals(utf8, preview.getUtf8ByteSize());
        assertTrue("UTF-8 size should exceed the character count for non-ASCII text",
                preview.getUtf8ByteSize() > preview.getJson().length());
    }

    @Test
    public void longSummaryIsTruncatedWithoutSplittingCharacters() {
        StringBuilder summary = new StringBuilder();
        for (int i = 0; i < 40; i++) summary.append("\uD83D\uDD25a");

        String truncated = BlePayloadPreview.truncate(summary.toString(),
                BlePayloadPreview.MAX_SUMMARY_CODE_POINTS);

        assertEquals(BlePayloadPreview.MAX_SUMMARY_CODE_POINTS,
                truncated.codePointCount(0, truncated.length()));
        assertTrue(truncated.endsWith("\u2026"));
        for (int i = 0; i < truncated.length(); i++) {
            char c = truncated.charAt(i);
            if (Character.isHighSurrogate(c)) {
                assertTrue(i + 1 < truncated.length() && Character.isLowSurrogate(truncated.charAt(i + 1)));
            }
        }
    }

    @Test
    public void unknownValuesAreLeftOutInsteadOfGuessed() {
        JsonObject parsed = JsonParser.parseString(
                BlePayloadPreview.from("INC-1", new IncidentReport()).getJson()).getAsJsonObject();

        assertFalse(parsed.has("n"));
        assertFalse(parsed.has("s"));
        assertFalse(parsed.has("lat"));
        assertTrue(parsed.get("demo").getAsBoolean());
    }

    @Test
    public void locationIsIncludedOnlyWithRealCoordinates() {
        IncidentReport report = new IncidentReport();
        report.setPeopleAffected(2);
        report.setLatitude(49.282712);
        report.setLongitude(-123.120738);

        BlePayloadPreview preview = BlePayloadPreview.from("INC-1", report);
        JsonObject parsed = JsonParser.parseString(preview.getJson()).getAsJsonObject();

        assertTrue(preview.includesLocation());
        assertEquals(49.2827, parsed.get("lat").getAsDouble(), 0.0);
        assertEquals(-123.1207, parsed.get("lon").getAsDouble(), 0.0);
        assertEquals(2, parsed.get("n").getAsInt());
    }
}
