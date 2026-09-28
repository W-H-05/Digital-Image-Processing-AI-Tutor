import { defineStore } from 'pinia'
import { login as apiLogin, getMe } from '../api'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    role: localStorage.getItem('role') || '',
    realName: localStorage.getItem('realName') || '',
    username: localStorage.getItem('username') || '',
    className: localStorage.getItem('className') || '',
    userId: null,
    viewMode: localStorage.getItem('viewMode') || '' // 'student' 表示教师正以学生视角查看
  }),
  getters: {
    isTeacher: (state) => state.role === 'TEACHER',
    isStudent: (state) => state.role === 'STUDENT',
    isStudentView: (state) => state.viewMode === 'student'
  },
  actions: {
    async login(username, password) {
      const data = await apiLogin({ username, password })
      this.token = data.token
      this.role = data.role
      this.realName = data.realName
      this.username = data.username
      this.className = data.className
      this.userId = data.userId
      localStorage.setItem('token', data.token)
      localStorage.setItem('role', data.role)
      localStorage.setItem('realName', data.realName || '')
      localStorage.setItem('username', data.username)
      localStorage.setItem('className', data.className || '')
      return data
    },
    async fetchMe() {
      try {
        const data = await getMe()
        this.role = data.role
        this.realName = data.realName
        this.username = data.username
        this.className = data.className
        this.userId = data.userId
        localStorage.setItem('role', data.role)
        localStorage.setItem('realName', data.realName || '')
        localStorage.setItem('username', data.username)
        localStorage.setItem('className', data.className || '')
      } catch (e) {}
    },
    enterStudentView() {
      this.viewMode = 'student'
      localStorage.setItem('viewMode', 'student')
    },
    exitStudentView() {
      this.viewMode = ''
      localStorage.removeItem('viewMode')
    },
    logout() {
      this.token = ''
      this.role = ''
      this.realName = ''
      this.username = ''
      this.className = ''
      this.userId = null
      this.viewMode = ''
      localStorage.removeItem('token')
      localStorage.removeItem('role')
      localStorage.removeItem('realName')
      localStorage.removeItem('username')
      localStorage.removeItem('className')
      localStorage.removeItem('viewMode')
    }
  }
})
