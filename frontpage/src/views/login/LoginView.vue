<template>
  <div class="login-page">
    <div class="login-bg"></div>
    <div class="login-card card fade-in-up">
      <div class="login-left">
        <div class="brand">
          <span class="brand-emoji">🤖</span>
          <div>
            <h1 class="brand-title">数字图像处理</h1>
            <div class="brand-sub gradient-text">AI 助教 · 贾维斯</div>
          </div>
        </div>
        <p class="brand-desc">陪你调参、看图、排错、理解每一个知识点</p>
        <div class="feature-list">
          <div class="feature">🎨 交互课件 · 拖滑块看图像变化</div>
          <div class="feature">💬 AI 问答 · 概念/函数/报错随问随答</div>
          <div class="feature">🖼️ 图片分析 · 上传图像让 AI 讲解</div>
          <div class="feature">📊 学情看板 · 实时掌握学习动态</div>
        </div>
      </div>
      <div class="login-right">
        <h2 class="login-title">欢迎回来 👋</h2>
        <p class="login-sub">使用学号 / 教师账号登录</p>
        <el-form @submit.prevent="handleLogin">
          <el-form-item>
            <el-input v-model="username" size="large" placeholder="学号 / 教师账号" :prefix-icon="User" />
          </el-form-item>
          <el-form-item>
            <el-input v-model="password" size="large" type="password" placeholder="密码（初始为学号）" :prefix-icon="Lock" show-password @keyup.enter="handleLogin" />
          </el-form-item>
          <el-button type="primary" size="large" class="login-btn" :loading="loading" @click="handleLogin">
            登 录
          </el-button>
        </el-form>
        <div class="login-tip">💡 初始密码为学号，登录后可修改</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '../../store/user'

const router = useRouter()
const userStore = useUserStore()
const username = ref('')
const password = ref('')
const loading = ref(false)

async function handleLogin() {
  if (!username.value || !password.value) {
    ElMessage.warning('请输入账号和密码')
    return
  }
  loading.value = true
  try {
    const data = await userStore.login(username.value, password.value)
    ElMessage.success(`欢迎，${data.realName || data.username}！`)
    if (data.role === 'TEACHER') {
      router.push('/teacher/dashboard')
    } else {
      router.push('/home')
    }
  } catch (e) {
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}
.login-bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, #EEF2FF 0%, #E0F2FE 50%, #FEF3C7 100%);
}
.login-card {
  display: flex;
  max-width: 880px;
  width: 92%;
  position: relative;
  overflow: hidden;
  padding: 0;
}
.login-left {
  flex: 1.2;
  background: linear-gradient(135deg, #4F46E5 0%, #06B6D4 100%);
  color: #fff;
  padding: 48px 36px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}
.brand { display: flex; align-items: center; gap: 14px; }
.brand-emoji { font-size: 48px; }
.brand-title { font-size: 24px; font-weight: 700; }
.brand-sub { font-size: 16px; font-weight: 600; background: #FEF3C7; -webkit-background-clip: text; background-clip: text; }
.brand-desc { margin: 18px 0; opacity: 0.95; }
.feature-list { display: flex; flex-direction: column; gap: 10px; margin-top: 8px; }
.feature {
  background: rgba(255,255,255,0.15);
  padding: 10px 14px;
  border-radius: 10px;
  font-size: 14px;
  backdrop-filter: blur(4px);
}
.login-right {
  flex: 1;
  padding: 48px 40px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}
.login-title { font-size: 26px; font-weight: 700; color: #1F2937; }
.login-sub { color: #64748B; margin: 8px 0 24px; }
.login-btn { width: 100%; height: 44px; font-size: 16px; background: linear-gradient(135deg, #4F46E5 0%, #06B6D4 100%); border: none; }
.login-tip { margin-top: 16px; font-size: 12px; color: #94A3B8; text-align: center; }

@media (max-width: 700px) {
  .login-left { display: none; }
}
</style>
