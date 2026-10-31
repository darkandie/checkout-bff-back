package com.carlosoliveira.checkout.adapter.out.paymentmethod;

import com.carlosoliveira.checkout.domain.exception.ErrorCode;
import com.carlosoliveira.checkout.domain.payment.PaymentMethod;
import com.carlosoliveira.checkout.domain.payment.PaymentMethodType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static com.carlosoliveira.checkout.support.Errors.errorCodeOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class PaymentMethodsHttpAdapterTest {

    private static final String BASE_URL = "http://payment-method-service";

    private MockRestServiceServer server;
    private PaymentMethodsHttpAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new PaymentMethodsHttpAdapter(builder.build());
    }

    @Test
    @DisplayName("maps enabled payment methods and ignores disabled or unsupported ones")
    void mapsEnabledMethods() {
        server.expect(requestTo(BASE_URL + "/payment-methods"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        [
                          {"code": "CREDIT_CARD", "name": "Cartão de Crédito", "enabled": true},
                          {"code": "DEBIT_CARD", "name": "Cartão de Débito", "enabled": false},
                          {"code": "BOLETO", "name": "Boleto", "enabled": true},
                          {"code": "pix", "name": "Pix", "enabled": true}
                        ]
                        """, MediaType.APPLICATION_JSON));

        List<PaymentMethod> methods = adapter.findAvailable();

        assertThat(methods).containsExactly(
                new PaymentMethod("Cartão de Crédito", PaymentMethodType.CREDIT_CARD),
                new PaymentMethod("Pix", PaymentMethodType.PIX));
        server.verify();
    }

    @Test
    @DisplayName("translates upstream failures into EXTERNAL_SERVICE_UNAVAILABLE")
    void upstreamFailure() {
        server.expect(requestTo(BASE_URL + "/payment-methods")).andRespond(withServerError());

        assertThat(errorCodeOf(() -> adapter.findAvailable())).isEqualTo(ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE);
    }
}
