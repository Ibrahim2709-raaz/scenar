package com.example.scenar.beacon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BeaconPacketReceiverTest {
    @Test
    public void acceptsValidFreshPacket() {
        BeaconPacketReceiver receiver =
                new BeaconPacketReceiver(
                        new BeaconReplayGuard(30, 5)
                );

        BeaconPayload original =
                BeaconPayload.emergencyWithLocation(
                        100,
                        31.520370,
                        74.358749,
                        1_000
                );

        BeaconPacketReceiver.Result result =
                receiver.receive(
                        BeaconPayloadCodec.encode(original),
                        1_002
                );

        assertTrue(result.isAccepted());
        assertEquals(
                BeaconPacketReceiver.Status.ACCEPTED,
                result.status()
        );
        assertNotNull(result.payload());
        assertEquals(
                original.anonymousBeaconId(),
                result.payload().anonymousBeaconId()
        );
    }

    @Test
    public void rejectsCorruptedPacket() {
        BeaconPacketReceiver receiver =
                new BeaconPacketReceiver();

        byte[] encoded = BeaconPayloadCodec.encode(
                BeaconPayload.emergencyWithoutLocation(
                        100,
                        1_000
                )
        );

        encoded[4] ^= 0x01;

        BeaconPacketReceiver.Result result =
                receiver.receive(encoded, 1_001);

        assertFalse(result.isAccepted());
        assertEquals(
                BeaconPacketReceiver.Status.INVALID_PACKET,
                result.status()
        );
        assertNull(result.payload());
    }

    @Test
    public void rejectsRepeatedPacket() {
        BeaconPacketReceiver receiver =
                new BeaconPacketReceiver(
                        new BeaconReplayGuard(30, 5)
                );

        byte[] encoded = BeaconPayloadCodec.encode(
                BeaconPayload.emergencyWithoutLocation(
                        100,
                        1_000
                )
        );

        BeaconPacketReceiver.Result first =
                receiver.receive(encoded, 1_001);

        BeaconPacketReceiver.Result repeated =
                receiver.receive(encoded, 1_002);

        assertTrue(first.isAccepted());
        assertEquals(
                BeaconPacketReceiver.Status.REPLAYED,
                repeated.status()
        );
    }

    @Test
    public void rejectsStalePacket() {
        BeaconPacketReceiver receiver =
                new BeaconPacketReceiver(
                        new BeaconReplayGuard(30, 5)
                );

        byte[] encoded = BeaconPayloadCodec.encode(
                BeaconPayload.emergencyWithoutLocation(
                        100,
                        900
                )
        );

        BeaconPacketReceiver.Result result =
                receiver.receive(encoded, 1_000);

        assertEquals(
                BeaconPacketReceiver.Status.STALE,
                result.status()
        );
    }

    @Test
    public void rejectsPacketTooFarInFuture() {
        BeaconPacketReceiver receiver =
                new BeaconPacketReceiver(
                        new BeaconReplayGuard(30, 5)
                );

        byte[] encoded = BeaconPayloadCodec.encode(
                BeaconPayload.emergencyWithoutLocation(
                        100,
                        1_100
                )
        );

        BeaconPacketReceiver.Result result =
                receiver.receive(encoded, 1_000);

        assertEquals(
                BeaconPacketReceiver.Status.FROM_FUTURE,
                result.status()
        );
    }

    @Test
    public void acceptsNewTimestampFromSameBeacon() {
        BeaconPacketReceiver receiver =
                new BeaconPacketReceiver(
                        new BeaconReplayGuard(30, 5)
                );

        byte[] first = BeaconPayloadCodec.encode(
                BeaconPayload.emergencyWithoutLocation(
                        100,
                        1_000
                )
        );

        byte[] second = BeaconPayloadCodec.encode(
                BeaconPayload.emergencyWithoutLocation(
                        100,
                        1_005
                )
        );

        assertTrue(receiver.receive(first, 1_001).isAccepted());
        assertTrue(receiver.receive(second, 1_006).isAccepted());
        assertEquals(1, receiver.getTrackedBeaconCount());
    }

    @Test
    public void clearHistoryAllowsPacketAgain() {
        BeaconPacketReceiver receiver =
                new BeaconPacketReceiver(
                        new BeaconReplayGuard(30, 5)
                );

        byte[] encoded = BeaconPayloadCodec.encode(
                BeaconPayload.emergencyWithoutLocation(
                        100,
                        1_000
                )
        );

        assertTrue(receiver.receive(encoded, 1_001).isAccepted());

        receiver.clearHistory();

        assertEquals(0, receiver.getTrackedBeaconCount());
        assertTrue(receiver.receive(encoded, 1_002).isAccepted());
    }
}