import http from './http'
import { ElMessage } from 'element-plus'

// 清除借用相关缓存
const clearCache = () => {
  if (http.clearCache) {
    // 清除所有相关缓存

    http.clearCache('/borrow');
  }
};

/**
 * 安全处理借用相关的响应数据
 * @param {Object|Array} response - API响应
 * @param {Object} params - 请求参数
 * @returns {Object} - 标准化的响应数据
 */
const safeProcessBorrowResponse = (response, params = {}) => {
  try {

    // 如果响应是数组格式，包装为分页格式返回
    if (Array.isArray(response)) {
      return {
        data: {
          content: response,
          totalElements: response.length,
          totalPages: 1,
          number: 0,
          size: response.length,
          last: true
        }
      };
    }
    
    // 如果响应已经是分页格式，直接返回
    if (response && response.content) {
      return {
        data: {
          content: response.content,
          totalElements: response.totalElements || response.content.length,
          totalPages: response.totalPages || 1,
          number: response.number || 0,
          size: response.size || params.size || 10,
          last: response.last || true
        }
      };
    }
    
    // 其他情况，包装为标准格式返回
    return {
      data: {
        content: response || [],
        totalElements: Array.isArray(response) ? response.length : 0,
        totalPages: 1,
        number: 0,
        size: params.size || 10,
        last: true
      }
    };
  } catch (error) {
    console.error('处理借用响应数据失败:', error);
    // 返回一个空对象以防止前端崩溃
    return {
      data: {
        content: [],
        totalElements: 0,
        totalPages: 0,
        last: true,
        number: 0,
        size: 10
      }
    };
  }
};

