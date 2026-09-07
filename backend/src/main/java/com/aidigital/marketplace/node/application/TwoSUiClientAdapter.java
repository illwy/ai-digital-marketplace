package com.aidigital.marketplace.node.application;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import com.aidigital.marketplace.node.config.NodeProperties;
import com.aidigital.marketplace.node.infrastructure.entity.NodeProductPlanEntity;
import com.aidigital.marketplace.node.infrastructure.entity.NodeSubscriptionEntity;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Thin adapter for the 2S-UI v2 API. It deliberately speaks only the
 * clients/inbounds endpoints needed by the commerce flow and never accesses
 * the 2S-UI SQLite database.
 */
@Component
public class TwoSUiClientAdapter {

    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;
    private final NodeProperties properties;

    public TwoSUiClientAdapter(
            RestClient.Builder restClientBuilder, ObjectMapper objectMapper, NodeProperties properties) {
        this.restClientBuilder = restClientBuilder;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public RemoteClient provision(
            NodeProductPlanEntity plan,
            NodeSubscriptionEntity existing,
            String clientName,
            long trafficBytes,
            long expiryEpochSeconds,
            int deviceLimit) {
        if (!properties.isEnabled() || properties.getApiToken().isBlank()) {
            throw new NodeProviderException("2S-UI 适配器未配置");
        }
        String baseUrl = plan.getApiBaseUrl();
        RestClient client = restClientBuilder.baseUrl(baseUrl).build();
        JsonNode remote = existing == null || existing.getProviderClientId() == null
                ? null
                : getClient(client, plan, existing.getProviderClientId());

        Map<String, Object> payload = remote == null
                ? newClientPayload(plan, clientName, trafficBytes, expiryEpochSeconds, deviceLimit)
                : mergeExistingPayload(remote, clientName, trafficBytes, expiryEpochSeconds, deviceLimit);
        String action = remote == null ? "new" : "edit";
        if (remote != null && remote.path("id").asLong(0) == 0) {
            throw new NodeProviderException("2S-UI 客户端不存在，无法续费");
        }
        if (remote != null) {
            payload.put("id", remote.path("id").asLong());
        }
        saveClient(client, plan, action, payload);

        JsonNode saved = findByName(client, plan, clientName);
        if (saved == null || saved.path("id").asLong(0) == 0) {
            throw new NodeProviderException("2S-UI 未返回已保存的客户端");
        }
        String links = saved.path("links").isMissingNode() ? "[]" : saved.path("links").toString();
        return new RemoteClient(saved.path("id").asLong(), saved.path("name").asText(clientName), links,
                firstUri(saved.path("links")));
    }

    public void disable(NodeProductPlanEntity plan, NodeSubscriptionEntity subscription) {
        if (subscription.getProviderClientId() == null) {
            return;
        }
        RestClient client = restClientBuilder.baseUrl(plan.getApiBaseUrl()).build();
        JsonNode remote = getClient(client, plan, subscription.getProviderClientId());
        if (remote == null) {
            return;
        }
        Map<String, Object> payload = objectMapper.convertValue(remote, new TypeReference<Map<String, Object>>() {});
        payload.put("enable", false);
        saveClient(client, plan, "edit", payload);
    }

    private JsonNode getClient(RestClient client, NodeProductPlanEntity plan, Long id) {
        JsonNode root = client.get()
                .uri(uri -> uri.path(joinPath(plan.getWebPath(), "apiv2/clients"))
                        .queryParam("id", id).build())
                .header("Token", properties.getApiToken())
                .retrieve().body(JsonNode.class);
        return unwrapObject(root).stream().findFirst().orElse(null);
    }

    private JsonNode findByName(RestClient client, NodeProductPlanEntity plan, String name) {
        JsonNode root = client.get()
                .uri(joinPath(plan.getWebPath(), "apiv2/clients"))
                .header("Token", properties.getApiToken())
                .retrieve().body(JsonNode.class);
        return unwrapObject(root).stream()
                .filter(n -> name.equals(n.path("name").asText()))
                .findFirst().orElse(null);
    }

    private void saveClient(
            RestClient client, NodeProductPlanEntity plan, String action, Map<String, Object> payload) {
        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("object", "clients");
            form.add("action", action);
            form.add("sync", "true");
            form.add("data", objectMapper.writeValueAsString(payload));
            JsonNode response = client.post()
                    .uri(joinPath(plan.getWebPath(), "apiv2/save"))
                    .header("Token", properties.getApiToken())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve().body(JsonNode.class);
            if (response == null || !response.path("success").asBoolean(false)) {
                throw new NodeProviderException("2S-UI 保存客户端失败");
            }
        } catch (NodeProviderException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new NodeProviderException("2S-UI API 请求失败", ex);
        }
    }

