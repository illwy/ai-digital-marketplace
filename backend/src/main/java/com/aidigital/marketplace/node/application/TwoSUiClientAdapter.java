package com.aidigital.marketplace.node.application;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

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

    private final ObjectMapper objectMapper;
    private final NodeProperties properties;
    private final HttpClient httpClient;

    public TwoSUiClientAdapter(ObjectMapper objectMapper, NodeProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
    }

    public RemoteClient provision(
            NodeProductPlanEntity plan,
            NodeSubscriptionEntity existing,
            String clientName,
            long trafficBytes,
            long expiryEpochSeconds,
            int deviceLimit) {
        if (!properties.isEnabled() || properties.getApiToken().isBlank()) {
            throw new NodeProviderException("2S-UI adapter is not configured");
        }
        JsonNode remote = existing == null || existing.getProviderClientId() == null
                ? null
                : getClient(plan, existing.getProviderClientId());
        if (existing != null && existing.getProviderClientId() != null && remote == null) {
            throw new NodeProviderException("2S-UI client is missing");
        }

        Map<String, Object> payload = remote == null
                ? newClientPayload(plan, clientName, trafficBytes, expiryEpochSeconds, deviceLimit)
                : mergeExistingPayload(remote, clientName, trafficBytes, expiryEpochSeconds, deviceLimit);
        String action = remote == null ? "new" : "edit";
        if (remote != null && remote.path("id").asLong(0) == 0) {
            throw new NodeProviderException("2S-UI client is missing");
        }
        if (remote != null) {
            payload.put("id", remote.path("id").asLong());
        }
        saveClient(plan, action, payload);

        JsonNode saved = findByName(plan, clientName);
        if (saved == null || saved.path("id").asLong(0) == 0) {
            throw new NodeProviderException("2S-UI did not return the saved client");
        }
        JsonNode detailed = null;
        for (int i = 0; i < 8; i++) {
            detailed = getClient(plan, saved.path("id").asLong());
            if (detailed != null && detailed.path("links").isArray() && detailed.path("links").size() > 0) {
                saved = detailed;
                break;
            }
            if (detailed != null) {
                saved = detailed;
            }
            try {
                Thread.sleep(400);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        String links = saved.path("links").isMissingNode() ? "[]" : saved.path("links").toString();
        return new RemoteClient(saved.path("id").asLong(), saved.path("name").asText(clientName), links,
                firstUri(saved.path("links")));
    }

    public void disable(NodeProductPlanEntity plan, NodeSubscriptionEntity subscription) {
        if (subscription.getProviderClientId() == null) {
            return;
        }
        JsonNode remote = getClient(plan, subscription.getProviderClientId());
        if (remote == null) {
            return;
        }
        Map<String, Object> payload = objectMapper.convertValue(remote, new TypeReference<Map<String, Object>>() {});
        payload.put("enable", false);
        saveClient(plan, "edit", payload);
    }

    private JsonNode getClient(NodeProductPlanEntity plan, Long id) {
        JsonNode root = get(plan, "apiv2/clients?id=" + id);
        return unwrapObject(root).stream().findFirst().orElse(null);
    }

    private JsonNode findByName(NodeProductPlanEntity plan, String name) {
        JsonNode root = get(plan, "apiv2/clients");
        return unwrapObject(root).stream()
                .filter(n -> name.equals(n.path("name").asText()))
                .findFirst().orElse(null);
    }

    private void saveClient(NodeProductPlanEntity plan, String action, Map<String, Object> payload) {
        try {
            String encoded = URLEncoder.encode(objectMapper.writeValueAsString(payload), StandardCharsets.UTF_8);
            String form = "object=clients"
                    + "&action=" + URLEncoder.encode(action, StandardCharsets.UTF_8)
                    + "&sync=true"
                    + "&data=" + encoded;
            JsonNode response = postForm(plan, "apiv2/save", form);
            if (response == null || !isSuccess(response)) {
                throw new NodeProviderException("2S-UI save client failed");
            }
        } catch (NodeProviderException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new NodeProviderException("2S-UI API request failed", ex);
        }
    }

    private boolean isSuccess(JsonNode response) {
        JsonNode success = response.path("success");
        if (success.isBoolean()) {
            return success.asBoolean();
        }
        if (success.isTextual()) {
            return "true".equalsIgnoreCase(success.asText());
        }
        return false;
    }

    private JsonNode get(NodeProductPlanEntity plan, String suffix) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(absoluteUrl(plan, suffix)))
                    .timeout(Duration.ofSeconds(30))
                    .header("Token", properties.getApiToken())
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            ensureSuccessful(response);
            return objectMapper.readTree(response.body() == null ? "{}" : response.body());
        } catch (NodeProviderException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new NodeProviderException("2S-UI API request failed", ex);
        }
    }

    private JsonNode postForm(NodeProductPlanEntity plan, String suffix, String form) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(absoluteUrl(plan, suffix)))
                    .timeout(Duration.ofSeconds(30))
                    .header("Token", properties.getApiToken())
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            ensureSuccessful(response);
            return objectMapper.readTree(response.body() == null ? "{}" : response.body());
        } catch (NodeProviderException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new NodeProviderException("2S-UI API request failed", ex);
        }
    }

    private void ensureSuccessful(HttpResponse<String> response) {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new NodeProviderException("2S-UI HTTP request failed with status " + response.statusCode());
        }
    }

    private String absoluteUrl(NodeProductPlanEntity plan, String suffix) {
        String base = plan.getApiBaseUrl();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + joinPath(plan.getWebPath(), suffix);
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
            throw new NodeProviderException("invalid inbound ids", ex);
        }
    }

    private List<JsonNode> unwrapObject(JsonNode root) {
        if (root == null) return List.of();
        JsonNode obj = root.has("obj") ? root.get("obj") : root;
        if (obj != null && obj.has("clients")) {
            obj = obj.get("clients");
        }
        if (obj != null && obj.isArray()) {
            List<JsonNode> result = new ArrayList<>();
            obj.forEach(result::add);
            return result;
        }
        return obj != null && obj.isObject() ? List.of(obj) : List.of();
    }

    private String firstUri(JsonNode links) {
        if (links != null && links.isArray()) {
            for (JsonNode link : links) {
                if (link.hasNonNull("uri") && !link.path("uri").asText().isBlank()) {
                    return link.path("uri").asText();
                }
            }
        }
        if (links != null && links.isTextual()) {
            try {
                JsonNode parsed = objectMapper.readTree(links.asText());
                return firstUri(parsed);
            } catch (Exception ignored) {
                return "";
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
