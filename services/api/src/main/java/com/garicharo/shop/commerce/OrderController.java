package com.garicharo.shop.commerce;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.garicharo.shop.identity.CurrentUser;
import com.garicharo.shop.shared.PageResponse;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderQueryService orderQueryService;
    private final CurrentUser currentUser;

    public OrderController(OrderQueryService orderQueryService, CurrentUser currentUser) {
        this.orderQueryService = orderQueryService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public PageResponse<OrderResponse> list(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return orderQueryService.listMine(currentUser.require(authentication), page, size);
    }

    @GetMapping("/{id}")
    public OrderResponse get(Authentication authentication, @PathVariable UUID id) {
        return orderQueryService.getMine(currentUser.require(authentication), id);
    }
}
