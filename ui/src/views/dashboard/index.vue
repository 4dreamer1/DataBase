<template>
  <div class="container-fluid py-4">
    <div class="row mb-4">
      <div class="col-12">
        <h2 class="mb-3 fw-bold page-title">仪表盘</h2>
      </div>
    </div>
    
    <!-- 服务器错误提示 -->
    <transition name="fade">
    <div class="row mb-4" v-if="error">
      <div class="col-12">
          <div class="alert alert-danger d-flex align-items-center shadow-sm rounded-3" role="alert">
          <i class="bi bi-exclamation-circle-fill me-2 fs-5"></i>
          <div>
            <strong>服务器部分错误</strong>
            <p class="mb-0">{{ errorMessage }}</p>
          </div>
          <button type="button" class="btn btn-sm btn-outline-danger ms-auto" @click="refreshData">
            <i class="bi bi-arrow-clockwise me-1"></i>重试
          </button>
        </div>
      </div>
    </div>
    </transition>
    
    <!-- 数据卡片组 -->
    <div class="row mb-4">
      <!-- 装备总数 -->
      <div class="col-xl-3 col-md-6 mb-4">
        <div class="card border-0 dashboard-card stat-card">
          <div class="card-body d-flex align-items-center">
            <div class="rounded-circle p-3 bg-primary bg-opacity-10 me-3 icon-container">
              <i class="bi bi-box-seam text-primary fs-4"></i>
            </div>
            <div>
              <h6 class="text-muted mb-1">装备总数</h6>
              <h3 class="mb-0 fw-bold counter-value">
                <span v-if="statsLoading"><i class="bi bi-hourglass spinner-border spinner-border-sm"></i></span>
                <span v-else>{{ stats.totalEquipment }}</span>
              </h3>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 可用装备 -->
      <div class="col-xl-3 col-md-6 mb-4">
        <div class="card border-0 dashboard-card stat-card">
          <div class="card-body d-flex align-items-center">
            <div class="rounded-circle p-3 bg-success bg-opacity-10 me-3 icon-container">
              <i class="bi bi-check-circle text-success fs-4"></i>
            </div>
            <div>
              <h6 class="text-muted mb-1">可用装备</h6>
              <h3 class="mb-0 fw-bold counter-value">
                <span v-if="statsLoading"><i class="bi bi-hourglass spinner-border spinner-border-sm"></i></span>
                <span v-else>{{ stats.availableEquipment }}</span>
              </h3>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 借用装备 -->
      <div class="col-xl-3 col-md-6 mb-4">
        <div class="card border-0 dashboard-card stat-card">
          <div class="card-body d-flex align-items-center">
            <div class="rounded-circle p-3 bg-warning bg-opacity-10 me-3 icon-container">
              <i class="bi bi-file-earmark-text text-warning fs-4"></i>
            </div>
            <div>
              <h6 class="text-muted mb-1">借用装备</h6>
              <h3 class="mb-0 fw-bold counter-value">
                <span v-if="statsLoading"><i class="bi bi-hourglass spinner-border spinner-border-sm"></i></span>
                <span v-else>{{ stats.borrowedEquipment }}</span>
              </h3>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 待审批 -->
      <div class="col-xl-3 col-md-6 mb-4">
        <div class="card border-0 dashboard-card stat-card">
          <div class="card-body d-flex align-items-center">
            <div class="rounded-circle p-3 bg-info bg-opacity-10 me-3 icon-container">
              <i class="bi bi-hourglass-split text-info fs-4"></i>
            </div>
            <div>
              <h6 class="text-muted mb-1">待审批</h6>
              <h3 class="mb-0 fw-bold counter-value">
                <span v-if="statsLoading"><i class="bi bi-hourglass spinner-border spinner-border-sm"></i></span>
                <span v-else>
                  {{ stats.pendingRequests }}
                  <span v-if="stats.pendingRequests > 0 && isAdmin" 
                    class="badge bg-danger rounded-pill ms-2 notification-badge" style="font-size: 0.5em; vertical-align: middle">
                    {{ stats.pendingRequests }}
                  </span>
                </span>
              </h3>
            </div>
          </div>
        </div>
      </div>
    </div>
    
    <div class="row">
      <!-- 最近借用记录 -->
      <div class="col-lg-8 mb-4">
        <div class="card border-0 dashboard-card">
          <div class="card-header bg-transparent py-3 border-bottom">
            <div class="d-flex justify-content-between align-items-center">
              <h5 class="mb-0 fw-bold card-title">
                <i class="bi bi-clock-history me-2 text-primary"></i>
                最近借用记录
              </h5>
              <router-link to="/borrow/list" class="btn btn-sm btn-outline-primary rounded-pill">
                <i class="bi bi-list me-1"></i>
                查看全部
              </router-link>
            </div>
          </div>
          <div class="card-body p-0">
            <div v-if="recentBorrowsLoading" class="text-center py-4">
              <div class="spinner-border text-primary" role="status">
                <span class="visually-hidden">加载中...</span>
              </div>
              <p class="mt-2 text-muted">正在加载借用记录...</p>
            </div>
            <div v-else class="table-responsive">
              <table class="table table-hover align-middle mb-0">
                <thead class="table-light">
                  <tr>
                    <th class="border-0">装备名称</th>
                    <th class="border-0">借用人</th>
                    <th class="border-0">借用时间</th>
                    <th class="border-0">状态</th>
                    <th class="border-0">操作</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="item in recentBorrows" :key="item.id" class="table-row-hover">
                    <td>
                      <div class="d-flex align-items-center">
                        <i class="bi bi-box me-2 text-primary"></i>
                        {{ item.equipment?.name || '未知装备' }}
                      </div>
                    </td>
                    <td>
                      <div class="d-flex align-items-center">
                        <i class="bi bi-person me-2 text-secondary"></i>
                        {{ item.user?.name || '未知用户' }}
                      </div>
                    </td>
                    <td>{{ formatDate(item.borrowTime) }}</td>
                    <td>
                      <span 
                        class="badge status-badge"
                        :class="{
                          'bg-warning': item.status === 'PENDING',
                          'bg-success': item.status === 'APPROVED',
                          'bg-danger': item.status === 'REJECTED',
                          'bg-info': item.status === 'RETURNED'
                        }"
                      >
                        {{ getBorrowStatusLabel(item.status) }}
                      </span>
                    </td>
                    <td>
                      <button 
                        class="btn btn-sm btn-outline-primary rounded-circle"
                        @click="viewBorrowDetail(item.id)"
                      >
                        <i class="bi bi-eye"></i>
                      </button>
                    </td>
                  </tr>
                  <tr v-if="recentBorrows.length === 0">
                    <td colspan="5" class="text-center py-5">
                      <div class="empty-state">
                        <i class="bi bi-inbox fs-1 text-muted mb-3"></i>
                        <p class="text-muted">暂无借用记录</p>
                      </div>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
          <div class="card-footer bg-transparent py-3">
            <router-link to="/borrow/my" class="btn btn-sm btn-primary rounded-pill" v-if="!isAdmin">
              <i class="bi bi-plus-lg me-1"></i>
              申请借用
            </router-link>
          </div>
        </div>
      </div>
      
      <!-- 库存预警 -->
      <div class="col-lg-4 mb-4" v-if="isAdmin">
        <div class="card border-0 dashboard-card h-100">
          <div class="card-header bg-transparent py-3 border-bottom">
            <h5 class="mb-0 fw-bold card-title">
              <i class="bi bi-exclamation-triangle me-2 text-warning"></i>
              库存预警
            </h5>
          </div>
          <div class="card-body">
            <div v-if="lowStockLoading" class="text-center py-4">
              <div class="spinner-border text-warning" role="status">
                <span class="visually-hidden">加载中...</span>
              </div>
              <p class="mt-2 text-muted">正在加载库存预警...</p>
            </div>
            <div v-else-if="lowStockItems.length > 0" class="warning-items">
              <transition-group name="list" tag="div">
              <div 
                v-for="item in lowStockItems" 
                :key="item.id"
                  class="alert alert-warning mb-3 warning-item"
              >
                <div class="d-flex align-items-center">
                  <i class="bi bi-exclamation-triangle-fill fs-5 me-2"></i>
                    <div class="flex-grow-1">
                    <h6 class="mb-1">{{ item.name }}</h6>
                      <div class="text-muted d-flex align-items-center">
                        <span>当前可用:</span>
                        <div class="progress ms-2 me-2 flex-grow-1" style="height: 6px;">
                          <div class="progress-bar bg-danger" role="progressbar" 
                            :style="`width: ${(item.availableQuantity / item.quantity) * 100}%`" 
                            :aria-valuenow="item.availableQuantity" 
                            aria-valuemin="0" 
                            :aria-valuemax="item.quantity">
                          </div>
                        </div>
                        <strong>{{ item.availableQuantity }}/{{ item.quantity }}</strong>
                      </div>
                    </div>
                    <router-link :to="`/equipment/${item.id}`" class="btn btn-sm btn-outline-warning ms-2">
                      <i class="bi bi-eye"></i>
                    </router-link>
                  </div>
                </div>
              </transition-group>
            </div>
            <div v-else class="empty-state py-4">
              <i class="bi bi-check-circle text-success fs-1 mb-3"></i>
              <div class="text-muted">当前没有库存预警</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 用户管理快捷入口 -->
    <div class="row" v-if="isAdmin">
      <div class="col-12">
        <div class="card border-0 dashboard-card">
          <div class="card-header bg-transparent py-3 border-bottom">
            <h5 class="mb-0 fw-bold card-title">
              <i class="bi bi-person-gear me-2 text-primary"></i>
              用户管理
            </h5>
          </div>
          <div class="card-body p-4">
            <div class="row">
              <div class="col-md-6 mb-3">
                <div class="card border-0 shortcut-card bg-light h-100">
                  <div class="card-body p-4">
                    <div class="d-flex align-items-center">
                      <div class="rounded-circle p-3 bg-primary bg-opacity-10 me-3 icon-container">
                        <i class="bi bi-people text-primary fs-4"></i>
                      </div>
                      <div>
                        <h5 class="fw-bold mb-1">用户列表</h5>
                        <p class="text-muted mb-3">管理系统用户，修改权限和状态</p>
                        <router-link to="/user/list" class="btn btn-primary rounded-pill">
                          <i class="bi bi-people me-1"></i>
                          查看用户
                        </router-link>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
              <div class="col-md-6 mb-3">
                <div class="card border-0 shortcut-card bg-light h-100">
                  <div class="card-body p-4">
                    <div class="d-flex align-items-center">
                      <div class="rounded-circle p-3 bg-success bg-opacity-10 me-3 icon-container">
                        <i class="bi bi-person-badge text-success fs-4"></i>
                      </div>
                      <div>
                        <h5 class="fw-bold mb-1">个人资料</h5>
                        <p class="text-muted mb-3">查看和修改您的个人信息</p>
                        <router-link to="/user/profile" class="btn btn-success rounded-pill">
                          <i class="bi bi-person me-1"></i>
                          个人中心
                        </router-link>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/store/modules/auth'
