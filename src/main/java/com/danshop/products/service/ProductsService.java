package com.danshop.products.service;

import com.danshop.products.api.v1.ProductDTO;
import com.danshop.products.api.v1.ProductMapper;
import com.danshop.products.api.v1.UpdateProductDTO;
import com.danshop.products.client.InventoryClient;
import com.danshop.products.client.ProductInventoryDTO;
import com.danshop.products.client.UpdateProductInventoryDTO;
import com.danshop.products.persistency.model.ProductEntity;
import com.danshop.products.persistency.repository.ProductRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Optional;
import java.util.OptionalInt;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.stream.Collectors.toSet;
import static org.hibernate.type.descriptor.java.IntegerJavaType.ZERO;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ProductsService {
    private final ProductMapper productMapper;
    private final InventoryClient inventoryClient;
    private final ProductRepository productRepository;

    public ProductDTO add(@NonNull ProductDTO productDTO) {
        final ProductEntity storedProduct = productRepository
                .save(productMapper
                        .map(productDTO));
        final ProductDTO storedProductDTO = productMapper.map(storedProduct);

        updateInventoryQuantity(storedProduct.getCode(), productDTO.getQuantity())
                .ifPresent(inventory -> storedProductDTO.setQuantity(inventory.getInnerQuantity()));

        return storedProductDTO;
    }

    public ProductDTO update(@NotNull String code, @NotNull UpdateProductDTO updateProductDTO) {
        final ProductEntity existingProduct = retrieveMandatoryByCode(code);
        final ProductEntity storedProduct = productRepository.save(productMapper.map(existingProduct, updateProductDTO));

        Integer quantity = updateInventoryQuantity(storedProduct.getCode(), updateProductDTO.getQuantity())
                .map(ProductInventoryDTO::getInnerQuantity)
                .orElse(ZERO);

        return productMapper.map(
                storedProduct, quantity);
    }

    public void delete(String code) {
        deleteInventory(code);
        productRepository.deleteByCode(code);
    }

    public Optional<ProductDTO> find(@NonNull String code) {
        return productRepository
                .findByCode(code)
                .map(productMapper::map)
                .map(this::addInventoryQuantity);
    }

    public Collection<ProductDTO> findAll() {
        return productRepository
                .findAll()
                .stream()
                .map(productMapper::map)
                .map(this::addInventoryQuantity)
                .collect(toSet());
    }

    private Optional<ProductInventoryDTO> updateInventoryQuantity(String code, int quantity) {
        try {
            return inventoryClient
                    .updateProductInventory(code, UpdateProductInventoryDTO.builder()
                            .innerQuantity(OptionalInt.of(quantity)).build());
        } catch (Exception e) {
            log.error(format("Could not update product [%s] (inner quantity = [%s]) against inventory service", code, quantity), e);
            return empty();
        }
    }

    private void deleteInventory(String code) {
        try {
            inventoryClient.deleteProductInventory(code);
        } catch (Exception e) {
            log.error(format("Could not delete product [%s] against inventory service", code), e);
        }
    }

    private ProductEntity retrieveMandatoryByCode(String code) {
        return productRepository
                .findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException(format("Expected Product with code [%s] not found", code)));
    }

    private ProductDTO addInventoryQuantity(ProductDTO product) {
        inventoryClient.
                getProductInventory(product.getCode())
                .ifPresent(inventory -> product.setQuantity(inventory.getInnerQuantity()));

        return product;
    }
}
