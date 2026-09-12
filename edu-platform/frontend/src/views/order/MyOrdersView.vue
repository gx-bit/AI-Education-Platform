<template>
  <div class="orders-view">
    <h2 class="page-title">我的订单</h2>
    <el-card shadow="never" class="filter-card">
      <div class="filter-row">
        <el-select v-model="status" placeholder="订单状态" clearable @change="loadOrders">
          <el-option label="待支付" :value="0" />
          <el-option label="已支付" :value="1" />
          <el-option label="已取消" :value="2" />
          <el-option label="已退款" :value="3" />
        </el-select>
      </div>
    </el-card>

    <div v-if="loading">
      <el-skeleton :rows="4" animated v-for="i in 3" :key="i" style="margin-bottom:16px" />
    </div>

    <el-empty v-else-if="!orders.length" description="暂无订单记录" />

    <div v-else class="order-list">
      <el-card v-for="order in orders" :key="order.id" class="order-card" shadow="hover">
        <div class="order-header">
          <span class="order-no">订单号：{{ order.orderNo }}</span>
          <el-tag :type="statusType(order.status)" size="small">{{ statusLabel(order.status) }}</el-tag>
        </div>
        <div class="order-body">
          <el-image :src="order.courseCover" fit="cover" style="width:120px;height:68px;border-radius:6px;flex-shrink:0" />
          <div class="order-info">
            <div class="course-title">{{ order.courseTitle }}</div>
            <div class="order-time">下单时间：{{ formatTime(order.createdAt) }}</div>
            <div v-if="order.paidAt" class="order-time">支付时间：{{ formatTime(order.paidAt) }}</div>
          </div>
          <div class="order-right">
            <div class="amount">¥{{ order.amount }}</div>
            <div class="actions">
              <el-button v-if="order.status === 0" type="primary" size="small" @click="handlePay(order)">立即支付</el-button>
              <el-button v-if="order.status === 0" size="small" @click="handleCancel(order)">取消</el-button>
              <el-button v-if="order.status === 1" link size="small" @click="router.push(`/courses/${order.courseId}`)">去学习</el-button>
            </div>
          </div>
        </div>
      </el-card>
    </div>

    <div class="pagination-wrap">
      <el-pagination v-model:current-page="pageNum" :page-size="pageSize" :total="total"
        layout="total, prev, pager, next" background @current-change="loadOrders" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { orderApi } from '@/api/order'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'

const router = useRouter()
const orders = ref([])
const loading = ref(false)
const status = ref(undefined)
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

const statusMap = { 0: { label: '待支付', type: 'warning' }, 1: { label: '已支付', type: 'success' }, 2: { label: '已取消', type: 'info' }, 3: { label: '已退款', type: 'danger' } }
const statusLabel = s => statusMap[s]?.label || '未知'
const statusType = s => statusMap[s]?.type || ''
const formatTime = t => dayjs(t).format('YYYY-MM-DD HH:mm')

async function loadOrders() {
  loading.value = true
  try {
    const params = { pageNum: pageNum.value, pageSize: pageSize.value }
    if (status.value !== undefined && status.value !== null) params.status = status.value
    const res = await orderApi.getMyOrders(params)
    orders.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

async function handlePay(order) {
  if (Number(order.amount) === 0) {
    try {
      await orderApi.payOrder(order.id)
      ElMessage.success('已成功加入学习')
      loadOrders()
    } catch {}
    return
  }

  const paymentWindow = window.open('', '_blank')
  if (!paymentWindow) {
    ElMessage.error('浏览器阻止了支付窗口，请允许本站打开弹窗')
    return
  }
  try {
    const res = await orderApi.createAlipayPayment(order.id)
    localStorage.setItem('pendingPaymentOrderId', String(order.id))
    if (res.data.mode === 'mock') {
      paymentWindow.location.href = res.data.paymentUrl
    } else {
      paymentWindow.document.open()
      paymentWindow.document.write(res.data.paymentForm)
      paymentWindow.document.close()
    }
  } catch (e) {
    paymentWindow.close()
  }
}

async function handleCancel(order) {
  try {
    await ElMessageBox.confirm('确定要取消该订单吗？', '提示', { type: 'warning' })
    await orderApi.cancelByCourse(order.courseId) // 使用课程ID取消
    ElMessage.success('订单已取消')
    loadOrders()
  } catch (e) {
    // 忽略取消确认的取消异常
  }
}



onMounted(loadOrders)
</script>

<style scoped>
.orders-view { padding-bottom: 40px; }
.page-title { font-size: 22px; margin-bottom: 20px; }
.filter-card { margin-bottom: 20px; border-radius: 12px; }
.filter-row { display: flex; gap: 12px; }
.order-list { display: flex; flex-direction: column; gap: 16px; margin-bottom: 24px; }
.order-card { border-radius: 12px; }
.order-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; padding-bottom: 12px; border-bottom: 1px solid #f0f0f0; }
.order-no { color: #909399; font-size: 13px; }
.order-body { display: flex; gap: 16px; align-items: flex-start; }
.order-info { flex: 1; }
.course-title { font-weight: 600; margin-bottom: 8px; }
.order-time { color: #909399; font-size: 12px; margin-bottom: 4px; }
.order-right { text-align: right; }
.amount { font-size: 20px; font-weight: 700; color: #f56c6c; margin-bottom: 8px; }
.pagination-wrap { display: flex; justify-content: flex-end; }
</style>
