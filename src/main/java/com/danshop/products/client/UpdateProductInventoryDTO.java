package com.danshop.products.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;

import java.util.OptionalInt;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy;
import static java.util.OptionalInt.empty;

@Data
@Builder
@JsonInclude(NON_NULL)
@JsonNaming(SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class UpdateProductInventoryDTO {

    @Default
    private OptionalInt innerQuantity = empty();
    @Default
    private OptionalInt supplierQuantity = empty();

}
