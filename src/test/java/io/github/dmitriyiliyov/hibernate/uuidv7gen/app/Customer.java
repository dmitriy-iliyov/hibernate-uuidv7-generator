package io.github.dmitriyiliyov.hibernate.uuidv7gen.app;

import io.github.dmitriyiliyov.hibernate.uuidv7gen.UUIDv7;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Aggregate root of the sample application: identified by a generated UUIDv7 and owning its orders.
 */
@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @UUIDv7
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Version
    private long version;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Order> orders = new ArrayList<>();

    protected Customer() {
    }

    public Customer(String name) {
        this.name = name;
    }

    public Order placeOrder(String product, long amountCents) {
        Order order = new Order(this, product, amountCents);
        orders.add(order);
        return order;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void rename(String name) {
        this.name = name;
    }

    public long getVersion() {
        return version;
    }

    public List<Order> getOrders() {
        return orders;
    }
}
