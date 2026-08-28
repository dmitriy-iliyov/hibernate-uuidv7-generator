package io.github.dmitriyiliyov.hibernate.uuidv7gen.app;

import io.github.dmitriyiliyov.hibernate.uuidv7gen.UUIDv7;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * Entity wired to an application-supplied generator, exercising {@link UUIDv7#generator()} through a
 * real Hibernate bootstrap.
 */
@Entity
@Table(name = "audit_records")
public class AuditRecord {

    @Id
    @UUIDv7(generator = SequencedUuidGenerator.class)
    private UUID id;

    @Column(nullable = false)
    private String message;

    protected AuditRecord() {
    }

    public AuditRecord(String message) {
        this.message = message;
    }

    public UUID getId() {
        return id;
    }

    public String getMessage() {
        return message;
    }
}
