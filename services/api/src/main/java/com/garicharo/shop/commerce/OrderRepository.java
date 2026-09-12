package com.garicharo.shop.commerce;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    Optional<Order> findByUserIdAndIdempotencyKey(UUID userId, UUID idempotencyKey);

    Page<Order> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<Order> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
