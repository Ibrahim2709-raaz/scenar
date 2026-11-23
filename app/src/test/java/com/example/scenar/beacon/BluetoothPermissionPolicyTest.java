package com.example.scenar.beacon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.Manifest;
import android.os.Build;

import org.junit.Test;

import java.util.List;

public class BluetoothPermissionPolicyTest {
    @Test
    public void androidTwelveRequiresAdvertiseAndConnectAtRuntime() {
        List<String> permissions =
                BluetoothPermissionPolicy
                        .requiredRuntimePermissions(
                                Build.VERSION_CODES.S
                        );

        assertEquals(2, permissions.size());
        assertTrue(
                permissions.contains(
                        Manifest.permission.BLUETOOTH_ADVERTISE
                )
        );
        assertTrue(
                permissions.contains(
                        Manifest.permission.BLUETOOTH_CONNECT
                )
        );
        assertTrue(
                BluetoothPermissionPolicy
                        .requiresRuntimePermission(
                                Build.VERSION_CODES.S
                        )
        );
    }

    @Test
    public void olderAndroidDoesNotRequireRuntimeBluetoothPermission() {
        List<String> runtimePermissions =
                BluetoothPermissionPolicy
                        .requiredRuntimePermissions(
                                Build.VERSION_CODES.R
                        );

        assertTrue(runtimePermissions.isEmpty());
        assertFalse(
                BluetoothPermissionPolicy
                        .requiresRuntimePermission(
                                Build.VERSION_CODES.R
                        )
        );
    }

    @Test
    public void olderAndroidStillRequiresManifestPermissions() {
        List<String> manifestPermissions =
                BluetoothPermissionPolicy
                        .requiredManifestPermissions(
                                Build.VERSION_CODES.R
                        );

        assertEquals(2, manifestPermissions.size());
        assertTrue(
                manifestPermissions.contains(
                        Manifest.permission.BLUETOOTH
                )
        );
        assertTrue(
                manifestPermissions.contains(
                        Manifest.permission.BLUETOOTH_ADMIN
                )
        );
    }
}