package io.github.dmitriyiliyov.hibernate.uuidv7gen.app;

import io.github.dmitriyiliyov.hibernate.uuidv7gen.UUIDv7;
import jakarta.persistence.*;

import java.util.UUID;

/**
 * Child entity, inserted through a cascade so its identifier must be generated as part of the same
 * flush that inserts the parent.
 */
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @UUIDv7
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(nullable = false)
    private String product;

    @Column(name = "amount_cents", nullable = false)
    private long amountCents;

    protected Order() {
    }

    Order(Customer customer, String product, long amountCents) {
        this.customer = customer;
        this.product = product;
        this.amountCents = amountCents;
    }

    public UUID getId() {
        return id;
    }

    public Customer getCustomer() {
        return customer;
    }

    public String getProduct() {
        return product;
    }

    public long getAmountCents() {
        return amountCents;
    }
}
