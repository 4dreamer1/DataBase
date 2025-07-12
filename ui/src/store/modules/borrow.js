import { defineStore } from 'pinia'
import borrowService from '@/api/borrow'
import { useUserStore } from './user'

export const useBorrowStore = defineStore('borrow', {
  state: () => ({
    myBorrows: [],
    pendingBorrows: [],
    allBorrows: [],
    loading: false
  }),
  getters: {
    pendingCount: (state) => state.pendingBorrows.length,
    activeBorrowCount: (state) => state.myBorrows.filter(item => item.status === 'BORROWED' || item.status === 'APPROVED').length,
    overdueCount: (state) => state.myBorrows.filter(item => item.status === 'OVERDUE' || 
      (item.status === 'BORROWED' && new Date(item.expectedReturnDate) < new Date())).length
  },
  actions: {
    // 获取我的借用记录
    async fetchMyBorrows() {
      this.loading = true
      try {
        const res = await borrowService.getBorrows()

        // 安全处理响应数据
        if (res && res.data && res.data.content) {
          this.myBorrows = res.data.content;
        } else if (res && Array.isArray(res.data)) {
          this.myBorrows = res.data;
        } else if (Array.isArray(res)) {
          this.myBorrows = res;
        } else {
          this.myBorrows = [];
          console.warn('借用记录响应格式不符合预期:', res);
        }
        
        // 输出调试信息
        console.log('获取到的我的借用记录:', this.myBorrows);
        
        // 确保每个记录都有正确的数据结构
        if (this.myBorrows.length > 0) {
          this.myBorrows = this.myBorrows.map(item => {
            if (!item.equipment) item.equipment = { name: '未知装备' };
            if (!item.user) item.user = { name: '未知用户' };
            return item;
          });
        }
        
        return Promise.resolve(res)
      } catch (error) {
        console.error('获取我的借用记录失败', error);
        
        // 提取详细的错误信息
        let errorMsg = '请稍后重试';
        if (error.response?.data?.message) {
          errorMsg = error.response.data.message;
        } else if (error.message) {
          errorMsg = error.message;
        }
        
        console.error('错误详情:', errorMsg);
        
        this.myBorrows = [];
        return Promise.reject(error)
      } finally {
        this.loading = false
      }
    },
    
    // 获取待审批借用记录
    async fetchPendingBorrows() {
      this.loading = true
      try {
        const res = await borrowService.getPendingBorrows()

        // 安全处理响应数据
        if (res && Array.isArray(res)) {
          this.pendingBorrows = res;
        } else if (res && Array.isArray(res.data)) {
          this.pendingBorrows = res.data;
        } else {
          this.pendingBorrows = [];
          console.warn('待审批借用记录响应格式不符合预期:', res);
        }
        
        // 更新用户状态中的待审批数量
        const userStore = useUserStore()
        userStore.updatePendingCount(this.pendingBorrows.length)
        
        return Promise.resolve(res)
      } catch (error) {
        console.error('获取待审批借用记录失败', error);
        
        // 提取详细的错误信息
        let errorMsg = '请稍后重试';
        if (error.response?.data?.message) {
          errorMsg = error.response.data.message;
        } else if (error.message) {
          errorMsg = error.message;
        }
        
        console.error('错误详情:', errorMsg);
        
        this.pendingBorrows = [];
        return Promise.reject(error)
      } finally {
        this.loading = false
      }
    },
    
    // 获取所有借用记录
    async fetchAllBorrows(params) {
      this.loading = true
      try {
        const res = await borrowService.getAllBorrows(params)

        // 安全处理响应数据
        if (res && res.data && Array.isArray(res.data.content)) {
          this.allBorrows = res.data.content;
        } else if (res && res.content && Array.isArray(res.content)) {
          this.allBorrows = res.content;
        } else if (Array.isArray(res)) {
          this.allBorrows = res;
        } else if (res && Array.isArray(res.data)) {
          this.allBorrows = res.data;
        } else {
          this.allBorrows = [];
          console.warn('所有借用记录响应格式不符合预期:', res);
        }
        
        // 输出调试信息
        console.log('处理后的借用记录:', this.allBorrows);
        
        return Promise.resolve(res)
      } catch (error) {
        console.error('获取所有借用记录失败', error);
        
        // 提取详细的错误信息
        let errorMsg = '请稍后重试';
        if (error.response?.data?.message) {
          errorMsg = error.response.data.message;
        } else if (error.message) {
          errorMsg = error.message;
        }
        
        console.error('错误详情:', errorMsg);
        
        this.allBorrows = [];
        return Promise.reject(error)
      } finally {
        this.loading = false
      }
    },
    
    // 创建借用申请
    async createBorrow(borrowData) {
      try {
        const res = await borrowService.create(borrowData)
        await this.fetchMyBorrows()
        return Promise.resolve(res)
      } catch (error) {
        console.error('创建借用申请失败', error)
        return Promise.reject(error)
      }
    },
    
    // 审批借用申请
    async approveBorrow(id, data) {
      try {
        const res = await borrowService.approveBorrow(id, data)
        await this.fetchPendingBorrows()
        return Promise.resolve(res)
      } catch (error) {
        console.error('审批借用申请失败', error)
        return Promise.reject(error)
      }
    },
    
    // 拒绝借用申请
    async rejectBorrow(id, data) {
      try {
        const res = await borrowService.rejectBorrow(id, data)
        await this.fetchPendingBorrows()
        return Promise.resolve(res)
      } catch (error) {
        console.error('拒绝借用申请失败', error)
        return Promise.reject(error)
      }
    },
    
    // 归还装备
    async returnEquipment(id, notes) {
      try {
        const res = await borrowService.returnEquipment(id, notes)
        await this.fetchMyBorrows()
        return Promise.resolve(res)
      } catch (error) {
        console.error('归还装备失败', error)
        return Promise.reject(error)
      }
    },
    
    // 取消借用申请
    async cancelBorrow(id, reason) {
      try {
        const res = await borrowService.cancelBorrow(id, { reason })
        await this.fetchMyBorrows()
        return Promise.resolve(res)
      } catch (error) {
        console.error('取消借用申请失败', error)
        return Promise.reject(error)
      }
    },
    
    // 设置待审批借用记录
    setPendingBorrows(borrows) {
      if (Array.isArray(borrows)) {
        this.pendingBorrows = borrows
      } else {
        this.pendingBorrows = []
        console.warn('待审批借用记录数据格式错误:', borrows)
      }
    },
    
    // 设置待审批数量
    setPendingCount(count) {
      const userStore = useUserStore()
      if (typeof count === 'number') {
        userStore.updatePendingCount(count)
      } else {
        userStore.updatePendingCount(this.pendingBorrows.length)
      }
    }
  }
}) 