import borrowService from '@/api/borrow'
import equipmentService from '@/api/equipment'
import { useUserStore } from '@/store/modules/user'
import { useBorrowStore } from '@/store/modules/borrow'
import { ElMessage } from 'element-plus'

const router = useRouter()
const authStore = useAuthStore()
const userStore = useUserStore()
const borrowStore = useBorrowStore()

// 加载状态
const loading = ref(false)
const statsLoading = ref(true)
const recentBorrowsLoading = ref(true)
const lowStockLoading = ref(true)

// 错误状态
const error = ref(false)
const errorMessage = ref('服务器部分错误，请稍后再试')

// 统计数据
const stats = ref({
  totalEquipment: 0,
  availableEquipment: 0,
  borrowedEquipment: 0,
  pendingRequests: 0
})

// 最近借用记录
const recentBorrows = ref([])
const categoryStats = ref([])
const borrowTrend = ref([])

// 库存不足的装备
const lowStockItems = ref([])

// 是否是管理员
const isAdmin = computed(() => userStore.isAdmin)

// 刷新数据
const refreshData = async () => {
  error.value = false
  await fetchAllData()
}

// 获取装备统计数据
const fetchEquipmentStats = async () => {
  statsLoading.value = true
  try {
    const equipmentStats = await equipmentService.getStatistics()

    if (!equipmentStats) {
      throw new Error('获取装备统计数据失败')
    }
    
    // 确保使用正确的数据键名
    stats.value.totalEquipment = equipmentStats.totalCount || 0
    stats.value.availableEquipment = equipmentStats.availableCount || 0
    
    // 分类统计
    categoryStats.value = equipmentStats.categoryDistribution || []
    
    return true
  } catch (err) {
    console.error('获取装备统计数据出错:', err)
    error.value = true
    errorMessage.value = '获取装备统计数据失败，请稍后重试'
    return false
  } finally {
    statsLoading.value = false
  }
}

