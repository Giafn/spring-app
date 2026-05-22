package com.tecnicaltest.spring_app.feature.product.controller;

import com.tecnicaltest.spring_app.dto.ApiResponse;
import com.tecnicaltest.spring_app.feature.product.dto.CategoryResponse;
import com.tecnicaltest.spring_app.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@Tag(name = "Products Category", description = "Product Category only for reference, used in product creation and filtering. No management API provided.")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryRepository categoryRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> list() {
        List<CategoryResponse> categories = categoryRepository.findAll().stream()
                .map(category -> CategoryResponse.builder()
                        .id(category.getId())
                        .name(category.getName())
                        .build())
                .toList();
        return ResponseEntity.ok(ApiResponse.success(categories));
    }
}
