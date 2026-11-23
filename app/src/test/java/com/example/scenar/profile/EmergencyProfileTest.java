package com.example.scenar.profile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class EmergencyProfileTest {

    @Test
    public void emptyProfileIsZeroPercentComplete() {
        EmergencyProfile profile =
                EmergencyProfile.empty();

        assertTrue(profile.isEmpty());
        assertFalse(profile.hasEmergencyContact());
        assertFalse(profile.hasMedicalInformation());
        assertEquals(0, profile.completionPercentage());
    }

    @Test
    public void completeProfileIsOneHundredPercentComplete() {
        EmergencyProfile profile =
                new EmergencyProfile(
                        "Sam",
                        "2000-01-15",
                        "O+",
                        "Latex",
                        "None",
                        "Asthma",
                        "Uses a hearing aid",
                        "Alex",
                        "Sibling",
                        "+1 555 0100"
                );

        assertFalse(profile.isEmpty());
        assertTrue(profile.hasEmergencyContact());
        assertTrue(profile.hasMedicalInformation());
        assertEquals(
                100,
                profile.completionPercentage()
        );
    }

    @Test
    public void completionCountsOnlyNonBlankFields() {
        EmergencyProfile profile =
                new EmergencyProfile(
                        "Sam",
                        " ",
                        "",
                        "None known",
                        "",
                        "",
                        "",
                        "Alex",
                        "",
                        ""
                );

        assertEquals(
                30,
                profile.completionPercentage()
        );
    }

    @Test
    public void constructorNormalizesWhitespaceAndNulls() {
        EmergencyProfile profile =
                new EmergencyProfile(
                        "  Sam  ",
                        null,
                        " O+ ",
                        null,
                        "",
                        "",
                        "",
                        " Alex ",
                        null,
                        " 555-0100 "
                );

        assertEquals("Sam", profile.preferredName());
        assertEquals("", profile.dateOfBirth());
        assertEquals("O+", profile.bloodType());
        assertEquals("", profile.allergies());
        assertEquals("Alex", profile.emergencyContactName());
        assertEquals("", profile.emergencyContactRelationship());
        assertEquals("555-0100", profile.emergencyContactPhone());
    }

    @Test
    public void toStringDoesNotExposeSensitiveDetails() {
        EmergencyProfile profile =
                new EmergencyProfile(
                        "Private Name",
                        "2000-01-15",
                        "AB-",
                        "Private Allergy",
                        "Private Medication",
                        "Private Condition",
                        "Private Accessibility Need",
                        "Private Contact",
                        "Parent",
                        "555-0100"
                );

        String output = profile.toString();

        assertFalse(output.contains("Private Name"));
        assertFalse(output.contains("Private Allergy"));
        assertFalse(output.contains("555-0100"));
        assertTrue(output.contains("100%"));
    }
}