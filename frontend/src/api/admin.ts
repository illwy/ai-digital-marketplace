import { http } from './http'
import type {
  AdminOverviewView,
  AdminUserView,
  AfterSaleView,
  AnnouncementView,
  CategoryView,
  DataResponse,
  DeliveryView,
  InventoryStatsView,
  InventoryView,
  ListResponse,
  OrderView,
  ProductView,
  WalletView,
} from '../types/api'

export function fetchAdminOverview() {
  return http.get<DataResponse<AdminOverviewView>>('/admin/overview')
}

export function uploadProductImage(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return http.post<DataResponse<{ url: string }>>('/admin/uploads', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function fetchAdminUsers(params: { username?: string; status?: string; page?: number }) {
  return http.get<ListResponse<AdminUserView>>('/admin/users', { params: { pageSize: 20, ...params } })
}

export function updateUserStatus(id: number, status: 'ENABLED' | 'DISABLED') {
  return http.post<DataResponse<AdminUserView>>(`/admin/users/${id}/status`, { status })
}

export function creditUserWallet(id: number, amountFen: number) {
  return http.post<DataResponse<WalletView>>(`/admin/users/${id}/wallet-credits`, { amountFen })
}

export function fetchAdminCategories() {
  return http.get<DataResponse<CategoryView[]>>('/admin/categories')
}

export function saveCategory(payload: { name: string; sortOrder: number; status: string }, id?: number) {
  return id
    ? http.put<DataResponse<CategoryView>>(`/admin/categories/${id}`, payload)
    : http.post<DataResponse<CategoryView>>('/admin/categories', payload)
}

export function deleteCategory(id: number) {
  return http.delete(`/admin/categories/${id}`)
}

export function fetchAdminProducts(params: {
  categoryId?: number
  status?: string
  page?: number
  pageSize?: number
}) {
  return http.get<ListResponse<ProductView>>('/admin/products', { params: { pageSize: 20, ...params } })
}

export function saveProduct(
  payload: {
    categoryId: number
    name: string
    description?: string
    coverUrl?: string
    priceFen: number
    deliveryType: string
    status: string
  },
  id?: number,
) {
  return id
    ? http.put<DataResponse<ProductView>>(`/admin/products/${id}`, payload)
    : http.post<DataResponse<ProductView>>('/admin/products', payload)
}

export function fetchInventoryStats(productId: number) {
  return http.get<DataResponse<InventoryStatsView>>(`/admin/products/${productId}/inventory-stats`)
}

export function fetchInventory(params: { productId?: number; status?: string; orderId?: number; page?: number }) {
  return http.get<ListResponse<InventoryView>>('/admin/inventory-items', { params: { pageSize: 20, ...params } })
}

export function importInventory(productId: number, contents: string[], remark?: string) {
  return http.post<DataResponse<InventoryView[]>>('/admin/inventory-items', { productId, contents, remark })
}

export function invalidateInventory(id: number) {
  return http.post<DataResponse<InventoryView>>(`/admin/inventory-items/${id}/invalidate`)
}

export function fetchAdminOrders(params: {
  payStatus?: string
  deliveryStatus?: string
  orderNo?: string
  page?: number
}) {
  return http.get<ListResponse<OrderView>>('/admin/orders', { params: { pageSize: 20, ...params } })
}

export function fetchAdminOrder(id: number) {
  return http.get<DataResponse<OrderView>>(`/admin/orders/${id}`)
}

export function cancelAdminOrder(id: number) {
  return http.post<DataResponse<OrderView>>(`/admin/orders/${id}/cancel`)
}

export function fetchAdminDeliveries(params: { orderId?: number; page?: number } = {}) {
  return http.get<ListResponse<DeliveryView>>('/admin/deliveries', { params: { pageSize: 20, page: 1, ...params } })
}

export function remarkAdminDelivery(id: number, remark: string) {
  return http.post<DataResponse<DeliveryView>>(`/admin/deliveries/${id}/remarks`, { remark })
}

export function fetchAdminAnnouncements() {
  return http.get<DataResponse<AnnouncementView[]>>('/admin/announcements')
}

export function saveAnnouncement(payload: { title: string; body: string; status: string }, id?: number) {
  return id
    ? http.put<DataResponse<AnnouncementView>>(`/admin/announcements/${id}`, payload)
    : http.post<DataResponse<AnnouncementView>>('/admin/announcements', payload)
}

export function fetchAdminAfterSales(params: { status?: string; page?: number }) {
  return http.get<ListResponse<AfterSaleView>>('/admin/after-sale-tickets', { params: { pageSize: 20, ...params } })
}

export function handleAfterSale(id: number, status: 'PROCESSING' | 'CLOSED', adminReply: string) {
  return http.post<DataResponse<AfterSaleView>>(`/admin/after-sale-tickets/${id}/handle`, { status, adminReply })
}
