package com.example.scenar.beacon;

import android.content.Context;

import java.util.function.LongSupplier;

public final class BeaconBroadcastController {
    public enum State {
        STOPPED,
        STARTING,
        BROADCASTING,
        UPDATING
    }

    public interface Listener {
        void onBroadcastStarted(BeaconPayload payload);

        void onPayloadUpdated(BeaconPayload payload);

        void onBroadcastStopped();

        void onBroadcastFailure(String message);
    }

    private final BeaconAdvertiser advertiser;
    private final BeaconSession session;
    private final BeaconUpdateScheduler scheduler;
    private final LongSupplier epochSecondsSource;
    private final Object stateLock = new Object();

    private State state = State.STOPPED;
    private Listener listener;
    private BeaconUpdateScheduler.ScheduledTask scheduledUpdate;
    private BeaconPayload lastPayload;
    private Double latitude;
    private Double longitude;
    private boolean desiredActive;
    private boolean firstPacketStarted;
    private long generation;

    public BeaconBroadcastController(Context context) {
        this(
                new AndroidBleAdvertiser(context),
                new BeaconSession(),
                new MainThreadBeaconUpdateScheduler(),
                () -> System.currentTimeMillis() / 1_000L
        );
    }

    BeaconBroadcastController(
            BeaconAdvertiser advertiser,
            BeaconSession session,
            BeaconUpdateScheduler scheduler,
            LongSupplier epochSecondsSource
    ) {
        if (advertiser == null
                || session == null
                || scheduler == null
                || epochSecondsSource == null) {
            throw new IllegalArgumentException(
                    "All beacon controller dependencies are required."
            );
        }

        this.advertiser = advertiser;
        this.session = session;
        this.scheduler = scheduler;
        this.epochSecondsSource = epochSecondsSource;
    }

    public void start(
            Double latitude,
            Double longitude,
            Listener listener
    ) {
        if (listener == null) {
            throw new IllegalArgumentException(
                    "A beacon listener is required."
            );
        }

        if ((latitude == null) != (longitude == null)) {
            listener.onBroadcastFailure(
                    "Latitude and longitude must both be present or both be absent."
            );
            return;
        }

        final long currentGeneration;

        synchronized (stateLock) {
            if (state != State.STOPPED) {
                listener.onBroadcastFailure(
                        "The beacon is already active."
                );
                return;
            }

            this.latitude = latitude;
            this.longitude = longitude;
            this.listener = listener;
            desiredActive = true;
            firstPacketStarted = false;
            state = State.STARTING;
            generation++;
            currentGeneration = generation;
        }

        startCurrentPacket(currentGeneration);
    }

    public void stop() {
        final boolean platformActive;
        final long currentGeneration;

        synchronized (stateLock) {
            if (state == State.STOPPED) {
                return;
            }

            desiredActive = false;
            cancelScheduledUpdateLocked();
            currentGeneration = generation;

            platformActive =
                    advertiser.getState()
                            != BeaconAdvertiser.State.STOPPED;
        }

        if (platformActive) {
            advertiser.stop();
        } else {
            finishStopped(currentGeneration);
        }
    }

    public State getState() {
        synchronized (stateLock) {
            return state;
        }
    }

    public boolean isActive() {
        synchronized (stateLock) {
            return state != State.STOPPED;
        }
    }

    public BeaconPayload getLastPayload() {
        synchronized (stateLock) {
            return lastPayload;
        }
    }

    private void startCurrentPacket(long currentGeneration) {
        final BeaconPayload payload;

        try {
            long now = epochSecondsSource.getAsLong();

            synchronized (stateLock) {
                if (!isCurrentActiveGenerationLocked(
                        currentGeneration
                )) {
                    return;
                }

                payload = session.createEmergencyPayload(
                        latitude,
                        longitude,
                        now
                );

                lastPayload = payload;
                state = State.STARTING;
            }
        } catch (IllegalArgumentException exception) {
            fail(
                    currentGeneration,
                    exception.getMessage()
            );
            return;
        }

        advertiser.start(
                payload,
                new BeaconAdvertiser.Callback() {
                    @Override
                    public void onStarted() {
                        handleAdvertiserStarted(
                                currentGeneration,
                                payload
                        );
                    }

                    @Override
                    public void onStopped() {
                        handleAdvertiserStopped(
                                currentGeneration
                        );
                    }

                    @Override
                    public void onFailure(String message) {
                        fail(currentGeneration, message);
                    }
                }
        );
    }

