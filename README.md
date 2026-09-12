# 钥市 · 数字商品自动交付平台

打开 http://localhost:8088 可演示普通数字商品的货架、下单和交付。节点商品使用 `NODE_SUBSCRIPTION` 类型：付款成功后由后台任务调用 2S-UI API v2 开通，用户在「节点」页面获取订阅地址。普通卡密/文本商品仍使用原有虚拟库存流程。

演示管理员见 `.env` 的 `ADMIN_USERNAME` / `ADMIN_PASSWORD`。支付渠道仍可使用 `SANDBOX` / 钱包做联调；真实支付宝配置按环境变量填写。

## 技术栈

| 层 | 技术 |
| --- | --- |
| 前端 | Vue 3、TypeScript、Vite、Pinia、Vue Router、Element Plus |
| 后端 | Java 17、Spring Boot 3、MyBatis Plus、Spring Security、JWT |
| 数据 | MySQL 8、Redis |
| 部署 | Docker Compose、Nginx |

## 目录

```
frontend/     用户端与后续后台将基于此 Vue 工程扩展
backend/      Spring Boot API
docker/       Compose 与 Nginx
docs/         规划与架构
infra/        节点运维脚本与 systemd 单元
```

开发前请阅读：

- [docs/项目规划.md](docs/项目规划.md)
- [docs/architecture/v1-system-architecture.md](docs/architecture/v1-system-architecture.md)
- [AGENTS.md](AGENTS.md)

## 本地开发环境

需要：Node.js 20+、Docker。后端可在本机安装 JDK 17，或只用 Compose 构建。

```bash
cp .env.example .env
# 编辑 .env：设置 JWT_SECRET（≥32 字符）和 ADMIN_PASSWORD（≥10 字符，不要用 admin123456）
docker compose --env-file .env -f docker/docker-compose.yml up -d mysql redis
```

前端：

```bash
cd frontend
npm install
npm run dev
```

后端（已安装 JDK 17 时）：

```bash
cd backend
mvn spring-boot:run
```

整套（含 Nginx 网关）：

```bash
docker compose --env-file .env -f docker/docker-compose.yml up --build
```

后端测试（需本机 MySQL 3307、Redis 6379 已启动）：

```bash
cd backend
mvn test
```

节点适配器测试：

```bash
python infra/la02/run_tests.py
```

本机若已占用 3306/8080/80，开发端口映射为：

- 前端（经 Nginx）：http://localhost:8088
- API 探活：http://localhost:8088/api/v1/ping
- MySQL：localhost:3307 / marketplace
- Redis：localhost:6379

管理员账号来自 `.env` 的 `ADMIN_USERNAME` / `ADMIN_PASSWORD`。若数据库里已有旧管理员，种子不会改其密码，需要清 volume 或手工改密：

```bash
docker compose --env-file .env -f docker/docker-compose.yml down -v
docker compose --env-file .env -f docker/docker-compose.yml up --build
```

启动后会补种演示货架（账号月卡 / 激活码 / API 额度）和一条站点说明。购买路径用「立即交付（演示）」，不接入支付宝/微信商户。
CI：GitHub Actions 在 push / PR 时跑后端 `mvn test`、前端 `npm run build` 和 `python infra/la02/run_tests.py`。

## 2S-UI 节点商品配置

先在后台创建商品并把交付方式设为「节点订阅」，再调用管理员接口配置该商品对应的 2S-UI 面板和入站：

```http
POST /api/v1/admin/node-plans
Authorization: Bearer <admin-access-token>
Content-Type: application/json

{
  "productId": 12,
  "apiBaseUrl": "https://panel.example.com",
  "webPath": "/app/",
  "inboundIdsJson": "[1,2]",
  "trafficBytes": 322122547200,
  "durationDays": 30,
  "deviceLimit": 3,
  "enabled": true
}
```

后端环境变量只保存 2S-UI API Token，不写入数据库或 Git：`NODE_2SUI_ENABLED=true`、`NODE_2SUI_API_TOKEN=...`。适配器只使用 `/app/apiv2/clients` 和 `/app/apiv2/save`，不读取 2S-UI 数据库。支付回调先落库并创建唯一开通任务，任务失败会自动重试；用户可通过 `GET /api/v1/node-subscriptions` 查看订阅地址和协议链接。

上线前请先用测试用户和测试节点验证创建、续费、到期禁用及重复回调；不要把生产服务器地址、节点密钥或其他基础设施信息提交到公开仓库。

## 节点基础设施

节点运维资产统一位于 `infra/`，其中 `infra/la02/` 提供节点配置渲染、应用、导出和健康检查脚本，`infra/systemd/` 提供服务单元。商城后端的节点商品、订单、支付和 2S-UI 订阅开通仍由 `backend/.../node` 负责。详见 `docs/nodes/tizi-integration.md`。
