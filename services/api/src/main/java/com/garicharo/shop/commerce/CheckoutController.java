package com.garicharo.shop.commerce;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.garicharo.shop.identity.CurrentUser;
import com.garicharo.shop.shared.ApiException;

@RestController
@RequestMapping("/api/checkout")
public class CheckoutController {

    private final CheckoutService checkoutService;
    private final CurrentUser currentUser;

    public CheckoutController(CheckoutService checkoutService, CurrentUser currentUser) {
        this.checkoutService = checkoutService;
        this.currentUser = currentUser;
    }

    @PostMapping
    public ResponseEntity<CheckoutResponse> checkout(
            Authentication authentication,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey) {
        UUID key;
        try {
            key = UUID.fromString(idempotencyKey == null ? "" : idempotencyKey.trim());
        } catch (IllegalArgumentException ex) {
            throw new ApiException("INVALID_IDEMPOTENCY_KEY", "Idempotency-Key must be a UUID", HttpStatus.BAD_REQUEST);
        }
        CheckoutResult result = checkoutService.checkoutResult(currentUser.require(authentication), key);
        CheckoutResponse body = new CheckoutResponse(result.order().getId(), result.order().getStatus().name());
        if (result.replay()) {
            return ResponseEntity.ok(body);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }
}
