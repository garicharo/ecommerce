package com.garicharo.shop.commerce;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.garicharo.shop.identity.CurrentUser;
import com.garicharo.shop.shared.ApiException;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;
    private final CurrentUser currentUser;

    public CartController(CartService cartService, CurrentUser currentUser) {
        this.cartService = cartService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public CartResponse get(Authentication authentication) {
        return cartService.view(currentUser.require(authentication));
    }

    @PutMapping("/items")
    public CartResponse put(Authentication authentication, @RequestBody PutCartItemRequest body) {
        if (body == null || body.sku() == null || body.sku().isBlank()) {
            throw new ApiException("INVALID_SKU", "sku is required", HttpStatus.BAD_REQUEST);
        }
        return cartService.putItem(currentUser.require(authentication), body.sku().trim(), body.qty());
    }
}
