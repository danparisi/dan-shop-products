package com.danshop.products.api.v1;

import com.danshop.products.service.ProductsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

import static com.danshop.products.api.v1.ProductsController.BASE_ENDPOINT_PRODUCTS;
import static org.springframework.http.ResponseEntity.noContent;
import static org.springframework.http.ResponseEntity.ok;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(BASE_ENDPOINT_PRODUCTS)
public class ProductsController {
    static final String BASE_ENDPOINT_PRODUCTS = "/v1/products";

    private final ProductsService productsService;

    @GetMapping("/{code}")
    public ResponseEntity<ProductDTO> get(@PathVariable String code) {
        log.info("Returning product [{}]", code);

        return productsService
                .find(code)
                .map(ResponseEntity::ok)
                .orElse(noContent().build());
    }

    @GetMapping
    public ResponseEntity<Collection<ProductDTO>> getAll() {
        log.info("Returning all products");

        return ok(productsService.findAll());
    }

}
