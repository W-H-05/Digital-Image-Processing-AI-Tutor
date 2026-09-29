<template>
  <div class="home">
    <StudentViewBar />
    <header class="topbar">
      <div class="brand">
        <AgentAvatar emoji="🤖" :size="40" class="agent-avatar" />
        <span class="brand-name">数字图像处理 · AI 助教</span>
      </div>
      <div class="user-area">
        <span class="user-info">{{ userStore.realName || userStore.username }} · {{ userStore.className || '学生' }}</span>
        <el-dropdown @command="handleCommand">
          <el-button type="primary" link>
            <el-icon><User /></el-icon> 我的
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="self">📊 学情自查</el-dropdown-item>
              <el-dropdown-item command="review">🤝 挑战互评</el-dropdown-item>
              <el-dropdown-item command="pwd">🔑 修改密码</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <main class="content">
      <div class="welcome card">
        <h1>欢迎回来，{{ userStore.realName || '同学' }} 👋</h1>
        <p>今天想学点什么？选择一次课开始探索吧！</p>
      </div>

      <div class="map">
        <div v-for="pack in packs" :key="pack.id" class="pack-card card fade-in-up" @click="goPack(pack)">
          <div class="pack-emoji">{{ emojiFor(pack) }}</div>
          <div class="pack-no">{{ pack.lessonNo }}</div>
          <div class="pack-title">{{ pack.title }}</div>
          <div class="pack-chapter">{{ pack.chapter }}</div>
          <div class="pack-go">进入学习 <span class="arrow">&gt;</span></div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { User } from '@element-plus/icons-vue'
import AgentAvatar from '../../components/AgentAvatar.vue'
import StudentViewBar from '../../components/StudentViewBar.vue'
import { useUserStore } from '../../store/user'
import { listLessonPacks, changePassword } from '../../api'

const router = useRouter()
const userStore = useUserStore()
const packs = ref([])

onMounted(async () => {
  try {
    packs.value = await listLessonPacks()
  } catch (e) {}
})

function emojiFor(pack) {
  const title = pack.title || ''
  if (pack.lessonNo && pack.lessonNo.includes('实验')) return '🧪'
  if (title.includes('直方图') || title.includes('点运算')) return '📊'
  if (title.includes('滤波')) return '🌊'
  if (title.includes('频域') || title.includes('傅里叶')) return '🌀'
  if (title.includes('形态学')) return '🧱'
  if (title.includes('分割') || title.includes('边缘')) return '✂️'
  if (title.includes('特征')) return '🔍'
  if (title.includes('视频') || title.includes('检测')) return '🎥'
  return '📚'
}

function goPack(pack) {
  router.push(`/pack/${pack.id}`)
}

function handleCommand(cmd) {
  if (cmd === 'self') router.push('/self')
  else if (cmd === 'review') router.push('/review')
  else if (cmd === 'pwd') changePwd()
  else if (cmd === 'logout') {
    userStore.logout()
    router.push('/login')
  }
}

async function changePwd() {
  try {
    const { value } = await ElMessageBox.prompt('请输入原密码', '修改密码', { inputType: 'password' })
    const { value: np } = await ElMessageBox.prompt('请输入新密码（4-32位）', '修改密码', { inputType: 'password' })
    await changePassword({ oldPassword: value, newPassword: np })
    ElMessageBox.alert('密码修改成功！', '提示')
  } catch (e) {}
}
</script>

<style scoped>
.home { min-height: 100vh; }
.topbar {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 24px;
  background: #fff;
  box-shadow: 0 2px 10px rgba(0,0,0,0.04);
}
.brand { display: flex; align-items: center; gap: 10px; }
.brand-name { font-weight: 700; font-size: 17px; background: linear-gradient(135deg, #4F46E5, #06B6D4); -webkit-background-clip: text; background-clip: text; -webkit-text-fill-color: transparent; }
.user-area { display: flex; align-items: center; gap: 12px; }
.user-info { color: #64748B; font-size: 14px; }

.content { max-width: 1100px; margin: 0 auto; padding: 24px; }
.welcome { margin-bottom: 24px; background: linear-gradient(135deg, #EEF2FF 0%, #E0F2FE 100%); border: none; }
.welcome h1 { font-size: 22px; color: #1F2937; }
.welcome p { color: #64748B; margin-top: 6px; }

.map { display: grid; grid-template-columns: repeat(auto-fill, minmax(230px, 1fr)); gap: 18px; }
.pack-card { cursor: pointer; transition: all 0.25s; position: relative; overflow: hidden; }
.pack-card:hover { transform: translateY(-4px); box-shadow: 0 12px 30px rgba(79,70,229,0.18); }
.pack-emoji { font-size: 34px; }
.pack-no { margin-top: 8px; font-size: 12px; color: #4F46E5; font-weight: 600; }
.pack-title { font-size: 16px; font-weight: 600; color: #1F2937; margin-top: 2px; }
.pack-chapter { font-size: 13px; color: #94A3B8; margin-top: 4px; }
.pack-go { margin-top: 14px; font-size: 13px; color: #06B6D4; font-weight: 600; display: flex; align-items: center; gap: 4px; }
.pack-go .arrow { font-weight: 700; font-size: 16px; transition: transform 0.2s; }
.pack-card:hover .pack-go .arrow { transform: translateX(4px); }
</style>
