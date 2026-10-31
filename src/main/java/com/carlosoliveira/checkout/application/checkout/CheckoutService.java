package com.carlosoliveira.checkout.application.checkout;

import com.carlosoliveira.checkout.application.port.out.CartRepositoryPort;
import com.carlosoliveira.checkout.application.port.out.PaymentGatewayPort;
import com.carlosoliveira.checkout.domain.cart.Cart;
import com.carlosoliveira.checkout.domain.cart.CartStatus;
import com.carlosoliveira.checkout.domain.exception.CheckoutException;
import com.carlosoliveira.checkout.domain.payment.CardData;
import com.carlosoliveira.checkout.domain.payment.PaymentMethod;
import com.carlosoliveira.checkout.domain.payment.PaymentRequest;
import com.carlosoliveira.checkout.domain.payment.PaymentResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
public class CheckoutService {

    private static final Logger log = LoggerFactory.getLogger(CheckoutService.class);

    private final CartRepositoryPort cartRepository;
    private final PaymentGatewayPort paymentGateway;
    private final Clock clock;

    public CheckoutService(CartRepositoryPort cartRepository, PaymentGatewayPort paymentGateway, Clock clock) {
        this.cartRepository = cartRepository;
        this.paymentGateway = paymentGateway;
        this.clock = clock;
    }

    public Cart checkout(CheckoutCommand command) {
        Instant now = Instant.now(clock);
        Cart cart = cartRepository.findById(command.cartId())
                .orElseThrow(() -> CheckoutException.cartNotFound(command.cartId()));

        cart.ensurePayable(now);
        PaymentMethod method = cart.resolvePaymentMethod(command.paymentMethodName());
        validatePaymentData(cart.getId(), method, command.card(), now);

        if (!cartRepository.claimForProcessing(cart.getId(), now)) {
            throw concurrentCheckoutConflict(cart.getId());
        }
        CartStatus previousStatus = cart.startProcessing();

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
