<template>
  <div class="container-fluid py-4">
    <!-- 页面标题 -->
    <div class="row mb-4">
      <div class="col-12">
        <div class="card border-0 shadow-sm">
          <div class="card-body d-flex justify-content-between align-items-center">
            <h5 class="m-0"><i class="bi bi-exclamation-triangle me-2"></i>预期管理</h5>
            <div>
              <button class="btn btn-outline-primary me-2" @click="refreshData">
                <i class="bi bi-arrow-clockwise me-1"></i>刷新数据
              </button>
              <button 
                v-if="selectedRecords.length > 0" 
                class="btn btn-warning"
                @click="confirmSendBatchReminders"
              >
                <i class="bi bi-bell me-1"></i>发送提醒 ({{ selectedRecords.length }})
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 数据卡片 -->
    <div class="row mb-4">
      <!-- 逾期总数 -->
      <div class="col-xl-3 col-md-6 mb-4">
        <div class="card border-0 shadow-sm h-100">
          <div class="card-body">
            <div class="d-flex justify-content-between align-items-center">
              <div>
                <h6 class="text-muted mb-1">逾期总数</h6>
                <h3 class="mb-0 fw-bold">{{ overdueBorrows.length }}</h3>
              </div>
              <div class="rounded-circle p-3 bg-danger bg-opacity-10">
                <i class="bi bi-calendar-x text-danger fs-4"></i>
              </div>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 今日到期 -->
      <div class="col-xl-3 col-md-6 mb-4">
        <div class="card border-0 shadow-sm h-100">
          <div class="card-body">
            <div class="d-flex justify-content-between align-items-center">
              <div>
                <h6 class="text-muted mb-1">今日到期</h6>
                <h3 class="mb-0 fw-bold">{{ todayExpiring }}</h3>
              </div>
              <div class="rounded-circle p-3 bg-warning bg-opacity-10">
                <i class="bi bi-calendar-day text-warning fs-4"></i>
              </div>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 即将到期 -->
      <div class="col-xl-3 col-md-6 mb-4">
        <div class="card border-0 shadow-sm h-100">
          <div class="card-body">
            <div class="d-flex justify-content-between align-items-center">
              <div>
                <h6 class="text-muted mb-1">即将到期 (3天内)</h6>
                <h3 class="mb-0 fw-bold">{{ upcomingExpiring }}</h3>
              </div>
              <div class="rounded-circle p-3 bg-info bg-opacity-10">
                <i class="bi bi-calendar-week text-info fs-4"></i>
              </div>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 提醒发送 -->
      <div class="col-xl-3 col-md-6 mb-4">
        <div class="card border-0 shadow-sm h-100">
          <div class="card-body">
            <div class="d-flex justify-content-between align-items-center">
              <div>
                <h6 class="text-muted mb-1">已发送提醒</h6>
                <h3 class="mb-0 fw-bold">{{ reminderSent }}</h3>
              </div>
              <div class="rounded-circle p-3 bg-success bg-opacity-10">
                <i class="bi bi-bell text-success fs-4"></i>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 筛选选项卡 -->
    <div class="row mb-4">
      <div class="col-12">
        <div class="card border-0 shadow-sm">
          <div class="card-body">
            <ul class="nav nav-tabs" role="tablist">
              <li class="nav-item" role="presentation">
                <button 
                  class="nav-link active" 
                  data-bs-toggle="tab" 
                  data-bs-target="#overdue-tab" 
                  type="button" 
                  role="tab" 
                  aria-selected="true"
                  @click="currentTab = 'overdue'"
                >
                  已逾期 <span class="badge bg-danger ms-1">{{ overdueBorrows.length }}</span>
                </button>
              </li>
              <li class="nav-item" role="presentation">
                <button 
                  class="nav-link" 
                  data-bs-toggle="tab" 
                  data-bs-target="#expiring-tab" 
                  type="button" 
                  role="tab" 
                  aria-selected="false"
                  @click="currentTab = 'expiring'"
                >
                  即将到期 <span class="badge bg-warning text-dark ms-1">{{ expiringBorrows.length }}</span>
                </button>
              </li>
            </ul>
          </div>
        </div>
      </div>
    </div>

    <!-- 表格内容 -->
    <div class="row">
      <div class="col-12">
        <div class="tab-content">
          <!-- 已逾期标签页 -->
          <div class="tab-pane fade show active" id="overdue-tab" role="tabpanel">
            <div class="card border-0 shadow-sm">
              <div class="card-body p-0">
                <div class="table-responsive">
                  <table class="table table-hover align-middle mb-0">
                    <thead class="table-light">
                      <tr>
                        <th width="40">
                          <div class="form-check">
                            <input 
                              class="form-check-input" 
                              type="checkbox" 
                              id="selectAllOverdue" 
                              v-model="selectAllOverdue"
                              @change="toggleSelectAll('overdue')"
                            >
                          </div>
                        </th>
                        <th>装备名称</th>
                        <th>借用人</th>
                        <th>借用日期</th>
                        <th>预计归还日期</th>
                        <th>逾期天数</th>
                        <th>提醒状态</th>
                        <th width="120">操作</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-if="loading">
                        <td colspan="8" class="text-center py-4">
                          <div class="spinner-border text-primary" role="status">
                            <span class="visually-hidden">加载中...</span>
                          </div>
                        </td>
                      </tr>
                      <tr v-else-if="overdueBorrows.length === 0">
                        <td colspan="8" class="text-center py-4">
                          <div class="text-muted">
                            <i class="bi bi-inbox fs-4 d-block mb-2"></i>
                            <p class="mb-0">暂无逾期记录</p>
                          </div>
                        </td>
                      </tr>
                      <tr v-for="item in overdueBorrows" :key="item.id">
                        <td>
                          <div class="form-check">
                            <input 
                              class="form-check-input" 
                              type="checkbox" 
                              :id="`overdue-${item.id}`"
                              v-model="selectedRecords"
                              :value="item.id"
                            >
                          </div>
                        </td>
                        <td>
                          <div class="d-flex align-items-center">
                            <div class="rounded-circle bg-light p-2 me-2">
                              <i class="bi bi-box-seam text-primary"></i>
                            </div>
                            <div>
                              <div class="fw-bold">{{ item.equipment?.name }}</div>
                              <div class="small text-muted">{{ item.equipment?.serialNumber }}</div>
                            </div>
                          </div>
                        </td>
                        <td>
                          <div class="d-flex align-items-center">
                            <div class="rounded-circle bg-light p-2 me-2">
                              <i class="bi bi-person text-primary"></i>
                            </div>
                            <div>{{ item.borrower?.name }}</div>
                          </div>
                        </td>
                        <td>{{ formatDate(item.borrowDate) }}</td>
                        <td>{{ formatDate(item.expectedReturnDate) }}</td>
                        <td>
                          <span class="badge bg-danger">{{ calculateOverdueDays(item.expectedReturnDate) }}天</span>
                        </td>
                        <td>
                          <span 
                            class="badge" 
                            :class="item.reminderSent ? 'bg-success' : 'bg-secondary'"
                          >
                            {{ item.reminderSent ? '已提醒' : '未提醒' }}
                          </span>
                        </td>
                        <td>
                          <div class="btn-group btn-group-sm">
                            <button 
                              class="btn btn-outline-primary" 
                              @click="viewDetail(item.id)"
                              title="查看详情"
                            >
                              <i class="bi bi-eye"></i>
                            </button>
                            <button 
                              class="btn btn-outline-warning" 
                              @click="sendReminder(item.id)"
                              title="发送提醒"
                              :disabled="item.reminderSent"
                            >
                              <i class="bi bi-bell"></i>
                            </button>
                          </div>
                        </td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          </div>
          
          <!-- 即将到期标签页 -->
          <div class="tab-pane fade" id="expiring-tab" role="tabpanel">
            <div class="card border-0 shadow-sm">
              <div class="card-body p-0">
                <div class="table-responsive">
                  <table class="table table-hover align-middle mb-0">
                    <thead class="table-light">
                      <tr>
                        <th width="40">
                          <div class="form-check">
                            <input 
                              class="form-check-input" 
                              type="checkbox" 
                              id="selectAllExpiring" 
                              v-model="selectAllExpiring"
                              @change="toggleSelectAll('expiring')"
                            >
                          </div>
                        </th>
                        <th>装备名称</th>
                        <th>借用人</th>
                        <th>借用日期</th>
                        <th>预计归还日期</th>
                        <th>剩余天数</th>
                        <th>提醒状态</th>
                        <th width="120">操作</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-if="loading">
                        <td colspan="8" class="text-center py-4">
                          <div class="spinner-border text-primary" role="status">
                            <span class="visually-hidden">加载中...</span>
                          </div>
                        </td>
                      </tr>
                      <tr v-else-if="expiringBorrows.length === 0">
                        <td colspan="8" class="text-center py-4">
                          <div class="text-muted">
                            <i class="bi bi-inbox fs-4 d-block mb-2"></i>
                            <p class="mb-0">暂无即将到期记录</p>
                          </div>
                        </td>
                      </tr>
                      <tr v-for="item in expiringBorrows" :key="item.id">
                        <td>
                          <div class="form-check">
                            <input 
                              class="form-check-input" 
                              type="checkbox" 
                              :id="`expiring-${item.id}`"
                              v-model="selectedRecords"
                              :value="item.id"
                            >
                          </div>
                        </td>
                        <td>
                          <div class="d-flex align-items-center">
                            <div class="rounded-circle bg-light p-2 me-2">
                              <i class="bi bi-box-seam text-primary"></i>
                            </div>
                            <div>
                              <div class="fw-bold">{{ item.equipment?.name }}</div>
                              <div class="small text-muted">{{ item.equipment?.serialNumber }}</div>
                            </div>
                          </div>
                        </td>
                        <td>
                          <div class="d-flex align-items-center">
                            <div class="rounded-circle bg-light p-2 me-2">
                              <i class="bi bi-person text-primary"></i>
                            </div>
                            <div>{{ item.borrower?.name }}</div>
                          </div>
                        </td>
                        <td>{{ formatDate(item.borrowDate) }}</td>
                        <td>{{ formatDate(item.expectedReturnDate) }}</td>
                        <td>
                          <span 
                            class="badge"
                            :class="{
                              'bg-danger': calculateRemainingDays(item.expectedReturnDate) === 0,
                              'bg-warning text-dark': calculateRemainingDays(item.expectedReturnDate) === 1,
                              'bg-info': calculateRemainingDays(item.expectedReturnDate) > 1
                            }"
                          >
                            {{ calculateRemainingDays(item.expectedReturnDate) }}天
                          </span>
                        </td>
                        <td>
                          <span 
                            class="badge" 
                            :class="item.reminderSent ? 'bg-success' : 'bg-secondary'"
                          >
                            {{ item.reminderSent ? '已提醒' : '未提醒' }}
                          </span>
                        </td>
                        <td>
                          <div class="btn-group btn-group-sm">
                            <button 
                              class="btn btn-outline-primary" 
                              @click="viewDetail(item.id)"
                              title="查看详情"
                            >
                              <i class="bi bi-eye"></i>
                            </button>
                            <button 
                              class="btn btn-outline-warning" 
                              @click="sendReminder(item.id)"
                              title="发送提醒"
                              :disabled="item.reminderSent"
                            >
                              <i class="bi bi-bell"></i>
                            </button>
                          </div>
                        </td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 批量提醒模态框 -->
    <div class="modal fade" id="batchReminderModal" tabindex="-1" aria-hidden="true">
      <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content">
          <div class="modal-header">
            <h5 class="modal-title">发送批量提醒</h5>
            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
          </div>
          <div class="modal-body">
            <div class="text-center mb-4">
              <div class="rounded-circle bg-warning bg-opacity-10 p-3 mx-auto mb-3" style="width: 80px; height: 80px; display: flex; align-items: center; justify-content: center;">
                <i class="bi bi-bell fs-1 text-warning"></i>
              </div>
              <h5>确定发送批量提醒？</h5>
              <p class="text-muted">
                您选择了 <strong>{{ selectedRecords.length }}</strong> 条记录，确定要发送提醒吗？
              </p>
            </div>
            
            <div class="mb-3">
              <label for="reminderMessage" class="form-label">提醒内容</label>
              <textarea 
                class="form-control" 
                id="reminderMessage" 
                rows="3" 
                v-model="reminderMessage"
                placeholder="请归还您借用的装备，谢谢配合。"
              ></textarea>
            </div>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">取消</button>
            <button 
              type="button" 
              class="btn btn-warning" 
              @click="sendBatchReminders"
              :disabled="actionLoading"
            >
              <span v-if="actionLoading" class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
              确认发送
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import borrowService from '@/api/borrow'
import { ElMessage } from 'element-plus'
import { Modal } from 'bootstrap'

