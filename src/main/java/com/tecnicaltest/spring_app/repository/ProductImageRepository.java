package com.tecnicaltest.spring_app.repository;

import com.tecnicaltest.spring_app.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
}

