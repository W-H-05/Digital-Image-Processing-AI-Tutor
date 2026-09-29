<template>
  <div class="ai-assistant">
    <!-- 头部 -->
    <div class="ai-header">
      <AgentAvatar emoji="🤖" :size="40" class="agent-avatar" />
      <div class="ai-header-text">
        <div class="ai-name">贾维斯 · AI 助教</div>
        <div class="ai-status">
          <span class="dot"></span> 在线，随时提问
        </div>
      </div>
    </div>

    <!-- 消息区 -->
    <div class="ai-messages" ref="msgBox">
      <div v-if="chat.messages.length === 0" class="ai-empty">
        <div class="ai-empty-emoji">👋</div>
        <p>你好！我是数字图像处理课程的 AI 助教「贾维斯」</p>
        <p class="tip">可以问我概念、函数、报错，或上传文件/图片让我分析</p>
        <div class="suggestions">
          <div class="sug" v-for="s in suggestions" :key="s" @click="quickAsk(s)">{{ s }}</div>
        </div>
      </div>
      <div v-for="(m, i) in chat.messages" :key="i" class="msg" :class="m.role">
        <div class="msg-bubble" :class="m.role">
          <span v-if="m.role === 'user'">{{ m.content }}</span>
          <template v-else>
            <div v-if="m.reasoning" class="reason-box">
              <div class="reason-head">💭 思考过程</div>
              <div class="reason-body">{{ m.reasoning }}</div>
            </div>
            <div class="md-body" v-html="renderMarkdown(m.content)"></div>
          </template>
          <div v-if="m.role === 'assistant' && m.source" class="source-tag">📚 来源：{{ m.source }}</div>
        </div>
      </div>
      <div v-if="chat.loading && !lastAssistantHasContent" class="msg assistant">
        <div class="msg-bubble assistant typing">
          <span class="typing-dot"></span><span class="typing-dot"></span><span class="typing-dot"></span>
        </div>
      </div>
    </div>

    <!-- 附件预览 -->
    <div v-if="previewImage" class="image-preview">
      <img v-if="fileIsImage" :src="previewImage" alt="待分析图片" />
      <span v-else class="file-chip">📎 {{ fileName }}</span>
      <button class="img-remove" @click="removeFile">✕</button>
    </div>

    <!-- 输入区 -->
    <div class="ai-input">
      <div class="input-row">
        <el-upload
          :show-file-list="false"
          :auto-upload="false"
          :on-change="onFileChange"
        >
          <button class="img-btn" title="上传文件/图片分析">📎</button>
        </el-upload>
        <input
          v-model="input"
          class="text-input"
          :placeholder="placeholder"
          @keyup.enter="send"
          @paste="onPaste"
        />
        <button class="send-btn" @click="send" :disabled="chat.loading">发送</button>
      </div>
      <div class="sync-tip">💡 你的问题会同步给老师，用于改进教学</div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import AgentAvatar from './AgentAvatar.vue'
import { renderMarkdown } from '../utils/markdown'
import { useChatStore } from '../store/chat'
import request from '../utils/request'
import { recordOperation, aiFileAnalysis } from '../api'

const props = defineProps({
  lessonPackId: { type: [Number, String], default: null }
})

const chat = useChatStore()
const input = ref('')
const previewImage = ref('')
const imageFile = ref(null)
const fileIsImage = ref(false)
const fileName = ref('')
const msgBox = ref(null)

const placeholder = computed(() => '输入你的问题，如：cv2.Canny 怎么用？或粘贴报错信息…')

const lastAssistantHasContent = computed(() => {
  const msgs = chat.messages
  if (!msgs.length) return false
  const last = msgs[msgs.length - 1]
  return last.role === 'assistant' && (last.content || '').length > 0
})

const suggestions = ['什么是灰度直方图？', '直方图均衡化的原理', 'cv2.Canny 怎么用？', 'BGR 和 RGB 有什么区别？']

