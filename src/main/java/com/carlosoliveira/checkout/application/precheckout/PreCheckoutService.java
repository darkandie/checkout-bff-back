package com.carlosoliveira.checkout.application.precheckout;

import com.carlosoliveira.checkout.application.CheckoutProperties;
import com.carlosoliveira.checkout.application.port.out.CartRepositoryPort;
import com.carlosoliveira.checkout.application.port.out.PaymentMethodsPort;
import com.carlosoliveira.checkout.application.port.out.ProductCatalogPort;
import com.carlosoliveira.checkout.domain.cart.Cart;
import com.carlosoliveira.checkout.domain.catalog.Product;
import com.carlosoliveira.checkout.domain.exception.CheckoutException;
import com.carlosoliveira.checkout.domain.exception.ExternalServiceException;
import com.carlosoliveira.checkout.domain.payment.PaymentMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PreCheckoutService {

    private static final Logger log = LoggerFactory.getLogger(PreCheckoutService.class);

    private final ProductCatalogPort productCatalog;
    private final PaymentMethodsPort paymentMethods;
    private final CartRepositoryPort cartRepository;
    private final CheckoutProperties properties;
    private final Clock clock;

    public PreCheckoutService(ProductCatalogPort productCatalog,
                              PaymentMethodsPort paymentMethods,
                              CartRepositoryPort cartRepository,
                              CheckoutProperties properties,
                              Clock clock) {
        this.productCatalog = productCatalog;
        this.paymentMethods = paymentMethods;
        this.cartRepository = cartRepository;
        this.properties = properties;
        this.clock = clock;
    }

    public Cart start(PreCheckoutCommand command) {
        Set<String> requestedSlugs = normalize(command.productSlugs());
        List<Product> products = resolveProducts(requestedSlugs);
        List<PaymentMethod> availableMethods = paymentMethods.findAvailable();
        if (availableMethods.isEmpty()) {
            throw new ExternalServiceException("payment-method-service", null);
        }

        Cart cart = Cart.create(UUID.randomUUID(), products, availableMethods, Instant.now(clock), properties.cartTtl());
        Cart saved = cartRepository.save(cart);
        log.info("Cart created cartId={} products={} total={}", saved.getId(), requestedSlugs, saved.total());
        return saved;
    }

    private List<Product> resolveProducts(Set<String> requestedSlugs) {
        Map<String, Product> catalog = productCatalog.findAll().stream()
                .collect(Collectors.toMap(Product::slug, Function.identity(), (first, second) -> first));

        List<String> missing = requestedSlugs.stream()
                .filter(slug -> !catalog.containsKey(slug))
                .toList();
        if (!missing.isEmpty()) {
            throw CheckoutException.productNotFound(missing);
        }
        return requestedSlugs.stream().map(catalog::get).toList();
    }

    private static Set<String> normalize(List<String> slugs) {
        return slugs.stream()
                .map(slug -> slug.trim().toLowerCase(Locale.ROOT))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
