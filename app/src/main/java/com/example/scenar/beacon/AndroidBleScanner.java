package com.example.scenar.beacon;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;

import java.util.Collections;
import java.util.List;
import java.util.function.LongSupplier;

public final class AndroidBleScanner implements BeaconScanner {
    private final Context applicationContext;
    private final BluetoothScanCapabilityChecker capabilityChecker;
    private final BeaconPacketReceiver packetReceiver;
    private final LongSupplier epochSecondsSource;
    private final Object stateLock = new Object();

    private State state = State.STOPPED;
    private BluetoothLeScanner platformScanner;
    private ScanCallback platformCallback;
    private Listener listener;

    public AndroidBleScanner(Context context) {
        this(
                context,
                new BluetoothScanCapabilityChecker(),
                new BeaconPacketReceiver(),
                () -> System.currentTimeMillis() / 1_000L
        );
    }

    AndroidBleScanner(
            Context context,
            BluetoothScanCapabilityChecker capabilityChecker,
            BeaconPacketReceiver packetReceiver,
            LongSupplier epochSecondsSource
    ) {
        if (context == null
                || capabilityChecker == null
                || packetReceiver == null
                || epochSecondsSource == null) {
            throw new IllegalArgumentException(
                    "All scanner dependencies are required."
            );
        }

        applicationContext = context.getApplicationContext();
        this.capabilityChecker = capabilityChecker;
        this.packetReceiver = packetReceiver;
        this.epochSecondsSource = epochSecondsSource;
    }

    @Override
    public void start(Listener listener) {
        if (listener == null) {
            throw new IllegalArgumentException(
                    "A scanner listener is required."
            );
        }

        synchronized (stateLock) {
            if (state != State.STOPPED) {
                listener.onScanFailure(
                        "The BLE rescue scanner is already active."
                );
                return;
            }
        }

        BluetoothScanCapability capability =
                capabilityChecker.check(applicationContext);

        if (!capability.isReady()) {
            listener.onScanFailure(capability.message());
            return;
        }

        BluetoothManager manager =
                applicationContext.getSystemService(
                        BluetoothManager.class
                );

        if (manager == null) {
            listener.onScanFailure(
                    "The Android Bluetooth service is unavailable."
            );
            return;
        }

        BluetoothAdapter adapter = manager.getAdapter();

        if (adapter == null) {
            listener.onScanFailure(
                    "This device has no Bluetooth adapter."
            );
            return;
        }

        final BluetoothLeScanner scanner;

        try {
            scanner = adapter.getBluetoothLeScanner();
        } catch (SecurityException exception) {
            listener.onScanFailure(
                    "Bluetooth scanning permission is required."
            );
            return;
        }

        if (scanner == null) {
            listener.onScanFailure(
                    "Bluetooth LE scanning is unavailable."
            );
            return;
        }

        ScanCallback callback = createScanCallback();

        synchronized (stateLock) {
            this.listener = listener;
            platformScanner = scanner;
            platformCallback = callback;
            state = State.SCANNING;
        }

        ScanFilter filter = new ScanFilter.Builder()
                .setServiceUuid(
                        AndroidBleAdvertiser
                                .PROTOTYPE_SERVICE_UUID
                )
                .build();

        ScanSettings settings =
                new ScanSettings.Builder()
                        .setScanMode(
                                ScanSettings.SCAN_MODE_LOW_LATENCY
                        )
                        .build();

        try {
            scanner.startScan(
                    Collections.singletonList(filter),
                    settings,
                    callback
            );
        } catch (SecurityException exception) {
            fail(
                    "Bluetooth scanning permission is required."
            );
        } catch (IllegalArgumentException
                 | IllegalStateException exception) {
            fail(
                    "Bluetooth LE scanning could not be started."
            );
        }
    }

