package com.carlosoliveira.checkout.mock.productservice;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/mocks/product-service")
@ConditionalOnProperty(prefix = "mocks", name = "enabled", havingValue = "true")
public class MockProductServiceController {

    private static final List<MockProduct> CATALOG = List.of(
            new MockProduct("basico", "Plano Básico",
                    "Assinatura mensal com catálogo essencial", new BigDecimal("19.90"), "BRL", true),
            new MockProduct("premium", "Plano Premium",
                    "Assinatura mensal com catálogo completo", new BigDecimal("39.90"), "BRL", true),
            new MockProduct("familia", "Plano Família",
                    "Assinatura mensal com catálogo completo para até 4 perfis", new BigDecimal("59.90"), "BRL", true)
    );

    @GetMapping("/products")
    public List<MockProduct> listProducts() {
        return CATALOG;
    }

    public record MockProduct(
            String slug,
            String name,
            String description,
            BigDecimal price,
            String currency,
            boolean active
    ) {
    }
}
