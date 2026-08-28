package io.github.dmitriyiliyov.hibernate.uuidv7gen;

import com.github.f4b6a3.uuid.UuidCreator;
import org.hibernate.HibernateException;
import org.hibernate.generator.EventType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.EnumSet;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static io.github.dmitriyiliyov.hibernate.uuidv7gen.support.Uuids.assertIsUuidV7;
import static org.assertj.core.api.Assertions.*;

/**
 * Exercises {@link UUIDv7Generator} without a Hibernate bootstrap: annotations are read off real
 * fields, so the reflective wiring is the one Hibernate itself performs.
 */
class UUIDv7GeneratorUnitTest {

    private static final String NO_ARG_CONSTRUCTOR = "no-argument constructor";

    @Test
    @DisplayName("UT getEventTypes() when called should contain the insert event only")
    void getEventTypes_whenCalled_shouldContainInsertEventOnly() {
        // given
        UUIDv7Generator tested = new UUIDv7Generator();

        // when
        EnumSet<EventType> result = tested.getEventTypes();

        // then
        assertThat(result).containsExactly(EventType.INSERT);
        assertThat(result).doesNotContain(EventType.UPDATE);
    }

    @Test
    @DisplayName("UT initialize() when the annotation declares no generator should fall back to DefaultUuidGenerator")
    void initialize_whenAnnotationDeclaresNoGenerator_shouldFallBackToDefaultUuidGenerator() {
        // given
        UUIDv7Generator tested = initializedGenerator("defaultGenerator");

        // when
        Object result = tested.generate(null, new Object(), null, EventType.INSERT);

        // then
        assertThat(annotation("defaultGenerator").generator()).isEqualTo(DefaultUuidGenerator.class);
        assertThat(result).isInstanceOf(UUID.class);
        assertIsUuidV7((UUID) result);
    }

    @Test
    @DisplayName("UT generate() when the annotation declares a generator should delegate to it")
    void generate_whenAnnotationDeclaresGenerator_shouldDelegateToIt() {
        // given
        UUIDv7Generator tested = initializedGenerator("customGenerator");

        // when
        Object result = tested.generate(null, new Object(), null, EventType.INSERT);

        // then
        assertThat(result).isEqualTo(FixedUuidGenerator.VALUE);
    }

    @Test
    @DisplayName("UT initialize() when called should instantiate the generator once and reuse it for every value")
    void initialize_whenCalled_shouldInstantiateGeneratorOnceAndReuseItForEveryValue() {
        // given
        CountingGenerator.INSTANCES.set(0);

        // when
        UUIDv7Generator tested = initializedGenerator("counting");
        Object first = tested.generate(null, new Object(), null, EventType.INSERT);
        Object second = tested.generate(null, new Object(), null, EventType.INSERT);

        // then
        assertThat(CountingGenerator.INSTANCES).hasValue(1);
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    @DisplayName("UT generate() when the session owner and current value are null should still return a value")
    void generate_whenSessionOwnerAndCurrentValueAreNull_shouldStillReturnValue() {
        // given
        UUIDv7Generator tested = initializedGenerator("customGenerator");

        // when / then
        assertThatCode(() -> tested.generate(null, null, null, EventType.INSERT)).doesNotThrowAnyException();
        assertThat(tested.generate(null, null, UUID.randomUUID(), EventType.INSERT))
                .isEqualTo(FixedUuidGenerator.VALUE);
    }

    @Test
    @DisplayName("UT initialize() when the declared generator has no no-arg constructor should throw HibernateException")
    void initialize_whenDeclaredGeneratorHasNoNoArgConstructor_shouldThrowHibernateException() {
        // when / then
        assertThatThrownBy(() -> initializedGenerator("withoutNoArgConstructor"))
                .isInstanceOf(HibernateException.class)
                .hasMessageContaining(NoDefaultConstructorGenerator.class.getName())
                .hasMessageContaining(NO_ARG_CONSTRUCTOR)
                .hasRootCauseInstanceOf(NoSuchMethodException.class);
    }

    @Test
    @DisplayName("UT initialize() when the declared generator is abstract should throw HibernateException")
    void initialize_whenDeclaredGeneratorIsAbstract_shouldThrowHibernateException() {
        // when / then
        assertThatThrownBy(() -> initializedGenerator("abstractGenerator"))
                .isInstanceOf(HibernateException.class)
                .hasMessageContaining(AbstractGenerator.class.getName())
                .hasRootCauseInstanceOf(InstantiationException.class);
    }

    @Test
    @DisplayName("UT initialize() when the declared generator constructor throws should propagate the cause")
    void initialize_whenDeclaredGeneratorConstructorThrows_shouldPropagateCause() {
        // when / then
        assertThatThrownBy(() -> initializedGenerator("throwingConstructor"))
                .isInstanceOf(HibernateException.class)
                .hasRootCauseInstanceOf(IllegalStateException.class)
                .hasRootCauseMessage("boom");
    }

    private static UUIDv7Generator initializedGenerator(String fieldName) {
        UUIDv7Generator generator = new UUIDv7Generator();
        generator.initialize(annotation(fieldName), field(fieldName), null);
        return generator;
    }

    private static UUIDv7 annotation(String fieldName) {
        return field(fieldName).getAnnotation(UUIDv7.class);
    }

    private static Field field(String fieldName) {
        try {
            return Ids.class.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            throw new AssertionError(e);
        }
    }

    /** Holder whose fields supply real {@link UUIDv7} annotation instances. */
    @SuppressWarnings("unused")
    private static class Ids {

        @UUIDv7
        UUID defaultGenerator;

        @UUIDv7(generator = FixedUuidGenerator.class)
        UUID customGenerator;

        @UUIDv7(generator = CountingGenerator.class)
        UUID counting;

        @UUIDv7(generator = NoDefaultConstructorGenerator.class)
        UUID withoutNoArgConstructor;

        @UUIDv7(generator = AbstractGenerator.class)
        UUID abstractGenerator;

        @UUIDv7(generator = ThrowingConstructorGenerator.class)
        UUID throwingConstructor;
    }

    static class FixedUuidGenerator implements UuidGenerator {

        static final UUID VALUE = UUID.fromString("0189d6a0-1111-7222-8333-444444444444");

        @Override
        public UUID generate() {
            return VALUE;
        }
    }

    static class CountingGenerator implements UuidGenerator {

        static final AtomicInteger INSTANCES = new AtomicInteger();

        CountingGenerator() {
            INSTANCES.incrementAndGet();
        }

        @Override
        public UUID generate() {
            return UuidCreator.getTimeOrderedEpoch();
        }
    }

    static class NoDefaultConstructorGenerator implements UuidGenerator {

        NoDefaultConstructorGenerator(String required) {
        }

        @Override
        public UUID generate() {
            return UuidCreator.getTimeOrderedEpoch();
        }
    }

    abstract static class AbstractGenerator implements UuidGenerator {
    }

    static class ThrowingConstructorGenerator implements UuidGenerator {

        ThrowingConstructorGenerator() {
            throw new IllegalStateException("boom");
        }

        @Override
        public UUID generate() {
            return UuidCreator.getTimeOrderedEpoch();
        }
    }
}
