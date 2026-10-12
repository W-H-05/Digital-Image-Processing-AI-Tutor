<template>
  <div class="practice-lab" :class="'streak-' + streakTier">
    <!-- 通关页面 -->
    <div v-if="finished" class="pl-finished">
      <div class="fin-burst">
        <div class="fin-emoji">🏆</div>
        <div class="fin-title">恭喜你通过闯关！</div>
        <div class="fin-sub">你已完成全部 {{ questions.length }} 道练习题</div>
        <div class="fin-stats">
          <div class="fin-stat"><div class="fin-num">{{ totalCorrectCount }}</div><div class="fin-label">答对</div></div>
          <div class="fin-stat"><div class="fin-num">{{ questions.length }}</div><div class="fin-label">总题数</div></div>
          <div class="fin-stat"><div class="fin-num">{{ accuracy }}%</div><div class="fin-label">正确率</div></div>
        </div>
        <div class="fin-confetti">
          <span v-for="i in 20" :key="i" class="confetti" :style="confettiStyle(i)"></span>
        </div>
        <el-button type="primary" size="large" @click="restart">🔄 再练一次</el-button>
        <el-button size="large" @click="exitPractice">📖 返回目录</el-button>
      </div>
    </div>

    <!-- 做题页面 -->
    <template v-else>
    <!-- 顶部进度 -->
    <div class="pl-header">
      <div class="pl-progress-info">
        <span class="pl-title">🎯 随堂练习</span>
        <span class="pl-counter">第 {{ currentIndex + 1 }} / {{ questions.length }} 题</span>
      </div>
      <div class="pl-streak" :class="{ hot: streak >= 3 }">
        🔥 连续答对 <b>{{ streak }}</b> 题
      </div>
    </div>
    <div class="pl-progress-bar">
      <div class="pl-progress-fill" :style="{ width: progressPercent + '%' }"></div>
    </div>

    <!-- 题目卡片 -->
    <div v-if="questions.length === 0" class="pl-empty">
      <div class="pl-empty-emoji">📝</div>
      <p>该课次暂无练习题</p>
    </div>
    <div v-else-if="current" class="pl-card" :class="{ shake: shaking, correct: showCorrect, wrong: showWrong }">
      <!-- 炫酷边框特效层 -->
      <div v-if="streakTier >= 3" class="pl-glow" :class="'glow-' + streakTier"></div>

      <div class="pl-question-head">
        <span class="pl-q-no">第 {{ current.questionNo }} 题</span>
        <el-tag v-if="isMultiple" type="warning" size="small">多选题</el-tag>
        <el-tag v-else type="info" size="small">单选题</el-tag>
      </div>
      <div class="pl-stem">{{ current.stem }}</div>
      <img v-if="current.stemImage" :src="current.stemImage" class="pl-stem-img" />

      <div class="pl-options">
        <div
          v-for="opt in current.options"
          :key="opt.key"
          class="pl-option"
          :class="optionClass(opt.key)"
          @click="chooseOption(opt.key)"
        >
          <span class="pl-opt-key">{{ opt.key }}</span>
          <div class="pl-opt-content">
            <span v-if="opt.text" class="pl-opt-text">{{ opt.text }}</span>
            <img v-if="opt.image" :src="opt.image" class="pl-opt-img" />
          </div>
          <span v-if="isRevealed && showCorrect && isCorrectKey(opt.key)" class="pl-opt-mark correct-mark">✓</span>
          <span v-else-if="isRevealed && !showCorrect && isWrongPicked(opt.key)" class="pl-opt-mark wrong-mark">✗</span>
        </div>
      </div>

      <div v-if="isMultiple && !isRevealed" class="pl-tip">多选题：选择所有正确答案后点击确认</div>
      <div v-if="isMultiple && !isRevealed" class="pl-confirm-row">
        <el-button type="primary" :disabled="selectedKeys.length === 0" @click="confirmMultiple">确认答案</el-button>
      </div>
    </div>
    </template>

    <!-- 连对动画弹层 -->
    <transition name="pop">
      <div v-if="comboPopup" class="combo-popup" :class="'combo-' + comboLevel">
        <div class="combo-text">{{ comboText }}</div>
        <div class="combo-sub">{{ comboSub }}</div>
      </div>
    </transition>

    <!-- 错因强制弹窗 -->
    <el-dialog
      v-model="wrongDialogVisible"
      title="🤔 这道题怎么错了？"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      :show-close="false"
      width="480px"
    >
      <p class="wrong-dialog-tip">你已经连续答错 3 次了，请认真思考并写下你做错的原因（必填）</p>
      <el-input
        v-model="wrongReason"
        type="textarea"
        :rows="4"
        placeholder="例如：我对直方图均衡化的原理理解不深，混淆了灰度级和像素值…"
        maxlength="300"
        show-word-limit
      />
      <template #footer>
        <el-button type="primary" :disabled="!wrongReason.trim()" @click="submitWrongReason">提交并继续</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { listPracticeForStudent, answerPractice, recordPractice } from '../api'

