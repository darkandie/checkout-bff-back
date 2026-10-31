package com.carlosoliveira.checkout.application.checkout;

import com.carlosoliveira.checkout.application.CheckoutProperties;
import com.carlosoliveira.checkout.application.port.out.CartRepositoryPort;
import com.carlosoliveira.checkout.application.port.out.PaymentGatewayPort;
import com.carlosoliveira.checkout.domain.cart.Cart;
import com.carlosoliveira.checkout.domain.cart.CartStatus;
import com.carlosoliveira.checkout.domain.cart.PaymentAttempt;
import com.carlosoliveira.checkout.domain.exception.CheckoutException;
import com.carlosoliveira.checkout.domain.payment.CardData;
import com.carlosoliveira.checkout.domain.payment.PaymentMethod;
import com.carlosoliveira.checkout.domain.payment.PaymentRequest;
import com.carlosoliveira.checkout.domain.payment.PaymentResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
public class CheckoutService {

    private static final Logger log = LoggerFactory.getLogger(CheckoutService.class);

    private final CartRepositoryPort cartRepository;
    private final PaymentGatewayPort paymentGateway;
    private final CheckoutProperties properties;
    private final Clock clock;

    public CheckoutService(CartRepositoryPort cartRepository,
                           PaymentGatewayPort paymentGateway,
                           CheckoutProperties properties,
                           Clock clock) {
        this.cartRepository = cartRepository;
        this.paymentGateway = paymentGateway;
        this.properties = properties;
        this.clock = clock;
    }

    public Cart checkout(CheckoutCommand command) {
        Instant now = Instant.now(clock);
        Cart cart = cartRepository.findById(command.cartId())
                .orElseThrow(() -> CheckoutException.cartNotFound(command.cartId()));

        Duration processingTimeout = properties.processingTimeout();
        cart.ensurePayable(now, processingTimeout);
        PaymentMethod method = cart.resolvePaymentMethod(command.paymentMethodName());
        validatePaymentData(cart.getId(), method, command.card(), now);

        CartStatus previousStatus = cart.startProcessing(now, processingTimeout);
        if (!cartRepository.claimForProcessing(cart, now.minus(processingTimeout))) {
            throw concurrentCheckoutConflict(cart.getId());
        }
        if (previousStatus == CartStatus.PROCESSING) {
            log.warn("Recovering payment stuck in processing cartId={} attemptId={}",
                    cart.getId(), cart.getAttempt().map(PaymentAttempt::id).orElse(null));
        }

        PaymentResult result;
        try {
            result = paymentGateway.charge(
                    new PaymentRequest(cart.getId(), method, cart.total(), cart.currency(), command.card()));
        } catch (RuntimeException e) {
            log.error("Payment processing failed cartId={}", cart.getId(), e);
            cart.abortProcessing(previousStatus);
            cartRepository.save(cart);
            throw e;
        }

        if (result.isApproved()) {
            cart.approve(method, result);
            log.info("Payment approved cartId={} method={} transactionId={}",
                    cart.getId(), method.type(), result.transactionId());
            return cartRepository.save(cart);
        }

        cart.decline();
        cartRepository.save(cart);
        log.info("Payment declined cartId={} method={} transactionId={}",
                cart.getId(), method.type(), result.transactionId());
        throw CheckoutException.paymentDeclined(cart.getId());
    }

    private void validatePaymentData(UUID cartId, PaymentMethod method, CardData card, Instant now) {
        if (!method.type().requiresCard()) {
            if (card != null) {
                throw CheckoutException.invalidPaymentData(cartId, "Card data must not be sent for " + method.type());
            }
            return;
        }
        if (card == null) {
            throw CheckoutException.invalidPaymentData(cartId, "Card data is required for " + method.type());
        }
        if (card.isExpiredAt(YearMonth.from(now.atZone(ZoneOffset.UTC)))) {
            throw CheckoutException.invalidPaymentData(cartId, "Card is expired");
        }
    }

    private CheckoutException concurrentCheckoutConflict(UUID cartId) {
        boolean approved = cartRepository.findById(cartId)
                .map(current -> current.getStatus() == CartStatus.APPROVED)
                .orElse(false);
        return approved ? CheckoutException.alreadyApproved(cartId) : CheckoutException.paymentInProgress(cartId);
    }
}
