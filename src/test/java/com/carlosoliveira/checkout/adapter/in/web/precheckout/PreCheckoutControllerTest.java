package com.carlosoliveira.checkout.adapter.in.web.precheckout;

import com.carlosoliveira.checkout.adapter.in.web.filter.CorrelationIdFilter;
import com.carlosoliveira.checkout.application.precheckout.PreCheckoutCommand;
import com.carlosoliveira.checkout.application.precheckout.PreCheckoutService;
import com.carlosoliveira.checkout.domain.cart.Cart;
import com.carlosoliveira.checkout.domain.exception.CheckoutException;
import com.carlosoliveira.checkout.domain.exception.ExternalServiceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static com.carlosoliveira.checkout.support.ErrorContract.assertFrontendError;
import static com.carlosoliveira.checkout.support.Fixtures.PREMIUM;
import static com.carlosoliveira.checkout.support.Fixtures.newCart;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PreCheckoutController.class)
class PreCheckoutControllerTest {

    private static final String URL = "/operations/pre-checkout";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PreCheckoutService preCheckoutService;

    @Test
    @DisplayName("returns the cart ready for the payment screen")
    void returnsCart() throws Exception {
        Cart cart = newCart(PREMIUM);
        when(preCheckoutService.start(any())).thenReturn(cart);

        String body = mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"products": [{"slug": "premium"}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cartId").value(cart.getId().toString()))
                .andExpect(jsonPath("$.products.length()").value(1))
                .andExpect(jsonPath("$.products[0].slug").value("premium"))
                .andExpect(jsonPath("$.products[0].name").value("Plano Premium"))
                .andExpect(jsonPath("$.products[0].description").value("Assinatura mensal com catálogo completo"))
                .andExpect(jsonPath("$.products[0].currency").value("BRL"))
                .andExpect(jsonPath("$.paymentMethods[0].name").value("Cartão de Crédito"))
                .andExpect(jsonPath("$.paymentMethods[1].name").value("Cartão de Débito"))
                .andExpect(jsonPath("$.paymentMethods[2].name").value("Pix"))
                .andExpect(jsonPath("$.paymentMethods[0].type").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).contains("\"price\":39.90");
        ArgumentCaptor<PreCheckoutCommand> command = ArgumentCaptor.forClass(PreCheckoutCommand.class);
        verify(preCheckoutService).start(command.capture());
        assertThat(command.getValue().productSlugs()).isEqualTo(List.of("premium"));
    }

    @Test
    @DisplayName("returns PRODUCT_NOT_FOUND with a null cartId for unknown products")
    void unknownProduct() throws Exception {
        when(preCheckoutService.start(any())).thenThrow(CheckoutException.productNotFound(List.of("ultra")));

        String body = mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"products": [{"slug": "ultra"}]}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andReturn().getResponse().getContentAsString();

        assertFrontendError(body, "PRODUCT_NOT_FOUND", "P-2001", null);
    }

    @Test
    @DisplayName("rejects a request without products")
    void rejectsEmptyProducts() throws Exception {
        String body = mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"products": []}
                                """))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();

        assertFrontendError(body, "INVALID_REQUEST", "P-0001", null);
        verify(preCheckoutService, never()).start(any());
    }

    @Test
    @DisplayName("rejects a product without slug")
    void rejectsBlankSlug() throws Exception {
        String body = mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"products": [{"slug": " "}]}
                                """))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();

        assertFrontendError(body, "INVALID_REQUEST", "P-0001", null);
    }

    @Test
    @DisplayName("rejects malformed JSON")
    void rejectsMalformedJson() throws Exception {
        String body = mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"products\": ["))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();

        assertFrontendError(body, "INVALID_REQUEST", "P-0001", null);
    }

    @Test
    @DisplayName("rejects payloads that are not JSON")
    void rejectsUnsupportedMediaType() throws Exception {
        String body = mockMvc.perform(post(URL)
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("premium"))
                .andExpect(status().isUnsupportedMediaType())
                .andReturn().getResponse().getContentAsString();

        assertFrontendError(body, "UNSUPPORTED_MEDIA_TYPE", "P-0004", null);
    }

    @Test
    @DisplayName("returns 503 with the same error object when an upstream is unavailable")
    void upstreamUnavailable() throws Exception {
        when(preCheckoutService.start(any()))
                .thenThrow(new ExternalServiceException("product-service", new RuntimeException("timeout")));

        String body = mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"products": [{"slug": "premium"}]}
                                """))
                .andExpect(status().isServiceUnavailable())
                .andReturn().getResponse().getContentAsString();

        assertFrontendError(body, "EXTERNAL_SERVICE_UNAVAILABLE", "P-9001", null);
    }

    @Test
    @DisplayName("returns 500 with the same error object on unexpected failures")
    void unexpectedFailure() throws Exception {
        when(preCheckoutService.start(any())).thenThrow(new IllegalStateException("boom"));

        String body = mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"products": [{"slug": "premium"}]}
                                """))
                .andExpect(status().isInternalServerError())
                .andReturn().getResponse().getContentAsString();

        assertFrontendError(body, "UNEXPECTED_ERROR", "P-9999", null);
        assertThat(body).doesNotContain("boom");
    }

    @Test
    @DisplayName("keeps the error contract for unknown routes and unsupported methods")
    void unknownRouteAndMethod() throws Exception {
        String notFound = mockMvc.perform(get("/operations/unknown"))
                .andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString();
        String notAllowed = mockMvc.perform(get(URL))
                .andExpect(status().isMethodNotAllowed())
                .andReturn().getResponse().getContentAsString();

        assertFrontendError(notFound, "RESOURCE_NOT_FOUND", "P-0002", null);
        assertFrontendError(notAllowed, "METHOD_NOT_SUPPORTED", "P-0003", null);
    }

    @Test
    @DisplayName("propagates a valid correlation id and generates one otherwise")
    void correlationId() throws Exception {
        when(preCheckoutService.start(any())).thenReturn(newCart(PREMIUM));
        String content = """
                {"products": [{"slug": "premium"}]}
                """;

        mockMvc.perform(post(URL)
                        .header(CorrelationIdFilter.HEADER, "checkout-test-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content))
                .andExpect(header().string(CorrelationIdFilter.HEADER, "checkout-test-123"));

        String generated = mockMvc.perform(post(URL)
                        .header(CorrelationIdFilter.HEADER, "invalid value\nwith line break")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content))
                .andReturn().getResponse().getHeader(CorrelationIdFilter.HEADER);

        assertThat(generated).matches("[0-9a-f-]{36}");
    }
}
