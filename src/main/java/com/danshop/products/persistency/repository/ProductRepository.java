package com.danshop.products.persistency.repository;

import com.danshop.products.persistency.model.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<ProductEntity, Integer> {

    Optional<ProductEntity> findByCode(String code);

    void deleteByCode(String code);
}
