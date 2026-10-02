package com.garicharo.shop.commerce;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.garicharo.shop.PostgresIT;
import com.garicharo.shop.identity.User;

class CartValidationIT extends PostgresIT {

    private static final String PASSWORD = "shopper123";

    @Test
    void putRejectsBlankSkuNegativeQtyAndUnknownProduct() throws Exception {
        User user = saveShopper("cart-" + UUID.randomUUID() + "@shop.local", PASSWORD);

        mockMvc.perform(put("/api/cart/items")
                        .with(httpBasic(user.getEmail(), PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"\",\"qty\":1}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/cart/items")
                        .with(httpBasic(user.getEmail(), PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"ANY\",\"qty\":-2}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/cart/items")
                        .with(httpBasic(user.getEmail(), PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"MISSING-SKU\",\"qty\":1}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
    }
}
