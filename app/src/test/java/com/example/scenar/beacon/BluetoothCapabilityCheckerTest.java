package com.example.scenar.beacon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.Manifest;

import org.junit.Test;

import java.util.Collections;
import java.util.List;

public class BluetoothCapabilityCheckerTest {
    @Test
    public void readyWhenAllRequirementsAreAvailable() {
        BluetoothCapability result =
                BluetoothCapabilityChecker.evaluate(
                        true,
                        true,
                        true,
                        true,
                        true,
                        Collections.emptyList()
                );

        assertTrue(result.isReady());
        assertEquals(
                BluetoothCapability.Status.READY,
                result.status()
        );
    }

    @Test
    public void rejectsDeviceWithoutBle() {
        BluetoothCapability result =
                BluetoothCapabilityChecker.evaluate(
                        false,
                        true,
                        true,
                        true,
                        true,
                        Collections.emptyList()
                );

        assertFalse(result.isReady());
        assertEquals(
                BluetoothCapability.Status.BLE_UNSUPPORTED,
                result.status()
        );
    }

    @Test
    public void reportsUnavailableBluetoothService() {
        BluetoothCapability result =
                BluetoothCapabilityChecker.evaluate(
                        true,
                        false,
                        false,
                        false,
                        false,
                        Collections.emptyList()
                );

        assertEquals(
                BluetoothCapability.Status.SERVICE_UNAVAILABLE,
                result.status()
        );
    }

    @Test
    public void reportsMissingPermissionBeforeBluetoothState() {
        List<String> missing = Collections.singletonList(
                Manifest.permission.BLUETOOTH_ADVERTISE
        );

        BluetoothCapability result =
                BluetoothCapabilityChecker.evaluate(
                        true,
                        true,
                        true,
                        false,
                        false,
                        missing
                );

        assertEquals(
                BluetoothCapability.Status.PERMISSION_REQUIRED,
                result.status()
        );
        assertTrue(result.requiresUserAction());
        assertEquals(missing, result.missingPermissions());
    }

    @Test
    public void reportsDisabledBluetooth() {
        BluetoothCapability result =
                BluetoothCapabilityChecker.evaluate(
                        true,
                        true,
                        true,
                        false,
                        true,
                        Collections.emptyList()
                );

        assertEquals(
                BluetoothCapability.Status.BLUETOOTH_DISABLED,
                result.status()
        );
        assertTrue(result.requiresUserAction());
    }

    @Test
    public void reportsUnsupportedAdvertising() {
        BluetoothCapability result =
                BluetoothCapabilityChecker.evaluate(
                        true,
                        true,
                        true,
                        true,
                        false,
                        Collections.emptyList()
                );

        assertEquals(
                BluetoothCapability.Status.ADVERTISING_UNSUPPORTED,
                result.status()
        );
        assertFalse(result.requiresUserAction());
    }

    @Test
    public void missingPermissionsCannotBeModifiedExternally() {
        List<String> missing = new java.util.ArrayList<>();
        missing.add(
                Manifest.permission.BLUETOOTH_CONNECT
        );

        BluetoothCapability capability =
                BluetoothCapability.permissionRequired(missing);

        missing.clear();

        assertEquals(
                1,
                capability.missingPermissions().size()
        );
    }
}