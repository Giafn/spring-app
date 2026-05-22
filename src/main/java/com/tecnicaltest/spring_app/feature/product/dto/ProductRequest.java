package com.tecnicaltest.spring_app.feature.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRequest {
    @Schema(description = "Product title", example = "Awesome T-Shirt")
    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    @Schema(description = "Product price", example = "99.99")
    @NotNull(message = "Price is required")
    @Positive(message = "Price must be positive")
    private BigDecimal price;

    @Schema(description = "Product description", example = "High-quality cotton t-shirt")
    private String description;

    @Schema(description = "Category ID, use GET /api/categories to get available options", example = "1")
    @NotNull(message = "Category is required")
    private Long categoryId;
}