    private void handleAdvertiserStarted(
            long currentGeneration,
            BeaconPayload payload
    ) {
        final Listener currentListener;
        final boolean firstPacket;

        synchronized (stateLock) {
            if (!isCurrentActiveGenerationLocked(
                    currentGeneration
            )) {
                advertiser.stop();
                return;
            }

            state = State.BROADCASTING;
            firstPacket = !firstPacketStarted;
            firstPacketStarted = true;
            currentListener = listener;

            cancelScheduledUpdateLocked();

            scheduledUpdate = scheduler.schedule(
                    () -> refreshPayload(currentGeneration),
                    SimulatedBeaconService
                            .BROADCAST_INTERVAL_MILLIS
            );
        }

        if (currentListener != null) {
            if (firstPacket) {
                currentListener.onBroadcastStarted(payload);
            } else {
                currentListener.onPayloadUpdated(payload);
            }
        }
    }

    private void refreshPayload(long currentGeneration) {
        synchronized (stateLock) {
            if (!isCurrentActiveGenerationLocked(
                    currentGeneration
            ) || state != State.BROADCASTING) {
                return;
            }

            scheduledUpdate = null;
            state = State.UPDATING;
        }

        /*
         * Legacy Android BLE advertising does not provide an in-place
         * service-data update. Stop and restart with the refreshed packet.
         */
        advertiser.stop();
    }

    private void handleAdvertiserStopped(
            long currentGeneration
    ) {
        final boolean restart;
        final Listener stoppedListener;

        synchronized (stateLock) {
            if (generation != currentGeneration) {
                return;
            }

            restart = desiredActive
                    && state == State.UPDATING;

            if (restart) {
                state = State.STARTING;
                stoppedListener = null;
            } else if (!desiredActive) {
                stoppedListener = listener;
                clearLocked();
            } else {
                stoppedListener = null;
            }
        }

        if (restart) {
            startCurrentPacket(currentGeneration);
        } else if (stoppedListener != null) {
            stoppedListener.onBroadcastStopped();
        }
    }

    private void finishStopped(long currentGeneration) {
        final Listener stoppedListener;

        synchronized (stateLock) {
            if (generation != currentGeneration
                    || state == State.STOPPED) {
                return;
            }

            stoppedListener = listener;
            clearLocked();
        }

        if (stoppedListener != null) {
            stoppedListener.onBroadcastStopped();
        }
    }

    private void fail(
            long failedGeneration,
            String message
    ) {
        final Listener failedListener;

        synchronized (stateLock) {
            if (generation != failedGeneration) {
                return;
            }

            failedListener = listener;
            clearLocked();
        }

        if (failedListener != null) {
            failedListener.onBroadcastFailure(
                    message == null || message.isBlank()
                            ? "The BLE beacon could not continue."
                            : message
            );
        }
    }

    private boolean isCurrentActiveGenerationLocked(
            long expectedGeneration
    ) {
        return generation == expectedGeneration
                && desiredActive;
    }

    private void cancelScheduledUpdateLocked() {
        if (scheduledUpdate != null) {
            scheduledUpdate.cancel();
            scheduledUpdate = null;
        }
    }

    private void clearLocked() {
        cancelScheduledUpdateLocked();
        desiredActive = false;
        state = State.STOPPED;
        latitude = null;
        longitude = null;
        lastPayload = null;
        firstPacketStarted = false;
        listener = null;
        session.reset();

        // Invalidate delayed callbacks from the completed session.
        generation++;
    }
}