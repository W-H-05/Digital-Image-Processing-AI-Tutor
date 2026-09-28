<template>
  <div>
    <div class="header">
      <h2>👥 学生管理</h2>
      <div class="actions">
        <el-button type="danger" plain :disabled="selectedIds.length === 0" @click="batchReset">
          🔑 批量重置密码（{{ selectedIds.length }}）
        </el-button>
        <el-upload :show-file-list="false" :auto-upload="false" accept=".csv" :on-change="onCsvChange">
          <el-button type="primary">📥 批量导入 CSV</el-button>
        </el-upload>
        <el-button @click="manualImport">✏️ 手动录入</el-button>
      </div>
    </div>

    <div class="import-tip card">
      <p>CSV 格式：<code>学号,姓名,班级</code>（每行一名学生，初始密码 = 学号）</p>
      <div v-if="csvPreview" class="csv-preview">
        <div class="csv-head">导入预览（共 {{ csvPreview.length }} 行）：</div>
        <div v-for="(row, i) in csvPreview" :key="i" class="csv-row">{{ row }}</div>
        <el-button type="primary" size="small" @click="confirmImport" class="confirm-btn">确认导入</el-button>
      </div>
    </div>

    <div class="card table-card">
      <el-table :data="students" stripe style="width: 100%" @selection-change="onSelectionChange">
        <el-table-column type="selection" width="50" />
        <el-table-column prop="username" label="学号" min-width="130" />
        <el-table-column prop="realName" label="姓名" min-width="120" />
        <el-table-column prop="className" label="班级" min-width="130" />
        <el-table-column prop="questionCount" label="提问数" width="90" />
        <el-table-column prop="operationCount" label="操作数" width="90" />
        <el-table-column prop="lastLoginTime" label="最后登录" min-width="180">
          <template #default="{ row }">{{ row.lastLoginTime ? String(row.lastLoginTime).replace('T',' ').substring(0,16) : '—' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="showDetail(row)">学习记录</el-button>
            <el-button size="small" type="warning" link @click="reset(row)">重置密码</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 学生学习记录详情 -->
    <el-drawer v-model="detailDrawer" :title="`学习记录：${detail.realName || ''}（${detail.username || ''}）`" size="650px">
      <div v-if="!detail.username" class="loading">加载中…</div>
      <template v-else>
        <div class="detail-stats">
          <div class="d-stat"><div class="d-num">{{ detail.questionCount }}</div><div class="d-label">提问</div></div>
          <div class="d-stat"><div class="d-num">{{ detail.operationCount }}</div><div class="d-label">操作</div></div>
          <div class="d-stat"><div class="d-num">{{ detail.challengeCount }}</div><div class="d-label">挑战</div></div>
          <div class="d-stat"><div class="d-num">{{ detail.peerReviewCount }}</div><div class="d-label">互评</div></div>
          <div class="d-stat"><div class="d-num">{{ detail.materialViewCount }}</div><div class="d-label">阅材</div></div>
        </div>

        <el-tabs>
          <el-tab-pane label="问答记录" :name="1">
            <div v-if="detail.qaRecords?.length === 0" class="empty">暂无记录</div>
            <div v-for="q in detail.qaRecords" :key="q.id" class="rec-item">
              <div class="rec-q">💬 {{ q.question }}</div>
              <div class="rec-a">{{ truncate(q.answer) }}</div>
              <div class="rec-time">{{ formatTime(q.createTime) }}</div>
            </div>
          </el-tab-pane>
          <el-tab-pane label="操作记录" :name="2">
            <div v-if="detail.operations?.length === 0" class="empty">暂无记录</div>
            <div v-for="o in detail.operations" :key="o.id" class="rec-item">
              <div class="rec-q">⚡ {{ o.actionType }} · {{ o.actionDetail || '—' }}</div>
              <div class="rec-time">{{ formatTime(o.createTime) }}</div>
            </div>
          </el-tab-pane>
          <el-tab-pane label="挑战提交" :name="3">
            <div v-if="detail.challenges?.length === 0" class="empty">暂无记录</div>
            <div v-for="c in detail.challenges" :key="c.id" class="rec-item">
              <div class="rec-q">🏆 {{ c.conclusion || '（无结论）' }}</div>
              <div class="rec-a">{{ c.paramsJson }}</div>
              <div class="rec-time">{{ formatTime(c.createTime) }}</div>
            </div>
          </el-tab-pane>
          <el-tab-pane label="材料阅览" :name="4">
            <div v-if="detail.materialViews?.length === 0" class="empty">暂无记录</div>
            <div v-for="v in detail.materialViews" :key="v.id" class="rec-item">
              <div class="rec-q">📖 {{ v.actionDetail || '材料' + (v.materialId || '') }}</div>
              <div class="rec-time">{{ formatTime(v.createTime) }}</div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listStudents, importStudents, resetStudentPassword, batchResetStudents, getStudentDetail
} from '../../api'

