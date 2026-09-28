<template>
  <div>
    <div class="header">
      <h2>⚙️ 系统配置</h2>
    </div>
    <p class="sub">配置大模型 API、调用限流、数据备份，修改后立即生效（密钥不硬编码，默认值来自 .env）</p>

    <!-- AI 服务配置 -->
    <div class="card config-card">
      <h3 class="sec-title">🤖 大模型 API 配置</h3>
      <div class="config-row" v-for="c in aiConfigs" :key="c.key">
        <div class="config-label">{{ c.label }}</div>
        <el-input v-model="c.value" :placeholder="c.placeholder" :type="c.secret ? 'password' : 'text'" show-password />
        <span class="config-hint">{{ c.hint }}</span>
      </div>
    </div>

    <!-- 限流配置 -->
    <div class="card config-card">
      <h3 class="sec-title">🚦 API 调用限流（控制成本）</h3>
      <p class="desc">设置每个场景在时间窗口内的最大调用次数，超出后自动拒绝</p>
      <div class="rate-grid">
        <div v-for="r in rateConfigs" :key="r.label" class="rate-item">
          <div class="rate-label">{{ r.label }}</div>
          <div class="rate-inputs">
            <el-input-number v-model="r.limit" :min="1" :max="10000" size="small" />
            <span class="rate-unit">次 /</span>
            <el-input-number v-model="r.windowSec" :min="60" :max="86400" :step="60" size="small" />
            <span class="rate-unit">秒</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 备份配置 -->
    <div class="card config-card">
      <h3 class="sec-title">💾 数据备份</h3>
      <div class="config-row">
        <div class="config-label">备份目录</div>
        <el-input v-model="backupPath" placeholder="./backup" style="max-width: 300px" />
        <div class="config-label" style="width:auto">保留份数</div>
        <el-input-number v-model="backupKeepCount" :min="1" :max="100" />
      </div>
      <div class="backup-actions">
        <el-button type="primary" :loading="backingUp" @click="runBackup">💾 立即备份</el-button>
        <span v-if="lastBackup" class="backup-result">✅ 最近备份：{{ lastBackup.fileName }}（{{ formatSize(lastBackup.size) }}）</span>
      </div>
      <div v-if="backups.length" class="backup-list">
        <div class="backup-list-title">历史备份：</div>
        <div v-for="b in backups" :key="b.fileName" class="backup-item">
          <span>📦 {{ b.fileName }}</span>
          <span class="backup-meta">{{ formatSize(b.size) }} · {{ b.time }}</span>
          <a :href="'/api/system/backup/download/' + b.fileName" class="backup-download" target="_blank">下载</a>
        </div>
      </div>
    </div>

    <div class="config-actions">
      <el-button type="primary" size="large" @click="save">💾 保存全部配置</el-button>
    </div>

    <div class="card note-card">
      <h3>💡 说明</h3>
      <ul>
        <li>API Key 默认从 <code>backend/.env</code> 读取（不硬编码），此处填写会覆盖并实时生效。</li>
        <li>数据库连接信息在 <code>.env</code> 中配置（DB_HOST/DB_PORT/DB_NAME/DB_USERNAME/DB_PASSWORD），修改后需重启后端。</li>
        <li>限流配置修改后立即生效，用于控制大模型 API 调用成本。</li>
      </ul>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getSystemConfig, updateSystemConfig, doBackup, listBackups } from '../../api'

const aiConfigs = ref([
  { key: 'ai.baseUrl', label: 'API 地址', value: '', placeholder: 'https://api.deepseek.com', hint: 'OpenAI 兼容接口地址' },
  { key: 'ai.apiKey', label: 'API Key', value: '', placeholder: 'sk-...', hint: '密钥，加密存储', secret: true },
  { key: 'ai.model', label: '文本模型', value: '', placeholder: 'deepseek-chat', hint: '对话/问答模型' },
  { key: 'ai.visionModel', label: '视觉模型', value: '', placeholder: 'deepseek-vl', hint: '图片分析模型' }
])

