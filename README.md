# 钥市 · 数字商品自动交付原型

打开 http://localhost:8088 即可演示：货架浏览 → 下单 → **立即交付（演示）** → 「已购资源」看卡密。不接入微信/支付宝，也不做真实收款。

演示管理员见 `.env` 的 `ADMIN_USERNAME` / `ADMIN_PASSWORD`。建议先买「演示激活码」（1 元）。

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

本机若已占用 3306/8080/80，开发端口映射为：

- 前端（经 Nginx）：http://localhost:8088
- API 探活：http://localhost:8088/api/v1/ping
- 后端直连：http://localhost:8081/actuator/health
- MySQL：localhost:3307 / marketplace
- Redis：localhost:6379

管理员账号来自 `.env` 的 `ADMIN_USERNAME` / `ADMIN_PASSWORD`。若数据库里已有旧管理员，种子不会改其密码，需要清 volume 或手工改密：

```bash
docker compose --env-file .env -f docker/docker-compose.yml down -v
docker compose --env-file .env -f docker/docker-compose.yml up --build
```

启动后会补种演示货架（账号月卡 / 激活码 / API 额度）和一条站点说明。购买路径用「立即交付（演示）」，不接入支付宝/微信商户。

CI：GitHub Actions 在 push / PR 时跑后端 `mvn test` 和前端 `npm run build`。
