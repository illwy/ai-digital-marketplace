# 第一版系统架构

状态：**已确认**（2026-08-21）  
依据：`AI数字商品自动售卖平台-初步规划与技术选型.md`  
形态：模块化 Spring Boot 单体 + 两个 Vue 3 应用 + MySQL 8 + Redis  
原则：无物流、自动交付、虚拟库存、后台运营驱动。不做微服务、消息队列、多商户。

领域用词见 `CONTEXT.md`。相关决策见 `docs/adr/`。

---

## 1. 系统模块划分

运行时只有一个后端进程。模块是包边界，不是独立服务。模块之间只通过应用服务调用，禁止直接改对方的表。

```
用户浏览器
  ├─ 用户端 web
  └─ 管理端 admin
        │
      Nginx（静态资源 + TLS + /api 反代）
        │
      API 单体
        ├─ 用户中心 identity
        ├─ 商品中心 catalog
        ├─ 虚拟库存中心 inventory
        ├─ 订单中心 order
        ├─ 支付中心 payment
        ├─ 钱包中心 wallet
        ├─ 交付中心 delivery
        ├─ 售后中心 aftersale
        ├─ 公告中心 cms
        └─ 公共能力 shared
        │
      MySQL 8  +  Redis
```

| 模块 | 职责 | 对外只暴露 | 不做什么 |
| --- | --- | --- | --- |
| 用户中心 | 注册登录、JWT、角色（USER / ADMIN）、账号启停 | 认证、当前用户、管理员查用户 | 不存订单、不存库存 |
| 商品中心 | 分类、商品信息、价格、上下架 | 前台浏览、后台维护 | 不计算可售件数权威值（问库存中心） |
| 虚拟库存中心 | 一条条资源的状态机：可用→锁定→已售出 / 失效 | 锁定一条、售出、释放、作废、可售计数、后台导入 | 不创建订单、不对接支付渠道 |
| 订单中心 | 下单、超时取消、支付/发货/售后状态 | 创建订单、查询、标记已支付（仅支付中心调用） | 不直接改库存内容明文 |
| 支付中心 | 渠道下单、回调验签、支付单状态 | 发起支付、处理回调 | 不展示交付内容 |
| 钱包中心 | 余额与流水 | 入账、扣款 | 不替代订单状态 |
| 交付中心 | 支付成功后生成交付记录，给买家看资源 | 按订单生成、用户查询已购 | 不在未支付时暴露库存明文 |
| 售后中心 | 售后单与人工处理 | 用户提交、管理员处理 | 不自动退库存（第一版人工） |
| 公告中心 | 站点公告 | 前台列表、后台维护 | 不参与购买事务 |
| 公共能力 | 统一错误体、分页、幂等、鉴权注解、限流 | 被各模块使用 | 无业务表 |

第一版约束：

- 一单一件商品、绑定一条库存项。
- 同一用户对同一商品，同一时间最多一笔未支付订单。
- 未支付订单 15 分钟过期，锁定库存释放。

---

## 2. 前后端目录结构

两个前端应用、一个后端应用。实现阶段再脚手架，此处只定边界。

```
ai-digital-marketplace/
  frontend/              Vue 3 + TS + Vite + Pinia + Vue Router + Element Plus
    src/views/           页面（业务功能后续再加）
    src/stores/
    src/api/
    src/router/
  backend/               Spring Boot 3 / Java 17 / Maven / MyBatis Plus
    src/main/java/.../
      identity/          （业务阶段再建包）
      catalog/
      inventory/
      order/
      payment/
      wallet/
      delivery/
      aftersale/
      cms/
      shared/
      config/
    src/main/resources/
      mapper/
      db/migration/
  docker/                docker-compose、nginx
  docs/
  AGENTS.md
  CONTEXT.md
```

约定：

- 用户端和管理端不共用页面组件，可共享 TypeScript 类型约定（字段名与枚举与 API 一致）。
- 后端每个业务模块内部按 `controller / application / domain / infrastructure` 分层，对外只从 application 进入。
- 管理端接口全部挂在 `/api/v1/admin`，与用户端隔离。

---

## 3. 数据库设计

