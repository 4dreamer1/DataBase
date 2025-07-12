import http from './http'

// 认证相关API
const authService = {
  // 登录
  login(username, password) {
    return http.post('/auth/login', { username, password })
  },
  
  // 注册
  register(userData) {
    return http.post('/auth/signup', userData)
  },
  
  // 获取当前用户信息
  getCurrentUser() {
    return http.get('/users/profile')
  },
  
  // 更新用户信息
  updateProfile(profileData) {
    return http.put('/users/profile', profileData)
  },
  
  // 修改密码
  changePassword(oldPassword, newPassword) {
    return http.put('/users/profile', {
      oldPassword,
      password: newPassword
    })
  }
}

export default authService 