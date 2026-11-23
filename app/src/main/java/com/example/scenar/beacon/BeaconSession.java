package com.example.scenar.beacon;

public final class BeaconSession {
    public static final long DEFAULT_ID_ROTATION_SECONDS = 300L;

    private final BeaconIdGenerator idGenerator;
    private final long rotationIntervalSeconds;

    private long activeAnonymousId;
    private long nextRotationEpochSeconds;
    private long lastPacketEpochSeconds = -1;

    public BeaconSession() {
        this(
                new BeaconIdGenerator(),
                DEFAULT_ID_ROTATION_SECONDS
        );
    }

    BeaconSession(
            BeaconIdGenerator idGenerator,
            long rotationIntervalSeconds
    ) {
        if (idGenerator == null) {
            throw new IllegalArgumentException(
                    "A beacon ID generator is required."
            );
        }

        if (rotationIntervalSeconds < 1) {
            throw new IllegalArgumentException(
                    "The rotation interval must be positive."
            );
        }

        this.idGenerator = idGenerator;
        this.rotationIntervalSeconds = rotationIntervalSeconds;
    }

    public synchronized BeaconPayload createEmergencyPayload(
            Double latitude,
            Double longitude,
            long currentEpochSeconds
    ) {
        validateTime(currentEpochSeconds);

        if (shouldRotate(currentEpochSeconds)) {
            rotateId(currentEpochSeconds);
        }

        lastPacketEpochSeconds = currentEpochSeconds;

        return new BeaconPayload(
                BeaconPayload.CURRENT_PROTOCOL_VERSION,
                activeAnonymousId,
                true,
                latitude,
                longitude,
                currentEpochSeconds
        );
    }

    public synchronized BeaconPayload createEmergencyPayloadWithoutLocation(
            long currentEpochSeconds
    ) {
        return createEmergencyPayload(
                null,
                null,
                currentEpochSeconds
        );
    }

    public synchronized boolean hasActiveSession() {
        return activeAnonymousId != 0;
    }

    public synchronized long getActiveAnonymousId() {
        if (!hasActiveSession()) {
            throw new IllegalStateException(
                    "The beacon session has not started."
            );
        }

        return activeAnonymousId;
    }

    public synchronized long getNextRotationEpochSeconds() {
        if (!hasActiveSession()) {
            throw new IllegalStateException(
                    "The beacon session has not started."
            );
        }

        return nextRotationEpochSeconds;
    }

    public synchronized void reset() {
        activeAnonymousId = 0;
        nextRotationEpochSeconds = 0;
        lastPacketEpochSeconds = -1;
    }

    private boolean shouldRotate(long currentEpochSeconds) {
        if (!hasActiveSession()) {
            return true;
        }

        if (currentEpochSeconds >= nextRotationEpochSeconds) {
            return true;
        }

        // A backwards clock change starts a new anonymous session.
        return lastPacketEpochSeconds >= 0
                && currentEpochSeconds < lastPacketEpochSeconds;
    }

    private void rotateId(long currentEpochSeconds) {
        long previousId = activeAnonymousId;
        long nextId = idGenerator.nextAnonymousId();

        // Avoid retaining the same identifier across a rotation.
        if (nextId == previousId) {
            nextId = previousId == BeaconPayload.MAX_UNSIGNED_INT
                    ? 1
                    : previousId + 1;
        }

        activeAnonymousId = nextId;
        nextRotationEpochSeconds =
                currentEpochSeconds + rotationIntervalSeconds;
    }

    private static void validateTime(long epochSeconds) {
        if (epochSeconds < 0
                || epochSeconds > BeaconPayload.MAX_UNSIGNED_INT) {
            throw new IllegalArgumentException(
                    "Session time must fit in an unsigned 32-bit integer."
            );
        }
    }
}