<template>
  <section>
    <div class="page-header">
      <div><h2>我的收藏</h2><p>保存感兴趣的课程，推荐系统也会据此理解你的偏好。</p></div>
      <el-button @click="$router.push('/courses')">发现更多课程</el-button>
    </div>
    <div v-loading="loading" class="course-grid">
      <CourseCard v-for="course in courses" :key="course.id" :course="course" />
    </div>
    <el-empty v-if="!loading && !courses.length" description="还没有收藏课程" />
    <el-pagination v-if="total > size" v-model:current-page="page" :page-size="size"
      :total="total" layout="prev, pager, next" @current-change="load" />
  </section>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { courseApi } from '@/api/course'
import CourseCard from '@/components/common/CourseCard.vue'

const courses = ref([])
const loading = ref(false)
const page = ref(1)
const size = 12
const total = ref(0)

async function load() {
  loading.value = true
  try {
    const res = await courseApi.getFavorites({ page: page.value, size })
    courses.value = res.data.records
    total.value = res.data.total
  } finally { loading.value = false }
}
onMounted(load)
</script>

<style scoped>
.page-header { display:flex; justify-content:space-between; align-items:center; margin-bottom:24px; }
.page-header h2 { margin:0 0 8px; }
.page-header p { margin:0; color:#909399; }
.course-grid { display:grid; grid-template-columns:repeat(auto-fill,minmax(260px,1fr)); gap:20px; min-height:180px; }
.course-grid :deep(.course-card) { width:100%; }
.el-pagination { justify-content:center; margin-top:28px; }
</style>
