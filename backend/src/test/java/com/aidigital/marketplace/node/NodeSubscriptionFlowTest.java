package com.aidigital.marketplace.node;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.aidigital.marketplace.node.application.NodeProvisioningService;
import com.aidigital.marketplace.node.application.TwoSUiClientAdapter;
import com.aidigital.marketplace.node.infrastructure.entity.NodeSubscriptionEntity;
import com.aidigital.marketplace.order.infrastructure.entity.OrderEntity;
import com.aidigital.marketplace.order.infrastructure.mapper.OrderMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class NodeSubscriptionFlowTest {

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

    @MockitoBean
    private TwoSUiClientAdapter adapter;

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private NodeProvisioningService nodeProvisioningService;

    @Autowired
    private OrderMapper orderMapper;

    @Test
    void nodePurchaseEnqueuesOnceThenDeliversAndRenews() throws Exception {
        List<NodeSubscriptionEntity> provisioned = new ArrayList<>();
        when(adapter.provision(any(), nullable(NodeSubscriptionEntity.class), any(), anyLong(), anyLong(), anyInt()))
                .thenAnswer(invocation -> {
                    provisioned.add(invocation.getArgument(1));
                    String clientName = invocation.getArgument(2);
                    return new TwoSUiClientAdapter.RemoteClient(
                            99L, clientName, "[{\"uri\":\"vless://panel\"}]", "vless://panel");
                });

        String buyerName = "n" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        JsonNode buyer = post(
                "/api/v1/auth/register",
                null,
                Map.of("username", buyerName, "password", "password123", "nickname", "node-buyer"));
        String buyerToken = buyer.path("data").path("accessToken").asText();

        JsonNode admin =
                post("/api/v1/auth/login", null, Map.of("username", "admin", "password", "test-admin-password"));
        String adminToken = admin.path("data").path("accessToken").asText();

        JsonNode category = post(
                "/api/v1/admin/categories",
                adminToken,
                Map.of("name", "节点", "sortOrder", 9, "status", "ENABLED"));
        long categoryId = category.path("data").path("id").asLong();
        JsonNode product = post(
                "/api/v1/admin/products",
                adminToken,
                Map.of(
                        "categoryId",
                        categoryId,
                        "name",
                        "节点月卡",
                        "description",
                        "node",
                        "priceFen",
                        19900,
                        "deliveryType",
                        "NODE_SUBSCRIPTION",
                        "status",
                        "ON_SALE"));
        long productId = product.path("data").path("id").asLong();
        assertThat(get("/api/v1/products/" + productId, null).path("data").path("availableCount").asLong())
                .isEqualTo(1);

        post(
                "/api/v1/admin/node-plans",
                adminToken,
                Map.of(
                        "productId",
                        productId,
                        "apiBaseUrl",
                        "https://panel.example.com",
                        "webPath",
                        "/app/",
                        "inboundIdsJson",
                        "[1,2]",
                        "trafficBytes",
                        1024,
                        "durationDays",
                        30,
                        "deviceLimit",
                        3,
                        "enabled",
                        true));

        JsonNode order = post("/api/v1/orders", buyerToken, Map.of("productId", productId));
        long orderId = order.path("data").path("id").asLong();
        assertThat(order.path("data").path("inventoryId").isMissingNode()
                        || order.path("data").path("inventoryId").isNull())
                .isTrue();
        assertThat(order.path("data").path("payStatus").asText()).isEqualTo("PENDING");

        JsonNode payment = post("/api/v1/orders/" + orderId + "/payments", buyerToken, Map.of("channel", "SANDBOX"));
        assertThat(payment.path("data").path("status").asText()).isEqualTo("SUCCESS");
        assertThat(get("/api/v1/orders/" + orderId, buyerToken).path("data").path("deliveryStatus").asText())
                .isEqualTo("PROVISIONING");

        Integer jobCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM node_provision_job WHERE order_id = ?", Integer.class, orderId);
        assertThat(jobCount).isEqualTo(1);

        OrderEntity paid = orderMapper.selectById(orderId);
        nodeProvisioningService.enqueuePaidOrder(paid);
        assertThat(jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM node_provision_job WHERE order_id = ?", Integer.class, orderId))
                .isEqualTo(1);

        nodeProvisioningService.processPendingJobs();

        assertThat(get("/api/v1/orders/" + orderId, buyerToken).path("data").path("deliveryStatus").asText())
                .isEqualTo("DELIVERED");
        JsonNode mine = get("/api/v1/node-subscriptions", buyerToken);
        assertThat(mine.path("data")).hasSize(1);
        assertThat(mine.path("data").get(0).path("subscriptionUrl").asText()).isEqualTo("vless://panel");
        assertThat(mine.path("data").get(0).path("status").asText()).isEqualTo("ACTIVE");
        long subscriptionId = mine.path("data").get(0).path("id").asLong();

        String otherName = "o" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        JsonNode other = post(
                "/api/v1/auth/register",
                null,
                Map.of("username", otherName, "password", "password123", "nickname", "other"));
        String otherToken = other.path("data").path("accessToken").asText();
        ResponseEntity<String> hidden =
                exchange("/api/v1/node-subscriptions/" + subscriptionId, HttpMethod.GET, otherToken, null);
        assertThat(hidden.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        JsonNode renewOrder = post("/api/v1/orders", buyerToken, Map.of("productId", productId));
        long renewId = renewOrder.path("data").path("id").asLong();
        post("/api/v1/orders/" + renewId + "/payments", buyerToken, Map.of("channel", "SANDBOX"));
        nodeProvisioningService.processPendingJobs();

        verify(adapter, times(2))
                .provision(any(), nullable(NodeSubscriptionEntity.class), any(), anyLong(), anyLong(), anyInt());
        assertThat(provisioned).hasSize(2);
        assertThat(provisioned.get(0)).isNull();
        assertThat(provisioned.get(1).getProviderClientId()).isEqualTo(99L);
        assertThat(get("/api/v1/orders/" + renewId, buyerToken).path("data").path("deliveryStatus").asText())
                .isEqualTo("DELIVERED");
        assertThat(get("/api/v1/node-subscriptions", buyerToken).path("data")).hasSize(1);
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
