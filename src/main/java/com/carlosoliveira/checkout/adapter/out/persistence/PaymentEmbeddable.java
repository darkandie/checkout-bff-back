package com.carlosoliveira.checkout.adapter.out.persistence;

import com.carlosoliveira.checkout.domain.payment.PaymentMethodType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.math.BigDecimal;
import java.time.Instant;

@Embeddable
public class PaymentEmbeddable {

    @Column(name = "payment_method_name", length = 60)
    private String methodName;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method_type", length = 20)
    private PaymentMethodType methodType;

    @Column(name = "payment_transaction_id", length = 64)
    private String transactionId;

    @Column(name = "payment_last_digits", length = 4)
    private String lastDigits;

    @Column(name = "payment_pix_code", length = 512)
    private String pixCode;

    @Column(name = "payment_amount", precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "payment_currency", length = 3)
    private String currency;

    @Column(name = "paid_at")
    private Instant paidAt;

    protected PaymentEmbeddable() {
    }

    public PaymentEmbeddable(String methodName,
                             PaymentMethodType methodType,
                             String transactionId,
                             String lastDigits,
                             String pixCode,
                             BigDecimal amount,
                             String currency,
                             Instant paidAt) {
        this.methodName = methodName;
        this.methodType = methodType;
        this.transactionId = transactionId;
        this.lastDigits = lastDigits;
        this.pixCode = pixCode;
        this.amount = amount;
        this.currency = currency;
        this.paidAt = paidAt;
    }

    public String getMethodName() {
        return methodName;
    }

    public PaymentMethodType getMethodType() {
        return methodType;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getLastDigits() {
        return lastDigits;
    }

    public String getPixCode() {
        return pixCode;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public Instant getPaidAt() {
        return paidAt;
    }
}
