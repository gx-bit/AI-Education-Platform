import request from '@/utils/request'
export const agentApi = {
  current: () => request.get('/agent/plans/current'),
  preview: data => request.post('/agent/plans/preview', data),
  confirm: id => request.post(`/agent/plans/${id}/confirm`),
  complete: id => request.post(`/agent/tasks/${id}/complete`),
  reopen: id => request.post(`/agent/tasks/${id}/reopen`),
  chat: data => request.post('/agent/chat', data)
}
