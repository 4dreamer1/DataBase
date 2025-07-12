import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/modules/user'

// 创建axios实例
const http = axios.create({
  baseURL: '/api',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 简化的缓存系统
const responseCache = new Map();
const CACHE_DURATION = 30000; // 30秒

// 简化的缓存清理函数
http.clearCache = (url) => {
  url ? responseCache.delete(url) : responseCache.clear();
};

// 请求拦截器
http.interceptors.request.use(
  config => {
    // 获取token
    let token = null;
    try {
      token = useUserStore().token || localStorage.getItem('token');
    } catch (e) {
      token = localStorage.getItem('token');
    }
    
    // 设置Authorization头
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`;
    }
    
    // 简化的缓存逻辑
    if (config.method === 'get' && config.useCache !== false) {
      const cacheKey = `${config.url}${JSON.stringify(config.params || {})}`;
      const cached = responseCache.get(cacheKey);
      if (cached && Date.now() - cached.timestamp < CACHE_DURATION) {
        config.adapter = () => Promise.resolve({
          data: cached.data,
              status: 200,
              statusText: 'OK',
              headers: {},
          config,
              cached: true
            });
      }
    }
    
    return config;
  },
  error => Promise.reject(error)
)

// 响应拦截器
http.interceptors.response.use(
  response => {
    // 处理缓存的响应
    if (response.cached) return response.data;
    
    // 缓存GET请求
    if (response.config.method === 'get' && response.config.useCache !== false) {
      const cacheKey = `${response.config.url}${JSON.stringify(response.config.params || {})}`;
      responseCache.set(cacheKey, {
        data: response.data,
        timestamp: Date.now()
      });
    }
    
    // 修改类请求清除缓存
    if (['post', 'put', 'delete', 'patch'].includes(response.config.method)) {
      http.clearCache(response.config.url);
    }
    
      return response.data;
  },
  error => {
    // 处理401错误
    if (error.response?.status === 401 && !error.config.url.includes('/auth/')) {
        const userStore = useUserStore();
      userStore.logout();
    }
    
    // 显示错误消息
    const errorMessage = error.response?.data?.message || '请求失败，请稍后再试';
    ElMessage.error(errorMessage);
    
      return Promise.reject(error);
  }
)

export default http 