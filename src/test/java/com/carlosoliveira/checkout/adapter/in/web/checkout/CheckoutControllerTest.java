package com.carlosoliveira.checkout.adapter.in.web.checkout;

import com.carlosoliveira.checkout.application.checkout.CheckoutCommand;
import com.carlosoliveira.checkout.application.checkout.CheckoutService;
import com.carlosoliveira.checkout.domain.cart.Cart;
import com.carlosoliveira.checkout.domain.exception.CheckoutException;
import com.carlosoliveira.checkout.domain.payment.PaymentResult;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static com.carlosoliveira.checkout.support.ErrorContract.assertFrontendError;
import static com.carlosoliveira.checkout.support.Fixtures.NOW;
import static com.carlosoliveira.checkout.support.Fixtures.PIX;
import static com.carlosoliveira.checkout.support.Fixtures.PREMIUM;
import static com.carlosoliveira.checkout.support.Fixtures.approvedCart;
import static com.carlosoliveira.checkout.support.Fixtures.newCart;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CheckoutController.class)
class CheckoutControllerTest {

    private static final String URL = "/operations/checkout";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CheckoutService checkoutService;

    @Test
    @DisplayName("returns payment and product details for the success screen")
    void approvedCardPayment() throws Exception {
        Cart cart = approvedCart(PREMIUM);
        when(checkoutService.checkout(any())).thenReturn(cart);

        String body = mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardPayload(cart.getId(), "4111111111114242")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cartId").value(cart.getId().toString()))
                .andExpect(jsonPath("$.status").value("APROVADO"))
                .andExpect(jsonPath("$.paidAt").value("2026-10-31T12:00:00Z"))
                .andExpect(jsonPath("$.payment.method").value("Cartão de Crédito"))
                .andExpect(jsonPath("$.payment.lastDigits").value("4242"))
                .andExpect(jsonPath("$.payment.currency").value("BRL"))
                .andExpect(jsonPath("$.payment.pixCode").doesNotExist())
                .andExpect(jsonPath("$.products[0].slug").value("premium"))
                .andExpect(jsonPath("$.products[0].name").value("Plano Premium"))
                .andExpect(jsonPath("$.products[0].description").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).contains("\"amount\":39.90");
        assertThat(body).doesNotContain("4111111111114242");
        assertThat(body).doesNotContain("\"cvv\"");

        ArgumentCaptor<CheckoutCommand> command = ArgumentCaptor.forClass(CheckoutCommand.class);
        verify(checkoutService).checkout(command.capture());
        assertThat(command.getValue().cartId()).isEqualTo(cart.getId());
        assertThat(command.getValue().paymentMethodName()).isEqualTo("Cartão de Crédito");
        assertThat(command.getValue().card().number()).isEqualTo("4111111111114242");
        assertThat(command.getValue().card().holderName()).isEqualTo("Ana Souza");
    }

    @Test
    @DisplayName("returns the Pix code instead of card digits for Pix payments")
    void approvedPixPayment() throws Exception {
        Cart cart = newCart(PREMIUM);
        cart.startProcessing();
        cart.approve(PIX, PaymentResult.approvedPix("tx-pix", "00020126PIXCODE", NOW));
        when(checkoutService.checkout(any())).thenReturn(cart);

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pixPayload(cart.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payment.method").value("Pix"))
                .andExpect(jsonPath("$.payment.pixCode").value("00020126PIXCODE"))
                .andExpect(jsonPath("$.payment.lastDigits").doesNotExist());

        ArgumentCaptor<CheckoutCommand> command = ArgumentCaptor.forClass(CheckoutCommand.class);
        verify(checkoutService).checkout(command.capture());
        assertThat(command.getValue().card()).isNull();
    }

    @Test
    @DisplayName("returns the PAYMENT_DECLINED contract expected by the frontend")
    void declinedPayment() throws Exception {
        UUID cartId = UUID.randomUUID();
        when(checkoutService.checkout(any())).thenThrow(CheckoutException.paymentDeclined(cartId));

        String body = mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardPayload(cartId, "4000000000000002")))
                .andExpect(status().isUnprocessableEntity())
                .andReturn().getResponse().getContentAsString();

        JsonNode error = assertFrontendError(body, "PAYMENT_DECLINED", "P-1001", cartId);
        assertThat(error.get("displayType").asText()).isEqualTo("BLOCK");
        assertThat(error.at("/frontendError/title").asText()).isEqualTo("Pagamento não aprovado");
        assertThat(error.at("/frontendError/primaryButton/label").asText()).isEqualTo("Tentar novamente");
        assertThat(error.at("/frontendError/primaryButton/url").asText()).isEqualTo("RETRY");
        assertThat(error.at("/frontendError/secondaryButton/label").asText()).isEqualTo("Trocar meio de pagamento");
        assertThat(error.at("/frontendError/secondaryButton/url").asText()).isEqualTo("/checkout/pagamento");
    }

    @Test
    @DisplayName("returns 409 as an intervention when the cart was already paid")
    void alreadyApproved() throws Exception {
        UUID cartId = UUID.randomUUID();
        when(checkoutService.checkout(any())).thenThrow(CheckoutException.alreadyApproved(cartId));

        String body = mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pixPayload(cartId)))
                .andExpect(status().isConflict())
                .andReturn().getResponse().getContentAsString();

        JsonNode error = assertFrontendError(body, "PAYMENT_ALREADY_APPROVED", "P-5001", cartId);
        assertThat(error.get("displayType").asText()).isEqualTo("INTERVENTION");
        assertThat(error.at("/frontendError/secondaryButton").isNull()).isTrue();
    }

    @Test
    @DisplayName("returns 404 for an unknown cart")
    void unknownCart() throws Exception {
        UUID cartId = UUID.randomUUID();
        when(checkoutService.checkout(any())).thenThrow(CheckoutException.cartNotFound(cartId));

        String body = mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pixPayload(cartId)))
                .andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString();

        assertFrontendError(body, "CART_NOT_FOUND", "P-3001", cartId);
    }

    @Test
    @DisplayName("rejects malformed card data echoing the cart id")
    void rejectsInvalidCard() throws Exception {
        UUID cartId = UUID.randomUUID();

        String body = mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardPayload(cartId, "4111-1111")))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();

        assertFrontendError(body, "INVALID_REQUEST", "P-0001", cartId);
        verify(checkoutService, never()).checkout(any());
    }

    @Test
    @DisplayName("rejects a request without cart id")
    void rejectsMissingCartId() throws Exception {
        String body = mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"paymentMethod": {"name": "Pix"}}
                                """))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();

        assertFrontendError(body, "INVALID_REQUEST", "P-0001", null);
        verify(checkoutService, never()).checkout(any());
    }

    @Test
    @DisplayName("rejects a cart id that is not a UUID")
    void rejectsInvalidCartId() throws Exception {
        String body = mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cartId": "abc", "paymentMethod": {"name": "Pix"}}
                                """))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();

        assertFrontendError(body, "INVALID_REQUEST", "P-0001", null);
    }

    private static String cardPayload(UUID cartId, String number) {
        return """
                {
                  "cartId": "%s",
                  "paymentMethod": {
                    "name": "Cartão de Crédito",
                    "card": {"holderName": "Ana Souza", "number": "%s", "expiry": "12/28", "cvv": "123"}
                  }
                }
                """.formatted(cartId, number);
    }

    private static String pixPayload(UUID cartId) {
        return """
                {"cartId": "%s", "paymentMethod": {"name": "Pix"}}
                """.formatted(cartId);
    }
}
