package io.github.dmitriyiliyov.hibernate.uuidv7gen;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.AnnotationBasedGenerator;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.generator.GeneratorCreationContext;

import java.lang.reflect.Member;
import java.util.EnumSet;

/**
 * Hibernate generator backing the {@link UUIDv7} annotation.
 * <p>
 * Hibernate discovers this class through {@link org.hibernate.annotations.IdGeneratorType} and
 * instantiates one generator per annotated identifier during {@code SessionFactory} bootstrap.
 * As a {@link BeforeExecutionGenerator} restricted to {@link EventType#INSERT}, it assigns the
 * identifier in memory before the {@code INSERT} is issued and leaves it untouched on update.
 * <p>
 * The actual value comes from the {@link UuidGenerator} named by {@link UUIDv7#generator()},
 * which is resolved once in {@link #initialize} and reused afterwards. Instances are shared
 * across sessions and must be treated as thread-safe.
 *
 * @see UUIDv7
 * @see UuidGenerator
 */
public class UUIDv7Generator implements BeforeExecutionGenerator, AnnotationBasedGenerator<UUIDv7> {

    private UuidGenerator generator;

    public UUIDv7Generator() { }

    @Override
    public void initialize(UUIDv7 annotation, Member member, GeneratorCreationContext context) {
        Class<? extends UuidGenerator> generatorType = annotation.generator();
        try {
            generator = generatorType.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new HibernateException(
                    """
                    Cannot instantiate UuidGenerator [%s] declared by @UUIDv7 on [%s]; \
                    it must be a concrete class with an accessible no-argument constructor\
                    """.formatted(generatorType.getName(), member),
                    e
            );
        }
    }

    @Override
    public Object generate(SharedSessionContractImplementor session, Object owner, Object currentValue, EventType eventType) {
        return generator.generate();
    }

    @Override
    public EnumSet<EventType> getEventTypes() {
        return EnumSet.of(EventType.INSERT);
    }
}
