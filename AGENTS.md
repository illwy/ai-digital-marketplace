这是一个AI数字商品自动售卖平台。

开发前必须阅读 docs 目录中的项目规划文档。

必须遵循：
- Vue3 + TypeScript
- Spring Boot 3
- MySQL8
- Redis
- Docker

优先使用成熟社区 Skills。

开发前先设计，再编码。
禁止未经确认修改架构。

## 补充约定

- 规划：`docs/项目规划.md`
- Skills 规格：`docs/Skills规格说明.md`
- 已确认架构：`docs/architecture/v1-system-architecture.md`
- 领域用词：`CONTEXT.md`
- 工程目录：`frontend/`、`backend/`、`docker/`（本轮初始化按此落地，与早期 `apps/` 草案不同，以本仓库实际目录为准）
- 第一版按 `docs/plans/v1-four-phase-execution.md` 执行：身份、商品库存、下单支付交付、后台运营
- 后端持久层用 MyBatis Plus，不要用 JPA 模板
- 表名用 `orders`；金额用整数分
