<template>
  <div class="courseware">
    <div class="cw-header">
      <span class="cw-title">🎛️ {{ courseware.name }}</span>
      <span class="cw-checkpoint" v-if="checkpoint">🎯 任务：{{ checkpoint }}</span>
    </div>
    <div class="cw-toolbar">
      <span class="cw-toolbar-label">🖼️ 图像来源：</span>
      <el-button size="small" :type="useUpload ? 'default' : 'primary'" @click="useBuiltin">示例图</el-button>
      <el-upload :show-file-list="false" :auto-upload="false" accept="image/*" :on-change="onUploadImage">
        <el-button size="small" :type="useUpload ? 'primary' : 'default'">📤 上传自己的图片</el-button>
      </el-upload>
      <span v-if="uploadedName" class="cw-uploaded">已上传：{{ uploadedName }}</span>
    </div>
    <div class="cw-body">
      <!-- 左容器：图像 -->
      <div class="cw-left">
        <canvas ref="canvas" :width="canvasW" :height="canvasH" class="cw-canvas"></canvas>
      </div>
      <!-- 右容器：直方图(上) + 参数(下) -->
      <div class="cw-right">
        <canvas ref="histCanvas" :width="histW" :height="histH" class="cw-hist"></canvas>
        <div class="cw-controls">
          <div v-for="c in controls" :key="c.key" class="control-item">
            <label>{{ c.label }}：<b>{{ c.value }}</b></label>
            <input type="range" :min="c.min" :max="c.max" :step="c.step || 1" v-model.number="c.value" @input="render" />
          </div>
          <div v-if="type === '形态学'" class="control-item">
            <label>操作：</label>
            <div class="op-btns">
              <button v-for="op in ['腐蚀','膨胀','开运算','闭运算']" :key="op"
                :class="['op-btn', opValue === op && 'active']" @click="opValue = op; render()">{{ op }}</button>
            </div>
          </div>
        </div>
      </div>
    </div>
    <div class="cw-foot">
      <el-button size="small" @click="submitChallenge" type="primary">提交挑战结论</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { submitChallenge as apiSubmitChallenge, recordOperation } from '../api'

const props = defineProps({
  courseware: { type: Object, required: true },
  lessonPackId: { type: [Number, String], default: null }
})

const canvas = ref(null)
const histCanvas = ref(null)
const canvasW = 400
const canvasH = 300
const histW = 400
const histH = 160
const type = computed(() => props.courseware.type || '')
const checkpoint = computed(() => {
  try { return JSON.parse(props.courseware.configJson || '{}').checkpoint } catch (e) { return '' }
})

// 生成模拟灰度图像（256 灰度的合成图案）
let baseImage = []
const controls = ref([])
const opValue = ref('腐蚀')
const useUpload = ref(false)
const uploadedName = ref('')
const imageW = ref(64)
const imageH = ref(64)

onMounted(() => {
  initControls()
  // 若课件配置了示例图，优先加载；否则用内置合成图
  if (props.courseware.resourcePath) {
    loadSampleImage(props.courseware.resourcePath)
  } else {
    initImage()
    render()
  }
})

/** 加载课件示例图（教师上传的） */
function loadSampleImage(src) {
  const img = new Image()
  img.crossOrigin = 'anonymous'
  img.onload = () => {
    let w = img.width, h = img.height
    const maxSide = 96
    if (w > maxSide || h > maxSide) {
      const ratio = Math.min(maxSide / w, maxSide / h)
      w = Math.round(w * ratio)
      h = Math.round(h * ratio)
    }
    const off = document.createElement('canvas')
    off.width = w; off.height = h
    const ctx = off.getContext('2d')
    ctx.drawImage(img, 0, 0, w, h)
    const data = ctx.getImageData(0, 0, w, h).data
    baseImage = []
    for (let i = 0; i < w * h; i++) {
      const r = data[i * 4], g = data[i * 4 + 1], b = data[i * 4 + 2]
      baseImage.push(Math.round(0.299 * r + 0.587 * g + 0.114 * b))
    }
    imageW.value = w
    imageH.value = h
    useUpload.value = false
    uploadedName.value = '课件示例图'
    render()
  }
  img.onerror = () => {
    initImage()
    render()
  }
  img.src = src
}

