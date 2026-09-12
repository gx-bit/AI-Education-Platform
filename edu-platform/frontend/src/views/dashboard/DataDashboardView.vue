<template>
  <div class="data-dashboard">
    <h2 class="page-title">数据统计</h2>

    <el-row :gutter="20">
      <!-- 订单趋势图 -->
      <el-col :span="16">
        <el-card shadow="never" class="chart-card">
          <template #header><span>近30天订单趋势</span></template>
          <div ref="orderChartRef" style="height:320px"></div>
        </el-card>
      </el-col>

      <!-- 课程分类分布 -->
      <el-col :span="8">
        <el-card shadow="never" class="chart-card">
          <template #header><span>课程分类分布</span></template>
          <div ref="categoryChartRef" style="height:320px"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top:20px">
      <!-- 热门课程排行 -->
      <el-col :span="12">
        <el-card shadow="never" class="chart-card">
          <template #header><span>热门课程学员数量</span></template>
          <div ref="hotCourseChartRef" style="height:300px"></div>
        </el-card>
      </el-col>

      <!-- 用户角色分布 -->
      <el-col :span="12">
        <el-card shadow="never" class="chart-card">
          <template #header><span>用户角色分布</span></template>
          <div ref="userRoleChartRef" style="height:300px"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts/core'
import { BarChart, LineChart, PieChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([BarChart, LineChart, PieChart, GridComponent, LegendComponent, TooltipComponent, CanvasRenderer])

const orderChartRef = ref()
const categoryChartRef = ref()
const hotCourseChartRef = ref()
const userRoleChartRef = ref()

let charts = []

function initOrderChart(el) {
  const chart = echarts.init(el)
  const days = Array.from({ length: 30 }, (_, i) => {
    const d = new Date()
    d.setDate(d.getDate() - (29 - i))
    return `${d.getMonth() + 1}/${d.getDate()}`
  })
  const values = days.map(() => Math.floor(Math.random() * 50 + 10))
  chart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: days, axisLabel: { rotate: 45, fontSize: 10 } },
    yAxis: { type: 'value', name: '订单数' },
    series: [{
      name: '订单数', type: 'line', data: values,
      smooth: true, areaStyle: { opacity: .2 },
      lineStyle: { color: '#409eff' },
      itemStyle: { color: '#409eff' }
    }],
    grid: { left: 60, right: 20, bottom: 60, top: 20 }
  })
  return chart
}

function initCategoryChart(el) {
  const chart = echarts.init(el)
  chart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { orient: 'vertical', left: 10, top: 'center' },
    series: [{
      type: 'pie', radius: ['40%', '70%'],
      center: ['65%', '50%'],
      data: [
        { name: '编程开发', value: 3 },
        { name: '数据科学', value: 1 },
        { name: '人工智能', value: 2 },
        { name: '产品设计', value: 1 },
        { name: '商业运营', value: 1 }
      ],
      label: { show: false }
    }]
  })
  return chart
}

function initHotCourseChart(el) {
  const chart = echarts.init(el)
  const courses = ['Spring Boot微服务', 'Vue3全栈', 'Python数据分析', '深度学习', 'Docker K8s', '产品经理', 'MySQL优化', '大模型应用']
  const counts = [856, 1234, 2341, 567, 423, 789, 634, 1089]
  chart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    xAxis: { type: 'value', name: '学员数' },
    yAxis: { type: 'category', data: courses.reverse(), axisLabel: { fontSize: 11 } },
    series: [{
      type: 'bar', data: counts.reverse(),
      itemStyle: { color: (params) => ['#409eff','#67c23a','#e6a23c','#f56c6c','#909399','#b37feb','#ff85c2','#36cfc9'][params.dataIndex] },
      label: { show: true, position: 'right' }
    }],
    grid: { left: 100, right: 60, top: 10, bottom: 30 }
  })
  return chart
}

function initUserRoleChart(el) {
  const chart = echarts.init(el)
  chart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie', radius: '60%', center: ['50%', '45%'],
      data: [
        { name: '学员', value: 85 },
        { name: '讲师', value: 12 },
        { name: '管理员', value: 3 }
      ],
      emphasis: { itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0,0,0,.5)' } }
    }]
  })
  return chart
}

onMounted(() => {
  charts = [
    initOrderChart(orderChartRef.value),
    initCategoryChart(categoryChartRef.value),
    initHotCourseChart(hotCourseChartRef.value),
    initUserRoleChart(userRoleChartRef.value)
  ]
  const resize = () => charts.forEach(c => c.resize())
  window.addEventListener('resize', resize)
  onUnmounted(() => {
    window.removeEventListener('resize', resize)
    charts.forEach(c => c.dispose())
  })
})
</script>

<style scoped>
.page-title { font-size: 22px; margin-bottom: 20px; }
.chart-card { border-radius: 12px; }
</style>
