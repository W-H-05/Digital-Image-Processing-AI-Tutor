<template>
  <div class="dashboard">
    <div class="dash-header">
      <h2>📊 实时学情看板</h2>
      <div class="dash-actions">
        <span class="live-badge"><span class="dot"></span> 实时</span>
        <el-select v-model="currentPackId" placeholder="全部课次" clearable style="width: 240px" @change="onPackChange">
          <el-option label="📚 全部课次（总览）" :value="null" />
          <el-option v-for="p in packs" :key="p.id" :label="`${p.lessonNo} ${p.title}`" :value="p.id" />
        </el-select>
        <el-button size="small" @click="openReport">📄 课后报告</el-button>
        <el-button size="small" @click="refresh">刷新</el-button>
      </div>
    </div>

    <!-- 概览卡片 -->
    <div class="overview">
      <div class="ov-card"><div class="ov-num" style="color:#4F46E5">{{ overview.online || 0 }}</div><div class="ov-label">在线人数</div></div>
      <div class="ov-card"><div class="ov-num" style="color:#06B6D4">{{ overview.operations || 0 }}</div><div class="ov-label">操作次数</div></div>
      <div class="ov-card"><div class="ov-num" style="color:#F59E0B">{{ overview.questions || 0 }}</div><div class="ov-label">提问数</div></div>
      <div class="ov-card"><div class="ov-num" style="color:#10B981">{{ overview.challenges || 0 }}</div><div class="ov-label">挑战提交</div></div>
      <div class="ov-card"><div class="ov-num" style="color:#8B5CF6">{{ overview.studentCount || 0 }}</div><div class="ov-label">学生总数</div></div>
    </div>

    <!-- 实时词云 -->
    <div class="panel card wordcloud-panel">
      <h3>☁️ 实时词云 · AI 提问热点</h3>
      <div v-if="wordCloud.length === 0" class="empty">暂无提问数据</div>
      <div v-else class="wordcloud">
        <span v-for="(w, i) in wordCloud" :key="i" class="wc-word"
          :style="{ fontSize: wcFont(w) + 'px', color: wcColor(i), opacity: wcOpacity(w) }">
          {{ w.name }}
        </span>
      </div>
    </div>

    <div class="grid">
      <!-- 掌握度柱状图 -->
      <div class="panel card">
        <h3>知识点掌握度</h3>
        <div ref="masteryChart" class="chart"></div>
      </div>
      <!-- 困惑点 Top5 -->
      <div class="panel card">
        <h3>集中困惑点 Top 5</h3>
        <div v-if="confusion.length === 0" class="empty">暂无数据</div>
        <div v-for="(c, i) in confusion" :key="i" class="conf-item">
          <span class="conf-rank">{{ i + 1 }}</span>
          <span class="conf-name">{{ c.name }}</span>
          <div class="conf-bar"><div class="conf-fill" :style="{ width: confWidth(c) }"></div></div>
          <span class="conf-count">{{ c.count }}次</span>
        </div>
      </div>
    </div>

    <!-- 实时动态流 -->
    <div class="panel card stream-panel">
      <h3>实时动态流</h3>
      <div class="stream-list">
        <div v-for="(s, i) in stream" :key="i" class="stream-item">
          <span class="stream-type">{{ typeEmoji(s.type) }}</span>
          <span class="stream-detail">{{ s.detail || s.type }}</span>
          <span class="stream-time">{{ formatTime(s.time) }}</span>
        </div>
        <div v-if="stream.length === 0" class="empty">等待学生操作…</div>
      </div>
    </div>

    <!-- 易错点排行 + 异常提醒 -->
    <div class="grid grid-bottom">
      <div class="panel card">
        <h3>🐛 易错点排行</h3>
        <div v-if="errors.length === 0" class="empty">暂无数据</div>
        <div v-for="(e, i) in errors" :key="i" class="conf-item">
          <span class="conf-rank">{{ i + 1 }}</span>
          <span class="conf-name">{{ e.type }}</span>
          <span v-if="e.source" class="err-source">{{ e.source }}</span>
          <div class="conf-bar"><div class="conf-fill err" :style="{ width: errWidth(e) }"></div></div>
          <span class="conf-count">{{ e.count }}次 / {{ e.studentCount }}人</span>
        </div>
      </div>
      <div class="panel card">
        <h3>⚠️ 异常学习提醒</h3>
        <div v-if="anomalies.length === 0" class="empty">暂无异常</div>
        <div v-for="(a, i) in anomalies" :key="i" class="anomaly-item">
          <span class="anomaly-tag">{{ a.type }}</span>
          <span class="anomaly-name">{{ a.studentName }}（{{ a.username }}）</span>
          <span class="anomaly-detail">{{ a.detail }}</span>
        </div>
      </div>
    </div>

    <!-- 课后报告弹窗 -->
    <el-dialog v-model="reportVisible" title="📄 课后课堂总结报告" width="650px">
      <div v-if="!report" class="loading">生成中…</div>
      <div v-else class="report-body">
        <div class="report-time">生成时间：{{ report.generateTime }}</div>
        <div class="report-section">
          <h4>📈 课堂概览</h4>
          <div class="report-stats">
            <span>在线 {{ report.overview?.online }}</span>
            <span>操作 {{ report.overview?.operations }}</span>
            <span>提问 {{ report.overview?.questions }}</span>
            <span>挑战 {{ report.overview?.challenges }}</span>
          </div>
        </div>
        <div class="report-section">
          <h4>🎯 薄弱知识点（困惑点 Top）</h4>
          <div v-for="(w, i) in report.weakPoints" :key="i" class="report-item">
            {{ i + 1 }}. {{ w.name }}（{{ w.count }} 次提问）
          </div>
          <div v-if="!report.weakPoints?.length" class="report-empty">暂无困惑点数据</div>
        </div>
        <div class="report-section">
          <h4>🐛 易错点排行</h4>
          <div v-for="(e, i) in report.errorRanking" :key="i" class="report-item">
            {{ i + 1 }}. {{ e.type }}（{{ e.count }} 次）
          </div>
          <div v-if="!report.errorRanking?.length" class="report-empty">暂无错误数据</div>
        </div>
        <div class="report-section suggestion">
          <h4>💡 教学建议</h4>
          <p>{{ report.suggestion }}</p>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import {
  dashboardOverview, dashboardTopConfusion, dashboardMastery, dashboardStream,
  dashboardErrorRanking, dashboardAnomalies, dashboardReport, dashboardWordCloud, listLessonPacks
} from '../../api'

