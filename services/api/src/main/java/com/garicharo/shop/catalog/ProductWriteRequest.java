package com.garicharo.shop.catalog;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductWriteRequest {

    private String sku;
    private String name;
    private String description;
    private String category;
    private String price;
    private String stock;
    @JsonAlias("weight_kg")
    private String weightKg;
    private Long version;

    public String getSku() {
        return sku;
    }

    public void setSku(Object sku) {
        this.sku = stringify(sku);
    }

    public String getName() {
        return name;
    }

    public void setName(Object name) {
        this.name = stringify(name);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(Object description) {
        this.description = stringify(description);
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(Object category) {
        this.category = stringify(category);
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(Object price) {
        this.price = stringify(price);
    }

    public String getStock() {
        return stock;
    }

    public void setStock(Object stock) {
        this.stock = stringify(stock);
    }

    public String getWeightKg() {
        return weightKg;
    }

    public void setWeightKg(Object weightKg) {
        this.weightKg = stringify(weightKg);
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    static String stringify(Object value) {
        if (value == null) {
            return null;
        }
        return String.valueOf(value);
    }
}
