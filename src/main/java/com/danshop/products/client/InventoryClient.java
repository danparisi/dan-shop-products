package com.danshop.products.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Optional;

@FeignClient("dan-shop-inventory-service")
public interface InventoryClient {

    @GetMapping("v1/products/{code}")
    Optional<ProductInventoryDTO> getProductInventory(@PathVariable String code);

}