// 路由
const router = useRouter()

// 状态管理
const loading = ref(false)
const actionLoading = ref(false)
const currentTab = ref('overdue')
const overdueBorrows = ref([])
const expiringBorrows = ref([])
const selectedRecords = ref([])
const selectAllOverdue = ref(false)
const selectAllExpiring = ref(false)
const reminderMessage = ref('请注意您借用的装备即将到期或已逾期，请尽快归还，谢谢配合。')

// 批量提醒模态框实例
let batchReminderModalInstance = null

// 计算属性
const todayExpiring = computed(() => {
  return expiringBorrows.value.filter(item => 
    calculateRemainingDays(item.expectedReturnDate) === 0
  ).length
})

const upcomingExpiring = computed(() => {
  return expiringBorrows.value.filter(item => 
    calculateRemainingDays(item.expectedReturnDate) > 0
  ).length
})

const reminderSent = computed(() => {
  return [...overdueBorrows.value, ...expiringBorrows.value].filter(item => 
    item.reminderSent
  ).length
})

// 加载数据
const loadData = async () => {
  try {
    loading.value = true
    
    // 获取逾期借用记录
    const overdueResponse = await borrowService.getOverdueBorrows()
    overdueBorrows.value = overdueResponse.data || []
    
    // 获取即将到期的借用记录（3天内）
    const expiringResponse = await borrowService.getExpiringBorrows(3)
    expiringBorrows.value = expiringResponse.data || []
    
    // 重置选择状态
    selectedRecords.value = []
    selectAllOverdue.value = false
    selectAllExpiring.value = false
  } catch (error) {
    console.error('加载数据失败:', error)
    ElMessage.error('加载数据失败: ' + (error.message || '服务器错误'))
  } finally {
    loading.value = false
  }
}

