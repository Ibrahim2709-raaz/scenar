package com.example.scenar.beacon;

public interface BeaconScanner {
    enum State {
        STOPPED,
        SCANNING
    }

    interface Listener {
        void onBeaconReceived(
                BeaconPayload payload,
                int rssi
        );

        void onPacketRejected(
                BeaconPacketReceiver.Status status,
                String message
        );

        void onScanFailure(String message);
    }

    void start(Listener listener);

    void stop();

    State getState();
}