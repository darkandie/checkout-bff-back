package com.carlosoliveira.checkout.adapter.out.payment;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "payment-simulator")
public record PaymentSimulatorProperties(
        @NotBlank String declinedCardSuffix,
        @NotBlank String pixMerchantName,
        @NotBlank String pixMerchantCity
) {
}
