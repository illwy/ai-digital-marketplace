package com.aidigital.marketplace.node.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import com.aidigital.marketplace.node.config.NodeProperties;
import com.aidigital.marketplace.node.infrastructure.entity.NodeProductPlanEntity;
import com.aidigital.marketplace.node.infrastructure.entity.NodeSubscriptionEntity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpServer;

class TwoSUiClientAdapterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void provisionRequiresEnabledToken() {
        TwoSUiClientAdapter adapter = adapter(properties(false, "token"));
        assertThatThrownBy(() -> adapter.provision(plan("http://127.0.0.1"), null, "n", 1, 1, 1))
                .isInstanceOf(TwoSUiClientAdapter.NodeProviderException.class)
                .hasMessageContaining("未配置");
    }

    @Test
    void provisionRejectsInvalidInboundIds() {
        TwoSUiClientAdapter adapter = adapter(properties(true, "token"));
        NodeProductPlanEntity plan = plan("http://127.0.0.1");
        plan.setInboundIdsJson("oops");
        assertThatThrownBy(() -> adapter.provision(plan, null, "n", 1, 1, 1))
                .isInstanceOf(TwoSUiClientAdapter.NodeProviderException.class)
                .hasMessageContaining("入站");
    }

    @Test
    void provisionCreatesClientAndReturnsPrimaryUri() throws Exception {
        FakePanel panel = startPanel();
        TwoSUiClientAdapter adapter = adapter(properties(true, "test-token"));

        TwoSUiClientAdapter.RemoteClient remote =
                adapter.provision(plan(panel.baseUrl()), null, "market-u1-p2", 2048, 1700000000L, 3);

        assertThat(remote.id()).isEqualTo(1L);
        assertThat(remote.name()).isEqualTo("market-u1-p2");
        assertThat(remote.primaryUri()).isEqualTo("vless://client-1");
        assertThat(panel.savedActions).containsExactly("new");
        JsonNode stored = panel.clients.get(1L);
        assertThat(stored.path("enable").asBoolean()).isTrue();
        assertThat(stored.path("volume").asLong()).isEqualTo(2048L);
        assertThat(stored.path("limitIp").asInt()).isEqualTo(3);
    }

    @Test
    void provisionEditsExistingClient() throws Exception {
        FakePanel panel = startPanel();
        ObjectNode existing = objectMapper.createObjectNode();
        existing.put("id", 7);
        existing.put("name", "market-u1-p2");
        existing.put("enable", false);
        existing.set("links", objectMapper.createArrayNode());
        panel.clients.put(7L, existing);
        panel.nextId.set(8);

        NodeSubscriptionEntity subscription = new NodeSubscriptionEntity();
        subscription.setProviderClientId(7L);
        TwoSUiClientAdapter adapter = adapter(properties(true, "test-token"));

        TwoSUiClientAdapter.RemoteClient remote =
                adapter.provision(plan(panel.baseUrl()), subscription, "market-u1-p2", 4096, 1800000000L, 2);

        assertThat(remote.id()).isEqualTo(7L);
        assertThat(panel.savedActions).containsExactly("edit");
        assertThat(panel.clients.get(7L).path("enable").asBoolean()).isTrue();
        assertThat(panel.clients.get(7L).path("volume").asLong()).isEqualTo(4096L);
    }

    @Test
    void disableTurnsOffRemoteClient() throws Exception {
        FakePanel panel = startPanel();
        ObjectNode existing = objectMapper.createObjectNode();
        existing.put("id", 7);
        existing.put("name", "market-u1-p2");
        existing.put("enable", true);
        panel.clients.put(7L, existing);

        NodeSubscriptionEntity subscription = new NodeSubscriptionEntity();
        subscription.setProviderClientId(7L);
        adapter(properties(true, "test-token")).disable(plan(panel.baseUrl()), subscription);

        assertThat(panel.clients.get(7L).path("enable").asBoolean()).isFalse();
        assertThat(panel.savedActions).containsExactly("edit");
    }

    @Test
    void disableSkipsWhenClientIdMissing() {
        adapter(properties(true, "test-token")).disable(plan("http://127.0.0.1:1"), new NodeSubscriptionEntity());
    }

    @Test
    void saveFailureIsSurfaced() throws Exception {
        FakePanel panel = startPanel();
        panel.saveSuccess = false;
        TwoSUiClientAdapter adapter = adapter(properties(true, "test-token"));

        assertThatThrownBy(() -> adapter.provision(plan(panel.baseUrl()), null, "n", 1, 1, 1))
                .isInstanceOf(TwoSUiClientAdapter.NodeProviderException.class)
                .hasMessageContaining("保存客户端失败");
    }

    private TwoSUiClientAdapter adapter(NodeProperties properties) {
        return new TwoSUiClientAdapter(
                RestClient.builder().messageConverters(converters -> {
                    converters.add(0, new MappingJackson2HttpMessageConverter(objectMapper));
                }),
                objectMapper,
                properties);
    }

    private static NodeProperties properties(boolean enabled, String token) {
        NodeProperties properties = new NodeProperties();
        properties.setEnabled(enabled);
        properties.setApiToken(token);
        return properties;
    }

    private static NodeProductPlanEntity plan(String baseUrl) {
        NodeProductPlanEntity entity = new NodeProductPlanEntity();
        entity.setApiBaseUrl(baseUrl);
        entity.setWebPath("/app/");
        entity.setInboundIdsJson("[1,2]");
        return entity;
    }

    private FakePanel startPanel() throws IOException {
        FakePanel panel = new FakePanel();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/app/apiv2/clients", exchange -> {
            if (!"test-token".equals(exchange.getRequestHeaders().getFirst("Token"))) {
                exchange.sendResponseHeaders(401, -1);
                exchange.close();
                return;
            }
            String query = exchange.getRequestURI().getQuery();
            ArrayNode arr = objectMapper.createArrayNode();
            if (query != null && query.startsWith("id=")) {
                long id = Long.parseLong(query.substring(3));
                JsonNode found = panel.clients.get(id);
                if (found != null) {
                    arr.add(found);
                }
            } else {
                panel.clients.values().forEach(arr::add);
            }
            ObjectNode root = objectMapper.createObjectNode();
            root.set("obj", arr);
            writeJson(exchange, 200, root);
        });
        server.createContext("/app/apiv2/save", exchange -> {
            byte[] raw = exchange.getRequestBody().readAllBytes();
            Map<String, String> form = parseForm(new String(raw, StandardCharsets.UTF_8));
            panel.savedActions.add(form.getOrDefault("action", ""));
            if (!panel.saveSuccess) {
                ObjectNode root = objectMapper.createObjectNode();
                root.put("success", false);
                writeJson(exchange, 200, root);
                return;
            }
            ObjectNode data = (ObjectNode) objectMapper.readTree(form.getOrDefault("data", "{}"));
            long id = data.path("id").asLong(0);
            if (id == 0) {
                id = panel.nextId.getAndIncrement();
                data.put("id", id);
            }
            ArrayNode links = objectMapper.createArrayNode();
            ObjectNode link = objectMapper.createObjectNode();
            link.put("uri", "vless://client-" + id);
            links.add(link);
            data.set("links", links);
            data.put("name", data.path("name").asText(""));
            panel.clients.put(id, data);
            ObjectNode root = objectMapper.createObjectNode();
            root.put("success", true);
            writeJson(exchange, 200, root);
        });
        server.start();
        panel.port = server.getAddress().getPort();
        return panel;
    }

    private void writeJson(com.sun.net.httpserver.HttpExchange exchange, int status, ObjectNode body)
            throws IOException {
        byte[] bytes = objectMapper.writeValueAsBytes(body);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static Map<String, String> parseForm(String raw) {
        Map<String, String> form = new LinkedHashMap<>();
        if (raw == null || raw.isBlank()) {
            return form;
        }
        for (String part : raw.split("&")) {
            int idx = part.indexOf('=');
            if (idx < 0) {
                continue;
            }
            String key = URLDecoder.decode(part.substring(0, idx), StandardCharsets.UTF_8);
            String value = URLDecoder.decode(part.substring(idx + 1), StandardCharsets.UTF_8);
            form.put(key, value);
        }
        return form;
    }

    private static final class FakePanel {
        private final Map<Long, JsonNode> clients = new LinkedHashMap<>();
        private final List<String> savedActions = new ArrayList<>();
        private final AtomicLong nextId = new AtomicLong(1);
        private int port;
        private boolean saveSuccess = true;

        private String baseUrl() {
            return "http://127.0.0.1:" + port;
        }
    }
}
