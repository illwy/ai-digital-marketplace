# AI 数字商品自动售卖平台 — 架构设计

日期：2026-08-21  
状态：**已确认**（2026-08-21）。实现计划见 `docs/superpowers/plans/`。

依据：

- `AI数字商品自动售卖平台-初步规划与技术选型.md`
- `AI数字商品自动售卖平台-开发Skills技能规格说明.md`
- 领域词汇：`CONTEXT.md`
- 决策记录：`docs/adr/`

---

## 1. 需求理解

### 1.1 定位

自营数字商品自动售卖平台。卖的是账号、API 额度、软件授权、虚拟资源，不是实物。没有物流。核心竞争力是：**支付完成后自动把一条虚拟库存交给买家，并只让买家看见**。

### 1.2 成功标准（MVP）

1. 访客能浏览分类与商品详情。
2. 用户能注册、登录、下单、完成支付（至少一种真实或沙箱渠道）。
3. 支付成功后，对应库存项变为已售出，用户能在「已购资源」看到交付内容。
4. 并发下单不会把同一条库存卖给两个人。
5. 管理员能维护用户、商品、分类、库存、订单、交付、公告、售后。
6. 未支付订单超时后库存回到可售。

### 1.3 明确不做（MVP）

多商户、分销、优惠券、会员等级、自动续费、数据分析、Kubernetes、微服务、独立消息队列。

### 1.4 主路径

```
注册/登录 → 浏览商品 → 创建订单（锁库存） → 支付 → 回调确认
→ 库存售出 + 写交付记录 → 用户查看资源 → 必要时提交售后
```

---

## 2. 技术选型审查

规划选型整体适合 4 核 8G 单机 MVP。建议保留，并做下列约束。

| 层 | 规划 | 结论 |
| --- | --- | --- |
| 前端 | Vue 3 + TS + Vite + Pinia + Vue Router + Axios + Element Plus | 保留。拆成 `apps/web` 与 `apps/admin` 两个应用。Element Plus 以管理端为主。 |
| 后端 | Java 17 + Spring Boot 3 + Maven + MyBatis Plus + Spring Security + JWT | 保留。补 Hibernate Validator、Flyway、Actuator。 |
| 库 | MySQL 8 | 保留。自建 Docker，不用 PlanetScale。utf8mb4，主键 `BIGINT`。表名 `orders` 避开保留字。 |
| 缓存 | Redis | 保留。会话刷新令牌、验证码、热点商品、防重复提交、支付幂等键、限流。客户端用 Lettuce 连接池。 |
| 部署 | Ubuntu 24.04 + Docker Compose + Nginx | 保留。一个 Compose 编排 web、admin 静态资源、api、mysql、redis、nginx。 |

需要补上、规划未写清的点：

- **认证**：Access Token 短时（约 15 分钟）+ Refresh Token 存 Redis，可吊销。不要把长期 JWT 当唯一会话。
- **支付回调**：必须幂等。以渠道通知号或商户支付单号做唯一约束，先落库再调渠道。
- **交付**：支付回调里不要做耗时外部 IO。同事务内改订单/库存/交付表；通知用户可异步。
- **CRUD skill 注意**：已装的 `spring-boot-crud-patterns` 按 JPA 生成，本项目禁止照抄其持久层模板。

Java 21 也可，但规划写死 17，不升级。Nuxt SSR 可后置。钱包表纳入 MVP schema，充值可与商品支付共用支付中心。

---

## 3. 方案对比与推荐

### 3.1 后端形态

| 方案 | 优点 | 缺点 |
| --- | --- | --- |
| **A. 模块化单体（推荐）** | 同库事务覆盖下单锁库存；运维简单 | 包边界要自律 |
| B. 两个后端（商城 API + 后台 API） | 权限隔离更硬 | 重复认证与部署，MVP 过重 |
| C. 微服务 | 可独立扩交付 | 与「初期不引入微服务」冲突 |

选 A。模块是包，不是进程。见 `docs/adr/0001-modular-monolith.md`。

### 3.2 前端形态

选两个 Vue 应用。见 `docs/adr/0002-two-vue-apps.md`。

### 3.3 库存与交付

选「下单锁定、支付售出、超时释放」。见 `docs/adr/0003-lock-inventory-on-create.md`。

---

## 4. 系统结构

```
浏览器
  ├─ apps/web     用户商城
  └─ apps/admin   管理后台
        │
     Nginx（TLS、静态资源、/api 反代）
        │
     apps/api     Spring Boot 模块化单体
        ├─ identity     用户/角色/认证
        ├─ catalog      分类/商品
        ├─ inventory    虚拟库存状态机
        ├─ order        订单
        ├─ payment      支付渠道与回调
        ├─ wallet       余额与流水
        ├─ delivery     交付记录
        ├─ aftersale    售后
        ├─ cms          公告
        └─ shared       鉴权、错误体、幂等、分页
        │
     MySQL 8  +  Redis
```

