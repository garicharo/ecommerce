package com.garicharo.shop.catalog;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

final class CsvRowValidator {

    private static final List<String> REQUIRED = List.of(
            "name", "sku", "description", "category", "price", "stock", "weight_kg");

    record Failure(String code, String message) {
    }

    record ValidRow(
            String name,
            String sku,
            String description,
            String category,
            BigDecimal price,
            int stock,
            BigDecimal weightKg) {
    }

    private CsvRowValidator() {
    }

    static List<String> requiredColumns() {
        return REQUIRED;
    }

    static boolean isEmptyRow(String name, String sku, String description, String category, String price,
            String stock, String weightKg) {
        return blank(name) && blank(sku) && blank(description) && blank(category) && blank(price) && blank(stock)
                && blank(weightKg);
    }

    static Failure validate(String name, String sku, String description, String category, String price, String stock,
            String weightKg) {
        if (blank(name)) {
            return new Failure("EMPTY_NAME", "Name is required");
        }
        if (blank(sku)) {
            return new Failure("EMPTY_SKU", "SKU is required");
        }
        if (blank(description)) {
            return new Failure("EMPTY_DESCRIPTION", "Description is required");
        }
        if (blank(category)) {
            return new Failure("EMPTY_CATEGORY", "Category is required");
        }
        if (blank(price)) {
            return new Failure("INVALID_PRICE", "Price is required");
        }
        String priceRaw = price.trim();
        if (priceRaw.contains("$") || "free".equalsIgnoreCase(priceRaw)) {
            return new Failure("INVALID_PRICE", "Price must be a decimal >= 0 (got \"" + priceRaw + "\")");
        }
        BigDecimal parsedPrice;
        try {
            parsedPrice = new BigDecimal(priceRaw);
        } catch (NumberFormatException ex) {
            return new Failure("INVALID_PRICE", "Price must be a decimal >= 0 (got \"" + priceRaw + "\")");
        }
        if (parsedPrice.scale() > 2 && parsedPrice.stripTrailingZeros().scale() > 2) {
            parsedPrice = parsedPrice.setScale(2, RoundingMode.HALF_UP);
        }
        if (parsedPrice.compareTo(BigDecimal.ZERO) < 0) {
            return new Failure("INVALID_PRICE", "Price must be a decimal >= 0 (got \"" + priceRaw + "\")");
        }
        if (blank(stock)) {
            return new Failure("INVALID_STOCK", "Stock must be an integer >= 0");
        }
        String stockRaw = stock.trim();
        if (!stockRaw.matches("-?\\d+")) {
            return new Failure("INVALID_STOCK", "Stock must be an integer >= 0 (got \"" + stockRaw + "\")");
        }
        int parsedStock;
        try {
            parsedStock = Integer.parseInt(stockRaw);
        } catch (NumberFormatException ex) {
            return new Failure("INVALID_STOCK", "Stock must be an integer >= 0 (got \"" + stockRaw + "\")");
        }
        if (parsedStock < 0) {
            return new Failure("NEGATIVE_STOCK", "Stock must be >= 0 (got \"" + stockRaw + "\")");
        }
        if (blank(weightKg)) {
            return new Failure("EMPTY_WEIGHT", "Weight is required");
        }
        String weightRaw = weightKg.trim();
        BigDecimal parsedWeight;
        try {
            parsedWeight = new BigDecimal(weightRaw);
        } catch (NumberFormatException ex) {
            return new Failure("INVALID_WEIGHT", "Weight must be a decimal >= 0 (got \"" + weightRaw + "\")");
        }
        if (parsedWeight.compareTo(BigDecimal.ZERO) < 0) {
            return new Failure("INVALID_WEIGHT", "Weight must be a decimal >= 0 (got \"" + weightRaw + "\")");
        }
        return null;
    }

    static ValidRow parse(String name, String sku, String description, String category, String price, String stock,
            String weightKg) {
        return new ValidRow(
                name.trim(),
                sku.trim(),
                description.trim(),
                category.trim(),
                new BigDecimal(price.trim()).setScale(2, RoundingMode.HALF_UP),
                Integer.parseInt(stock.trim()),
                new BigDecimal(weightKg.trim()));
    }

    static String blankToNull(String value) {
        return blank(value) ? null : value.trim();
    }

    static boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
