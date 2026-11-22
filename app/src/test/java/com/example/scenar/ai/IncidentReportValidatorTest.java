package com.example.scenar.ai;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.scenar.model.IncidentReport;

import org.junit.Before;
import org.junit.Test;

public class IncidentReportValidatorTest {

    private IncidentReportValidator validator;

    @Before
    public void setUp() {
        validator = new IncidentReportValidator();
    }

    @Test
    public void completeReportIsValid() {
        IncidentReport report = new IncidentReport();
        report.setSummary("Two people affected by a ceiling collapse.");
        report.setPeopleAffected(2);
        report.setLatitude(25.2);
        report.setLongitude(55.3);

        assertTrue(validator.validate(report).isValid());
    }

    @Test
    public void missingSummaryIsInvalid() {
        IncidentReport report = new IncidentReport();

        assertFalse(validator.validate(report).isValid());
    }

    @Test
    public void negativePeopleAffectedIsInvalid() {
        IncidentReport report = new IncidentReport();
        report.setSummary("Reported emergency.");
        report.setPeopleAffected(-2);

        assertFalse(validator.validate(report).isValid());
    }

    @Test
    public void invalidLatitudeIsRejected() {
        IncidentReport report = new IncidentReport();
        report.setSummary("Reported emergency.");
        report.setLatitude(120.0);

        assertFalse(validator.validate(report).isValid());
    }

    @Test
    public void invalidLongitudeIsRejected() {
        IncidentReport report = new IncidentReport();
        report.setSummary("Reported emergency.");
        report.setLongitude(200.0);

        assertFalse(validator.validate(report).isValid());
    }
}