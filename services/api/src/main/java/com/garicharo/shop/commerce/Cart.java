package com.garicharo.shop.commerce;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import com.garicharo.shop.identity.User;

@Entity
@Table(name = "carts")
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> items = new ArrayList<>();

    public Cart() {
    }

    public Cart(User user, OffsetDateTime createdAt) {
        this.user = user;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<CartItem> getItems() {
        return items;
    }

    public void addItem(CartItem item) {
        items.add(item);
        item.setCart(this);
    }

    public void removeItem(CartItem item) {
        items.remove(item);
        item.setCart(null);
    }

    public void clearItems() {
        items.clear();
    }
}
