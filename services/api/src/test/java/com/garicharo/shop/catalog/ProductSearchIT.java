package com.garicharo.shop.catalog;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.garicharo.shop.PostgresIT;

class ProductSearchIT extends PostgresIT {

    @Test
    void shortQueryIsIgnoredAndFieldsAreSearchable() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String hatSku = "QLEN-HAT-" + suffix;
        String shoeSku = "QLEN-SHOE-" + suffix;
        saveProduct(hatSku, "QlenOneHat", "warm headgear", "Caps-" + suffix, "5.00", 2);
        saveProduct(shoeSku, "QlenOneShoe", "daily trainer", "Footwear-" + suffix, "20.00", 4);

        // q length < 2 is ignored; category still filters (avoid relying on an empty shared DB page).
        mockMvc.perform(get("/api/products")
                        .param("q", "s")
                        .param("category", "Caps-" + suffix)
                        .param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.sku=='" + hatSku + "')]").exists())
                .andExpect(jsonPath("$.items[?(@.sku=='" + shoeSku + "')]").doesNotExist());

        mockMvc.perform(get("/api/products")
                        .param("q", "s")
                        .param("category", "Footwear-" + suffix)
                        .param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.sku=='" + shoeSku + "')]").exists());

        mockMvc.perform(get("/api/products").param("q", "sh").param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.sku=='" + shoeSku + "')]").exists())
                .andExpect(jsonPath("$.items[?(@.sku=='" + hatSku + "')]").doesNotExist());

        mockMvc.perform(get("/api/products").param("q", "warm").param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.sku=='" + hatSku + "')]").exists());

        mockMvc.perform(get("/api/products").param("q", "Footwear-" + suffix).param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.sku=='" + shoeSku + "')]").exists());

        mockMvc.perform(get("/api/products").param("q", hatSku).param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.sku=='" + hatSku + "')]").exists())
                .andExpect(jsonPath("$.items[?(@.sku=='" + shoeSku + "')]").doesNotExist());

        mockMvc.perform(get("/api/products").param("category", "Caps-" + suffix).param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.sku=='" + hatSku + "')]").exists())
                .andExpect(jsonPath("$.items[?(@.sku=='" + shoeSku + "')]").doesNotExist());
    }

    @Test
    void invalidSortIsBadRequest() throws Exception {
        mockMvc.perform(get("/api/products").param("sort", "popularity"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_SORT"));
    }
}
