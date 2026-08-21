package com.aidigital.marketplace;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.aidigital.marketplace.order.application.OrderService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class MarketplaceFlowTest {

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        String mysqlHost = env("MYSQL_HOST", "localhost");
        String mysqlPort = env("MYSQL_PORT", "3307");
        String redisHost = env("REDIS_HOST", "localhost");
        registry.add(
                "spring.datasource.url",
                () -> "jdbc:mysql://"
                        + mysqlHost
                        + ":"
                        + mysqlPort
                        + "/marketplace_test?createDatabaseIfNotExist=true&useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false");
        registry.add("spring.datasource.username", () -> env("MYSQL_ROOT_USER", "root"));
        registry.add("spring.datasource.password", () -> env("MYSQL_ROOT_PASSWORD", "rootpass"));
        registry.add("spring.data.redis.host", () -> redisHost);
        registry.add("spring.data.redis.port", () -> env("REDIS_PORT", "6379"));
        registry.add("spring.data.redis.database", () -> "1");
    }

    private static String env(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderService orderService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void purchaseFlowLocksPaysDeliversAndExpires() throws Exception {
        String username = "b" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        JsonNode buyer = post("/api/v1/auth/register", null, Map.of(
                "username", username, "password", "password123", "nickname", "buyer"));
        String buyerToken = buyer.path("data").path("accessToken").asText();
        assertThat(buyerToken).isNotBlank();

        JsonNode me = get("/api/v1/me", buyerToken);
        assertThat(me.path("data").path("username").asText()).isEqualTo(username);

        ResponseEntity<String> forbidden = exchange("/api/v1/admin/ping", HttpMethod.GET, buyerToken, null);
        assertThat(forbidden.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(read(forbidden).path("error").path("code").asText()).isEqualTo("FORBIDDEN");

        JsonNode admin =
                post("/api/v1/auth/login", null, Map.of("username", "admin", "password", "test-admin-password"));
        String adminToken = admin.path("data").path("accessToken").asText();
        assertThat(get("/api/v1/admin/ping", adminToken).path("data").path("status").asText()).isEqualTo("ok");

        JsonNode category = post(
                "/api/v1/admin/categories",
                adminToken,
                Map.of("name", "IT分类", "sortOrder", 1, "status", "ENABLED"));
        long categoryId = category.path("data").path("id").asLong();
        JsonNode product = post(
                "/api/v1/admin/products",
                adminToken,
                Map.of(
                        "categoryId",
                        categoryId,
                        "name",
                        "IT激活码",
                        "description",
                        "it",
                        "priceFen",
                        100,
                        "deliveryType",
                        "LICENSE",
                        "status",
                        "ON_SALE"));
        long productId = product.path("data").path("id").asLong();
        post(
                "/api/v1/admin/inventory-items",
                adminToken,
                Map.of("productId", productId, "contents", List.of("IT-KEY-1", "IT-KEY-2"), "remark", "it"));

        assertThat(get("/api/v1/products/" + productId, null).path("data").path("availableCount").asLong())
                .isEqualTo(2);

        JsonNode order = post("/api/v1/orders", buyerToken, Map.of("productId", productId));
        long orderId = order.path("data").path("id").asLong();
        assertThat(order.path("data").path("payStatus").asText()).isEqualTo("PENDING");
        assertThat(get("/api/v1/products/" + productId, null).path("data").path("availableCount").asLong())
                .isEqualTo(1);

        JsonNode payment = post("/api/v1/orders/" + orderId + "/payments", buyerToken, Map.of("channel", "SANDBOX"));
        assertThat(payment.path("data").path("status").asText()).isEqualTo("SUCCESS");
        assertThat(get("/api/v1/orders/" + orderId, buyerToken).path("data").path("deliveryStatus").asText())
                .isEqualTo("DELIVERED");
        JsonNode deliveries = get("/api/v1/deliveries", buyerToken);
        assertThat(deliveries.path("data").get(0).path("content").asText()).isEqualTo("IT-KEY-1");

        JsonNode unpaid = post("/api/v1/orders", buyerToken, Map.of("productId", productId));
        long unpaidId = unpaid.path("data").path("id").asLong();
        jdbcTemplate.update("UPDATE orders SET expire_at = DATE_SUB(NOW(3), INTERVAL 1 MINUTE) WHERE id = ?", unpaidId);
        orderService.expireDueOrders();
        assertThat(get("/api/v1/orders/" + unpaidId, buyerToken).path("data").path("payStatus").asText())
                .isEqualTo("EXPIRED");
        assertThat(get("/api/v1/products/" + productId, null).path("data").path("availableCount").asLong())
                .isEqualTo(1);

        ResponseEntity<String> unauthenticated = exchange("/api/v1/me", HttpMethod.GET, null, null);
        assertThat(unauthenticated.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private JsonNode post(String path, String token, Object body) throws Exception {
        ResponseEntity<String> response = exchange(path, HttpMethod.POST, token, body);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        return read(response);
    }

    private JsonNode get(String path, String token) throws Exception {
        ResponseEntity<String> response = exchange(path, HttpMethod.GET, token, null);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        return read(response);
    }

    private ResponseEntity<String> exchange(String path, HttpMethod method, String token, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return rest.exchange(path, method, new HttpEntity<>(body, headers), String.class);
    }

    private JsonNode read(ResponseEntity<String> response) throws Exception {
        return objectMapper.readTree(response.getBody());
    }
}
