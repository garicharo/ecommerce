package com.garicharo.shop.catalog;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductCsvWriter {

    private final ProductRepository productRepository;

    public ProductCsvWriter(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional
    public ImportRowOutcome upsert(CsvRowValidator.ValidRow row) {
        OffsetDateTime now = OffsetDateTime.now();
        return productRepository.findBySku(row.sku())
                .map(existing -> {
                    existing.setName(row.name());
                    existing.setDescription(row.description());
                    existing.setCategory(row.category());
                    existing.setPrice(row.price());
                    existing.setStock(row.stock());
                    existing.setWeightKg(row.weightKg());
                    existing.setUpdatedAt(now);
                    productRepository.save(existing);
                    return ImportRowOutcome.UPDATED;
                })
                .orElseGet(() -> {
                    Product product = new Product();
                    product.setSku(row.sku());
                    product.setName(row.name());
                    product.setDescription(row.description());
                    product.setCategory(row.category());
                    product.setPrice(row.price());
                    product.setStock(row.stock());
                    product.setWeightKg(row.weightKg());
                    product.setOrigin(ProductOrigin.CSV);
                    product.setCreatedAt(now);
                    product.setUpdatedAt(now);
                    productRepository.save(product);
                    return ImportRowOutcome.INSERTED;
                });
    }
}
