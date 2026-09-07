import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import ShopLayout from '../layouts/ShopLayout.vue'
import AdminLayout from '../layouts/AdminLayout.vue'
import HomeView from '../views/shop/HomeView.vue'
import ProductDetailView from '../views/shop/ProductDetailView.vue'
import OrderListView from '../views/shop/OrderListView.vue'
import OrderDetailView from '../views/shop/OrderDetailView.vue'
import AlipayPayView from '../views/shop/AlipayPayView.vue'
import DeliveryListView from '../views/shop/DeliveryListView.vue'
import DeliveryDetailView from '../views/shop/DeliveryDetailView.vue'
import AfterSaleListView from '../views/shop/AfterSaleListView.vue'
import NodeSubscriptionListView from '../views/shop/NodeSubscriptionListView.vue'
import LoginView from '../views/auth/LoginView.vue'
import RegisterView from '../views/auth/RegisterView.vue'
import DashboardView from '../views/admin/DashboardView.vue'
import UsersView from '../views/admin/UsersView.vue'
import CategoriesView from '../views/admin/CategoriesView.vue'
import ProductsView from '../views/admin/ProductsView.vue'
import InventoryView from '../views/admin/InventoryView.vue'
import OrdersView from '../views/admin/OrdersView.vue'
import DeliveriesView from '../views/admin/DeliveriesView.vue'
import AnnouncementsView from '../views/admin/AnnouncementsView.vue'
import AfterSalesView from '../views/admin/AfterSalesView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      component: ShopLayout,
      children: [
        { path: '', name: 'home', component: HomeView },
        { path: 'products/:id', name: 'product-detail', component: ProductDetailView },
        { path: 'login', name: 'login', component: LoginView, meta: { guestOnly: true } },
        { path: 'register', name: 'register', component: RegisterView, meta: { guestOnly: true } },
        { path: 'orders', name: 'orders', component: OrderListView, meta: { requiresAuth: true } },
        { path: 'orders/:id', name: 'order-detail', component: OrderDetailView, meta: { requiresAuth: true } },
        { path: 'orders/:id/pay', name: 'alipay-pay', component: AlipayPayView, meta: { requiresAuth: true } },
        { path: 'deliveries', name: 'deliveries', component: DeliveryListView, meta: { requiresAuth: true } },
        { path: 'deliveries/:id', name: 'delivery-detail', component: DeliveryDetailView, meta: { requiresAuth: true } },
        { path: 'after-sales', name: 'after-sales', component: AfterSaleListView, meta: { requiresAuth: true } },
        { path: 'node-subscriptions', name: 'node-subscriptions', component: NodeSubscriptionListView, meta: { requiresAuth: true } },
      ],
    },
    {
      path: '/admin',
      component: AdminLayout,
      meta: { requiresAuth: true, requiresAdmin: true },
      children: [
        { path: '', name: 'admin-home', component: DashboardView },
        { path: 'users', name: 'admin-users', component: UsersView },
        { path: 'categories', name: 'admin-categories', component: CategoriesView },
        { path: 'products', name: 'admin-products', component: ProductsView },
        { path: 'inventory', name: 'admin-inventory', component: InventoryView },
        { path: 'orders', name: 'admin-orders', component: OrdersView },
        { path: 'deliveries', name: 'admin-deliveries', component: DeliveriesView },
        { path: 'announcements', name: 'admin-announcements', component: AnnouncementsView },
        { path: 'after-sales', name: 'admin-after-sales', component: AfterSalesView },
      ],
    },
  ],
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  if (!auth.ready) {
    await auth.hydrate()
  }
  if (to.meta.requiresAuth && !auth.isAuthenticated) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.meta.requiresAdmin && !auth.isAdmin) {
    return { name: 'home' }
  }
  if (to.meta.guestOnly && auth.isAuthenticated) {
    return { name: 'home' }
  }
  return true
})

export default router
