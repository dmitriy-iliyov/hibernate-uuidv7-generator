package io.github.dmitriyiliyov.hibernate.uuidv7gen.app;

import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

/**
 * Minimal application service over a real {@link SessionFactory}.
 * <p>
 * Every method runs in its own transaction and returns detached results, so tests observe the same
 * behaviour an ordinary application would: identifiers assigned before the {@code INSERT}, and rows
 * read back through a fresh session rather than out of the persistence context.
 */
public class ShopApplication implements AutoCloseable {

    private final SessionFactory sessionFactory;

    public ShopApplication(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    /**
     * Registers a customer and returns the identifier assigned during {@code persist}, captured
     * before the transaction commits.
     */
    public UUID registerCustomer(String name) {
        return inTransaction(session -> {
            Customer customer = new Customer(name);
            session.persist(customer);
            // Read back immediately: a BeforeExecutionGenerator must have populated the id already.
            return customer.getId();
        });
    }

    /** Registers a customer together with its orders in a single cascading flush. */
    public UUID registerCustomerWithOrders(String name, List<String> products) {
        return inTransaction(session -> {
            Customer customer = new Customer(name);
            products.forEach(product -> customer.placeOrder(product, 1_000L));
            session.persist(customer);
            return customer.getId();
        });
    }

    public Customer findCustomer(UUID id) {
        return inTransaction(session -> {
            Customer customer = session.find(Customer.class, id);
            if (customer != null) {
                customer.getOrders().size(); // initialise before detaching
            }
            return customer;
        });
    }

    public void renameCustomer(UUID id, String newName) {
        inTransaction(session -> {
            session.find(Customer.class, id).rename(newName);
            return null;
        });
    }

    public List<UUID> findOrderIds(UUID customerId) {
        return inTransaction(session -> session
                .createQuery("select o.id from Order o where o.customer.id = :id order by o.id", UUID.class)
                .setParameter("id", customerId)
                .getResultList());
    }

    /** Customer identifiers as the database sorts them, which for UUIDv7 is creation order. */
    public List<UUID> findAllCustomerIdsSorted() {
        return inTransaction(session -> session
                .createQuery("select c.id from Customer c order by c.id", UUID.class)
                .getResultList());
    }

    public UUID recordAudit(String message) {
        return inTransaction(session -> {
            AuditRecord record = new AuditRecord(message);
            session.persist(record);
            return record.getId();
        });
    }

    /** Reads the identifier straight out of the table, bypassing the persistence context. */
    public UUID readCustomerIdViaSql(String name) {
        return inTransaction(session -> session
                .createNativeQuery("select id from customers where name = :name", UUID.class)
                .setParameter("name", name)
                .getSingleResult());
    }

    public long countCustomers() {
        return inTransaction(session -> session
                .createQuery("select count(c) from Customer c", Long.class)
                .getSingleResult());
    }

    private <T> T inTransaction(Function<Session, T> work) {
        return sessionFactory.fromTransaction(work::apply);
    }

    @Override
    public void close() {
        sessionFactory.close();
    }
}