MySQL 8，`utf8mb4` / `utf8mb4_0900_ai_ci`。主键 `BIGINT UNSIGNED` 自增。金额用整数分，不用小数。时间用 `DATETIME(3)`。状态用字符串常量，不用 MySQL ENUM。表名 `orders` 避开保留字 `order`。规划中的 `virtual_inventory` 作为库存表名。

### 3.1 用户与权限

**user**

| 字段 | 说明 |
| --- | --- |
| id | 主键 |
| username | 唯一 |
| password_hash | 慢哈希，禁止明文 |
| nickname | 展示名 |
| status | ENABLED / DISABLED |
| created_at / updated_at | |

**role**：`USER`、`ADMIN`。  
**user_role**：用户与角色多对多。第一版一个用户一个主角色即可，表结构预留多角色。

### 3.2 商品

**category**

| 字段 | 说明 |
| --- | --- |
| id | 主键 |
| name | 分类名 |
| sort_order | 前台排序 |
| status | ENABLED / DISABLED |

**product**

| 字段 | 说明 |
| --- | --- |
| id | 主键 |
| category_id | 分类 |
| name | 商品名 |
| description | 详情文案 |
| cover_url | 封面 |
| price_fen | 售价（分） |
| delivery_type | ACCOUNT / LICENSE / TOKEN / TEXT |
| status | DRAFT / ON_SALE / OFF_SALE |
| created_at / updated_at | |

可售件数不冗余在商品表，查询时问库存中心 `countAvailable`。

### 3.3 虚拟库存

**virtual_inventory**

| 字段 | 说明 |
| --- | --- |
| id | 主键 |
| product_id | 所属商品 |
| content | 交付内容（账号密文、激活码等） |
| status | AVAILABLE / LOCKED / SOLD / INVALID |
| order_id | 锁定或售出后归属的订单，可空 |
| locked_at | 锁定时间 |
| sold_at | 售出时间 |
| remark | 后台备注 |
| created_at / updated_at | |

索引：`(product_id, status, id)` 支撑「取一条可售」。状态变更必须带期望原状态（条件更新）。列表接口不对普通用户返回 `content`。

### 3.4 订单与支付

**orders**

| 字段 | 说明 |
| --- | --- |
| id | 主键 |
| order_no | 对外订单号，唯一 |
| user_id | 买家 |
| amount_fen | 应付金额 |
| pay_status | PENDING / PAID / CANCELLED / EXPIRED |
| delivery_status | WAITING / DELIVERED / FAILED |
| aftersale_status | NONE / OPEN / CLOSED |
| inventory_id | 锁定的库存项 |
| expire_at | 未支付过期时间 |
| paid_at | |
| created_at / updated_at | |

索引：`(user_id, created_at)`；`(pay_status, expire_at)` 给超时任务。

**order_item**：商品快照（名称、单价、数量=1），下单后不随后台改价而变。

**payment**

| 字段 | 说明 |
| --- | --- |
| id | 主键 |
| payment_no | 商户支付单号，唯一 |
| order_id | 所属订单 |
| channel | ALIPAY / WECHAT / WALLET |
| amount_fen | 必须与订单金额一致 |
| status | PENDING / SUCCESS / FAILED / CLOSED |
| channel_trade_no | 渠道单号 |
| notify_raw | 回调原文摘要，便于对账 |
| created_at / updated_at | |

同一订单可有多条支付单（用户取消后重试），最多一条 SUCCESS。

### 3.5 钱包、交付、售后、公告、幂等

**wallet**：`user_id` 唯一，`balance_fen`。  
**wallet_log**：不可变流水（充值、扣款、退回），含变动前后余额。

**delivery_record**：`order_id` 唯一；指向 `inventory_id`；`delivered_at`。用户通过本表看已购资源。

**after_sale_ticket**：订单、用户、原因、状态 OPEN / PROCESSING / CLOSED、管理员回复。

**announcement**：标题、正文、ENABLED / DISABLED、发布时间。

**idempotency_record**：`idempotency_key` 唯一、请求体哈希、状态 IN_PROGRESS / SUCCEEDED、响应快照。写操作防重。

