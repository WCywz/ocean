import request from '../utils/request'

export function getProfile() {
  return request({ url: '/profile', method: 'get' })
}

export function updateProfile(data) {
  return request({ url: '/profile', method: 'put', data })
}

export function changePassword(data) {
  return request({ url: '/profile/password', method: 'put', data })
}

export function uploadAvatar(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/profile/avatar',
    method: 'post',
    data: formData
  })
}

export function getSettings() {
  return request({ url: '/profile/settings', method: 'get' })
}

export function updateSettings(data) {
  return request({ url: '/profile/settings', method: 'put', data })
}

export function getCredentials() {
  return request({ url: '/profile/credentials', method: 'get' })
}

export function saveCredential(data) {
  return request({ url: '/profile/credentials', method: 'post', data })
}

export function deleteCredential(id) {
  return request({ url: `/profile/credentials/${id}`, method: 'delete' })
}