function initImage() {
  const w = 64, h = 64
  baseImage = []
  for (let y = 0; y < h; y++) {
    for (let x = 0; x < w; x++) {
      // 一个圆 + 渐变背景
      const dx = x - 32, dy = y - 32
      const dist = Math.sqrt(dx * dx + dy * dy)
      let v
      if (dist < 16) v = 200 + Math.floor(55 * Math.sin(x / 3))
      else v = Math.floor(80 + (x / w) * 120)
      baseImage.push(v)
    }
  }
  imageW.value = w
  imageH.value = h
}

/** 切回内置示例图 */
function useBuiltin() {
  useUpload.value = false
  uploadedName.value = ''
  initImage()
  render()
}

/** 上传本地图片作为调参对象 */
function onUploadImage(file) {
  const raw = file.raw
  if (!raw || !raw.type.startsWith('image/')) {
    ElMessage.warning('请选择图片文件')
    return
  }
  const reader = new FileReader()
  reader.onload = (e) => {
    const img = new Image()
    img.onload = () => {
      // 缩放到合适尺寸（最大 96px，保持长宽比）
      let w = img.width, h = img.height
      const maxSide = 96
      if (w > maxSide || h > maxSide) {
        const ratio = Math.min(maxSide / w, maxSide / h)
        w = Math.round(w * ratio)
        h = Math.round(h * ratio)
      }
      const off = document.createElement('canvas')
      off.width = w; off.height = h
      const ctx = off.getContext('2d')
      ctx.drawImage(img, 0, 0, w, h)
      const data = ctx.getImageData(0, 0, w, h).data
      baseImage = []
      for (let i = 0; i < w * h; i++) {
        const r = data[i * 4], g = data[i * 4 + 1], b = data[i * 4 + 2]
        baseImage.push(Math.round(0.299 * r + 0.587 * g + 0.114 * b))
      }
      imageW.value = w
      imageH.value = h
      useUpload.value = true
      uploadedName.value = raw.name
      ElMessage.success('图片已加载，可拖动滑块调参')
      render()
    }
    img.src = e.target.result
  }
  reader.readAsDataURL(raw)
}

function initControls() {
  const cfg = JSON.parse(props.courseware.configJson || '{}')
  const list = []
  if (type.value === '直方图') {
    list.push({ key: 'brightness', label: '亮度', value: cfg.min ?? 0, min: -100, max: 100 })
  } else if (type.value === '均衡化') {
    list.push({ key: 'gamma', label: 'γ 值', value: 1.0, min: 0.3, max: 3.0, step: 0.1 })
  } else if (type.value === '低通') {
    list.push({ key: 'kernel', label: '核大小', value: 3, min: 3, max: 11, step: 2 })
  } else if (type.value === '频域') {
    list.push({ key: 'radius', label: '截止半径', value: 100, min: 10, max: 200 })
  } else if (type.value === '形态学') {
    list.push({ key: 'kernelSize', label: '结构元素', value: 3, min: 3, max: 7, step: 2 })
  } else if (type.value === '分割') {
    list.push({ key: 'threshold', label: '阈值', value: 128, min: 0, max: 255 })
  } else if (type.value === '边缘') {
    list.push({ key: 'low', label: '低阈值', value: 50, min: 0, max: 255 })
    list.push({ key: 'high', label: '高阈值', value: 150, min: 0, max: 255 })
  } else if (type.value === '视频') {
    list.push({ key: 'speed', label: '播放速度', value: 1, min: 1, max: 10 })
  }
  controls.value = list
}

function getValue(key) {
  const c = controls.value.find(x => x.key === key)
  return c ? c.value : 0
}

