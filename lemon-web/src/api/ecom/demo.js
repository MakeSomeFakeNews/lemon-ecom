import request from '@/utils/request'

/**
 * 电商演示数据：进度 { generated, running, current, done, total, failures: [{ key, message }], startTime, endTime }；
 * 生成为后台执行，返回当前进度，之后轮询 demoStatus
 */
export const demoStatus = () => request.get('/ecom/demo/status')
export const demoGenerate = () => request.post('/ecom/demo/generate')
