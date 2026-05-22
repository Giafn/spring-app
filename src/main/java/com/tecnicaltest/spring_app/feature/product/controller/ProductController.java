package com.tecnicaltest.spring_app.feature.product.controller;

import com.tecnicaltest.spring_app.config.RateLimit;
import com.tecnicaltest.spring_app.dto.ApiResponse;
import com.tecnicaltest.spring_app.dto.PaginationResponse;
import com.tecnicaltest.spring_app.feature.product.dto.ProductRequest;
import com.tecnicaltest.spring_app.feature.product.dto.ProductResponse;
import com.tecnicaltest.spring_app.feature.product.service.ProductService;
import com.tecnicaltest.spring_app.utils.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Product management APIs")
@Validated
public class ProductController {
    private final ProductService productService;
    private final FileStorageService fileStorageService;
    private final Validator validator;

    @Operation(summary = "List products", description = "Get paginated list of products with optional search and category filter")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Products retrieved successfully")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<PaginationResponse<ProductResponse>>> list(
            @RequestParam(required = false)
            @Parameter(description = "Search keyword (matches product title)")
            String search,
            @RequestParam(required = false)
            @Parameter(description = "Category ID, use GET /api/categories to get available options")
            Long categoryId,
            @ParameterObject @PageableDefault(size = 10) Pageable pageable) {
        PaginationResponse<ProductResponse> products = productService.findAll(search, categoryId, pageable);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @Operation(summary = "Get product by ID", description = "Retrieve a single product by its ID")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> get(@PathVariable @Parameter(description = "Product ID") Long id) {
        ProductResponse product = productService.findById(id);
        return ResponseEntity.ok(ApiResponse.success(product));
    }

    @Operation(summary = "Create product", description = "Create a new product with image uploads")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Product created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed")
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RateLimit(maxRequests = 1, windowSeconds = 5)
    public ResponseEntity<ApiResponse<ProductResponse>> create(
            @RequestParam @Parameter(description = "Product title") String title,
            @RequestParam @Parameter(description = "Product price") BigDecimal price,
            @RequestParam(required = false) @Parameter(description = "Product description") String description,
            @RequestParam @Parameter(description = "Category ID") Long categoryId,
            @RequestPart(value = "images", required = false) MultipartFile[] images) {
        if (images == null || images.length == 0) {
            throw new IllegalArgumentException("Multipart part 'images' is required");
        }
        ProductRequest request = buildRequest(title, price, description, categoryId);
        List<String> urls = saveImages(images);
        ProductResponse product = productService.create(request, urls);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Product created successfully", product));
    }

    @Operation(summary = "Update product", description = "Update an existing product with image uploads")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed")
    })
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RateLimit(maxRequests = 1, windowSeconds = 5)
    public ResponseEntity<ApiResponse<ProductResponse>> update(
            @PathVariable @Parameter(description = "Product ID") Long id,
            @RequestParam @Parameter(description = "Product title") String title,
            @RequestParam @Parameter(description = "Product price") BigDecimal price,
            @RequestParam(required = false) @Parameter(description = "Product description") String description,
            @RequestParam @Parameter(description = "Category ID") Long categoryId,
            @RequestPart(value = "images", required = false) MultipartFile[] images) {
        if (images == null || images.length == 0) {
            throw new IllegalArgumentException("Multipart part 'images' is required");
        }
        ProductRequest request = buildRequest(title, price, description, categoryId);
        List<String> urls = saveImages(images);
        ProductResponse productResponse = productService.update(id, request, urls);
        return ResponseEntity.ok(ApiResponse.success("Product updated successfully", productResponse));
    }

    @Operation(summary = "Delete product", description = "Delete a product by its ID")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product deleted successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found")
    })
    @DeleteMapping("/{id}")
    @RateLimit(maxRequests = 1, windowSeconds = 5)
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable @Parameter(description = "Product ID") Long id) {
        productService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Product deleted successfully", null));
    }

    private List<String> saveImages(MultipartFile[] images) {
        List<String> urls = new ArrayList<>();
        if (images == null) {
            return urls;
        }
        for (MultipartFile file : images) {
            if (!file.isEmpty()) {
                urls.add(fileStorageService.store(file));
            }
        }
        return urls;
    }

    private ProductRequest buildRequest(String title, BigDecimal price, String description, Long categoryId) {
        ProductRequest request = ProductRequest.builder()
                .title(title)
                .price(price)
                .description(description)
                .categoryId(categoryId)
                .build();
        var violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new com.tecnicaltest.spring_app.exception.ProductValidationException(
                    violations.stream().collect(java.util.stream.Collectors.toMap(
                            violation -> violation.getPropertyPath().toString(),
                            jakarta.validation.ConstraintViolation::getMessage,
                            (left, right) -> left,
                            java.util.LinkedHashMap::new
                    )));
        }
        return request;
    }
}
