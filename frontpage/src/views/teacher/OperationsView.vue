<template>
  <div>
    <div class="header">
      <h2>📡 学习行为记录（埋点）</h2>
    </div>
    <p class="sub">查看学生全部学习行为：登录退出、材料浏览与停留、PPT页码、课件调参、AI问答、代码排错、挑战、互评、作业、知识点浏览</p>

    <div class="filter card">
      <el-form inline>
        <el-form-item label="课次">
          <el-select v-model="filter.lessonPackId" clearable placeholder="全部课次" style="width: 200px" @change="load">
            <el-option v-for="p in packs" :key="p.id" :label="`${p.lessonNo} ${p.title}`" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="学生">
          <el-select v-model="filter.studentId" clearable filterable placeholder="全部学生" style="width: 180px" @change="load">
            <el-option v-for="s in students" :key="s.id" :label="`${s.realName}(${s.username})`" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="行为类型">
          <el-select v-model="filter.actionType" clearable placeholder="全部类型" style="width: 180px" @change="load">
            <el-option v-for="t in actionTypes" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load">查询</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="card table-card">
      <el-table :data="records" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="studentName" label="学生" width="110" />
        <el-table-column prop="username" label="学号" width="110" />
        <el-table-column label="行为类型" width="140">
          <template #default="{ row }"><el-tag size="small" :type="typeColor(row.actionType)">{{ typeLabel(row.actionType) }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="actionDetail" label="详情" min-width="260" show-overflow-tooltip />
        <el-table-column label="停留(秒)" width="80">
          <template #default="{ row }">{{ row.duration || '—' }}</template>
        </el-table-column>
        <el-table-column label="时间" width="160">
          <template #default="{ row }">{{ formatTime(row.time) }}</template>
        </el-table-column>
      </el-table>
      <div v-if="records.length === 0" class="empty">暂无行为记录</div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { listOperations, listLessonPacks, listStudents } from '../../api'

const packs = ref([])
const students = ref([])
const records = ref([])
const filter = ref({ lessonPackId: null, studentId: null, actionType: '' })

const actionTypes = [
  { value: 'login', label: '登录' },
  { value: 'logout', label: '退出' },
  { value: 'open_pack', label: '打开课次包' },
  { value: 'open_material', label: '打开材料' },
  { value: 'close_material', label: '关闭材料' },
  { value: 'ppt_page', label: 'PPT翻页' },
  { value: 'cw_param', label: '课件调参' },
  { value: 'question', label: 'AI提问' },
  { value: 'followup', label: 'AI追问' },
  { value: 'image_question', label: '图片提问' },
  { value: 'knowledge_view', label: '知识点浏览' },
  { value: 'challenge', label: '挑战提交' },
  { value: 'homework_submit', label: '作业提交' }
]

onMounted(async () => {
  packs.value = await listLessonPacks()
  students.value = await listStudents()
  await load()
})

async function load() {
  const params = { limit: 500 }
  if (filter.value.lessonPackId) params.lessonPackId = filter.value.lessonPackId
  if (filter.value.studentId) params.studentId = filter.value.studentId
  if (filter.value.actionType) params.actionType = filter.value.actionType
  records.value = await listOperations(params)
}

function typeLabel(t) {
  const map = {
    login: '登录', logout: '退出', open_pack: '打开课次包',
    open_material: '打开材料', close_material: '关闭材料',
    ppt_page: 'PPT翻页', cw_param: '课件调参',
    question: 'AI提问', followup: 'AI追问', image_question: '图片提问',
    knowledge_view: '知识点浏览', challenge: '挑战提交',
    homework_submit: '作业提交'
  }
  return map[t] || t
}

function typeColor(t) {
  const map = {
    login: 'success', logout: 'info', open_pack: 'primary',
    open_material: 'primary', close_material: 'info', ppt_page: 'warning',
    cw_param: 'warning', question: 'danger', followup: 'danger',
    image_question: 'danger', knowledge_view: 'primary',
    challenge: 'success', homework_submit: 'success'
  }
  return map[t] || 'info'
}

function formatTime(t) {
  if (!t) return ''
  return String(t).replace('T', ' ').substring(0, 19)
}
</script>

<style scoped>
.header { margin-bottom: 8px; }
.sub { color: #64748B; margin-bottom: 16px; font-size: 14px; }
.filter { margin-bottom: 16px; }
.table-card { overflow: hidden; }
.empty { color: #94A3B8; text-align: center; padding: 40px; }
</style>
