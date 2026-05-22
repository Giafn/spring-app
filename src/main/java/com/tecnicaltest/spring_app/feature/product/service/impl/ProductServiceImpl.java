package com.tecnicaltest.spring_app.feature.product.service.impl;

import com.tecnicaltest.spring_app.dto.PaginationResponse;
import com.tecnicaltest.spring_app.entity.Category;
import com.tecnicaltest.spring_app.entity.Product;
import com.tecnicaltest.spring_app.entity.User;
import com.tecnicaltest.spring_app.exception.ResourceNotFoundException;
import com.tecnicaltest.spring_app.feature.product.dto.ProductRequest;
import com.tecnicaltest.spring_app.feature.product.dto.ProductResponse;
import com.tecnicaltest.spring_app.feature.product.service.ProductService;
import com.tecnicaltest.spring_app.repository.CategoryRepository;
import com.tecnicaltest.spring_app.repository.ProductRepository;
import com.tecnicaltest.spring_app.repository.UserRepository;
import com.tecnicaltest.spring_app.feature.product.service.ProductSpecifications;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    @Value("${app.base-url:}")
    private String baseUrl;

    @Override
    @Cacheable(value = "products", key = "T(java.util.Objects).hash(#search, #categoryId, #pageable.pageNumber, #pageable.pageSize, #pageable.sort.toString())", unless = "#result.content.isEmpty()")
    public PaginationResponse<ProductResponse> findAll(String search, Long categoryId, Pageable pageable) {
        Specification<Product> specification = Specification.where(ProductSpecifications.search(search))
                .and(ProductSpecifications.categoryId(categoryId));
        Page<Product> page = productRepository.findAll(specification, pageable);
        return PaginationResponse.from(page.map(this::toResponse));
    }

    @Override
    @Cacheable(value = "products", key = "#id")
    public ProductResponse findById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return toResponse(product);
    }

    @Override
    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public ProductResponse create(ProductRequest request, List<String> imageUrls) {
        User currentUser = getCurrentUser();
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.getCategoryId()));
        Product product = Product.builder()
                .title(request.getTitle())
                .price(request.getPrice())
                .description(request.getDescription())
                .category(category)
                .createdBy(currentUser)
                .updatedBy(currentUser)
                .build();

        var images = new java.util.ArrayList<com.tecnicaltest.spring_app.entity.ProductImage>();
        var urls = imageUrls == null ? java.util.List.<String>of() : imageUrls;
        for (int i = 0; i < urls.size(); i++) {
            images.add(com.tecnicaltest.spring_app.entity.ProductImage.builder()
                    .product(product)
                    .url(urls.get(i))
                    .position(i)
                    .build());
        }
        product.setImages(images);

        Product saved = productRepository.save(product);
        return toResponse(saved);
    }

    @Override
    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public ProductResponse update(Long id, ProductRequest request, List<String> imageUrls) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        User currentUser = getCurrentUser();
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.getCategoryId()));
        existing.setTitle(request.getTitle());
        existing.setPrice(request.getPrice());
        existing.setDescription(request.getDescription());
        existing.setCategory(category);
        var urls = imageUrls == null ? java.util.List.<String>of() : imageUrls;
        var newImages = new java.util.ArrayList<com.tecnicaltest.spring_app.entity.ProductImage>();
        for (int i = 0; i < urls.size(); i++) {
            newImages.add(com.tecnicaltest.spring_app.entity.ProductImage.builder()
                    .product(existing)
                    .url(urls.get(i))
                    .position(i)
                    .build());
        }
        existing.getImages().clear();
        existing.getImages().addAll(newImages);
        existing.setUpdatedBy(currentUser);
        existing.setUpdatedAt(java.time.Instant.now());
        Product saved = productRepository.save(existing);
        return toResponse(saved);
    }

    @Override
    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public void delete(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product not found with id: " + id);
        }
        productRepository.deleteById(id);
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    private ProductResponse toResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .title(product.getTitle())
                .price(product.getPrice())
                .description(product.getDescription())
                .categoryId(product.getCategory().getId())
                .categoryName(product.getCategory().getName())
                .images(product.getImages().stream()
                        .sorted(java.util.Comparator.comparingInt(com.tecnicaltest.spring_app.entity.ProductImage::getPosition))
                        .map(img -> baseUrl + img.getUrl())
                        .toList())
                .createdById(product.getCreatedBy() != null ? product.getCreatedBy().getId() : null)
                .createdAt(product.getCreatedAt())
                .updatedById(product.getUpdatedBy() != null ? product.getUpdatedBy().getId() : null)
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
