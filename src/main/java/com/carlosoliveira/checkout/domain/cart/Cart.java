package com.carlosoliveira.checkout.domain.cart;

import com.carlosoliveira.checkout.domain.catalog.Product;
import com.carlosoliveira.checkout.domain.event.DomainEvent;
import com.carlosoliveira.checkout.domain.event.PaymentApproved;
import com.carlosoliveira.checkout.domain.event.PaymentDeclined;
import com.carlosoliveira.checkout.domain.exception.CheckoutException;
import com.carlosoliveira.checkout.domain.payment.PaymentMethod;
import com.carlosoliveira.checkout.domain.payment.PaymentResult;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class Cart {

    private final UUID id;
    private final List<CartItem> items;
    private final List<PaymentMethod> paymentMethods;
    private final Instant createdAt;
    private final Instant expiresAt;
    private final List<DomainEvent> domainEvents = new ArrayList<>();
    private CartStatus status;
    private PaymentAttempt attempt;
    private PaymentDetails payment;

    private Cart(UUID id,
                 List<CartItem> items,
                 List<PaymentMethod> paymentMethods,
                 CartStatus status,
                 Instant createdAt,
                 Instant expiresAt,
                 PaymentAttempt attempt,
                 PaymentDetails payment) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.items = List.copyOf(items);
        this.paymentMethods = List.copyOf(paymentMethods);
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        this.attempt = attempt;
        this.payment = payment;
        if (this.items.isEmpty()) {
            throw new IllegalArgumentException("cart must contain at least one item");
        }
        if (this.items.stream().map(CartItem::currency).distinct().count() > 1) {
            throw new IllegalArgumentException("cart items must share the same currency");
        }
        if (this.status == CartStatus.PROCESSING && this.attempt == null) {
            throw new IllegalArgumentException("a processing cart must have a payment attempt");
        }
    }

    public static Cart create(UUID id,
                              List<Product> products,
                              List<PaymentMethod> paymentMethods,
                              Instant now,
                              Duration ttl) {
        List<CartItem> items = products.stream().map(CartItem::from).toList();
        return new Cart(id, items, paymentMethods, CartStatus.CREATED, now, now.plus(ttl), null, null);
    }

    public static Cart restore(UUID id,
                               List<CartItem> items,
                               List<PaymentMethod> paymentMethods,
                               CartStatus status,
                               Instant createdAt,
                               Instant expiresAt,
                               PaymentAttempt attempt,
                               PaymentDetails payment) {
        return new Cart(id, items, paymentMethods, status, createdAt, expiresAt, attempt, payment);
    }

    public void ensurePayable(Instant now, Duration processingTimeout) {
        if (status == CartStatus.APPROVED) {
            throw CheckoutException.alreadyApproved(id);
        }
        if (status == CartStatus.PROCESSING && !isProcessingStale(now, processingTimeout)) {
            throw CheckoutException.paymentInProgress(id);
        }
        if (isExpiredAt(now)) {
            throw CheckoutException.cartExpired(id);
        }
    }

    public PaymentMethod resolvePaymentMethod(String methodName) {
        return paymentMethods.stream()
                .filter(method -> method.matches(methodName))
                .findFirst()
                .orElseThrow(() -> CheckoutException.paymentMethodNotAllowed(id, methodName));
    }

    public CartStatus startProcessing(Instant now, Duration processingTimeout) {
        CartStatus previous = status;
        if (status.isPayable()) {
            attempt = PaymentAttempt.start(now);
        } else if (isProcessingStale(now, processingTimeout)) {
            attempt = attempt.renew(now);
        } else {
            throw new IllegalStateException("cart " + id + " cannot start processing from status " + status);
        }
        status = CartStatus.PROCESSING;
        return previous;
    }

    public void abortProcessing(CartStatus previous) {
        requireProcessing();
        status = previous.isPayable() ? previous : CartStatus.CREATED;
    }

    public void approve(PaymentMethod method, PaymentResult result) {
        requireProcessing();
        if (!result.isApproved()) {
            throw new IllegalArgumentException("payment result is not approved");
        }
        status = CartStatus.APPROVED;
        payment = new PaymentDetails(
                method.name(),
                method.type(),
                result.transactionId(),
                result.lastDigits(),
                result.pixCode(),
                total(),
                currency(),
                result.processedAt()
        );
        domainEvents.add(new PaymentApproved(
                UUID.randomUUID(),
                id,
                result.processedAt(),
                attempt.id(),
                result.transactionId(),
                method.type(),
                payment.amount(),
                payment.currency(),
                items.stream().map(CartItem::slug).toList()
        ));
    }

    public void decline(PaymentMethod method, PaymentResult result) {
        requireProcessing();
        if (result.isApproved()) {
            throw new IllegalArgumentException("payment result is approved");
        }
        status = CartStatus.DECLINED;
        domainEvents.add(new PaymentDeclined(
                UUID.randomUUID(),
                id,
                result.processedAt(),
                attempt.id(),
                result.transactionId(),
                method.type()
        ));
    }

    public List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> pulled = List.copyOf(domainEvents);
        domainEvents.clear();
        return pulled;
    }

    public boolean isProcessingStale(Instant now, Duration processingTimeout) {
        return status == CartStatus.PROCESSING && attempt.isStaleAt(now, processingTimeout);
    }

    public boolean isExpiredAt(Instant now) {
        return !now.isBefore(expiresAt);
    }

    public BigDecimal total() {
        return items.stream().map(CartItem::price).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public String currency() {
        return items.getFirst().currency();
    }

    private void requireProcessing() {
        if (status != CartStatus.PROCESSING) {
            throw new IllegalStateException("cart " + id + " is not processing a payment");
        }
    }

    public UUID getId() {
        return id;
    }

    public List<CartItem> getItems() {
        return items;
    }

    public List<PaymentMethod> getPaymentMethods() {
        return paymentMethods;
    }

    public CartStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Optional<PaymentAttempt> getAttempt() {
        return Optional.ofNullable(attempt);
    }

    public Optional<PaymentDetails> getPayment() {
        return Optional.ofNullable(payment);
    }
}
