package com.carlosoliveira.checkout.adapter.out.payment;

import com.carlosoliveira.checkout.application.port.out.PaymentGatewayPort;
import com.carlosoliveira.checkout.domain.payment.PaymentMethodType;
import com.carlosoliveira.checkout.domain.payment.PaymentRequest;
import com.carlosoliveira.checkout.domain.payment.PaymentResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Component
public class SimulatedPaymentGateway implements PaymentGatewayPort {

    private static final Logger log = LoggerFactory.getLogger(SimulatedPaymentGateway.class);

    private final PaymentSimulatorProperties properties;
    private final PixCodeGenerator pixCodeGenerator;
    private final Clock clock;

    public SimulatedPaymentGateway(PaymentSimulatorProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        this.pixCodeGenerator = new PixCodeGenerator(properties.pixMerchantName(), properties.pixMerchantCity());
    }

    @Override
    public PaymentResult charge(PaymentRequest request) {
        String transactionId = UUID.randomUUID().toString();
        Instant processedAt = Instant.now(clock).truncatedTo(ChronoUnit.SECONDS);

        if (request.method().type() == PaymentMethodType.PIX) {
            String pixCode = pixCodeGenerator.generate(pixCodeGenerator.randomKey(), request.amount(), transactionId);
            log.info("Simulated Pix charge approved cartId={} transactionId={}", request.cartId(), transactionId);
            return PaymentResult.approvedPix(transactionId, pixCode, processedAt);
        }

        if (request.card().endsWith(properties.declinedCardSuffix())) {
            log.info("Simulated card charge declined cartId={} transactionId={} card={}",
                    request.cartId(), transactionId, request.card());
            return PaymentResult.declined(transactionId, processedAt);
        }

        log.info("Simulated card charge approved cartId={} transactionId={} card={}",
                request.cartId(), transactionId, request.card());
        return PaymentResult.approvedCard(transactionId, request.card().lastDigits(), processedAt);
    }
}