function render() {
  // 参数调整埋点（防抖，避免频繁上报）
  trackParamAdjust()
  const ctx = canvas.value.getContext('2d')
  const w = imageW.value, h = imageH.value
  const img = new ImageData(w, h)
  for (let i = 0; i < baseImage.length; i++) {
    let v = baseImage[i]
    if (type.value === '直方图') {
      v += getValue('brightness')
    } else if (type.value === '均衡化') {
      const g = getValue('gamma')
      v = Math.pow(v / 255, 1 / g) * 255
    } else if (type.value === '低通') {
      const k = getValue('kernel')
      if (k > 1) v = blurAt(i, w, h, k)
    } else if (type.value === '频域') {
      const r = getValue('radius')
      const cx = w / 2, cy = h / 2
      const x = i % w - cx, y = Math.floor(i / w) - cy
      const freq = Math.sqrt(x * x + y * y)
      if (freq > r) v = 128 // 高频抑制
    } else if (type.value === '形态学') {
      const k = getValue('kernelSize')
      v = morphAt(i, w, h, k, opValue.value)
    } else if (type.value === '分割') {
      const t = getValue('threshold')
      v = v >= t ? 255 : 0
    } else if (type.value === '边缘') {
      const low = getValue('low'), high = getValue('high')
      const g = gradientAt(i, w, h)
      v = g > high ? 255 : (g > low ? 128 : 0)
    }
    v = Math.max(0, Math.min(255, Math.round(v)))
    const idx = i * 4
    img.data[idx] = v
    img.data[idx + 1] = v
    img.data[idx + 2] = v
    img.data[idx + 3] = 255
  }
  // 放大绘制
  ctx.clearRect(0, 0, canvasW, canvasH)
  const off = document.createElement('canvas')
  off.width = w; off.height = h
  off.getContext('2d').putImageData(img, 0, 0)
  ctx.imageSmoothingEnabled = false
  ctx.drawImage(off, 0, 0, canvasW, canvasH)

  // 单独绘制直方图
  renderHist()
}

/** 灰度直方图单独绘制到独立画布 */
function renderHist() {
  const hctx = histCanvas.value.getContext('2d')
  const hist = new Array(256).fill(0)
  for (let i = 0; i < baseImage.length; i++) {
    let v = baseImage[i]
    if (type.value === '直方图') v += getValue('brightness')
    if (type.value === '均衡化') v = Math.pow(v / 255, 1 / getValue('gamma')) * 255
    v = Math.max(0, Math.min(255, Math.round(v)))
    hist[v]++
  }
  const maxHist = Math.max(...hist, 1)
  const pad = 10
  const hw = histW - pad * 2
  const hh = histH - pad * 2 - 18
  hctx.clearRect(0, 0, histW, histH)
  // 背景
  hctx.fillStyle = 'rgba(255,255,255,0.9)'
  hctx.fillRect(0, 0, histW, histH)
  hctx.strokeStyle = '#E2E8F0'
  hctx.strokeRect(0.5, 0.5, histW - 1, histH - 1)
  // 柱状
  hctx.fillStyle = '#4F46E5'
  for (let b = 0; b < 256; b++) {
    const bh = (hist[b] / maxHist) * hh
    hctx.fillRect(pad + (b / 256) * hw, pad + hh - bh, Math.max(1, hw / 256), bh)
  }
  // 标签
  hctx.fillStyle = '#64748B'
  hctx.font = '12px sans-serif'
  hctx.fillText('灰度直方图（0 ~ 255）', pad, histH - 4)
}

function blurAt(i, w, h, k) {
  const r = Math.floor(k / 2)
  const x = i % w, y = Math.floor(i / w)
  let sum = 0, n = 0
  for (let dy = -r; dy <= r; dy++) {
    for (let dx = -r; dx <= r; dx++) {
      const nx = x + dx, ny = y + dy
      if (nx >= 0 && nx < w && ny >= 0 && ny < h) {
        sum += baseImage[ny * w + nx]; n++
      }
    }
  }
  return sum / n
}