// 获取借用统计数据
const fetchBorrowStats = async () => {
  try {
    const borrowStats = await borrowService.getStatistics()

    if (!borrowStats) {
      throw new Error('获取借用统计数据失败')
    }
    
    stats.value.borrowedEquipment = borrowStats.activeBorrowCount || 0
    stats.value.pendingRequests = borrowStats.pendingCount || 0
    
    // 借用趋势
    borrowTrend.value = borrowStats.borrowTrend || []
    
    return true
  } catch (err) {
    console.error('获取借用统计数据出错:', err)
    error.value = true
    errorMessage.value = '获取借用统计数据失败，请稍后重试'
    return false
  }
}

// 获取最近借用记录
const fetchRecentBorrows = async (retryAttempt = 0) => {
  recentBorrowsLoading.value = true;
  try {
    // 最大重试次数
    const MAX_RETRIES = 2;
    
    let result = [];
    if (isAdmin.value) {
      // 确保store方法存在
      if (!borrowStore.fetchAllBorrows) {
        throw new Error('borrowStore.fetchAllBorrows方法不存在');
      }
      
      await borrowStore.fetchAllBorrows({ pageNum: 1, pageSize: 5 });
      result = borrowStore.allBorrows || [];
    } else {
      // 确保store方法存在
      if (!borrowStore.fetchMyBorrows) {
        throw new Error('borrowStore.fetchMyBorrows方法不存在');
      }
      
      await borrowStore.fetchMyBorrows();
      result = (borrowStore.myBorrows || []).slice(0, 5);
    }
    
    // 检查结果是否有效
    if (!Array.isArray(result)) {
      console.warn('借用记录返回值不是数组:', result);
      result = [];
    }
    
    // 确保结果数据格式正确
    if (result.length > 0) {
      console.log('获取到的借用记录:', result);
      
      // 确保每条记录都有正确的格式
      result = result.map(item => {
        // 确保必要的嵌套对象存在
        if (!item.equipment) item.equipment = { name: '未知装备' };
        if (!item.user) item.user = { name: '未知用户' };
        
        // 确保关键字段存在
        return {
          id: item.id || 0,
          equipment: item.equipment,
          user: item.user,
          borrowTime: item.borrowTime || new Date().toISOString(),
          status: item.status || 'PENDING',
          returnTime: item.returnTime,
          ...item // 保留其他字段
        };
      });
    }
    
    recentBorrows.value = result;
    return true;
  } catch (err) {
    console.error(`获取最近借用记录错误(尝试${retryAttempt + 1}/3):`, err);
    
    // 自动重试逻辑
    if (retryAttempt < 2) {
      const retryDelay = 1000 * (retryAttempt + 1); // 递增延迟

      return new Promise(resolve => {
        setTimeout(async () => {
          const success = await fetchRecentBorrows(retryAttempt + 1);
          resolve(success);
        }, retryDelay);
      });
    }
    
    // 所有重试失败后，设置错误状态
    error.value = true;
    errorMessage.value = '获取借用记录失败，请稍后重试';
    return false;
  } finally {
    // 在成功或所有重试失败后，将loading设置为false
    if (retryAttempt === 0 || retryAttempt >= 2) {
      recentBorrowsLoading.value = false;
    }
  }
};

