package com.garicharo.shop.commerce;

import java.util.UUID;

public record CheckoutResponse(UUID orderId, String status) {
}
