package com.garicharo.shop.catalog;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api")
public class ProductController {
    private final ProductRepository productRepository;

    public ProductController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @GetMapping("/products")
    public Page<Product> getAllProducts(@PageableDefault(size = 20) Pageable pageable) {
        return productRepository.findAll(pageable);
    }

    @GetMapping("/products/{sku}")
    public Product getProductBySku(@PathVariable String sku) {
        return productRepository.findBySku(sku)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found with sku: " + sku));
    }

    @PostMapping("/admin/products")
    public Product createProduct(@RequestBody Product product) {
        if (productRepository.findBySku(product.getSku()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Product with sku: " + product.getSku() + " already exists");
        }
        product.setId(null);
        product.setOrigin(ProductOrigin.MANUAL);
        product.setCreatedAt(OffsetDateTime.now());
        product.setUpdatedAt(OffsetDateTime.now());
        return productRepository.save(product);
    }

    @PutMapping("/admin/products/{sku}")
    public Product updateProduct(@PathVariable String sku, @RequestBody Product product) {
        Product existingProduct = productRepository.findBySku(sku)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found with sku: " + sku));
        existingProduct.setName(product.getName());
        existingProduct.setDescription(product.getDescription());
        existingProduct.setCategory(product.getCategory());
        existingProduct.setPrice(product.getPrice());
        existingProduct.setStock(product.getStock());
        existingProduct.setWeightKg(product.getWeightKg());
        existingProduct.setVersion(product.getVersion());
        existingProduct.setUpdatedAt(OffsetDateTime.now());
        return productRepository.save(existingProduct);
    }

    @DeleteMapping("/admin/products/{sku}")
    public void deleteProduct(@PathVariable String sku) {
        Product existingProduct = productRepository.findBySku(sku)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found with sku: " + sku));
        productRepository.delete(existingProduct);
    }

}
