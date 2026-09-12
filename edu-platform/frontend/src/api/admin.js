import request from '@/utils/request'

export const adminApi = {
  getOrders: params => request.get('/order/admin/list', { params }),
  getStats: () => request.get('/order/admin/stats'),
  sendNotification: data => request.post('/notify/admin/send', data),
  getRecommendationConfig: () => request.get('/recommendation/admin/config'),
  updateRecommendationConfig: data => request.put('/recommendation/admin/config', data)
}
