import request from '@/utils/request'

/** AI 写作是否可用：{ available, reason }（未在「AI 设置」中开启时 available 为 false，reason 为原因） */
export const ecomAiStatus = () => request.get('/ecom/ai/status')
/** 草拟会话回复：data { language（为空按买家语言）, instruction }，返回 { text } */
export const draftThreadReply = (threadId, data) => request.post(`/ecom/ai/cs/thread/${threadId}/draft`, data || {})
/** 翻译：data { text, target（语言代码，为空时译成会话买家的语言）, threadId }，返回 { text } */
export const translateCsText = (data) => request.post('/ecom/ai/cs/translate', data)
/** 草拟评价回复：data { language, instruction }，返回 { text } */
export const draftReviewReply = (reviewId, data) => request.post(`/ecom/ai/cs/review/${reviewId}/draft`, data || {})
/** 评价翻译：data { text, target }，返回 { text } */
export const translateReviewText = (data) => request.post('/ecom/ai/cs/review/translate', data)
/** 刊登文案翻译：data { text, target }，返回 { text } */
export const translateListingText = (data) => request.post('/ecom/ai/listing/translate', data)
/** 刊登文案优化：data { platform, title, description, language, maxTitle, instruction }，返回 { title, description } */
export const optimizeListingText = (data) => request.post('/ecom/ai/listing/optimize', data)
