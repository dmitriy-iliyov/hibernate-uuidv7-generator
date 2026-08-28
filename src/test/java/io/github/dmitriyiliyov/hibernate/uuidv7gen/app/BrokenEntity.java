package io.github.dmitriyiliyov.hibernate.uuidv7gen.app;

import io.github.dmitriyiliyov.hibernate.uuidv7gen.UUIDv7;
import io.github.dmitriyiliyov.hibernate.uuidv7gen.UuidGenerator;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * Entity whose declared generator cannot be instantiated, used to assert that a misconfiguration is
 * reported while the {@code SessionFactory} is built rather than on the first insert.
 */
@Entity
@Table(name = "broken")
public class BrokenEntity {

    @Id
    @UUIDv7(generator = BrokenEntity.UninstantiableGenerator.class)
    private UUID id;

    public static class UninstantiableGenerator implements UuidGenerator {

        public UninstantiableGenerator(String required) {
        }

        @Override
        public UUID generate() {
            throw new UnsupportedOperationException();
        }
    }

    public UUID getId() {
        return id;
    }
}
