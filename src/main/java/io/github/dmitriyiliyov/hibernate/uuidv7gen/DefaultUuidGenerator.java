package io.github.dmitriyiliyov.hibernate.uuidv7gen;

import com.github.f4b6a3.uuid.UuidCreator;

import java.util.UUID;

/**
 * Default {@link UuidGenerator}, producing RFC 9562 version 7 identifiers.
 * <p>
 * A UUIDv7 embeds a 48-bit Unix epoch timestamp in milliseconds in its most significant bits and
 * fills the remainder with randomness. Consecutive values are therefore ordered by creation time
 * down to the millisecond, which keeps B-tree index inserts local instead of scattering them the
 * way a random UUIDv4 primary key does.
 * <p>
 * Ordering holds <em>between</em> milliseconds only: values created within the same millisecond
 * carry the same timestamp and are ordered arbitrarily relative to one another. Uniqueness is
 * unaffected.
 * <p>
 * This implementation is stateless and thread-safe.
 *
 * @see <a href="https://www.rfc-editor.org/rfc/rfc9562#name-uuid-version-7">RFC 9562, UUID Version 7</a>
 */
public class DefaultUuidGenerator implements UuidGenerator {

    public DefaultUuidGenerator() { }

    /**
     * {@inheritDoc}
     * <p>
     * Delegates to {@link UuidCreator#getTimeOrderedEpoch()}, seeding the random bits from a
     * secure random source.
     */
    @Override
    public UUID generate() {
        return UuidCreator.getTimeOrderedEpoch();
    }
}
