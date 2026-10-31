package com.carlosoliveira.checkout.adapter.out.persistence;

import com.carlosoliveira.checkout.domain.cart.CartStatus;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "carts")
public class CartJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CartStatus status;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "cart_items", joinColumns = @JoinColumn(name = "cart_id"))
    @OrderColumn(name = "item_order")
    private List<CartItemEmbeddable> items = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "cart_payment_methods", joinColumns = @JoinColumn(name = "cart_id"))
    @OrderColumn(name = "method_order")
    private List<CartPaymentMethodEmbeddable> paymentMethods = new ArrayList<>();

    @Embedded
    private PaymentEmbeddable payment;

    protected CartJpaEntity() {
    }

    public CartJpaEntity(UUID id,
                         CartStatus status,
                         String currency,
                         BigDecimal totalAmount,
                         Instant createdAt,
                         Instant expiresAt,
                         Instant updatedAt,
                         List<CartItemEmbeddable> items,
                         List<CartPaymentMethodEmbeddable> paymentMethods,
                         PaymentEmbeddable payment) {
        this.id = id;
        this.status = status;
        this.currency = currency;
        this.totalAmount = totalAmount;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.updatedAt = updatedAt;
        this.items = new ArrayList<>(items);
        this.paymentMethods = new ArrayList<>(paymentMethods);
        this.payment = payment;
    }

    public UUID getId() {
        return id;
    }

    public CartStatus getStatus() {
        return status;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<CartItemEmbeddable> getItems() {
        return items;
    }

    public List<CartPaymentMethodEmbeddable> getPaymentMethods() {
        return paymentMethods;
    }

    public PaymentEmbeddable getPayment() {
        return payment;
    }
}
