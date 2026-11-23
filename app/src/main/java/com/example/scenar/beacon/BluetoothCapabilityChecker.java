package com.example.scenar.beacon;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BluetoothCapabilityChecker {
    public BluetoothCapability check(Context context) {
        if (context == null) {
            throw new IllegalArgumentException(
                    "An Android context is required."
            );
        }

        Context applicationContext =
                context.getApplicationContext();

        PackageManager packageManager =
                applicationContext.getPackageManager();

        boolean supportsBle = packageManager.hasSystemFeature(
                PackageManager.FEATURE_BLUETOOTH_LE
        );

        if (!supportsBle) {
            return BluetoothCapability.bleUnsupported();
        }

        BluetoothManager bluetoothManager =
                applicationContext.getSystemService(
                        BluetoothManager.class
                );

        if (bluetoothManager == null) {
            return BluetoothCapability.serviceUnavailable();
        }

        BluetoothAdapter adapter =
                bluetoothManager.getAdapter();

        if (adapter == null) {
            return BluetoothCapability.serviceUnavailable();
        }

        List<String> missingPermissions =
                findMissingRuntimePermissions(
                        applicationContext,
                        Build.VERSION.SDK_INT
                );

        if (!missingPermissions.isEmpty()) {
            return BluetoothCapability.permissionRequired(
                    missingPermissions
            );
        }

        try {
            boolean enabled = adapter.isEnabled();
            boolean advertisingSupported =
                    adapter.isMultipleAdvertisementSupported();

            return evaluate(
                    true,
                    true,
                    true,
                    enabled,
                    advertisingSupported,
                    Collections.emptyList()
            );
        } catch (SecurityException exception) {
            return BluetoothCapability.permissionRequired(
                    BluetoothPermissionPolicy
                            .requiredRuntimePermissions(
                                    Build.VERSION.SDK_INT
                            )
            );
        }
    }

    static BluetoothCapability evaluate(
            boolean supportsBle,
            boolean hasBluetoothManager,
            boolean hasBluetoothAdapter,
            boolean bluetoothEnabled,
            boolean advertisingSupported,
            List<String> missingPermissions
    ) {
        if (!supportsBle) {
            return BluetoothCapability.bleUnsupported();
        }

        if (!hasBluetoothManager
                || !hasBluetoothAdapter) {
            return BluetoothCapability.serviceUnavailable();
        }

        if (missingPermissions != null
                && !missingPermissions.isEmpty()) {
            return BluetoothCapability.permissionRequired(
                    missingPermissions
            );
        }

        if (!bluetoothEnabled) {
            return BluetoothCapability.bluetoothDisabled();
        }

        if (!advertisingSupported) {
            return BluetoothCapability.advertisingUnsupported();
        }

        return BluetoothCapability.ready();
    }

    private List<String> findMissingRuntimePermissions(
            Context context,
            int sdkVersion
    ) {
        List<String> requiredPermissions =
                BluetoothPermissionPolicy
                        .requiredRuntimePermissions(sdkVersion);

        List<String> missingPermissions =
                new ArrayList<>();

        for (String permission : requiredPermissions) {
            if (context.checkSelfPermission(permission)
                    != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission);
            }
        }

        return missingPermissions;
    }
}