<script setup lang="ts">
import { RouterLink } from 'vue-router'
import { formatFen } from '../../utils/money'

defineProps<{
  amountFen: number
  alipayEnabled: boolean
  paying: boolean
  confirming: boolean
  cashierOpen: boolean
  waitingResult: boolean
  errorMessage: string
  orderId: number
}>()

const emit = defineEmits<{
  pay: []
  confirm: []
}>()
</script>

<template>
  <aside class="desk glass-panel">
    <div class="desk-channel">
      <span class="desk-alipay-mark">支</span>
      <div>
        <p class="desk-kicker">付款方式</p>
        <h2 class="desk-title">支付宝电脑网站支付</h2>
      </div>
    </div>

    <p class="desk-amount-label">应付金额</p>
    <p class="desk-amount font-mono">{{ formatFen(amountFen) }}</p>
    <p class="desk-amount-hint">人民币 · 与支付宝收银台金额一致</p>

    <div class="desk-qr">
      <iframe
        v-show="cashierOpen"
        name="alipayCashier"
        class="desk-frame"
        title="支付宝付款码"
      />
      <div v-if="!cashierOpen" class="desk-qr-empty">
        <p>点击下方按钮生成支付宝付款码</p>
        <p>请用支付宝扫码，付款后立刻出卡</p>
      </div>
    </div>

    <ul class="desk-notes">
      <li>收款方：钥市（开放平台正式应用）</li>
      <li>商品为虚拟库存，无物流，付款成功后自动发放卡密</li>
      <li>请核对金额后再扫码；未确认到账前不要重复付款</li>
      <li>同步跳回本页后会自动向支付宝查询结果</li>
    </ul>

    <p v-if="errorMessage" class="desk-error">{{ errorMessage }}</p>
    <p v-if="!alipayEnabled" class="desk-warn">
      支付宝尚未配置。请把正式环境 AppId、PKCS#8 应用私钥、支付宝公钥写入本机
      <code>.env</code>，不要发到聊天里。
    </p>
    <p v-if="waitingResult" class="desk-wait">
      {{ confirming ? '正在向支付宝查询支付结果…' : '若尚未到账，请稍候或点下方再查一次。' }}
    </p>

    <div class="desk-actions">
      <el-button type="primary" size="large" :loading="paying" :disabled="!alipayEnabled" @click="emit('pay')">
        {{ cashierOpen ? '刷新支付宝付款码' : '生成支付宝付款码' }}
      </el-button>
      <el-button size="large" :loading="confirming" @click="emit('confirm')">我已付款，查询结果</el-button>
      <RouterLink class="desk-back" :to="`/orders/${orderId}`">返回订单</RouterLink>
    </div>
  </aside>
</template>

<style scoped>
.desk {
  padding: 24px 24px 26px;
}

.desk-channel {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 18px;
}

.desk-alipay-mark {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: #1677ff;
  color: #fff;
  font-weight: 800;
}

.desk-kicker {
  margin: 0 0 4px;
  color: #69b1ff;
  font-size: 12px;
  letter-spacing: 0.14em;
}

.desk-title {
  margin: 0;
  font-size: 18px;
  font-weight: 800;
}

.desk-amount-label {
  margin: 0;
  color: var(--mute);
  font-size: 13px;
}

.desk-amount {
  margin: 4px 0 0;
  font-size: 34px;
  font-weight: 800;
  color: var(--cyan-soft);
}

.desk-amount-hint {
  margin: 4px 0 16px;
  color: var(--mute);
  font-size: 12.5px;
}

.desk-qr {
  min-height: 320px;
  border-radius: 14px;
  overflow: hidden;
  background: #fff;
  border: 1px solid var(--line);
}

.desk-frame {
  display: block;
  width: 100%;
  height: 360px;
  border: 0;
  background: #fff;
}

.desk-qr-empty {
  display: grid;
  place-content: center;
  gap: 6px;
  min-height: 320px;
  padding: 24px;
  text-align: center;
  color: #4b5563;
  font-size: 14px;
}

.desk-notes {
  margin: 16px 0 0;
  padding-left: 18px;
  color: var(--ink-soft);
  font-size: 13px;
  line-height: 1.7;
}

.desk-error {
  color: var(--red);
}

.desk-warn,
.desk-wait {
  color: var(--ink-soft);
  line-height: 1.7;
}

.desk-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-top: 18px;
}

.desk-back {
  color: var(--mute);
  text-decoration: none;
  font-size: 14px;
}
</style>