### 3.6 Redis 用途（不算表，但是第一版数据面）

- 刷新令牌与可吊销会话
- 登录/下单/支付限流
- 验证码
- 热点商品与分类缓存
- 防重复提交（短 TTL，与幂等表互补）

---

## 4. API 设计规范

前缀：`/api/v1`。资源用复数名词，不用动词路径。字段 camelCase。枚举 `UPPER_SNAKE`。布尔 `is/has/can`。列表必须分页。

### 4.1 认证

- 用户端与管理端都用 Bearer Access Token（约 15 分钟）。
- Refresh Token 存 Redis，可吊销。
- 支付渠道回调不做 JWT，只验渠道签名。
- 管理端接口要求 ADMIN 角色。

### 4.2 统一响应

成功列表：`data` + `pagination(page, pageSize, totalItems, totalPages)`。  
成功单资源：`data`。  
失败：

```
error.code      机器可读，如 INVENTORY_EMPTY
error.message   可展示给用户
error.details   可选校验细节
```

HTTP：400 参数 / 401 未登录 / 403 无权限 / 404 不存在 / 409 冲突或进行中的幂等重复 / 422 语义校验 / 429 限流 / 500 不暴露内部信息。

创建类写操作接受 `Idempotency-Key`。键与请求体哈希一起落库，唯一约束抢占；键相同、体不同则失败。

### 4.3 用户端接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | /auth/register | 注册 |
| POST | /auth/login | 登录 |
| POST | /auth/refresh | 刷新令牌 |
| POST | /auth/logout | 登出 |
| GET | /me | 当前用户 |
| GET | /announcements | 已启用公告 |
| GET | /categories | 启用中的分类 |
| GET | /products | 在售商品分页，可按分类筛 |
| GET | /products/{id} | 详情（含可售件数，不含库存明文） |
| POST | /orders | 下单（锁库存） |
| GET | /orders | 我的订单 |
| GET | /orders/{id} | 订单详情 |
| POST | /orders/{id}/payments | 发起支付 |
| GET | /deliveries | 已购资源 |
| GET | /deliveries/{id} | 单条交付内容（仅本人） |
| POST | /after-sale-tickets | 提交售后 |
| GET | /after-sale-tickets | 我的售后 |

### 4.4 回调

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | /payments/notify/alipay | 支付宝异步通知 |
| POST | /payments/notify/wechat | 微信异步通知 |

回调必须幂等：已 SUCCESS 的支付单再次通知直接应答成功。金额必须与订单一致。

### 4.5 管理端接口（均在 /admin 下）

用户、分类、商品、库存项、订单、交付记录、公告、售后单的列表、详情、状态变更、库存导入。库存列表默认掩码 `content`，仅在授权查看或导入结果中按策略展示。

禁止：任何公开接口返回未售库存明文；用户端订单列表返回其他用户数据。

---

## 5. 用户购买流程

```
浏览商品 → 登录 → 创建订单（锁一条库存）
    → 选择支付方式 → 调起渠道或钱包
    → 支付成功回调 → 订单已支付 + 库存已售出 + 写交付记录
    → 用户在「已购资源」查看内容
```

分步：

1. **浏览**：无需登录。只展示在售商品。详情给出价格与可售件数。
2. **登录**：未登录购买则跳转登录。
3. **下单**：校验商品在售、用户启用。若该用户该商品已有未支付订单，拒绝新单并引导继续支付。事务内锁定一条 AVAILABLE 库存，写订单为 PENDING / WAITING，设置 15 分钟过期。
4. **支付**：生成支付单 PENDING。支付宝电脑网站支付或微信 Native 扫码；或钱包扣款。
5. **确认**：渠道回调验签通过后进入发货（见第 7 节）。钱包支付在同一请求事务内完成扣款与发货，无回调。
6. **查看**：用户打开已购资源，只能看到属于自己的交付记录内容。
7. **失败路径**：无库存则下单失败。超时未付则订单 EXPIRED、库存释放。支付金额不符则拒绝回调。用户取消未支付订单则释放库存。

第一版不做购物车、优惠券、多件、分销。

---

## 6. 虚拟库存流程

