package com.jdevs.scalablecacheapi.service;

import com.jdevs.scalablecacheapi.dto.ProductRequest;
import com.jdevs.scalablecacheapi.dto.ProductResponse;

import java.util.List;

public interface ProductService {
    ProductResponse createProduct(ProductRequest request);

    ProductResponse getProductById(Long id);

    List<ProductResponse> getAllProducts();

    ProductResponse updateProduct(Long id, ProductRequest request);

    void deleteProduct(Long id);
}
