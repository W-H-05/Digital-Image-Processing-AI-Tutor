<template>
  <div class="doc-preview">
    <!-- 工具栏 -->
    <div class="dp-toolbar">
      <button class="dp-btn" :disabled="page <= 1" @click="prevPage">‹ 上一页</button>
      <span class="dp-page">{{ page }} / {{ total }}</span>
      <button class="dp-btn" :disabled="page >= total" @click="nextPage">下一页 ›</button>
      <span class="dp-sep"></span>
      <button class="dp-btn" @click="zoomOut">−</button>
      <span class="dp-zoom">{{ Math.round(zoom * 100) }}%</span>
      <button class="dp-btn" @click="zoomIn">+</button>
      <button class="dp-btn" @click="resetZoom">适应</button>
      <span class="dp-sep"></span>
      <button class="dp-btn" @click="toggleFullscreen">⛶ 全屏</button>
    </div>
    <!-- 内容区 -->
    <div class="dp-body" ref="body" :class="{ fullscreen: isFullscreen }">
      <img v-if="images[page - 1]" :src="images[page - 1]" :style="{ transform: `scale(${zoom})` }" class="dp-img" alt="预览页" />
      <div v-if="loading" class="dp-loading">🔄 正在生成预览…</div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { previewMaterial, recordOperation } from '../api'

const props = defineProps({
  materialId: { type: [Number, String], required: true },
  lessonPackId: { type: [Number, String], default: null }
})

const images = ref([])
const page = ref(1)
const zoom = ref(1)
const loading = ref(false)
const isFullscreen = ref(false)
const body = ref(null)

const total = ref(1)

onMounted(async () => {
  await loadPreview()
  document.addEventListener('keydown', onKeydown)
})

onUnmounted(() => {
  document.removeEventListener('keydown', onKeydown)
})

async function loadPreview() {
  loading.value = true
  try {
    const res = await previewMaterial(props.materialId)
    images.value = res.images || []
    total.value = images.value.length || 1
    page.value = 1
  } catch (e) {
    ElMessage.error('预览加载失败')
  } finally {
    loading.value = false
  }
}

function prevPage() { if (page.value > 1) { page.value--; trackPage() } }
function nextPage() { if (page.value < total.value) { page.value++; trackPage() } }
function zoomIn() { zoom.value = Math.min(3, zoom.value + 0.25) }
function zoomOut() { zoom.value = Math.max(0.25, zoom.value - 0.25) }
function resetZoom() { zoom.value = 1 }

/** 记录 PPT/PDF 浏览页码埋点 */
function trackPage() {
  recordOperation({
    lessonPackId: props.lessonPackId ? Number(props.lessonPackId) : null,
    materialId: Number(props.materialId),
    actionType: 'ppt_page',
    actionDetail: '浏览第 ' + page.value + ' 页'
  }).catch(() => {})
}

function toggleFullscreen() {
  if (!body.value) return
  if (!isFullscreen.value) {
    if (body.value.requestFullscreen) body.value.requestFullscreen()
    isFullscreen.value = true
  } else {
    if (document.exitFullscreen) document.exitFullscreen()
    isFullscreen.value = false
  }
}

function onKeydown(e) {
  if (e.key === 'ArrowLeft') prevPage()
  if (e.key === 'ArrowRight') nextPage()
  if (e.key === 'Escape' && isFullscreen.value) isFullscreen.value = false
}
</script>

<style scoped>
.doc-preview { border: 1px solid #E2E8F0; border-radius: 12px; overflow: hidden; background: #F8FAFC; }
.dp-toolbar { display: flex; align-items: center; gap: 8px; padding: 10px 14px; background: #fff; border-bottom: 1px solid #E2E8F0; flex-wrap: wrap; }
.dp-btn { border: 1px solid #E2E8F0; background: #fff; border-radius: 8px; padding: 4px 12px; cursor: pointer; font-size: 13px; color: #374151; }
.dp-btn:hover:not(:disabled) { background: #EEF2FF; border-color: #4F46E5; color: #4F46E5; }
.dp-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.dp-page { font-size: 13px; color: #475569; font-weight: 500; }
.dp-zoom { font-size: 13px; color: #475569; min-width: 48px; text-align: center; }
.dp-sep { width: 1px; height: 20px; background: #E2E8F0; margin: 0 4px; }
.dp-body { position: relative; min-height: 400px; display: flex; align-items: center; justify-content: center; overflow: auto; background: #E5E7EB; }
.dp-body.fullscreen { min-height: calc(100vh - 60px); }
.dp-img { max-width: 100%; transition: transform 0.2s; box-shadow: 0 4px 20px rgba(0,0,0,0.15); }
.dp-loading { position: absolute; color: #64748B; }
</style>