const students = ref([])
const csvPreview = ref(null)
const selectedIds = ref([])
const detailDrawer = ref(false)
const detail = ref({})
let csvContent = ''

onMounted(load)

async function load() {
  students.value = await listStudents()
}

function onSelectionChange(rows) {
  selectedIds.value = rows.map(r => r.id)
}

function onCsvChange(file) {
  const reader = new FileReader()
  reader.onload = (e) => {
    csvContent = e.target.result
    const lines = csvContent.split(/\r?\n/).filter(l => l.trim())
    csvPreview.value = lines.slice(0, 10)
    if (lines.length > 10) csvPreview.value.push(`… 共 ${lines.length} 行`)
  }
  reader.readAsText(file.raw)
}

async function confirmImport() {
  try {
    const res = await importStudents(csvContent)
    ElMessage.success(`成功导入 ${res.imported} 名学生`)
    csvPreview.value = null
    load()
  } catch (e) {}
}

async function manualImport() {
  try {
    const { value } = await ElMessageBox.prompt('请输入（格式：学号,姓名,班级 每行一名）', '手动录入', {
      inputType: 'textarea',
      inputPlaceholder: '20240001,张三,计科2401\n20240002,李四,计科2401'
    })
    const res = await importStudents(value)
    ElMessage.success(`成功导入 ${res.imported} 名学生`)
    load()
  } catch (e) {}
}

async function reset(row) {
  try {
    await ElMessageBox.confirm(`确定将学生 ${row.realName}（${row.username}）的密码重置为学号？`, '重置密码', { type: 'warning' })
    await resetStudentPassword(row.id)
    ElMessage.success('密码已重置为学号')
  } catch (e) {}
}

async function batchReset() {
  try {
    await ElMessageBox.confirm(`确定将选中的 ${selectedIds.value.length} 名学生的密码重置为学号？`, '批量重置密码', { type: 'warning' })
    const res = await batchResetStudents(selectedIds.value)
    ElMessage.success(`已重置 ${res.reset} 名学生密码`)
  } catch (e) {}
}

async function showDetail(row) {
  detailDrawer.value = true
  detail.value = {}
  detail.value = await getStudentDetail(row.id)
}

function formatTime(t) {
  if (!t) return ''
  return String(t).replace('T', ' ').substring(0, 16)
}
function truncate(text) {
  if (!text) return ''
  return text.length > 80 ? text.substring(0, 80) + '…' : text
}
</script>

<style scoped>
.header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; flex-wrap: wrap; gap: 10px; }
.actions { display: flex; gap: 10px; flex-wrap: wrap; }
.import-tip { margin-bottom: 16px; }
.import-tip code { background: #EEF2FF; color: #4F46E5; padding: 2px 6px; border-radius: 4px; }
.csv-preview { margin-top: 12px; }
.csv-head { font-weight: 600; margin-bottom: 6px; }
.csv-row { color: #475569; font-size: 13px; }
.confirm-btn { margin-top: 10px; }
.table-card { overflow: hidden; }

.loading { text-align: center; color: #94A3B8; padding: 40px; }
.detail-stats { display: grid; grid-template-columns: repeat(5, 1fr); gap: 10px; margin-bottom: 16px; }
.d-stat { text-align: center; background: #FAFBFF; border-radius: 10px; padding: 12px; }
.d-num { font-size: 24px; font-weight: 700; color: #4F46E5; }
.d-label { font-size: 12px; color: #64748B; margin-top: 2px; }
.empty { color: #94A3B8; text-align: center; padding: 30px; }
.rec-item { padding: 12px 0; border-bottom: 1px solid #F1F5F9; }
.rec-q { font-weight: 500; color: #1F2937; }
.rec-a { color: #64748B; font-size: 13px; margin-top: 4px; }
.rec-time { color: #94A3B8; font-size: 12px; margin-top: 4px; }
</style>