const overview = ref({})
const confusion = ref([])
const stream = ref([])
const errors = ref([])
const anomalies = ref([])
const report = ref(null)
const reportVisible = ref(false)
const masteryChart = ref(null)
const packs = ref([])
const currentPackId = ref(null)
const wordCloud = ref([])
let chart = null
let ws = null
let timer = null

onMounted(async () => {
  packs.value = await listLessonPacks()
  await refresh()
  initChart()
  connectWs()
  timer = setInterval(refresh, 15000)
})

onUnmounted(() => {
  if (ws) ws.close()
  if (timer) clearInterval(timer)
  if (chart) chart.dispose()
})

async function refresh() {
  try {
    const params = {}
    if (currentPackId.value) params.lessonPackId = currentPackId.value
    const [ov, conf, master, st, err, ano, wc] = await Promise.all([
      dashboardOverview(params),
      dashboardTopConfusion(params),
      dashboardMastery(params),
      dashboardStream({ limit: 20 }),
      dashboardErrorRanking(params),
      dashboardAnomalies(params),
      dashboardWordCloud(params)
    ])
    overview.value = ov
    confusion.value = conf
    stream.value = st
    errors.value = err
    anomalies.value = ano
    wordCloud.value = wc
    renderMastery(master)
  } catch (e) {}
}

function onPackChange() {
  refresh()
}

async function openReport() {
  reportVisible.value = true
  report.value = null
  const params = {}
  if (currentPackId.value) params.lessonPackId = currentPackId.value
  report.value = await dashboardReport(params)
}

function initChart() {
  chart = echarts.init(masteryChart.value)
}

function renderMastery(master) {
  if (!chart) return
  const names = master.map(m => m.knowledgePoint)
  const values = master.map(m => m.mastery)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 30, bottom: 60 },
    xAxis: {
      type: 'category', data: names,
      axisLabel: { rotate: 30, fontSize: 10 }
    },
    yAxis: { type: 'value', max: 100, name: '掌握度' },
    series: [{
      type: 'bar', data: values,
      itemStyle: {
        borderRadius: [6, 6, 0, 0],
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#4F46E5' },
          { offset: 1, color: '#06B6D4' }
        ])
      },
      barMaxWidth: 40
    }]
  })
}

function connectWs() {
  const token = localStorage.getItem('token')
  const proto = location.protocol === 'https:' ? 'wss' : 'ws'
  ws = new WebSocket(`${proto}://${location.host}/ws/learning?token=${token}`)
  ws.onmessage = (e) => {
    try {
      const msg = JSON.parse(e.data)
      stream.value.unshift({
        type: msg.type, detail: msg.detail,
        time: new Date(msg.time).toLocaleTimeString()
      })
      if (stream.value.length > 50) stream.value.pop()
      // 实时刷新概览
      refresh()
    } catch (err) {}
  }
}

function confWidth(c) {
  const max = Math.max(...confusion.value.map(x => x.count), 1)
  return (c.count / max * 100) + '%'
}

function errWidth(e) {
  const max = Math.max(...errors.value.map(x => x.count), 1)
  return (e.count / max * 100) + '%'
}

function typeEmoji(t) {
  const map = { 'question': '💬', 'AI 提问': '💬', '图片提问': '🖼️', 'challenge': '🏆', 'open_material': '📖', 'open_pack': '📦', '代码排错': '🐛' }
  return map[t] || '⚡'
}

