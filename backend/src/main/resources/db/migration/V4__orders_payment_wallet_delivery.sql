CREATE TABLE `orders` (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(32) NOT NULL,
    user_id BIGINT UNSIGNED NOT NULL,
    amount_fen INT NOT NULL,
    pay_status VARCHAR(16) NOT NULL,
    delivery_status VARCHAR(16) NOT NULL,
    aftersale_status VARCHAR(16) NOT NULL,
    inventory_id BIGINT UNSIGNED NULL,
    expire_at DATETIME(3) NULL,
    paid_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_orders_no (order_no),
    KEY idx_orders_user_created (user_id, created_at),
    KEY idx_orders_pay_expire (pay_status, expire_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `order_item` (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT UNSIGNED NOT NULL,
    product_id BIGINT UNSIGNED NOT NULL,
    product_name VARCHAR(128) NOT NULL,
    price_fen INT NOT NULL,
    quantity INT NOT NULL,
    KEY idx_order_item_order (order_id),
    KEY idx_order_item_user_product (product_id, order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `payment` (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    payment_no VARCHAR(32) NOT NULL,
    order_id BIGINT UNSIGNED NOT NULL,
    channel VARCHAR(16) NOT NULL,
    amount_fen INT NOT NULL,
    status VARCHAR(16) NOT NULL,
    channel_trade_no VARCHAR(64) NULL,
    notify_raw TEXT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_payment_no (payment_no),
    KEY idx_payment_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `wallet` (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    balance_fen INT NOT NULL,
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_wallet_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `wallet_log` (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    wallet_id BIGINT UNSIGNED NOT NULL,
    user_id BIGINT UNSIGNED NOT NULL,
    change_fen INT NOT NULL,
    balance_before_fen INT NOT NULL,
    balance_after_fen INT NOT NULL,
    type VARCHAR(16) NOT NULL,
    biz_no VARCHAR(32) NULL,
    remark VARCHAR(255) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    KEY idx_wallet_log_user (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `delivery_record` (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT UNSIGNED NOT NULL,
    inventory_id BIGINT UNSIGNED NOT NULL,
    user_id BIGINT UNSIGNED NOT NULL,
    delivered_at DATETIME(3) NOT NULL,
    status VARCHAR(16) NOT NULL,
    remark VARCHAR(255) NULL,
    UNIQUE KEY uk_delivery_order (order_id),
    KEY idx_delivery_user (user_id, delivered_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
