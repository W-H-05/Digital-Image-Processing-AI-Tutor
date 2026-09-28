<template>
  <div>
    <div class="header">
      <h2>🤝 互评答辩管理</h2>
    </div>
    <p class="sub">设置互评量表、配置课堂互评活动（模式+计时）、统计互评数据</p>

    <el-tabs v-model="tab">
      <!-- 互评量表 -->
      <el-tab-pane label="📋 互评量表" name="rubric">
        <div class="tab-header">
          <el-button type="primary" @click="openRubric">➕ 新增量表</el-button>
        </div>
        <div class="rubric-list">
          <div v-for="r in rubrics" :key="r.id" class="rubric-item card">
            <div class="rubric-info">
              <div class="rubric-name">{{ r.name }}</div>
              <div class="rubric-desc">{{ r.description }}</div>
              <div class="rubric-dims">
                <span v-for="(d, i) in parseDims(r.dimensionsJson)" :key="i" class="dim-tag">
                  {{ d.name }}（{{ d.weight }}%）
                </span>
              </div>
            </div>
            <div class="rubric-actions">
              <el-button size="small" @click="editRubric(r)">编辑</el-button>
              <el-button size="small" type="danger" link @click="removeRubric(r)">删除</el-button>
            </div>
          </div>
        </div>
      </el-tab-pane>

      <!-- 互评活动 -->
      <el-tab-pane label="🎯 互评活动" name="activity">
        <div class="tab-header">
          <el-button type="primary" @click="openActivity">➕ 新建活动</el-button>
        </div>
        <el-table :data="activities" stripe>
          <el-table-column prop="name" label="活动名称" min-width="160" />
          <el-table-column label="模式" width="130">
            <template #default="{ row }"><el-tag size="small">{{ modeLabel(row.mode) }}</el-tag></template>
          </el-table-column>
          <el-table-column label="计时" width="100">
            <template #default="{ row }">{{ Math.round(row.durationSec / 60) }} 分钟</template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }"><el-tag size="small" :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag></template>
          </el-table-column>
          <el-table-column label="操作" width="220">
            <template #default="{ row }">
              <el-button v-if="row.status === 'PENDING'" size="small" type="success" @click="start(row)">开始计时</el-button>
              <el-button v-if="row.status === 'RUNNING'" size="small" type="warning" @click="finish(row)">结束</el-button>
              <el-button size="small" @click="editActivity(row)">编辑</el-button>
              <el-button size="small" type="danger" link @click="removeActivity(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- 统计 -->
      <el-tab-pane label="📊 互评统计" name="stats">
        <div class="tab-header">
          <el-button size="small" @click="loadStats">刷新</el-button>
          <span class="stats-total">共 {{ stats.totalReviews || 0 }} 条互评</span>
        </div>
        <el-table :data="stats.students || []" stripe>
          <el-table-column prop="studentName" label="学生" width="140" />
          <el-table-column prop="username" label="学号" width="120" />
          <el-table-column prop="reviewCount" label="收到评价数" width="110" />
          <el-table-column prop="avgScore" label="平均分" width="100" />
          <el-table-column label="评论" min-width="250">
            <template #default="{ row }">
              <span v-for="(c, i) in row.comments" :key="i" class="comment-tag">{{ c }}</span>
              <span v-if="!row.comments?.length" class="empty-text">暂无评论</span>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <!-- 量表弹窗 -->
    <el-dialog v-model="rubricDialog" :title="editingRubric ? '编辑量表' : '新增量表'" width="620px">
      <el-form label-width="90px">
        <el-form-item label="量表名称"><el-input v-model="rubricForm.name" /></el-form-item>
        <el-form-item label="说明"><el-input v-model="rubricForm.description" /></el-form-item>
        <el-form-item label="评分维度">
          <div class="dim-editor">
            <div v-for="(d, i) in rubricDims" :key="i" class="dim-row">
              <el-input v-model="d.name" placeholder="维度名" style="width: 140px" />
              <el-input v-model="d.desc" placeholder="描述" style="flex: 1" />
              <el-input-number v-model="d.weight" :min="0" :max="100" size="small" /> %
              <el-button size="small" type="danger" link @click="rubricDims.splice(i, 1)">删</el-button>
            </div>
            <el-button size="small" @click="rubricDims.push({ name: '', desc: '', weight: 10 })">➕ 添加维度</el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rubricDialog = false">取消</el-button>
        <el-button type="primary" @click="saveRubric">保存</el-button>
      </template>
    </el-dialog>

    <!-- 活动弹窗 -->
    <el-dialog v-model="activityDialog" :title="editingActivity ? '编辑活动' : '新建活动'" width="560px">
      <el-form label-width="90px">
        <el-form-item label="活动名称"><el-input v-model="activityForm.name" /></el-form-item>
        <el-form-item label="互评模式">
          <el-select v-model="activityForm.mode" style="width: 100%">
            <el-option label="🖼️ 画廊漫步（作品展示互评）" value="gallery" />
            <el-option label="💬 微型辩论（观点交锋互评）" value="debate" />
            <el-option label="🧩 专家拼图（分组专长互评）" value="jigsaw" />
          </el-select>
        </el-form-item>
        <el-form-item label="关联量表">
          <el-select v-model="activityForm.rubricId" style="width: 100%" placeholder="选择量表">
            <el-option v-for="r in rubrics" :key="r.id" :label="r.name" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="关联课次">
          <el-select v-model="activityForm.lessonPackId" style="width: 100%" placeholder="可选">
            <el-option v-for="p in packs" :key="p.id" :label="`${p.lessonNo} ${p.title}`" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="计时时长">
          <el-input-number v-model="activityForm.durationSec" :min="60" :max="7200" :step="60" /> 秒
          <span class="hint">（{{ Math.round(activityForm.durationSec / 60) }} 分钟）</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="activityDialog = false">取消</el-button>
        <el-button type="primary" @click="saveActivity">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listRubrics, addRubric, updateRubric, deleteRubric,
  listActivities, addActivity, updateActivity, startActivity, finishActivity, deleteActivity,
  reviewStats, listLessonPacks
} from '../../api'

