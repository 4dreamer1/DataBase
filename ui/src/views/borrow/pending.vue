<template>
  <div class="container-fluid py-4">
    <div class="row mb-4">
      <div class="col-12">
        <h2 class="mb-3 fw-bold">待审批借用</h2>
      </div>
    </div>
    
    <!-- 过滤和搜索 -->
    <div class="row mb-4">
      <div class="col-12">
        <div class="card border-0 shadow-sm">
          <div class="card-body">
            <div class="row g-3">
              <div class="col-lg-4 col-md-6">
                <div class="form-floating">
                  <input 
                    type="text" 
                    class="form-control" 
                    id="searchQuery" 
                    v-model="searchQuery"
                    placeholder="搜索装备名称或借用人"
                  >
                  <label for="searchQuery">搜索装备名称或借用人</label>
                </div>
              </div>
              
              <div class="col-lg-4 col-md-6">
                <div class="form-floating">
                  <div class="input-group">
                    <input 
                      type="date" 
                      class="form-control" 
                      id="startDate" 
                      v-model="dateRange.start"
                      placeholder="开始日期"
                    >
                    <span class="input-group-text bg-light">至</span>
                    <input 
                      type="date" 
                      class="form-control" 
                      id="endDate" 
                      v-model="dateRange.end"
                      placeholder="结束日期"
                    >
                  </div>
                </div>
              </div>
              
              <div class="col-lg-4 col-md-12 d-flex align-items-center justify-content-end">
                <button class="btn btn-outline-secondary me-2" @click="resetFilters">
                  <i class="bi bi-arrow-clockwise me-1"></i> 重置
                </button>
                <button class="btn btn-primary" @click="fetchPendingBorrows">
                  <i class="bi bi-search me-1"></i> 搜索
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 批量操作 -->
    <div class="row mb-3">
      <div class="col-12">
        <div class="card border-0 shadow-sm">
          <div class="card-body d-flex justify-content-between align-items-center">
            <div>
              <span class="me-2">已选择 {{ selectedBorrows.length }} 项</span>
              <button 
                class="btn btn-success me-2" 
                :disabled="selectedBorrows.length === 0"
                @click="showBatchApprovalModal"
              >
                <i class="bi bi-check-lg me-1"></i> 批量批准
              </button>
              <button 
                class="btn btn-danger" 
                :disabled="selectedBorrows.length === 0"
                @click="showBatchRejectModal"
              >
                <i class="bi bi-x-lg me-1"></i> 批量拒绝
              </button>
            </div>
            <div class="badge bg-primary fs-6">
              <i class="bi bi-hourglass-split me-1"></i> 待审批: {{ totalItems }}
            </div>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 数据表格 -->
    <div class="row">
      <div class="col-12">
        <div class="card border-0 shadow-sm">
          <div class="card-body p-0">
            <div class="table-responsive">
              <table class="table table-hover align-middle mb-0">
                <thead class="table-light">
                  <tr>
                    <th class="border-0">
                      <div class="form-check">
                        <input 
                          class="form-check-input" 
                          type="checkbox" 
                          id="selectAll"
                          :checked="isAllSelected"
                          @change="toggleSelectAll"
                        >
                        <label class="form-check-label" for="selectAll"></label>
                      </div>
                    </th>
                    <th class="border-0">装备名称</th>
                    <th class="border-0">借用人</th>
                    <th class="border-0">申请日期</th>
                    <th class="border-0">借用日期</th>
                    <th class="border-0">预计归还日期</th>
                    <th class="border-0">借用原因</th>
                    <th class="border-0">操作</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="item in pendingBorrows" :key="item.id">
                    <td>
                      <div class="form-check">
                        <input 
                          class="form-check-input" 
                          type="checkbox" 
                          :id="`item-${item.id}`" 
                          :value="item.id"
                          v-model="selectedBorrows"
                        >
                        <label class="form-check-label" :for="`item-${item.id}`"></label>
                      </div>
                    </td>
                    <td>
                      <div class="d-flex align-items-center">
                        <div class="rounded-circle bg-light p-2 me-2">
                          <i class="bi bi-box-seam text-primary"></i>
                        </div>
                        <div>
                          <div class="fw-bold">{{ item.equipment?.name || '未知装备' }}</div>
                          <div class="small text-muted">{{ item.equipment?.serialNumber || item.equipment?.code || '无编码' }}</div>
                        </div>
                      </div>
                    </td>
                    <td>
                      <div>{{ item.borrower?.name || item.userName || '未知用户' }}</div>
                      <div class="small text-muted">{{ item.borrower?.email || item.userEmail || '' }}</div>
                    </td>
                    <td>{{ formatDate(item.createdAt || item.applicationDate) }}</td>
                    <td>{{ formatDate(item.borrowDate) }}</td>
                    <td>{{ formatDate(item.expectedReturnDate) }}</td>
                    <td>
                      <div class="text-truncate" style="max-width: 200px;" :title="item.reason || item.purpose || ''">
                        {{ item.reason || item.purpose || '未提供原因' }}
                      </div>
                    </td>
                    <td>
                      <div class="btn-group btn-group-sm">
                        <button 
                          class="btn btn-outline-success" 
                          title="批准"
                          @click="handleApprove(item)"
                        >
                          <i class="bi bi-check-lg"></i>
                        </button>
                        <button 
                          class="btn btn-outline-danger" 
                          title="拒绝"
                          @click="handleReject(item)"
                        >
                          <i class="bi bi-x-lg"></i>
                        </button>
                        <button 
                          class="btn btn-outline-primary" 
                          title="查看详情"
                          @click="viewDetail(item.id)"
                        >
                          <i class="bi bi-eye"></i>
                        </button>
                      </div>
                    </td>
                  </tr>
                  <tr v-if="loading">
                    <td colspan="8" class="text-center py-5">
                      <div class="spinner-border text-primary" role="status">
                        <span class="visually-hidden">加载中...</span>
                      </div>
                      <div class="mt-2 text-muted">正在加载数据...</div>
                    </td>
                  </tr>
                  <tr v-else-if="pendingBorrows.length === 0">
                    <td colspan="8" class="text-center py-5">
                      <div class="text-muted">
                        <i class="bi bi-inbox fs-4 d-block mb-2"></i>
                        暂无待审批记录
                      </div>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
          <!-- 分页 -->
          <div v-if="pendingBorrows.length > 0" class="card-footer bg-transparent py-3">
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
    
    <!-- 批准模态框 -->
    <div class="modal fade" id="approveModal" tabindex="-1" aria-hidden="true">
      <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content">
          <div class="modal-header">
            <h5 class="modal-title">批准借用</h5>
            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
          </div>
          <div class="modal-body">
            <p>确定要批准 <strong>{{ selectedBorrow?.borrower?.name }}</strong> 借用装备 <strong>{{ selectedBorrow?.equipment?.name }}</strong> 吗？</p>
            
            <div class="mb-3">
              <label for="approveNote" class="form-label">批准备注</label>
              <textarea 
                class="form-control" 
                id="approveNote" 
                rows="2" 
                v-model="approveForm.note"
              ></textarea>
            </div>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">取消</button>
            <button type="button" class="btn btn-success" @click="submitApprove" :disabled="approveLoading">
              <span v-if="approveLoading" class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
              {{ approveLoading ? '处理中...' : '确认批准' }}
            </button>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 批量批准模态框 -->
    <div class="modal fade" id="batchApproveModal" tabindex="-1" aria-hidden="true">
      <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content">
          <div class="modal-header">
            <h5 class="modal-title">批量批准借用</h5>
            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
          </div>
          <div class="modal-body">
            <p>确定要批准选中的 <strong>{{ selectedBorrows.length }}</strong> 条借用申请吗？</p>
            
            <div class="mb-3">
              <label for="batchApproveNote" class="form-label">批准备注</label>
              <textarea 
                class="form-control" 
                id="batchApproveNote" 
                rows="2" 
                v-model="batchApproveForm.note"
              ></textarea>
            </div>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">取消</button>
            <button type="button" class="btn btn-success" @click="submitBatchApprove" :disabled="batchApproveLoading">
              <span v-if="batchApproveLoading" class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
              {{ batchApproveLoading ? '处理中...' : '确认批准' }}
            </button>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 拒绝模态框 -->
    <div class="modal fade" id="rejectModal" tabindex="-1" aria-hidden="true">
      <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content">
          <div class="modal-header">
            <h5 class="modal-title">拒绝借用</h5>
            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
          </div>
          <div class="modal-body">
            <p>确定要拒绝 <strong>{{ selectedBorrow?.borrower?.name }}</strong> 借用装备 <strong>{{ selectedBorrow?.equipment?.name }}</strong> 吗？</p>
            
            <div class="mb-3">
              <label for="rejectReason" class="form-label">拒绝原因</label>
              <textarea 
                class="form-control" 
                id="rejectReason" 
                rows="2" 
                v-model="rejectForm.reason"
                :class="{'is-invalid': rejectFormError}"
              ></textarea>
              <div class="invalid-feedback" v-if="rejectFormError">
                {{ rejectFormError }}
              </div>
            </div>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">取消</button>
            <button type="button" class="btn btn-danger" @click="submitReject" :disabled="rejectLoading">
              <span v-if="rejectLoading" class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
              {{ rejectLoading ? '处理中...' : '确认拒绝' }}
            </button>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 批量拒绝模态框 -->
    <div class="modal fade" id="batchRejectModal" tabindex="-1" aria-hidden="true">
      <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content">
          <div class="modal-header">
            <h5 class="modal-title">批量拒绝借用</h5>
            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
          </div>
          <div class="modal-body">
            <p>确定要拒绝选中的 <strong>{{ selectedBorrows.length }}</strong> 条借用申请吗？</p>
            
            <div class="mb-3">
              <label for="batchRejectReason" class="form-label">拒绝原因</label>
              <textarea 
                class="form-control" 
                id="batchRejectReason" 
                rows="2" 
                v-model="batchRejectForm.reason"
                :class="{'is-invalid': batchRejectFormError}"
              ></textarea>
              <div class="invalid-feedback" v-if="batchRejectFormError">
                {{ batchRejectFormError }}
              </div>
            </div>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">取消</button>
            <button type="button" class="btn btn-danger" @click="submitBatchReject" :disabled="batchRejectLoading">
              <span v-if="batchRejectLoading" class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
              {{ batchRejectLoading ? '处理中...' : '确认拒绝' }}
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
import { useBorrowStore } from '@/store/modules/borrow'

