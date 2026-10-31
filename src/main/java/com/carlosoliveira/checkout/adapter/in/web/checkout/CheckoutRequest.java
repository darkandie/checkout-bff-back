package com.carlosoliveira.checkout.adapter.in.web.checkout;

import com.carlosoliveira.checkout.adapter.in.web.error.CartScoped;
import com.carlosoliveira.checkout.application.checkout.CheckoutCommand;
import com.carlosoliveira.checkout.domain.payment.CardData;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CheckoutRequest(
        @NotNull UUID cartId,
        @NotNull @Valid PaymentMethodRequest paymentMethod
) implements CartScoped {

    public CheckoutCommand toCommand() {
        CardData card = paymentMethod.card() == null ? null : paymentMethod.card().toDomain();
        return new CheckoutCommand(cartId, paymentMethod.name(), card);
    }

    public record PaymentMethodRequest(
            @NotBlank @Size(max = 60) String name,
            @Valid CardRequest card
    ) {
    }

    public record CardRequest(
            @NotBlank @Size(max = 80) String holderName,
            @NotBlank @Pattern(regexp = "\\d{13,19}") String number,
            @NotBlank @Pattern(regexp = "(0[1-9]|1[0-2])/\\d{2}") String expiry,
            @NotBlank @Pattern(regexp = "\\d{3,4}") String cvv
    ) {

        CardData toDomain() {
            return new CardData(holderName == null ? null : holderName.trim(), number, expiry, cvv);
        }

        @Override
        public String toString() {
            return toDomain().toString();
        }
    }
}
