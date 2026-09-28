import { defineStore } from 'pinia'
import { aiSmart } from '../api'

export const useChatStore = defineStore('chat', {
  state: () => ({
    messages: [],
    loading: false
  }),
  actions: {
    reset() {
      this.messages = []
      this.loading = false
    },
    pushUser(text) {
      this.messages.push({ role: 'user', content: text })
    },
    pushAssistant(text, source) {
      this.messages.push({ role: 'assistant', content: text, source })
    },
    async send(question, lessonPackId) {
      this.pushUser(question)
      this.loading = true
      try {
        const result = await aiSmart({ question, lessonPackId })
        this.pushAssistant(result.answer, result.source)
      } catch (e) {
        this.pushAssistant('抱歉，AI 服务暂时不可用，请稍后再试。', '')
      } finally {
        this.loading = false
      }
    }
  }
})
