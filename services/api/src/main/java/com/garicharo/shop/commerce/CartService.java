package com.garicharo.shop.commerce;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;

import com.garicharo.shop.identity.User;
import com.garicharo.shop.catalog.Product;
import com.garicharo.shop.catalog.ProductRepository;
import com.garicharo.shop.shared.ApiException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;

    public CartService(CartRepository cartRepository, ProductRepository productRepository) {
        this.productRepository = productRepository;
        this.cartRepository = cartRepository;
    }

    private Cart createCart(User user) {
        Optional<Cart> existingCart = cartRepository.findByUserId(user.getId());
        if (existingCart.isPresent()) {
            return existingCart.get();
        }
        return cartRepository.save(new Cart(user, OffsetDateTime.now(ZoneOffset.UTC)));
    }

    @Transactional
    public Cart getCart(User user) {
        Optional<Cart> cart = cartRepository.findByUserId(user.getId());
        if (cart.isPresent()) {
            return cart.get();
        }
        return createCart(user);
    }

    @Transactional
    public Cart addItem(User user, String sku, int quantity) {
        Cart cart = getCart(user);
        productRepository.findBySku(sku)
                .orElseThrow(() -> new ApiException("PRODUCT_NOT_FOUND", "Product not found", HttpStatus.NOT_FOUND));
        Optional<CartItem> existingItem = cart.getItems().stream().filter(item -> item.getSku().equals(sku))
                .findFirst();
        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            if (quantity == 0) {
                cart.removeItem(item);
            } else if (quantity > 0) {
                item.setQty(quantity);
            } else {
                throw new ApiException("INVALID_QUANTITY", "Invalid quantity", HttpStatus.BAD_REQUEST);
            }
        } else {
            if (quantity == 0) {
                return cart;
            } else if (quantity < 0) {
                throw new ApiException("INVALID_QUANTITY", "Invalid quantity", HttpStatus.BAD_REQUEST);
            }
            cart.addItem(new CartItem(cart, sku, quantity));
        }
        return cartRepository.save(cart);
    }

    @Transactional
    public Cart removeItem(User user, String sku) {
        Cart cart = getCart(user);
        Optional<CartItem> existingItem = cart.getItems().stream().filter(item -> item.getSku().equals(sku))
                .findFirst();
        if (existingItem.isPresent()) {
            cart.removeItem(existingItem.get());
            return cartRepository.save(cart);
        }
        return cart;
    }

    @Transactional
    public Cart clearCart(User user) {
        Cart cart = getCart(user);
        cart.clearItems();
        return cartRepository.save(cart);
    }

    @Transactional
    public CartResponse view(User user) {
        return toResponse(getCart(user));
    }

    @Transactional
    public CartResponse putItem(User user, String sku, int quantity) {
        return toResponse(addItem(user, sku, quantity));
    }

    @Transactional
    public CartResponse toResponse(Cart cart) {
        List<CartItemResponse> items = new ArrayList<>();
        for (CartItem item : cart.getItems()) {
            Product product = productRepository.findBySku(item.getSku()).orElse(null);
            String name = product == null ? item.getSku() : product.getName();
            String description = product == null || product.getDescription() == null ? "" : product.getDescription();
            BigDecimal price = product == null ? BigDecimal.ZERO : product.getPrice();
            int stock = product == null ? 0 : product.getStock();
            items.add(new CartItemResponse(item.getSku(), item.getQty(), name, description, price, stock));
        }
        return new CartResponse(items);
    }

}
