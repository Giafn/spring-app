package com.tecnicaltest.spring_app.feature.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {
    @Schema(description = "Product ID", example = "1")
    private Long id;

    @Schema(description = "Product title", example = "Awesome T-Shirt")
    private String title;

    @Schema(description = "Product price", example = "99.99")
    private BigDecimal price;

    @Schema(description = "Product description", example = "High-quality cotton t-shirt")
    private String description;

    @Schema(description = "Category ID", example = "1")
    private Long categoryId;

    @Schema(description = "Category name", example = "Clothes")
    private String categoryName;

    @Schema(description = "Product image URLs")
    private List<String> images;

    @Schema(description = "ID of the user who created this product", example = "1")
    private Long createdById;

    @Schema(description = "Creation timestamp")
    private Instant createdAt;

    @Schema(description = "ID of the user who last updated this product", example = "1")
    private Long updatedById;

    @Schema(description = "Last update timestamp")
    private Instant updatedAt;
}
