import { http, newIdempotencyKey } from './http'
import type {
  AfterSaleView,
  AnnouncementView,
  CategoryView,
  DataResponse,
  DeliveryView,
  ListResponse,
  OrderView,
  PayChannelView,
  PaymentView,
  ProductView,
  WalletView,
} from '../types/api'

export function fetchCategories() {
  return http.get<DataResponse<CategoryView[]>>('/categories')
}

export function fetchProducts(params: { categoryId?: number; page?: number; pageSize?: number }) {
  return http.get<ListResponse<ProductView>>('/products', { params })
}

export function fetchProduct(id: number) {
  return http.get<DataResponse<ProductView>>(`/products/${id}`)
}

export function fetchAnnouncements() {
  return http.get<DataResponse<AnnouncementView[]>>('/announcements')
}

export function createOrder(productId: number) {
  return http.post<DataResponse<OrderView>>(
    '/orders',
    { productId },
    { headers: { 'Idempotency-Key': newIdempotencyKey() } },
  )
}

export function fetchOrders(page = 1) {
  return http.get<ListResponse<OrderView>>('/orders', { params: { page, pageSize: 20 } })
}

export function fetchOrder(id: number) {
  return http.get<DataResponse<OrderView>>(`/orders/${id}`)
}

export function cancelOrder(id: number) {
  return http.post<DataResponse<OrderView>>(`/orders/${id}/cancel`)
}

export function payOrder(id: number, channel: 'WALLET' | 'SANDBOX' | 'ALIPAY') {
  return http.post<DataResponse<PaymentView>>(
    `/orders/${id}/payments`,
    { channel },
    { headers: { 'Idempotency-Key': newIdempotencyKey() } },
  )
}

export function fetchPayChannels() {
  return http.get<DataResponse<PayChannelView>>('/payments/channels')
}

export function syncAlipayPayment(orderId: number) {
  return http.post<DataResponse<PaymentView>>(`/orders/${orderId}/payments/sync`)
}

export function submitAlipayForm(paymentHtml: string, target = 'alipayCashier'): void {
  const container = document.createElement('div')
  container.innerHTML = paymentHtml
  container.style.display = 'none'
  document.body.appendChild(container)
  const form = container.querySelector('form')
  if (form) {
    form.setAttribute('target', target)
    form.submit()
  }
  window.setTimeout(() => container.remove(), 2000)
}

export function fetchDeliveries(params: { orderId?: number; page?: number } = {}) {
  return http.get<ListResponse<DeliveryView>>('/deliveries', { params: { pageSize: 20, page: 1, ...params } })
}

export function fetchDelivery(id: number) {
  return http.get<DataResponse<DeliveryView>>(`/deliveries/${id}`)
}

export function fetchWallet() {
  return http.get<DataResponse<WalletView>>('/wallet')
}

export function sandboxTopup(amountFen: number) {
  return http.post<DataResponse<WalletView>>('/wallet/sandbox-credits', { amountFen })
}

export function createAfterSale(orderId: number, reason: string) {
  return http.post<DataResponse<AfterSaleView>>('/after-sale-tickets', { orderId, reason })
}

export function fetchAfterSales(page = 1) {
  return http.get<ListResponse<AfterSaleView>>('/after-sale-tickets', { params: { page, pageSize: 20 } })
}
