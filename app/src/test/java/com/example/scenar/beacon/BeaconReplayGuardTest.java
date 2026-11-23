package com.example.scenar.beacon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BeaconReplayGuardTest {
    @Test
    public void acceptsFreshPacketThenRejectsReplay() {
        BeaconReplayGuard guard =
                new BeaconReplayGuard(30, 5);

        BeaconPayload payload =
                BeaconPayload.emergencyWithoutLocation(
                        100,
                        1_000
                );

        BeaconReplayGuard.Result first =
                guard.evaluate(payload, 1_002);

        BeaconReplayGuard.Result replay =
                guard.evaluate(payload, 1_003);

        assertTrue(first.isAccepted());
        assertEquals(
                BeaconReplayGuard.Decision.ACCEPTED,
                first.decision()
        );

        assertFalse(replay.isAccepted());
        assertEquals(
                BeaconReplayGuard.Decision.REPLAYED,
                replay.decision()
        );
    }

    @Test
    public void acceptsNewerPacketFromSameBeacon() {
        BeaconReplayGuard guard =
                new BeaconReplayGuard(30, 5);

        BeaconPayload first =
                BeaconPayload.emergencyWithoutLocation(
                        100,
                        1_000
                );

        BeaconPayload second =
                BeaconPayload.emergencyWithoutLocation(
                        100,
                        1_005
                );

        assertTrue(guard.evaluate(first, 1_001).isAccepted());
        assertTrue(guard.evaluate(second, 1_006).isAccepted());
    }

    @Test
    public void rejectsStalePacket() {
        BeaconReplayGuard guard =
                new BeaconReplayGuard(30, 5);

        BeaconPayload stale =
                BeaconPayload.emergencyWithoutLocation(
                        100,
                        900
                );

        BeaconReplayGuard.Result result =
                guard.evaluate(stale, 1_000);

        assertEquals(
                BeaconReplayGuard.Decision.STALE,
                result.decision()
        );
    }

    @Test
    public void rejectsPacketTooFarInFuture() {
        BeaconReplayGuard guard =
                new BeaconReplayGuard(30, 5);

        BeaconPayload future =
                BeaconPayload.emergencyWithoutLocation(
                        100,
                        1_100
                );

        BeaconReplayGuard.Result result =
                guard.evaluate(future, 1_000);

        assertEquals(
                BeaconReplayGuard.Decision.FROM_FUTURE,
                result.decision()
        );
    }

    @Test
    public void removesExpiredBeaconHistory() {
        BeaconReplayGuard guard =
                new BeaconReplayGuard(30, 5);

        BeaconPayload first =
                BeaconPayload.emergencyWithoutLocation(
                        100,
                        1_000
                );

        BeaconPayload second =
                BeaconPayload.emergencyWithoutLocation(
                        200,
                        1_100
                );

        guard.evaluate(first, 1_000);
        guard.evaluate(second, 1_100);

        assertEquals(1, guard.getTrackedBeaconCount());
    }

    @Test
    public void clearRemovesReplayHistory() {
        BeaconReplayGuard guard =
                new BeaconReplayGuard(30, 5);

        BeaconPayload payload =
                BeaconPayload.emergencyWithoutLocation(
                        100,
                        1_000
                );

        guard.evaluate(payload, 1_000);
        guard.clear();

        assertEquals(0, guard.getTrackedBeaconCount());
        assertTrue(guard.evaluate(payload, 1_001).isAccepted());
    }
}