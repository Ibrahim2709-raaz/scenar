package com.example.scenar.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.example.scenar.model.IncidentReport;

import org.junit.Before;
import org.junit.Test;

public class IncidentReportJsonParserTest {

    private IncidentReportJsonParser parser;

    @Before
    public void setUp() {
        parser = new IncidentReportJsonParser();
    }

    @Test
    public void validJsonCreatesIncidentReport()
            throws IncidentParsingException {

        String json =
                "{"
                        + "\"incidentType\":\"Structural emergency\","
                        + "\"summary\":\"Ceiling collapsed.\","
                        + "\"peopleAffected\":2,"
                        + "\"criticalConditions\":["
                        + "\"One person is unresponsive\""
                        + "],"
                        + "\"hazards\":[\"Smoke\"],"
                        + "\"locationDescription\":\"Building B\","
                        + "\"latitude\":null,"
                        + "\"longitude\":null,"
                        + "\"medicalFlags\":[],"
                        + "\"missingInformation\":[],"
                        + "\"uncertainties\":[]"
                        + "}";

        IncidentReport report = parser.parse(json);

        assertNotNull(report);
        assertEquals(
                "Structural emergency",
                report.getIncidentType()
        );
        assertEquals(
                Integer.valueOf(2),
                report.getPeopleAffected()
        );
        assertTrue(report.getHazards().contains("Smoke"));
    }

    @Test
    public void markdownFenceIsRemoved()
            throws IncidentParsingException {

        String response =
                "```json\n"
                        + "{"
                        + "\"incidentType\":null,"
                        + "\"summary\":\"Someone is hurt.\","
                        + "\"peopleAffected\":null,"
                        + "\"criticalConditions\":[],"
                        + "\"hazards\":[],"
                        + "\"locationDescription\":null,"
                        + "\"latitude\":null,"
                        + "\"longitude\":null,"
                        + "\"medicalFlags\":[],"
                        + "\"missingInformation\":["
                        + "\"Confirm the location\""
                        + "],"
                        + "\"uncertainties\":[]"
                        + "}\n"
                        + "```";

        IncidentReport report = parser.parse(response);

        assertEquals(
                "Someone is hurt.",
                report.getSummary()
        );
    }

    @Test(expected = IncidentParsingException.class)
    public void malformedJsonIsRejected()
            throws IncidentParsingException {

        parser.parse(
                "{\"summary\":\"Incomplete JSON\""
        );
    }

    @Test(expected = IncidentParsingException.class)
    public void emptyResponseIsRejected()
            throws IncidentParsingException {

        parser.parse("   ");
    }
}