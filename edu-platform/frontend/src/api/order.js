import request from '@/utils/request'

function normalizePageParams(params = {}) {
  const { pageNum, pageSize, ...rest } = params
  return {
    ...rest,
    ...(pageNum !== undefined ? { page: pageNum } : {}),
    ...(pageSize !== undefined ? { size: pageSize } : {})
  }
}

export const orderApi = {
  createOrder: data => request.post('/order/create', data),
  payOrder: orderId => request.post(`/order/${orderId}/pay`),
  createAlipayPayment: orderId => request.post(`/order/${orderId}/payment/alipay`),
  getPaymentStatus: orderId => request.get(`/order/${orderId}/payment/status`),
  getMyOrders: params => request.get('/order/list', { params: normalizePageParams(params) }),
  getOrderDetail: orderId => request.get(`/order/${orderId}`),
  cancelOrder: orderId => request.post(`/order/${orderId}/cancel`),
  checkPurchased: courseId => request.get(`/order/check/${courseId}`),
  // 新增：通过课程ID支付/取消
  payByCourse: courseId => request.post(`/order/pay-by-course/${courseId}`),
  cancelByCourse: courseId => request.post(`/order/cancel-by-course/${courseId}`),
  getAllOrders: params => request.get('/order/admin/list', { params: normalizePageParams(params) }),
  getStats: () => request.get('/order/admin/stats')
}
