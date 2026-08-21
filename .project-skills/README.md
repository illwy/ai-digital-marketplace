# 项目级 Skills 目录

本目录是规格说明中的索引，不复制技能正文。

实际安装位置：

- 权威副本：`.agents/skills/`
- Grok Build：`.grok/skills/`

安装方式：`npx skills add <owner/repo> --skill <name> -y --copy -a grok`（项目级，不要 `-g`，不要 `-a "*"`）。

更新：`npx skills update -p -y`

---

## 已安装

### architecture/

| Skill | 来源 | installs / 质量 | 用途 |
| --- | --- | --- | --- |
| codebase-design | mattpocock/skills | 440K+；作者 Matt Pocock | 模块界面与缝 |
| domain-modeling | mattpocock/skills | 453K+ | 领域词、CONTEXT.md、ADR |
| api-and-interface-design | addyosmani/agent-skills | 作者 Addy Osmani | REST 契约、幂等 |
| documentation-and-adrs | addyosmani/agent-skills | 同上 | 决策记录 |

### backend/

| Skill | 来源 | 质量 | 用途 |
| --- | --- | --- | --- |
| spring-boot-rest-api-standards | giuseppe-trisciuoglio/developer-kit | 330★ / 2.6K installs | REST、错误体、分页 |
| spring-boot-security-jwt | 同上 | 2.5K | Spring Security + JWT |
| spring-boot-crud-patterns | 同上 | 2.3K | **只借鉴分层；禁止用其 JPA 模板** |
| spring-boot-cache | 同上 | 2.3K | Spring Cache + Redis |
| spring-boot-test-patterns | 同上 | 2.6K | JUnit / Testcontainers |

### frontend/

| Skill | 来源 | 质量 | 用途 |
| --- | --- | --- | --- |
| vue-best-practices | vuejs-ai/skills | 2.8k★ / 37K | Vue 3 Composition API |
| vue-pinia-best-practices | 同上 | 13.9K | Pinia |
| vue-router-best-practices | 同上 | 11.9K | 路由守卫 |
| vue-testing-best-practices | 同上 | 10.8K | Vitest / Playwright |

### database/

| Skill | 来源 | 质量 | 用途 |
| --- | --- | --- | --- |
| mysql | planetscale/database-skills | 官方 PlanetScale / 7.1K | 表、索引、锁、事务 |
| redis-core | redis/agent-skills | Redis 官方 / 2.2K | 数据结构与 key 命名 |
| redis-connections | 同上 | 1.6K | Lettuce 连接池 |

### payment/（规格中的 business）

| Skill | 来源 | 质量 | 用途 |
| --- | --- | --- | --- |
| wechatpay-payment-integration | wechatpay-apiv3/wechatpay-skills | 微信支付官方 | 微信收款 |
| alipay-payment-integration | alipay/ai | 支付宝官方 | 支付宝收款 |

### security/

| Skill | 来源 | 质量 | 用途 |
| --- | --- | --- | --- |
| security-and-hardening | addyosmani/agent-skills | 25.6K | 输入/认证/存储加固 |
| owasp-security | agamm/claude-code-owasp | 342★ / 1.7K | OWASP Top 10:2025 |

### quality/

| Skill | 来源 | 质量 | 用途 |
| --- | --- | --- | --- |
| code-review-and-quality | addyosmani/agent-skills | 29.5K | 合并前审查 |

### devops/

| Skill | 来源 | 质量 | 用途 |
| --- | --- | --- | --- |
| docker-patterns | affaan-m/ecc | 10.4K；Snyk High Risk（正文为 Markdown 实践，无脚本）。例子偏 Node，Compose 思路可迁移到 Java。 | Docker Compose |
| nginx-configuration | aj-geddes/useful-ai-prompts | 1.3K installs；仓库 321★ / 344 commits | Nginx 反代、TLS、限流 |

---

## 搜过但未安装（遵守「不造重复技能」）

| 规格项 | 搜索结论 | 处理 |
| --- | --- | --- |
| Admin Dashboard / Element Plus | `partme-ai/full-stack-skills` 已拆成目录仓，安装不到 SKILL.md；其余 Element Plus 技能 installs 很低 | 不安装；后台用 Vue + Element Plus 官方文档 |
| Ecommerce / 订单商城 | 结果多为 Shopify/跨境营销，不是自营数字商品 | 不安装 |
| Digital goods / 自动发货 | 无匹配的成熟技能 | 不安装；业务规则写在架构文档 |
| Inventory | 结果为实物需求预测、Shopify、游戏背包 | 不安装；库存状态机见 ADR-0003 |
| Linux Server | 独立 Linux 运维技能 installs 低或作者不明 | 不安装；Ubuntu 维护按常规操作 |
| Nginx | 已补装 `nginx-configuration` | 已安装 |
| MyBatis Plus | 全部 <400 installs | 不安装 |
| github/awesome-copilot java-springboot / dockerfile | 仓库过大，克隆不稳定 | 用 developer-kit 的 Spring Boot 技能替代 |

---

## 使用约定

1. 改模块边界 → `codebase-design` + `domain-modeling`
2. 写/改 REST → `api-and-interface-design` + `spring-boot-rest-api-standards`
3. 表结构/锁/索引 → `mysql`
4. Vue 页面 → `vue-best-practices`（及 pinia/router/testing）
5. 登录鉴权 → `spring-boot-security-jwt` + `owasp-security`
6. 支付 → 对应渠道官方 skill；回调必须幂等
7. 合并前 → `code-review-and-quality`
8. 不要根据 `spring-boot-crud-patterns` 生成 JPA 实体