function quickAsk(q) {
  input.value = q
  send()
}

function onFileChange(file) {
  const raw = file.raw
  if (!raw) return
  imageFile.value = raw
  fileName.value = raw.name
  fileIsImage.value = raw.type.startsWith('image/')
  if (fileIsImage.value) {
    previewImage.value = URL.createObjectURL(raw)
  } else {
    previewImage.value = ''
  }
}

/** 粘贴图片支持 */
function onPaste(e) {
  const items = e.clipboardData && e.clipboardData.items
  if (!items) return
  for (const item of items) {
    if (item.type.startsWith('image/')) {
      const file = item.getAsFile()
      if (file) {
        imageFile.value = file
        fileName.value = '粘贴图片.png'
        fileIsImage.value = true
        previewImage.value = URL.createObjectURL(file)
        ElMessage.success('已粘贴图片，可直接发送')
      }
      break
    }
  }
}

function removeFile() {
  imageFile.value = null
  previewImage.value = ''
  fileName.value = ''
  fileIsImage.value = false
}

async function send() {
  const text = input.value.trim()
  if (!text && !imageFile.value) return
  if (chat.loading) return

  // 文件分析
  if (imageFile.value) {
    chat.pushUser((text || '[文件提问]') + (fileName.value ? ' 📎' + fileName.value : ''))
    chat.loading = true
    const formData = new FormData()
    formData.append('file', imageFile.value)
    formData.append('question', text)
    if (props.lessonPackId) formData.append('lessonPackId', props.lessonPackId)
    try {
      const res = await aiFileAnalysis(formData)
      chat.pushAssistant(res.answer, res.source)
    } catch (e) {
      chat.pushAssistant('文件分析失败，请稍后再试。', '')
    } finally {
      chat.loading = false
      removeFile()
    }
    recordOperation({
      lessonPackId: props.lessonPackId ? Number(props.lessonPackId) : null,
      materialId: null,
      actionType: fileIsImage.value ? 'image_question' : 'file_question',
      actionDetail: (fileIsImage.value ? '图片提问' : '文件提问') + '：' + (text || fileName.value).substring(0, 50)
    }).catch(() => {})
    input.value = ''
    scrollBottom()
    return
  }

  // 文本问答（自动识别类型）
  await chat.send(text, props.lessonPackId)
  // 追问埋点（首问为 question，追问为 followup）
  const isFollowup = chat.messages.filter(m => m.role === 'user').length > 1
  recordOperation({
    lessonPackId: props.lessonPackId ? Number(props.lessonPackId) : null,
    materialId: null,
    actionType: isFollowup ? 'followup' : 'question',
    actionDetail: 'AI 提问：' + text.substring(0, 50)
  }).catch(() => {})
  input.value = ''
  scrollBottom()
}

function scrollBottom() {
  nextTick(() => {
    if (msgBox.value) msgBox.value.scrollTop = msgBox.value.scrollHeight
  })
}

watch(() => chat.messages.length, scrollBottom)
</script>

