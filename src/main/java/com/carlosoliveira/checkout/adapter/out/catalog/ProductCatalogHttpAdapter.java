package com.carlosoliveira.checkout.adapter.out.catalog;

import com.carlosoliveira.checkout.application.port.out.ProductCatalogPort;
import com.carlosoliveira.checkout.config.CacheConfig;
import com.carlosoliveira.checkout.domain.catalog.Product;
import com.carlosoliveira.checkout.domain.exception.ExternalServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Arrays;
import java.util.List;

@Component
public class ProductCatalogHttpAdapter implements ProductCatalogPort {

    private static final Logger log = LoggerFactory.getLogger(ProductCatalogHttpAdapter.class);
    private static final String SERVICE_NAME = "product-service";

    private final RestClient restClient;

    public ProductCatalogHttpAdapter(@Qualifier("productServiceRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    @Cacheable(cacheNames = CacheConfig.PRODUCTS_CACHE, key = "'all'", unless = "#result.isEmpty()")
    public List<Product> findAll() {
        try {
            ProductResponse[] response = restClient.get()
                    .uri("/products")
                    .retrieve()
                    .body(ProductResponse[].class);
            if (response == null) {
                return List.of();
            }
            return Arrays.stream(response)
                    .filter(ProductResponse::active)
                    .map(ProductResponse::toDomain)
                    .toList();
        } catch (RestClientException e) {
            log.warn("Failed to fetch products from {}", SERVICE_NAME, e);
            throw new ExternalServiceException(SERVICE_NAME, e);
        }
    }
}