const rateConfigs = ref([
  { label: 'AI 问答', key: 'ai-chat' },
  { label: '图片分析', key: 'ai-image' },
  { label: '代码排错', key: 'ai-code' },
  { label: '函数查询', key: 'ai-func' },
  { label: '备课助手', key: 'ai-prepare' }
])

const backupPath = ref('./backup')
const backupKeepCount = ref(10)
const backups = ref([])
const backingUp = ref(false)
const lastBackup = ref(null)

onMounted(load)

async function load() {
  const cfg = await getSystemConfig()
  aiConfigs.value.forEach(c => {
    if (cfg[c.key] !== undefined) c.value = cfg[c.key]
  })
  rateConfigs.value.forEach(r => {
    const limit = cfg[`ratelimit.${r.key}.limit`]
    const window = cfg[`ratelimit.${r.key}.windowSec`]
    r.limit = limit !== undefined ? parseInt(limit) : 30
    r.windowSec = window !== undefined ? parseInt(window) : 3600
  })
  if (cfg['backup.path']) backupPath.value = cfg['backup.path']
  if (cfg['backup.keepCount']) backupKeepCount.value = parseInt(cfg['backup.keepCount'])
  await loadBackups()
}

async function loadBackups() {
  backups.value = await listBackups()
}

async function save() {
  const payload = {}
  aiConfigs.value.forEach(c => {
    if (c.value && !c.value.includes('****')) payload[c.key] = c.value
  })
  rateConfigs.value.forEach(r => {
    payload[`ratelimit.${r.key}.limit`] = String(r.limit)
    payload[`ratelimit.${r.key}.windowSec`] = String(r.windowSec)
  })
  payload['backup.path'] = backupPath.value
  payload['backup.keepCount'] = String(backupKeepCount.value)
  await updateSystemConfig(payload)
  ElMessage.success('配置已保存并生效')
  load()
}

async function runBackup() {
  backingUp.value = true
  try {
    lastBackup.value = await doBackup()
    ElMessage.success('备份完成')
    await loadBackups()
  } catch (e) {
  } finally {
    backingUp.value = false
  }
}

function formatSize(bytes) {
  if (!bytes) return '0 B'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1024 / 1024).toFixed(1) + ' MB'
}
</script>

<style scoped>
.header { margin-bottom: 8px; }
.sub { color: #64748B; margin-bottom: 16px; font-size: 14px; }
.config-card { margin-bottom: 16px; }
.sec-title { margin-bottom: 14px; font-size: 16px; }
.desc { color: #94A3B8; font-size: 13px; margin-bottom: 14px; }
.config-row { display: flex; align-items: center; gap: 16px; margin-bottom: 12px; flex-wrap: wrap; }
.config-label { width: 110px; font-weight: 500; color: #374151; flex-shrink: 0; }
.config-hint { font-size: 12px; color: #94A3B8; }

.rate-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); gap: 14px; }
.rate-item { background: #FAFBFF; border-radius: 10px; padding: 12px; }
.rate-label { font-weight: 500; margin-bottom: 8px; }
.rate-inputs { display: flex; align-items: center; gap: 6px; font-size: 13px; color: #64748B; flex-wrap: nowrap; white-space: nowrap; }
.rate-unit { white-space: nowrap; flex-shrink: 0; }

.backup-actions { display: flex; align-items: center; gap: 16px; margin: 14px 0; }
.backup-result { color: #16A34A; font-size: 13px; }
.backup-list { border-top: 1px solid #F1F5F9; padding-top: 12px; }
.backup-list-title { font-weight: 600; margin-bottom: 8px; }
.backup-item { display: flex; align-items: center; gap: 16px; padding: 6px 0; font-size: 13px; }
.backup-meta { color: #94A3B8; flex: 1; }
.backup-download { color: #4F46E5; text-decoration: none; }

.config-actions { margin: 16px 0; text-align: right; }
.note-card h3 { margin-bottom: 10px; }
.note-card ul { padding-left: 20px; line-height: 2; color: #475569; }
.note-card code { background: #EEF2FF; color: #4F46E5; padding: 2px 6px; border-radius: 4px; }
</style>
