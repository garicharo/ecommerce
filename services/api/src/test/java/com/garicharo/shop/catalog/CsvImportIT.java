package com.garicharo.shop.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import com.garicharo.shop.PostgresIT;
import com.jayway.jsonpath.JsonPath;

class CsvImportIT extends PostgresIT {

    private static final String ADMIN = "admin@shop.local";
    private static final String ADMIN_PASSWORD = "admin1234";

    @Test
    void missingRequiredHeaderFailsWithoutWrites() throws Exception {
        long before = productRepository.count();
        MockMultipartFile file = csv("bad.csv", """
                name,description,category,price,stock,weight_kg
                Ghost,no sku column,Cat,1.00,1,0.1
                """);
        String jobId = importJob(file);
        mockMvc.perform(get("/api/admin/imports/" + jobId).with(httpBasic(ADMIN, ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED_HEADER"))
                .andExpect(jsonPath("$.inserted").value(0))
                .andExpect(jsonPath("$.updated").value(0));
        assertThat(productRepository.count()).isEqualTo(before);
    }

    @Test
    void extraColumnsAreIgnored() throws Exception {
        String sku = "EXCOL-" + UUID.randomUUID().toString().substring(0, 8);
        MockMultipartFile file = csv("extra.csv", """
                name,sku,description,category,price,stock,weight_kg,image_url
                Extra Column,%s,desc,Gadgets,1.50,3,0.2,http://example.test/x.png
                """.formatted(sku));
        String jobId = importJob(file);
        mockMvc.perform(get("/api/admin/imports/" + jobId).with(httpBasic(ADMIN, ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.inserted").value(1));
        mockMvc.perform(get("/api/products/" + sku))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Extra Column"))
                .andExpect(jsonPath("$.stock").value(3));
    }

    @Test
    void providedCsvLastWinsAndStoresXssAsText() throws Exception {
        MockMultipartFile file = csv(
                "Code Challenge E-Commerce.csv",
                Files.readAllBytes(challengeCsv()));
        String jobId = importJob(file);
        String body = mockMvc.perform(get("/api/admin/imports/" + jobId).with(httpBasic(ADMIN, ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        int failed = JsonPath.read(body, "$.failed");
        assertThat(failed).isGreaterThan(0);
        assertThat(body).contains("INVALID_PRICE");

        mockMvc.perform(get("/api/products/RS-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Running Shoes"))
                .andExpect(jsonPath("$.description").value("Updated lightweight shoes — now with better arch support"))
                .andExpect(jsonPath("$.price").value(94.99))
                .andExpect(jsonPath("$.stock").value(120));

        mockMvc.perform(get("/api/products/BS-021"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Portable waterproof speaker, 10W, 12hr battery"))
                .andExpect(jsonPath("$.price").value(59.99))
                .andExpect(jsonPath("$.stock").value(110));

        mockMvc.perform(get("/api/products/XS-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("<script>alert('xss')</script>"));

        mockMvc.perform(get("/api/products/SQL-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Robert'); DROP TABLE products;--"));
    }

    @Test
    void shopperCannotImport() throws Exception {
        String email = "shop-" + UUID.randomUUID() + "@shop.local";
        saveShopper(email, "shopper123");
        MockMultipartFile file = csv("x.csv", """
                name,sku,description,category,price,stock,weight_kg
                Nope,NOPE-1,d,Cat,1.00,1,0.1
                """);
        mockMvc.perform(multipart("/api/admin/imports").file(file).with(httpBasic(email, "shopper123")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminWithoutCredentialsIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/products"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/imports"))
                .andExpect(status().isUnauthorized());
    }

    private String importJob(MockMultipartFile file) throws Exception {
        String body = mockMvc.perform(multipart("/api/admin/imports")
                        .file(file)
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD)))
                .andExpect(status().isAccepted())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(body, "$.jobId");
    }

    private static MockMultipartFile csv(String filename, String content) {
        return csv(filename, content.getBytes(StandardCharsets.UTF_8));
    }

    private static MockMultipartFile csv(String filename, byte[] content) {
        return new MockMultipartFile("file", filename, "text/csv", content);
    }

    private static Path challengeCsv() {
        Path dir = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        for (int i = 0; i < 6; i++) {
            Path candidate = dir.resolve("fixtures/Code Challenge E-Commerce.csv");
            if (Files.exists(candidate)) {
                return candidate;
            }
            dir = dir.getParent();
            if (dir == null) {
                break;
            }
        }
        throw new IllegalStateException("fixtures/Code Challenge E-Commerce.csv not found");
    }
}
