package io.github.dmitriyiliyov.hibernate.uuidv7gen;

import java.util.UUID;

/**
 * Supplies the {@link UUID} values assigned to identifiers annotated with {@link UUIDv7}.
 * <p>
 * Implementations are selected per field through {@link UUIDv7#generator()} and are
 * instantiated reflectively by {@link UUIDv7Generator} at bootstrap. An implementation
 * therefore must be a concrete class exposing a no-argument constructor.
 * <p>
 * A single instance is created per annotated identifier and reused for the lifetime of the
 * {@code SessionFactory}, so implementations must be thread-safe.
 *
 * @see DefaultUuidGenerator
 * @see UUIDv7
 */
@FunctionalInterface
public interface UuidGenerator {

    /**
     * Returns the value to assign to the identifier of an entity about to be inserted.
     *
     * @return a new identifier, never {@code null}
     */
    UUID generate();
}
