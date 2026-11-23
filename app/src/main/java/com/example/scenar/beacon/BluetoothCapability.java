package com.example.scenar.beacon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record BluetoothCapability(
        Status status,
        String message,
        List<String> missingPermissions
) {
    public enum Status {
        READY,
        BLE_UNSUPPORTED,
        ADVERTISING_UNSUPPORTED,
        BLUETOOTH_DISABLED,
        PERMISSION_REQUIRED,
        SERVICE_UNAVAILABLE
    }

    public BluetoothCapability {
        if (status == null) {
            throw new IllegalArgumentException(
                    "A Bluetooth capability status is required."
            );
        }

        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException(
                    "A Bluetooth capability message is required."
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

    public boolean requiresUserAction() {
        return status == Status.BLUETOOTH_DISABLED
                || status == Status.PERMISSION_REQUIRED;
    }

    public static BluetoothCapability ready() {
        return new BluetoothCapability(
                Status.READY,
                "Bluetooth LE advertising is available.",
                Collections.emptyList()
        );
    }

    public static BluetoothCapability bleUnsupported() {
        return new BluetoothCapability(
                Status.BLE_UNSUPPORTED,
                "This device does not support Bluetooth Low Energy.",
                Collections.emptyList()
        );
    }

    public static BluetoothCapability advertisingUnsupported() {
        return new BluetoothCapability(
                Status.ADVERTISING_UNSUPPORTED,
                "This device cannot advertise Bluetooth LE packets.",
                Collections.emptyList()
        );
    }

    public static BluetoothCapability bluetoothDisabled() {
        return new BluetoothCapability(
                Status.BLUETOOTH_DISABLED,
                "Bluetooth is turned off. Enable it before starting the beacon.",
                Collections.emptyList()
        );
    }

    public static BluetoothCapability permissionRequired(
            List<String> permissions
    ) {
        return new BluetoothCapability(
                Status.PERMISSION_REQUIRED,
                "Bluetooth permission is required before starting the beacon.",
                permissions
        );
    }

    public static BluetoothCapability serviceUnavailable() {
        return new BluetoothCapability(
                Status.SERVICE_UNAVAILABLE,
                "The Android Bluetooth service is unavailable.",
                Collections.emptyList()
        );
    }
}