<style scoped>
.ai-assistant {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(79, 70, 229, 0.08);
  overflow: hidden;
}
.ai-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 16px;
  background: linear-gradient(135deg, #4F46E5 0%, #06B6D4 100%);
  color: #fff;
}
.ai-name { font-weight: 600; font-size: 16px; }
.ai-status { font-size: 12px; opacity: 0.9; display: flex; align-items: center; gap: 4px; }
.dot { width: 8px; height: 8px; border-radius: 50%; background: #34D399; animation: blink 1.5s infinite; }
@keyframes blink { 50% { opacity: 0.3; } }

.ai-messages {
  flex: 1;
  overflow-y: auto;
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  background: #FAFBFF;
}
.ai-empty {
  text-align: center;
  color: #64748B;
  padding: 30px 10px;
}
.ai-empty-emoji { font-size: 40px; margin-bottom: 8px; }
.tip { font-size: 12px; margin-top: 4px; }
.suggestions { display: flex; flex-wrap: wrap; gap: 8px; justify-content: center; margin-top: 14px; }
.sug {
  background: #EEF2FF;
  color: #4F46E5;
  font-size: 12px;
  padding: 6px 12px;
  border-radius: 16px;
  cursor: pointer;
  transition: all 0.2s;
}
.sug:hover { background: #E0E7FF; }

.msg { display: flex; }
.msg.user { justify-content: flex-end; }
.msg.assistant { justify-content: flex-start; }
.msg-bubble {
  max-width: 82%;
  padding: 10px 14px;
  border-radius: 14px;
  font-size: 14px;
  line-height: 1.6;
}
.msg.assistant .msg-bubble {
  max-width: 100%;
  width: 100%;
}
.msg-bubble.user {
  background: linear-gradient(135deg, #4F46E5 0%, #06B6D4 100%);
  color: #fff;
  border-bottom-right-radius: 4px;
}
.msg-bubble.assistant {
  background: #fff;
  color: #1F2937;
  border: 1px solid #E2E8F0;
  border-bottom-left-radius: 4px;
}
.source-tag {
  margin-top: 6px;
  font-size: 11px;
  color: #94A3B8;
}
.reason-box {
  margin-bottom: 8px;
  background: #F8FAFC;
  border: 1px solid #E2E8F0;
  border-left: 3px solid #94A3B8;
  border-radius: 8px;
  padding: 8px 12px;
}
.reason-head { font-size: 12px; color: #64748B; font-weight: 600; margin-bottom: 4px; }
.reason-body { font-size: 12px; color: #94A3B8; line-height: 1.6; white-space: pre-wrap; word-break: break-word; }
.typing { display: flex; gap: 4px; align-items: center; padding: 14px 16px; }
.typing-dot {
  width: 6px; height: 6px; border-radius: 50%; background: #94A3B8;
  animation: bounce 1s infinite;
}
.typing-dot:nth-child(2) { animation-delay: 0.15s; }
.typing-dot:nth-child(3) { animation-delay: 0.3s; }
@keyframes bounce { 0%, 60%, 100% { transform: translateY(0); } 30% { transform: translateY(-6px); } }

.image-preview {
  position: relative;
  padding: 8px 12px;
  background: #FAFBFF;
}
.image-preview img {
  max-height: 120px;
  border-radius: 8px;
  border: 1px solid #E2E8F0;
}
.file-chip {
  display: inline-block;
  background: #EEF2FF;
  color: #4F46E5;
  padding: 4px 10px;
  border-radius: 8px;
  font-size: 12px;
}
.img-remove {
  position: absolute;
  top: 12px; left: 12px;
  background: #EF4444; color: #fff; border: none;
  width: 22px; height: 22px; border-radius: 50%; cursor: pointer;
}

.ai-input { padding: 12px; border-top: 1px solid #F1F5F9; }
.input-row { display: flex; gap: 8px; align-items: center; }
.img-btn {
  width: 36px; height: 36px; border: 1px solid #E2E8F0; background: #F8FAFC;
  border-radius: 10px; cursor: pointer; font-size: 18px;
  display: flex; align-items: center; justify-content: center;
  transition: all 0.2s;
}
.img-btn:hover { background: #EEF2FF; border-color: #4F46E5; }
.text-input {
  flex: 1;
  border: 1px solid #E2E8F0;
  border-radius: 10px;
  padding: 8px 12px;
  font-size: 14px;
  outline: none;
}
.text-input:focus { border-color: #4F46E5; }
.send-btn {
  background: linear-gradient(135deg, #4F46E5 0%, #06B6D4 100%);
  color: #fff; border: none; padding: 8px 16px; border-radius: 10px;
  cursor: pointer; font-size: 14px; font-weight: 600;
}
.send-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.sync-tip { font-size: 11px; color: #94A3B8; margin-top: 8px; text-align: center; }
</style>
