package com.garicharo.shop.commerce;

import java.util.List;

public record CartResponse(List<CartItemResponse> items) {
}
