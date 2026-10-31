package com.carlosoliveira.checkout;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CheckoutBffApplication {

    public static void main(String[] args) {
        SpringApplication.run(CheckoutBffApplication.class, args);
    }
}
