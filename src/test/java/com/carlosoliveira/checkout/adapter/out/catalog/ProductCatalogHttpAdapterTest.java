package com.carlosoliveira.checkout.adapter.out.catalog;

import com.carlosoliveira.checkout.domain.catalog.Product;
import com.carlosoliveira.checkout.domain.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static com.carlosoliveira.checkout.support.Errors.errorCodeOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ProductCatalogHttpAdapterTest {

    private static final String BASE_URL = "http://product-service";

    private MockRestServiceServer server;
    private ProductCatalogHttpAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new ProductCatalogHttpAdapter(builder.build());
    }

    @Test
    @DisplayName("maps active products from the product service")
    void mapsActiveProducts() {
        server.expect(requestTo(BASE_URL + "/products"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        [
                          {"slug": "basico", "name": "Plano Básico", "description": "Essencial",
                           "price": 19.9, "currency": "BRL", "active": true},
                          {"slug": "legado", "name": "Plano Legado", "description": "Descontinuado",
                           "price": 9.90, "currency": "BRL", "active": false},
                          {"slug": "premium", "name": "Plano Premium", "description": "Completo",
                           "price": 39.90, "currency": "BRL", "active": true}
                        ]
                        """, MediaType.APPLICATION_JSON));

        List<Product> products = adapter.findAll();

        assertThat(products.stream().map(Product::slug).toList()).containsExactly("basico", "premium");
        assertThat(products.get(0).price()).isEqualByComparingTo("19.90");
        assertThat(products.get(0).name()).isEqualTo("Plano Básico");
        server.verify();
    }

    @Test
    @DisplayName("returns an empty catalog when the service has no body")
    void emptyBody() {
        server.expect(requestTo(BASE_URL + "/products")).andRespond(withNoContent());

        assertThat(adapter.findAll()).isEmpty();
    }

    @Test
    @DisplayName("translates upstream failures into EXTERNAL_SERVICE_UNAVAILABLE")
    void upstreamFailure() {
        server.expect(requestTo(BASE_URL + "/products")).andRespond(withServerError());

        assertThat(errorCodeOf(() -> adapter.findAll())).isEqualTo(ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE);
    }
}
