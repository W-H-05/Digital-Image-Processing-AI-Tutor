<template>
  <div class="pack-page">
    <StudentViewBar />
    <header class="topbar">
      <div class="left">
        <el-button text @click="$router.push('/home')"><span class="arrow">&lt;</span> 返回课程地图</el-button>
        <span class="sep">|</span>
        <span class="pack-title">{{ pack.title }}</span>
        <span class="pack-no">{{ pack.lessonNo }}</span>
      </div>
      <div class="right">
        <span>{{ userStore.realName || userStore.username }} · {{ userStore.className }}</span>
      </div>
    </header>

    <div class="layout">
      <!-- 左侧材料目录 -->
      <aside class="side" :class="{ collapsed: sideCollapsed }">
        <div class="side-header">
          <div v-if="!sideCollapsed" class="side-title">📂 材料目录</div>
          <button class="side-toggle" :title="sideCollapsed ? '展开目录' : '折叠目录'" @click="sideCollapsed = !sideCollapsed">
            {{ sideCollapsed ? '»' : '«' }}
          </button>
        </div>
        <div v-for="m in materials.filter(x => x.materialType !== '作业')" :key="'m' + m.id" class="mat-item" :class="{ active: activeKey === 'm' + m.id }" @click="openMaterial(m)" :title="sideCollapsed ? m.title : ''">
          <span class="mat-icon">{{ iconFor(m.materialType) }}</span>
          <div v-if="!sideCollapsed" class="mat-info">
            <div class="mat-name">{{ m.title }}</div>
            <div class="mat-meta">{{ m.materialType }} · {{ m.groupName }}</div>
          </div>
        </div>
        <!-- 交互课件目录项 -->
        <div v-for="cw in coursewares" :key="'cw' + cw.id" class="mat-item" :class="{ active: activeKey === 'cw' + cw.id }" @click="openCourseware(cw)" :title="sideCollapsed ? cw.name : ''">
          <span class="mat-icon">🎛️</span>
          <div v-if="!sideCollapsed" class="mat-info">
            <div class="mat-name">{{ cw.name }}</div>
            <div class="mat-meta">交互课件 · {{ cw.type }}</div>
          </div>
        </div>
        <!-- 课后作业目录项 -->
        <div v-for="hw in homeworks" :key="'hw' + hw.id" class="mat-item" :class="{ active: activeKey === 'hw' + hw.id }" @click="openHomework(hw)" :title="sideCollapsed ? hw.title : ''">
          <span class="mat-icon">📝</span>
          <div v-if="!sideCollapsed" class="mat-info">
            <div class="mat-name">{{ hw.title }}</div>
            <div class="mat-meta">课后作业 · 共 {{ hw.totalScore }} 分<span v-if="hw.submitted" class="hw-done"> · 已提交</span></div>
          </div>
        </div>
      </aside>

      <!-- 中间内容区 -->
      <main class="main">
        <div v-if="!activeKey" class="placeholder">
          <div class="ph-emoji">📖</div>
          <p>从左侧选择一个材料开始学习</p>
        </div>

        <!-- 材料内容 -->
        <div v-else-if="activeMaterial" class="content-panel">
          <div class="content-header">
            <h2>{{ activeMaterial.title }}</h2>
            <span class="mat-type-tag">{{ activeMaterial.materialType }}</span>
          </div>
          <div class="content-body">
            <!-- 文本描述 -->
            <div v-if="activeMaterial.description" class="desc">{{ activeMaterial.description }}</div>
            <!-- 图片素材 -->
            <el-image v-if="activeMaterial.filePath && isImage(activeMaterial.fileType)" :src="activeMaterial.filePath" fit="contain" class="mat-image" />
            <!-- HTML/课件 -->
            <div v-if="activeMaterial.filePath && isHtml(activeMaterial.fileType)" class="html-wrap" :class="{ fullscreen: htmlFullscreen }">
              <div class="html-toolbar">
                <span class="html-name">📄 {{ activeMaterial.title }}</span>
                <button class="html-btn" @click="toggleHtmlFullscreen">{{ htmlFullscreen ? '退出全屏' : '⛶ 全屏' }}</button>
              </div>
              <iframe :src="activeMaterial.filePath" class="mat-iframe"></iframe>
            </div>
            <!-- PPT/PDF 在线预览 -->
            <DocPreview v-if="activeMaterial.filePath && isPreviewable(activeMaterial.fileType)" :material-id="activeMaterial.id" :lesson-pack-id="pack.id" />
            <!-- 其他文件提示 -->
            <div v-if="activeMaterial.filePath && !isImage(activeMaterial.fileType) && !isHtml(activeMaterial.fileType) && !isPreviewable(activeMaterial.fileType)" class="file-tip">
              📎 <a :href="activeMaterial.filePath" target="_blank">点击查看/下载文件</a>
            </div>
          </div>
        </div>

        <!-- 交互课件内容 -->
        <div v-else-if="activeCourseware" class="cw-section">
          <CoursewareLab :courseware="activeCourseware" :lesson-pack-id="pack.id" class="cw-item" />
        </div>

        <!-- 课后作业内容 -->
        <div v-else-if="activeHomework" class="hw-section">
          <div class="hw-card">
            <div class="hw-head">
              <span class="hw-title">{{ activeHomework.title }}</span>
              <span class="hw-total">共 {{ activeHomework.totalScore }} 分</span>
              <el-tag v-if="activeHomework.submitted" type="success" size="small">已提交</el-tag>
            </div>
            <div v-if="activeHomework.description" class="hw-desc">{{ activeHomework.description }}</div>
            <div v-if="activeHomework.deadline" class="hw-deadline">截止：{{ formatTime(activeHomework.deadline) }}</div>
            <div v-if="activeHomework.submitted && hwScores[activeHomework.id] !== undefined" class="hw-score-line">
              🎉 本次得分：<b>{{ hwScores[activeHomework.id] }}</b> / {{ activeHomework.totalScore }}（已提交，不可修改）
            </div>
            <div v-for="(q, qi) in activeHomework.questions" :key="qi" class="hw-question">
              <div class="hw-q-stem"><span class="hw-q-tag">{{ typeLabel(q.type) }}</span> {{ qi + 1 }}. {{ q.stem }}（{{ q.score }}分）</div>
              <!-- 选择题 -->
              <el-radio-group v-if="q.type === 'choice'" v-model="hwAnswers[activeHomework.id][qi]" class="hw-options" :disabled="activeHomework.submitted">
                <el-radio v-for="(o, oi) in q.options" :key="oi" :value="extractOptionKey(o)">{{ o }}</el-radio>
              </el-radio-group>
              <!-- 填空题 -->
              <el-input v-else-if="q.type === 'blank'" v-model="hwAnswers[activeHomework.id][qi]" placeholder="填写答案" style="max-width: 400px" :disabled="activeHomework.submitted" />
              <!-- 代码题 -->
              <el-input v-else v-model="hwAnswers[activeHomework.id][qi]" type="textarea" :rows="4" placeholder="粘贴代码" :disabled="activeHomework.submitted" />
            </div>
            <div class="hw-submit-row">
              <el-button v-if="!activeHomework.submitted" type="primary" :loading="hwSubmitting[activeHomework.id]" @click="submitHw(activeHomework)">提交作业（自动判分）</el-button>
            </div>
          </div>
        </div>
      </main>

      <!-- 可拖拽分隔条 -->
      <div class="divider" @mousedown="startDrag"></div>

      <!-- 右侧 AI 助手 -->
      <aside class="assistant" :style="{ width: assistantW + 'px' }">
        <AiAssistant :lesson-pack-id="pack.id" />
      </aside>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import AiAssistant from '../../components/AiAssistant.vue'
