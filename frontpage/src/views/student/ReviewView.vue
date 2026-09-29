<template>
  <div class="review-page">
    <header class="topbar">
      <el-button text @click="$router.back()"><span class="arrow">&lt;</span> 返回</el-button>
      <span class="title">🤝 挑战互评</span>
      <span class="user">{{ userStore.realName || userStore.username }}</span>
    </header>

    <main class="content">
      <!-- 选择要互评的挑战 -->
      <div class="card section">
        <h3>📋 待评作品</h3>
        <el-form inline>
          <el-form-item label="课次">
            <el-select v-model="selectedPack" placeholder="选择课次" style="width: 220px" @change="loadChallenges">
              <el-option v-for="p in packs" :key="p.id" :label="`${p.lessonNo} ${p.title}`" :value="p.id" />
            </el-select>
          </el-form-item>
        </el-form>
        <div v-if="challenges.length === 0" class="empty">该课次暂无挑战提交</div>
        <div v-for="c in challenges" :key="c.id" class="challenge-item">
          <div class="challenge-info">
            <div class="challenge-user">👤 {{ c.userName || ('同学 #' + c.userId) }} 的挑战</div>
            <div class="challenge-conclusion">{{ c.conclusion || '（无文字结论）' }}</div>
            <div class="challenge-params" v-if="c.paramsJson">参数：{{ c.paramsJson }}</div>
          </div>
          <el-button size="small" type="primary" @click="openReview(c)">去互评</el-button>
        </div>
      </div>

      <!-- 我收到的评价 -->
      <div class="card section">
        <h3>💌 我收到的评价</h3>
        <div v-if="myReviews.length === 0" class="empty">暂无评价</div>
        <div v-for="r in myReviews" :key="r.id" class="review-item">
          <div class="review-scores" v-if="r.scoresJson">{{ r.scoresJson }}</div>
          <div class="review-comments">{{ r.comments || '（无评语）' }}</div>
        </div>
      </div>
    </main>

    <!-- 互评弹窗 -->
    <el-dialog v-model="dialogVisible" title="互评打分" width="500px">
      <el-form label-width="100px">
        <el-form-item label="参数合理性">
          <el-rate v-model="scoreForm.paramScore" :max="5" />
        </el-form-item>
        <el-form-item label="结论正确性">
          <el-rate v-model="scoreForm.conclusionScore" :max="5" />
        </el-form-item>
        <el-form-item label="展示清晰度">
          <el-rate v-model="scoreForm.presentScore" :max="5" />
        </el-form-item>
        <el-form-item label="评语">
          <el-input v-model="scoreForm.comments" type="textarea" :rows="3" placeholder="给出建设性评语" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">提交评价</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../../store/user'
import { listLessonPacks, listChallenges, submitPeerReview, listMyPeerReviews } from '../../api'

const userStore = useUserStore()
const packs = ref([])
const selectedPack = ref(null)
const challenges = ref([])
const myReviews = ref([])
const dialogVisible = ref(false)
const currentChallenge = ref(null)
const scoreForm = ref({ paramScore: 3, conclusionScore: 3, presentScore: 3, comments: '' })

onMounted(async () => {
  packs.value = await listLessonPacks()
  if (packs.value.length) {
    selectedPack.value = packs.value[0].id
    await loadChallenges()
  }
  await loadMyReviews()
})

async function loadChallenges() {
  if (!selectedPack.value) return
  challenges.value = await listChallenges(selectedPack.value)
}

async function loadMyReviews() {
  myReviews.value = await listMyPeerReviews()
}

function openReview(c) {
  currentChallenge.value = c
  scoreForm.value = { paramScore: 3, conclusionScore: 3, presentScore: 3, comments: '' }
  dialogVisible.value = true
}

async function submit() {
  const scores = {
    paramScore: scoreForm.value.paramScore,
    conclusionScore: scoreForm.value.conclusionScore,
    presentScore: scoreForm.value.presentScore
  }
  await submitPeerReview({
    revieweeId: currentChallenge.value.userId,
    taskId: currentChallenge.value.id,
    scoresJson: JSON.stringify(scores),
    comments: scoreForm.value.comments
  })
  ElMessage.success('评价已提交！')
  dialogVisible.value = false
  await loadMyReviews()
}
</script>

<style scoped>
.review-page { min-height: 100vh; }
.topbar { display: flex; align-items: center; gap: 16px; padding: 12px 24px; background: #fff; box-shadow: 0 2px 10px rgba(0,0,0,0.04); }
.topbar .arrow { font-weight: 700; font-size: 15px; }
.title { font-weight: 700; }
.user { margin-left: auto; color: #64748B; }
.content { max-width: 900px; margin: 0 auto; padding: 24px; }
.section { margin-bottom: 20px; }
.section h3 { margin-bottom: 14px; }
.empty { color: #94A3B8; text-align: center; padding: 30px; }
.challenge-item { display: flex; align-items: center; justify-content: space-between; padding: 12px; border-bottom: 1px solid #F1F5F9; gap: 12px; }
.challenge-user { font-weight: 600; }
.challenge-conclusion { color: #475569; font-size: 14px; margin-top: 4px; }
.challenge-params { color: #94A3B8; font-size: 12px; margin-top: 4px; }
.review-item { padding: 12px; border-bottom: 1px solid #F1F5F9; }
.review-scores { color: #4F46E5; font-size: 13px; margin-bottom: 4px; }
.review-comments { color: #475569; font-size: 14px; }
</style>
