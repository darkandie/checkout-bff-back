package com.carlosoliveira.checkout.adapter.in.web.error;

import com.carlosoliveira.checkout.domain.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorCatalogTest {

    @ParameterizedTest
    @EnumSource(ErrorCode.class)
    @DisplayName("every business error code has a frontend error definition")
    void mapsEveryBusinessError(ErrorCode errorCode) {
        ErrorCatalog error = ErrorCatalog.from(errorCode);

        assertThat(error.name()).isEqualTo(errorCode.name());
    }

    @ParameterizedTest
    @EnumSource(ErrorCatalog.class)
    @DisplayName("every error carries code, title, description and a primary button")
    void definesMandatoryFields(ErrorCatalog error) {
        FrontendErrorResponse response = error.toResponse(null);

        assertThat(response.displayType()).isNotNull();
        assertThat(response.errorCode()).isEqualTo(error.name());
        assertThat(response.frontendError().code()).matches("P-\\d{4}");
        assertThat(response.frontendError().title()).isNotEmpty();
        assertThat(response.frontendError().description()).isNotEmpty();
        assertThat(response.frontendError().primaryButton()).isNotNull();
        assertThat(response.frontendError().primaryButton().label()).isNotEmpty();
        assertThat(response.frontendError().primaryButton().url()).isNotEmpty();
    }

    @ParameterizedTest
    @EnumSource(ErrorCode.class)
    @DisplayName("business failures are reported as client errors")
    void businessFailuresAre4xx(ErrorCode errorCode) {
        ErrorCatalog error = ErrorCatalog.from(errorCode);

        if (errorCode == ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE) {
            assertThat(error.status().value()).isEqualTo(503);
        } else {
            assertThat(error.status().is4xxClientError()).isTrue();
        }
    }

    @Test
    @DisplayName("frontend codes are unique")
    void uniqueFrontendCodes() {
        long distinct = Arrays.stream(ErrorCatalog.values())
                .map(error -> error.toResponse(null).frontendError().code())
                .distinct()
                .count();

        assertThat(distinct).isEqualTo((long) ErrorCatalog.values().length);
    }

    @Test
    @DisplayName("payment declined matches the contract defined for the frontend")
    void paymentDeclinedContract() {
        UUID cartId = UUID.randomUUID();

        FrontendErrorResponse response = ErrorCatalog.PAYMENT_DECLINED.toResponse(cartId);

        assertThat(ErrorCatalog.PAYMENT_DECLINED.status().is4xxClientError()).isTrue();
        assertThat(response.displayType()).isEqualTo(DisplayType.BLOCK);
        assertThat(response.errorCode()).isEqualTo("PAYMENT_DECLINED");
        assertThat(response.cartId()).isEqualTo(cartId);
        assertThat(response.frontendError().code()).isEqualTo("P-1001");
        assertThat(response.frontendError().title()).isEqualTo("Pagamento não aprovado");
        assertThat(response.frontendError().description())
                .isEqualTo("Não foi possível concluir o pagamento. Tente novamente ou escolha outro meio.");
        assertThat(response.frontendError().primaryButton())
                .isEqualTo(new FrontendErrorResponse.Button("Tentar novamente", "RETRY"));
        assertThat(response.frontendError().secondaryButton())
                .isEqualTo(new FrontendErrorResponse.Button("Trocar meio de pagamento", "/checkout/pagamento"));
    }
}
