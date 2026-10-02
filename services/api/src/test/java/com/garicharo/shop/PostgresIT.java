package com.garicharo.shop;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.garicharo.shop.catalog.Product;
import com.garicharo.shop.catalog.ProductOrigin;
import com.garicharo.shop.catalog.ProductRepository;
import com.garicharo.shop.identity.Role;
import com.garicharo.shop.identity.User;
import com.garicharo.shop.identity.UserRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public abstract class PostgresIT {

    /**
     * Prefer Testcontainers when Docker can actually start containers.
     * Set {@code SHOP_IT_LOCAL_DB=true} to force host Postgres (jdbc://127.0.0.1:5432/shop).
     */
    private static final PostgreSQLContainer POSTGRES = startPostgresIfPossible();

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        if (POSTGRES != null) {
            registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
            registry.add("spring.datasource.username", POSTGRES::getUsername);
            registry.add("spring.datasource.password", POSTGRES::getPassword);
            return;
        }
        registry.add("spring.datasource.url", () -> "jdbc:postgresql://127.0.0.1:5432/shop");
        registry.add("spring.datasource.username", () -> "shop");
        registry.add("spring.datasource.password", () -> "shop");
    }

    private static PostgreSQLContainer startPostgresIfPossible() {
        if (Boolean.parseBoolean(System.getenv().getOrDefault("SHOP_IT_LOCAL_DB", "false"))) {
            return null;
        }
        try {
            if (!DockerClientFactory.instance().isDockerAvailable()) {
                return null;
            }
            PostgreSQLContainer container = new PostgreSQLContainer("postgres:16-alpine");
            container.start();
            return container;
        } catch (Throwable ignored) {
            return null;
        }
    }

    @LocalServerPort
    protected int port;

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected ProductRepository productRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    protected User saveShopper(String email, String password) {
        User user = new User();
        user.setEmail(email);
        user.setDisplayName(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(Role.SHOPPER);
        user.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        return userRepository.save(user);
    }

    protected Product saveProduct(String sku, String name, String description, String category, String price,
            int stock) {
        Product product = new Product();
        product.setSku(sku);
        product.setName(name);
        product.setDescription(description);
        product.setCategory(category);
        product.setPrice(new BigDecimal(price));
        product.setStock(stock);
        product.setWeightKg(new BigDecimal("0.500"));
        product.setOrigin(ProductOrigin.MANUAL);
        product.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        product.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        return productRepository.save(product);
    }
}