import CoursewareLab from '../../components/CoursewareLab.vue'
import DocPreview from '../../components/DocPreview.vue'
import StudentViewBar from '../../components/StudentViewBar.vue'
import { useUserStore } from '../../store/user'
import { getLessonPack, listMaterials, listCoursewares, recordOperation, listHomeworkForStudent, submitHomework } from '../../api'
import { ElMessage } from 'element-plus'

const route = useRoute()
const userStore = useUserStore()
const pack = ref({})
const materials = ref([])
const coursewares = ref([])
const activeMaterial = ref(null)
const activeCourseware = ref(null)
const activeHomework = ref(null)
const activeKey = ref('') // 'm'+id / 'cw'+id / 'hw'+id
const homeworks = ref([])
const hwAnswers = ref({})
const hwScores = ref({})
const hwSubmitting = ref({})
const sideCollapsed = ref(false)
const assistantW = ref(360)
const htmlFullscreen = ref(false)
let materialOpenTime = Date.now()

onMounted(async () => {
  const id = route.params.id
  try {
    pack.value = await getLessonPack(id)
    materials.value = await listMaterials(id)
    coursewares.value = await listCoursewares(id)
    homeworks.value = await listHomeworkForStudent(id)
    // 初始化答案容器
    homeworks.value.forEach(hw => {
      hwAnswers.value[hw.id] = {}
    })
    if (materials.value.length) openMaterial(materials.value[0])
    await recordOperation({ lessonPackId: Number(id), actionType: 'open_pack', actionDetail: '打开课次包 ' + pack.value.title })
  } catch (e) {}
})

