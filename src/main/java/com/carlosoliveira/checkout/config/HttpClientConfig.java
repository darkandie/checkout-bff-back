package com.carlosoliveira.checkout.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class HttpClientConfig {

    @Bean
    RestClient productServiceRestClient(RestClient.Builder builder, ExternalServicesProperties properties) {
        return build(builder, properties.productService());
    }

    @Bean
    RestClient paymentMethodServiceRestClient(RestClient.Builder builder, ExternalServicesProperties properties) {
        return build(builder, properties.paymentMethodService());
    }

    private RestClient build(RestClient.Builder builder, ExternalServicesProperties.Service service) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(service.connectTimeout());
        requestFactory.setReadTimeout(service.readTimeout());
        return builder.clone()
                .baseUrl(service.baseUrl())
                .requestFactory(requestFactory)
                .build();
    }
}
