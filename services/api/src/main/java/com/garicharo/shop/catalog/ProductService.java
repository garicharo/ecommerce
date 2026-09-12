package com.garicharo.shop.catalog;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.garicharo.shop.shared.ApiException;

@Service
public class ProductService {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public Page<Product> search(String q, String category, String sort, int page, int size) {
        String query = normalizeQuery(q);
        if (query != null) {
            query = "%" + query.toLowerCase() + "%";
        }
        String categoryFilter = blankToNull(category);
        int pageIndex = Math.max(page, 0);
        int pageSize = size <= 0 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        PageRequest pageable = PageRequest.of(pageIndex, pageSize, toSort(sort));
        return productRepository.search(query, categoryFilter, pageable);
    }

    @Transactional(readOnly = true)
    public List<String> categories() {
        return productRepository.findDistinctCategories();
    }

    @Transactional(readOnly = true)
    public Product getBySku(String sku) {
        return productRepository.findBySku(sku)
                .orElseThrow(() -> notFound(sku));
    }

    @Transactional
    public List<Product> lockBySkus(Collection<String> skus) {
        return productRepository.findAllBySkuInForUpdate(skus);
    }

    @Transactional
    public void decrementStock(Product product, int qty) {
        product.setStock(product.getStock() - qty);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public Product create(Product product) {
        if (productRepository.findBySku(product.getSku()).isPresent()) {
            throw new ApiException(
                    "SKU_EXISTS",
                    "Product with sku: " + product.getSku() + " already exists",
                    HttpStatus.CONFLICT);
        }
        product.setId(null);
        product.setOrigin(ProductOrigin.MANUAL);
        product.setCreatedAt(OffsetDateTime.now());
        product.setUpdatedAt(OffsetDateTime.now());
        return productRepository.save(product);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public Product update(String sku, Product product) {
        Product existing = getBySku(sku);
        existing.setName(product.getName());
        existing.setDescription(product.getDescription());
        existing.setCategory(product.getCategory());
        existing.setPrice(product.getPrice());
        existing.setStock(product.getStock());
        existing.setWeightKg(product.getWeightKg());
        existing.setVersion(product.getVersion());
        existing.setUpdatedAt(OffsetDateTime.now());
        return productRepository.save(existing);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void delete(String sku) {
        Product existing = getBySku(sku);
        if (productRepository.existsInOrderItems(existing.getId())) {
            throw new ApiException(
                    "PRODUCT_IN_ORDERS",
                    "Cannot delete SKU " + sku + " because it appears on an order. Set stock to 0 instead.",
                    HttpStatus.CONFLICT);
        }
        productRepository.delete(existing);
    }

    private static ApiException notFound(String sku) {
        return new ApiException("NOT_FOUND", "Product not found with sku: " + sku, HttpStatus.NOT_FOUND);
    }

    private static String normalizeQuery(String q) {
        String trimmed = blankToNull(q);
        if (trimmed == null || trimmed.length() < 2) {
            return null;
        }
        return trimmed;
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static Sort toSort(String sort) {
        String key = sort == null || sort.isBlank() ? "name_asc" : sort.trim();
        return switch (key) {
            case "price_asc" -> Sort.by(Sort.Direction.ASC, "price");
            case "price_desc" -> Sort.by(Sort.Direction.DESC, "price");
            case "name_asc" -> Sort.by(Sort.Direction.ASC, "name");
            default -> throw new ApiException(
                    "INVALID_SORT",
                    "sort must be name_asc, price_asc, or price_desc",
                    HttpStatus.BAD_REQUEST);
        };
    }
}
