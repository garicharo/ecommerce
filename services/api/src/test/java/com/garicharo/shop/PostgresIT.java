package com.garicharo.shop;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
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

    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    static {
        postgres.start();
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
