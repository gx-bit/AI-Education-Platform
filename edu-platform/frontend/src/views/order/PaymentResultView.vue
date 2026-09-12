<template>
  <div class="payment-result">
    <el-result
      :icon="state === 'paid' ? 'success' : state === 'failed' ? 'error' : 'info'"
      :title="title"
      :sub-title="subtitle"
    >
      <template #extra>
        <el-button type="primary" @click="router.push('/orders')">查看我的订单</el-button>
        <el-button @click="checkStatus">刷新状态</el-button>
      </template>
    </el-result>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { orderApi } from '@/api/order'

const router = useRouter()
const state = ref('checking')
const attempts = ref(0)
let timer

const title = computed(() => state.value === 'paid' ? '支付成功' : state.value === 'failed' ? '暂未确认支付' : '正在确认支付结果')
const subtitle = computed(() => state.value === 'paid'
  ? '课程已经加入你的学习列表。'
  : '支付状态以支付宝服务器异步通知为准，请勿重复支付。')

async function checkStatus() {
  const orderId = localStorage.getItem('pendingPaymentOrderId')
  if (!orderId) { state.value = 'failed'; return }
  try {
    const res = await orderApi.getPaymentStatus(orderId)
    if (res.data?.status === 1) {
      state.value = 'paid'
      localStorage.removeItem('pendingPaymentOrderId')
      clearInterval(timer)
    } else if (++attempts.value >= 15) {
      state.value = 'failed'
      clearInterval(timer)
    }
  } catch {
    if (++attempts.value >= 15) state.value = 'failed'
  }
}

onMounted(() => { checkStatus(); timer = setInterval(checkStatus, 2000) })
onUnmounted(() => clearInterval(timer))
</script>

<style scoped>
.payment-result { max-width: 720px; margin: 60px auto; background: #fff; border-radius: 12px; padding: 24px; }
</style>
