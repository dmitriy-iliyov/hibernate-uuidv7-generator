package io.github.dmitriyiliyov.hibernate.uuidv7gen;

import org.hibernate.annotations.IdGeneratorType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a {@link java.util.UUID} identifier field as populated with a time-ordered UUIDv7.
 * <p>
 * The value is assigned by the application before the {@code INSERT} statement is issued, so the
 * identifier is available as soon as {@code persist} returns and no database round trip is spent
 * on it. Existing identifiers are never overwritten on update.
 *
 * @see UUIDv7Generator
 * @see DefaultUuidGenerator
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@IdGeneratorType(UUIDv7Generator.class)
public @interface UUIDv7 {

    /**
     * The {@link UuidGenerator} implementation to instantiate for this identifier.
     * <p>
     * The class must be concrete and declare a no-argument constructor; it is instantiated once
     * during {@code SessionFactory} bootstrap and shared across sessions.
     *
     * @return the generator implementation, {@link DefaultUuidGenerator} by default
     */
    Class<? extends UuidGenerator> generator() default DefaultUuidGenerator.class;
}
