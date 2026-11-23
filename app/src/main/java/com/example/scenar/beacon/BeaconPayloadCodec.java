package com.example.scenar.beacon;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public final class BeaconPayloadCodec {
    public static final int PAYLOAD_SIZE_BYTES = 20;

    private static final int CHECKSUM_DATA_SIZE = 18;
    private static final int FLAG_EMERGENCY = 1;
    private static final int FLAG_HAS_LOCATION = 1 << 1;
    private static final int MISSING_COORDINATE = Integer.MIN_VALUE;
    private static final double COORDINATE_SCALE = 1_000_000.0;

    private BeaconPayloadCodec() {
    }

    public static byte[] encode(BeaconPayload payload) {
        if (payload == null) {
            throw new IllegalArgumentException("Beacon payload is required.");
        }

        ByteBuffer buffer = ByteBuffer
                .allocate(PAYLOAD_SIZE_BYTES)
                .order(ByteOrder.BIG_ENDIAN);

        buffer.put((byte) payload.protocolVersion());

        int flags = 0;
        if (payload.emergency()) {
            flags |= FLAG_EMERGENCY;
        }
        if (payload.hasLocation()) {
            flags |= FLAG_HAS_LOCATION;
        }
        buffer.put((byte) flags);

        buffer.putInt((int) payload.anonymousBeaconId());

        if (payload.hasLocation()) {
            buffer.putInt(toMicrodegrees(payload.latitude()));
            buffer.putInt(toMicrodegrees(payload.longitude()));
        } else {
            buffer.putInt(MISSING_COORDINATE);
            buffer.putInt(MISSING_COORDINATE);
        }

        buffer.putInt((int) payload.generatedAtEpochSeconds());

        byte[] bytes = buffer.array();
        int checksum = crc16Ccitt(bytes, 0, CHECKSUM_DATA_SIZE);
        buffer.putShort((short) checksum);

        return bytes;
    }

    public static BeaconPayload decode(byte[] encoded) {
        if (encoded == null
                || encoded.length != PAYLOAD_SIZE_BYTES) {
            throw new IllegalArgumentException(
                    "Beacon payload must contain exactly "
                            + PAYLOAD_SIZE_BYTES
                            + " bytes."
            );
        }

        int expectedChecksum = unsignedShort(
                ByteBuffer.wrap(
                                encoded,
                                CHECKSUM_DATA_SIZE,
                                Short.BYTES
                        )
                        .order(ByteOrder.BIG_ENDIAN)
                        .getShort()
        );

        int actualChecksum = crc16Ccitt(
                encoded,
                0,
                CHECKSUM_DATA_SIZE
        );

        if (expectedChecksum != actualChecksum) {
            throw new IllegalArgumentException(
                    "Beacon payload checksum is invalid."
            );
        }

        ByteBuffer buffer = ByteBuffer
                .wrap(Arrays.copyOf(encoded, encoded.length))
                .order(ByteOrder.BIG_ENDIAN);

        int version = Byte.toUnsignedInt(buffer.get());
        int flags = Byte.toUnsignedInt(buffer.get());
        long anonymousId = Integer.toUnsignedLong(buffer.getInt());

        int encodedLatitude = buffer.getInt();
        int encodedLongitude = buffer.getInt();
        long timestamp = Integer.toUnsignedLong(buffer.getInt());

        boolean emergency = (flags & FLAG_EMERGENCY) != 0;
        boolean hasLocation = (flags & FLAG_HAS_LOCATION) != 0;

        Double latitude = null;
        Double longitude = null;

        if (hasLocation) {
            if (encodedLatitude == MISSING_COORDINATE
                    || encodedLongitude == MISSING_COORDINATE) {
                throw new IllegalArgumentException(
                        "Beacon location flag does not match its coordinates."
                );
            }

            latitude = encodedLatitude / COORDINATE_SCALE;
            longitude = encodedLongitude / COORDINATE_SCALE;
        } else if (encodedLatitude != MISSING_COORDINATE
                || encodedLongitude != MISSING_COORDINATE) {
            throw new IllegalArgumentException(
                    "Beacon contains coordinates without the location flag."
            );
        }

        return new BeaconPayload(
                version,
                anonymousId,
                emergency,
                latitude,
                longitude,
                timestamp
        );
    }

    private static int toMicrodegrees(double coordinate) {
        return (int) Math.round(coordinate * COORDINATE_SCALE);
    }

    private static int unsignedShort(short value) {
        return Short.toUnsignedInt(value);
    }

    private static int crc16Ccitt(
            byte[] data,
            int offset,
            int length
    ) {
        int crc = 0xffff;

        for (int index = offset; index < offset + length; index++) {
            crc ^= Byte.toUnsignedInt(data[index]) << 8;

            for (int bit = 0; bit < 8; bit++) {
                if ((crc & 0x8000) != 0) {
                    crc = (crc << 1) ^ 0x1021;
                } else {
                    crc <<= 1;
                }

                crc &= 0xffff;
            }
        }

        return crc;
    }
}