const props = defineProps({
  lessonPackId: { type: [Number, String], required: true }
})
const emit = defineEmits(['lock', 'unlock', 'back'])

const questions = ref([])
const currentIndex = ref(0)
const current = computed(() => questions.value[currentIndex.value])
const isMultiple = computed(() => current.value?.isMultiple === 1)

// 作答状态
const selectedKeys = ref([])   // 已选中的选项
const isRevealed = ref(false)  // 是否已揭示对错
const showCorrect = ref(false)
const showWrong = ref(false)
const shaking = ref(false)
const correctKeys = ref([])    // 当前题正确答案（从 answer 接口返回）

// 连续答对
const streak = ref(0)
const streakTier = ref(0)      // 0-3 用于边框特效等级
const comboPopup = ref(false)
const comboLevel = ref(0)
const comboText = ref('')
const comboSub = ref('')

// 通关状态
const finished = ref(false)
// 每道题首次作答结果：{ questionId: true/false }，正确率按首次作答计算
const firstTryResults = ref({})

const totalCorrectCount = computed(() => {
  return Object.values(firstTryResults.value).filter(v => v === true).length
})

const accuracy = computed(() => {
  if (!questions.value.length) return 0
  return Math.round(totalCorrectCount.value * 100 / questions.value.length)
})

// 错3次强制弹窗
const wrongDialogVisible = ref(false)
const wrongReason = ref('')
let wrongCount = 0              // 当前题错误次数

async function load() {
  questions.value = await listPracticeForStudent(props.lessonPackId)
  currentIndex.value = 0
  wrongCount = 0
  firstTryResults.value = {}
  finished.value = false
  resetQuestion()
  // 进入练习即锁定目录
  emit('lock')
}

function resetQuestion() {
  selectedKeys.value = []
  isRevealed.value = false
  showCorrect.value = false
  showWrong.value = false
  correctKeys.value = []
  wrongCount = 0
}

function optionClass(key) {
  if (isRevealed.value) {
    if (showCorrect.value) {
      // 答对：正确项绿色对勾，其余淡化
      if (correctKeys.value.includes(key)) return ['correct']
      return ['dim']
    }
    // 答错：选中的错误项红色叉，其余（含正确项）淡化，不暴露正确选项
    if (selectedKeys.value.includes(key)) return ['wrong']
    return ['dim']
  }
  // 未揭示：选中的项高亮
  return selectedKeys.value.includes(key) ? ['selected'] : []
}

function isCorrectKey(key) {
  return correctKeys.value.includes(key)
}

function isWrongPicked(key) {
  return isRevealed.value && selectedKeys.value.includes(key) && !correctKeys.value.includes(key)
}

