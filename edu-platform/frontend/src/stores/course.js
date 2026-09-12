import { defineStore } from 'pinia'
import { courseApi } from '@/api/course'

export const useCourseStore = defineStore('course', {
  state: () => ({
    categories: [],
    courseList: [],
    total: 0,
    loading: false,
    aiRecommendations: [],
    recommendationRequestId: null,
    recommendationStrategy: null,
    recommendationPersonalized: false
  }),

  actions: {
    async fetchCategories() {
      if (this.categories.length) return
      const res = await courseApi.getCategoryList()
      this.categories = res.data || []
    },

    async fetchCourseList(params) {
      this.loading = true
      try {
        const res = await courseApi.getCourseList(params)
        this.courseList = res.data?.records || []
        this.total = res.data?.total || 0
      } finally {
        this.loading = false
      }
    },

    async fetchAiRecommendations(params = {}) {
      const sessionId = getRecommendationSessionId()
      const res = await courseApi.getPersonalizedRecommendations({ ...params, sessionId })
      const data = res.data || {}
      this.recommendationRequestId = data.requestId || null
      this.recommendationStrategy = data.strategy || null
      this.recommendationPersonalized = Boolean(data.personalized)
      this.aiRecommendations = (data.courses || []).map(course => ({
        ...course,
        recommendationRequestId: data.requestId
      }))
      await Promise.allSettled(this.aiRecommendations.map(course => courseApi.recordBehavior({
        courseId: course.id,
        behaviorType: 'impression',
        requestId: data.requestId,
        sessionId
      })))
    }
  }
})

function getRecommendationSessionId() {
  const key = 'recommendationSessionId'
  let value = localStorage.getItem(key)
  if (!value) {
    value = globalThis.crypto?.randomUUID?.() || `${Date.now()}-${Math.random().toString(16).slice(2)}`
    localStorage.setItem(key, value)
  }
  return value
}
