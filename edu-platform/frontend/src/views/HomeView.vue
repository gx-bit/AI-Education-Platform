<template>
  <div class="home-view">
    <section class="banner">
      <div class="banner-content">
        <h1>AI 课程推荐演示平台</h1>
        <p>基于本地课程库，为学习兴趣、当前水平和目标生成可解释的课程推荐。</p>
        <div class="banner-search">
          <el-input v-model="searchKeyword" placeholder="搜索课程..." size="large" clearable @keyup.enter="goSearch">
            <template #append>
              <el-button type="primary" @click="goSearch">搜索</el-button>
            </template>
          </el-input>
        </div>
        <div class="banner-stats">
          <div class="stat-item"><span class="num">8+</span><span>课程</span></div>
          <div class="stat-item"><span class="num">6</span><span>分类</span></div>
          <div class="stat-item"><span class="num">AI</span><span>推荐</span></div>
        </div>
      </div>
    </section>

    <section class="section recommend-section">
      <div class="section-title">
        <h2>
          <el-icon color="#409eff"><MagicStick /></el-icon>
          AI 智能推荐
        </h2>
      </div>

      <el-form class="recommend-form" :model="recommendForm" label-position="top">
        <el-form-item label="学习兴趣">
          <el-input v-model="recommendForm.interest" placeholder="例如：Java 后端、前端开发、数据分析" clearable />
        </el-form-item>
        <el-form-item label="当前水平">
          <el-select v-model="recommendForm.level" placeholder="请选择">
            <el-option label="入门" value="beginner" />
            <el-option label="进阶" value="intermediate" />
            <el-option label="高级" value="advanced" />
          </el-select>
        </el-form-item>
        <el-form-item label="学习目标">
          <el-input v-model="recommendForm.goal" placeholder="例如：完成课设、准备实习、系统入门" clearable />
        </el-form-item>
        <el-form-item label="推荐数量">
          <el-input-number v-model="recommendForm.limit" :min="1" :max="6" />
        </el-form-item>
        <el-form-item class="recommend-action">
          <el-button type="primary" :loading="aiLoading" @click="refreshRecommend">
            生成推荐
          </el-button>
        </el-form-item>
      </el-form>

      <div v-if="aiLoading" class="skeleton-row">
        <el-skeleton v-for="i in recommendForm.limit" :key="i" :rows="4" animated style="width:280px" />
      </div>
      <div v-else class="course-row">
        <CourseCard v-for="c in courseStore.aiRecommendations" :key="c.id" :course="c" />
      </div>
    </section>

    <section class="section">
      <div class="section-title">
        <h2>课程分类</h2>
        <router-link to="/courses">查看全部</router-link>
      </div>
      <div class="category-grid">
        <button v-for="cat in courseStore.categories" :key="cat.id" class="category-card" @click="goCategory(cat.id)">
          <span class="cat-icon">{{ cat.icon }}</span>
          <span class="cat-name">{{ cat.name }}</span>
        </button>
      </div>
    </section>

    <section class="section">
      <div class="section-title">
        <h2>热门课程</h2>
        <router-link to="/courses">查看全部</router-link>
      </div>
      <div v-if="courseStore.loading" class="skeleton-row">
        <el-skeleton v-for="i in 4" :key="i" :rows="4" animated style="width:280px" />
      </div>
      <div v-else class="course-row">
        <CourseCard v-for="c in courseStore.courseList" :key="c.id" :course="c" />
      </div>
    </section>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useCourseStore } from '@/stores/course'
import CourseCard from '@/components/common/CourseCard.vue'

const router = useRouter()
const courseStore = useCourseStore()
const searchKeyword = ref('')
const aiLoading = ref(false)
const recommendForm = reactive({
  interest: '',
  level: 'beginner',
  goal: '',
  limit: 3
})

function goSearch() {
  router.push({ path: '/courses', query: { keyword: searchKeyword.value } })
}

function goCategory(id) {
  router.push({ path: '/courses', query: { categoryId: id } })
}

async function refreshRecommend() {
  aiLoading.value = true
  try {
    await courseStore.fetchAiRecommendations({ ...recommendForm })
  } finally {
    aiLoading.value = false
  }
}

onMounted(async () => {
  await Promise.all([
    courseStore.fetchCategories(),
    courseStore.fetchCourseList({ pageNum: 1, pageSize: 8, sortBy: 'studentCount' }),
    refreshRecommend()
  ])
})
</script>

<style scoped>
.home-view { padding-bottom: 40px; }
.banner {
  background: linear-gradient(135deg, #153243 0%, #284b63 55%, #3c6e71 100%);
  border-radius: 14px;
  padding: 48px 40px;
  color: #fff;
  margin-bottom: 32px;
  text-align: center;
}
.banner h1 { font-size: 36px; font-weight: 700; margin-bottom: 12px; }
.banner p { font-size: 16px; color: rgba(255,255,255,.82); margin-bottom: 28px; }
.banner-search { max-width: 600px; margin: 0 auto 28px; }
.banner-stats { display: flex; justify-content: center; gap: 48px; }
.stat-item { display: flex; flex-direction: column; align-items: center; gap: 4px; }
.stat-item .num { font-size: 26px; font-weight: 700; color: #ffd166; }
.stat-item span:last-child { font-size: 13px; color: rgba(255,255,255,.72); }
.section { margin-bottom: 36px; }
.section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
}
.section-title h2 { display: flex; align-items: center; gap: 8px; font-size: 20px; }
.section-title a { color: #409eff; text-decoration: none; font-size: 14px; }
.recommend-section {
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 10px;
  padding: 20px;
}
.recommend-form {
  display: grid;
  grid-template-columns: minmax(180px, 1.3fr) minmax(140px, .8fr) minmax(180px, 1.3fr) 120px auto;
  gap: 14px;
  align-items: end;
  margin-bottom: 20px;
}
.recommend-form :deep(.el-form-item) { margin-bottom: 0; }
.recommend-action { min-width: 96px; }
.category-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 12px;
}
.category-card {
  border: 0;
  background: #fff;
  border-radius: 10px;
  padding: 18px 12px;
  text-align: center;
  cursor: pointer;
  transition: all .2s;
  box-shadow: 0 2px 8px rgba(0,0,0,.06);
}
.category-card:hover { transform: translateY(-3px); box-shadow: 0 8px 24px rgba(64,158,255,.18); }
.cat-icon { display: block; font-size: 28px; margin-bottom: 8px; }
.cat-name { font-size: 13px; color: #303133; }
.course-row { display: flex; gap: 20px; flex-wrap: wrap; }
.skeleton-row { display: flex; gap: 20px; flex-wrap: wrap; }
@media (max-width: 900px) {
  .recommend-form { grid-template-columns: 1fr; }
  .category-grid { grid-template-columns: repeat(2, 1fr); }
  .banner { padding: 36px 20px; }
  .banner-stats { gap: 24px; }
}
</style>
