package com.garicharo.shop.commerce;

public record StockShortage(String sku, int requested, int available) {
}
