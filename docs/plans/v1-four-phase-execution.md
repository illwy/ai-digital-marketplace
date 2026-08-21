# 第一版四阶段执行计划

状态：已落地（2026-08-21），可在 http://localhost:8088 验收  
依据：`docs/architecture/v1-system-architecture.md`（已确认）  
顺序：Phase 1 → 2 → 3 → 4，不并行改架构。  
不做：购物车、优惠券、多商户、生产微信/支付宝进件、K8s、消息队列。

金额一律整数分；表名 `orders`；库存下单锁定；持久层 MyBatis Plus。

---

## Phase 1 身份与权限

目标：同一用户体系，USER / ADMIN；JWT Access（15 分钟）+ Redis Refresh（可吊销）。

后端：

- Flyway `user` / `role` / `user_role` / `idempotency_record`
- 统一错误体 `{ error: { code, message, details } }`
- 注册、登录、刷新、登出、`GET /me`
- 登录限流（Redis，10 次 / 10 分钟）
- SecurityFilterChain：公开 auth/ping/健康检查；`/api/v1/admin/**` 需 `ROLE_ADMIN`；JSON 401/403
- 启动时种子管理员（`ADMIN_USERNAME` / `ADMIN_PASSWORD`）

前端：

- Pinia 会话、Axios Bearer、401 刷新一次
- 登录 / 注册
- 商城布局与后台布局
- 路由守卫：未登录跳转登录；非 ADMIN 禁止 `/admin`

验收：注册→登录→`/me`；普通用户访问管理接口 403；管理员可进后台。

---

## Phase 2 商品与虚拟库存

目标：前台浏览在售商品；后台维护分类、商品、库存导入。

数据：`category`、`product`、`virtual_inventory`。

规则：

- 可售件数 = `AVAILABLE` 行数，不冗余在商品表
- 列表不对普通用户返回 `content`
- 库存状态机：AVAILABLE → LOCKED → SOLD；也可作废为 INVALID

接口（节选）：

- 用户：`GET /categories`、`GET /products`、`GET /products/{id}`
- 管理：分类/商品 CRUD 与上下架；库存导入、掩码列表、作废 AVAILABLE

验收：后台导入库存后前台看到可售件数；公开接口无库存明文。

---

## Phase 3 下单、支付、自动交付

目标：一单一件；下单锁一条库存；支付成功售出并写交付记录。

数据：`orders`、`order_item`、`payment`、`wallet`、`wallet_log`、`delivery_record`。

规则：

- 同一用户同一商品同时最多一笔未支付
- 未支付 15 分钟过期，锁定库存释放（应用内定时任务）
- 支付渠道 MVP：`WALLET`、`SANDBOX`；支付宝/微信回调仅桩
- 写操作接受 `Idempotency-Key`

验收：无库存下单失败；锁库后可售减 1；钱包/沙箱支付后可在已购资源看到内容；超时释放。

---

## Phase 4 后台运营

目标：用户启停、订单查看/关闭、公告、售后人工处理。

数据：`announcement`、`after_sale_ticket`。

验收：禁用用户无法登录；管理员关闭未支付订单释放库存；前台只显示启用公告；售后不自动回滚 SOLD。

---

## 验证入口

- 网关：http://localhost:8088
- API：http://localhost:8081
- 默认管理员：`admin` / `admin123456`（仅开发）
