package com.garicharo.shop.commerce;

import java.math.BigDecimal;

public record CartItemResponse(String sku, int qty, String name, BigDecimal price, int stock) {
}
