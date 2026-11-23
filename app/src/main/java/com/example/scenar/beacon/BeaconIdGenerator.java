package com.example.scenar.beacon;

import java.security.SecureRandom;
import java.util.function.IntSupplier;

public final class BeaconIdGenerator {
    private final IntSupplier randomSource;

    public BeaconIdGenerator() {
        SecureRandom secureRandom = new SecureRandom();
        randomSource = secureRandom::nextInt;
    }

    BeaconIdGenerator(IntSupplier randomSource) {
        if (randomSource == null) {
            throw new IllegalArgumentException(
                    "A random source is required."
            );
        }

        this.randomSource = randomSource;
    }

    public long nextAnonymousId() {
        long candidate = Integer.toUnsignedLong(
                randomSource.getAsInt()
        );

        // Zero is reserved to represent an uninitialized session.
        return candidate == 0 ? 1 : candidate;
    }
}