// 获取库存预警
const fetchLowStockItems = async () => {
  if (!isAdmin.value) return true
  
  lowStockLoading.value = true
  try {
    const lowStockResponse = await equipmentService.getLowStockItems()

    if (!lowStockResponse || !lowStockResponse.data) {
      throw new Error('获取库存预警数据失败')
    }
    
    lowStockItems.value = lowStockResponse.data || []
    return true
  } catch (err) {
    console.error('获取库存预警出错:', err)
    error.value = true
    errorMessage.value = '获取库存预警失败，请稍后重试'
    return false
  } finally {
    lowStockLoading.value = false
  }
}

// 获取所有数据
const fetchAllData = async () => {
  loading.value = true
  error.value = false
  
  try {
    // 并行获取所有数据
    const [equipmentStatsSuccess, borrowStatsSuccess, recentBorrowsSuccess, lowStockSuccess] = await Promise.all([
      fetchEquipmentStats(),
      fetchBorrowStats(),
      fetchRecentBorrows(),
      fetchLowStockItems()
    ])
    
    // 如果任一请求失败，显示错误信息
    if (!equipmentStatsSuccess || !borrowStatsSuccess || !recentBorrowsSuccess || !lowStockSuccess) {
      error.value = true
    }
  } catch (err) {
    console.error('获取仪表板数据失败', err)
    error.value = true
    errorMessage.value = '获取数据过程中出现错误，请稍后重试'
  } finally {
    loading.value = false
  }
}

