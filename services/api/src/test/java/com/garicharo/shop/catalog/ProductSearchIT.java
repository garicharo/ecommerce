package com.garicharo.shop.catalog;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

import com.garicharo.shop.PostgresIT;

class ProductSearchIT extends PostgresIT {

    @Test
    void shortQueryIsIgnoredAndFieldsAreSearchable() throws Exception {
        saveProduct("QLEN-HAT", "QlenOneHat", "warm headgear", "Caps", "5.00", 2);
        saveProduct("QLEN-SHOE", "QlenOneShoe", "daily trainer", "Footwear", "20.00", 4);

        mockMvc.perform(get("/api/products").param("q", "s").param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.sku=='QLEN-HAT')]").exists())
                .andExpect(jsonPath("$.items[?(@.sku=='QLEN-SHOE')]").exists());

        mockMvc.perform(get("/api/products").param("q", "sh").param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.sku=='QLEN-SHOE')]").exists())
                .andExpect(jsonPath("$.items[?(@.sku=='QLEN-HAT')]").doesNotExist());

        mockMvc.perform(get("/api/products").param("q", "warm").param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.sku=='QLEN-HAT')]").exists());

        mockMvc.perform(get("/api/products").param("q", "Footwear").param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.sku=='QLEN-SHOE')]").exists());

        mockMvc.perform(get("/api/products").param("q", "QLEN-HAT").param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.sku=='QLEN-HAT')]").exists())
                .andExpect(jsonPath("$.items[?(@.sku=='QLEN-SHOE')]").doesNotExist());

        mockMvc.perform(get("/api/products").param("category", "Caps").param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.sku=='QLEN-HAT')]").exists())
                .andExpect(jsonPath("$.items[?(@.sku=='QLEN-SHOE')]").doesNotExist());
    }
}