const tab = ref('rubric')
const rubrics = ref([])
const activities = ref([])
const stats = ref({})
const packs = ref([])

const rubricDialog = ref(false)
const editingRubric = ref(false)
const rubricForm = ref({})
const rubricDims = ref([])

const activityDialog = ref(false)
const editingActivity = ref(false)
const activityForm = ref({})

onMounted(async () => {
  await loadAll()
})

async function loadAll() {
  rubrics.value = await listRubrics()
  activities.value = await listActivities()
  packs.value = await listLessonPacks()
  await loadStats()
}

async function loadStats() {
  stats.value = await reviewStats({})
}

function parseDims(json) {
  try { return JSON.parse(json || '[]') } catch (e) { return [] }
}

function modeLabel(m) {
  const map = { gallery: '画廊漫步', debate: '微型辩论', jigsaw: '专家拼图' }
  return map[m] || m
}
function statusLabel(s) {
  const map = { PENDING: '待开始', RUNNING: '进行中', FINISHED: '已结束' }
  return map[s] || s
}
function statusType(s) {
  const map = { PENDING: 'info', RUNNING: 'success', FINISHED: 'warning' }
  return map[s] || 'info'
}

// 量表
function openRubric() {
  editingRubric.value = false
  rubricForm.value = { name: '', description: '' }
  rubricDims.value = [{ name: '', desc: '', weight: 20 }]
  rubricDialog.value = true
}
function editRubric(r) {
  editingRubric.value = true
  rubricForm.value = { id: r.id, name: r.name, description: r.description }
  rubricDims.value = parseDims(r.dimensionsJson)
  rubricDialog.value = true
}
async function saveRubric() {
  if (!rubricForm.value.name) { ElMessage.warning('请输入量表名称'); return }
  const dims = rubricDims.value.filter(d => d.name)
  if (dims.length === 0) { ElMessage.warning('至少配置一个评分维度'); return }
  const payload = {
    name: rubricForm.value.name,
    description: rubricForm.value.description,
    dimensionsJson: JSON.stringify(dims)
  }
  if (editingRubric.value) await updateRubric(rubricForm.value.id, payload)
  else await addRubric(payload)
  ElMessage.success('已保存')
  rubricDialog.value = false
  loadAll()
}
async function removeRubric(r) {
  try {
    await ElMessageBox.confirm(`确定删除量表「${r.name}」？`, '删除', { type: 'warning' })
    await deleteRubric(r.id)
    ElMessage.success('已删除')
    loadAll()
  } catch (e) {}
}

// 活动
function openActivity() {
  editingActivity.value = false
  activityForm.value = { name: '', mode: 'gallery', rubricId: rubrics.value[0]?.id, durationSec: 300 }
  activityDialog.value = true
}
function editActivity(a) {
  editingActivity.value = true
  activityForm.value = { ...a }
  activityDialog.value = true
}
async function saveActivity() {
  if (!activityForm.value.name) { ElMessage.warning('请输入活动名称'); return }
  if (editingActivity.value) await updateActivity(activityForm.value.id, activityForm.value)
  else await addActivity(activityForm.value)
  ElMessage.success('已保存')
  activityDialog.value = false
  loadAll()
}
async function start(a) {
  await startActivity(a.id)
  ElMessage.success('活动已开始计时')
  loadAll()
}
async function finish(a) {
  await finishActivity(a.id)
  ElMessage.success('活动已结束')
  loadAll()
}
async function removeActivity(a) {
  try {
    await ElMessageBox.confirm(`确定删除活动「${a.name}」？`, '删除', { type: 'warning' })
    await deleteActivity(a.id)
    ElMessage.success('已删除')
    loadAll()
  } catch (e) {}
}
</script>

<style scoped>
.header { margin-bottom: 8px; }
.sub { color: #64748B; margin-bottom: 16px; font-size: 14px; }
.tab-header { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
.stats-total { color: #64748B; font-size: 13px; }

.rubric-list { display: flex; flex-direction: column; gap: 12px; }
.rubric-item { display: flex; align-items: center; justify-content: space-between; padding: 16px 20px; }
.rubric-name { font-weight: 600; font-size: 15px; }
.rubric-desc { color: #64748B; font-size: 13px; margin-top: 4px; }
.rubric-dims { display: flex; gap: 8px; margin-top: 8px; flex-wrap: wrap; }
.dim-tag { background: #EEF2FF; color: #4F46E5; padding: 2px 10px; border-radius: 12px; font-size: 12px; }
.rubric-actions { display: flex; gap: 6px; }

.dim-editor { width: 100%; }
.dim-row { display: flex; gap: 8px; align-items: center; margin-bottom: 8px; }
.hint { margin-left: 8px; color: #94A3B8; font-size: 12px; }
.comment-tag { display: inline-block; background: #F1F5F9; color: #475569; padding: 2px 8px; border-radius: 8px; font-size: 12px; margin: 2px; }
.empty-text { color: #94A3B8; font-size: 12px; }
</style>
