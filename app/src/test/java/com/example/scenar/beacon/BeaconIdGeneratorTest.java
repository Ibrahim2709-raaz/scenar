package com.example.scenar.beacon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BeaconIdGeneratorTest {
    @Test
    public void convertsSignedRandomValueToUnsignedId() {
        BeaconIdGenerator generator =
                new BeaconIdGenerator(() -> -1);

        assertEquals(
                BeaconPayload.MAX_UNSIGNED_INT,
                generator.nextAnonymousId()
        );
    }

    @Test
    public void neverReturnsReservedZeroId() {
        BeaconIdGenerator generator =
                new BeaconIdGenerator(() -> 0);

        assertEquals(1L, generator.nextAnonymousId());
    }

    @Test
    public void defaultGeneratorReturnsValidId() {
        long id = new BeaconIdGenerator().nextAnonymousId();

        assertTrue(id >= 1);
        assertTrue(id <= BeaconPayload.MAX_UNSIGNED_INT);
    }
}