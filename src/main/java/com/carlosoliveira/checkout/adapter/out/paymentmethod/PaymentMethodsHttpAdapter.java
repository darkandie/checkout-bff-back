package com.carlosoliveira.checkout.adapter.out.paymentmethod;

import com.carlosoliveira.checkout.application.port.out.PaymentMethodsPort;
import com.carlosoliveira.checkout.config.CacheConfig;
import com.carlosoliveira.checkout.domain.exception.ExternalServiceException;
import com.carlosoliveira.checkout.domain.payment.PaymentMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
public class PaymentMethodsHttpAdapter implements PaymentMethodsPort {

    private static final Logger log = LoggerFactory.getLogger(PaymentMethodsHttpAdapter.class);
    private static final String SERVICE_NAME = "payment-method-service";

    private final RestClient restClient;

    public PaymentMethodsHttpAdapter(@Qualifier("paymentMethodServiceRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    @Cacheable(cacheNames = CacheConfig.PAYMENT_METHODS_CACHE, key = "'all'", unless = "#result.isEmpty()")
    public List<PaymentMethod> findAvailable() {
        try {
            PaymentMethodResponse[] response = restClient.get()
                    .uri("/payment-methods")
                    .retrieve()
                    .body(PaymentMethodResponse[].class);
            if (response == null) {
                return List.of();
            }
            return Arrays.stream(response)
                    .filter(PaymentMethodResponse::enabled)
                    .map(this::toDomain)
                    .flatMap(Optional::stream)
                    .toList();
        } catch (RestClientException e) {
            log.warn("Failed to fetch payment methods from {}", SERVICE_NAME, e);
            throw new ExternalServiceException(SERVICE_NAME, e);
        }
    }

    private Optional<PaymentMethod> toDomain(PaymentMethodResponse response) {
        Optional<PaymentMethod> method = response.toDomain();
        if (method.isEmpty()) {
            log.warn("Ignoring unsupported payment method code {} from {}", response.code(), SERVICE_NAME);
        }
        return method;
    }
}
