<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import type { ComponentPublicInstance } from 'vue'
import { useRoute } from 'vue-router'
import { motion } from 'motion-v'
import { ElMessage } from 'element-plus'
import type { UploadRequestOptions } from 'element-plus'
import { fetchAdminCategories, fetchAdminProducts, saveProduct, uploadProductImage } from '../../api/admin'
import { readApiError } from '../../api/http'
import FoilBadge from '../../components/shop/FoilBadge.vue'
import { deliveryTypeLabel, productStatusLabel } from '../../utils/labels'
import { formatFen } from '../../utils/money'
import { renderMarkdown } from '../../utils/markdown'
import type { CategoryView, ProductView } from '../../types/api'

const route = useRoute()
const products = ref<ProductView[]>([])
const categories = ref<CategoryView[]>([])
const categoryId = ref<number | undefined>(undefined)
const status = ref(typeof route.query.status === 'string' ? route.query.status : '')
const page = ref(1)
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const errorMessage = ref('')
const form = reactive({
  id: 0,
  categoryId: 0,
  name: '',
  description: '',
  coverUrl: '',
  priceFen: 100,
  deliveryType: 'LICENSE',
  status: 'ON_SALE',
})

// ---------- 描述编辑器：Markdown + 图片上传 ----------
const descInputRef = ref<ComponentPublicInstance | null>(null)
const uploadingImage = ref(false)
const descriptionPreview = computed(() => renderMarkdown(form.description))

async function onUploadImage(options: UploadRequestOptions): Promise<unknown> {
  uploadingImage.value = true
  try {
    const { data } = await uploadProductImage(options.file as File)
    const markdown = `![商品图片](${data.data.url})\n`
    const root = descInputRef.value?.$el as HTMLElement | undefined
    const textarea = (root?.querySelector('textarea') ?? null) as HTMLTextAreaElement | null
    if (textarea?.selectionStart != null) {
      const start = textarea.selectionStart
      const end = textarea.selectionEnd ?? start
      form.description = form.description.slice(0, start) + markdown + form.description.slice(end)
    } else {
      form.description = (form.description ? form.description.replace(/\n*$/, '\n\n') : '') + markdown
    }
    ElMessage.success('图片已插入描述')
    return data.data.url
  } catch (error) {
    ElMessage.error(readApiError(error).message)
    return Promise.reject(error)
  } finally {
    uploadingImage.value = false
  }
}

function categoryName(id: number): string {
  return categories.value.find((item) => item.id === id)?.name ?? String(id)
}

async function loadCategories(): Promise<void> {
  const { data } = await fetchAdminCategories()
  categories.value = data.data
}

async function load(): Promise<void> {
  loading.value = true
  errorMessage.value = ''
  try {
    const { data } = await fetchAdminProducts({
      categoryId: categoryId.value || undefined,
      status: status.value || undefined,
      page: page.value,
    })
    products.value = data.data
    total.value = data.pagination.totalItems
  } catch (error) {
    errorMessage.value = readApiError(error).message
  } finally {
    loading.value = false
  }
}

function search(): void {
  page.value = 1
  void load()
}

async function onPageChange(next: number): Promise<void> {
  page.value = next
  await load()
}

function resetForm(): void {
  form.id = 0
  form.categoryId = categories.value[0]?.id ?? 0
  form.name = ''
  form.description = ''
  form.coverUrl = ''
  form.priceFen = 100
  form.deliveryType = 'LICENSE'
  form.status = 'ON_SALE'
}

function openCreate(): void {
  resetForm()
  dialogVisible.value = true
}

function openEdit(item: ProductView): void {
  form.id = item.id
  form.categoryId = item.categoryId
  form.name = item.name
  form.description = item.description ?? ''
  form.coverUrl = item.coverUrl ?? ''
  form.priceFen = item.priceFen
  form.deliveryType = item.deliveryType
  form.status = item.status
  dialogVisible.value = true
}

async function submit(): Promise<void> {
  if (!form.name.trim() || !form.categoryId) {
    ElMessage.warning('请填写名称并选择分类')
    return
  }
  saving.value = true
  try {
    await saveProduct(
      {
        categoryId: form.categoryId,
        name: form.name.trim(),
        description: form.description,
        coverUrl: form.coverUrl || undefined,
        priceFen: form.priceFen,
        deliveryType: form.deliveryType,
        status: form.status,
      },
      form.id || undefined,
    )
    ElMessage.success(form.id ? '已更新商品' : '已创建商品')
    dialogVisible.value = false
    await load()
  } catch (error) {
    ElMessage.error(readApiError(error).message)
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  try {
    await loadCategories()
  } catch (error) {
    errorMessage.value = readApiError(error).message
  }
  await load()
})
</script>

