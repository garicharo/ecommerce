package com.garicharo.shop.catalog;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.garicharo.shop.PostgresIT;
import com.garicharo.shop.identity.User;

class ProductWriteIT extends PostgresIT {

    private static final String ADMIN = "admin@shop.local";
    private static final String ADMIN_PASSWORD = "admin1234";

    @Test
    void createRejectsNegativePrice() throws Exception {
        mockMvc.perform(post("/api/admin/products")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson("NEG-" + uuid(), "Lamp", -1, 2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PRICE"));
    }

    @Test
    void createRejectsBlankNameAndDollarPrice() throws Exception {
        mockMvc.perform(post("/api/admin/products")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sku":"BLANK-%s","name":"  ","description":"d","category":"Cat","price":"$29.99","stock":1,"weightKg":0.2}
                                """.formatted(uuid())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EMPTY_NAME"));
    }

    @Test
    void createThenDuplicateSkuConflicts() throws Exception {
        String sku = "OK-" + uuid();
        mockMvc.perform(post("/api/admin/products")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson(sku, "Mug", 12.5, 3)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value(sku))
                .andExpect(jsonPath("$.price").value(12.50));
        mockMvc.perform(post("/api/admin/products")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson(sku, "Mug 2", 9, 1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SKU_EXISTS"));
    }

    @Test
    void updateRejectsNegativeStock() throws Exception {
        String sku = "UP-" + uuid();
        saveProduct(sku, "Chair", "wood", "Home", "40.00", 4);
        mockMvc.perform(put("/api/admin/products/" + sku)
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Chair","description":"wood","category":"Home","price":40.00,"stock":-2,"weightKg":1.2,"version":0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("NEGATIVE_STOCK"));
    }

    @Test
    void shopperCannotCreate() throws Exception {
        User shopper = saveShopper("s-" + uuid() + "@shop.local", "shopper123");
        mockMvc.perform(post("/api/admin/products")
                        .with(httpBasic(shopper.getEmail(), "shopper123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson("NOPE-" + uuid(), "Nope", 1, 1)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteUnknownSkuIsNotFound() throws Exception {
        mockMvc.perform(delete("/api/admin/products/MISSING-" + uuid())
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    private static String productJson(String sku, String name, double price, int stock) {
        return """
                {"sku":"%s","name":"%s","description":"desc","category":"Cat","price":%s,"stock":%s,"weightKg":0.250}
                """.formatted(sku, name, price, stock);
    }

    private static String uuid() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
