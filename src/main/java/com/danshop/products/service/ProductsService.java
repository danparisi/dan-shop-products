package com.danshop.products.service;

import com.danshop.products.api.v1.ProductDTO;
import com.danshop.products.client.InventoryClient;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toSet;

@Service
@RequiredArgsConstructor
public class ProductsService {
    private final Map<String, ProductDTO> PRODUCTS = new ConcurrentHashMap<>();

    private final InventoryClient inventoryClient;

    @PostConstruct
    public void init() {
        PRODUCTS.put("123", ProductDTO.builder().code("123").name("Product 1").build());
        PRODUCTS.put("456", ProductDTO.builder().code("456").name("Product 2").build());
        PRODUCTS.put("789", ProductDTO.builder().code("789").name("Product 3").build());
    }

    public Optional<ProductDTO> find(@NonNull String code) {
        return ofNullable(PRODUCTS.get(code))
                .map(this::addInventoryQuantity);
    }

    public Collection<ProductDTO> findAll() {
        return PRODUCTS
                .values()
                .stream()
                .map(this::addInventoryQuantity)
                .collect(toSet());
    }

    private ProductDTO addInventoryQuantity(ProductDTO product) {
        inventoryClient.
                getProductInventory(product.getCode())
                .ifPresent(inventory -> product.setQuantity(inventory.getInnerQuantity()));

        return product;
    }

}
