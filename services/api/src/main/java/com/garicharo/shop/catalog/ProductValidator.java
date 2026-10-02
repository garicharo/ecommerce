package com.garicharo.shop.catalog;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.http.HttpStatus;

import com.garicharo.shop.shared.ApiException;

/**
 * Shared field rules for manual product create/update. Mirrors {@link CsvRowValidator}
 * once values are typed (CSV still owns string parsing for dirty cells like {@code $29.99}).
 */
final class ProductValidator {

    private ProductValidator() {
    }

    static void validateCreate(Product product) {
        if (product == null) {
            throw new ApiException("INVALID_BODY", "Product body is required", HttpStatus.BAD_REQUEST);
        }
        requireSku(product.getSku());
        validateSharedFields(product);
    }

    static void validateUpdate(Product product) {
        if (product == null) {
            throw new ApiException("INVALID_BODY", "Product body is required", HttpStatus.BAD_REQUEST);
        }
        validateSharedFields(product);
    }

    static void normalize(Product product) {
        if (product.getSku() != null) {
            product.setSku(product.getSku().trim());
        }
        if (product.getName() != null) {
            product.setName(product.getName().trim());
        }
        if (product.getDescription() != null) {
            product.setDescription(product.getDescription().trim());
        }
        if (product.getCategory() != null) {
            product.setCategory(product.getCategory().trim());
        }
        if (product.getPrice() != null) {
            product.setPrice(product.getPrice().setScale(2, RoundingMode.HALF_UP));
        }
    }

    private static void validateSharedFields(Product product) {
        if (blank(product.getName())) {
            throw new ApiException("EMPTY_NAME", "Name is required", HttpStatus.BAD_REQUEST);
        }
        if (blank(product.getDescription())) {
            throw new ApiException("EMPTY_DESCRIPTION", "Description is required", HttpStatus.BAD_REQUEST);
        }
        if (blank(product.getCategory())) {
            throw new ApiException("EMPTY_CATEGORY", "Category is required", HttpStatus.BAD_REQUEST);
        }
        if (product.getPrice() == null) {
            throw new ApiException("INVALID_PRICE", "Price is required", HttpStatus.BAD_REQUEST);
        }
        if (product.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new ApiException("INVALID_PRICE", "Price must be a decimal >= 0", HttpStatus.BAD_REQUEST);
        }
        if (product.getStock() == null) {
            throw new ApiException("INVALID_STOCK", "Stock must be an integer >= 0", HttpStatus.BAD_REQUEST);
        }
        if (product.getStock() < 0) {
            throw new ApiException("NEGATIVE_STOCK", "Stock must be >= 0", HttpStatus.BAD_REQUEST);
        }
        if (product.getWeightKg() == null) {
            throw new ApiException("EMPTY_WEIGHT", "Weight is required", HttpStatus.BAD_REQUEST);
        }
        if (product.getWeightKg().compareTo(BigDecimal.ZERO) < 0) {
            throw new ApiException("INVALID_WEIGHT", "Weight must be a decimal >= 0", HttpStatus.BAD_REQUEST);
        }
    }

    private static void requireSku(String sku) {
        if (blank(sku)) {
            throw new ApiException("EMPTY_SKU", "SKU is required", HttpStatus.BAD_REQUEST);
        }
    }

    private static boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
