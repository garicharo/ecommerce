package com.garicharo.shop.commerce;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.garicharo.shop.PostgresIT;
import com.garicharo.shop.identity.User;
import com.jayway.jsonpath.JsonPath;

class CheckoutIT extends PostgresIT {

    private static final String PASSWORD = "shopper123";

    @Test
    void addingToCartDoesNotChangeStock() throws Exception {
        String sku = "CART-" + UUID.randomUUID().toString().substring(0, 8);
        saveProduct(sku, "Held in cart", "cart does not reserve", "Test", "10.00", 3);
        User user = saveShopper("c-" + sku + "@shop.local", PASSWORD);
        putCart(user.getEmail(), sku, 2);
        assertThat(productRepository.findBySku(sku).orElseThrow().getStock()).isEqualTo(3);
    }

    @Test
    void raceOnLastUnit() throws Exception {
        String sku = "RACE-" + UUID.randomUUID().toString().substring(0, 8);
        saveProduct(sku, "Last unit", "race", "Test", "10.00", 1);
        User a = saveShopper("a-" + sku + "@shop.local", PASSWORD);
        User b = saveShopper("b-" + sku + "@shop.local", PASSWORD);
        putCart(a.getEmail(), sku, 1);
        putCart(b.getEmail(), sku, 1);

        HttpClient client = HttpClient.newHttpClient();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            Future<Integer> first = pool.submit(() -> {
                ready.countDown();
                go.await(10, TimeUnit.SECONDS);
                return httpCheckout(client, a.getEmail(), UUID.randomUUID()).statusCode();
            });
            Future<Integer> second = pool.submit(() -> {
                ready.countDown();
                go.await(10, TimeUnit.SECONDS);
                return httpCheckout(client, b.getEmail(), UUID.randomUUID()).statusCode();
            });
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            go.countDown();
            List<Integer> statuses = List.of(first.get(30, TimeUnit.SECONDS), second.get(30, TimeUnit.SECONDS));
            assertThat(statuses).containsExactlyInAnyOrder(201, 409);
        }

        assertThat(productRepository.findBySku(sku).orElseThrow().getStock()).isZero();
    }

    @Test
    void sameIdempotencyKeyDoesNotChargeTwice() throws Exception {
        String sku = "IDEM-" + UUID.randomUUID().toString().substring(0, 8);
        saveProduct(sku, "Idempotent", "one charge", "Test", "10.00", 5);
        User user = saveShopper("i-" + sku + "@shop.local", PASSWORD);
        putCart(user.getEmail(), sku, 1);
        UUID key = UUID.randomUUID();
        var first = mockMvc.perform(post("/api/checkout")
                        .with(httpBasic(user.getEmail(), PASSWORD))
                        .header("Idempotency-Key", key.toString()))
                .andExpect(status().isCreated())
                .andReturn();
        String orderId = JsonPath.read(first.getResponse().getContentAsString(), "$.orderId");
        var second = mockMvc.perform(post("/api/checkout")
                        .with(httpBasic(user.getEmail(), PASSWORD))
                        .header("Idempotency-Key", key.toString()))
                .andExpect(status().isOk())
                .andReturn();
        assertThat((String) JsonPath.read(second.getResponse().getContentAsString(), "$.orderId")).isEqualTo(orderId);
        assertThat(productRepository.findBySku(sku).orElseThrow().getStock()).isEqualTo(4);
    }

    private HttpResponse<String> httpCheckout(HttpClient client, String email, UUID key) throws Exception {
        String basic = Base64.getEncoder().encodeToString((email + ":" + PASSWORD).getBytes(StandardCharsets.UTF_8));
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/checkout"))
                .header("Authorization", "Basic " + basic)
                .header("Idempotency-Key", key.toString())
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private void putCart(String email, String sku, int qty) throws Exception {
        mockMvc.perform(put("/api/cart/items")
                        .with(httpBasic(email, PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"" + sku + "\",\"qty\":" + qty + "}"))
                .andExpect(status().isOk());
    }
}
