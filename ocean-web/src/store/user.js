import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || 'null'))

  function setToken(val) {
    token.value = val
    localStorage.setItem('token', val)
  }

  function setUserInfo(val) {
    userInfo.value = val
    localStorage.setItem('userInfo', JSON.stringify(val))
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
  }

  const isAdmin = () => userInfo.value?.role === 'ADMIN'

  return { token, userInfo, setToken, setUserInfo, logout, isAdmin }
})
