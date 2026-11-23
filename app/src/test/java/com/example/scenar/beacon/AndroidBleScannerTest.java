package com.example.scenar.beacon;

import static org.junit.Assert.assertTrue;

import android.bluetooth.le.ScanCallback;

import org.junit.Test;

public class AndroidBleScannerTest {
    @Test
    public void mapsUnsupportedFailure() {
        String message =
                AndroidBleScanner.messageForFailureCode(
                        ScanCallback
                                .SCAN_FAILED_FEATURE_UNSUPPORTED
                );

        assertTrue(
                message.toLowerCase()
                        .contains("does not support")
        );
    }

    @Test
    public void mapsFrequentScanningFailure() {
        String message =
                AndroidBleScanner.messageForFailureCode(
                        ScanCallback
                                .SCAN_FAILED_SCANNING_TOO_FREQUENTLY
                );

        assertTrue(
                message.toLowerCase()
                        .contains("too frequently")
        );
    }

    @Test
    public void preservesUnknownFailureCode() {
        assertTrue(
                AndroidBleScanner
                        .messageForFailureCode(999)
                        .contains("999")
        );
    }
}