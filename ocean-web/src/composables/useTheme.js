import { ref } from 'vue'
import * as echarts from 'echarts'
import { useUserStore } from '../store/user'
import { getSettings, updateSettings } from '../api/profile'

const STORAGE_KEY = 'ocean-theme-preference'

const mode = ref(localStorage.getItem(STORAGE_KEY) || 'system')
const resolved = ref('light')

let systemMq = null
let registered = false

function computeResolved(modeValue) {
  if (modeValue === 'system') {
    return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
  }
  return modeValue
}

function applyTheme(theme) {
  resolved.value = theme
  document.documentElement.dataset.theme = theme
}

function registerEChartsTheme() {
  if (registered) return
  registered = true

  echarts.registerTheme('ocean-dark', {
    backgroundColor: '#0d1117',
    textStyle: { color: '#8b949e' },
    legend: { textStyle: { color: '#8b949e' } },
    tooltip: {
      backgroundColor: 'rgba(22,27,34,0.96)',
      borderColor: '#30363d',
      textStyle: { color: '#e6edf3' }
    }
  })
}

export function useTheme() {
  const userStore = useUserStore()

  async function init() {
    registerEChartsTheme()

    if (userStore.token) {
      try {
        const res = await getSettings()
        if (res.data?.theme) {
          mode.value = res.data.theme
          localStorage.setItem(STORAGE_KEY, res.data.theme)
        }
      } catch { /* network or not logged in — use localStorage fallback */ }
    }

    applyTheme(computeResolved(mode.value))

    systemMq = window.matchMedia('(prefers-color-scheme: dark)')
    systemMq.addEventListener('change', (e) => {
      if (mode.value === 'system') {
        applyTheme(e.matches ? 'dark' : 'light')
      }
    })
  }

  async function setMode(newMode) {
    mode.value = newMode
    localStorage.setItem(STORAGE_KEY, newMode)
    applyTheme(computeResolved(newMode))

    if (userStore.token) {
      try {
        await updateSettings({ settings: { theme: newMode } })
      } catch { /* best-effort persistence */ }
    }
  }

  return { mode, resolved, init, setMode }
}
