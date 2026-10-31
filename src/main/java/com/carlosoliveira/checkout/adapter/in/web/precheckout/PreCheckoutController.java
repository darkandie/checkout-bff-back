package com.carlosoliveira.checkout.adapter.in.web.precheckout;

import com.carlosoliveira.checkout.application.precheckout.PreCheckoutCommand;
import com.carlosoliveira.checkout.application.precheckout.PreCheckoutService;
import com.carlosoliveira.checkout.domain.cart.Cart;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/operations")
public class PreCheckoutController {

    private final PreCheckoutService preCheckoutService;

    public PreCheckoutController(PreCheckoutService preCheckoutService) {
        this.preCheckoutService = preCheckoutService;
    }

    @PostMapping(
            path = "/pre-checkout",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<PreCheckoutResponse> preCheckout(@Valid @RequestBody PreCheckoutRequest request) {
        Cart cart = preCheckoutService.start(new PreCheckoutCommand(request.slugs()));
        return ResponseEntity.ok(PreCheckoutResponse.from(cart));
    }
}
