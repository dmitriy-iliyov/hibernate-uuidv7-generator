package io.github.dmitriyiliyov.hibernate.uuidv7gen;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

import static io.github.dmitriyiliyov.hibernate.uuidv7gen.support.Uuids.*;
import static org.assertj.core.api.Assertions.assertThat;

class DefaultUuidGeneratorUnitTest {

    private static final int THREADS = 8;
    private static final int PER_THREAD = 2_000;

    private final DefaultUuidGenerator tested = new DefaultUuidGenerator();

    @Test
    @DisplayName("UT generate() when called should return a version 7 IETF variant UUID")
    void generate_whenCalled_shouldReturnVersion7IetfVariantUuid() {
        // when
        UUID result = tested.generate();

        // then
        assertIsUuidV7(result);
        assertThat(result.version()).isEqualTo(UUID_VERSION_7);
        assertThat(result.variant()).isEqualTo(IETF_VARIANT);
    }

    @Test
    @DisplayName("UT generate() when called should never return null")
    void generate_whenCalled_shouldNeverReturnNull() {
        // when
        UUID result = tested.generate();

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("UT generate() when called should embed the current wall clock in the high 48 bits")
    void generate_whenCalled_shouldEmbedCurrentWallClockInHigh48Bits() {
        // given
        long before = Instant.now().toEpochMilli();

        // when
        UUID result = tested.generate();

        // then
        long after = Instant.now().toEpochMilli();
        assertThat(timestampMillis(result)).isBetween(before, after);
    }

    @Test
    @DisplayName("UT generate() when called many times should return distinct values")
    void generate_whenCalledManyTimes_shouldReturnDistinctValues() {
        // given
        int count = 10_000;

        // when
        Set<UUID> result = new HashSet<>();
        for (int i = 0; i < count; i++) {
            result.add(tested.generate());
        }

        // then
        assertThat(result).hasSize(count);
    }

    @Test
    @DisplayName("UT generate() when called many times should never move the embedded timestamp backwards")
    void generate_whenCalledManyTimes_shouldNeverMoveEmbeddedTimestampBackwards() {
        // when
        List<Long> result = new ArrayList<>();
        for (int i = 0; i < 1_000; i++) {
            result.add(timestampMillis(tested.generate()));
        }

        // then
        assertThat(result).isSorted();
    }

    @Test
    @DisplayName("UT generate() when calls fall into distinct milliseconds should return values sorting in creation order")
    void generate_whenCallsFallIntoDistinctMilliseconds_shouldReturnValuesSortingInCreationOrder() throws InterruptedException {
        // when
        List<UUID> result = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            result.add(tested.generate());
            Thread.sleep(2);
        }

        // then
        assertThat(result).isSortedAccordingTo(unsignedOrder());
    }

    @Test
    @DisplayName("UT generate() when called from several threads should return distinct values")
    void generate_whenCalledFromSeveralThreads_shouldReturnDistinctValues() throws InterruptedException {
        // given
        Set<UUID> result = ConcurrentHashMap.newKeySet();
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(THREADS);

        // when
        try (ExecutorService pool = Executors.newFixedThreadPool(THREADS)) {
            for (int thread = 0; thread < THREADS; thread++) {
                pool.submit(() -> {
                    try {
                        start.await();
                        for (int i = 0; i < PER_THREAD; i++) {
                            result.add(tested.generate());
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        done.countDown();
                    }
                });
            }
            start.countDown();
            assertThat(done.await(30, TimeUnit.SECONDS)).isTrue();
        }

        // then
        assertThat(result).hasSize(THREADS * PER_THREAD);
        assertThat(result).allSatisfy(uuid -> assertIsUuidV7(uuid));
    }

    @Test
    @DisplayName("UT generate() when the generator is written as a lambda should return the lambda value")
    void generate_whenGeneratorIsWrittenAsLambda_shouldReturnLambdaValue() {
        // given
        UUID expected = UUID.fromString("0189d6a0-0000-7000-8000-000000000001");
        UuidGenerator testedLambda = () -> expected;

        // when
        UUID result = testedLambda.generate();

        // then
        assertThat(result).isEqualTo(expected);
    }

    /**
     * Unsigned comparison: the sign bit of the high word is part of the timestamp, so the natural
     * {@link UUID#compareTo} ordering does not match the ordering a database applies.
     */
    private static Comparator<UUID> unsignedOrder() {
        return (left, right) -> {
            int high = Long.compareUnsigned(left.getMostSignificantBits(), right.getMostSignificantBits());
            return high != 0
                    ? high
                    : Long.compareUnsigned(left.getLeastSignificantBits(), right.getLeastSignificantBits());
        };
    }
}
