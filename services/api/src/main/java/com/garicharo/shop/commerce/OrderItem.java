package com.garicharo.shop.commerce;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.garicharo.shop.catalog.Product;

@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private String sku;

    @Column(name = "name_snapshot", nullable = false)
    private String nameSnapshot;

    @Column(nullable = false)
    private Integer qty;

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "line_total", nullable = false)
    private BigDecimal lineTotal;

    public OrderItem() {}

    public OrderItem(Order order, Product product, String sku, String nameSnapshot, BigDecimal unitPrice, Integer qty) {
        this.order = order;
        this.product = product;
        this.sku = sku;
        this.nameSnapshot = nameSnapshot;
        this.unitPrice = unitPrice;
        this.qty = qty;
        this.lineTotal = unitPrice.multiply(BigDecimal.valueOf(qty));
    }

    public UUID getId() {
        return id;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public Product getProduct() {
        return product;
    }

    public String getSku() {
        return sku;
    }

    public String getNameSnapshot() {
        return nameSnapshot;
    }

    public Integer getQty() {
        return qty;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }
}
