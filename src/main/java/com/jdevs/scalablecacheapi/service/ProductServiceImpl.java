package com.jdevs.scalablecacheapi.service;

import com.jdevs.scalablecacheapi.dto.ProductRequest;
import com.jdevs.scalablecacheapi.dto.ProductResponse;
import com.jdevs.scalablecacheapi.entity.Product;
import com.jdevs.scalablecacheapi.exception.ResourceNotFoundException;
import com.jdevs.scalablecacheapi.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    @Override
    public ProductResponse createProduct(ProductRequest request) {
        Product product = Product.builder()
                .name(request.name())
                .description(request.description())
                .price(request.price())
                .quantityAvailable(request.quantityAvailable())
                .build();

        Product savedProduct = productRepository.save(product);

        log.info("Product created successfully. productId={}", savedProduct.getId());

        return toResponse(savedProduct);
    }

    @Override
    @Cacheable(value = "products",key = "#id")
    public ProductResponse getProductById(Long id) {
        log.info("Fetching product from database. productId={}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        return toResponse(product);
    }

    @Override
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @CachePut(value = "products", key = "#id")
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setQuantityAvailable(request.quantityAvailable());

        Product updatedProduct = productRepository.save(product);

        log.info("Product updated successfully. productId={}", updatedProduct.getId());

        return toResponse(updatedProduct);
    }

    @Override
    @CacheEvict(value = "products", key = "#id")
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        productRepository.delete(product);

        log.info("Product deleted successfully. productId={}", id);
    }

    private ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getQuantityAvailable(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}
