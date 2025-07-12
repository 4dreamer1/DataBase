<template>
  <div class="container-fluid py-4">
    <div class="row mb-4">
      <div class="col-12">
        <h2 class="mb-3 fw-bold">我的借用</h2>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="row mb-4">
      <div class="col-xl-3 col-md-6 mb-4">
        <div class="card border-0 shadow-sm h-100">
          <div class="card-body d-flex align-items-center">
            <div class="rounded-circle p-3 bg-primary bg-opacity-10 me-3">
              <i class="bi bi-hourglass-split text-primary fs-4"></i>
            </div>
            <div>
              <h6 class="text-muted mb-1">待审批</h6>
              <h3 class="mb-0 fw-bold">{{ borrowStats.pending }}</h3>
            </div>
          </div>
        </div>
      </div>
      
      <div class="col-xl-3 col-md-6 mb-4">
        <div class="card border-0 shadow-sm h-100">
          <div class="card-body d-flex align-items-center">
            <div class="rounded-circle p-3 bg-success bg-opacity-10 me-3">
              <i class="bi bi-box-arrow-right text-success fs-4"></i>
            </div>
            <div>
              <h6 class="text-muted mb-1">已借出</h6>
              <h3 class="mb-0 fw-bold">{{ borrowStats.borrowed }}</h3>
            </div>
          </div>
        </div>
      </div>
      
      <div class="col-xl-3 col-md-6 mb-4">
        <div class="card border-0 shadow-sm h-100">
          <div class="card-body d-flex align-items-center">
            <div class="rounded-circle p-3 bg-danger bg-opacity-10 me-3">
              <i class="bi bi-exclamation-triangle text-danger fs-4"></i>
            </div>
            <div>
              <h6 class="text-muted mb-1">已逾期</h6>
              <h3 class="mb-0 fw-bold">{{ borrowStats.overdue }}</h3>
            </div>
          </div>
        </div>
      </div>
      
      <div class="col-xl-3 col-md-6 mb-4">
        <div class="card border-0 shadow-sm h-100">
          <div class="card-body d-flex align-items-center">
            <div class="rounded-circle p-3 bg-info bg-opacity-10 me-3">
              <i class="bi bi-check-circle text-info fs-4"></i>
            </div>
            <div>
              <h6 class="text-muted mb-1">已归还</h6>
              <h3 class="mb-0 fw-bold">{{ borrowStats.returned }}</h3>
            </div>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 状态过滤标签 -->
    <div class="row mb-4">
      <div class="col-12">
        <div class="btn-group" role="group">
          <input 
            type="radio" 
            class="btn-check" 
            name="statusFilter" 
            id="all" 
            value="" 
            v-model="statusFilter"
            @change="handleStatusChange"
          >
          <label class="btn btn-outline-primary" for="all">全部</label>
          
          <input 
            type="radio" 
            class="btn-check" 
            name="statusFilter" 
            id="pending" 
            value="PENDING" 
            v-model="statusFilter"
            @change="handleStatusChange"
          >
          <label class="btn btn-outline-primary" for="pending">待审批</label>
          
          <input 
            type="radio" 
            class="btn-check" 
            name="statusFilter" 
            id="active" 
            value="ACTIVE" 
            v-model="statusFilter"
            @change="handleStatusChange"
          >
          <label class="btn btn-outline-primary" for="active">使用中</label>
          
          <input 
            type="radio" 
            class="btn-check" 
            name="statusFilter" 
            id="returned" 
            value="RETURNED" 
            v-model="statusFilter"
            @change="handleStatusChange"
          >
          <label class="btn btn-outline-primary" for="returned">已归还</label>
          
          <input 
            type="radio" 
            class="btn-check" 
            name="statusFilter" 
            id="rejected" 
            value="REJECTED" 
            v-model="statusFilter"
            @change="handleStatusChange"
          >
          <label class="btn btn-outline-primary" for="rejected">已拒绝</label>
        </div>
      </div>
    </div>
    
    <!-- 借用列表 -->
    <div class="row">
      <div class="col-12">
        <div class="card border-0 shadow-sm">
          <div class="card-body p-0">
            <div class="table-responsive">
              <table class="table table-hover align-middle mb-0">
                <thead class="table-light">
                  <tr>
                    <th class="border-0">装备名称</th>
                    <th class="border-0">借用日期</th>
                    <th class="border-0">预计归还日期</th>
                    <th class="border-0">状态</th>
                    <th class="border-0">已借天数</th>
                    <th class="border-0">操作</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="item in myBorrows" :key="item.id">
                    <td>
                      <div class="d-flex align-items-center">
                        <div class="rounded-circle bg-light p-2 me-2">
                          <i class="bi bi-box-seam text-primary"></i>
                        </div>
                        <div>
                          <div class="fw-bold">{{ item.equipment.name }}</div>
                          <div class="small text-muted">{{ item.equipment.code }}</div>
                        </div>
                      </div>
                    </td>
                    <td>{{ formatDate(item.borrowDate) }}</td>
                    <td>{{ formatDate(item.expectedReturnDate) }}</td>
                    <td>
                      <span 
                        class="badge"
                        :class="{
                          'bg-warning': item.status === 'PENDING',
                          'bg-success': item.status === 'APPROVED' || item.status === 'BORROWED',
                          'bg-danger': item.status === 'REJECTED' || item.status === 'OVERDUE',
                          'bg-info': item.status === 'RETURNED'
                        }"
                      >
                        {{ getBorrowStatusLabel(item.status) }}
                      </span>
                    </td>
                    <td>
                      <div v-if="['BORROWED', 'APPROVED'].includes(item.status)">
                        {{ calculateDays(item.borrowDate) }}天
                        <span 
                          v-if="isOverdue(item.expectedReturnDate)" 
                          class="badge bg-danger ms-1"
                        >
                          已逾期{{ calculateOverdueDays(item.expectedReturnDate) }}天
                        </span>
                      </div>
                      <div v-else-if="item.status === 'RETURNED'">
                        {{ calculateDaysBetween(item.borrowDate, item.actualReturnDate) }}天
                      </div>
                      <div v-else>-</div>
                    </td>
                    <td>
                      <div class="btn-group btn-group-sm">
                        <button 
                          class="btn btn-outline-primary" 
                          title="查看详情"
                          @click="viewDetail(item.id)"
                        >
                          <i class="bi bi-eye"></i>
                        </button>
                        <button 
                          v-if="canReturn(item)"
                          class="btn btn-outline-success" 
                          title="归还"
                          @click="handleReturn(item)"
                        >
                          <i class="bi bi-box-arrow-in-left"></i>
                        </button>
                        <button 
                          v-if="canCancel(item)"
                          class="btn btn-outline-danger" 
                          title="取消借用"
                          @click="handleCancel(item)"
                        >
                          <i class="bi bi-x-lg"></i>
                        </button>
                      </div>
                    </td>
                  </tr>
                  <tr v-if="loading">
                    <td colspan="6" class="text-center py-5">
                      <div class="spinner-border text-primary" role="status">
                        <span class="visually-hidden">加载中...</span>
                      </div>
                      <div class="mt-2 text-muted">正在加载数据...</div>
                    </td>
                  </tr>
                  <tr v-else-if="myBorrows.length === 0">
                    <td colspan="6" class="text-center py-5">
                      <div class="text-muted">
                        <i class="bi bi-inbox fs-4 d-block mb-2"></i>
                        暂无借用记录
                      </div>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
          <!-- 分页 -->
          <div v-if="myBorrows.length > 0" class="card-footer bg-transparent py-3">
            <div class="row align-items-center">
              <div class="col-auto">
                <select class="form-select form-select-sm" v-model="pageSize" @change="handlePageSizeChange">
                  <option :value="10">10条/页</option>
                  <option :value="20">20条/页</option>
                  <option :value="50">50条/页</option>
                </select>
              </div>
              <div class="col d-flex justify-content-end">
                <nav aria-label="Page navigation">
                  <ul class="pagination pagination-sm mb-0">
                    <li class="page-item" :class="{ disabled: currentPage === 1 }">
                      <a class="page-link" href="#" @click.prevent="goToPage(currentPage - 1)">
                        <i class="bi bi-chevron-left"></i>
                      </a>
                    </li>
                    <li v-for="page in displayPages" :key="page" class="page-item" :class="{ active: currentPage === page }">
                      <a class="page-link" href="#" @click.prevent="goToPage(page)">{{ page }}</a>
                    </li>
                    <li class="page-item" :class="{ disabled: currentPage === totalPages }">
                      <a class="page-link" href="#" @click.prevent="goToPage(currentPage + 1)">
                        <i class="bi bi-chevron-right"></i>
                      </a>
                    </li>
                  </ul>
                </nav>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 归还确认模态框 -->
    <div class="modal fade" id="returnModal" tabindex="-1" aria-hidden="true">
      <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content">
          <div class="modal-header">
            <h5 class="modal-title">确认归还</h5>
            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
          </div>
          <div class="modal-body">
            <p>确定要归还装备 <strong>{{ selectedBorrow?.equipment?.name }}</strong> 吗？</p>
            
            <div class="mb-3">
              <label for="returnNote" class="form-label">归还备注</label>
              <textarea 
                class="form-control" 
                id="returnNote" 
                rows="2" 
                v-model="returnForm.note"
              ></textarea>
            </div>
            
            <div class="mb-3">
              <label for="equipmentCondition" class="form-label">装备状态</label>
              <select class="form-select" id="equipmentCondition" v-model="returnForm.condition">
                <option value="GOOD">良好</option>
                <option value="DAMAGED">轻微损坏</option>
                <option value="BROKEN">严重损坏</option>
              </select>
            </div>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">取消</button>
            <button type="button" class="btn btn-success" @click="submitReturn" :disabled="returnLoading">
              <span v-if="returnLoading" class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
              {{ returnLoading ? '处理中...' : '确认归还' }}
            </button>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 取消确认模态框 -->
    <div class="modal fade" id="cancelModal" tabindex="-1" aria-hidden="true">
      <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content">
          <div class="modal-header">
            <h5 class="modal-title">确认取消</h5>
            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
          </div>
          <div class="modal-body">
            <p>确定要取消借用装备 <strong>{{ selectedBorrow?.equipment?.name }}</strong> 吗？此操作不可撤销。</p>
            
            <div class="mb-3">
              <label for="cancelReason" class="form-label">取消原因</label>
              <textarea 
                class="form-control" 
                id="cancelReason" 
                rows="2" 
                v-model="cancelForm.reason"
              ></textarea>
            </div>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">关闭</button>
            <button type="button" class="btn btn-danger" @click="submitCancel" :disabled="cancelLoading">
              <span v-if="cancelLoading" class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
              {{ cancelLoading ? '处理中...' : '确认取消' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Modal } from 'bootstrap'
import borrowService from '@/api/borrow'
import { ElMessage } from 'element-plus'

const router = useRouter()

// 模态框实例
let returnModalInstance = null
let cancelModalInstance = null

// 列表数据
const myBorrows = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const totalItems = ref(0)
const totalPages = computed(() => Math.ceil(totalItems.value / pageSize.value))

// 借用统计
const borrowStats = reactive({
  pending: 0,
  borrowed: 0,
  overdue: 0,
  returned: 0
})

// 状态过滤
const statusFilter = ref('')

// 分页展示逻辑
const displayPages = computed(() => {
  const range = []
  const maxPages = 5
  const halfMax = Math.floor(maxPages / 2)
  
  let start = Math.max(1, currentPage.value - halfMax)
  let end = Math.min(totalPages.value, start + maxPages - 1)
  
  if (end - start + 1 < maxPages) {
    start = Math.max(1, end - maxPages + 1)
  }
  
  for (let i = start; i <= end; i++) {
    range.push(i)
  }
  
  return range
})

// 归还和取消
const selectedBorrow = ref(null)
const returnForm = reactive({
  note: '',
  condition: 'GOOD'
})
const returnLoading = ref(false)
const cancelForm = reactive({
  reason: ''
})
const cancelLoading = ref(false)

// 初始化
onMounted(async () => {
  // 初始化模态框
  const returnModalEl = document.getElementById('returnModal')
  const cancelModalEl = document.getElementById('cancelModal')
  
  if (returnModalEl) returnModalInstance = new Modal(returnModalEl)
  if (cancelModalEl) cancelModalInstance = new Modal(cancelModalEl)
  
  // 获取借用数据
  await fetchMyBorrows()
  calculateBorrowStats()
})

// 获取我的借用记录
const fetchMyBorrows = async () => {
  loading.value = true
  
  try {
    const params = {
      page: currentPage.value - 1,
      size: pageSize.value,
      status: statusFilter.value
    }
    
    const response = await borrowService.getMyBorrows(params)
    // 安全处理响应数据
    if (response && response.data && response.data.content) {
      myBorrows.value = response.data.content || [];
      totalItems.value = response.data.totalElements || 0;
    } else if (response && Array.isArray(response.data)) {
      myBorrows.value = response.data;
      totalItems.value = response.data.length;
    } else if (Array.isArray(response)) {
      myBorrows.value = response;
      totalItems.value = response.length;
    } else {
      myBorrows.value = [];
      totalItems.value = 0;
      console.warn('未获取到有效的借用记录数据或数据格式不符合预期:', response);
      
      // 显示友好的提示信息
      if (!response) {
        ElMessage.warning('暂无借用记录数据');
      }
    }
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
    ElMessage.error(`获取借用记录失败: ${errorMsg}`);
    
    myBorrows.value = [];
    totalItems.value = 0;
  } finally {
    loading.value = false;
  }
}

// 计算借用统计
const calculateBorrowStats = async () => {
  try {
    const response = await borrowService.getMyBorrowStats()
    const stats = response.data || {}
    
    borrowStats.pending = stats.pending || 0
    borrowStats.borrowed = stats.active || 0
    borrowStats.overdue = stats.overdue || 0
    borrowStats.returned = stats.returned || 0
  } catch (error) {
    console.error('获取借用统计失败', error)
  }
}

// 状态过滤变更
const handleStatusChange = () => {
  currentPage.value = 1
  fetchMyBorrows()
}

// 分页处理
const goToPage = (page) => {
  if (page < 1 || page > totalPages.value) return
  currentPage.value = page
  fetchMyBorrows()
}

const handlePageSizeChange = () => {
  currentPage.value = 1
  fetchMyBorrows()
}

// 查看详情
const viewDetail = (id) => {
  router.push(`/borrow/${id}`)
}

// 归还装备
const handleReturn = (borrow) => {
  selectedBorrow.value = borrow
  returnForm.note = ''
  returnForm.condition = 'GOOD'
  returnModalInstance.show()
}

// 提交归还
const submitReturn = async () => {
  if (!selectedBorrow.value) return
  
  returnLoading.value = true
  
  try {
    await borrowService.returnEquipment(selectedBorrow.value.id, {
      note: returnForm.note,
      condition: returnForm.condition
    })
    
    // 关闭模态框
    returnModalInstance.hide()
    
    // 刷新列表
    await fetchMyBorrows()
    calculateBorrowStats()
    
    // 显示成功消息
    alert('装备已成功归还')
  } catch (error) {
    console.error('归还失败', error)
    alert('归还失败：' + (error.response?.data?.message || '请稍后再试'))
  } finally {
    returnLoading.value = false
  }
}

// 取消借用
const handleCancel = (borrow) => {
  selectedBorrow.value = borrow
  cancelForm.reason = ''
  cancelModalInstance.show()
}

// 提交取消
const submitCancel = async () => {
  if (!selectedBorrow.value) return
  
  cancelLoading.value = true
  
  try {
    await borrowService.cancelBorrow(selectedBorrow.value.id, {
      reason: cancelForm.reason
    })
    
    // 关闭模态框
    cancelModalInstance.hide()
    
    // 刷新列表
    await fetchMyBorrows()
    calculateBorrowStats()
    
    // 显示成功消息
    alert('借用已成功取消')
  } catch (error) {
    console.error('取消失败', error)
    alert('取消失败：' + (error.response?.data?.message || '请稍后再试'))
  } finally {
    cancelLoading.value = false
  }
}

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
    'BORROWED': '已借出',
    'RETURNED': '已归还',
    'OVERDUE': '已逾期'
  }
  return statusMap[status] || '未知'
}

