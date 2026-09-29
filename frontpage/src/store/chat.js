import { defineStore } from 'pinia'

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
      this.messages.push({ role: 'assistant', content: '', source: '', reasoning: '' })
      const target = this.messages[this.messages.length - 1]
      try {
        const token = localStorage.getItem('token')
        const headers = {
          'Content-Type': 'application/json',
          'Authorization': token || ''
        }
        const viewMode = localStorage.getItem('viewMode')
        if (viewMode === 'student') headers['X-View-As'] = 'student'
        const resp = await fetch('/api/ai/smart/stream', {
          method: 'POST',
          headers,
          body: JSON.stringify({ question, lessonPackId })
        })
        if (!resp.ok) throw new Error('HTTP ' + resp.status)

        const reader = resp.body.getReader()
        const decoder = new TextDecoder()
        let buffer = ''
        let currentEvent = 'chunk'
        while (true) {
          const { done, value } = await reader.read()
          if (done) break
          buffer += decoder.decode(value, { stream: true })
          const lines = buffer.split('\n')
          buffer = lines.pop() || ''
          for (const raw of lines) {
            const line = raw.replace(/\r$/, '')
            if (line.startsWith('event:')) {
              currentEvent = line.substring(6).trim()
            } else if (line.startsWith('data:')) {
              const data = line.substring(5).trim()
              if (currentEvent === 'chunk' && data) {
                target.content += data
              } else if (currentEvent === 'reason' && data) {
                target.reasoning = (target.reasoning || '') + data
              } else if (currentEvent === 'end') {
                try {
                  const meta = JSON.parse(data)
                  if (meta.source) target.source = meta.source
                } catch (e) {}
              } else if (currentEvent === 'error' && data) {
                if (!target.content) target.content = '抱歉，AI 服务暂时不可用，请稍后再试。'
              }
              currentEvent = 'chunk'
            }
          }
        }
      } catch (e) {
        if (!target.content) {
          target.content = '抱歉，AI 服务暂时不可用，请稍后再试。'
        }
      } finally {
        this.loading = false
      }
    }
  }
})
