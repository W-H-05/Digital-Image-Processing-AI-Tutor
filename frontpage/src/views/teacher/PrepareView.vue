<template>
  <div>
    <div class="header">
      <h2>✨ 备课助手</h2>
      <p class="sub">上传教案，AI 自动抽取教学目标、重难点、思政、讨论题、挑战任务，生成课次包草案</p>
    </div>

    <div class="prepare-grid">
      <div class="card input-panel">
        <h3>📋 教案输入</h3>
        <el-input v-model="teachingPlan" type="textarea" :rows="14" placeholder="粘贴教案内容，或上传教案文件（文本内容）…" />
        <div class="input-actions">
          <el-upload :show-file-list="false" :auto-upload="false" accept=".txt,.md" :on-change="onFileChange">
            <el-button>📎 上传教案文件</el-button>
          </el-upload>
          <el-button type="primary" :loading="loading" @click="prepare">✨ AI 抽取</el-button>
        </div>
      </div>

      <div class="card result-panel">
        <h3>🎯 抽取结果</h3>
        <div v-if="!result && !loading" class="empty">点击「AI 抽取」生成备课素材</div>
        <div v-if="loading" class="loading-tip">🤖 贾维斯正在分析教案…</div>
        <div v-if="result && !loading" class="result">
          <div v-if="result.objectives" class="res-item"><b>教学目标：</b>{{ result.objectives }}</div>
          <div v-if="result.keyPoints" class="res-item"><b>重点：</b>{{ result.keyPoints }}</div>
          <div v-if="result.difficultPoints" class="res-item"><b>难点：</b>{{ result.difficultPoints }}</div>
          <div v-if="result.ideologicalNotes" class="res-item"><b>思政元素：</b>{{ result.ideologicalNotes }}</div>
          <div v-if="result.knowledgePoints" class="res-item">
            <b>知识点：</b>
            <div v-for="(kp, i) in result.knowledgePoints" :key="i" class="kp">{{ kp.name }}：{{ kp.description }}</div>
          </div>
          <div v-if="result.discussionQuestions" class="res-item">
            <b>讨论题：</b>
            <div v-for="(dq, i) in result.discussionQuestions" :key="i" class="kp">• {{ dq }}</div>
          </div>
          <div v-if="result.challengeTasks" class="res-item">
            <b>挑战任务：</b>
            <div v-for="(ct, i) in result.challengeTasks" :key="i" class="kp">• {{ ct }}</div>
          </div>
          <div v-if="result.raw" class="res-item"><b>原始返回：</b><pre>{{ result.raw }}</pre></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { aiPrepare } from '../../api'

const teachingPlan = ref('')
const result = ref(null)
const loading = ref(false)

function onFileChange(file) {
  const reader = new FileReader()
  reader.onload = (e) => {
    teachingPlan.value = e.target.result
  }
  reader.readAsText(file.raw)
}

async function prepare() {
  if (!teachingPlan.value.trim()) { ElMessage.warning('请先输入教案内容'); return }
  loading.value = true
  result.value = null
  try {
    result.value = await aiPrepare({ teachingPlan: teachingPlan.value })
  } catch (e) {
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.header { margin-bottom: 20px; }
.sub { color: #64748B; margin-top: 6px; font-size: 14px; }
.prepare-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.input-panel h3, .result-panel h3 { margin-bottom: 14px; }
.input-actions { display: flex; gap: 10px; margin-top: 14px; }
.empty { color: #94A3B8; text-align: center; padding: 60px 0; }
.loading-tip { color: #4F46E5; text-align: center; padding: 40px 0; font-size: 15px; }
.res-item { margin-bottom: 14px; line-height: 1.7; }
.kp { color: #475569; margin: 4px 0 4px 14px; }
.res-item pre { background: #1E293B; color: #E2E8F0; padding: 10px; border-radius: 8px; overflow-x: auto; font-size: 12px; }

@media (max-width: 900px) {
  .prepare-grid { grid-template-columns: 1fr; }
}
</style>
