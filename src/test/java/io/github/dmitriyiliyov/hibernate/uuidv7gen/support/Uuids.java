package io.github.dmitriyiliyov.hibernate.uuidv7gen.support;

import java.util.UUID;

/**
 * Helpers for inspecting the RFC 9562 bit layout of a UUIDv7.
 */
public final class Uuids {

    /** IETF variant, as reported by {@link UUID#variant()}. */
    public static final int IETF_VARIANT = 2;

    /** RFC 9562 version 7, as reported by {@link UUID#version()}. */
    public static final int UUID_VERSION_7 = 7;

    private Uuids() {
    }

    /**
     * Extracts the 48-bit Unix epoch timestamp in milliseconds stored in the high bits of a v7 UUID.
     */
    public static long timestampMillis(UUID uuid) {
        return uuid.getMostSignificantBits() >>> 16;
    }

    public static void assertIsUuidV7(UUID uuid) {
        if (uuid == null) {
            throw new AssertionError("expected a UUIDv7 but was null");
        }
        if (uuid.version() != UUID_VERSION_7 || uuid.variant() != IETF_VARIANT) {
            throw new AssertionError("expected a UUIDv7 (version 7, IETF variant) but was " + uuid
                    + " (version=" + uuid.version() + ", variant=" + uuid.variant() + ")");
        }
    }
}
