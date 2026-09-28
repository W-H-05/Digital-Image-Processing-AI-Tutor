import request from '../utils/request'

// 认证
export const login = (data) => request.post('/auth/login', data)
export const logout = () => request.post('/auth/logout')
export const changePassword = (data) => request.post('/auth/change-password', data)
export const getMe = () => request.get('/auth/me')

// 课次包
export const listLessonPacks = () => request.get('/lesson-packs')
export const getLessonPack = (id) => request.get(`/lesson-packs/${id}`)
export const createLessonPack = (data) => request.post('/teacher/lesson-packs', data)
export const updateLessonPack = (id, data) => request.put(`/teacher/lesson-packs/${id}`, data)
export const deleteLessonPack = (id) => request.delete(`/teacher/lesson-packs/${id}`)
export const publishLessonPack = (id) => request.post(`/teacher/lesson-packs/${id}/publish`)
export const offlineLessonPack = (id) => request.post(`/teacher/lesson-packs/${id}/offline`)
export const copyLessonPack = (id) => request.post(`/teacher/lesson-packs/${id}/copy`)

// 材料
export const listMaterials = (packId) => request.get(`/lesson-packs/${packId}/materials`)
export const addTextMaterial = (packId, data) => request.post(`/teacher/materials/${packId}/text`, data)
export const updateMaterial = (id, data) => request.put(`/teacher/materials/${id}`, data)
export const deleteMaterial = (id) => request.delete(`/teacher/materials/${id}`)
export const uploadMaterial = (packId, formData) => request.post(`/teacher/materials/${packId}`, formData, {
  headers: { 'Content-Type': 'multipart/form-data' }
})
export const sortMaterials = (items) => request.post('/teacher/materials/sort', { items })
export const getMaterialVersions = (id) => request.get(`/teacher/materials/${id}/versions`)
export const restoreMaterialVersion = (id, versionId) => request.post(`/teacher/materials/${id}/restore/${versionId}`)
export const previewPack = (packId) => request.get(`/teacher/lesson-packs/${packId}/preview`)

// 知识点 & 课件
export const listKnowledgePoints = (packId) => request.get(`/lesson-packs/${packId}/knowledge-points`)
export const listCoursewares = (packId) => request.get(`/lesson-packs/${packId}/coursewares`)
export const createCourseware = (packId, data) => request.post(`/teacher/coursewares/${packId}`, data)
export const updateCourseware = (id, data) => request.put(`/teacher/coursewares/${id}`, data)
export const deleteCourseware = (id) => request.delete(`/teacher/coursewares/${id}`)
export const uploadCoursewareSample = (id, formData) => request.post(`/teacher/coursewares/${id}/sample-image`, formData, {
  headers: { 'Content-Type': 'multipart/form-data' }
})

// AI
export const aiChat = (data) => request.post('/ai/chat', data)
export const aiSmart = (data) => request.post('/ai/smart', data)
export const aiCodeHelp = (data) => request.post('/ai/code-help', data)
export const aiFunctionQuery = (data) => request.post('/ai/function-query', data)
export const aiPrepare = (data) => request.post('/teacher/ai/prepare', data)
export const aiFileAnalysis = (formData) => request.post('/ai/file-analysis', formData, {
  headers: { 'Content-Type': 'multipart/form-data' }
})

// 学生管理
export const importStudents = (csv) => request.post('/teacher/students/import', { csv })
export const listStudents = () => request.get('/teacher/students')
export const resetStudentPassword = (id) => request.post(`/teacher/students/${id}/reset`)
export const batchResetStudents = (ids) => request.post('/teacher/students/batch-reset', { ids })
export const getStudentDetail = (id) => request.get(`/teacher/students/${id}/detail`)

// 学习埋点
export const recordOperation = (data) => request.post('/learning/operation', data)

// 挑战 & 互评
export const submitChallenge = (data) => request.post('/challenges', data)
export const listChallenges = (packId) => request.get(`/challenges/${packId}`)
export const submitPeerReview = (data) => request.post('/peer-reviews', data)
export const listPeerReviewsByTask = (taskId) => request.get(`/peer-reviews/task/${taskId}`)
export const listMyPeerReviews = () => request.get('/peer-reviews/mine')

