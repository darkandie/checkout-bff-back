package com.carlosoliveira.checkout.application.precheckout;

import java.util.List;

public record PreCheckoutCommand(List<String> productSlugs) {

    public PreCheckoutCommand {
        productSlugs = List.copyOf(productSlugs);
    }
}