仓库布局（实现阶段再脚手架，此处只定边界）：

```
apps/web/          用户端 Vue3
apps/admin/        管理端 Vue3
apps/api/          Spring Boot
deploy/            compose、nginx、env 示例
docs/              架构与 ADR
CONTEXT.md
```

---

## 5. 模块接口（深模块，小表面）

每个模块对外只暴露少量应用服务。其他模块禁止直接改其表。

### identity

职责：注册、登录、刷新令牌、当前用户、管理员禁用用户。

- `register` / `login` / `refresh` / `logout`
- `getCurrentUser`
- `requireUser(userId)`、`requireAdmin`

依赖：Redis（刷新令牌、登录限流、验证码）。

### catalog

职责：分类与商品的展示/管理。不碰库存数量的权威数据（可售件数由 inventory 提供）。

- 前台：`listCategories`、`listOnSaleProducts`、`getProductDetail`
- 后台：商品/分类 CRUD、上下架

### inventory

职责：库存项生命周期。这是超卖防护的唯一入口。

- `lockOne(productId) -> InventoryItem`（无货则失败）
- `markSold(itemId, orderId)`
- `release(itemId)`（仅 `LOCKED`）
- `invalidate(itemId)`
- `countAvailable(productId)`
- 后台导入/查询库存（列表接口不得把明文凭证返回给普通用户）

### order

职责：创建订单、取消、超时、查询。创建时调用 `inventory.lockOne`。

- `createOrder(userId, productId) -> Order`
- `cancelUnpaid(orderId)`
- `markPaid(orderId, paymentId)`（只由 payment 在回调成功后调用）
- 用户/管理员查询

### payment

职责：向渠道下单、验签回调、钱包扣款。

- `createPayment(orderId, channel)`
- `handleChannelNotify(channel, rawBody)`（幂等）
- 钱包支付走同一 `markPaid` 出口

第三方回调视为不可信输入，验签后再用。

### wallet

职责：余额与流水。扣款与入账必须记账。

- `credit` / `debit`（条件更新余额，禁止先读后写无版本）

### delivery

职责：生成并查询交付记录。用户侧只返回属于自己的明文。

- `createFromSoldItem(orderId, itemId)`
- `listMine(userId)`

### aftersale / cms

售后单状态机与公告 CRUD。不参与主购买事务。

---

## 6. 关键流程

### 6.1 下单

1. 校验商品在售、用户状态正常。
2. 同一用户同一商品若有未支付订单，拒绝或引导继续支付（避免锁多条库存）。
3. 事务内：`inventory.lockOne` → 写 `orders` + `order_item`（商品快照）→ 库存项记录 `order_id`。
4. 返回订单与过期时间（建议 15 分钟）。

### 6.2 支付与交付

1. `payment.createPayment` 写 `payments`（`PENDING`），带商户单号。
2. 用户完成渠道支付或钱包扣款。
3. 回调：验签 → 按商户单号/渠道单号唯一插入或命中已有行。
4. 已成功则直接返回成功给渠道（幂等短路径）。
5. 未处理则：支付单 `SUCCESS` → `order.markPaid` → `inventory.markSold` → `delivery.createFromSoldItem`。
6. 渠道超时视为未知结果，禁止盲目重放扣款；以查单和对账补齐。

钱包支付：同一事务借记钱包并走步骤 5，不经过渠道回调。

### 6.3 超时释放

定时任务扫描 `EXPIRED` 的 `PENDING` 订单：取消订单，`inventory.release`。只释放仍为 `LOCKED` 且归属该订单的库存。

### 6.4 错误

统一错误体：

```json
{ "error": { "code": "INVENTORY_EMPTY", "message": "暂无可用库存", "details": null } }
```

HTTP：400 参数 / 401 未登录 / 403 无权限 / 404 不存在 / 409 冲突（含幂等冲突）/ 422 语义校验 / 429 限流 / 500 不暴露内部信息。

写操作接受 `Idempotency-Key`。键与请求体哈希一起落库，唯一约束抢占；进行中的重复请求返回 409。

---

## 7. 数据设计

字符集 `utf8mb4`，主键 `BIGINT UNSIGNED`。金额用整数分（`INT`/`BIGINT`），不用浮点。时间用 `DATETIME(3)`。

权威表：

