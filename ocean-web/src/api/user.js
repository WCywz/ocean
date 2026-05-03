import request from '../utils/request'

/** 用户登录 */
export function login(data) {
  return request({ url: '/user/login', method: 'post', data })
}

/** 获取当前用户信息 */
export function getCurrentUser() {
  return request({ url: '/user/current', method: 'get' })
}

/** 分页查询用户 */
export function getUserPage(params) {
  return request({ url: '/user/page', method: 'get', params })
}

/** 根据ID查询用户 */
export function getUserById(id) {
  return request({ url: `/user/${id}`, method: 'get' })
}

/** 用户注册 */
export function register(data) {
  return request({ url: '/user/register', method: 'post', data })
}

/** 新增用户 */
export function addUser(data) {
  return request({ url: '/user', method: 'post', data })
}

/** 修改用户 */
export function updateUser(id, data) {
  return request({ url: `/user/${id}`, method: 'put', data })
}

/** 删除用户 */
export function deleteUser(id) {
  return request({ url: `/user/${id}`, method: 'delete' })
}