// 计算借用天数
const calculateDays = (startDate) => {
  if (!startDate) return 0
  
  const start = new Date(startDate)
  const now = new Date()
  const diffTime = Math.abs(now - start)
  const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24))
  
  return diffDays
}

// 计算两个日期之间的天数
const calculateDaysBetween = (startDate, endDate) => {
  if (!startDate || !endDate) return 0
  
  const start = new Date(startDate)
  const end = new Date(endDate)
  const diffTime = Math.abs(end - start)
  const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24))
  
  return diffDays
}

// 检查是否逾期
const isOverdue = (expectedReturnDate) => {
  if (!expectedReturnDate) return false
  
  const expected = new Date(expectedReturnDate)
  const now = new Date()
  
  return now > expected
}

// 计算逾期天数
const calculateOverdueDays = (expectedReturnDate) => {
  if (!expectedReturnDate) return 0
  
  const expected = new Date(expectedReturnDate)
  const now = new Date()
  
  if (now <= expected) return 0
  
  const diffTime = Math.abs(now - expected)
  const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24))
  
  return diffDays
}

// 判断是否可以归还
const canReturn = (borrow) => {
  return borrow.status === 'BORROWED' || borrow.status === 'APPROVED'
}

// 判断是否可以取消
const canCancel = (borrow) => {
  return borrow.status === 'PENDING'
}
</script>

<style scoped>
.table th {
  font-weight: 600;
  font-size: 0.8rem;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.card {
  border-radius: 0.75rem;
  transition: transform 0.2s, box-shadow 0.2s;
}

.card:hover {
  transform: translateY(-5px);
  box-shadow: 0 10px 20px rgba(0, 0, 0, 0.05) !important;
}

.rounded-circle {
  width: 60px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 渐变图标背景 */
.bg-primary.bg-opacity-10 {
  background: linear-gradient(135deg, rgba(41, 121, 255, 0.1) 0%, rgba(30, 136, 229, 0.1) 100%) !important;
}

.bg-success.bg-opacity-10 {
  background: linear-gradient(135deg, rgba(46, 204, 113, 0.1) 0%, rgba(39, 174, 96, 0.1) 100%) !important;
}

.bg-danger.bg-opacity-10 {
  background: linear-gradient(135deg, rgba(220, 53, 69, 0.1) 0%, rgba(200, 35, 51, 0.1) 100%) !important;
}

.bg-info.bg-opacity-10 {
  background: linear-gradient(135deg, rgba(3, 169, 244, 0.1) 0%, rgba(0, 140, 208, 0.1) 100%) !important;
}
</style> 