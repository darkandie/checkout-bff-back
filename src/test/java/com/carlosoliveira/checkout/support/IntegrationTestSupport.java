package com.carlosoliveira.checkout.support;

import com.carlosoliveira.checkout.application.port.out.PaymentMethodsPort;
import com.carlosoliveira.checkout.application.port.out.ProductCatalogPort;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.mockito.Mockito.when;

@SpringBootTest(properties = "spring.cache.type=none")
@AutoConfigureMockMvc
public abstract class IntegrationTestSupport {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @MockitoBean
    protected ProductCatalogPort productCatalog;

    @MockitoBean
    protected PaymentMethodsPort paymentMethods;

    @BeforeEach
    void stubExternalServices() {
        when(productCatalog.findAll()).thenReturn(Fixtures.CATALOG);
        when(paymentMethods.findAvailable()).thenReturn(Fixtures.PAYMENT_METHODS);
    }
}