// 首次加载时获取数据
onMounted(async () => {
  await fetchAllData()
})

// 格式化日期
const formatDate = (dateString) => {
  if (!dateString) return '-'
  const date = new Date(dateString)
  return date.toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' })
}

// 获取借用状态标签
const getBorrowStatusLabel = (status) => {
  const statusMap = {
    'PENDING': '待审批',
    'APPROVED': '已批准',
    'REJECTED': '已拒绝',
    'RETURNED': '已归还'
  }
  return statusMap[status] || '未知'
}

// 查看借用详情
const viewBorrowDetail = (id) => {
  router.push(`/borrow/${id}`)
}
</script>

<style scoped>
.dashboard-card {
  border-radius: 0.75rem;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05) !important;
  overflow: hidden;
}

.dashboard-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.08) !important;
}

.stat-card {
  position: relative;
  overflow: hidden;
}

.stat-card::after {
  content: '';
  position: absolute;
  top: -10px;
  right: -10px;
  width: 100px;
  height: 100px;
  background: radial-gradient(circle, rgba(255,255,255,0.2) 0%, rgba(255,255,255,0) 70%);
  border-radius: 50%;
  z-index: 0;
}

.shortcut-card {
  transition: all 0.3s ease;
  border-radius: 0.75rem;
}

.shortcut-card:hover {
  background: linear-gradient(45deg, rgba(245,247,250,1) 0%, rgba(255,255,255,1) 100%) !important;
  transform: translateY(-5px);
  box-shadow: 0 10px 20px rgba(0, 0, 0, 0.05) !important;
}

/* 表格样式 */
.table th {
  font-weight: 600;
  font-size: 0.8rem;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  color: #495057;
}

.table td {
  padding: 0.75rem 1rem;
  vertical-align: middle;
}

.table-row-hover {
  transition: all 0.2s ease;
}

.table-row-hover:hover {
  background-color: rgba(67, 97, 238, 0.05);
}

