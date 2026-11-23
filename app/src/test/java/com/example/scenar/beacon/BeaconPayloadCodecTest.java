package com.example.scenar.beacon;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BeaconPayloadCodecTest {
    @Test
    public void roundTripPreservesEmergencyPayloadWithLocation() {
        BeaconPayload original =
                BeaconPayload.emergencyWithLocation(
                        4_294_967_000L,
                        31.520370,
                        74.358749,
                        1_795_000_000L
                );

        byte[] encoded = BeaconPayloadCodec.encode(original);
        BeaconPayload decoded = BeaconPayloadCodec.decode(encoded);

        assertEquals(
                BeaconPayloadCodec.PAYLOAD_SIZE_BYTES,
                encoded.length
        );
        assertEquals(original.protocolVersion(), decoded.protocolVersion());
        assertEquals(original.anonymousBeaconId(), decoded.anonymousBeaconId());
        assertEquals(original.emergency(), decoded.emergency());
        assertEquals(
                original.latitude(),
                decoded.latitude(),
                0.000001
        );
        assertEquals(
                original.longitude(),
                decoded.longitude(),
                0.000001
        );
        assertEquals(
                original.generatedAtEpochSeconds(),
                decoded.generatedAtEpochSeconds()
        );
    }

    @Test
    public void roundTripPreservesPayloadWithoutLocation() {
        BeaconPayload original =
                BeaconPayload.emergencyWithoutLocation(
                        42,
                        1_795_000_000L
                );

        BeaconPayload decoded = BeaconPayloadCodec.decode(
                BeaconPayloadCodec.encode(original)
        );

        assertTrue(decoded.emergency());
        assertFalse(decoded.hasLocation());
        assertNull(decoded.latitude());
        assertNull(decoded.longitude());
    }

    @Test
    public void encodingIsDeterministic() {
        BeaconPayload payload =
                BeaconPayload.emergencyWithLocation(
                        100,
                        31.5,
                        74.3,
                        1_795_000_000L
                );

        assertArrayEquals(
                BeaconPayloadCodec.encode(payload),
                BeaconPayloadCodec.encode(payload)
        );
    }

    @Test
    public void corruptedPayloadIsRejected() {
        byte[] encoded = BeaconPayloadCodec.encode(
                BeaconPayload.emergencyWithoutLocation(
                        77,
                        1_795_000_000L
                )
        );

        encoded[5] ^= 0x01;

        assertThrows(
                IllegalArgumentException.class,
                () -> BeaconPayloadCodec.decode(encoded)
        );
    }

    @Test
    public void partialCoordinatesAreRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new BeaconPayload(
                        BeaconPayload.CURRENT_PROTOCOL_VERSION,
                        123,
                        true,
                        31.5,
                        null,
                        1_795_000_000L
                )
        );
    }

    @Test
    public void invalidCoordinateIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> BeaconPayload.emergencyWithLocation(
                        123,
                        95,
                        74,
                        1_795_000_000L
                )
        );
    }
}