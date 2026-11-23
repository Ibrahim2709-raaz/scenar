package com.example.scenar.beacon;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class BeaconReplayGuard {
    public static final long DEFAULT_MAX_AGE_SECONDS = 30L;
    public static final long DEFAULT_FUTURE_TOLERANCE_SECONDS = 5L;

    public enum Decision {
        ACCEPTED,
        STALE,
        FROM_FUTURE,
        REPLAYED
    }

    public record Result(
            Decision decision,
            String message
    ) {
        public boolean isAccepted() {
            return decision == Decision.ACCEPTED;
        }
    }

    private final long maxAgeSeconds;
    private final long futureToleranceSeconds;
    private final Map<Long, Long> latestTimestampByBeacon =
            new HashMap<>();

    public BeaconReplayGuard() {
        this(
                DEFAULT_MAX_AGE_SECONDS,
                DEFAULT_FUTURE_TOLERANCE_SECONDS
        );
    }

    BeaconReplayGuard(
            long maxAgeSeconds,
            long futureToleranceSeconds
    ) {
        if (maxAgeSeconds < 1) {
            throw new IllegalArgumentException(
                    "Maximum packet age must be positive."
            );
        }

        if (futureToleranceSeconds < 0) {
            throw new IllegalArgumentException(
                    "Future tolerance cannot be negative."
            );
        }

        this.maxAgeSeconds = maxAgeSeconds;
        this.futureToleranceSeconds = futureToleranceSeconds;
    }

    public synchronized Result evaluate(
            BeaconPayload payload,
            long receivedAtEpochSeconds
    ) {
        if (payload == null) {
            throw new IllegalArgumentException(
                    "A beacon payload is required."
            );
        }

        if (receivedAtEpochSeconds < 0
                || receivedAtEpochSeconds
                > BeaconPayload.MAX_UNSIGNED_INT) {
            throw new IllegalArgumentException(
                    "Received time must fit in an unsigned 32-bit integer."
            );
        }

        removeExpiredEntries(receivedAtEpochSeconds);

        long packetTime = payload.generatedAtEpochSeconds();

        if (packetTime < receivedAtEpochSeconds - maxAgeSeconds) {
            return new Result(
                    Decision.STALE,
                    "The beacon packet is too old."
            );
        }

        if (packetTime
                > receivedAtEpochSeconds + futureToleranceSeconds) {
            return new Result(
                    Decision.FROM_FUTURE,
                    "The beacon packet timestamp is too far in the future."
            );
        }

        Long latestTimestamp = latestTimestampByBeacon.get(
                payload.anonymousBeaconId()
        );

        if (latestTimestamp != null
                && packetTime <= latestTimestamp) {
            return new Result(
                    Decision.REPLAYED,
                    "The beacon packet has already been processed."
            );
        }

        latestTimestampByBeacon.put(
                payload.anonymousBeaconId(),
                packetTime
        );

        return new Result(
                Decision.ACCEPTED,
                "Beacon packet accepted for simulation."
        );
    }

    public synchronized int getTrackedBeaconCount() {
        return latestTimestampByBeacon.size();
    }

    public synchronized void clear() {
        latestTimestampByBeacon.clear();
    }

    private void removeExpiredEntries(
            long receivedAtEpochSeconds
    ) {
        long oldestAllowedTimestamp =
                receivedAtEpochSeconds - maxAgeSeconds;

        Iterator<Map.Entry<Long, Long>> iterator =
                latestTimestampByBeacon.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<Long, Long> entry = iterator.next();

            if (entry.getValue() < oldestAllowedTimestamp) {
                iterator.remove();
            }
        }
    }
}