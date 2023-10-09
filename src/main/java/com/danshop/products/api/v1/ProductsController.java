package com.danshop.products.api.v1;

import com.danshop.products.service.ProductsService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

import static com.danshop.products.api.v1.ProductsController.BASE_ENDPOINT_PRODUCTS;
import static com.danshop.products.api.v1.ProductsValidator.validateForCreation;
import static com.danshop.products.api.v1.ProductsValidator.validateForUpdate;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.ResponseEntity.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(BASE_ENDPOINT_PRODUCTS)
public class ProductsController {
    static final String BASE_ENDPOINT_PRODUCTS = "/v1/products";

    private final ProductsService productsService;

    @GetMapping("/{code}")
    public ResponseEntity<ProductDTO> get(@NotEmpty @PathVariable String code) {
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

    @PostMapping
    public ResponseEntity<ProductDTO> add(@RequestBody @Valid ProductDTO productDTO) {
        log.info("Adding product [{}]", productDTO);

        validateForCreation(productDTO);
        return status(CREATED).body(productsService.add(productDTO));
    }

    @PutMapping("/{code}")
    public ResponseEntity<ProductDTO> update(@NotEmpty @PathVariable String code, @RequestBody @Valid UpdateProductDTO updateProductDTO) {
        log.info("Updating product [{}]", updateProductDTO);

        validateForUpdate(code, updateProductDTO);
        return ok(productsService.update(code, updateProductDTO));
    }

    @DeleteMapping("/{code}")
    public ResponseEntity<Void> delete(@NotEmpty @PathVariable String code) {
        log.info("Deleting product [{}]", code);

        productsService.delete(code);
        return ok().build();
    }
}
