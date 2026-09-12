package com.garicharo.shop.commerce;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.garicharo.shop.catalog.Product;
import com.garicharo.shop.catalog.ProductService;
import com.garicharo.shop.identity.User;
import com.garicharo.shop.shared.ApiException;

@Service
public class CheckoutService {

    private final CartService cartService;
    private final OrderRepository orderRepository;
    private final ProductService productService;
    private final PaymentClient paymentClient;

    public CheckoutService(
            CartService cartService,
            OrderRepository orderRepository,
            ProductService productService,
            PaymentClient paymentClient) {
        this.cartService = cartService;
        this.orderRepository = orderRepository;
        this.productService = productService;
        this.paymentClient = paymentClient;
    }

    @Transactional
    public Order checkout(User user, UUID idempotencyKey) {
        return checkoutResult(user, idempotencyKey).order();
    }

    @Transactional
    public CheckoutResult checkoutResult(User user, UUID idempotencyKey) {
        return orderRepository.findByUserIdAndIdempotencyKey(user.getId(), idempotencyKey)
                .map(order -> new CheckoutResult(order, true))
                .orElseGet(() -> new CheckoutResult(placeOrder(user, idempotencyKey), false));
    }

    private Order placeOrder(User user, UUID idempotencyKey) {
        Cart cart = cartService.getCart(user);
        if (cart.getItems().isEmpty()) {
            throw new ApiException("CART_EMPTY", "Cart is empty", HttpStatus.BAD_REQUEST);
        }

        List<String> skus = cart.getItems().stream().map(CartItem::getSku).sorted().toList();
        Map<String, Product> locked = productService.lockBySkus(skus).stream()
                .collect(Collectors.toMap(Product::getSku, Function.identity()));

        List<Map<String, Object>> stockErrors = new ArrayList<>();
        for (CartItem item : cart.getItems()) {
            Product product = locked.get(item.getSku());
            if (product == null) {
                throw new ApiException("NOT_FOUND", "Product not found with sku: " + item.getSku(), HttpStatus.NOT_FOUND);
            }
            if (product.getStock() < item.getQty()) {
                stockErrors.add(Map.of(
                        "sku", item.getSku(),
                        "requested", item.getQty(),
                        "available", product.getStock()));
            }
        }
        if (!stockErrors.isEmpty()) {
            throw new ApiException(
                    "INSUFFICIENT_STOCK",
                    "Not enough stock",
                    HttpStatus.CONFLICT,
                    stockErrors);
        }

        Order order = new Order(
                user,
                idempotencyKey,
                OrderStatus.PENDING_PAYMENT,
                BigDecimal.ZERO,
                OffsetDateTime.now(ZoneOffset.UTC));

        for (CartItem item : cart.getItems()) {
            Product product = locked.get(item.getSku());
            order.addItem(new OrderItem(
                    order,
                    product,
                    product.getSku(),
                    product.getName(),
                    product.getPrice(),
                    item.getQty()));
        }
        order.setTotal(order.getItems().stream()
                .map(OrderItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        PaymentOutcome paymentOutcome = paymentClient.charge(order.getTotal());
        if (paymentOutcome != PaymentOutcome.APPROVED) {
            order.setStatus(OrderStatus.REJECTED);
            return orderRepository.save(order);
        }

        for (CartItem item : cart.getItems()) {
            productService.decrementStock(locked.get(item.getSku()), item.getQty());
        }
        order.setStatus(OrderStatus.PAID);
        Order saved = orderRepository.save(order);
        cartService.clearCart(user);
        return saved;
    }
}