const router = useRouter()
const borrowStore = useBorrowStore()

// 模态框实例
let approveModalInstance = null
let rejectModalInstance = null
let batchApproveModalInstance = null
let batchRejectModalInstance = null

// 列表数据
const pendingBorrows = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const totalItems = ref(0)
const totalPages = computed(() => Math.ceil(totalItems.value / pageSize.value))

// 搜索和筛选
const searchQuery = ref('')
const dateRange = reactive({
  start: '',
  end: ''
})

// 选择项
const selectedBorrows = ref([])
const isAllSelected = computed(() => {
  return pendingBorrows.value.length > 0 && selectedBorrows.value.length === pendingBorrows.value.length
})

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

// 审批
const selectedBorrow = ref(null)
const approveForm = reactive({
  note: ''
})
const approveLoading = ref(false)
const rejectForm = reactive({
  reason: ''
})
const rejectFormError = ref('')
const rejectLoading = ref(false)

// 批量审批
const batchApproveForm = reactive({
  note: ''
})
const batchApproveLoading = ref(false)
const batchRejectForm = reactive({
  reason: ''
})
const batchRejectFormError = ref('')
const batchRejectLoading = ref(false)

// 初始化
onMounted(async () => {
  // 初始化模态框实例
  initModalInstances()
  
  // 加载待审批数据
  await fetchPendingBorrows()
})

