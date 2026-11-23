package com.example.scenar.beacon;

public record BeaconPayload(
        int protocolVersion,
        long anonymousBeaconId,
        boolean emergency,
        Double latitude,
        Double longitude,
        long generatedAtEpochSeconds
) {
    public static final int CURRENT_PROTOCOL_VERSION = 1;
    public static final long MAX_UNSIGNED_INT = 0xffff_ffffL;

    public BeaconPayload {
        if (protocolVersion != CURRENT_PROTOCOL_VERSION) {
            throw new IllegalArgumentException(
                    "Unsupported beacon protocol version."
            );
        }

        if (anonymousBeaconId < 1
                || anonymousBeaconId > MAX_UNSIGNED_INT) {
            throw new IllegalArgumentException(
                    "Anonymous beacon ID must fit in an unsigned 32-bit integer."
            );
        }

        if ((latitude == null) != (longitude == null)) {
            throw new IllegalArgumentException(
                    "Latitude and longitude must either both be present or both be absent."
            );
        }

        if (latitude != null) {
            if (!Double.isFinite(latitude)
                    || latitude < -90
                    || latitude > 90) {
                throw new IllegalArgumentException(
                        "Latitude must be between -90 and 90."
                );
            }

            if (!Double.isFinite(longitude)
                    || longitude < -180
                    || longitude > 180) {
                throw new IllegalArgumentException(
                        "Longitude must be between -180 and 180."
                );
            }
        }

        if (generatedAtEpochSeconds < 0
                || generatedAtEpochSeconds > MAX_UNSIGNED_INT) {
            throw new IllegalArgumentException(
                    "Beacon timestamp must fit in an unsigned 32-bit integer."
            );
        }
    }

    public boolean hasLocation() {
        return latitude != null;
    }

    public static BeaconPayload emergencyWithLocation(
            long anonymousBeaconId,
            double latitude,
            double longitude,
            long generatedAtEpochSeconds
    ) {
        return new BeaconPayload(
                CURRENT_PROTOCOL_VERSION,
                anonymousBeaconId,
                true,
                latitude,
                longitude,
                generatedAtEpochSeconds
        );
    }

    public static BeaconPayload emergencyWithoutLocation(
            long anonymousBeaconId,
            long generatedAtEpochSeconds
    ) {
        return new BeaconPayload(
                CURRENT_PROTOCOL_VERSION,
                anonymousBeaconId,
                true,
                null,
                null,
                generatedAtEpochSeconds
        );
    }
}