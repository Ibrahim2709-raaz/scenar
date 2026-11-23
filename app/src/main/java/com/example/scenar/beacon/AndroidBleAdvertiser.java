package com.example.scenar.beacon;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.os.ParcelUuid;

import java.util.Objects;
import java.util.UUID;

public final class AndroidBleAdvertiser
        implements BeaconAdvertiser {

    /*
     * Prototype-only 16-bit-style service UUID.
     *
     * Production use should obtain an appropriate assigned identifier
     * or move to a project-owned 128-bit protocol with an adjusted payload.
     */
    public static final ParcelUuid PROTOTYPE_SERVICE_UUID =
            new ParcelUuid(
                    UUID.fromString(
                            "0000fff0-0000-1000-8000-00805f9b34fb"
                    )
            );

    private final Context applicationContext;
    private final BluetoothCapabilityChecker capabilityChecker;
    private final Object stateLock = new Object();

    private State state = State.STOPPED;
    private BluetoothLeAdvertiser platformAdvertiser;
    private AdvertiseCallback platformCallback;
    private Callback clientCallback;
    private long operationId;

    public AndroidBleAdvertiser(Context context) {
        this(
                context,
                new BluetoothCapabilityChecker()
        );
    }

    AndroidBleAdvertiser(
            Context context,
            BluetoothCapabilityChecker capabilityChecker
    ) {
        if (context == null) {
            throw new IllegalArgumentException(
                    "An Android context is required."
            );
        }

        this.applicationContext =
                context.getApplicationContext();

        this.capabilityChecker = Objects.requireNonNull(
                capabilityChecker,
                "capabilityChecker"
        );
    }

    @Override
    public void start(
            BeaconPayload payload,
            Callback callback
    ) {
        if (payload == null) {
            notifyFailure(
                    callback,
                    "A beacon payload is required."
            );
            return;
        }

        if (callback == null) {
            throw new IllegalArgumentException(
                    "A beacon callback is required."
            );
        }

        synchronized (stateLock) {
            if (state != State.STOPPED) {
                callback.onFailure(
                        "The BLE beacon is already starting or advertising."
                );
                return;
            }
        }

        BluetoothCapability capability =
                capabilityChecker.check(applicationContext);

        if (!capability.isReady()) {
            callback.onFailure(capability.message());
            return;
        }

        BluetoothManager bluetoothManager =
                applicationContext.getSystemService(
                        BluetoothManager.class
                );

        if (bluetoothManager == null) {
            callback.onFailure(
                    "The Android Bluetooth service is unavailable."
            );
            return;
        }

        BluetoothAdapter adapter =
                bluetoothManager.getAdapter();

        if (adapter == null) {
            callback.onFailure(
                    "This device does not have a Bluetooth adapter."
            );
            return;
        }

        final BluetoothLeAdvertiser advertiser;

        try {
            advertiser = adapter.getBluetoothLeAdvertiser();
        } catch (SecurityException exception) {
            callback.onFailure(
                    "Bluetooth advertising permission is required."
            );
            return;
        }

        if (advertiser == null) {
            callback.onFailure(
                    "Bluetooth LE advertising is unavailable."
            );
            return;
        }

        byte[] encodedPayload =
                BeaconPayloadCodec.encode(payload);

        AdvertiseSettings settings =
                createAdvertiseSettings();

        AdvertiseData data =
                createAdvertiseData(encodedPayload);

        final long currentOperation;

        synchronized (stateLock) {
            operationId++;
            currentOperation = operationId;
            state = State.STARTING;
            platformAdvertiser = advertiser;
            clientCallback = callback;
        }

        AdvertiseCallback androidCallback =
                createPlatformCallback(
                        advertiser,
                        currentOperation
                );

        synchronized (stateLock) {
            if (operationId != currentOperation
                    || state != State.STARTING) {
                callback.onFailure(
                        "The BLE beacon start operation was cancelled."
                );
                return;
            }

            platformCallback = androidCallback;
        }

        try {
            advertiser.startAdvertising(
                    settings,
                    data,
                    androidCallback
            );
        } catch (SecurityException exception) {
            failCurrentOperation(
                    currentOperation,
                    "Bluetooth advertising permission is required."
            );
        } catch (IllegalArgumentException exception) {
            failCurrentOperation(
                    currentOperation,
                    "The BLE advertisement data is invalid or too large."
            );
        } catch (IllegalStateException exception) {
            failCurrentOperation(
                    currentOperation,
                    "Bluetooth advertising could not be started."
            );
        }
    }

    @Override
    public void stop() {
        final BluetoothLeAdvertiser advertiser;
        final AdvertiseCallback callback;
        final Callback listener;

        synchronized (stateLock) {
            if (state == State.STOPPED) {
                return;
            }

            operationId++;
            advertiser = platformAdvertiser;
            callback = platformCallback;
            listener = clientCallback;

            clearStateLocked();
        }

        if (advertiser != null && callback != null) {
            try {
                advertiser.stopAdvertising(callback);
            } catch (SecurityException ignored) {
                // Local state is still stopped. The caller will receive
                // a stopped result instead of a false active state.
            } catch (IllegalStateException ignored) {
                // Bluetooth may have been disabled during advertising.
            }
        }

        if (listener != null) {
            listener.onStopped();
        }
    }

    @Override
    public State getState() {
        synchronized (stateLock) {
            return state;
        }
    }

    private AdvertiseCallback createPlatformCallback(
            BluetoothLeAdvertiser advertiser,
            long callbackOperation
    ) {
        return new AdvertiseCallback() {
            @Override
            public void onStartSuccess(
                    AdvertiseSettings settingsInEffect
            ) {
                Callback listener;
                boolean stale;

                synchronized (stateLock) {
                    stale = operationId != callbackOperation
                            || state != State.STARTING;

                    if (stale) {
                        listener = null;
                    } else {
                        state = State.ADVERTISING;
                        listener = clientCallback;
                    }
                }

                if (stale) {
                    try {
                        advertiser.stopAdvertising(this);
                    } catch (SecurityException
                             | IllegalStateException ignored) {
                        // The stale operation is already considered stopped.
                    }
                    return;
                }

                if (listener != null) {
                    listener.onStarted();
                }
            }

            @Override
            public void onStartFailure(int errorCode) {
                failCurrentOperation(
                        callbackOperation,
                        messageForFailureCode(errorCode)
                );
            }
        };
    }

    private void failCurrentOperation(
            long failedOperation,
            String message
    ) {
        final Callback listener;

        synchronized (stateLock) {
            if (operationId != failedOperation) {
                return;
            }

            listener = clientCallback;
            clearStateLocked();
        }

        if (listener != null) {
            listener.onFailure(message);
        }
    }

    private void clearStateLocked() {
        state = State.STOPPED;
        platformAdvertiser = null;
        platformCallback = null;
        clientCallback = null;
    }

    private static AdvertiseSettings createAdvertiseSettings() {
        return new AdvertiseSettings.Builder()
                .setAdvertiseMode(
                        AdvertiseSettings.ADVERTISE_MODE_LOW_POWER
                )
                .setTxPowerLevel(
                        AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM
                )
                .setConnectable(false)
                .setTimeout(0)
                .build();
    }

    private static AdvertiseData createAdvertiseData(
            byte[] encodedPayload
    ) {
        return new AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .setIncludeTxPowerLevel(false)
                .addServiceData(
                        PROTOTYPE_SERVICE_UUID,
                        encodedPayload
                )
                .build();
    }

    static String messageForFailureCode(int errorCode) {
        return switch (errorCode) {
            case AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE ->
                    "The BLE advertisement is too large.";

            case AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS ->
                    "This device is already running too many BLE advertisers.";

            case AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED ->
                    "The BLE beacon has already been started.";

            case AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR ->
                    "Android reported an internal BLE advertising error.";

            case AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED ->
                    "This device does not support BLE advertising.";

            default ->
                    "BLE advertising failed with error code "
                            + errorCode
                            + ".";
        };
    }

    private static void notifyFailure(
            Callback callback,
            String message
    ) {
        if (callback != null) {
            callback.onFailure(message);
        }
    }
}