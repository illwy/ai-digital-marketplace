export interface ApiErrorBody {
  code: string
  message: string
  details?: unknown
}

export interface ApiErrorResponse {
  error: ApiErrorBody
}

export interface DataResponse<T> {
  data: T
}

export interface Pagination {
  page: number
  pageSize: number
  totalItems: number
  totalPages: number
}

export interface ListResponse<T> {
  data: T[]
  pagination: Pagination
}

export interface UserView {
  id: number
  username: string
  nickname: string
  roles: string[]
}

export interface TokenPayload {
  accessToken: string
  refreshToken: string
  expiresIn: number
  user: UserView
}

export interface CategoryView {
  id: number
  name: string
  sortOrder: number
  status: string
}

export interface ProductView {
  id: number
  categoryId: number
  name: string
  description: string | null
  coverUrl: string | null
  priceFen: number
  deliveryType: string
  status: string
  availableCount: number
}

export interface InventoryView {
  id: number
  productId: number
  maskedContent: string
  status: string
  orderId: number | null
  remark: string | null
  createdAt: string
}

export interface OrderView {
  id: number
  orderNo: string
  userId: number
  amountFen: number
  payStatus: string
  deliveryStatus: string
  aftersaleStatus: string
  inventoryId: number | null
  productId: number
  productName: string
  expireAt: string | null
  paidAt: string | null
  createdAt: string
}

export interface PaymentView {
  id: number
  paymentNo: string
  orderId: number
  channel: string
  amountFen: number
  status: string
}

export interface DeliveryView {
  id: number
  orderId: number
  inventoryId: number
  orderNo: string
  productName: string
  status: string
  content: string
  remark: string | null
  deliveredAt: string
}

export interface WalletView {
  userId: number
  balanceFen: number
}

export interface AnnouncementView {
  id: number
  title: string
  body: string
  status: string
  publishedAt: string | null
  createdAt: string
}

export interface AfterSaleView {
  id: number
  orderId: number
  userId: number
  orderNo: string
  productName: string
  reason: string
  status: string
  adminReply: string | null
  createdAt: string
}

export interface AdminUserView {
  id: number
  username: string
  nickname: string
  status: string
  roles: string[]
  createdAt: string
}

export interface InventoryStatsView {
  available: number
  locked: number
  sold: number
  invalid: number
}

export interface AdminOverviewView {
  userCount: number
  productOnSaleCount: number
  availableInventoryCount: number
  pendingOrderCount: number
  paidOrderCount: number
  openAfterSaleCount: number
  enabledAnnouncementCount: number
}
