package com.garicharo.shop.commerce;

import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.garicharo.shop.shared.PageResponse;

@RestController
@RequestMapping("/api/admin/orders")
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {

    private final OrderQueryService orderQueryService;

    public AdminOrderController(OrderQueryService orderQueryService) {
        this.orderQueryService = orderQueryService;
    }

    @GetMapping
    public PageResponse<OrderResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return orderQueryService.listAll(page, size);
    }

    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable UUID id) {
        return orderQueryService.getAny(id);
    }
}
