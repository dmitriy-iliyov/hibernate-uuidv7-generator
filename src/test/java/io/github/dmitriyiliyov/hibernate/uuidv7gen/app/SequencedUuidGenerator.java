package io.github.dmitriyiliyov.hibernate.uuidv7gen.app;

import com.github.f4b6a3.uuid.UuidCreator;
import io.github.dmitriyiliyov.hibernate.uuidv7gen.UuidGenerator;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Custom {@link UuidGenerator} pinned to a fixed instant, so a test can assert on exact identifier
 * values. Also counts invocations to prove Hibernate really routes through it.
 */
public class SequencedUuidGenerator implements UuidGenerator {

    public static final Instant FIXED_INSTANT = Instant.parse("2024-01-01T00:00:00Z");

    private static final AtomicLong INVOCATIONS = new AtomicLong();

    @Override
    public UUID generate() {
        INVOCATIONS.incrementAndGet();
        return UuidCreator.getTimeOrderedEpoch(FIXED_INSTANT);
    }

    public static long invocations() {
        return INVOCATIONS.get();
    }

    public static void resetInvocations() {
        INVOCATIONS.set(0);
    }
}
