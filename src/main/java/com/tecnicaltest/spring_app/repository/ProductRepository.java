package com.tecnicaltest.spring_app.repository;

import com.tecnicaltest.spring_app.entity.Product;
import com.tecnicaltest.spring_app.feature.product.service.ProductSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    default Page<Product> findByFilters(String search, Long categoryId, Pageable pageable) {
        Specification<Product> specification = Specification.where(ProductSpecifications.search(search))
                .and(ProductSpecifications.categoryId(categoryId));
        return findAll(specification, pageable);
    }
}
