package com.carlosoliveira.checkout.adapter.in.web.precheckout;

import com.carlosoliveira.checkout.adapter.in.web.error.FrontendErrorResponse;
import com.carlosoliveira.checkout.application.precheckout.PreCheckoutCommand;
import com.carlosoliveira.checkout.application.precheckout.PreCheckoutService;
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

@Tag(name = "Pré-checkout")
@RestController
@RequestMapping("/operations")
public class PreCheckoutController {

    private final PreCheckoutService preCheckoutService;

    public PreCheckoutController(PreCheckoutService preCheckoutService) {
        this.preCheckoutService = preCheckoutService;
    }

    @Operation(
            summary = "Inicia o checkout",
            description = "Monta o carrinho a partir dos produtos escolhidos, com preços e meios de pagamento disponíveis.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Carrinho criado",
                            content = @Content(schema = @Schema(implementation = PreCheckoutResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Requisição inválida",
                            content = @Content(schema = @Schema(implementation = FrontendErrorResponse.class))),
                    @ApiResponse(responseCode = "422", description = "Produto inexistente (PRODUCT_NOT_FOUND)",
                            content = @Content(schema = @Schema(implementation = FrontendErrorResponse.class))),
                    @ApiResponse(responseCode = "503", description = "Serviço de produtos ou de meios de pagamento indisponível",
                            content = @Content(schema = @Schema(implementation = FrontendErrorResponse.class)))
            }
    )
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
