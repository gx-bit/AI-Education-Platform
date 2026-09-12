<template>
  <div class="course-card" @click="goDetail(course.id)">
    <div class="cover-wrap">
      <el-image :src="course.coverImage" fit="cover" class="cover-img" lazy>
        <template #error>
          <div class="cover-placeholder">
            <el-icon size="32" color="#c0c4cc"><Picture /></el-icon>
          </div>
        </template>
      </el-image>
      <el-tag class="level-tag" :type="levelType(course.level)" size="small">
        {{ levelLabel(course.level) }}
      </el-tag>
    </div>
    <div class="card-body">
      <div class="course-title" :title="course.title">{{ course.title }}</div>
      <div v-if="course.recommendReason" class="recommend-box">
        <div class="recommend-score">匹配度 {{ course.matchScore || 88 }}%</div>
        <div class="recommend-reason">{{ course.recommendReason }}</div>
      </div>
      <div class="course-teacher">
        <el-icon><Avatar /></el-icon>{{ course.teacherName }}
      </div>
      <div class="course-meta">
        <el-rate :model-value="course.rating" disabled text-color="#f7ba2a"
          :score-template="course.rating + ''" size="small" />
        <span class="student-count">{{ formatCount(course.studentCount) }}人学习</span>
      </div>
      <div class="course-footer">
        <span class="price">
          <template v-if="course.price > 0">¥{{ course.price }}</template>
          <template v-else><span style="color:#67c23a">免费</span></template>
        </span>

        <!-- 根据登录状态和购买状态显示不同按钮 -->
        <template v-if="userStore.isLoggedIn">
          <!-- 已购买：显示观看课程（使用后端重定向，避免风控） -->
          <el-button
            v-if="isPaid"
            type="success"
            size="small"
            @click.stop="watchCourse(course.id)"
          >
            🎬 观看课程
          </el-button>
          <!-- 未购买但有课程链接：显示立即学习 -->
          <el-button
            v-else-if="course.linkUrl"
            type="primary"
            size="small"
            @click.stop="handleEnroll(course)"
          >
            立即学习
          </el-button>
          <!-- 没有链接的情况 -->
          <span v-else style="font-size:12px; color:#909399">暂无课程链接</span>
        </template>
        <!-- 未登录：显示登录按钮 -->
        <el-button v-else type="primary" size="small" @click.stop="router.push('/auth/login')">
          登录后学习
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { orderApi } from '@/api/order'
import { courseApi } from '@/api/course'
import { ElMessage } from 'element-plus'

const props = defineProps({ course: { type: Object, required: true } })
const router = useRouter()
const userStore = useUserStore()

const isPaid = ref(false)

// 级别映射
const levelMap = {
  beginner: { label: '入门', type: 'success' },
  intermediate: { label: '中级', type: 'warning' },
  advanced: { label: '高级', type: 'danger' }
}
const levelLabel = l => levelMap[l]?.label || l
const levelType = l => levelMap[l]?.type || ''
const formatCount = n => n >= 1000 ? (n / 1000).toFixed(1) + 'k' : n

// 检查用户是否已购买该课程
async function checkPaid() {
  if (userStore.isLoggedIn && props.course.id) {
    try {
      const res = await orderApi.checkPurchased(props.course.id)
      isPaid.value = res.data && res.data.paid
    } catch {
      isPaid.value = false
    }
  }
}

// 创建订单并支付（免费课自动支付）
async function handleEnroll(course) {
  if (!userStore.isLoggedIn) {
    router.push('/auth/login')
    return
  }
  try {
    const res = await orderApi.createOrder({ courseId: course.id })
    recordBehavior('order')
    const orderId = res.data.id
    if (course.price === 0) {
      await orderApi.payOrder(orderId)
      ElMessage.success('已成功加入学习')
      isPaid.value = true   // 免费课支付后立即更新状态
      recordBehavior('purchase')
    } else {
      router.push('/orders')
    }
  } catch {
    // 错误已在请求拦截器统一处理
  }
}

// 通过后端重定向打开课程链接，彻底避免B站风控
function watchCourse(courseId) {
  recordBehavior('start_learning')
  window.open(`/api/course/${courseId}/goto`, '_blank')
}

// 点击卡片跳转到课程详情页
function goDetail(courseId) {
  recordBehavior('click')
  router.push(`/courses/${courseId}`)
}

function recordBehavior(behaviorType) {
  courseApi.recordBehavior({
    courseId: props.course.id,
    behaviorType,
    requestId: props.course.recommendationRequestId,
    sessionId: localStorage.getItem('recommendationSessionId')
  }).catch(() => {})
}

onMounted(() => {
  checkPaid()
})
</script>

<style scoped>
/* 保持原有样式不变，以下为原样照抄 */
.course-card {
  width: 280px;
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: all .2s;
  box-shadow: 0 2px 8px rgba(0,0,0,.06);
  flex-shrink: 0;
}
.course-card:hover { transform: translateY(-4px); box-shadow: 0 12px 32px rgba(0,0,0,.12); }
.cover-wrap { position: relative; height: 157px; }
.cover-img { width: 100%; height: 100%; }
.cover-placeholder {
  width: 100%; height: 100%;
  display: flex; align-items: center; justify-content: center;
  background: #f5f7fa;
}
.level-tag { position: absolute; top: 8px; right: 8px; }
.card-body { padding: 12px; }
.course-title {
  font-weight: 600; font-size: 14px; line-height: 1.4;
  margin-bottom: 8px;
  display: -webkit-box; -webkit-line-clamp: 2; line-clamp: 2;
  -webkit-box-orient: vertical; overflow: hidden;
}
.recommend-box {
  background: #f0f7ff;
  border-left: 3px solid #409eff;
  border-radius: 6px;
  padding: 8px;
  margin-bottom: 10px;
}
.recommend-score { color: #1f6feb; font-size: 12px; font-weight: 600; margin-bottom: 4px; }
.recommend-reason {
  color: #606266;
  font-size: 12px;
  line-height: 1.4;
  display: -webkit-box; -webkit-line-clamp: 2; line-clamp: 2;
  -webkit-box-orient: vertical; overflow: hidden;
}
.course-teacher { color: #909399; font-size: 12px; display: flex; align-items: center; gap: 4px; margin-bottom: 8px; }
.course-meta { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.student-count { color: #909399; font-size: 12px; }
.course-footer { display: flex; align-items: center; justify-content: space-between; }
.price { font-size: 18px; font-weight: 700; color: #f56c6c; }
</style>
