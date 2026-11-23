package com.example.scenar.beacon;

import android.Manifest;
import android.os.Build;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BluetoothPermissionPolicy {
    private BluetoothPermissionPolicy() {
    }

    public static List<String> requiredManifestPermissions(
            int sdkVersion
    ) {
        List<String> permissions = new ArrayList<>();

        if (sdkVersion >= Build.VERSION_CODES.S) {
            permissions.add(
                    Manifest.permission.BLUETOOTH_ADVERTISE
            );
            permissions.add(
                    Manifest.permission.BLUETOOTH_CONNECT
            );
        } else {
            permissions.add(
                    Manifest.permission.BLUETOOTH
            );
            permissions.add(
                    Manifest.permission.BLUETOOTH_ADMIN
            );
        }

        return Collections.unmodifiableList(permissions);
    }

    public static List<String> requiredRuntimePermissions(
            int sdkVersion
    ) {
        if (sdkVersion < Build.VERSION_CODES.S) {
            return Collections.emptyList();
        }

        List<String> permissions = new ArrayList<>();
        permissions.add(
                Manifest.permission.BLUETOOTH_ADVERTISE
        );
        permissions.add(
                Manifest.permission.BLUETOOTH_CONNECT
        );

        return Collections.unmodifiableList(permissions);
    }

    public static boolean requiresRuntimePermission(
            int sdkVersion
    ) {
        return sdkVersion >= Build.VERSION_CODES.S;
    }
}