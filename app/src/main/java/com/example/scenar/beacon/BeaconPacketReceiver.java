package com.example.scenar.beacon;

public final class BeaconPacketReceiver {
    public enum Status {
        ACCEPTED,
        INVALID_PACKET,
        STALE,
        FROM_FUTURE,
        REPLAYED
    }

    public record Result(
            Status status,
            BeaconPayload payload,
            String message
    ) {
        public Result {
            if (status == null) {
                throw new IllegalArgumentException(
                        "A receiver result status is required."
                );
            }

            if (message == null || message.isBlank()) {
                throw new IllegalArgumentException(
                        "A receiver result message is required."
                );
            }

            if (status == Status.ACCEPTED && payload == null) {
                throw new IllegalArgumentException(
                        "An accepted result requires a beacon payload."
                );
            }
        }

        public boolean isAccepted() {
            return status == Status.ACCEPTED;
        }
    }

    private final BeaconReplayGuard replayGuard;

    public BeaconPacketReceiver() {
        this(new BeaconReplayGuard());
    }

    BeaconPacketReceiver(
            BeaconReplayGuard replayGuard
    ) {
        if (replayGuard == null) {
            throw new IllegalArgumentException(
                    "A beacon replay guard is required."
            );
        }

        this.replayGuard = replayGuard;
    }

    public Result receive(
            byte[] encodedPacket,
            long receivedAtEpochSeconds
    ) {
        final BeaconPayload payload;

        try {
            payload = BeaconPayloadCodec.decode(encodedPacket);
        } catch (IllegalArgumentException exception) {
            return new Result(
                    Status.INVALID_PACKET,
                    null,
                    exception.getMessage() == null
                            ? "The BLE beacon packet is invalid."
                            : exception.getMessage()
            );
        }

        BeaconReplayGuard.Result replayResult =
                replayGuard.evaluate(
                        payload,
                        receivedAtEpochSeconds
                );

        return switch (replayResult.decision()) {
            case ACCEPTED -> new Result(
                    Status.ACCEPTED,
                    payload,
                    "BLE beacon packet accepted."
            );

            case STALE -> new Result(
                    Status.STALE,
                    payload,
                    replayResult.message()
            );

            case FROM_FUTURE -> new Result(
                    Status.FROM_FUTURE,
                    payload,
                    replayResult.message()
            );

            case REPLAYED -> new Result(
                    Status.REPLAYED,
                    payload,
                    replayResult.message()
            );
        };
    }

    public void clearHistory() {
        replayGuard.clear();
    }

    public int getTrackedBeaconCount() {
        return replayGuard.getTrackedBeaconCount();
    }
}