package com.carlosoliveira.checkout;

import com.carlosoliveira.checkout.support.IntegrationTestSupport;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.carlosoliveira.checkout.support.ErrorContract.assertFrontendError;
import static com.carlosoliveira.checkout.support.Fixtures.PIX;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CheckoutFlowIT extends IntegrationTestSupport {

    private static final String APPROVED_CARD = "4111111111114242";
    private static final String DECLINED_CARD = "4000000000000002";

    @Test
    @DisplayName("declines, retries, approves and then blocks a second payment of the same cart")
    void cardJourney() throws Exception {
        UUID cartId = preCheckout("premium");

        String declined = checkout(cardPayload(cartId, DECLINED_CARD))
                .andExpect(status().isUnprocessableEntity())
                .andReturn().getResponse().getContentAsString();
        assertFrontendError(declined, "PAYMENT_DECLINED", "P-1001", cartId);

        String approved = checkout(cardPayload(cartId, APPROVED_CARD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cartId").value(cartId.toString()))
                .andExpect(jsonPath("$.status").value("APROVADO"))
                .andExpect(jsonPath("$.paidAt").exists())
                .andExpect(jsonPath("$.payment.method").value("Cartão de Crédito"))
                .andExpect(jsonPath("$.payment.lastDigits").value("4242"))
                .andExpect(jsonPath("$.products[0].slug").value("premium"))
                .andReturn().getResponse().getContentAsString();
        assertThat(approved).contains("\"amount\":39.90");

        String duplicated = checkout(pixPayload(cartId))
                .andExpect(status().isConflict())
                .andReturn().getResponse().getContentAsString();
        assertFrontendError(duplicated, "PAYMENT_ALREADY_APPROVED", "P-5001", cartId);
    }

    @Test
    @DisplayName("never stores the full card number")
    void doesNotStoreCardNumber() throws Exception {
        UUID cartId = preCheckout("basico");
        checkout(cardPayload(cartId, APPROVED_CARD)).andExpect(status().isOk());

        String row = jdbcTemplate.queryForObject("select c::text from carts c where c.id = ?", String.class, cartId);
        String lastDigits = jdbcTemplate.queryForObject(
                "select payment_last_digits from carts where id = ?", String.class, cartId);

        assertThat(row).doesNotContain(APPROVED_CARD);
        assertThat(lastDigits).isEqualTo("4242");
    }

    @Test
    @DisplayName("approves Pix returning a copy-and-paste code")
    void pixJourney() throws Exception {
        UUID cartId = preCheckout("familia");

        String body = checkout(pixPayload(cartId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payment.method").value("Pix"))
                .andExpect(jsonPath("$.payment.lastDigits").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        String pixCode = JsonPath.read(body, "$.payment.pixCode");
        assertThat(pixCode).startsWith("000201");
        assertThat(pixCode).contains("540559.90");
    }

    @Test
    @DisplayName("charges the sum of every product in the cart")
    void multipleProducts() throws Exception {
        UUID cartId = preCheckout("basico", "familia");

        String body = checkout(pixPayload(cartId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products.length()").value(2))
                .andReturn().getResponse().getContentAsString();

        assertThat(body).contains("\"amount\":79.80");
    }

    @Test
    @DisplayName("rejects unknown products at pre-checkout")
    void unknownProduct() throws Exception {
        String body = mockMvc.perform(post("/operations/pre-checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"products": [{"slug": "ultra"}]}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andReturn().getResponse().getContentAsString();

        assertFrontendError(body, "PRODUCT_NOT_FOUND", "P-2001", null);
    }

    @Test
    @DisplayName("rejects checkout of a cart that was never created")
    void unknownCart() throws Exception {
        UUID cartId = UUID.randomUUID();

        String body = checkout(pixPayload(cartId))
                .andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString();

        assertFrontendError(body, "CART_NOT_FOUND", "P-3001", cartId);
    }

    @Test
    @DisplayName("only accepts payment methods offered for that cart at pre-checkout")
    void paymentMethodMustBeOfferedForCart() throws Exception {
        when(paymentMethods.findAvailable()).thenReturn(List.of(PIX));
        UUID cartId = preCheckout("premium");

        String body = checkout(cardPayload(cartId, APPROVED_CARD))
                .andExpect(status().isUnprocessableEntity())
                .andReturn().getResponse().getContentAsString();

        assertFrontendError(body, "PAYMENT_METHOD_NOT_ALLOWED", "P-4001", cartId);
    }

    @Test
    @DisplayName("rejects checkout of an expired cart")
    void expiredCart() throws Exception {
        UUID cartId = preCheckout("premium");
        jdbcTemplate.update("update carts set expires_at = now() - interval '1 minute' where id = ?", cartId);

        String body = checkout(pixPayload(cartId))
                .andExpect(status().isGone())
                .andReturn().getResponse().getContentAsString();

        assertFrontendError(body, "CART_EXPIRED", "P-3002", cartId);
    }

    @Test
    @DisplayName("approves concurrent checkouts of the same cart exactly once")
    void concurrentCheckouts() throws Exception {
        UUID cartId = preCheckout("premium");
        int attempts = 8;
        CountDownLatch start = new CountDownLatch(1);
        List<Callable<Integer>> requests = new ArrayList<>();
        for (int i = 0; i < attempts; i++) {
            requests.add(() -> {
                start.await();
                return checkout(cardPayload(cartId, APPROVED_CARD)).andReturn().getResponse().getStatus();
            });
        }

        List<Integer> statuses = new ArrayList<>();
        try (ExecutorService executor = Executors.newFixedThreadPool(attempts)) {
            List<Future<Integer>> futures = new ArrayList<>();
            for (Callable<Integer> request : requests) {
                futures.add(executor.submit(request));
            }
            start.countDown();
            for (Future<Integer> future : futures) {
                statuses.add(future.get());
            }
        }

        assertThat(statuses.stream().filter(status -> status == 200).count()).isEqualTo(1L);
        assertThat(statuses.stream().filter(status -> status == 409).count()).isEqualTo((long) attempts - 1);
        String status = jdbcTemplate.queryForObject("select status from carts where id = ?", String.class, cartId);
        assertThat(status).isEqualTo("APPROVED");
    }

    private UUID preCheckout(String... slugs) throws Exception {
        String products = String.join(",", Arrays.stream(slugs)
                .map(slug -> "{\"slug\": \"" + slug + "\"}")
                .toList());
        MvcResult result = mockMvc.perform(post("/operations/pre-checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"products\": [" + products + "]}"))
                .andExpect(status().isOk())
                .andReturn();
        return UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(), "$.cartId"));
    }

    private ResultActions checkout(String payload) throws Exception {
        return mockMvc.perform(post("/operations/checkout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload));
    }

    private static String cardPayload(UUID cartId, String number) {
        return """
                {
                  "cartId": "%s",
                  "paymentMethod": {
                    "name": "Cartão de Crédito",
                    "card": {"holderName": "Ana Souza", "number": "%s", "expiry": "%s", "cvv": "123"}
                  }
                }
                """.formatted(cartId, number, futureExpiry());
    }

    private static String pixPayload(UUID cartId) {
        return """
                {"cartId": "%s", "paymentMethod": {"name": "Pix"}}
                """.formatted(cartId);
    }

    private static String futureExpiry() {
        return YearMonth.now(ZoneOffset.UTC).plusYears(2).format(DateTimeFormatter.ofPattern("MM/yy"));
    }
}
