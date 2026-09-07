CREATE TABLE `node_product_plan` (
    product_id BIGINT UNSIGNED NOT NULL PRIMARY KEY,
    api_base_url VARCHAR(512) NOT NULL,
    web_path VARCHAR(128) NOT NULL DEFAULT '/app/',
    inbound_ids_json TEXT NOT NULL,
    traffic_bytes BIGINT NOT NULL DEFAULT 0,
    duration_days INT NOT NULL,
    device_limit INT NOT NULL DEFAULT 0,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `node_subscription` (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    product_id BIGINT UNSIGNED NOT NULL,
    last_order_id BIGINT UNSIGNED NOT NULL,
    client_name VARCHAR(128) NOT NULL,
    provider_client_id BIGINT UNSIGNED NULL,
    traffic_bytes BIGINT NOT NULL DEFAULT 0,
    device_limit INT NOT NULL DEFAULT 0,
    expires_at DATETIME(3) NOT NULL,
    subscription_url TEXT NULL,
    clash_config TEXT NULL,
    status VARCHAR(24) NOT NULL,
    last_error VARCHAR(1000) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_node_subscription_client_name (client_name),
    UNIQUE KEY uk_node_subscription_user_product (user_id, product_id),
    KEY idx_node_subscription_status_expiry (status, expires_at),
    KEY idx_node_subscription_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `node_provision_job` (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT UNSIGNED NOT NULL,
    user_id BIGINT UNSIGNED NOT NULL,
    product_id BIGINT UNSIGNED NOT NULL,
    action VARCHAR(16) NOT NULL,
    status VARCHAR(24) NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    next_run_at DATETIME(3) NOT NULL,
    last_error VARCHAR(1000) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_node_provision_order_action (order_id, action),
    KEY idx_node_provision_pending (status, next_run_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