function iconFor(type) {
  const map = {
    '教案': '📋', '重难点': '🎯', '知识点': '📘', 'PPT': '📊',
    '交互课件': '🎛️', '图片素材': '🖼️', '作业': '📝', '其他': '📎', '备课笔记': '🔒'
  }
  return map[type] || '📄'
}

function isImage(ft) {
  return ['png', 'jpg', 'jpeg', 'gif', 'webp', 'bmp'].includes((ft || '').toLowerCase())
}
function isHtml(ft) {
  return ['html', 'htm'].includes((ft || '').toLowerCase())
}
function isPreviewable(ft) {
  return ['ppt', 'pptx', 'pdf'].includes((ft || '').toLowerCase())
}

async function openMaterial(m) {
  // 上报上一个材料的停留时长（关闭事件）
  if (activeKey.value && activeKey.value.startsWith('m') && activeMaterial.value && activeMaterial.value.id !== m.id) {
    const stayMs = Date.now() - materialOpenTime
    recordOperation({
      lessonPackId: Number(route.params.id),
      materialId: activeMaterial.value.id,
      actionType: 'close_material',
      actionDetail: '关闭材料 ' + activeMaterial.value.title,
      duration: Math.round(stayMs / 1000)
    }).catch(() => {})
  }
  activeKey.value = 'm' + m.id
  activeMaterial.value = m
  activeCourseware.value = null
  activeHomework.value = null
  materialOpenTime = Date.now()
  await recordOperation({
    lessonPackId: Number(route.params.id),
    materialId: m.id,
    actionType: 'open_material',
    actionDetail: '打开材料 ' + m.title
  })
  // 知识点浏览埋点
  if (m.materialType === '知识点') {
    recordOperation({
      lessonPackId: Number(route.params.id),
      materialId: m.id,
      actionType: 'knowledge_view',
      actionDetail: '浏览知识点 ' + m.title
    }).catch(() => {})
  }
}

function openCourseware(cw) {
  activeKey.value = 'cw' + cw.id
  activeCourseware.value = cw
  activeMaterial.value = null
  activeHomework.value = null
  recordOperation({
    lessonPackId: Number(route.params.id),
    materialId: null,
    actionType: 'open_courseware',
    actionDetail: '打开课件 ' + cw.name
  }).catch(() => {})
}

function openHomework(hw) {
  activeKey.value = 'hw' + hw.id
  activeHomework.value = hw
  activeMaterial.value = null
  activeCourseware.value = null
}

