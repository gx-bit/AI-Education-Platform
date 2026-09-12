import request from '@/utils/request'

function normalizeCourseParams(params = {}) {
  const { pageNum, pageSize, sortBy, ...rest } = params
  const sortMap = {
    studentCount: 'student_count',
    createdAt: 'created_at'
  }
  return {
    ...rest,
    ...(pageNum !== undefined ? { page: pageNum } : {}),
    ...(pageSize !== undefined ? { size: pageSize } : {}),
    ...(sortBy !== undefined ? { orderBy: sortMap[sortBy] || sortBy } : {})
  }
}

export const courseApi = {
  getCourseList: params => request.get('/course/list', { params: normalizeCourseParams(params) }),
  getCourseById: id => request.get(`/course/${id}`),
  getCategoryList: () => request.get('/course/category/list'),
  getAiRecommendations: params => request.get('/course/recommend', { params }),
  createCourse: data => request.post('/course', data),
  updateCourse: (id, data) => request.put(`/course/${id}`, data),
  deleteCourse: id => request.delete(`/course/${id}`),
  publishCourse: id => request.put(`/course/${id}/publish`),
  unpublishCourse: id => request.put(`/course/${id}/unpublish`),
  getPersonalizedRecommendations: params => request.get('/recommendation/courses', { params }),
  recordBehavior: data => request.post('/recommendation/behavior', data),
  getInteraction: id => request.get(`/course/${id}/interaction`),
  favoriteCourse: id => request.post(`/course/${id}/favorite`),
  unfavoriteCourse: id => request.delete(`/course/${id}/favorite`),
  getFavorites: params => request.get('/course/favorites', { params }),
  getReviews: (id, params) => request.get(`/course/${id}/reviews`, { params }),
  saveReview: (id, data) => request.post(`/course/${id}/reviews`, data)
}
