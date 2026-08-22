export type StatusTagType = 'success' | 'info' | 'warning' | 'danger'

const DELIVERY_TYPE_LABELS: Record<string, string> = {
  ACCOUNT: '账号',
  LICENSE: '卡密',
  TOKEN: '额度',
  TEXT: '文本',
}

const PAY_STATUS_LABELS: Record<string, string> = {
  PENDING: '待支付',
  PAID: '已支付',
  CANCELLED: '已取消',
  EXPIRED: '已过期',
}

const PAY_STATUS_TAGS: Record<string, StatusTagType> = {
  PENDING: 'warning',
  PAID: 'success',
  CANCELLED: 'info',
  EXPIRED: 'danger',
}

const DELIVERY_STATUS_LABELS: Record<string, string> = {
  WAITING: '待交付',
  DELIVERED: '已交付',
  FAILED: '交付失败',
}

const DELIVERY_STATUS_TAGS: Record<string, StatusTagType> = {
  WAITING: 'warning',
  DELIVERED: 'success',
  FAILED: 'danger',
}

const ORDER_AFTERSALE_LABELS: Record<string, string> = {
  NONE: '无售后',
  OPEN: '售后中',
  CLOSED: '售后已关闭',
}

const TICKET_STATUS_LABELS: Record<string, string> = {
  OPEN: '待处理',
  PROCESSING: '处理中',
  CLOSED: '已关闭',
}

const TICKET_STATUS_TAGS: Record<string, StatusTagType> = {
  OPEN: 'warning',
  PROCESSING: 'info',
  CLOSED: 'success',
}

const INVENTORY_STATUS_LABELS: Record<string, string> = {
  AVAILABLE: '可用',
  LOCKED: '锁定',
  SOLD: '已售',
  INVALID: '作废',
}

const INVENTORY_STATUS_TAGS: Record<string, StatusTagType> = {
  AVAILABLE: 'success',
  LOCKED: 'warning',
  SOLD: 'info',
  INVALID: 'danger',
}

function lookup(map: Record<string, string>, status: string): string {
  return map[status] ?? status
}

function lookupTag(map: Record<string, StatusTagType>, status: string): StatusTagType {
  return map[status] ?? 'info'
}

export function deliveryTypeLabel(type: string): string {
  return lookup(DELIVERY_TYPE_LABELS, type)
}

export function payStatusLabel(status: string): string {
  return lookup(PAY_STATUS_LABELS, status)
}

export function payStatusTagType(status: string): StatusTagType {
  return lookupTag(PAY_STATUS_TAGS, status)
}

export function deliveryStatusLabel(status: string): string {
  return lookup(DELIVERY_STATUS_LABELS, status)
}

export function deliveryStatusTagType(status: string): StatusTagType {
  return lookupTag(DELIVERY_STATUS_TAGS, status)
}

export function orderAftersaleLabel(status: string): string {
  return lookup(ORDER_AFTERSALE_LABELS, status)
}

export function ticketStatusLabel(status: string): string {
  return lookup(TICKET_STATUS_LABELS, status)
}

export function ticketStatusTagType(status: string): StatusTagType {
  return lookupTag(TICKET_STATUS_TAGS, status)
}

export function inventoryStatusLabel(status: string): string {
  return lookup(INVENTORY_STATUS_LABELS, status)
}

export function inventoryStatusTagType(status: string): StatusTagType {
  return lookupTag(INVENTORY_STATUS_TAGS, status)
}

const PRODUCT_STATUS_LABELS: Record<string, string> = {
  DRAFT: '草稿',
  ON_SALE: '在售',
  OFF_SALE: '下架',
}

const ENABLEMENT_LABELS: Record<string, string> = {
  ENABLED: '启用',
  DISABLED: '停用',
}

const ROLE_LABELS: Record<string, string> = {
  ADMIN: '管理员',
  USER: '买家',
}

export function productStatusLabel(status: string): string {
  return lookup(PRODUCT_STATUS_LABELS, status)
}

export function enablementLabel(status: string): string {
  return lookup(ENABLEMENT_LABELS, status)
}

export function roleLabel(role: string): string {
  return lookup(ROLE_LABELS, role)
}
