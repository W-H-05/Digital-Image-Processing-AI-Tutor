<template>
  <div>
    <div class="header">
      <h2>📝 Prompt 模板管理</h2>
      <el-button type="primary" @click="openAdd">➕ 新增模板</el-button>
    </div>
    <p class="sub">管理 AI 问答、代码排错、函数查询、备课助手、图片分析等场景的 Prompt 模板</p>

    <div class="card table-card">
      <el-table :data="prompts" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="模板名称" width="140" />
        <el-table-column prop="purpose" label="用途" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="purposeType(row.purpose)">{{ purposeLabel(row.purpose) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="内容" min-width="300" show-overflow-tooltip />
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-button size="small" @click="openEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" link @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑模板' : '新增模板'" width="650px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="模板名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="用途">
          <el-select v-model="form.purpose" style="width: 100%">
            <el-option v-for="p in purposes" :key="p.value" :label="p.label" :value="p.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="模板内容">
          <el-input v-model="form.content" type="textarea" :rows="8" placeholder="输入 Prompt 模板内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listPrompts, addPrompt, updatePrompt, deletePrompt } from '../../api'

const prompts = ref([])
const dialogVisible = ref(false)
const editing = ref(false)
const form = ref({})
const purposes = [
  { value: 'chat', label: 'RAG 问答' },
  { value: 'code', label: '代码排错' },
  { value: 'func', label: '函数查询' },
  { value: 'prepare', label: '备课助手' },
  { value: 'vision', label: '图片分析' }
]

onMounted(load)

async function load() {
  prompts.value = await listPrompts()
}

function purposeType(p) {
  const map = { chat: 'success', code: 'warning', func: 'info', prepare: 'primary', vision: 'danger' }
  return map[p] || 'info'
}
function purposeLabel(p) {
  const map = { chat: 'RAG 问答', code: '代码排错', func: '函数查询', prepare: '备课助手', vision: '图片分析' }
  return map[p] || p
}

function openAdd() {
  editing.value = false
  form.value = { name: '', purpose: 'chat', content: '' }
  dialogVisible.value = true
}
function openEdit(row) {
  editing.value = true
  form.value = { ...row }
  dialogVisible.value = true
}
async function save() {
  if (!form.value.name || !form.value.content) { ElMessage.warning('名称和内容不能为空'); return }
  if (editing.value) await updatePrompt(form.value.id, form.value)
  else await addPrompt(form.value)
  ElMessage.success('已保存')
  dialogVisible.value = false
  load()
}
async function remove(row) {
  try {
    await ElMessageBox.confirm(`确定删除模板「${row.name}」？`, '删除', { type: 'warning' })
    await deletePrompt(row.id)
    ElMessage.success('已删除')
    load()
  } catch (e) {}
}
</script>

<style scoped>
.header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; }
.sub { color: #64748B; margin-bottom: 16px; font-size: 14px; }
.table-card { overflow: hidden; }
</style>
