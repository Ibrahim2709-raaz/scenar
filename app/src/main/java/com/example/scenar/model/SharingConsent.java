package com.example.scenar.model;

import java.io.Serializable;

public record SharingConsent(
        boolean shareLocation,
        boolean shareMedicalFlags,
        boolean notifyEmergencyContacts
) implements Serializable {
    public static SharingConsent shareNothing() {
        return new SharingConsent(false, false, false);
    }

    public static SharingConsent shareReportDetails() {
        return new SharingConsent(true, true, false);
    }
}