function wcFont(w) {
  const max = Math.max(...wordCloud.value.map(x => x.value), 1)
  return Math.round(14 + (w.value / max) * 22)
}
function wcOpacity(w) {
  const max = Math.max(...wordCloud.value.map(x => x.value), 1)
  return 0.6 + (w.value / max) * 0.4
}
function wcColor(i) {
  const colors = ['#4F46E5', '#06B6D4', '#F59E0B', '#10B981', '#EF4444', '#8B5CF6', '#0EA5E9']
  return colors[i % colors.length]
}

function formatTime(t) {
  if (!t) return ''
  if (typeof t === 'number') return new Date(t).toLocaleTimeString()
  return String(t).substring(11, 19) || t
}
</script>

<style scoped>
.dash-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; }
.dash-actions { display: flex; align-items: center; gap: 12px; }
.live-badge { display: flex; align-items: center; gap: 6px; color: #EF4444; font-size: 13px; font-weight: 600; }
.live-badge .dot { width: 8px; height: 8px; border-radius: 50%; background: #EF4444; animation: blink 1.5s infinite; }
@keyframes blink { 50% { opacity: 0.2; } }

.overview { display: grid; grid-template-columns: repeat(5, 1fr); gap: 14px; margin-bottom: 20px; }
.ov-card { background: #fff; border-radius: 14px; padding: 20px; text-align: center; box-shadow: 0 4px 16px rgba(0,0,0,0.05); }
.ov-num { font-size: 32px; font-weight: 700; }
.ov-label { color: #64748B; margin-top: 6px; font-size: 13px; }

.grid { display: grid; grid-template-columns: 1.4fr 1fr; gap: 16px; margin-bottom: 16px; }
.panel h3 { margin-bottom: 16px; font-size: 16px; }
.chart { height: 300px; }
.empty { color: #94A3B8; text-align: center; padding: 40px; }

.wordcloud-panel { margin-bottom: 16px; }
.wordcloud { display: flex; flex-wrap: wrap; gap: 12px 20px; align-items: center; justify-content: center; padding: 10px; min-height: 80px; }
.wc-word { font-weight: 600; line-height: 1.2; cursor: default; transition: transform 0.15s; }
.wc-word:hover { transform: scale(1.15); }

.conf-item { display: flex; align-items: center; gap: 10px; margin-bottom: 14px; }
.conf-rank { width: 22px; height: 22px; border-radius: 50%; background: #EEF2FF; color: #4F46E5; display: flex; align-items: center; justify-content: center; font-size: 12px; font-weight: 700; }
.conf-name { width: 90px; font-size: 13px; color: #374151; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.err-source { font-size: 11px; color: #94A3B8; background: #F1F5F9; padding: 1px 6px; border-radius: 8px; flex-shrink: 0; }
.conf-bar { flex: 1; height: 10px; background: #F1F5F9; border-radius: 5px; overflow: hidden; }
.conf-fill { height: 100%; background: linear-gradient(90deg, #F59E0B, #EF4444); border-radius: 5px; transition: width 0.5s; }
.conf-count { font-size: 12px; color: #94A3B8; white-space: nowrap; }

.stream-panel { margin-top: 0; }
.stream-list { max-height: 360px; overflow-y: auto; }
.stream-item { display: flex; align-items: center; gap: 10px; padding: 8px 0; border-bottom: 1px solid #F8FAFC; font-size: 13px; }
.stream-type { font-size: 16px; }
.stream-detail { flex: 1; color: #374151; }
.stream-time { color: #94A3B8; font-size: 12px; white-space: nowrap; }

.grid-bottom { margin-top: 16px; }
.conf-fill.err { background: linear-gradient(90deg, #F59E0B, #EF4444); }
.anomaly-item { display: flex; align-items: center; gap: 8px; padding: 10px 0; border-bottom: 1px solid #F8FAFC; font-size: 13px; flex-wrap: wrap; }
.anomaly-tag { background: #FEF2F2; color: #DC2626; padding: 2px 8px; border-radius: 8px; font-size: 12px; }
.anomaly-name { font-weight: 500; color: #1F2937; }
.anomaly-detail { color: #64748B; flex: 1; }

.loading { text-align: center; color: #94A3B8; padding: 40px; }
.report-body { line-height: 1.7; }
.report-time { color: #94A3B8; font-size: 13px; margin-bottom: 14px; }
.report-section { margin-bottom: 18px; }
.report-section h4 { margin-bottom: 8px; color: #1F2937; }
.report-stats { display: flex; gap: 16px; flex-wrap: wrap; }
.report-stats span { background: #EEF2FF; color: #4F46E5; padding: 6px 12px; border-radius: 8px; font-size: 13px; }
.report-item { padding: 6px 0; color: #374151; font-size: 14px; }
.report-empty { color: #94A3B8; font-size: 13px; }
.suggestion { background: #FEF3C7; padding: 12px 16px; border-radius: 10px; }
.suggestion p { color: #92400E; }

@media (max-width: 900px) {
  .overview { grid-template-columns: repeat(3, 1fr); }
  .grid { grid-template-columns: 1fr; }
  .grid-bottom { grid-template-columns: 1fr; }
}
</style>
