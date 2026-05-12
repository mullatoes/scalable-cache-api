package com.jdevs.scalablecacheapi.repository;

import com.jdevs.scalablecacheapi.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
