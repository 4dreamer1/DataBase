import http from './http'
import { ElMessage } from 'element-plus'

// 用户相关API
const userService = {
  // 用户登录
  login(data) {
    return http.post('/auth/signin', data)
  },
  
  // 用户注册
  register(data) {
    return http.post('/auth/signup', data)
  },
  
  // 退出登录
  logout() {
    // 这个接口后端未实现，只在前端处理
    return Promise.resolve()
  },
  
  // 获取当前用户信息
  getCurrentUser() {
    return http.get('/users/profile')
  },
  
  // 更新个人资料
  updateProfile(data) {
    return http.put('/users/profile', data)
  },
  
  // 修改密码
  changePassword(data) {
    return http.put('/users/password', data)
  },
  
  // 上传头像
  uploadAvatar(formData) {
    return http.post('/users/avatar', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
  },
  
  // 获取用户列表
  getUsers(query) {

    // 强制不使用缓存，每次都重新请求最新数据
    return http.get('/users', { 
      params: query, 
      headers: {
        'Cache-Control': 'no-cache',
        'Pragma': 'no-cache',
        'Expires': '0',
        'X-No-Cache': Date.now() // 添加时间戳参数确保每次请求都不同
      }
    })
      .then(response => {

        // 标准化响应格式
        let result;
        
        if (!response) {
          console.warn('用户列表响应为空');
          return { data: { list: [], total: 0 } };
        }
        
        try {
          // 处理各种可能的响应格式
          if (response && response.data && response.data.list && Array.isArray(response.data.list)) {
            // 标准格式: { data: { list: [...], total: n } }
            result = response;
          } else if (Array.isArray(response)) {
            // 数组格式
            result = { data: { list: response, total: response.length } };
          } else if (response && response.data && Array.isArray(response.data)) {
            // { data: [...] } 格式
            result = { data: { list: response.data, total: response.data.length } };
          } else if (response && response.content && Array.isArray(response.content)) {
            // Spring Data分页格式
            result = {
              data: {
                list: response.content,
                total: response.totalElements || response.content.length
              }
            };
          } else if (typeof response === 'object' && response !== null) {
            // 尝试直接使用响应对象
            const list = [];
            const keys = Object.keys(response).filter(key => !isNaN(parseInt(key)));
            
            if (keys.length > 0) {
              // 对象的键是数字，可能是索引
              for (const key of keys) {
                if (response[key] && typeof response[key] === 'object') {
                  list.push(response[key]);
                }
              }
              result = { data: { list, total: list.length } };
            } else {
              // 将响应包装为标准格式
              result = { data: { list: [response], total: 1 } };
            }
          } else {
            // 默认返回空列表
            console.error('未知的用户列表响应格式:', response);
            result = { data: { list: [], total: 0 } };
          }
          
          // 确保结果中的list是数组
          if (!result.data.list || !Array.isArray(result.data.list)) {
            result.data.list = [];
            result.data.total = 0;
          }

          return result;
        } catch (error) {
          console.error('处理用户列表响应时出错:', error);
          return { data: { list: [], total: 0 } };
        }
      })
      .catch(error => {
        console.error('获取用户列表失败:', error);
        ElMessage.error('获取用户列表失败，请稍后再试');
        return { data: { list: [], total: 0 } };
      });
  },
  
  // 强制刷新用户列表
  forceRefreshUsers(query) {
    // 清除缓存
    http.clearCache('/users');
    // 添加时间戳参数确保请求不被缓存
    return this.getUsers({
      ...query,
      _t: Date.now()
    });
  },
  
  // 获取用户详情
  getUserById(id) {
    return http.get(`/users/${id}`);
  },
  
  // 创建用户
  createUser(data) {
    // 处理角色数据格式
    const userData = { ...data };
    
    // 确保roles字段正确
    if (userData.role && !userData.roles) {
      userData.roles = [];
      // 将ROLE_前缀去掉
      const roleValue = userData.role.replace('ROLE_', '').toLowerCase();
      userData.roles.push(roleValue);
    }

    return http.post('/users', userData);
  },
  
  // 更新用户
  updateUser(id, data) {
    // 处理角色数据格式
    const userData = { ...data };
    
    // 确保roles字段正确
    if (userData.role && !userData.roles) {
      userData.roles = [];
      // 将ROLE_前缀去掉
      const roleValue = userData.role.replace('ROLE_', '').toLowerCase();
      userData.roles.push(roleValue);
    }
    
    // 如果密码为空，则不提交密码字段
    if (!userData.password) {
      delete userData.password;
    }

    return http.put(`/users/${id}`, userData);
  },
  
  // 删除用户
  deleteUser(id) {
    return http.delete(`/users/${id}`);
  },
  
  // 切换用户状态
  toggleUserStatus(id, status) {
    return http.put(`/users/${id}/status`, { status })
      .then(response => {
        const statusText = status === 0 ? '启用' : '禁用';
        ElMessage.success(`用户已${statusText}`);
        return response;
      })
      .catch(error => {
        console.error('切换用户状态失败:', error);
        ElMessage.error(`切换用户状态失败: ${error.response?.data?.message || '请稍后再试'}`);
        throw error;
      });
  },
  
  // 重置用户密码
  resetPassword(id) {
    return http.put(`/users/${id}/password/reset`)
      .then(response => {
        ElMessage.success('密码已重置为默认密码');
        return response;
      })
      .catch(error => {
        console.error('重置密码失败:', error);
        ElMessage.error(`重置密码失败: ${error.response?.data?.message || '请稍后再试'}`);
        throw error;
      });
  }
}

export default userService 