/** 拖拽调节 AI 助手宽度 */
function startDrag(e) {
  e.preventDefault()
  const startX = e.clientX
  const startW = assistantW.value
  const onMove = (ev) => {
    const dx = startX - ev.clientX
    const w = Math.min(600, Math.max(280, startW + dx))
    assistantW.value = w
  }
  const onUp = () => {
    document.removeEventListener('mousemove', onMove)
    document.removeEventListener('mouseup', onUp)
  }
  document.addEventListener('mousemove', onMove)
  document.addEventListener('mouseup', onUp)
}

/** HTML 课件全屏 */
function toggleHtmlFullscreen() {
  htmlFullscreen.value = !htmlFullscreen.value
}

function typeLabel(t) {
  const map = { choice: '选择题', blank: '填空题', code: '代码题' }
  return map[t] || t
}

function extractOptionKey(option) {
  // 选项形如 "A. xxx"，提取字母
  const m = String(option).match(/^([A-Ha-h])/)
  return m ? m[1].toUpperCase() : String(option).substring(0, 1)
}

function formatTime(t) {
  if (!t) return ''
  return String(t).replace('T', ' ').substring(0, 16)
}

async function submitHw(hw) {
  hwSubmitting.value[hw.id] = true
  try {
    const answers = hwAnswers.value[hw.id] || {}
    const res = await submitHomework(hw.id, answers)
    hwScores.value[hw.id] = res.score
    hw.submitted = true
    ElMessage.success(`提交成功！得分 ${res.score} / ${res.totalScore}`)
    // 作业提交埋点
    recordOperation({
      lessonPackId: Number(route.params.id),
      materialId: null,
      actionType: 'homework_submit',
      actionDetail: '提交作业 ' + hw.title + '，得分 ' + res.score
    }).catch(() => {})
  } catch (e) {
  } finally {
    hwSubmitting.value[hw.id] = false
  }
}
</script>

