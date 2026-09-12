<template>
  <div class="course-list-view">
    <!-- 筛选栏 -->
    <el-card class="filter-card" shadow="never">
      <div class="filter-row">
        <el-input v-model="filters.keyword" placeholder="搜索课程名称..." clearable
          prefix-icon="Search" style="width:280px" @change="handleSearch" />
        <el-select v-model="filters.categoryId" placeholder="课程分类" clearable @change="handleSearch">
          <el-option v-for="c in courseStore.categories" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
        <el-select v-model="filters.level" placeholder="难度" clearable @change="handleSearch">
          <el-option label="入门" value="beginner" />
          <el-option label="中级" value="intermediate" />
          <el-option label="高级" value="advanced" />
        </el-select>
        <el-select v-model="filters.sortBy" placeholder="排序" @change="handleSearch">
          <el-option label="最热门" value="studentCount" />
          <el-option label="最新" value="createdAt" />
          <el-option label="评分最高" value="rating" />
          <el-option label="价格最低" value="priceAsc" />
        </el-select>
        <el-button @click="resetFilters">重置</el-button>
      </div>
    </el-card>

    <!-- 课程列表 -->
    <div class="list-header">
      <span class="total-text">共 {{ courseStore.total }} 门课程</span>
    </div>

    <div v-if="courseStore.loading" class="course-grid loading">
      <el-skeleton v-for="i in 9" :key="i" animated>
        <template #template>
          <el-skeleton-item variant="image" style="height:157px;border-radius:8px 8px 0 0" />
          <div style="padding:12px">
            <el-skeleton-item variant="p" style="width:80%" />
            <el-skeleton-item variant="p" style="width:50%;margin-top:8px" />
          </div>
        </template>
      </el-skeleton>
    </div>

    <div v-else-if="courseStore.courseList.length" class="course-grid">
      <CourseCard v-for="c in courseStore.courseList" :key="c.id" :course="c" />
    </div>

    <el-empty v-else description="没有找到相关课程" :image-size="120" />

    <!-- 分页 -->
    <div class="pagination-wrap">
      <el-pagination
        v-model:current-page="filters.pageNum"
        v-model:page-size="filters.pageSize"
        :total="courseStore.total"
        :page-sizes="[9, 18, 27]"
        layout="total, sizes, prev, pager, next"
        background
        @change="loadCourses"
      />
    </div>
  </div>
</template>

<script setup>
import { reactive, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useCourseStore } from '@/stores/course'
import CourseCard from '@/components/common/CourseCard.vue'

const route = useRoute()
const router = useRouter()
const courseStore = useCourseStore()

const filters = reactive({
  keyword: route.query.keyword || '',
  categoryId: route.query.categoryId ? Number(route.query.categoryId) : undefined,
  level: '',
  sortBy: 'studentCount',
  pageNum: 1,
  pageSize: 9
})

function handleSearch() {
  filters.pageNum = 1
  loadCourses()
}

function resetFilters() {
  Object.assign(filters, { keyword: '', categoryId: undefined, level: '', sortBy: 'studentCount', pageNum: 1 })
  loadCourses()
}

function loadCourses() {
  const params = { ...filters }
  if (!params.categoryId) delete params.categoryId
  if (!params.level) delete params.level
  courseStore.fetchCourseList(params)
}

onMounted(() => {
  courseStore.fetchCategories()
  loadCourses()
})
</script>

<style scoped>
.course-list-view { padding-bottom: 40px; }
.filter-card { margin-bottom: 20px; border-radius: 12px; }
.filter-row { display: flex; gap: 12px; align-items: center; flex-wrap: wrap; }
.list-header { margin-bottom: 16px; }
.total-text { color: #909399; font-size: 14px; }
.course-grid {
  display: grid;
  grid-template-columns: repeat(3, 280px);
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 24px;
}
.course-grid.loading { opacity: .6; }
.pagination-wrap { display: flex; justify-content: flex-end; }
@media (max-width: 950px) { .course-grid { grid-template-columns: repeat(2, 280px); justify-content:start; } }
@media (max-width: 620px) { .course-grid { grid-template-columns: 1fr; } }
</style>
