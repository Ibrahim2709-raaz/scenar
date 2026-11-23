package com.example.scenar.beacon;

import java.util.Arrays;

public final class SimulatedBeaconService {
    public static final long BROADCAST_INTERVAL_MILLIS = 5_000L;

    public enum State {
        STOPPED,
        BROADCASTING_SIMULATION
    }

    private State state = State.STOPPED;
    private BeaconPayload activePayload;

    public synchronized void start(BeaconPayload payload) {
        if (payload == null) {
            throw new IllegalArgumentException(
                    "A beacon payload is required."
            );
        }

        activePayload = payload;
        state = State.BROADCASTING_SIMULATION;
    }

    public synchronized void stop() {
        activePayload = null;
        state = State.STOPPED;
    }

    public synchronized State getState() {
        return state;
    }

    public synchronized boolean isRunning() {
        return state == State.BROADCASTING_SIMULATION;
    }

    public synchronized String getStatusLabel() {
        if (isRunning()) {
            return "BLE beacon simulation active — no real signal is being broadcast";
        }

        return "BLE beacon simulation stopped";
    }

    public synchronized byte[] createNextPacket() {
        if (!isRunning() || activePayload == null) {
            throw new IllegalStateException(
                    "Start the beacon simulation before creating a packet."
            );
        }

        byte[] encoded = BeaconPayloadCodec.encode(activePayload);
        return Arrays.copyOf(encoded, encoded.length);
    }
}