// 刷新数据
const refreshData = () => {
  loadData()
  ElMessage.success('数据已刷新')
}

// 打开批量提醒确认模态框
const confirmSendBatchReminders = () => {
  if (selectedRecords.value.length === 0) {
    ElMessage.warning('请先选择记录')
    return
  }
  
  batchReminderModalInstance.show()
}

// 发送批量提醒
const sendBatchReminders = async () => {
  if (selectedRecords.value.length === 0) return
  
  actionLoading.value = true
  
  try {
    await borrowService.sendBatchOverdueReminders({
      ids: selectedRecords.value,
      message: reminderMessage.value
    })
    
    ElMessage.success(`已成功发送 ${selectedRecords.value.length} 条提醒`)
    batchReminderModalInstance.hide()
    await loadData() // 刷新数据
  } catch (error) {
    console.error('发送批量提醒失败:', error)
    ElMessage.error('发送批量提醒失败: ' + (error.message || '服务器错误'))
  } finally {
    actionLoading.value = false
  }
}

// 发送单个提醒
const sendReminder = async (id) => {
  try {
    await borrowService.sendOverdueReminder(id, reminderMessage.value)
    ElMessage.success('提醒已发送')
    await loadData() // 刷新数据
  } catch (error) {
    console.error('发送提醒失败:', error)
    ElMessage.error('发送提醒失败: ' + (error.message || '服务器错误'))
  }
}