// 获取待审批记录
const fetchPendingBorrows = async () => {
  loading.value = true
  selectedBorrows.value = []
  
  try {
    // 构建查询参数
    const params = {
      page: currentPage.value - 1,  // 后端分页通常从0开始
      size: pageSize.value,
      status: 'PENDING', // 确保只获取待审批的记录
      sort: 'createdAt,desc' // 按创建时间降序排序
    }
    
    // 添加搜索关键词
    if (searchQuery.value) {
      params.keyword = searchQuery.value
    }
    
    // 添加日期范围
    if (dateRange.start) {
      params.startDate = dateRange.start
    }
    
    if (dateRange.end) {
      params.endDate = dateRange.end
    }
    
    // 直接调用服务获取数据
    const response = await borrowService.getPendingBorrows(params)
    
    // 处理响应数据
    if (response && response.content) {
      pendingBorrows.value = response.content
      totalItems.value = response.totalElements || response.content.length
      
      // 更新store中的数据
      borrowStore.setPendingBorrows(pendingBorrows.value)
      borrowStore.setPendingCount(totalItems.value)
    } else {
      // 如果响应格式不符合预期，尝试直接使用响应
      pendingBorrows.value = Array.isArray(response) ? response : []
      totalItems.value = pendingBorrows.value.length
      
      // 更新store
      borrowStore.setPendingBorrows(pendingBorrows.value)
      borrowStore.setPendingCount(totalItems.value)
    }
    
    // 检查数据格式，确保必要的字段存在
    pendingBorrows.value = pendingBorrows.value.map(item => {
      return {
        ...item,
        equipment: item.equipment || {},
        borrower: item.borrower || {},
        createdAt: item.createdAt || item.applicationDate || new Date().toISOString(),
        borrowDate: item.borrowDate || item.createdAt || new Date().toISOString(),
        expectedReturnDate: item.expectedReturnDate || null,
        reason: item.reason || '未提供借用原因'
      }
    })
  } catch (error) {
    console.error('加载待审批数据失败', error)
  } finally {
    loading.value = false
  }
}

