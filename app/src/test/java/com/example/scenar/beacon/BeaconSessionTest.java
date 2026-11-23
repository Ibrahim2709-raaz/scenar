package com.example.scenar.beacon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

public class BeaconSessionTest {
    @Test
    public void keepsIdBeforeRotationAndChangesItAfterRotation() {
        AtomicInteger nextId = new AtomicInteger(100);

        BeaconSession session = new BeaconSession(
                new BeaconIdGenerator(nextId::getAndIncrement),
                300
        );

        BeaconPayload first =
                session.createEmergencyPayloadWithoutLocation(1_000);

        BeaconPayload second =
                session.createEmergencyPayloadWithoutLocation(1_299);

        BeaconPayload rotated =
                session.createEmergencyPayloadWithoutLocation(1_300);

        assertEquals(
                first.anonymousBeaconId(),
                second.anonymousBeaconId()
        );

        assertNotEquals(
                first.anonymousBeaconId(),
                rotated.anonymousBeaconId()
        );

        assertEquals(101L, rotated.anonymousBeaconId());
    }

    @Test
    public void rotatesIdWhenClockMovesBackwards() {
        AtomicInteger nextId = new AtomicInteger(200);

        BeaconSession session = new BeaconSession(
                new BeaconIdGenerator(nextId::getAndIncrement),
                300
        );

        BeaconPayload first =
                session.createEmergencyPayloadWithoutLocation(2_000);

        BeaconPayload afterClockChange =
                session.createEmergencyPayloadWithoutLocation(1_900);

        assertNotEquals(
                first.anonymousBeaconId(),
                afterClockChange.anonymousBeaconId()
        );
    }

    @Test
    public void resetClearsActiveSession() {
        BeaconSession session = new BeaconSession(
                new BeaconIdGenerator(() -> 500),
                300
        );

        session.createEmergencyPayloadWithoutLocation(1_000);

        assertTrue(session.hasActiveSession());

        session.reset();

        assertFalse(session.hasActiveSession());

        assertThrows(
                IllegalStateException.class,
                session::getActiveAnonymousId
        );
    }

    @Test
    public void payloadCanContainValidatedLocation() {
        BeaconSession session = new BeaconSession(
                new BeaconIdGenerator(() -> 700),
                300
        );

        BeaconPayload payload =
                session.createEmergencyPayload(
                        31.520370,
                        74.358749,
                        1_000
                );

        assertTrue(payload.hasLocation());
        assertEquals(
                31.520370,
                payload.latitude(),
                0.000001
        );
        assertEquals(
                74.358749,
                payload.longitude(),
                0.000001
        );
    }
}