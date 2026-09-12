package com.garicharo.shop.commerce;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.garicharo.shop.identity.User;
import com.garicharo.shop.shared.ApiException;
import com.garicharo.shop.shared.PageResponse;

@Service
public class OrderQueryService {

    private final OrderRepository orderRepository;

    public OrderQueryService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> listMine(User user, int page, int size) {
        Page<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(
                user.getId(), PageRequest.of(Math.max(page, 0), clamp(size)));
        orders.forEach(order -> order.getItems().size());
        return PageResponse.of(orders.map(OrderResponse::from));
    }

    @Transactional(readOnly = true)
    public OrderResponse getMine(User user, UUID id) {
        Order order = orderRepository.findById(id)
                .filter(found -> found.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Order not found", HttpStatus.NOT_FOUND));
        order.getItems().size();
        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> listAll(int page, int size) {
        Page<Order> orders = orderRepository.findAllByOrderByCreatedAtDesc(
                PageRequest.of(Math.max(page, 0), clamp(size)));
        orders.forEach(order -> order.getItems().size());
        return PageResponse.of(orders.map(OrderResponse::from));
    }

    @Transactional(readOnly = true)
    public OrderResponse getAny(UUID id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Order not found", HttpStatus.NOT_FOUND));
        order.getItems().size();
        return OrderResponse.from(order);
    }

    private static int clamp(int size) {
        if (size <= 0) {
            return 20;
        }
        return Math.min(size, 50);
    }
}