// 查看详情
const viewDetail = (id) => {
  router.push(`/borrow/${id}`)
}

// 全选/取消全选
const toggleSelectAll = (type) => {
  if (type === 'overdue') {
    if (selectAllOverdue.value) {
      // 全选
      overdueBorrows.value.forEach(item => {
        if (!selectedRecords.value.includes(item.id)) {
          selectedRecords.value.push(item.id)
        }
      })
    } else {
      // 取消全选
      selectedRecords.value = selectedRecords.value.filter(id => 
        !overdueBorrows.value.some(item => item.id === id)
      )
    }
  } else if (type === 'expiring') {
    if (selectAllExpiring.value) {
      // 全选
      expiringBorrows.value.forEach(item => {
        if (!selectedRecords.value.includes(item.id)) {
          selectedRecords.value.push(item.id)
        }
      })
    } else {
      // 取消全选
      selectedRecords.value = selectedRecords.value.filter(id => 
        !expiringBorrows.value.some(item => item.id === id)
      )
    }
  }
}

// 计算逾期天数
const calculateOverdueDays = (expectedReturnDate) => {
  if (!expectedReturnDate) return 0
  
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  
  const returnDate = new Date(expectedReturnDate)
  returnDate.setHours(0, 0, 0, 0)
  
  const diffTime = today - returnDate
  const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24))
  
  return diffDays > 0 ? diffDays : 0
}

// 计算剩余天数
const calculateRemainingDays = (expectedReturnDate) => {
  if (!expectedReturnDate) return 0
  
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  
  const returnDate = new Date(expectedReturnDate)
  returnDate.setHours(0, 0, 0, 0)
  
  const diffTime = returnDate - today
  const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24))
  
  return diffDays >= 0 ? diffDays : 0
}

// 格式化日期
const formatDate = (dateString) => {
  if (!dateString) return '-'
  const date = new Date(dateString)
  return date.toLocaleDateString('zh-CN')
}

// 生命周期钩子
onMounted(() => {
  // 初始化模态框
  batchReminderModalInstance = new Modal(document.getElementById('batchReminderModal'))
  
  // 加载数据
  loadData()
})
</script>

<style scoped>
.card {
  border-radius: 0.75rem;
  transition: transform 0.2s, box-shadow 0.2s;
}

.card:hover {
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.1) !important;
}

.table th {
  font-weight: 600;
  font-size: 0.8rem;
  text-transform: uppercase;
}

.table td {
  vertical-align: middle;
  padding: 0.75rem;
}

.rounded-circle {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.nav-tabs .nav-link {
  color: #6c757d;
  border: none;
  padding: 0.75rem 1.25rem;
  font-weight: 500;
}

.nav-tabs .nav-link.active {
  color: #0d6efd;
  border-bottom: 2px solid #0d6efd;
}

.badge {
  font-weight: normal;
  padding: 0.5em 0.75em;
}
</style> 