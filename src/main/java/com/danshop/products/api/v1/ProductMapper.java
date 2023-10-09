package com.danshop.products.api.v1;

import com.danshop.products.persistency.model.ProductEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    ProductDTO map(ProductEntity productEntity);

    ProductDTO map(ProductEntity productEntity, int quantity);

    ProductEntity map(ProductDTO productDTO);

    ProductEntity map(@MappingTarget ProductEntity productEntity, UpdateProductDTO updateProductDTO);
}
