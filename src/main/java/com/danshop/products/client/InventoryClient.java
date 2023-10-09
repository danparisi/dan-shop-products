package com.danshop.products.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@FeignClient("dan-shop-inventory-service")
public interface InventoryClient {

    @GetMapping("v1/products/{code}")
    Optional<ProductInventoryDTO> getProductInventory(@PathVariable String code);

    @PutMapping("v1/products/{code}")
    Optional<ProductInventoryDTO> updateProductInventory(@PathVariable String code, @RequestBody UpdateProductInventoryDTO updateProductInventoryDTO);

    @DeleteMapping("v1/products/{code}")
    void deleteProductInventory(@PathVariable String code);

}
