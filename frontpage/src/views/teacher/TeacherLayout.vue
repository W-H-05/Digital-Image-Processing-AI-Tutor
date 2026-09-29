<template>
  <div class="teacher-layout">
    <aside class="side-nav">
      <div class="brand">
        <AgentAvatar emoji="🧑‍🏫" :size="38" />
        <span class="brand-name">教师端</span>
      </div>
      <nav class="nav">
        <router-link to="/teacher/dashboard" class="nav-item" :class="{ active: isActive('dashboard') }">📊 实时学情</router-link>
        <router-link to="/teacher/operations" class="nav-item" :class="{ active: isActive('operations') }">📡 行为记录</router-link>
        <router-link to="/teacher/students" class="nav-item" :class="{ active: isActive('students') }">👥 学生管理</router-link>
        <router-link to="/teacher/packs" class="nav-item" :class="{ active: isActive('packs') }">📦 课次包管理</router-link>
        <router-link to="/teacher/review" class="nav-item" :class="{ active: isActive('review') }">🤝 互评答辩</router-link>
        <router-link to="/teacher/prepare" class="nav-item" :class="{ active: isActive('prepare') }">✨ 备课助手</router-link>
        <router-link to="/teacher/rag" class="nav-item" :class="{ active: isActive('rag') }">📚 知识库</router-link>
        <router-link to="/teacher/prompts" class="nav-item" :class="{ active: isActive('prompts') }">📝 Prompt 模板</router-link>
        <router-link to="/teacher/config" class="nav-item" :class="{ active: isActive('config') }">⚙️ 系统配置</router-link>
      </nav>
      <div class="nav-footer">
        <el-button type="primary" class="switch-btn" @click="switchStudentView">🎓 切换学生页面</el-button>
        <el-button class="logout-btn" @click="logout">退出登录</el-button>
      </div>
    </aside>
    <main class="main">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router'
import AgentAvatar from '../../components/AgentAvatar.vue'
import { useUserStore } from '../../store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

function isActive(name) {
  return route.path === '/teacher/' + name
}

function switchStudentView() {
  userStore.enterStudentView()
  router.push('/home')
}

function logout() {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.teacher-layout { display: flex; min-height: 100vh; }
.side-nav {
  width: 220px;
  background: linear-gradient(180deg, #1E1B4B 0%, #312E81 100%);
  color: #fff;
  display: flex; flex-direction: column;
  padding: 20px 0;
  position: sticky; top: 0; height: 100vh;
}
.brand { display: flex; align-items: center; gap: 10px; padding: 0 20px 20px; }
.brand-name { font-weight: 700; font-size: 18px; }
.nav { display: flex; flex-direction: column; gap: 4px; flex: 1; }
.nav-item {
  display: block; padding: 12px 20px; color: #C7D2FE; text-decoration: none;
  font-size: 15px; transition: all 0.2s;
}
.nav-item:hover { background: rgba(255,255,255,0.08); color: #fff; }
.nav-item.active { background: rgba(255,255,255,0.15); color: #fff; font-weight: 600; border-left: 3px solid #06B6D4; }
.nav-footer { padding: 16px 20px; display: flex; flex-direction: column; gap: 8px; }
.switch-btn { width: 100%; margin: 0; }
.logout-btn { width: 100%; margin: 0; }
.main { flex: 1; overflow-y: auto; padding: 24px; background: #F5F7FB; }
</style>
