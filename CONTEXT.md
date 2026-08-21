# AI 数字商品自动售卖平台

自营数字商品商城：用户浏览、下单、支付后，系统从虚拟库存中分配一条资源并自动交付，无需物流。

## Language

**用户 (User)**:
注册后的购买方账号。后台运营人员也是 User，靠角色区分。
_Avoid_: 客户, Client, Buyer, Account

**角色 (Role)**:
内置 `USER` 与 `ADMIN`。MVP 不做自定义权限矩阵。
_Avoid_: Permission set, Group

**商品 (Product)**:
可售卖的数字商品 SKU，含价格、分类、状态和交付类型。
_Avoid_: 货品, SKU, Item, Listing

**分类 (Category)**:
商品的一层分类，用于前台导航。
_Avoid_: Tag, Channel

**虚拟库存项 (InventoryItem)**:
一条可交付资源：账号、激活码、Token 或授权信息。一个商品对应多条库存项。
_Avoid_: 卡密, Stock, License, Asset

**库存状态 (InventoryStatus)**:
`AVAILABLE` → `LOCKED` → `SOLD`；也可从 `AVAILABLE`/`LOCKED` 进入 `INVALID`。
_Avoid_: 在库, 预占, 出库

**订单 (Order)**:
一次购买意图。MVP 一单一件商品、一条库存项。
_Avoid_: 交易, Checkout, Cart, Purchase

**订单项 (OrderItem)**:
订单中的商品快照（名称、单价、数量）。MVP 固定数量为 1。
_Avoid_: Line, Cart item

**支付单 (Payment)**:
一次向支付渠道发起的收款尝试，归属某一订单。同一订单允许因超时重试产生多条支付单，但最多一条成功。
_Avoid_: 流水, Charge, Transaction

**钱包 (Wallet)**:
用户站内余额账户。可充值后用于支付订单。
_Avoid_: 账户, Balance, Account

**钱包流水 (WalletLog)**:
钱包余额变动的不可变记录。
_Avoid_: Ledger entry, Bill

**交付记录 (DeliveryRecord)**:
订单支付成功后，把一条已售库存项展示给用户的记录。
_Avoid_: 发货单, Fulfillment, Shipment

**售后单 (AfterSaleTicket)**:
用户对已购订单发起的人工处理请求。
_Avoid_: 工单, Refund, Ticket, RMA

**公告 (Announcement)**:
运营在前台展示的站点通知。
_Avoid_: 新闻, Banner, Notice
