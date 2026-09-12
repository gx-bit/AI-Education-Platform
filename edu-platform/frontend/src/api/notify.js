import request from '@/utils/request'

function normalizePageParams(params = {}) {
  const { pageNum, pageSize, ...rest } = params
  return {
    ...rest,
    ...(pageNum !== undefined ? { page: pageNum } : {}),
    ...(pageSize !== undefined ? { size: pageSize } : {})
  }
}

export const notifyApi = {
  getMyNotifications: params => request.get('/notify/my', { params: normalizePageParams(params) }),
  markAsRead: id => request.put(`/notify/${id}/read`),
  markAllAsRead: () => request.put('/notify/read-all'),
  getUnreadCount: () => request.get('/notify/unread-count'),
  deleteNotification: id => request.delete(`/notify/${id}`)
}
