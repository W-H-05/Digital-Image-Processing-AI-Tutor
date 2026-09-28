<template>
  <div>
    <div class="header">
      <h2>📚 RAG 知识库管理</h2>
      <el-button type="primary" @click="openAdd">➕ 新增知识文档</el-button>
    </div>
    <p class="sub">维护 AI 问答的知识来源：教材、教案、OpenCV 文档、NumPy 文档、报错库等</p>

    <div class="filter">
      <el-select v-model="category" placeholder="按分类筛选" clearable style="width: 200px" @change="load">
        <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
      </el-select>
    </div>

    <div class="card table-card">
      <el-table :data="docs" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="title" label="标题" min-width="180" />
        <el-table-column prop="category" label="分类" width="120">
          <template #default="{ row }"><el-tag size="small">{{ row.category }}</el-tag></template>
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

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑知识文档' : '新增知识文档'" width="650px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="标题"><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.category" style="width: 100%">
            <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="form.content" type="textarea" :rows="8" placeholder="输入知识片段，用于 RAG 检索" />
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
import { listRagDocs, addRagDoc, updateRagDoc, deleteRagDoc } from '../../api'

const docs = ref([])
const category = ref('')
const categories = ['教材', '教案', 'OpenCV', 'NumPy', '报错库']
const dialogVisible = ref(false)
const editing = ref(false)
const form = ref({})

onMounted(load)

async function load() {
  const params = category.value ? { category: category.value } : {}
  docs.value = await listRagDocs(params)
}

function openAdd() {
  editing.value = false
  form.value = { title: '', category: '教材', content: '' }
  dialogVisible.value = true
}

function openEdit(row) {
  editing.value = true
  form.value = { ...row }
  dialogVisible.value = true
}

async function save() {
  if (!form.value.title || !form.value.content) { ElMessage.warning('标题和内容不能为空'); return }
  if (editing.value) await updateRagDoc(form.value.id, form.value)
  else await addRagDoc(form.value)
  ElMessage.success('已保存，索引已更新')
  dialogVisible.value = false
  load()
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(`确定删除「${row.title}」？删除后 AI 将无法检索到该内容`, '删除', { type: 'warning' })
    await deleteRagDoc(row.id)
    ElMessage.success('已删除')
    load()
  } catch (e) {}
}
</script>

<style scoped>
.header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; }
.sub { color: #64748B; margin-bottom: 16px; font-size: 14px; }
.filter { margin-bottom: 14px; }
.table-card { overflow: hidden; }
</style>
