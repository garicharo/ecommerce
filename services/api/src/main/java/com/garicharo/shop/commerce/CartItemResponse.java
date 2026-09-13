package com.garicharo.shop.commerce;

import java.math.BigDecimal;

public record CartItemResponse(
        String sku, int qty, String name, String description, BigDecimal price, int stock) {
}
