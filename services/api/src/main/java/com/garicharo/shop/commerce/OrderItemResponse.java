package com.garicharo.shop.commerce;

import java.math.BigDecimal;

public record OrderItemResponse(
        String sku,
        String name,
        int qty,
        BigDecimal unitPrice,
        BigDecimal lineTotal) {
}