/* 图标容器样式 */
.icon-container {
  width: 60px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.card:hover .icon-container {
  transform: scale(1.1) rotate(5deg);
}

/* 状态徽章 */
.status-badge {
  font-weight: 500;
  padding: 0.4em 0.8em;
  border-radius: 30px;
}

.bg-primary {
  background: linear-gradient(135deg, #4361ee, #3f37c9) !important;
}

.bg-success {
  background: linear-gradient(135deg, #2ecc71, #27ae60) !important;
}

.bg-warning {
  background: linear-gradient(135deg, #f39c12, #e67e22) !important;
}

.bg-danger {
  background: linear-gradient(135deg, #e74c3c, #c0392b) !important;
}

.bg-info {
  background: linear-gradient(135deg, #3498db, #2980b9) !important;
}

/* 按钮样式 */
.btn-primary {
  background: linear-gradient(135deg, #4361ee, #3f37c9);
  border: none;
  transition: all 0.3s ease;
}

.btn-primary:hover {
  background: linear-gradient(135deg, #3f37c9, #3a0ca3);
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(67, 97, 238, 0.3);
}

.btn-success {
  background: linear-gradient(135deg, #2ecc71, #27ae60);
  border: none;
  transition: all 0.3s ease;
}

.btn-success:hover {
  background: linear-gradient(135deg, #27ae60, #219653);
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(46, 204, 113, 0.3);
}

.rounded-pill {
  padding-left: 1rem;
  padding-right: 1rem;
}

/* 渐变图标背景 */
.bg-primary.bg-opacity-10 {
  background: linear-gradient(135deg, rgba(67, 97, 238, 0.1) 0%, rgba(58, 12, 163, 0.1) 100%) !important;
}

.bg-success.bg-opacity-10 {
  background: linear-gradient(135deg, rgba(46, 204, 113, 0.1) 0%, rgba(39, 174, 96, 0.1) 100%) !important;
}

.bg-warning.bg-opacity-10 {
  background: linear-gradient(135deg, rgba(243, 156, 18, 0.1) 0%, rgba(230, 126, 34, 0.1) 100%) !important;
}

.bg-info.bg-opacity-10 {
  background: linear-gradient(135deg, rgba(52, 152, 219, 0.1) 0%, rgba(41, 128, 185, 0.1) 100%) !important;
}

/* 计数器动画 */
.counter-value {
  position: relative;
  display: inline-block;
}

/* 通知徽章 */
.notification-badge {
  position: relative;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0% { transform: scale(1); }
  50% { transform: scale(1.2); }
  100% { transform: scale(1); }
}

/* 加载中动画 */
.spinner-border {
  width: 1.8rem;
  height: 1.8rem;
  border-width: 0.2em;
}

/* 动画效果 */
.fade-enter-active, .fade-leave-active {
  transition: opacity 0.5s;
}
.fade-enter, .fade-leave-to {
  opacity: 0;
}

.list-enter-active, .list-leave-active {
  transition: all 0.5s;
}
.list-enter, .list-leave-to {
  opacity: 0;
  transform: translateX(-30px);
}

/* 警告项目 */
.warning-item {
  border-radius: 10px;
  border-left: 4px solid #f39c12;
  transition: all 0.3s ease;
}

.warning-item:hover {
  transform: translateX(5px);
  box-shadow: 0 5px 15px rgba(243, 156, 18, 0.2);
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 2rem 0;
}

/* 页面标题 */
.page-title {
  position: relative;
  padding-left: 1rem;
  font-weight: 700;
}

.page-title::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  height: 1.5em;
  width: 4px;
  background: linear-gradient(to bottom, #4361ee, #3a0ca3);
  border-radius: 2px;
}

/* 卡片标题 */
.card-title {
  color: #2d3748;
  font-weight: 600;
}

/* 进度条样式 */
.progress {
  border-radius: 1rem;
  background-color: rgba(0, 0, 0, 0.05);
}

.progress-bar {
  border-radius: 1rem;
}
</style> 