// 借用相关API
const borrowService = {
  // 获取用户的借用记录
  getBorrows(params = {}) {
    return http.get('/borrow', { 
      params,
      headers: { 'Cache-Control': 'no-cache' }
    }).then(response => {

      return safeProcessBorrowResponse(response, params);
    }).catch(error => {
      console.error('获取借用记录失败', error);
      
      // 提取详细的错误信息
      let errorMsg = '请稍后重试';
      if (error.response?.data?.message) {
        errorMsg = error.response.data.message;
      } else if (error.message) {
        errorMsg = error.message;
      }
      
      console.error('错误详情:', errorMsg);
      ElMessage.error(`获取借用记录失败: ${errorMsg}`);
      
      // 返回一个空对象以防止前端崩溃
      return {
        data: {
          content: [],
          totalElements: 0,
          totalPages: 0,
          last: true,
          number: 0,
          size: 10
        }
      };
    });
  },
  
  // 获取所有借用记录（管理员）
  getAllBorrows(query) {
    return http.get('/borrow', { 
      params: query,
      headers: { 'Cache-Control': 'no-cache' }
    }).then(response => {

      return safeProcessBorrowResponse(response, query);
    }).catch(error => {
      console.error('获取所有借用记录失败', error);
      
      // 提取详细的错误信息
      let errorMsg = '请稍后重试';
      if (error.response?.data?.message) {
        errorMsg = error.response.data.message;
      } else if (error.message) {
        errorMsg = error.message;
      }
      
      console.error('错误详情:', errorMsg);
      ElMessage.error(`获取借用记录失败: ${errorMsg}`);
      
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
  
  // 获取待审批的借用申请（管理员）
  getPendingBorrows(params = {}) {
    return http.get('/borrow/pending', {
      params,
      headers: { 'Cache-Control': 'no-cache' }
    }).then(response => {
      // 规范化响应数据结构
      return safeProcessBorrowResponse(response, params).data;
    }).catch(error => {
      console.error('获取待审批借用申请失败', error);
      
      // 提取错误消息
      let errorMsg = '请稍后重试';
      if (error.response?.data?.message) {
        errorMsg = error.response.data.message;
      } else if (error.message) {
        errorMsg = error.message;
      }
      
      console.error('错误详情:', errorMsg);
      ElMessage.error(`获取待审批借用申请失败: ${errorMsg}`);
      return Promise.reject(error);
    });
  },
  
  // 获取借用详情
  getById(id) {
    if (!id) return Promise.reject(new Error('借用ID不能为空'));
    
    return http.get(`/borrow/${id}`, {
      headers: { 'Cache-Control': 'no-cache' }
    }).then(response => {

      // 确保返回有效数据
      if (!response) {
        console.warn(`借用ID ${id} 返回空数据`);
        return Promise.reject(new Error('服务器返回空数据'));
      }
      
      // 确保关联实体存在
      if (!response.equipment) {
        console.warn('借用记录缺少装备信息:', response);
        response.equipment = response.equipment || { name: '未知装备' };
      }
      
      if (!response.borrower) {
        console.warn('借用记录缺少借用人信息:', response);
        response.borrower = response.borrower || { name: '未知用户' };
      }
      
      return response;
    }).catch(error => {
      console.error(`获取借用详情失败, ID: ${id}`, error);
      
      // 提取错误消息
      let errorMsg = '请稍后重试';
      if (error.response?.data?.message) {
        errorMsg = error.response.data.message;
      } else if (error.message) {
        errorMsg = error.message;
      }
      
      ElMessage.error(`获取借用详情失败: ${errorMsg}`);
      return Promise.reject(error);
    });
  },
  
  // 获取装备当前的借用记录
  getActiveByEquipmentId(equipmentId) {
    // 添加时间戳确保不使用缓存
    const timestamp = new Date().getTime();
    
    return http.get(`/borrow/active/${equipmentId}?_t=${timestamp}`, {
      headers: {
        'Cache-Control': 'no-cache, no-store, must-revalidate',
        'Pragma': 'no-cache',
        'Expires': '0'
      },
      useCache: false
    }).then(response => {
      return response;
    });
  },
  
  // 申请借用
  apply(data) {
    clearCache();
    // 处理日期格式，确保与后端一致
    const requestData = {
      equipmentId: data.equipmentId,
      quantity: data.quantity || 1,
      expectedReturnDate: data.expectedReturnTime ? new Date(data.expectedReturnTime) : null,
      purpose: data.reason,
      remarks: data.remarks || ''
    };
    
    return http.post('/borrow', requestData).then(response => {
      ElMessage.success('借用申请已提交');
      return response;
    }).catch(error => {
      console.error('提交借用申请失败', error);
      
      // 提取错误消息
      let errorMsg = '请稍后重试';
      if (error.response?.data?.message) {
        errorMsg = error.response.data.message;
      }
      
      ElMessage.error(`借用申请提交失败: ${errorMsg}`);
      return Promise.reject(error);
    });
  },
  
  // 创建借用申请
  create(data) {
    clearCache();
    return http.post('/borrow', data).then(response => {
      ElMessage.success('借用记录已创建');
      return response;
    }).catch(error => {
      console.error('创建借用记录失败', error);
      
      // 提取错误消息
      let errorMsg = '请稍后重试';
      if (error.response?.data?.message) {
        errorMsg = error.response.data.message;
      }
      
      ElMessage.error(`创建借用记录失败: ${errorMsg}`);
      return Promise.reject(error);
    });
  },
  
  // 批准借用
  approveBorrow(id, data) {
    clearCache();
    return http.put(`/borrow/${id}/approve`, data).then(response => {
      ElMessage.success('已批准借用申请');
      return response;
    }).catch(error => {
      console.error(`批准借用申请失败, ID: ${id}`, error);
      
      // 提取错误消息
      let errorMsg = '请稍后重试';
      if (error.response?.data?.message) {
        errorMsg = error.response.data.message;
      }
      
      ElMessage.error(`批准借用申请失败: ${errorMsg}`);
      return Promise.reject(error);
    });
  },
  
  // 拒绝借用
  rejectBorrow(id, data) {
    clearCache();
    return http.put(`/borrow/${id}/reject`, data).then(response => {
      ElMessage.success('已拒绝借用申请');
      return response;
    }).catch(error => {
      console.error(`拒绝借用申请失败, ID: ${id}`, error);
      
      // 提取错误消息
      let errorMsg = '请稍后重试';
      if (error.response?.data?.message) {
        errorMsg = error.response.data.message;
      }
      
      ElMessage.error(`拒绝借用申请失败: ${errorMsg}`);
      return Promise.reject(error);
    });
  },
  
  // 归还装备
  returnEquipment(id, notes) {
    clearCache();
    return http.put(`/borrow/${id}/return`, notes).then(response => {
      ElMessage.success('已归还装备');
      return response;
    }).catch(error => {
      console.error(`归还装备失败, ID: ${id}`, error);
      
      // 提取错误消息
      let errorMsg = '请稍后重试';
      if (error.response?.data?.message) {
        errorMsg = error.response.data.message;
      }
      
      ElMessage.error(`归还装备失败: ${errorMsg}`);
      return Promise.reject(error);
    });
  },
  
  // 取消借用
  cancelBorrow(id, data) {
    clearCache();
    return http.put(`/borrow/${id}/cancel`, typeof data === 'string' ? { reason: data } : data).then(response => {
      ElMessage.success('已取消借用申请');
      return response;
    }).catch(error => {
      console.error(`取消借用申请失败, ID: ${id}`, error);
      
      // 提取错误消息
      let errorMsg = '请稍后重试';
      if (error.response?.data?.message) {
        errorMsg = error.response.data.message;
      }
      
      ElMessage.error(`取消借用申请失败: ${errorMsg}`);
      return Promise.reject(error);
    });
  },
  
  // 获取借用统计数据（基本统计）
  getStatistics(params) {
    return http.get('/borrow/statistics', { 
      params: { 
        ...params, 
        _t: new Date().getTime() // 添加时间戳避免缓存
      },
      headers: { 'Cache-Control': 'no-cache' }
    }).then(response => {
      // 安全处理响应数据
      if (!response) {
        console.warn('未收到借用统计数据响应或响应为空');
        return {
          activeBorrowCount: 0,
          pendingCount: 0,
          totalCount: 0,
          overdueCount: 0
        };
      }
      
      // 字段名称映射，确保前端和后端字段名称一致
      return {
        activeBorrowCount: response.borrowedCount || 0,
        pendingCount: response.pendingCount || 0,
        totalCount: response.totalBorrows || 0, 
        overdueCount: response.overdueCount || 0
      };
    }).catch(error => {
      console.error('获取借用统计数据失败', error);
      
      // 如果是权限错误，尝试使用备用方法获取统计数据
      if (error.response?.status === 403) {
        return this.getFallbackStats();
      }
      
      ElMessage.error('获取借用统计数据失败，请稍后重试');
      return {
        activeBorrowCount: 0,
        pendingCount: 0,
        totalCount: 0,
        overdueCount: 0
      };
    });
  },
  
  // 备用方法：通过获取所有借用记录来计算统计数据
  getFallbackStats() {
    return http.get('/borrow', { 
      headers: { 'Cache-Control': 'no-cache' }
    }).then(response => {
      if (!response || !response.content || !Array.isArray(response.content)) {
        console.warn('获取借用记录失败，无法计算统计数据');
        return {
          activeBorrowCount: 0,
          pendingCount: 0,
          totalCount: 0,
          overdueCount: 0
        };
      }
      
      const records = response.content;
      const now = new Date();
      
      // 计算各状态数量
      let pendingCount = 0;
      let activeBorrowCount = 0;
      let overdueCount = 0;
      
      records.forEach(record => {
        if (record.status === 'PENDING') {
          pendingCount++;
        } else if (record.status === 'BORROWED' || record.status === 'APPROVED') {
          // 检查是否逾期
          const expectedReturnDate = record.expectedReturnDate ? new Date(record.expectedReturnDate) : null;
          if (expectedReturnDate && expectedReturnDate < now) {
            overdueCount++;
          } else {
            activeBorrowCount++;
          }
        }
      });
      
      return {
        activeBorrowCount,
        pendingCount,
        totalCount: records.length,
        overdueCount
      };
    }).catch(error => {
      console.error('备用统计方法失败:', error);
      return {
        activeBorrowCount: 0,
        pendingCount: 0, 
        totalCount: 0,
        overdueCount: 0
      };
    });
  },
  
  // 获取借用趋势数据（按日期统计）
  getTrendData(params) {
    return http.get('/borrow/trends', { params }).catch(error => {
      console.error('获取借用趋势数据失败', error);
      return [];
    });
  },
  
  // 获取装备借用排名
  getEquipmentRanking(params) {
    return http.get('/borrow/equipment-ranking', { params }).catch(error => {
      console.error('获取装备借用排名失败', error);
      return [];
    });
  },
  
  // 获取用户借用排名
  getUserRanking(params) {
    return http.get('/borrow/user-ranking', { params }).catch(error => {
      console.error('获取用户借用排名失败', error);
      return [];
    });
  },
  
  // 获取即将到期的借用记录
  getExpiringBorrows(days = 3) {
    return http.get(`/borrow/expiring?days=${days}`, {
      headers: { 'Cache-Control': 'no-cache' }
    }).then(response => {

      return { data: response };
    }).catch(error => {
      console.error('获取即将到期借用记录失败', error);
      ElMessage.error('获取即将到期借用记录失败，请稍后重试');
      return { data: [] };
    });
  },
  
  // 获取逾期的借用记录
  getOverdueBorrows() {
    return http.get('/borrow/overdue', {
      headers: { 'Cache-Control': 'no-cache' }
    }).then(response => {

      return { data: response };
    }).catch(error => {
      console.error('获取逾期借用记录失败', error);
      ElMessage.error('获取逾期借用记录失败，请稍后重试');
      return { data: [] };
    });
  },
  
  // 发送逾期提醒
  sendOverdueReminder(id, message = '') {
    return http.put(`/borrow/${id}/send-reminder`, { message }).then(response => {

      ElMessage.success('提醒已发送');
      return response;
    }).catch(error => {
      console.error('发送提醒失败', error);
      ElMessage.error('发送提醒失败，请稍后重试');
      throw error;
    });
  },
  
  // 批量发送逾期提醒
  sendBatchOverdueReminders(data) {
    return http.put('/borrow/batch-send-reminders', data).then(response => {

      return response;
    }).catch(error => {
      console.error('批量发送提醒失败', error);
      ElMessage.error('批量发送提醒失败，请稍后重试');
      throw error;
    });
  },
  
  // 导出借用记录
  exportData(query) {
    return http.get('/borrow/export', { 
      params: query,
      responseType: 'blob'
    }).catch(error => {
      console.error('导出借用记录失败', error);
      ElMessage.error('导出借用记录失败，请稍后重试');
      return null;
    });
  },
  
  // 添加借用备注
  addNote(id, note) {
    return http.post(`/borrow/${id}/notes`, { content: note }).then(response => {
      ElMessage.success('已添加备注');
      return response;
    }).catch(error => {
      console.error(`添加借用备注失败, ID: ${id}`, error);
      ElMessage.error('添加备注失败，请稍后重试');
      return null;
    });
  },
  
  // 获取借用记录的备注列表
  getNotes(id) {
    return http.get(`/borrow/${id}/notes`).catch(error => {
      console.error(`获取借用备注失败, ID: ${id}`, error);
      return [];
    });
  },
  
  // 删除借用记录
  delete(id) {
    clearCache();
    return http.delete(`/borrow/${id}`).then(response => {
      ElMessage.success('已删除借用记录');
      return response;
    }).catch(error => {
      console.error(`删除借用记录失败, ID: ${id}`, error);
      ElMessage.error('删除借用记录失败，请稍后重试');
      return null;
    });
  },
  
  // 获取我的借用记录
  getMyBorrows(params = {}) {
    return http.get('/borrow/my', { 
      params,
      headers: { 'Cache-Control': 'no-cache' }
    }).then(response => {

      return safeProcessBorrowResponse(response, params);
    }).catch(error => {
      console.error('获取我的借用记录失败', error);
      
      // 提取详细的错误信息
      let errorMsg = '请稍后重试';
      if (error.response?.data?.message) {
        errorMsg = error.response.data.message;
      } else if (error.message) {
        errorMsg = error.message;
      }
      
      console.error('错误详情:', errorMsg);
      ElMessage.error(`获取我的借用记录失败: ${errorMsg}`);
      
      // 返回一个空对象以防止前端崩溃
      return {
        data: {
          content: [],
          totalElements: 0,
          totalPages: 0,
          last: true,
          number: 0,
          size: 10
        }
      };
    });
  },
  
  // 获取我的借用统计
  getMyBorrowStats() {
    return http.get('/borrow/my/stats').then(response => {
      return {
        data: response || {
          pending: 0,
          active: 0,
          overdue: 0,
          returned: 0
        }
      };
    }).catch(error => {
      console.error('获取我的借用统计失败', error);
      return {
        data: {
          pending: 0,
          active: 0,
          overdue: 0,
          returned: 0
        }
      };
    });
  },
  
  // 批量批准借用
  batchApproveBorrows(data) {
    clearCache();
    return http.put('/borrow/batch-approve', data).then(response => {
      ElMessage.success(`已批准${data.ids.length}条借用申请`);
      return response;
    }).catch(error => {
      console.error('批量批准借用申请失败', error);
      
      // 提取错误消息
      let errorMsg = '请稍后重试';
      if (error.response?.data?.message) {
        errorMsg = error.response.data.message;
      }
      
      ElMessage.error(`批量批准借用申请失败: ${errorMsg}`);
      return Promise.reject(error);
    });
  },
  
  // 批量拒绝借用
  batchRejectBorrows(data) {
    clearCache();
    return http.put('/borrow/batch-reject', data).then(response => {
      ElMessage.success(`已拒绝${data.ids.length}条借用申请`);
      return response;
    }).catch(error => {
      console.error('批量拒绝借用申请失败', error);
      
      // 提取错误消息
      let errorMsg = '请稍后重试';
      if (error.response?.data?.message) {
        errorMsg = error.response.data.message;
      }
      
      ElMessage.error(`批量拒绝借用申请失败: ${errorMsg}`);
      return Promise.reject(error);
    });
  },
};

export default borrowService;