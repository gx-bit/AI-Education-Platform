<template>
  <div class="course-manage">
    <div class="page-header">
      <h2>课程管理</h2>
      <el-button type="primary" @click="openDialog()">
        <el-icon><Plus /></el-icon>添加课程
      </el-button>
    </div>

    <el-card shadow="never" class="table-card">
      <el-table :data="courses" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="封面" width="100">
          <template #default="{ row }">
            <el-image :src="row.coverImage" fit="cover" style="width:80px;height:45px;border-radius:4px" />
          </template>
        </el-table-column>
        <el-table-column prop="title" label="课程名称" min-width="200" show-overflow-tooltip />
        <el-table-column prop="teacherName" label="讲师" width="100" />
        <el-table-column prop="price" label="价格" width="90">
          <template #default="{ row }">
            <span style="color:#f56c6c;font-weight:600">¥{{ row.price }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="studentCount" label="学员数" width="90" />
        <el-table-column prop="rating" label="评分" width="80" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link size="small" @click="openDialog(row)">编辑</el-button>
            <el-button v-if="row.status !== 1" link size="small" type="success" @click="publish(row)">发布</el-button>
            <el-button v-else link size="small" type="warning" @click="unpublish(row)">下架</el-button>
            <el-button link size="small" type="danger" @click="deleteCourse(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrap">
        <el-pagination v-model:current-page="pageNum" :page-size="pageSize" :total="total"
          layout="total, prev, pager, next" background @current-change="loadCourses" />
      </div>
    </el-card>

    <!-- 编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="editForm.id ? '编辑课程' : '添加课程'" width="720px">
      <el-form ref="formRef" :model="editForm" :rules="formRules" label-width="90px">
        <el-form-item label="课程名称" prop="title">
          <el-input v-model="editForm.title" />
        </el-form-item>
        <el-form-item label="课程描述" prop="description">
          <el-input v-model="editForm.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="讲师姓名" prop="teacherName">
            <el-input v-model="editForm.teacherName" placeholder="请输入课程讲师姓名" />
          </el-form-item></el-col>
          <el-col :span="12"><el-form-item label="课程链接" prop="linkUrl">
            <el-input v-model="editForm.linkUrl" placeholder="https://..." clearable />
          </el-form-item></el-col>
        </el-row>
        <el-form-item label="封面图片" prop="coverImage">
          <div class="cover-editor">
            <el-input v-model="editForm.coverImage" placeholder="请输入封面图片 URL" clearable />
            <el-image v-if="editForm.coverImage" :src="editForm.coverImage" fit="cover" class="cover-preview">
              <template #error><div class="cover-error">图片无法加载</div></template>
            </el-image>
          </div>
        </el-form-item>
        <el-form-item label="分类" prop="categoryId">
          <el-select v-model="editForm.categoryId" style="width:100%">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="价格" prop="price">
          <el-input-number v-model="editForm.price" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="时长(分钟)" prop="duration">
          <el-input-number v-model="editForm.duration" :min="1" />
        </el-form-item>
        <el-form-item label="难度" prop="level">
          <el-select v-model="editForm.level" style="width:100%">
            <el-option label="入门" value="beginner" />
            <el-option label="中级" value="intermediate" />
            <el-option label="高级" value="advanced" />
          </el-select>
        </el-form-item>
        <el-form-item label="标签">
          <el-input v-model="editForm.tags" placeholder="逗号分隔，如：Java,Spring" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saveLoading" @click="saveCourse">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { courseApi } from '@/api/course'
import { ElMessage, ElMessageBox } from 'element-plus'

const courses = ref([])
const categories = ref([])
const loading = ref(false)
const saveLoading = ref(false)
const dialogVisible = ref(false)
const formRef = ref()
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

const emptyForm = () => ({ id: null, title: '', description: '', teacherName: '', coverImage: '', linkUrl: '', categoryId: null, price: 0, duration: 60, level: 'beginner', tags: '' })
const editForm = reactive(emptyForm())

const statusMap = { 0: { label: '草稿', type: 'info' }, 1: { label: '已发布', type: 'success' }, 2: { label: '已下架', type: 'warning' } }
const statusLabel = s => statusMap[s]?.label || '未知'
const statusType = s => statusMap[s]?.type || ''

const formRules = {
  title: [{ required: true, message: '请输入课程名称', trigger: 'blur' }],
  teacherName: [{ required: true, message: '请输入讲师姓名', trigger: 'blur' }],
  linkUrl: [{ type: 'url', message: '请输入有效的课程链接', trigger: 'blur' }],
  coverImage: [{ type: 'url', message: '请输入有效的图片链接', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  level: [{ required: true, message: '请选择难度', trigger: 'change' }]
}

function openDialog(row = null) {
  Object.assign(editForm, emptyForm())
  if (row) Object.assign(editForm, row)
  dialogVisible.value = true
}

async function loadCourses() {
  loading.value = true
  try {
    const res = await courseApi.getAdminCourseList({ pageNum: pageNum.value, pageSize: pageSize.value })
    courses.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally { loading.value = false }
}

async function saveCourse() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saveLoading.value = true
  try {
    if (editForm.id) {
      await courseApi.updateCourse(editForm.id, editForm)
    } else {
      await courseApi.createCourse(editForm)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadCourses()
  } finally { saveLoading.value = false }
}

async function publish(row) {
  await courseApi.publishCourse(row.id)
  ElMessage.success('发布成功')
  loadCourses()
}

async function unpublish(row) {
  await courseApi.unpublishCourse(row.id)
  ElMessage.success('已下架')
  loadCourses()
}

async function deleteCourse(row) {
  await ElMessageBox.confirm(`确定删除课程"${row.title}"吗？`, '警告', { type: 'warning' })
  await courseApi.deleteCourse(row.id)
  ElMessage.success('删除成功')
  loadCourses()
}

onMounted(async () => {
  const res = await courseApi.getCategoryList()
  categories.value = res.data || []
  loadCourses()
})
</script>

<style scoped>
.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; }
.page-header h2 { font-size: 22px; }
.table-card { border-radius: 12px; }
.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 16px; }
.cover-editor { width:100%; display:grid; grid-template-columns:1fr 180px; gap:14px; align-items:start; }
.cover-preview { width:180px; height:100px; border-radius:8px; border:1px solid #ebeef5; }
.cover-error { width:100%; height:100%; display:grid; place-items:center; color:#909399; background:#f5f7fa; font-size:12px; }
</style>