| 表 | 要点 |
| --- | --- |
| `user` | 登录名/邮箱唯一，密码哈希，状态 |
| `role` + `user_role` | MVP 仅 USER/ADMIN |
| `category` | 名称、排序、启用 |
| `product` | 分类、价格分、状态、交付类型 |
| `inventory_item` | `product_id`，状态，密文内容，`order_id` 可空 |
| `orders` | 用户、金额、支付状态、交付状态、过期时间 |
| `order_item` | 商品快照 |
| `payment` | 渠道、商户单号唯一、渠道单号、状态 |
| `wallet` | `user_id` 唯一，余额分 |
| `wallet_log` | 不可变流水 |
| `delivery_record` | `order_id` 唯一，指向库存项 |
| `after_sale_ticket` | 订单、状态、沟通摘要 |
| `announcement` | 标题、内容、上下线 |
| `idempotency_record` | 键、请求哈希、状态、响应快照 |

`inventory_item` 建议索引：`(product_id, status, id)` 以支撑「取一条可售」。状态更新必须带期望状态（CAS），禁止无条件覆盖。

库存内容列对列表 API 不可见；仅交付查询和解密路径可读。MVP 可用应用层加密或至少独立列权限；明文写日志视为缺陷。

---

## 8. API 约定

前缀 `/api/v1`。资源名词复数。分页 `page` + `pageSize`，响应带 `data` 与 `pagination`。

用户端示例：

- `POST /api/v1/auth/register|login|refresh|logout`
- `GET /api/v1/categories`
- `GET /api/v1/products`、`GET /api/v1/products/{id}`
- `POST /api/v1/orders`
- `POST /api/v1/orders/{id}/payments`
- `GET /api/v1/orders`、`GET /api/v1/deliveries`

管理端示例：`/api/v1/admin/users|products|inventory-items|orders|after-sales|announcements`

回调：`POST /api/v1/payments/notify/{channel}`，不做 JWT，只验渠道签名。

字段 camelCase。枚举 `UPPER_SNAKE`。布尔 `is/has/can` 前缀。

---

## 9. 前端

**web：** 首页、分类、详情、登录注册、下单支付、订单列表、已购资源。Composition API + `<script setup>` + Pinia。路由守卫保护订单与交付页。交付页禁止缓存敏感字段到 localStorage。

**admin：** 用户、商品、分类、库存导入与状态、订单、交付、公告、售后。Element Plus。库存表格默认掩码凭证。

两端 Axios 拦截器：带 Access Token；401 尝试 refresh；统一错误 toast。

---

## 10. 安全

按 OWASP 与 `security-and-hardening`：

- 密码：慢哈希（BCrypt/Argon2），禁止可逆。
- SQL：MyBatis 参数绑定，禁止拼接。
- XSS：管理端富文本公告按白名单消毒；用户输入默认文本。
- CSRF：JWT Bearer 为主；若 refresh 放 Cookie 则 SameSite + CSRF 双保险。
- 权限：方法级 `@PreAuthorize`；用户只能读自己的订单和交付。
- 支付：验签、幂等、金额与订单比对，防金额篡改。
- 限流：登录、下单、支付创建。
- 密钥：只从环境变量读取，不进镜像层。
- 回调与用户 API 的 CORS 分离。

---

## 11. 部署

Compose 服务：`nginx`、`api`、`mysql`、`redis`。web/admin 构建为静态文件由 nginx 提供。生产库端口只绑 `127.0.0.1` 或不对外。Nginx 终止 TLS，反代 `/api`。健康检查走 Actuator `/actuator/health`（对外收紧信息）。

4 核 8G：JVM 堆建议不超过 1.5–2G，给 MySQL/Redis 留内存。

---

## 12. 测试策略

- 库存：并发 `lockOne` 不会双卖；超时释放；已售不可释放。
- 支付：重复回调只成功一次；金额不一致拒绝。
- 订单：无库存创建失败；未支付可见；支付后出现交付。
- API 切片测试 + Testcontainers（MySQL/Redis）。
- 前端：Vitest 测守卫与 store；关键购买流 Playwright（有环境再跑）。

---

## 13. 实现顺序（确认架构后再写详细计划）

1. 仓库脚手架与 Compose（MySQL/Redis）。
2. identity + 安全基线。
3. catalog。
4. inventory 状态机（含并发测试）。
5. order + 超时释放。
6. payment + 钱包 + 交付。
7. web 购买闭环。
8. admin 运营闭环。
9. 售后与公告。
10. Nginx 与加固。

---

## 14. 默认假设（可在确认时推翻）

1. 单商户自营。
2. MVP 一单一件。
3. 支付渠道先做支付宝电脑网站支付 + 微信支付 Native 扫码；沙箱可先跑通。
4. 注册用用户名+密码，邮箱可选。
5. 订单支付超时 15 分钟。
6. 库存凭证仅交付接口对购买者可见。
