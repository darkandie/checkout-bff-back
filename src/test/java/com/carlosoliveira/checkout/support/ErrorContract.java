package com.carlosoliveira.checkout.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public final class ErrorContract {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ErrorContract() {
    }

    public static JsonNode assertFrontendError(String body, String errorCode, String code, UUID cartId) {
        JsonNode root = parse(body);

        assertThat(fieldNames(root)).containsExactly("displayType", "errorCode", "cartId", "frontendError");
        assertThat(root.get("displayType").asText()).matches("BLOCK|INTERVENTION");
        assertThat(root.get("errorCode").asText()).isEqualTo(errorCode);
        if (cartId == null) {
            assertThat(root.get("cartId").isNull()).isTrue();
        } else {
            assertThat(root.get("cartId").asText()).isEqualTo(cartId.toString());
        }

        JsonNode frontendError = root.get("frontendError");
        assertThat(fieldNames(frontendError))
                .containsExactly("code", "title", "description", "primaryButton", "secondaryButton");
        assertThat(frontendError.get("code").asText()).isEqualTo(code);
        assertThat(frontendError.get("title").asText()).isNotEmpty();
        assertThat(frontendError.get("description").asText()).isNotEmpty();
        assertThat(frontendError.get("primaryButton").get("label").asText()).isNotEmpty();
        assertThat(frontendError.get("primaryButton").get("url").asText()).isNotEmpty();
        return root;
    }

    private static JsonNode parse(String body) {
        try {
            return MAPPER.readTree(body);
        } catch (JsonProcessingException e) {
            throw new AssertionError("Response is not valid JSON: " + body, e);
        }
    }

    private static List<String> fieldNames(JsonNode node) {
        List<String> names = new ArrayList<>();
        node.fieldNames().forEachRemaining(names::add);
        return names;
    }
}
