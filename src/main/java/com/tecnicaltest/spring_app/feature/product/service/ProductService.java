package com.tecnicaltest.spring_app.feature.product.service;

import com.tecnicaltest.spring_app.dto.PaginationResponse;
import com.tecnicaltest.spring_app.feature.product.dto.ProductRequest;
import com.tecnicaltest.spring_app.feature.product.dto.ProductResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductService {
    PaginationResponse<ProductResponse> findAll(String search, Long categoryId, Pageable pageable);
    ProductResponse findById(Long id);
    ProductResponse create(ProductRequest request, List<String> imageUrls);
    ProductResponse update(Long id, ProductRequest request, List<String> imageUrls);
    void delete(Long id);
}
