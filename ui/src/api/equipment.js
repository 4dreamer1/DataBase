import http from './http'
import { ElMessage } from 'element-plus'
import axios from 'axios'; // Added axios import

/**
 * 清除装备相关缓存
 */
const clearCache = () => {
  if (http.clearCache) {
    // 清除所有相关缓存

    http.clearCache('/equipment');
    http.clearCache('/categories');
  }
};

/**
 * 安全解析响应数据
 * @param {Object} response - API响应
 * @param {String} defaultErrorMsg - 默认错误信息
 * @returns {Object} - 处理后的数据
 */
const safeParseResponse = (response, defaultErrorMsg = '获取数据失败') => {
  try {
    // 如果response是对象且有data属性，返回data
    if (response && typeof response === 'object') {
      if (response.data !== undefined) {
        return response.data;
      }
      return response;
    }
    
    // 如果response不是对象，返回一个空对象
    console.error(defaultErrorMsg, response);
    return {};
  } catch (error) {
    console.error('解析响应数据失败', error);
    return {};
  }
};

/**
 * 装备管理API服务
 */
const equipmentService = {
  /**
   * 清除缓存的方法，供组件直接调用
   */
  clearCache,
  
  /**
   * 获取装备列表（支持分页、关键词、分类筛选）
   * @param {Object} params - 搜索参数
   * @returns {Promise} - 返回装备列表
   */
  getAll(params = {}) {
    const defaultParams = {
      page: 0,
      size: 10,
      sort: ['id', 'desc']
    };
    
    const queryParams = { ...defaultParams, ...params };
    return http.get('/equipment', { 
      params: queryParams,
      headers: {
        'Cache-Control': 'no-cache'
      }
    }).catch(error => {
      console.error('获取装备列表失败', error);
      // 返回一个空对象以防止前端崩溃
      return {
        content: [],
        totalElements: 0,
        totalPages: 0,
        last: true,
        number: 0,
        size: 10
      };
    });
  },
  
  /**
   * 获取装备详情
   * @param {Number} id - 装备ID
   * @returns {Promise} - 返回装备详情
   */
  getById(id) {
    if (!id) {
      return Promise.reject(new Error('装备ID不能为空'));
    }
    
    // 添加时间戳参数确保不使用缓存
    const timestamp = new Date().getTime();
    
    return http.get(`/equipment/${id}?_t=${timestamp}`, {
      headers: {
        'Cache-Control': 'no-cache, no-store, must-revalidate',
        'Pragma': 'no-cache',
        'Expires': '0'
      },
      useCache: false
    }).catch(error => {
      console.error(`获取装备详情失败, ID: ${id}`, error);
      return {};
    });
  },
  
  /**
   * 获取分类下的装备
   * @param {Number} categoryId - 分类ID
   * @returns {Promise} - 返回分类下的装备列表
   */
  getByCategory(categoryId) {
    if (!categoryId) {
      return Promise.reject(new Error('分类ID不能为空'));
    }
    return http.get(`/equipment/category/${categoryId}`).catch(error => {
      console.error(`获取分类下装备失败, 分类ID: ${categoryId}`, error);
      return [];
    });
  },
  
  /**
   * 搜索装备
   * @param {Object} params - 搜索参数
   * @returns {Promise} - 返回搜索结果
   */
  search(params = {}) {
    const defaultParams = {
      page: 0,
      size: 10,
      sort: ['id', 'desc']
    };
    
    const queryParams = { ...defaultParams, ...params };
    // 使用getAll方法，保持一致性
    return this.getAll(queryParams);
  },
  
  /**
   * 获取可用装备
   * @returns {Promise} - 返回可用装备列表
   */
  getAvailable() {
    // 添加时间戳参数和随机值避免缓存问题
    const timestamp = new Date().getTime();
    const randomValue = Math.random().toString(36).substring(2, 15);
    
    // 明确禁用缓存
    const headers = {
      'Cache-Control': 'no-cache, no-store, must-revalidate',
      'Pragma': 'no-cache',
      'Expires': '0'
    };
    
    // 传递参数确保每次请求都是新的
    return http.get(`/equipment/available?_t=${timestamp}&_r=${randomValue}`, { 
      useCache: false,
      headers
    }).catch(error => {
      console.error('获取可用装备失败', error);
      return [];
    });
  },
  
  /**
   * 获取库存不足的装备
   * @returns {Promise} - 返回库存不足的装备列表
   */
  getLowStockItems() {
    return http.get('/equipment/low-stock').then(response => {
      return {
        data: response || [] 
      };
    }).catch(error => {
      console.error('获取库存不足装备失败', error);
      return { data: [] };
    });
  },
  
  /**
   * 检查序列号是否存在
   * @param {String} serialNumber - 序列号
   * @param {Number} excludeId - 排除的装备ID
   * @returns {Promise<boolean>} - 返回序列号是否存在
   */
  checkSerialNumber(serialNumber, excludeId = null) {
    const url = excludeId 
      ? `/equipment/check-serial/${serialNumber}?excludeId=${excludeId}`
      : `/equipment/check-serial/${serialNumber}`;
    return http.get(url).catch(() => false);
  },
  
  /**
   * 更新装备状态
   * @param {Number} id - 装备ID
   * @param {String} status - 新状态
   * @returns {Promise} - 返回更新后的装备
   */
  updateStatus(id, status) {
    if (!id) {
      return Promise.reject(new Error('装备ID不能为空'));
    }
    
    clearCache();
    return http.put(`/equipment/${id}/status`, { status }).catch(error => {
      console.error(`更新装备状态失败, ID: ${id}, 状态: ${status}`, error);
      ElMessage.error('更新装备状态失败，请稍后重试');
      return null;
    });
  },
  
  /**
   * 创建装备
   * @param {Object} equipment - 装备数据
   * @returns {Promise} - 返回创建的装备
   */
  create(equipment) {
    if (!equipment) {
      return Promise.reject(new Error('装备数据不能为空'));
    }
    
    clearCache();
    return http.post('/equipment', equipment, {
      headers: { 'Content-Type': 'application/json' }
    }).then(response => {
      ElMessage.success('装备创建成功');
      return response;
    }).catch(error => {
      console.error('创建装备失败', error);
      ElMessage.error('创建装备失败，请检查输入并稍后重试');
      return null;
    });
  },
  
  /**
   * 更新装备
   * @param {Number} id - 装备ID
   * @param {Object} equipment - 装备数据
   * @returns {Promise} - 返回更新后的装备
   */
  update(id, equipment) {
    if (!id) {
      return Promise.reject(new Error('装备ID不能为空'));
    }
    
    clearCache();
    return http.put(`/equipment/${id}`, equipment, {
      headers: { 'Content-Type': 'application/json' }
    }).then(response => {
      ElMessage.success('装备更新成功');
      return response;
    }).catch(error => {
      console.error(`更新装备失败, ID: ${id}`, error);
      ElMessage.error('更新装备失败，请检查输入并稍后重试');
      return null;
    });
  },
  
  /**
   * 删除装备
   * @param {Number} id - 装备ID
   * @returns {Promise} - 返回删除结果
   */
  delete(id) {
    if (!id) {
      return Promise.reject(new Error('装备ID不能为空'));
    }
    
    clearCache();
    return http.delete(`/equipment/${id}`).then(response => {
      ElMessage.success('装备删除成功');
      return response;
    }).catch(error => {
      console.error(`删除装备失败, ID: ${id}`, error);
      ElMessage.error('删除装备失败，请稍后重试');
      return null;
    });
  },
  
  /**
   * 批量删除装备
   * @param {Array} ids - 装备ID数组
   * @returns {Promise} - 返回删除结果
   */
  bulkDelete(ids) {
    if (!Array.isArray(ids) || ids.length === 0) {
      return Promise.reject(new Error('请选择要删除的装备'));
    }
    
    clearCache();
    return http.post('/equipment/bulk-delete', ids, {
      headers: { 'Content-Type': 'application/json' }
    }).then(response => {
      ElMessage.success(`成功删除${ids.length}个装备`);
      return response;
    }).catch(error => {
      console.error('批量删除装备失败', error);
      ElMessage.error('批量删除装备失败，请稍后重试');
      return null;
    });
  },
  
  /**
   * 获取装备统计信息
   * @returns {Promise} - 返回统计信息
   */
  getStatistics() {
    return http.get('/equipment/statistics', {
      // 添加时间戳确保不使用缓存
      params: { _t: new Date().getTime() },
      headers: { 'Cache-Control': 'no-cache' }
    }).then(response => {
      // 安全处理响应数据
      if (!response) {
        console.warn('未收到统计数据响应或响应为空');
        return {
          totalCount: 0,
          availableCount: 0,
          categoryDistribution: [],
          statusDistribution: {}
        };
      }
      return response;
    }).catch(error => {
      console.error('获取装备统计信息失败', error);
      // 返回默认值以防止前端崩溃
      return {
        totalCount: 0,
        availableCount: 0,
        categoryDistribution: [],
        statusDistribution: {}
      };
    });
  },
  
  /**
   * 获取所有分类
   * @returns {Promise} - 返回分类列表
   */
  getAllCategories() {
    return http.get('/categories').catch(error => {
      console.error('获取所有分类失败', error);
      return [];
    });
  },
  
  /**
   * 获取分类详情
   * @param {Number} id - 分类ID
   * @returns {Promise} - 返回分类详情
   */
  getCategoryById(id) {
    if (!id) {
      return Promise.reject(new Error('分类ID不能为空'));
    }
    return http.get(`/categories/${id}`).catch(error => {
      console.error(`获取分类详情失败, ID: ${id}`, error);
      return {};
    });
  },
  
  /**
   * 创建分类
   * @param {Object} category - 分类数据
   * @returns {Promise} - 返回创建的分类
   */
  createCategory(category) {
    if (!category || !category.name) {
      return Promise.reject(new Error('分类名称不能为空'));
    }
    
    clearCache();
    return http.post('/categories', category, {
      headers: { 'Content-Type': 'application/json' }
    }).then(response => {
      ElMessage.success('分类创建成功');
      return response;
    }).catch(error => {
      console.error('创建分类失败', error);
      ElMessage.error('创建分类失败，请稍后重试');
      return null;
    });
  },
  
  /**
   * 更新分类
   * @param {Number} id - 分类ID
   * @param {Object} category - 分类数据
   * @returns {Promise} - 返回更新后的分类
   */
  updateCategory(id, category) {
    if (!id) {
      return Promise.reject(new Error('分类ID不能为空'));
    }
    
    clearCache();
    return http.put(`/categories/${id}`, category, {
      headers: { 'Content-Type': 'application/json' }
    }).then(response => {
      ElMessage.success('分类更新成功');
      return response;
    }).catch(error => {
      console.error(`更新分类失败, ID: ${id}`, error);
      ElMessage.error('更新分类失败，请稍后重试');
      return null;
    });
  },
  
  /**
   * 删除分类
   * @param {Number} id - 分类ID
   * @returns {Promise} - 返回删除结果
   */
  deleteCategory(id) {
    if (!id) {
      return Promise.reject(new Error('分类ID不能为空'));
    }
    
    clearCache();
    return http.delete(`/categories/${id}`).then(response => {
      ElMessage.success('分类删除成功');
      return true;
    }).catch(error => {
      console.error(`删除分类失败, ID: ${id}`, error);
      ElMessage.error('删除分类失败，请稍后重试');
      return false;
    });
  },
  
  /**
   * 导入装备数据
   * @param {File} file - 上传的文件（Excel或CSV）
   * @param {Object} options - 导入选项
   * @returns {Promise} - 返回导入结果
   */
  import(file, options = {}) {
    if (!file) {
      return Promise.reject(new Error('请选择要导入的文件'));
    }
    
    // 验证文件类型
    if (!file.name.endsWith('.xlsx') && !file.name.endsWith('.csv')) {
      return Promise.reject(new Error('仅支持导入.xlsx或.csv格式的文件'));
    }
    
    // 创建表单数据
    const formData = new FormData();
    formData.append('file', file);
    
    // 添加选项
    if (options.updateExisting !== undefined) {
      formData.append('updateExisting', options.updateExisting);
    }
    
    if (options.skipErrors !== undefined) {
      formData.append('skipErrors', options.skipErrors);
    }
    
    // 获取当前token
    const token = localStorage.getItem('token');
    if (!token) {
      return Promise.reject(new Error('未找到有效的身份验证令牌'));
    }
    
    clearCache();
    
    // 使用直接的axios调用以确保正确的headers设置
    return axios({
      url: '/api/equipment/import',
      method: 'POST',
      data: formData,
      headers: {
        'Content-Type': 'multipart/form-data',
        'Authorization': `Bearer ${token}`
      },
      timeout: 60000 // 设置较长的超时时间，因为导入可能需要更多时间
    }).then(response => {
      const message = response.data?.message || '装备数据导入成功';
      ElMessage.success(message);
      return response.data;
    }).catch(error => {
      console.error('导入装备数据失败', error);
      
      // 处理401错误
      if (error.response?.status === 401) {
        throw error; // 让调用方处理401错误
      }
      
      const errorMessage = error.response?.data?.message || '导入装备数据失败，请检查文件格式是否正确';
      ElMessage.error(errorMessage);
      throw error;
    });
  },
  
  /**
   * 获取导入模板
   * @param {String} format - 导出格式 ('csv' 或 'excel')
   * @returns {Promise} - 返回二进制流
   */
  getImportTemplate(format = 'excel') {
    const validFormats = ['csv', 'excel'];
    if (!validFormats.includes(format)) {
      return Promise.reject(new Error('不支持的格式，请使用 csv 或 excel'));
    }
    
    // 获取当前token
    const token = localStorage.getItem('token');
    if (!token) {
      return Promise.reject(new Error('未找到有效的身份验证令牌'));
    }
    
    // 使用直接的axios调用以确保正确的headers设置
    return axios({
      url: `/api/equipment/template?type=${format === 'excel' ? 'excel' : 'csv'}`,
      method: 'GET',
      responseType: 'blob',
      headers: {
        'Accept': format === 'excel' 
          ? 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
          : 'text/csv;charset=UTF-8',
        'Authorization': `Bearer ${token}`
      },
      timeout: 30000
    }).then(response => {
      return response.data;
    }).catch(error => {
      console.error(`获取导入模板失败，格式: ${format}`, error);
      ElMessage.error('获取导入模板失败，请稍后重试');
      throw error;
    });
  },
  
  /**
   * 验证认证是否有效
   * @returns {Promise} - 返回验证结果
   */
  checkAuthentication() {
    return http.get('/equipment/check-auth', {
      headers: {
        'Cache-Control': 'no-cache',
        'Pragma': 'no-cache'
      }
    }).then(() => true).catch(() => false);
  }
};

export default equipmentService; 