<style scoped>
.pack-page { height: 100vh; display: flex; flex-direction: column; overflow: hidden; }
.topbar {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 20px; background: #fff; box-shadow: 0 2px 10px rgba(0,0,0,0.04);
}
.left { display: flex; align-items: center; gap: 10px; }
.left .arrow { font-weight: 700; font-size: 15px; margin-right: 2px; }
.sep { color: #CBD5E1; }
.pack-title { font-weight: 700; }
.pack-no { color: #4F46E5; font-size: 13px; background: #EEF2FF; padding: 2px 10px; border-radius: 10px; }
.right { color: #64748B; font-size: 14px; }

.layout {
  flex: 1; display: flex;
  gap: 0; padding: 16px;
  min-height: 0;
  align-items: stretch;
}
.side {
  background: #fff; border-radius: 16px; padding: 16px;
  overflow-y: auto; box-shadow: 0 4px 20px rgba(79,70,229,0.06);
  height: 100%;
  min-height: 0;
  width: 250px;
  flex-shrink: 0;
  transition: width 0.2s;
}
.side.collapsed {
  width: 60px;
  padding: 16px 10px;
}
.side-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.side-title { font-weight: 700; }
.side-toggle {
  border: none; background: #F1F5F9; color: #64748B; cursor: pointer;
  width: 24px; height: 24px; border-radius: 6px; font-size: 13px; line-height: 1;
  display: flex; align-items: center; justify-content: center;
}
.side-toggle:hover { background: #E2E8F0; }
.side.collapsed .side-header { justify-content: center; }
.side.collapsed .mat-item { justify-content: center; padding: 10px 0; }
.mat-item {
  display: flex; align-items: center; gap: 10px;
  padding: 10px; border-radius: 10px; cursor: pointer; transition: all 0.2s;
  margin-bottom: 4px;
}
.mat-item:hover { background: #F8FAFC; }
.mat-item.active { background: #EEF2FF; }
.mat-icon { font-size: 20px; }
.mat-name { font-size: 14px; font-weight: 500; color: #1F2937; }
.mat-meta { font-size: 11px; color: #94A3B8; }

.main {
  overflow-y: auto; display: flex; flex-direction: column; gap: 16px;
  height: 100%; min-height: 0; min-width: 0;
  background: #fff; border-radius: 16px; padding: 20px;
  box-shadow: 0 4px 20px rgba(79,70,229,0.06);
  flex: 1;
  margin: 0 12px;
}
.placeholder { text-align: center; padding: 60px 20px; color: #94A3B8; }
.ph-emoji { font-size: 50px; }
.content-header { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }
.mat-type-tag { font-size: 12px; background: #E0F2FE; color: #0369A1; padding: 2px 10px; border-radius: 10px; }
.desc { white-space: pre-wrap; line-height: 1.8; color: #374151; }
.mat-image { max-height: 400px; border-radius: 10px; }
.mat-iframe { width: 100%; height: 500px; border: 1px solid #E2E8F0; border-radius: 10px; }
.html-wrap { position: relative; border: 1px solid #E2E8F0; border-radius: 10px; overflow: hidden; }
.html-wrap .html-toolbar { display: flex; align-items: center; justify-content: space-between; padding: 8px 12px; background: #F8FAFC; border-bottom: 1px solid #E2E8F0; }
.html-wrap .html-name { font-size: 13px; color: #475569; font-weight: 500; }
.html-wrap .html-btn { border: 1px solid #E2E8F0; background: #fff; border-radius: 6px; padding: 3px 10px; font-size: 12px; cursor: pointer; color: #374151; }
.html-wrap .html-btn:hover { background: #EEF2FF; border-color: #4F46E5; color: #4F46E5; }
.html-wrap .mat-iframe { border: none; border-radius: 0; display: block; }
.html-wrap.fullscreen { position: fixed; inset: 0; z-index: 3000; border-radius: 0; background: #fff; }
.html-wrap.fullscreen .mat-iframe { height: calc(100vh - 41px); }
.file-tip { padding: 30px; text-align: center; }
.file-tip a { color: #4F46E5; font-weight: 600; }

.cw-section { margin-top: 0; }
.cw-item { margin-bottom: 16px; }

.hw-section { margin-top: 0; }
.hw-card { margin-bottom: 16px; padding: 18px; }
.hw-head { display: flex; align-items: center; gap: 12px; margin-bottom: 8px; flex-wrap: wrap; }
.hw-title { font-weight: 600; font-size: 16px; }
.hw-total { color: #4F46E5; font-size: 13px; }
.hw-deadline { color: #B45309; font-size: 13px; margin-bottom: 12px; }
.hw-desc { white-space: pre-wrap; color: #475569; font-size: 13px; line-height: 1.7; margin-bottom: 12px; background: #FAFBFF; padding: 10px 14px; border-radius: 8px; }
.hw-question { padding: 12px 0; border-bottom: 1px solid #F1F5F9; }
.hw-q-stem { font-weight: 500; color: #1F2937; margin-bottom: 8px; }
.hw-q-tag { background: #EEF2FF; color: #4F46E5; padding: 2px 8px; border-radius: 8px; font-size: 12px; margin-right: 4px; }
.hw-options { display: flex; flex-direction: column; gap: 6px; align-items: flex-start; }
.hw-submit-row { display: flex; align-items: center; gap: 16px; margin-top: 14px; }
.hw-score { color: #16A34A; font-weight: 500; }
.hw-score b { font-size: 18px; }
.hw-score-line { color: #16A34A; font-weight: 500; margin: 8px 0; }
.hw-score-line b { font-size: 18px; }
.hw-done { color: #16A34A; }

.assistant { height: 100%; min-height: 0; flex-shrink: 0; }
.assistant :deep(.ai-assistant) { height: 100%; }
.divider {
  width: 10px; cursor: col-resize; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  border-radius: 6px; transition: background 0.2s;
}
.divider:hover { background: #EEF2FF; }
.divider::after {
  content: ''; width: 3px; height: 48px; border-radius: 3px;
  background: #CBD5E1;
}
.divider:hover::after { background: #4F46E5; }

@media (max-width: 1100px) {
  .layout { flex-wrap: wrap; }
  .side { width: 100%; height: auto; max-height: 220px; }
  .main { margin: 12px 0; }
  .assistant { width: 100% !important; height: 500px; }
  .divider { display: none; }
}
</style>
