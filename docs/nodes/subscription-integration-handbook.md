# 节点订阅接入维护手册

更新时间：2026-09-13

## 地址与职责

- 香港订阅管理层：sub.k8izuh.com，公网 IP 47.243.227.121
- 2S-UI 管理面板：panel.k8izuh.com
- 美国节点：la02.k8izuh.com，公网 IP 45.63.95.89
- 发卡商城：toy-vps，公网 IP 81.71.22.169

用户拿到的订阅地址必须来自订阅管理层，格式为 https://sub.k8izuh.com/s/{token}。不要把 2S-UI 管理地址、API Token 或单节点 vless:// 地址当作 HTTP 订阅地址。

## 商城接入

商城后端通过 2S-UI API 开通客户端，并使用客户端名称生成订阅地址：

- 2S-UI API：panel.k8izuh.com/app/apiv2/clients 和 /app/apiv2/save
- HTTP 订阅：sub.k8izuh.com/sub/{clientName}

Token 只放在运行环境变量中，不提交到 Git、不写入日志和文档。商城数据库保存订阅管理层返回的 subscription_url，用户端和管理端只展示该地址。

## 生产配置

```env
NODE_2SUI_ENABLED=true
NODE_2SUI_API_BASE_URL=https://panel.k8izuh.com
NODE_2SUI_WEB_PATH=/app/
NODE_SUBSCRIPTION_URL_TEMPLATE=https://sub.k8izuh.com/sub/{clientName}
NODE_2SUI_API_TOKEN=<服务器上的 2S-UI API Token>
```

配置更新后，只重建商城 backend/frontend：

```bash
docker compose --env-file .env -f docker/docker-compose.yml -f docker/docker-compose.prod.yml up -d --build --no-deps backend frontend
```

不要执行全局 docker compose down 或 docker system prune，因为 toy-vps 还运行其他服务。

## 验收与回滚

1. 检查 GET /api/v1/ping 返回 MySQL、Redis 均为 ok。
2. 管理端打开“节点订阅”，确认能查询并复制 HTTP 地址。
3. 用户端打开“节点”，确认地址以 https://sub.k8izuh.com/s/ 开头。
4. 用 Clash/Mihomo 导入地址，确认返回 YAML。
5. 若新镜像异常，恢复上一镜像并检查 docker logs marketplace-backend；不要删除数据库卷。

## 常见问题

- 显示 vless://：商城未配置 NODE_SUBSCRIPTION_URL_TEMPLATE，或使用了旧配置。
- 返回 401：检查订阅管理层管理令牌，禁止把令牌写进前端。
- 返回 503：检查香港订阅管理层到 la02 的受限 SSH 同步链路。
- 页面空白：先刷新前端静态资源，再检查浏览器 Network 中 /api/v1/* 响应；路由已避免 out-in 过渡造成的空窗，接口失败应显示错误和重试按钮。
