package com.example.scenar.beacon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record BluetoothScanCapability(
        Status status,
        String message,
        List<String> missingPermissions
) {
    public enum Status {
        READY,
        BLE_UNSUPPORTED,
        BLUETOOTH_DISABLED,
        PERMISSION_REQUIRED,
        SERVICE_UNAVAILABLE
    }

    public BluetoothScanCapability {
        if (status == null) {
            throw new IllegalArgumentException(
                    "A scan capability status is required."
            );
        }

        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException(
                    "A scan capability message is required."
            );
        }

        List<String> permissions = missingPermissions == null
                ? Collections.emptyList()
                : missingPermissions;

        missingPermissions = Collections.unmodifiableList(
                new ArrayList<>(permissions)
        );
    }

    public boolean isReady() {
        return status == Status.READY;
    }

    public static BluetoothScanCapability ready() {
        return new BluetoothScanCapability(
                Status.READY,
                "Bluetooth LE scanning is available.",
                Collections.emptyList()
        );
    }

    public static BluetoothScanCapability bleUnsupported() {
        return new BluetoothScanCapability(
                Status.BLE_UNSUPPORTED,
                "This device does not support Bluetooth Low Energy.",
                Collections.emptyList()
        );
    }

    public static BluetoothScanCapability bluetoothDisabled() {
        return new BluetoothScanCapability(
                Status.BLUETOOTH_DISABLED,
                "Bluetooth is turned off.",
                Collections.emptyList()
        );
    }

    public static BluetoothScanCapability permissionRequired(
            List<String> permissions
    ) {
        return new BluetoothScanCapability(
                Status.PERMISSION_REQUIRED,
                "Bluetooth scanning permission is required.",
                permissions
        );
    }

    public static BluetoothScanCapability serviceUnavailable() {
        return new BluetoothScanCapability(
                Status.SERVICE_UNAVAILABLE,
                "The Android Bluetooth scanning service is unavailable.",
                Collections.emptyList()
        );
    }
}