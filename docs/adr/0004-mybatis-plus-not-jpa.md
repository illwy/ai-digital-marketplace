# 持久层用 MyBatis Plus，不用 Spring Data JPA

规划已选定 MyBatis Plus。库存锁定、状态机更新和支付幂等都是显式 SQL（`WHERE status = AVAILABLE`、条件更新），手写/Plus Wrapper 比 JPA 隐式脏检查更可控。

社区 Spring Boot CRUD skill 的模板是 JPA。实现时只吸收其 REST 分层和校验思路，禁止按其 JPA 模板生成实体。迁移用 Flyway，表名避开 SQL 保留字（用 `orders` 而不是 `order`）。

**Status:** accepted
