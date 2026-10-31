package com.carlosoliveira.checkout.adapter.in.web.checkout;

import com.carlosoliveira.checkout.adapter.in.web.error.FrontendErrorResponse;
import com.carlosoliveira.checkout.application.checkout.CheckoutService;
import com.carlosoliveira.checkout.domain.cart.Cart;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Checkout")
@RestController
@RequestMapping("/operations")
public class CheckoutController {

    private final CheckoutService checkoutService;

    public CheckoutController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @Operation(
            summary = "Conclui o pagamento",
            description = "Processa o pagamento simulado do carrinho. Cartões terminados em 0002 são recusados.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Pagamento aprovado",
                            content = @Content(schema = @Schema(implementation = CheckoutResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Requisição inválida",
                            content = @Content(schema = @Schema(implementation = FrontendErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Carrinho inexistente (CART_NOT_FOUND)",
                            content = @Content(schema = @Schema(implementation = FrontendErrorResponse.class))),
                    @ApiResponse(responseCode = "409", description = "Carrinho já pago ou pagamento em andamento",
                            content = @Content(schema = @Schema(implementation = FrontendErrorResponse.class))),
                    @ApiResponse(responseCode = "410", description = "Carrinho expirado (CART_EXPIRED)",
                            content = @Content(schema = @Schema(implementation = FrontendErrorResponse.class))),
                    @ApiResponse(responseCode = "422", description = "Pagamento recusado, meio não permitido ou dados de pagamento inválidos",
                            content = @Content(schema = @Schema(implementation = FrontendErrorResponse.class)))
            }
    )
    @PostMapping(
            path = "/checkout",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<CheckoutResponse> checkout(@Valid @RequestBody CheckoutRequest request) {
        Cart cart = checkoutService.checkout(request.toCommand());
        return ResponseEntity.ok(CheckoutResponse.from(cart));
    }
}
