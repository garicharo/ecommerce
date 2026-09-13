package com.garicharo.shop.catalog;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.garicharo.shop.shared.PageResponse;

@RestController
@RequestMapping("/api")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/products")
    public PageResponse<Product> getAllProducts(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "name_asc") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return PageResponse.of(productService.search(q, category, sort, page, size));
    }

    @GetMapping("/products/categories")
    public CategoryList getCategories() {
        return new CategoryList(productService.categories());
    }

    @GetMapping("/products/{sku}")
    public Product getProductBySku(@PathVariable String sku) {
        return productService.getBySku(sku);
    }

    @PostMapping("/admin/products")
    public Product createProduct(@RequestBody Product product) {
        return productService.create(product);
    }

    @PutMapping("/admin/products/{sku}")
    public Product updateProduct(@PathVariable String sku, @RequestBody Product product) {
        return productService.update(sku, product);
    }

    @DeleteMapping("/admin/products/{sku}")
    public void deleteProduct(@PathVariable String sku) {
        productService.delete(sku);
    }
}