// 单选：点击即判定
async function chooseOption(key) {
  if (isRevealed.value) return  // 揭示后不可再点
  if (isMultiple.value) {
    // 多选：toggle 选择
    const idx = selectedKeys.value.indexOf(key)
    if (idx >= 0) selectedKeys.value.splice(idx, 1)
    else selectedKeys.value.push(key)
    return
  }
  // 单选直接判定
  selectedKeys.value = [key]
  await judge()
}

// 多选：确认后判定
async function confirmMultiple() {
  if (selectedKeys.value.length === 0) return
  await judge()
}

async function judge() {
  if (!current.value) return
  const res = await answerPractice(current.value.id, selectedKeys.value)
  correctKeys.value = (res.correctKeys || []).map(String)
  isRevealed.value = true
  // 首次作答结果记录（正确率按首次作答计算，首次答错的题即使重选答对也算错）
  if (!(current.value.id in firstTryResults.value)) {
    firstTryResults.value[current.value.id] = res.correct
  }
  if (res.correct) {
    // 答对
    showCorrect.value = true
    showWrong.value = false
    streak.value++
    updateStreakTier()
    // 记录正确作答
    await recordPractice(current.value.id, { correct: true, wrongCount, wrongReason: '' }).catch(() => {})
    // 连对特效
    triggerCombo(streak.value)
    // 自动进入下一题
    setTimeout(() => {
      nextQuestion()
    }, 900)
  } else {
    // 答错：连对次数重置
    showCorrect.value = false
    showWrong.value = true
    streak.value = 0
    streakTier.value = 0
    wrongCount++
    // 抖动反馈
    triggerShake()
    if (wrongCount >= 3) {
      // 错3次强制弹窗
      wrongDialogVisible.value = true
      wrongReason.value = ''
    } else {
      // 错后自动变为重选
      setTimeout(() => {
        ElMessage.warning(`回答错误，请重新选择（已错 ${wrongCount} 次）`)
        // 重置选择但保留题目，让用户重选
        isRevealed.value = false
        selectedKeys.value = []
        showWrong.value = false
        correctKeys.value = []
      }, 800)
    }
  }
}

function triggerShake() {
  shaking.value = true
  setTimeout(() => { shaking.value = false }, 600)
}

function updateStreakTier() {
  if (streak.value >= 10) streakTier.value = 4
  else if (streak.value >= 5) streakTier.value = 3
  else if (streak.value >= 3) streakTier.value = 2
  else streakTier.value = 1
}

function triggerCombo(s) {
  let text = '', sub = '', level = 0
  if (s === 3) { text = 'GOOD!'; sub = '连续答对 3 题，状态不错！'; level = 1 }
  else if (s === 5) { text = 'AMAZING!'; sub = '连续答对 5 题，太棒了！'; level = 2 }
  else if (s === 10) { text = 'UNBELIEVABLE!'; sub = '连续答对 10 题，难以置信！'; level = 3 }
  else return
  comboText.value = text
  comboSub.value = sub
  comboLevel.value = level
  comboPopup.value = true
  setTimeout(() => { comboPopup.value = false }, 1800)
}

function nextQuestion() {
  if (currentIndex.value >= questions.value.length - 1) {
    // 已完成所有题 → 显示通关页面 + 解锁目录
    finished.value = true
    emit('unlock')
    return
  }
  currentIndex.value++
  resetQuestion()
}

async function submitWrongReason() {
  if (!wrongReason.value.trim()) return
  const q = current.value
  // 记录错因（错3次）
  await recordPractice(q.id, { correct: false, wrongCount: 3, wrongReason: wrongReason.value.trim() }).catch(() => {})
  wrongDialogVisible.value = false
  wrongReason.value = ''
  ElMessage.info('已记录错因，继续努力！')
  // 重置重选
  isRevealed.value = false
  selectedKeys.value = []
  showWrong.value = false
  correctKeys.value = []
  wrongCount = 0
}

