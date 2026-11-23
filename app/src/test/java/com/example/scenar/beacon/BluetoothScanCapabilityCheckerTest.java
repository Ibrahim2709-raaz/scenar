package com.example.scenar.beacon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.Manifest;

import org.junit.Test;

import java.util.Collections;

public class BluetoothScanCapabilityCheckerTest {
    @Test
    public void readyWhenRequirementsAreAvailable() {
        BluetoothScanCapability result =
                BluetoothScanCapabilityChecker.evaluate(
                        true,
                        true,
                        true,
                        true,
                        Collections.emptyList()
                );

        assertTrue(result.isReady());
    }

    @Test
    public void reportsMissingScanPermission() {
        BluetoothScanCapability result =
                BluetoothScanCapabilityChecker.evaluate(
                        true,
                        true,
                        true,
                        true,
                        Collections.singletonList(
                                Manifest.permission.BLUETOOTH_SCAN
                        )
                );

        assertEquals(
                BluetoothScanCapability.Status.PERMISSION_REQUIRED,
                result.status()
        );
    }

    @Test
    public void reportsDisabledBluetooth() {
        BluetoothScanCapability result =
                BluetoothScanCapabilityChecker.evaluate(
                        true,
                        true,
                        true,
                        false,
                        Collections.emptyList()
                );

        assertEquals(
                BluetoothScanCapability.Status.BLUETOOTH_DISABLED,
                result.status()
        );
    }
}