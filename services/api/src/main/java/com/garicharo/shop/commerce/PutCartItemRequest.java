package com.garicharo.shop.commerce;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record PutCartItemRequest(
        @NotBlank(message = "sku is required") String sku,
        @Min(value = 0, message = "qty must be >= 0") int qty) {
}