// 重置筛选条件
const resetFilters = () => {
  searchQuery.value = ''
  dateRange.start = ''
  dateRange.end = ''
  currentPage.value = 1
  fetchPendingBorrows()
}

// 分页处理
const goToPage = (page) => {
  if (page < 1 || page > totalPages.value) return
  currentPage.value = page
  fetchPendingBorrows()
}

const handlePageSizeChange = () => {
  currentPage.value = 1
  fetchPendingBorrows()
}

// 全选/取消全选
const toggleSelectAll = () => {
  if (isAllSelected.value) {
    selectedBorrows.value = []
  } else {
    selectedBorrows.value = pendingBorrows.value.map(item => item.id)
  }
}

// 查看详情
const viewDetail = (id) => {
  router.push(`/borrow/${id}`)
}

// 批准借用
const handleApprove = (borrow) => {
  selectedBorrow.value = borrow
  approveForm.note = ''
  approveModalInstance.show()
}

// 提交批准
const submitApprove = async () => {
  if (!selectedBorrow.value) return
  
  approveLoading.value = true
  
  try {
    await borrowService.approveBorrow(selectedBorrow.value.id, {
      note: approveForm.note
    })
    
    // 关闭模态框
    approveModalInstance.hide()
    
    // 刷新列表
    await fetchPendingBorrows()
  } catch (error) {
    console.error('批准失败', error)
  } finally {
    approveLoading.value = false
  }
}

// 拒绝借用
const handleReject = (borrow) => {
  selectedBorrow.value = borrow
  rejectForm.reason = ''
  rejectFormError.value = ''
  rejectModalInstance.show()
}

