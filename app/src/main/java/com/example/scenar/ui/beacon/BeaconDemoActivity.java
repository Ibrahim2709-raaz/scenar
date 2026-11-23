package com.example.scenar.ui.beacon;

import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.scenar.R;
import com.example.scenar.beacon.AndroidBleScanner;
import com.example.scenar.beacon.BeaconBroadcastController;
import com.example.scenar.beacon.BeaconPacketReceiver;
import com.example.scenar.beacon.BeaconPayload;
import com.example.scenar.beacon.BeaconScanner;
import com.example.scenar.beacon.BluetoothPermissionPolicy;
import com.example.scenar.model.IncidentReport;
import com.example.scenar.repository.IncidentRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class BeaconDemoActivity extends AppCompatActivity
        implements BeaconBroadcastController.Listener, BeaconScanner.Listener {

    private enum PendingAction {
        NONE,
        START_BROADCAST,
        START_SCAN
    }

    private BeaconBroadcastController broadcastController;
    private BeaconScanner beaconScanner;

    private Button startBroadcastButton;
    private Button stopBroadcastButton;
    private Button startScanButton;
    private Button stopScanButton;

    private TextView broadcastStatus;
    private TextView broadcastDetails;
    private TextView scanStatus;
    private TextView scanDetails;

    private PendingAction pendingAction = PendingAction.NONE;

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestMultiplePermissions(),
                    this::handlePermissionResult
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_beacon_demo);

        broadcastController = new BeaconBroadcastController(this);
        beaconScanner = new AndroidBleScanner(this);

        startBroadcastButton = findViewById(R.id.start_beacon_button);
        stopBroadcastButton = findViewById(R.id.stop_beacon_button);
        startScanButton = findViewById(R.id.start_scan_button);
        stopScanButton = findViewById(R.id.stop_scan_button);

        broadcastStatus = findViewById(R.id.beacon_status);
        broadcastDetails = findViewById(R.id.beacon_details);
        scanStatus = findViewById(R.id.scan_status);
        scanDetails = findViewById(R.id.scan_details);

        startBroadcastButton.setOnClickListener(
                ignored -> requestPermissionsAndRun(
                        PendingAction.START_BROADCAST
                )
        );

        stopBroadcastButton.setOnClickListener(
                ignored -> stopBroadcast()
        );

        startScanButton.setOnClickListener(
                ignored -> requestPermissionsAndRun(
                        PendingAction.START_SCAN
                )
        );

        stopScanButton.setOnClickListener(
                ignored -> stopScan()
        );

        updateBroadcastButtons(false);
        updateScanButtons(false);
    }

    private void requestPermissionsAndRun(PendingAction action) {
        List<String> requiredPermissions;

        if (action == PendingAction.START_BROADCAST) {
            requiredPermissions =
                    BluetoothPermissionPolicy.requiredRuntimePermissions(
                            Build.VERSION.SDK_INT
                    );
        } else {
            requiredPermissions =
                    BluetoothPermissionPolicy.requiredScanRuntimePermissions(
                            Build.VERSION.SDK_INT
                    );
        }

        List<String> missingPermissions = new ArrayList<>();

        for (String permission : requiredPermissions) {
            if (checkSelfPermission(permission)
                    != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission);
            }
        }

        if (missingPermissions.isEmpty()) {
            runAction(action);
            return;
        }

        pendingAction = action;
        permissionLauncher.launch(
                missingPermissions.toArray(new String[0])
        );
    }

    private void handlePermissionResult(
            Map<String, Boolean> permissionResults
    ) {
        boolean granted = true;

        for (Boolean result : permissionResults.values()) {
            if (!Boolean.TRUE.equals(result)) {
                granted = false;
                break;
            }
        }

        PendingAction action = pendingAction;
        pendingAction = PendingAction.NONE;

        if (!granted) {
            if (action == PendingAction.START_BROADCAST) {
                broadcastStatus.setText(
                        R.string.beacon_permission_denied
                );
            } else if (action == PendingAction.START_SCAN) {
                scanStatus.setText(
                        R.string.scan_permission_denied
                );
            }
            return;
        }

        runAction(action);
    }

    private void runAction(PendingAction action) {
        if (action == PendingAction.START_BROADCAST) {
            startBroadcast();
        } else if (action == PendingAction.START_SCAN) {
            startScan();
        }
    }

    private void startBroadcast() {
        if (broadcastController.isActive()) {
            broadcastStatus.setText(
                    R.string.beacon_already_active
            );
            return;
        }

        Double latitude = null;
        Double longitude = null;

        IncidentReport report =
                IncidentRepository.getInstance().getCurrentReport();

        if (report != null
                && report.getLatitude() != null
                && report.getLongitude() != null) {
            latitude = report.getLatitude();
            longitude = report.getLongitude();
        }

        broadcastStatus.setText(R.string.beacon_starting);
        broadcastDetails.setText(
                latitude == null
                        ? R.string.beacon_without_location
                        : R.string.beacon_with_report_location
        );

        updateBroadcastButtons(true);

        broadcastController.start(
                latitude,
                longitude,
                this
        );
    }

    private void stopBroadcast() {
        if (!broadcastController.isActive()) {
            return;
        }

        broadcastStatus.setText(R.string.beacon_stopping);
        broadcastController.stop();
    }

    private void startScan() {
        if (beaconScanner.getState()
                == BeaconScanner.State.SCANNING) {
            scanStatus.setText(R.string.scan_already_active);
            return;
        }

        scanStatus.setText(R.string.scan_starting);
        scanDetails.setText(R.string.no_beacon_received);

        beaconScanner.start(this);

        if (beaconScanner.getState()
                == BeaconScanner.State.SCANNING) {
            scanStatus.setText(R.string.scan_active);
            updateScanButtons(true);
        } else {
            updateScanButtons(false);
        }
    }

    private void stopScan() {
        beaconScanner.stop();
        scanStatus.setText(R.string.scan_stopped);
        updateScanButtons(false);
    }

    private void updateBroadcastButtons(boolean active) {
        startBroadcastButton.setEnabled(!active);
        stopBroadcastButton.setEnabled(active);
    }

    private void updateScanButtons(boolean active) {
        startScanButton.setEnabled(!active);
        stopScanButton.setEnabled(active);
    }

    @Override
    public void onBroadcastStarted(BeaconPayload payload) {
        runOnUiThread(() -> {
            broadcastStatus.setText(R.string.beacon_active);
            broadcastDetails.setText(formatPayload(payload, null));
            updateBroadcastButtons(true);
        });
    }

    @Override
    public void onPayloadUpdated(BeaconPayload payload) {
        runOnUiThread(() -> {
            broadcastStatus.setText(
                    R.string.beacon_payload_updated
            );
            broadcastDetails.setText(formatPayload(payload, null));
        });
    }

    @Override
    public void onBroadcastStopped() {
        runOnUiThread(() -> {
            broadcastStatus.setText(R.string.beacon_stopped);
            updateBroadcastButtons(false);
        });
    }

    @Override
    public void onBroadcastFailure(String message) {
        runOnUiThread(() -> {
            broadcastStatus.setText(
                    message == null
                            ? getString(R.string.beacon_failed)
                            : message
            );
            updateBroadcastButtons(false);
        });
    }

    @Override
    public void onBeaconReceived(
            BeaconPayload payload,
            int rssi
    ) {
        runOnUiThread(() -> {
            scanStatus.setText(R.string.beacon_detected);
            scanDetails.setText(formatPayload(payload, rssi));
        });
    }

    @Override
    public void onPacketRejected(
            BeaconPacketReceiver.Status status,
            String message
    ) {
        runOnUiThread(() -> scanStatus.setText(
                message == null
                        ? getString(R.string.packet_rejected)
                        : message
        ));
    }

    @Override
    public void onScanFailure(String message) {
        runOnUiThread(() -> {
            scanStatus.setText(
                    message == null
                            ? getString(R.string.scan_failed)
                            : message
            );
            updateScanButtons(false);
        });
    }

    private String formatPayload(
            BeaconPayload payload,
            Integer rssi
    ) {
        String location;

        if (payload.hasLocation()) {
            location = String.format(
                    Locale.ROOT,
                    "%.5f, %.5f",
                    payload.latitude(),
                    payload.longitude()
            );
        } else {
            location = "Not included";
        }

        String details =
                "Anonymous victim ID: "
                        + payload.anonymousBeaconId()
                        + "\nLocation: "
                        + location
                        + "\nGenerated at: "
                        + payload.generatedAtEpochSeconds()
                        + "\nProtocol version: "
                        + payload.protocolVersion();

        if (rssi != null) {
            details += "\nSignal strength: "
                    + rssi
                    + " dBm";
        }

        return details;
    }

    @Override
    protected void onDestroy() {
        if (broadcastController != null) {
            broadcastController.stop();
        }

        if (beaconScanner != null) {
            beaconScanner.stop();
        }

        super.onDestroy();
    }
}