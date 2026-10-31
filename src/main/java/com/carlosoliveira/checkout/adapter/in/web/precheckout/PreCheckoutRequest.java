package com.carlosoliveira.checkout.adapter.in.web.precheckout;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PreCheckoutRequest(
        @NotEmpty @Size(max = 10) List<@NotNull @Valid ProductSelection> products
) {

    public List<String> slugs() {
        return products.stream().map(ProductSelection::slug).toList();
    }

    public record ProductSelection(@NotBlank @Size(max = 60) String slug) {
    }
}
