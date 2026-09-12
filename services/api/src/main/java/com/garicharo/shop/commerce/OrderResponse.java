package com.garicharo.shop.commerce;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        String status,
        BigDecimal total,
        OffsetDateTime createdAt,
        List<OrderItemResponse> items) {

    public static OrderResponse from(Order order) {
        List<OrderItemResponse> lines = order.getItems().stream()
                .map(item -> new OrderItemResponse(
                        item.getSku(),
                        item.getNameSnapshot(),
                        item.getQty(),
                        item.getUnitPrice(),
                        item.getLineTotal()))
                .toList();
        return new OrderResponse(
                order.getId(),
                order.getStatus().name(),
                order.getTotal(),
                order.getCreatedAt(),
                lines);
    }
}