// 教师端互评答辩管理
export const listRubrics = () => request.get('/teacher/review/rubrics')
export const addRubric = (data) => request.post('/teacher/review/rubrics', data)
export const updateRubric = (id, data) => request.put(`/teacher/review/rubrics/${id}`, data)
export const deleteRubric = (id) => request.delete(`/teacher/review/rubrics/${id}`)
export const listActivities = () => request.get('/teacher/review/activities')
export const addActivity = (data) => request.post('/teacher/review/activities', data)
export const updateActivity = (id, data) => request.put(`/teacher/review/activities/${id}`, data)
export const startActivity = (id) => request.post(`/teacher/review/activities/${id}/start`)
export const finishActivity = (id) => request.post(`/teacher/review/activities/${id}/finish`)
export const deleteActivity = (id) => request.delete(`/teacher/review/activities/${id}`)
export const reviewStats = (params) => request.get('/teacher/review/stats', { params })

// 作业
export const parseHomeworkFile = (formData) => request.post('/teacher/homework/parse', formData, {
  headers: { 'Content-Type': 'multipart/form-data' }
})
export const saveHomework = (data) => request.post('/teacher/homework', data)
export const listHomeworkForTeacher = (packId) => request.get(`/teacher/homework/${packId}`)
export const listHomeworkForStudent = (packId) => request.get(`/lesson-packs/${packId}/homework`)
export const submitHomework = (id, answers) => request.post(`/homework/${id}/submit`, { answers })
export const listHomeworkSubmissions = (id) => request.get(`/teacher/homework/${id}/submissions`)
export const homeworkAnalysis = (id) => request.get(`/teacher/homework/${id}/analysis`)
export const rejectHomeworkSubmission = (submissionId) => request.delete(`/teacher/homework/submission/${submissionId}`)
export const deleteHomework = (id) => request.delete(`/teacher/homework/${id}`)

// 学情看板
export const dashboardOverview = (params) => request.get('/dashboard/overview', { params })
export const dashboardTopConfusion = (params) => request.get('/dashboard/top-confusion', { params })
export const dashboardMastery = (params) => request.get('/dashboard/mastery', { params })
export const dashboardStream = (params) => request.get('/dashboard/stream', { params })
export const dashboardErrorRanking = (params) => request.get('/dashboard/error-ranking', { params })
export const dashboardAnomalies = (params) => request.get('/dashboard/anomalies', { params })
export const dashboardReport = (params) => request.get('/dashboard/report', { params })
export const dashboardWordCloud = (params) => request.get('/dashboard/word-cloud', { params })
export const selfStats = () => request.get('/learning/self-stats')
export const listOperations = (params) => request.get('/learning/operations', { params })

// RAG 知识库管理
export const listRagDocs = (params) => request.get('/teacher/rag', { params })
export const addRagDoc = (data) => request.post('/teacher/rag', data)
export const updateRagDoc = (id, data) => request.put(`/teacher/rag/${id}`, data)
export const deleteRagDoc = (id) => request.delete(`/teacher/rag/${id}`)

// Prompt 模板管理
export const listPrompts = () => request.get('/teacher/prompts')
export const addPrompt = (data) => request.post('/teacher/prompts', data)
export const updatePrompt = (id, data) => request.put(`/teacher/prompts/${id}`, data)
export const deletePrompt = (id) => request.delete(`/teacher/prompts/${id}`)

// 系统配置
export const getSystemConfig = () => request.get('/system/config')
export const updateSystemConfig = (configs) => request.put('/system/config', configs)

// 数据备份
export const doBackup = () => request.post('/system/backup')
export const listBackups = () => request.get('/system/backup/list')

// 文档预览
export const previewMaterial = (materialId) => request.get(`/preview/material/${materialId}`)

// 问答记录
export const listQaRecords = (params) => request.get('/teacher/qa-records', { params })
export const myQaRecords = () => request.get('/student/qa-records')
