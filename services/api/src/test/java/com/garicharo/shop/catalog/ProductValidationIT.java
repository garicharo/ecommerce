package com.garicharo.shop.catalog;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.garicharo.shop.PostgresIT;

class ProductValidationIT extends PostgresIT {

    private static final String ADMIN = "admin@shop.local";
    private static final String ADMIN_PASSWORD = "admin1234";

    @Test
    void createRejectsNegativePriceAndEmptyName() throws Exception {
        String sku = "BAD-" + UUID.randomUUID().toString().substring(0, 8);
        mockMvc.perform(post("/api/admin/products")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sku":"%s","name":"","description":"d","category":"Cat","price":-1,"stock":1,"weightKg":0.2}
                                """.formatted(sku)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EMPTY_NAME"));

        mockMvc.perform(post("/api/admin/products")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sku":"%s","name":"Ok","description":"d","category":"Cat","price":-1,"stock":1,"weightKg":0.2}
                                """.formatted(sku)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PRICE"));
    }

    @Test
    void createRejectsDuplicateSkuWithConflict() throws Exception {
        String sku = "DUP-" + UUID.randomUUID().toString().substring(0, 8);
        saveProduct(sku, "One", "desc", "Cat", "9.99", 2);
        mockMvc.perform(post("/api/admin/products")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sku":"%s","name":"Two","description":"d","category":"Cat","price":1.00,"stock":1,"weightKg":0.2}
                                """.formatted(sku)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SKU_EXISTS"));
    }

    @Test
    void updateRejectsNegativeStock() throws Exception {
        String sku = "UPD-" + UUID.randomUUID().toString().substring(0, 8);
        saveProduct(sku, "One", "desc", "Cat", "9.99", 2);
        long version = productRepository.findBySku(sku).orElseThrow().getVersion();
        mockMvc.perform(put("/api/admin/products/" + sku)
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"One","description":"desc","category":"Cat","price":9.99,"stock":-3,"weightKg":0.5,"version":%d}
                                """.formatted(version)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("NEGATIVE_STOCK"));
    }

    @Test
    void malformedJsonReturnsStructuredError() throws Exception {
        mockMvc.perform(post("/api/admin/products")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_JSON"));
    }
}