    private Map<String, Object> newClientPayload(
            NodeProductPlanEntity plan, String name, long volume, long expiry, int limitIp) {
        String uuid = UUID.randomUUID().toString();
        String password = UUID.randomUUID().toString().replace("-", "");
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("vless", Map.of("name", name, "uuid", uuid, "flow", "xtls-rprx-vision"));
        config.put("hysteria2", Map.of("name", name, "password", password));
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("enable", true);
        payload.put("name", name);
        payload.put("config", config);
        payload.put("inbounds", parseInboundIds(plan.getInboundIdsJson()));
        payload.put("links", List.of());
        payload.put("volume", volume);
        payload.put("expiry", expiry);
        payload.put("limitIp", limitIp);
        payload.put("group", "commerce");
        payload.put("desc", "marketplace");
        return payload;
    }

    private Map<String, Object> mergeExistingPayload(
            JsonNode remote, String name, long volume, long expiry, int limitIp) {
        Map<String, Object> payload = objectMapper.convertValue(remote, new TypeReference<Map<String, Object>>() {});
        payload.put("name", name);
        payload.put("enable", true);
        payload.put("volume", volume);
        payload.put("expiry", expiry);
        payload.put("limitIp", limitIp);
        return payload;
    }

    private List<Long> parseInboundIds(String raw) {
        try {
            if (raw.trim().startsWith("[")) {
                return objectMapper.readValue(raw, new TypeReference<List<Long>>() {});
            }
            List<Long> ids = new ArrayList<>();
            for (String part : raw.split(",")) {
                if (!part.isBlank()) ids.add(Long.parseLong(part.trim()));
            }
            return ids;
        } catch (Exception ex) {
            throw new NodeProviderException("节点入站 ID 配置无效", ex);
        }
    }

    private List<JsonNode> unwrapObject(JsonNode root) {
        if (root == null) return List.of();
        JsonNode obj = root.has("obj") ? root.get("obj") : root;
        if (obj.isArray()) {
            List<JsonNode> result = new ArrayList<>();
            obj.forEach(result::add);
            return result;
        }
        return obj.isObject() ? List.of(obj) : List.of();
    }

    private String firstUri(JsonNode links) {
        if (links != null && links.isArray()) {
            for (JsonNode link : links) {
                if (link.hasNonNull("uri") && !link.path("uri").asText().isBlank()) {
                    return link.path("uri").asText();
                }
            }
        }
        return "";
    }

    private String joinPath(String webPath, String suffix) {
        String left = webPath == null || webPath.isBlank() ? "/app/" : webPath;
        if (!left.startsWith("/")) left = "/" + left;
        if (!left.endsWith("/")) left += "/";
        return left + suffix;
    }

    public record RemoteClient(Long id, String name, String linksJson, String primaryUri) {}

    public static class NodeProviderException extends RuntimeException {
        public NodeProviderException(String message) { super(message); }
        public NodeProviderException(String message, Throwable cause) { super(message, cause); }
    }
}
