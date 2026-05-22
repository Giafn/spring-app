package com.tecnicaltest.spring_app.feature.product.service;

import com.tecnicaltest.spring_app.dto.PaginationResponse;
import com.tecnicaltest.spring_app.entity.Category;
import com.tecnicaltest.spring_app.entity.Product;
import com.tecnicaltest.spring_app.entity.User;
import com.tecnicaltest.spring_app.exception.ResourceNotFoundException;
import com.tecnicaltest.spring_app.feature.product.dto.ProductRequest;
import com.tecnicaltest.spring_app.feature.product.dto.ProductResponse;
import com.tecnicaltest.spring_app.feature.product.service.impl.ProductServiceImpl;
import com.tecnicaltest.spring_app.repository.CategoryRepository;
import com.tecnicaltest.spring_app.repository.ProductRepository;
import com.tecnicaltest.spring_app.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.domain.Specification;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ProductServiceImplTest {

    private ProductRepository productRepository;
    private UserRepository userRepository;
    private CategoryRepository categoryRepository;
    private ProductServiceImpl service;

    private final Category clothesCategory = Category.builder().id(1L).name("Clothes").build();
    private final Category gadgetsCategory = Category.builder().id(2L).name("Gadgets").build();

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        userRepository = mock(UserRepository.class);
        categoryRepository = mock(CategoryRepository.class);
        service = new ProductServiceImpl(productRepository, userRepository, categoryRepository);

        // set authenticated user in SecurityContext
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", null)
        );
    }

    @Test
    void findAllShouldReturnPagination() {
        User creator = User.builder().id(2L).username("creator").name("Creator").build();
        Product p1 = Product.builder().id(1L).title("T1").price(BigDecimal.valueOf(10)).category(clothesCategory)
                .images(List.of(com.tecnicaltest.spring_app.entity.ProductImage.builder().url("img1").position(0).build()))
                .createdBy(creator)
                .updatedBy(creator)
                .build();
        Page<Product> page = new PageImpl<>(List.of(p1), PageRequest.of(0, 10), 1);

        when(productRepository.findAll(any(Specification.class), eq(PageRequest.of(0,10)))).thenReturn(page);

        PaginationResponse<ProductResponse> res = service.findAll(null, null, PageRequest.of(0,10));

        assertEquals(1, res.getContent().size());
        assertEquals("T1", res.getContent().get(0).getTitle());
    }

    @Test
    void findByIdShouldReturnProduct() {
        User creator2 = User.builder().id(3L).username("creator2").name("Creator2").build();
        Product p = Product.builder().id(2L).title("P").price(BigDecimal.valueOf(5)).category(gadgetsCategory)
                .images(List.of(com.tecnicaltest.spring_app.entity.ProductImage.builder().url("i").position(0).build()))
                .createdBy(creator2)
                .updatedBy(creator2)
                .build();
        when(productRepository.findById(2L)).thenReturn(Optional.of(p));

        ProductResponse resp = service.findById(2L);

        assertEquals(2L, resp.getId());
        assertEquals("P", resp.getTitle());
    }

    @Test
    void findByIdMissingShouldThrow() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.findById(99L));
    }

    @Test
    void createShouldSaveProductWithCurrentUser() {
        User user = User.builder().id(10L).username("admin").name("Admin").build();
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(clothesCategory));

        ProductRequest req = ProductRequest.builder()
                .title("New")
                .price(BigDecimal.valueOf(20))
                .categoryId(1L)
                .build();

        List<String> imageUrls = List.of("/uploads/img.jpg");

        when(productRepository.save(any())).thenAnswer(inv -> {
            Product arg = inv.getArgument(0);
            arg.setId(5L);
            return arg;
        });

        var resp = service.create(req, imageUrls);

        assertEquals(5L, resp.getId());
        assertEquals("New", resp.getTitle());
        assertEquals(10L, resp.getCreatedById());
    }

    @Test
    void updateShouldModifyAndSave() {
        User user = User.builder().id(11L).username("admin").name("Admin").build();
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(clothesCategory));

        User owner = User.builder().id(2L).username("owner").name("Owner").build();
        var existingImages = new ArrayList<com.tecnicaltest.spring_app.entity.ProductImage>();
        existingImages.add(com.tecnicaltest.spring_app.entity.ProductImage.builder().url("a").position(0).build());
        Product existing = Product.builder().id(7L).title("Old").price(BigDecimal.valueOf(1)).category(clothesCategory)
                .images(existingImages)
                .createdBy(owner).updatedBy(owner).build();
        when(productRepository.findById(7L)).thenReturn(Optional.of(existing));

        ProductRequest req = ProductRequest.builder().title("Updated").price(BigDecimal.valueOf(2)).categoryId(1L).build();
        List<String> imageUrls = List.of("/uploads/new.jpg");
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var resp = service.update(7L, req, imageUrls);

        assertEquals("Updated", resp.getTitle());
        assertEquals(11L, resp.getUpdatedById());
    }

    @Test
    void findAllWithSearchFilter() {
        User creator = User.builder().id(2L).username("creator").name("Creator").build();
        Product p = Product.builder().id(1L).title("Awesome Shirt").price(BigDecimal.valueOf(10)).category(clothesCategory)
                .images(List.of(com.tecnicaltest.spring_app.entity.ProductImage.builder().url("i1").position(0).build()))
                .createdBy(creator).updatedBy(creator).build();
        Page<Product> page = new PageImpl<>(List.of(p), PageRequest.of(0, 10), 1);

        when(productRepository.findAll(any(Specification.class), eq(PageRequest.of(0, 10)))).thenReturn(page);

        var res = service.findAll("Awesome", null, PageRequest.of(0, 10));

        assertEquals(1, res.getContent().size());
        assertEquals("Awesome Shirt", res.getContent().get(0).getTitle());
    }

    @Test
    void findAllWithCategoryFilter() {
        User creator = User.builder().id(2L).username("creator").name("Creator").build();
        Product p = Product.builder().id(1L).title("T1").price(BigDecimal.valueOf(10)).category(clothesCategory)
                .images(List.of(com.tecnicaltest.spring_app.entity.ProductImage.builder().url("i1").position(0).build()))
                .createdBy(creator).updatedBy(creator).build();
        Page<Product> page = new PageImpl<>(List.of(p), PageRequest.of(0, 10), 1);

        when(productRepository.findAll(any(Specification.class), eq(PageRequest.of(0, 10)))).thenReturn(page);

        var res = service.findAll(null, 1L, PageRequest.of(0, 10));

        assertEquals(1, res.getContent().size());
        assertEquals(clothesCategory.getId(), res.getContent().get(0).getCategoryId());
    }

    @Test
    void findAllWithSearchAndCategoryFilter() {
        User creator = User.builder().id(2L).username("creator").name("Creator").build();
        Product p = Product.builder().id(1L).title("Pagination Test 1").price(BigDecimal.valueOf(10)).category(clothesCategory)
                .images(List.of(com.tecnicaltest.spring_app.entity.ProductImage.builder().url("i1").position(0).build()))
                .createdBy(creator).updatedBy(creator).build();
        Page<Product> page = new PageImpl<>(List.of(p), PageRequest.of(0, 10), 1);

        when(productRepository.findAll(any(Specification.class), eq(PageRequest.of(0, 10)))).thenReturn(page);

        var res = service.findAll("Pagination", 1L, PageRequest.of(0, 10));

        assertEquals(1, res.getContent().size());
        assertEquals("Pagination Test 1", res.getContent().get(0).getTitle());
        assertEquals(1L, res.getContent().get(0).getCategoryId());
    }

    @Test
    void createShouldThrowWhenCategoryNotFound() {
        User user = User.builder().id(10L).username("admin").name("Admin").build();
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        ProductRequest req = ProductRequest.builder().title("X").price(BigDecimal.ONE).categoryId(999L).build();

        assertThrows(ResourceNotFoundException.class, () -> service.create(req, List.of("/uploads/i.jpg")));
    }

    @Test
    void updateShouldThrowWhenNotFound() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        ProductRequest req = ProductRequest.builder().title("X").price(BigDecimal.ONE).categoryId(1L).build();

        assertThrows(ResourceNotFoundException.class, () -> service.update(999L, req, List.of("/uploads/i.jpg")));
    }

    @Test
    void deleteShouldRemoveOrThrow() {
        when(productRepository.existsById(3L)).thenReturn(true);
        service.delete(3L);
        verify(productRepository).deleteById(3L);

        when(productRepository.existsById(99L)).thenReturn(false);
        assertThrows(ResourceNotFoundException.class, () -> service.delete(99L));
    }
}