// 提交拒绝
const submitReject = async () => {
  if (!selectedBorrow.value) return
  
  // 验证
  if (!rejectForm.reason) {
    rejectFormError.value = '请输入拒绝原因'
    return
  }
  
  rejectLoading.value = true
  
  try {
    await borrowService.rejectBorrow(selectedBorrow.value.id, {
      reason: rejectForm.reason
    })
    
    // 关闭模态框
    rejectModalInstance.hide()
    
    // 刷新列表
    await fetchPendingBorrows()
  } catch (error) {
    console.error('拒绝失败', error)
  } finally {
    rejectLoading.value = false
  }
}

// 批量批准
const showBatchApprovalModal = () => {
  if (selectedBorrows.value.length === 0) return
  
  batchApproveForm.note = ''
  batchApproveModalInstance.show()
}

// 提交批量批准
const submitBatchApprove = async () => {
  if (selectedBorrows.value.length === 0) return
  
  batchApproveLoading.value = true
  
  try {
    await borrowService.batchApproveBorrows({
      ids: selectedBorrows.value,
      note: batchApproveForm.note
    })
    
    // 关闭模态框
    batchApproveModalInstance.hide()
    
    // 清空选中项
    selectedBorrows.value = []
    
    // 刷新列表
    await fetchPendingBorrows()
  } catch (error) {
    console.error('批量批准失败', error)
  } finally {
    batchApproveLoading.value = false
  }
}

// 批量拒绝
const showBatchRejectModal = () => {
  if (selectedBorrows.value.length === 0) return
  
  batchRejectForm.reason = ''
  batchRejectFormError.value = ''
  batchRejectModalInstance.show()
}

// 提交批量拒绝
const submitBatchReject = async () => {
  if (selectedBorrows.value.length === 0) return
  
  // 验证
  if (!batchRejectForm.reason) {
    batchRejectFormError.value = '请输入拒绝原因'
    return
  }
  
  batchRejectLoading.value = true
  
  try {
    await borrowService.batchRejectBorrows({
      ids: selectedBorrows.value,
      reason: batchRejectForm.reason
    })
    
    // 关闭模态框
    batchRejectModalInstance.hide()
    
    // 清空选中项
    selectedBorrows.value = []
    
    // 刷新列表
    await fetchPendingBorrows()
  } catch (error) {
    console.error('批量拒绝失败', error)
  } finally {
    batchRejectLoading.value = false
  }
}

// 格式化日期
const formatDate = (dateString) => {
  if (!dateString) return '-'
  
  try {
    // 尝试解析日期
    const date = new Date(dateString)
    
    // 检查是否是有效日期
    if (isNaN(date.getTime())) {
      return '-'
    }
    
    // 格式化为本地日期
    return date.toLocaleDateString('zh-CN', { 
      year: 'numeric', 
      month: '2-digit', 
      day: '2-digit' 
    })
  } catch (error) {
    console.warn('日期格式化错误:', dateString, error)
    return '-'
  }
}

// 初始化模态框实例
const initModalInstances = () => {
  const approveModalEl = document.getElementById('approveModal')
  const rejectModalEl = document.getElementById('rejectModal')
  const batchApproveModalEl = document.getElementById('batchApproveModal')
  const batchRejectModalEl = document.getElementById('batchRejectModal')
  
  if (approveModalEl) approveModalInstance = new Modal(approveModalEl)
  if (rejectModalEl) rejectModalInstance = new Modal(rejectModalEl)
  if (batchApproveModalEl) batchApproveModalInstance = new Modal(batchApproveModalEl)
  if (batchRejectModalEl) batchRejectModalInstance = new Modal(batchRejectModalEl)
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
}

/* 表单浮动标签样式调整 */
.form-floating > .form-control {
  height: calc(3.5rem + 2px);
  line-height: 1.25;
}

.form-floating > label {
  padding: 1rem 0.75rem;
}

/* 日期范围输入框修复 */
.input-group .form-control:not(:first-child) {
  border-top-left-radius: 0;
  border-bottom-left-radius: 0;
}

.input-group .form-control:not(:last-child) {
  border-top-right-radius: 0;
  border-bottom-right-radius: 0;
}

/* 文本省略 */
.text-truncate {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style> 