    @Override
    public void stop() {
        final BluetoothLeScanner scanner;
        final ScanCallback callback;

        synchronized (stateLock) {
            if (state == State.STOPPED) {
                return;
            }

            scanner = platformScanner;
            callback = platformCallback;
            clearLocked();
        }

        if (scanner != null && callback != null) {
            try {
                scanner.stopScan(callback);
            } catch (SecurityException
                     | IllegalStateException ignored) {
                // Local scanner state is already stopped.
            }
        }

        packetReceiver.clearHistory();
    }

    @Override
    public State getState() {
        synchronized (stateLock) {
            return state;
        }
    }

    private ScanCallback createScanCallback() {
        return new ScanCallback() {
            @Override
            public void onScanResult(
                    int callbackType,
                    ScanResult result
            ) {
                processResult(result);
            }

            @Override
            public void onBatchScanResults(
                    List<ScanResult> results
            ) {
                if (results == null) {
                    return;
                }

                for (ScanResult result : results) {
                    processResult(result);
                }
            }

            @Override
            public void onScanFailed(int errorCode) {
                fail(messageForFailureCode(errorCode));
            }
        };
    }

    private void processResult(ScanResult result) {
        if (result == null) {
            return;
        }

        final Listener currentListener;

        synchronized (stateLock) {
            if (state != State.SCANNING) {
                return;
            }

            currentListener = listener;
        }

        ScanRecord record = result.getScanRecord();

        if (record == null) {
            return;
        }

        byte[] serviceData = record.getServiceData(
                AndroidBleAdvertiser.PROTOTYPE_SERVICE_UUID
        );

        if (serviceData == null) {
            return;
        }

        BeaconPacketReceiver.Result receiverResult =
                packetReceiver.receive(
                        serviceData,
                        epochSecondsSource.getAsLong()
                );

        if (currentListener == null) {
            return;
        }

        if (receiverResult.isAccepted()) {
            currentListener.onBeaconReceived(
                    receiverResult.payload(),
                    result.getRssi()
            );
        } else if (receiverResult.status()
                != BeaconPacketReceiver.Status.REPLAYED) {
            /*
             * Repeated BLE advertisements are normal. Suppress replay
             * notifications to prevent UI and log flooding.
             */
            currentListener.onPacketRejected(
                    receiverResult.status(),
                    receiverResult.message()
            );
        }
    }

    private void fail(String message) {
        final Listener failedListener;
        final BluetoothLeScanner scanner;
        final ScanCallback callback;

        synchronized (stateLock) {
            if (state == State.STOPPED) {
                return;
            }

            failedListener = listener;
            scanner = platformScanner;
            callback = platformCallback;
            clearLocked();
        }

        if (scanner != null && callback != null) {
            try {
                scanner.stopScan(callback);
            } catch (SecurityException
                     | IllegalStateException ignored) {
                // Scanner state has already been cleared.
            }
        }

        packetReceiver.clearHistory();

        if (failedListener != null) {
            failedListener.onScanFailure(message);
        }
    }

    private void clearLocked() {
        state = State.STOPPED;
        platformScanner = null;
        platformCallback = null;
        listener = null;
    }

    static String messageForFailureCode(int errorCode) {
        return switch (errorCode) {
            case ScanCallback.SCAN_FAILED_ALREADY_STARTED ->
                    "The BLE scanner has already started.";

            case ScanCallback
                         .SCAN_FAILED_APPLICATION_REGISTRATION_FAILED ->
                    "Android could not register the BLE scanner.";

            case ScanCallback.SCAN_FAILED_INTERNAL_ERROR ->
                    "Android reported an internal BLE scanning error.";

            case ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED ->
                    "This device does not support BLE scanning.";

            case ScanCallback.SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES ->
                    "The device has no available BLE scanning resources.";

            case ScanCallback.SCAN_FAILED_SCANNING_TOO_FREQUENTLY ->
                    "BLE scanning was started too frequently. Wait before retrying.";

            default ->
                    "BLE scanning failed with error code "
                            + errorCode
                            + ".";
        };
    }
}