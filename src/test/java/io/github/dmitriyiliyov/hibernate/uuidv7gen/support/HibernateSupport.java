package io.github.dmitriyiliyov.hibernate.uuidv7gen.support;

import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Boots a real Hibernate {@link SessionFactory} against an in-memory H2 database.
 * <p>
 * Each call gets its own database so tests stay independent.
 */
public final class HibernateSupport {

    private static final AtomicInteger DB_COUNTER = new AtomicInteger();

    private HibernateSupport() {
    }

    public static SessionFactory buildSessionFactory(Class<?>... annotatedClasses) {
        StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
                .applySetting(AvailableSettings.JAKARTA_JDBC_DRIVER, "org.h2.Driver")
                .applySetting(AvailableSettings.JAKARTA_JDBC_URL,
                        "jdbc:h2:mem:uuidv7-" + DB_COUNTER.incrementAndGet() + ";DB_CLOSE_DELAY=-1")
                .applySetting(AvailableSettings.JAKARTA_JDBC_USER, "sa")
                .applySetting(AvailableSettings.JAKARTA_JDBC_PASSWORD, "")
                .applySetting(AvailableSettings.HBM2DDL_AUTO, "create-drop")
                .applySetting(AvailableSettings.SHOW_SQL, "false")
                .build();
        try {
            MetadataSources sources = new MetadataSources(registry);
            for (Class<?> annotatedClass : annotatedClasses) {
                sources.addAnnotatedClass(annotatedClass);
            }
            return sources.buildMetadata().buildSessionFactory();
        } catch (RuntimeException e) {
            StandardServiceRegistryBuilder.destroy(registry);
            throw e;
        }
    }
}