<template>
  <motion.div
    class="page"
    :initial="{ opacity: 0, y: 24 }"
    :animate="{ opacity: 1, y: 0 }"
    :transition="{ duration: 0.4, ease: [0.22, 1, 0.36, 1] }"
  >
    <header class="page-head">
      <div>
        <h2 class="page-title">商品</h2>
        <p class="page-desc">维护在售商品的价格、交付方式与上下架状态。</p>
      </div>
      <el-button type="primary" @click="openCreate">新建商品</el-button>
    </header>

    <div class="filter-bar glass-panel">
      <el-form inline class="page-filters" @submit.prevent="search">
        <el-form-item label="分类">
          <el-select v-model="categoryId" clearable placeholder="全部" style="width: 180px">
            <el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="status" clearable placeholder="全部" style="width: 140px">
            <el-option label="草稿" value="DRAFT" />
            <el-option label="在售" value="ON_SALE" />
            <el-option label="下架" value="OFF_SALE" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
        </el-form-item>
      </el-form>
    </div>

    <el-alert
      v-if="errorMessage"
      :title="errorMessage"
      type="error"
      show-icon
      :closable="false"
      class="page-alert"
    />

    <div class="table-card glass-panel">
      <el-table v-loading="loading" :data="products">
        <el-table-column label="封面" width="80">
          <template #default="{ row }">
            <el-image v-if="row.coverUrl" :src="row.coverUrl" fit="cover" class="cover-thumb" />
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="名称" min-width="140" />
        <el-table-column label="分类" min-width="100">
          <template #default="{ row }">{{ categoryName(row.categoryId) }}</template>
        </el-table-column>
        <el-table-column label="价格" width="110">
          <template #default="{ row }">
            <span class="font-mono price-text">{{ formatFen(row.priceFen) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="发卡类型" width="110">
          <template #default="{ row }">{{ deliveryTypeLabel(row.deliveryType) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <FoilBadge
              :label="productStatusLabel(row.status)"
              :tone="row.status === 'ON_SALE' ? 'cyan' : row.status === 'DRAFT' ? 'mute' : 'warn'"
            />
          </template>
        </el-table-column>
        <el-table-column label="可售" width="80">
          <template #default="{ row }">
            <span class="font-mono">{{ row.availableCount }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="openEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无商品" />
        </template>
      </el-table>
      <div v-if="total > 0" class="page-pagination">
        <el-pagination
          :current-page="page"
          :page-size="20"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="onPageChange"
        />
      </div>
    </div>
  </motion.div>

  <el-dialog v-model="dialogVisible" :title="form.id ? '编辑商品' : '新建商品'" width="560px" destroy-on-close>
    <el-form label-width="90px">
      <el-form-item label="分类">
        <el-select v-model="form.categoryId" style="width: 240px">
          <el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="名称">
        <el-input v-model="form.name" maxlength="128" />
      </el-form-item>
      <el-form-item label="封面 URL">
        <el-input v-model="form.coverUrl" maxlength="512" placeholder="https://" />
      </el-form-item>
      <el-form-item label="价格(分)">
        <div class="price-row">
          <el-input-number v-model="form.priceFen" :min="0" :step="100" />
          <span class="price-preview font-mono">{{ formatFen(form.priceFen) }}</span>
        </div>
      </el-form-item>
      <el-form-item label="交付">
        <el-select v-model="form.deliveryType" style="width: 180px">
          <el-option label="卡密" value="LICENSE" />
          <el-option label="账号" value="ACCOUNT" />
          <el-option label="额度" value="TOKEN" />
          <el-option label="文本" value="TEXT" />
          <el-option label="节点订阅" value="NODE_SUBSCRIPTION" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="form.status" style="width: 180px">
          <el-option label="草稿" value="DRAFT" />
          <el-option label="在售" value="ON_SALE" />
          <el-option label="下架" value="OFF_SALE" />
        </el-select>
      </el-form-item>
      <el-form-item label="描述">
        <div class="desc-editor">
          <div class="desc-toolbar">
            <el-upload
              :show-file-list="false"
              :http-request="onUploadImage"
              accept="image/png,image/jpeg,image/webp,image/gif"
            >
              <el-button size="small" :loading="uploadingImage">插入图片</el-button>
            </el-upload>
            <span class="desc-hint">支持 Markdown：**加粗**、## 标题、![图片](链接)；图片可直接上传</span>
          </div>
          <el-input
            ref="descInputRef"
            v-model="form.description"
            type="textarea"
            :rows="8"
            class="desc-input font-mono"
          />
          <div v-if="form.description.trim()" class="desc-preview md" v-html="descriptionPreview"></div>
        </div>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.desc-editor {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.desc-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
}

.desc-hint {
  color: var(--mute);
  font-size: 12px;
}

.desc-input :deep(textarea) {
  font-family: var(--font-mono);
  font-size: 13px;
  line-height: 1.7;
}

.desc-preview {
  padding: 14px 16px;
  border-radius: 12px;
  border: 1px dashed var(--line-strong);
  background: var(--surface);
  max-height: 260px;
  overflow-y: auto;
}
.page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
}

.page-title {
  margin: 0;
  font-family: var(--font-display);
  font-size: 24px;
  letter-spacing: 0.02em;
}

.page-desc {
  margin: 4px 0 0;
  color: var(--mute);
  font-size: 13.5px;
}

.filter-bar {
  padding: 6px 16px 0;
}

.filter-bar :deep(.el-form-item) {
  margin-bottom: 6px;
}

.page-alert {
  border-radius: 12px;
}

.table-card {
  overflow: hidden;
}

.table-card :deep(.el-table::before) {
  display: none;
}

.page-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
  padding-bottom: 16px;
}

.cover-thumb {
  width: 48px;
  height: 48px;
  border-radius: 8px;
}

.price-text {
  color: var(--cyan-soft);
  font-weight: 600;
}

.price-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.price-preview {
  color: var(--amber);
  font-weight: 600;
}
</style>