/** 通关页：再练一次（重新从第一题开始） */
function restart() {
  streak.value = 0
  streakTier.value = 0
  currentIndex.value = 0
  firstTryResults.value = {}
  finished.value = false
  resetQuestion()
  // 再练一次重新锁定
  emit('lock')
}

/** 通关页：返回目录（通知父组件解锁并清空） */
function exitPractice() {
  emit('back')
}

/** 通关页：彩带随机样式 */
function confettiStyle(i) {
  const colors = ['#F59E0B', '#10B981', '#4F46E5', '#EC4899', '#06B6D4', '#EF4444', '#8B5CF6', '#34D399']
  const left = (i * 5) % 100
  const delay = (i % 10) * 0.15
  const color = colors[i % colors.length]
  return {
    left: left + '%',
    animationDelay: delay + 's',
    backgroundColor: color
  }
}

const progressPercent = computed(() => {
  if (!questions.value.length) return 0
  return Math.round((currentIndex.value / questions.value.length) * 100)
})

load()
</script>

<style scoped>
.practice-lab {
  position: relative;
  background: #fff;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 4px 20px rgba(79,70,229,0.06);
  min-height: 300px;
}
.pl-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 10px; flex-wrap: wrap; gap: 8px; }
.pl-title { font-weight: 700; font-size: 17px; }
.pl-counter { color: #64748B; font-size: 13px; margin-left: 10px; }
.pl-streak { font-size: 13px; color: #94A3B8; background: #F1F5F9; padding: 4px 12px; border-radius: 14px; transition: all 0.3s; }
.pl-streak.hot { color: #EA580C; background: #FFEDD5; font-weight: 600; animation: streakPulse 1s infinite; }
@keyframes streakPulse { 50% { transform: scale(1.08); } }
.pl-progress-bar { height: 6px; background: #E2E8F0; border-radius: 3px; margin-bottom: 18px; overflow: hidden; }
.pl-progress-fill { height: 100%; background: linear-gradient(90deg, #4F46E5, #06B6D4); border-radius: 3px; transition: width 0.4s; }

.pl-empty { text-align: center; color: #94A3B8; padding: 60px 20px; }
.pl-empty-emoji { font-size: 48px; margin-bottom: 8px; }

/* 题目卡片 */
.pl-card {
  position: relative;
  border: 2px solid #E2E8F0;
  border-radius: 16px;
  padding: 20px;
  transition: border-color 0.3s, box-shadow 0.3s, transform 0.3s;
  background: #fff;
}
/* 答对/答错边框 */
.pl-card.correct { border-color: #10B981; box-shadow: 0 0 0 3px rgba(16,185,129,0.25), 0 8px 24px rgba(16,185,129,0.2); }
.pl-card.wrong { border-color: #EF4444; box-shadow: 0 0 0 3px rgba(239,68,68,0.25), 0 8px 24px rgba(239,68,68,0.2); }

/* 抖动动画 */
.pl-card.shake { animation: shake 0.5s; }
@keyframes shake {
  0%, 100% { transform: translateX(0); }
  15% { transform: translateX(-10px) rotate(-1deg); }
  30% { transform: translateX(10px) rotate(1deg); }
  45% { transform: translateX(-8px); }
  60% { transform: translateX(8px); }
  75% { transform: translateX(-5px); }
  90% { transform: translateX(5px); }
}

.pl-question-head { display: flex; align-items: center; gap: 8px; margin-bottom: 10px; }
.pl-q-no { font-weight: 700; color: #4F46E5; }
.pl-stem { font-size: 16px; font-weight: 500; color: #1F2937; line-height: 1.7; margin-bottom: 18px; }
.pl-stem-img { width: 100%; height: auto; border-radius: 10px; border: 1px solid #E2E8F0; margin-bottom: 16px; display: block; }

.pl-options { display: flex; flex-direction: column; gap: 10px; }
.pl-option {
  display: flex; align-items: center; gap: 12px;
  padding: 12px 16px;
  border: 2px solid #E2E8F0;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
  background: #fff;
}
.pl-option:hover { border-color: #4F46E5; background: #F8FAFF; }
.pl-option.selected { border-color: #4F46E5; background: #EEF2FF; }
.pl-option.correct { border-color: #10B981; background: #ECFDF5; box-shadow: 0 0 0 2px rgba(16,185,129,0.2); }
.pl-option.wrong { border-color: #EF4444; background: #FEF2F2; box-shadow: 0 0 0 2px rgba(239,68,68,0.2); }
.pl-option.dim { opacity: 0.45; }
.pl-opt-key {
  width: 28px; height: 28px; border-radius: 50%;
  background: #F1F5F9; color: #475569;
  display: flex; align-items: center; justify-content: center;
  font-weight: 700; font-size: 14px; flex-shrink: 0;
  align-self: flex-start;
}
.pl-option.selected .pl-opt-key { background: #4F46E5; color: #fff; }
.pl-option.correct .pl-opt-key { background: #10B981; color: #fff; }
.pl-option.wrong .pl-opt-key { background: #EF4444; color: #fff; }
.pl-opt-content { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 8px; }
.pl-opt-text { font-size: 15px; color: #374151; }
.pl-opt-img { width: 100%; height: auto; border-radius: 6px; border: 1px solid #E2E8F0; display: block; }
.pl-opt-mark {
  width: 26px; height: 26px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-weight: 900; font-size: 16px; flex-shrink: 0;
  animation: markPop 0.35s ease-out;
}
.pl-opt-mark.correct-mark { background: #10B981; color: #fff; box-shadow: 0 0 10px rgba(16,185,129,0.7); }
.pl-opt-mark.wrong-mark { background: #EF4444; color: #fff; box-shadow: 0 0 10px rgba(239,68,68,0.7); }
@keyframes markPop { 0% { transform: scale(0); } 60% { transform: scale(1.3); } 100% { transform: scale(1); } }
.pl-tip { font-size: 12px; color: #B45309; margin-top: 12px; }
.pl-confirm-row { margin-top: 14px; display: flex; justify-content: flex-end; }

/* 连对特效弹层 */
.combo-popup {
  position: fixed; top: 50%; left: 50%; transform: translate(-50%, -50%);
  z-index: 4000; text-align: center; pointer-events: none;
  padding: 30px 50px; border-radius: 24px;
  animation: comboBurst 1.8s ease-out forwards;
}
.combo-text { font-size: 56px; font-weight: 900; letter-spacing: 2px; }
.combo-sub { font-size: 18px; margin-top: 6px; font-weight: 600; }
.combo-1 { background: linear-gradient(135deg, #34D399, #10B981); color: #fff; box-shadow: 0 0 60px rgba(16,185,129,0.8); }
.combo-2 { background: linear-gradient(135deg, #F59E0B, #EF4444); color: #fff; box-shadow: 0 0 80px rgba(245,158,11,0.9); }
.combo-3 { background: linear-gradient(135deg, #8B5CF6, #EC4899); color: #fff; box-shadow: 0 0 120px rgba(236,72,153,0.95); animation: comboBurstBig 1.8s ease-out forwards; }
@keyframes comboBurst {
  0% { transform: translate(-50%, -50%) scale(0.3); opacity: 0; }
  20% { transform: translate(-50%, -50%) scale(1.2); opacity: 1; }
  40% { transform: translate(-50%, -50%) scale(1); }
  80% { transform: translate(-50%, -50%) scale(1.05); opacity: 1; }
  100% { transform: translate(-50%, -50%) scale(1.4); opacity: 0; }
}
@keyframes comboBurstBig {
  0% { transform: translate(-50%, -50%) scale(0.2) rotate(-10deg); opacity: 0; }
  25% { transform: translate(-50%, -50%) scale(1.4) rotate(5deg); opacity: 1; }
  50% { transform: translate(-50%, -50%) scale(1.1) rotate(-3deg); }
  80% { transform: translate(-50%, -50%) scale(1.15); opacity: 1; }
  100% { transform: translate(-50%, -50%) scale(1.8) rotate(0); opacity: 0; }
}
.pop-enter-active { animation: popIn 0.3s; }
@keyframes popIn { from { opacity: 0; } to { opacity: 1; } }

/* 边框特效（根据连对等级） */
.pl-glow { position: absolute; inset: -3px; border-radius: 18px; pointer-events: none; z-index: -1; }
.glow-2 {
  border: 3px solid #34D399;
  box-shadow: 0 0 20px rgba(52,211,153,0.7), inset 0 0 20px rgba(52,211,153,0.15);
  animation: glowGreen 1.5s infinite;
}
.glow-3 {
  border: 4px solid #F59E0B;
  box-shadow: 0 0 35px rgba(245,158,11,0.85), inset 0 0 25px rgba(245,158,11,0.2);
  animation: glowOrange 1.2s infinite;
}
.glow-4 {
  border: 5px solid #EC4899;
  box-shadow: 0 0 55px rgba(236,72,153,0.95), inset 0 0 35px rgba(236,72,153,0.25);
  animation: glowPink 0.9s infinite;
}
@keyframes glowGreen { 50% { box-shadow: 0 0 40px rgba(52,211,153,0.95), inset 0 0 30px rgba(52,211,153,0.3); } }
@keyframes glowOrange { 50% { box-shadow: 0 0 60px rgba(245,158,11,1), inset 0 0 40px rgba(245,158,11,0.35); } }
@keyframes glowPink { 50% { box-shadow: 0 0 90px rgba(236,72,153,1), inset 0 0 50px rgba(236,72,153,0.4); } }

.wrong-dialog-tip { color: #B45309; font-size: 14px; margin-bottom: 12px; line-height: 1.6; }

/* 通关页面 */
.pl-finished {
  position: relative;
  min-height: 480px;
  display: flex; align-items: center; justify-content: center;
  overflow: hidden;
  background: linear-gradient(135deg, #EEF2FF 0%, #E0F2FE 50%, #FDF2F8 100%);
  border-radius: 16px;
}
.fin-burst {
  position: relative;
  z-index: 2;
  text-align: center;
  padding: 40px;
  animation: finPop 0.8s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}
@keyframes finPop {
  0% { transform: scale(0.3); opacity: 0; }
  60% { transform: scale(1.1); opacity: 1; }
  100% { transform: scale(1); }
}
.fin-emoji { font-size: 80px; animation: finBounce 1.2s infinite; }
@keyframes finBounce {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-18px); }
}
.fin-title {
  font-size: 38px; font-weight: 900; margin-top: 12px;
  background: linear-gradient(135deg, #F59E0B, #EF4444, #8B5CF6);
  -webkit-background-clip: text; background-clip: text;
  -webkit-text-fill-color: transparent;
  letter-spacing: 2px;
}
.fin-sub { color: #64748B; font-size: 16px; margin-top: 8px; }
.fin-stats { display: flex; gap: 24px; justify-content: center; margin: 28px 0; }
.fin-stat {
  background: #fff; border-radius: 16px; padding: 18px 28px;
  box-shadow: 0 4px 20px rgba(79,70,229,0.12);
  min-width: 110px;
}
.fin-num { font-size: 36px; font-weight: 900; color: #4F46E5; }
.fin-label { font-size: 13px; color: #94A3B8; margin-top: 2px; }
.fin-confetti { position: absolute; inset: 0; pointer-events: none; overflow: hidden; }
.confetti {
  position: absolute; top: -20px;
  width: 10px; height: 16px;
  border-radius: 2px;
  animation: confettiFall 3s linear infinite;
}
@keyframes confettiFall {
  0% { transform: translateY(-20px) rotate(0deg); opacity: 1; }
  100% { transform: translateY(520px) rotate(720deg); opacity: 0; }
}
</style>
