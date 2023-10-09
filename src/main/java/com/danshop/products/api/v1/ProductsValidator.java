package com.danshop.products.api.v1;

import jakarta.validation.ValidationException;
import lombok.NoArgsConstructor;

import java.util.function.Supplier;

import static java.lang.Boolean.FALSE;
import static lombok.AccessLevel.PRIVATE;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

@NoArgsConstructor(access = PRIVATE)
public class ProductsValidator {
    private static final String VALIDATION_MESSAGE_MANDATORY_PRODUCT_CODE = "Product code is mandatory";
    private static final String VALIDATION_MESSAGE_MANDATORY_PRODUCT_NAME = "Product name is mandatory";

    public static void validateForUpdate(String code, UpdateProductDTO updateProductDTO) {
        verifyCondition(() -> isNotEmpty(code), VALIDATION_MESSAGE_MANDATORY_PRODUCT_CODE);
        verifyCondition(() -> isNotEmpty(updateProductDTO.getName()), VALIDATION_MESSAGE_MANDATORY_PRODUCT_NAME);
    }

    public static void validateForCreation(ProductDTO productDTO) {
        verifyCondition(() -> isNotEmpty(productDTO.getCode()), VALIDATION_MESSAGE_MANDATORY_PRODUCT_CODE);
        verifyCondition(() -> isNotEmpty(productDTO.getName()), VALIDATION_MESSAGE_MANDATORY_PRODUCT_NAME);
    }

    private static void verifyCondition(Supplier<Boolean> condition, String message) {
        if (condition.get() == FALSE) {
            throw new ValidationException(message);
        }
    }
}
