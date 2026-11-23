package com.example.scenar.beacon;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BluetoothScanCapabilityChecker {
    public BluetoothScanCapability check(Context context) {
        if (context == null) {
            throw new IllegalArgumentException(
                    "An Android context is required."
            );
        }

        Context applicationContext =
                context.getApplicationContext();

        boolean supportsBle =
                applicationContext.getPackageManager()
                        .hasSystemFeature(
                                PackageManager.FEATURE_BLUETOOTH_LE
                        );

        if (!supportsBle) {
            return BluetoothScanCapability.bleUnsupported();
        }

        BluetoothManager manager =
                applicationContext.getSystemService(
                        BluetoothManager.class
                );

        if (manager == null) {
            return BluetoothScanCapability.serviceUnavailable();
        }

        BluetoothAdapter adapter = manager.getAdapter();

        if (adapter == null) {
            return BluetoothScanCapability.serviceUnavailable();
        }

        List<String> missingPermissions =
                findMissingPermissions(
                        applicationContext,
                        Build.VERSION.SDK_INT
                );

        if (!missingPermissions.isEmpty()) {
            return BluetoothScanCapability.permissionRequired(
                    missingPermissions
            );
        }

        try {
            return evaluate(
                    true,
                    true,
                    true,
                    adapter.isEnabled(),
                    Collections.emptyList()
            );
        } catch (SecurityException exception) {
            return BluetoothScanCapability.permissionRequired(
                    BluetoothPermissionPolicy
                            .requiredScanRuntimePermissions(
                                    Build.VERSION.SDK_INT
                            )
            );
        }
    }

    static BluetoothScanCapability evaluate(
            boolean supportsBle,
            boolean hasManager,
            boolean hasAdapter,
            boolean bluetoothEnabled,
            List<String> missingPermissions
    ) {
        if (!supportsBle) {
            return BluetoothScanCapability.bleUnsupported();
        }

        if (!hasManager || !hasAdapter) {
            return BluetoothScanCapability.serviceUnavailable();
        }

        if (missingPermissions != null
                && !missingPermissions.isEmpty()) {
            return BluetoothScanCapability.permissionRequired(
                    missingPermissions
            );
        }

        if (!bluetoothEnabled) {
            return BluetoothScanCapability.bluetoothDisabled();
        }

        return BluetoothScanCapability.ready();
    }

    private List<String> findMissingPermissions(
            Context context,
            int sdkVersion
    ) {
        List<String> missing = new ArrayList<>();

        for (String permission :
                BluetoothPermissionPolicy
                        .requiredScanRuntimePermissions(sdkVersion)) {

            if (context.checkSelfPermission(permission)
                    != PackageManager.PERMISSION_GRANTED) {
                missing.add(permission);
            }
        }

        return missing;
    }
}