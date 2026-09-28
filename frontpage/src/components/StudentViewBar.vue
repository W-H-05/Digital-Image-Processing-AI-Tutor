<template>
  <div v-if="userStore.isStudentView" class="student-view-bar">
    <span>🎓 当前正在以学生视角预览（教师切换）</span>
    <el-button size="small" type="primary" @click="exit">退出学生视角，返回教师端</el-button>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useUserStore } from '../store/user'

const router = useRouter()
const userStore = useUserStore()

function exit() {
  userStore.exitStudentView()
  router.push('/teacher/dashboard')
}
</script>

<style scoped>
.student-view-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 24px;
  background: #FEF3C7;
  color: #92400E;
  font-size: 14px;
  flex-wrap: wrap;
  gap: 8px;
}
</style>
