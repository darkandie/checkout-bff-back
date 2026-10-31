package com.carlosoliveira.checkout.application.port.out;

import com.carlosoliveira.checkout.domain.catalog.Product;

import java.util.List;

public interface ProductCatalogPort {

    List<Product> findAll();
}
