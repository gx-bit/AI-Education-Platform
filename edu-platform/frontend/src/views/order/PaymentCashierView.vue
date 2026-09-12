<template>
  <div class="cashier-page">
    <el-card class="cashier-card" shadow="never">
      <div class="brand"><span class="brand-icon">支</span><div><h1>本地沙箱收银台</h1><p>用于开发环境完整演示支付流程，不会产生真实扣款</p></div></div>
      <el-alert title="模拟支付环境" description="确认后订单会更新为已支付。接入支付宝商户密钥后，系统会自动使用真实支付宝收银台。" type="warning" :closable="false" show-icon />
      <div class="order-info">
        <div><span>商品</span><strong>{{ title }}</strong></div>
        <div><span>订单号</span><strong>{{ orderNo }}</strong></div>
        <div class="amount-row"><span>应付金额</span><strong>¥{{ amount }}</strong></div>
      </div>
      <el-button type="primary" size="large" class="pay-button" :loading="paying" @click="confirmPay">确认支付</el-button>
      <el-button size="large" class="cancel-button" @click="closeWindow">取消支付</el-button>
      <p class="secure-tip">🔒 支付请求仅用于本机开发演示</p>
    </el-card>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { orderApi } from '@/api/order'

const route = useRoute()
const router = useRouter()
const paying = ref(false)
// Snowflake IDs exceed JavaScript's safe integer range; keep the ID as a string.
const orderId = computed(() => String(route.query.orderId || ''))
const orderNo = computed(() => route.query.orderNo || '-')
const amount = computed(() => Number(route.query.amount || 0).toFixed(2))
const title = computed(() => route.query.title || '在线课程')

async function confirmPay() {
  if (!/^\d+$/.test(orderId.value)) return ElMessage.error('订单信息无效')
  paying.value = true
  try {
    await orderApi.confirmMockPayment(orderId.value)
    localStorage.setItem('pendingPaymentOrderId', String(orderId.value))
    ElMessage.success('支付成功')
    router.replace('/payment/result')
  } finally {
    paying.value = false
  }
}

function closeWindow() {
  window.close()
  if (!window.closed) router.replace('/orders')
}
</script>

<style scoped>
.cashier-page { min-height: calc(100vh - 150px); display: grid; place-items: center; padding: 40px 16px; background: linear-gradient(145deg, #f2f8ff, #f8fbff); }
.cashier-card { width: min(520px, 100%); border: 0; border-radius: 20px; box-shadow: 0 20px 60px rgba(31, 102, 180, .12); }
.brand { display: flex; align-items: center; gap: 14px; margin-bottom: 22px; }
.brand-icon { display: grid; place-items: center; width: 48px; height: 48px; border-radius: 14px; color: white; font-size: 25px; font-weight: 700; background: #1677ff; }
h1 { margin: 0 0 5px; font-size: 23px; color: #1f2d3d; }
.brand p { margin: 0; color: #8492a6; font-size: 13px; }
.order-info { margin: 26px 0; padding: 8px 20px; border-radius: 14px; background: #f7f9fc; }
.order-info div { display: flex; justify-content: space-between; gap: 20px; padding: 13px 0; border-bottom: 1px solid #e9edf3; }
.order-info div:last-child { border: 0; }
.order-info span { color: #8492a6; }
.order-info strong { max-width: 70%; text-align: right; color: #303133; }
.amount-row strong { color: #f56c6c; font-size: 24px; }
.pay-button, .cancel-button { width: 100%; margin: 0 0 12px; }
.cancel-button { margin-left: 0; }
.secure-tip { margin: 6px 0 0; text-align: center; color: #a0a9b8; font-size: 12px; }
</style>
