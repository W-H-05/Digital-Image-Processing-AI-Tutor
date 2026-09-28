<template>
  <div class="self-page">
    <header class="topbar">
      <el-button text @click="$router.push('/home')">← 返回</el-button>
      <span class="title">📊 学情自查</span>
      <span class="user">{{ userStore.realName || userStore.username }}</span>
    </header>

    <main class="content">
      <div class="stats-cards">
        <div class="stat card"><div class="stat-num">{{ stats.questionCount || 0 }}</div><div class="stat-label">提问次数</div></div>
        <div class="stat card"><div class="stat-num">{{ stats.challengeCount || 0 }}</div><div class="stat-label">挑战提交</div></div>
        <div class="stat card"><div class="stat-num">{{ stats.operationCount || 0 }}</div><div class="stat-label">学习操作</div></div>
      </div>

      <!-- 掌握度雷达图 -->
      <div class="card mastery-card">
        <h3>🧭 知识点掌握度</h3>
        <div v-if="mastery.length === 0" class="empty">暂无掌握度数据，多参与课件操作和挑战即可生成</div>
        <div v-else ref="radarChart" class="chart"></div>
      </div>

      <div class="qa-section card">
        <h3>💬 我的问答记录</h3>
        <div v-if="qaRecords.length === 0" class="empty">暂无记录，去课次包找「贾维斯」提问吧！</div>
        <div v-for="q in qaRecords" :key="q.id" class="qa-item">
          <div class="qa-q">
            <span class="qa-tag" :class="{ img: q.hasImage }">{{ q.hasImage ? '🖼️' : '💬' }}</span>
            {{ q.question }}
            <span class="qa-time">{{ formatTime(q.createTime) }}</span>
          </div>
          <div class="qa-a md-body" v-html="renderMarkdown(truncate(q.answer))"></div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { useUserStore } from '../../store/user'
import { myQaRecords, selfStats } from '../../api'
import { renderMarkdown } from '../../utils/markdown'

const userStore = useUserStore()
const qaRecords = ref([])
const stats = ref({})
const mastery = ref([])
const radarChart = ref(null)
let chart = null

onMounted(async () => {
  try {
    const [qa, st] = await Promise.all([myQaRecords(), selfStats()])
    qaRecords.value = qa
    stats.value = st
    mastery.value = st.mastery || []
    if (mastery.value.length) {
      await nextTick()
      renderRadar()
    }
  } catch (e) {}
})

const imageCount = computed(() => qaRecords.value.filter(q => q.hasImage).length)

function renderRadar() {
  chart = echarts.init(radarChart.value)
  const names = mastery.value.map(m => m.knowledgePoint)
  const values = mastery.value.map(m => m.mastery)
  chart.setOption({
    tooltip: {},
    radar: {
      indicator: names.map(n => ({ name: n, max: 100 })),
      radius: '65%'
    },
    series: [{
      type: 'radar',
      data: [{
        value: values,
        name: '掌握度',
        areaStyle: { color: 'rgba(79,70,229,0.25)' },
        lineStyle: { color: '#4F46E5' },
        itemStyle: { color: '#4F46E5' }
      }]
    }]
  })
}

function formatTime(t) {
  if (!t) return ''
  return t.replace('T', ' ').substring(0, 16)
}
function truncate(text) {
  if (!text) return ''
  return text.length > 200 ? text.substring(0, 200) + '…' : text
}
</script>

<style scoped>
.self-page { min-height: 100vh; }
.topbar { display: flex; align-items: center; gap: 16px; padding: 12px 24px; background: #fff; box-shadow: 0 2px 10px rgba(0,0,0,0.04); }
.title { font-weight: 700; }
.user { margin-left: auto; color: #64748B; }
.content { max-width: 900px; margin: 0 auto; padding: 24px; }
.stats-cards { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; margin-bottom: 20px; }
.stat { text-align: center; padding: 24px; }
.stat-num { font-size: 34px; font-weight: 700; background: linear-gradient(135deg, #4F46E5, #06B6D4); -webkit-background-clip: text; background-clip: text; -webkit-text-fill-color: transparent; }
.stat-label { color: #64748B; margin-top: 4px; }
.mastery-card { margin-bottom: 20px; }
.mastery-card h3 { margin-bottom: 12px; }
.chart { height: 320px; }
.empty { color: #94A3B8; text-align: center; padding: 30px; }
.qa-section h3 { margin-bottom: 16px; }
.qa-item { padding: 14px 0; border-bottom: 1px solid #F1F5F9; }
.qa-q { font-weight: 600; color: #1F2937; display: flex; align-items: flex-start; gap: 8px; }
.qa-tag { font-size: 16px; }
.qa-tag.img { background: #EEF2FF; border-radius: 6px; padding: 2px 6px; }
.qa-time { margin-left: auto; font-weight: 400; font-size: 12px; color: #94A3B8; white-space: nowrap; }
.qa-a { margin-top: 8px; color: #475569; font-size: 14px; padding-left: 26px; }
</style>
