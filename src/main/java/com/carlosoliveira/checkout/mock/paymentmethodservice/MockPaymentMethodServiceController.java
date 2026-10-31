package com.carlosoliveira.checkout.mock.paymentmethodservice;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/mocks/payment-method-service")
@ConditionalOnProperty(prefix = "mocks", name = "enabled", havingValue = "true")
public class MockPaymentMethodServiceController {

    private static final List<MockPaymentMethod> METHODS = List.of(
            new MockPaymentMethod("CREDIT_CARD", "Cartão de Crédito", true),
            new MockPaymentMethod("DEBIT_CARD", "Cartão de Débito", true),
            new MockPaymentMethod("PIX", "Pix", true)
    );

    @GetMapping("/payment-methods")
    public List<MockPaymentMethod> listPaymentMethods() {
        return METHODS;
    }

    public record MockPaymentMethod(String code, String name, boolean enabled) {
    }
}