库存是离散的「一条资源」，不是可无限切分的数量。状态只允许下列迁移：

```
AVAILABLE ─锁单──► LOCKED ─支付成功──► SOLD
    │                │
    └─后台作废──► INVALID ◄──后台作废（锁定单需先取消订单）
```

LOCKED 超时或用户取消 → 回到 AVAILABLE（仅当仍归属该订单）。  
SOLD 不可回到 AVAILABLE（售后第一版人工处理，不自动回库）。

后台运营动作：

- 按商品导入多条内容（账号、激活码、Token、授权文本）。
- 查看状态与掩码内容。
- 将 AVAILABLE 作废为 INVALID（输错的码）。
- 不直接改 SOLD 行内容来「补发」；补发走售后人工 + 新交付说明。

并发规则：取货时按 `product_id + AVAILABLE` 条件更新或 `SELECT … FOR UPDATE` 取一条，保证同一行不会卖给两人。可售件数 = 该商品 AVAILABLE 行数。

---

## 7. 自动发货流程

规划原文：订单完成 → 匹配库存 → 分配资源 → 生成交付记录 → 通知用户。

第一版把「匹配/分配」提前到下单锁定，支付成功只做确认售出，避免支付瞬间无货。

```
支付成功（回调或钱包）
  → 支付单 SUCCESS（幂等：已成功则直接返回）
  → 订单 pay_status = PAID
  → 库存 LOCKED → SOLD（必须仍归属该订单）
  → 写 delivery_record（一单一条）
  → delivery_status = DELIVERED
  → 用户可查询已购资源
```

异常：

- 回调重复：支付单已 SUCCESS，应答渠道成功，不再改库存。
- 库存已不是 LOCKED 或不属于该订单：记 delivery FAILED，后台发货管理可见，人工处理。此情况视为事故，正常路径不应出现。
- 不在回调里调用邮件/短信等外部 IO；第一版「通知」= 用户刷新订单/已购页可见。站内公告或邮件后置。

超时任务（应用内定时扫描，不用消息队列）：`pay_status=PENDING` 且 `expire_at < now` → 订单 EXPIRED，库存 LOCKED 且 `order_id` 匹配则改回 AVAILABLE。

---

## 8. 后台管理功能列表

对应规划 MVP，第一版范围如下。

### 用户管理

- 分页查询用户（用户名、状态、注册时间）
- 启用 / 禁用
- 查看角色（USER / ADMIN）
- 第一版不提供前台自助改密以外的管理员重置流程也可后置；禁用即可阻止登录

### 商品管理

- 创建 / 编辑商品（分类、名称、描述、封面、价格、交付类型）
- 上架 / 下架 / 草稿
- 列表展示可售件数（来自库存中心）

### 分类管理

- 增删改分类、排序、启用停用
- 删除前检查是否仍被商品引用

### 虚拟库存管理

- 按商品导入库存（批量粘贴或逐条）
- 按商品/状态筛选
- 掩码查看内容、作废 AVAILABLE
- 统计某商品 AVAILABLE / LOCKED / SOLD / INVALID 数量

### 订单管理

- 按订单号、用户、支付状态、发货状态筛选
- 查看订单快照、绑定库存、支付单
- 对未支付订单执行关闭（释放库存）
- 不提供随意改价

### 自动发货管理

- 交付记录列表（成功 / 失败）
- 查看某订单是否已生成交付
- 失败记录标记并备注（人工补救）
- 第一版不做一键对未支付订单「强制发货」

### 公告管理

- 发布 / 编辑 / 上下线公告
- 前台只展示启用中的公告

### 售后管理

- 售后单列表与详情
- 状态：待处理 → 处理中 → 已关闭
- 填写处理说明
- 第一版退款/补发为人工线下或钱包入账，不自动回滚 SOLD 库存

### 后台公共

- 管理员登录（同一用户体系 + ADMIN 角色）
- 所有写操作走鉴权与操作审计（至少记录操作者与时间；完整审计日志可后置）

---

## 第一版明确不做

购物车、优惠券、会员等级、多商户、分销、自动续费、数据分析大盘、Kubernetes、独立消息队列、用户端 SSR。
