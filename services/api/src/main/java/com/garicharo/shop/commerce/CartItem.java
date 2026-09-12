package com.garicharo.shop.commerce;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "cart_items", uniqueConstraints = @UniqueConstraint(columnNames = {"cart_id", "sku"}))
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @Column(nullable = false)
    private String sku;

    @Column(nullable = false)
    private Integer qty;

    public CartItem() {}

    public CartItem(Cart cart, String sku, Integer qty) {
        this.cart = cart;
        this.sku = sku;
        this.qty = qty;
    }

    public UUID getId() {
        return id;
    }

    public Cart getCart() {
        return cart;
    }

    public void setCart(Cart cart) {
        this.cart = cart;
    }

    public String getSku() {
        return sku;
    }

    public Integer getQty() {
        return qty;
    }

    public void setQty(Integer qty) {
        this.qty = qty;
    }
}
