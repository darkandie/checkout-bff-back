package com.carlosoliveira.checkout.adapter.out.payment;

import com.carlosoliveira.checkout.domain.payment.CardData;
import com.carlosoliveira.checkout.domain.payment.PaymentMethod;
import com.carlosoliveira.checkout.domain.payment.PaymentRequest;
import com.carlosoliveira.checkout.domain.payment.PaymentResult;
import com.carlosoliveira.checkout.domain.payment.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static com.carlosoliveira.checkout.support.Fixtures.CREDIT_CARD;
import static com.carlosoliveira.checkout.support.Fixtures.DEBIT_CARD;
import static com.carlosoliveira.checkout.support.Fixtures.FIXED_CLOCK;
import static com.carlosoliveira.checkout.support.Fixtures.NOW;
import static com.carlosoliveira.checkout.support.Fixtures.PIX;
import static com.carlosoliveira.checkout.support.Fixtures.approvedCard;
import static com.carlosoliveira.checkout.support.Fixtures.declinedCard;
import static org.assertj.core.api.Assertions.assertThat;

class SimulatedPaymentGatewayTest {

    private static final BigDecimal AMOUNT = new BigDecimal("39.90");

    private final SimulatedPaymentGateway gateway = new SimulatedPaymentGateway(
            new PaymentSimulatorProperties("0002", "Checkout Assinaturas", "Belem"), FIXED_CLOCK);

    @Test
    @DisplayName("approves a credit card returning only the last four digits")
    void approvesCreditCard() {
        PaymentResult result = gateway.charge(cardRequest(approvedCard(), CREDIT_CARD));

        assertThat(result.status()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(result.lastDigits()).isEqualTo("4242");
        assertThat(result.pixCode()).isNull();
        assertThat(result.transactionId()).isNotNull();
        assertThat(result.processedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("declines cards ending with the configured suffix")
    void declinesConfiguredSuffix() {
        PaymentResult credit = gateway.charge(cardRequest(declinedCard(), CREDIT_CARD));
        PaymentResult debit = gateway.charge(cardRequest(declinedCard(), DEBIT_CARD));

        assertThat(credit.status()).isEqualTo(PaymentStatus.DECLINED);
        assertThat(debit.status()).isEqualTo(PaymentStatus.DECLINED);
        assertThat(credit.lastDigits()).isNull();
    }

    @Test
    @DisplayName("approves Pix returning a copy-and-paste code instead of card digits")
    void approvesPix() {
        PaymentResult result = gateway.charge(new PaymentRequest(UUID.randomUUID(), PIX, AMOUNT, "BRL", null));

        assertThat(result.status()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(result.lastDigits()).isNull();
        assertThat(result.pixCode()).startsWith("000201");
        assertThat(result.pixCode()).contains("540539.90");
    }

    @Test
    @DisplayName("generates a unique transaction id per charge")
    void uniqueTransactionIds() {
        PaymentResult first = gateway.charge(cardRequest(approvedCard(), CREDIT_CARD));
        PaymentResult second = gateway.charge(cardRequest(approvedCard(), CREDIT_CARD));

        assertThat(first.transactionId()).isNotEqualTo(second.transactionId());
    }

    private static PaymentRequest cardRequest(CardData card, PaymentMethod method) {
        return new PaymentRequest(UUID.randomUUID(), method, AMOUNT, "BRL", card);
    }
}
