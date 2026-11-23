package com.example.scenar.beacon;

import static org.junit.Assert.assertTrue;

import android.bluetooth.le.AdvertiseCallback;

import org.junit.Test;

public class AndroidBleAdvertiserTest {
    @Test
    public void mapsDataTooLargeFailure() {
        String message =
                AndroidBleAdvertiser.messageForFailureCode(
                        AdvertiseCallback
                                .ADVERTISE_FAILED_DATA_TOO_LARGE
                );

        assertTrue(
                message.toLowerCase().contains("too large")
        );
    }

    @Test
    public void mapsUnsupportedDeviceFailure() {
        String message =
                AndroidBleAdvertiser.messageForFailureCode(
                        AdvertiseCallback
                                .ADVERTISE_FAILED_FEATURE_UNSUPPORTED
                );

        assertTrue(
                message.toLowerCase().contains("does not support")
        );
    }

    @Test
    public void mapsTooManyAdvertisersFailure() {
        String message =
                AndroidBleAdvertiser.messageForFailureCode(
                        AdvertiseCallback
                                .ADVERTISE_FAILED_TOO_MANY_ADVERTISERS
                );

        assertTrue(
                message.toLowerCase()
                        .contains("too many")
        );
    }

    @Test
    public void preservesUnknownFailureCode() {
        String message =
                AndroidBleAdvertiser.messageForFailureCode(999);

        assertTrue(message.contains("999"));
    }
}