function morphAt(i, w, h, k, op) {
  const r = Math.floor(k / 2)
  const x = i % w, y = Math.floor(i / w)
  let min = 255, max = 0
  for (let dy = -r; dy <= r; dy++) {
    for (let dx = -r; dx <= r; dx++) {
      const nx = x + dx, ny = y + dy
      if (nx >= 0 && nx < w && ny >= 0 && ny < h) {
        const v = baseImage[ny * w + nx]
        if (v < min) min = v
        if (v > max) max = v
      }
    }
  }
  if (op === '腐蚀') return min
  if (op === '膨胀') return max
  if (op === '开运算') return Math.min(255, Math.max(baseImage[i], min))
  return Math.max(0, Math.min(baseImage[i], max)) // 闭运算
}

function gradientAt(i, w, h) {
  const x = i % w, y = Math.floor(i / w)
  const right = x + 1 < w ? baseImage[y * w + x + 1] : baseImage[i]
  const down = y + 1 < h ? baseImage[(y + 1) * w + x] : baseImage[i]
  return Math.abs(baseImage[i] - right) + Math.abs(baseImage[i] - down)
}

async function submitChallenge() {
  const params = {}
  controls.value.forEach(c => { params[c.key] = c.value })
  try {
    await apiSubmitChallenge({
      lessonPackId: props.lessonPackId,
      coursewareId: props.courseware.id,
      paramsJson: JSON.stringify(params),
      conclusion: '已完成参数探索并观察效果'
    })
    ElMessage.success('挑战已提交！')
  } catch (e) {}
}

let trackTimer = null
function trackParamAdjust() {
  if (trackTimer) clearTimeout(trackTimer)
  trackTimer = setTimeout(() => {
    const params = {}
    controls.value.forEach(c => { params[c.key] = c.value })
    recordOperation({
      lessonPackId: props.lessonPackId ? Number(props.lessonPackId) : null,
      materialId: null,
      actionType: 'cw_param',
      actionDetail: '课件「' + (props.courseware.name || '') + '」调整参数：' + JSON.stringify(params)
    }).catch(() => {})
  }, 1500)
}
</script>

<style scoped>
.courseware {
  background: #fff;
  border-radius: 16px;
  padding: 16px;
  box-shadow: 0 4px 20px rgba(79, 70, 229, 0.06);
}
.cw-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; flex-wrap: wrap; gap: 8px; }
.cw-title { font-weight: 600; font-size: 16px; }
.cw-checkpoint { font-size: 13px; color: #B45309; background: #FEF3C7; padding: 4px 10px; border-radius: 12px; }
.cw-toolbar { display: flex; align-items: center; gap: 8px; margin-bottom: 12px; padding: 8px 12px; background: #FAFBFF; border-radius: 10px; flex-wrap: wrap; }
.cw-toolbar-label { font-size: 13px; color: #475569; font-weight: 500; }
.cw-uploaded { font-size: 12px; color: #4F46E5; }
.cw-body { display: flex; gap: 16px; align-items: stretch; }
.cw-left { flex: 1; display: flex; align-items: center; justify-content: center; }
.cw-canvas { border: 1px solid #E2E8F0; border-radius: 10px; background: #fff; max-width: 100%; }
.cw-right { flex: 1; display: flex; flex-direction: column; gap: 14px; min-width: 240px; }
.cw-hist { border: 1px solid #E2E8F0; border-radius: 10px; background: #fff; width: 100%; }
.cw-controls { display: flex; flex-direction: column; gap: 14px; flex: 1; justify-content: center; }
.control-item label { font-size: 13px; color: #475569; display: block; margin-bottom: 4px; }
.control-item input[type=range] { width: 100%; accent-color: #4F46E5; }
.op-btns { display: flex; gap: 6px; flex-wrap: wrap; }
.op-btn {
  border: 1px solid #E2E8F0; background: #F8FAFC; border-radius: 14px;
  padding: 4px 12px; font-size: 12px; cursor: pointer;
}
.op-btn.active { background: #EEF2FF; border-color: #4F46E5; color: #4F46E5; font-weight: 600; }
.cw-foot { margin-top: 12px; display: flex; justify-content: flex-end; }

@media (max-width: 700px) {
  .cw-body { flex-direction: column; }
}
</style>
