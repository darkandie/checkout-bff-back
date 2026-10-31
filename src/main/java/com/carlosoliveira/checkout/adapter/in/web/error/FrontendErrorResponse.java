package com.carlosoliveira.checkout.adapter.in.web.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.UUID;

@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonPropertyOrder({"displayType", "errorCode", "cartId", "frontendError"})
public record FrontendErrorResponse(
        DisplayType displayType,
        String errorCode,
        UUID cartId,
        FrontendError frontendError
) {

    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonPropertyOrder({"code", "title", "description", "primaryButton", "secondaryButton"})
    public record FrontendError(
            String code,
            String title,
            String description,
            Button primaryButton,
            Button secondaryButton
    ) {
    }

    public record Button(String label, String url) {
    }
}
