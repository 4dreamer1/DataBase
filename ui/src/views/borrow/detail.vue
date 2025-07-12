<template>
  <div class="container-fluid py-4">
    <!-- 页面标题和返回按钮 -->
    <div class="row mb-4">
      <div class="col-12">
        <div class="card border-0 shadow-sm">
          <div class="card-body d-flex justify-content-between align-items-center">
            <h5 class="m-0"><i class="bi bi-info-circle me-2"></i>借用详情</h5>
            <button class="btn btn-outline-secondary" @click="goBack">
              <i class="bi bi-arrow-left me-1"></i>返回
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 加载中和错误提示 -->
    <div v-if="loading" class="row mb-4">
      <div class="col-12 text-center py-5">
        <div class="spinner-border text-primary" role="status">
          <span class="visually-hidden">加载中...</span>
        </div>
        <div class="mt-3 text-muted">正在加载借用记录...</div>
      </div>
    </div>
    
    <div v-else-if="error" class="row mb-4">
      <div class="col-12">
        <div class="alert alert-danger" role="alert">
          <i class="bi bi-exclamation-triangle-fill me-2"></i>
          {{ error }}
        </div>
      </div>
    </div>

    <!-- 借用详情内容 -->
    <div v-else-if="borrowRecord" class="row">
      <!-- 左侧借用信息 -->
      <div class="col-lg-8 mb-4">
        <div class="card border-0 shadow-sm h-100">
          <div class="card-header bg-white border-bottom-0 py-3">
            <h5 class="mb-0"><i class="bi bi-clipboard-data me-2"></i>借用信息</h5>
          </div>
          <div class="card-body">
            <!-- 基本信息 -->
            <div class="row mb-4">
              <div class="col-md-6">
                <div class="border-start border-4 border-primary ps-3 mb-3">
                  <h6 class="text-muted mb-1">借用编号</h6>
                  <div class="fs-5">#{{ borrowRecord.id || '未知' }}</div>
                </div>
              </div>
              <div class="col-md-6">
                <div class="border-start border-4 border-primary ps-3 mb-3">
                  <h6 class="text-muted mb-1">当前状态</h6>
                  <div>
                    <span 
                      class="badge"
                      :class="{
                        'bg-warning': borrowRecord.status === 'PENDING',
                        'bg-success': borrowRecord.status === 'APPROVED' || borrowRecord.status === 'BORROWED',
                        'bg-danger': borrowRecord.status === 'REJECTED' || borrowRecord.status === 'OVERDUE',
                        'bg-info': borrowRecord.status === 'RETURNED',
                        'bg-secondary': !borrowRecord.status
                      }"
                    >
                      {{ getBorrowStatusLabel(borrowRecord.status) }}
                    </span>
                  </div>
                </div>
              </div>
            </div>

            <!-- 详细信息 -->
            <div class="row mb-4">
              <div class="col-md-6">
                <h6 class="fw-bold mb-3">时间信息</h6>
                <div class="mb-3">
                  <div class="text-muted mb-1">申请时间</div>
                  <div>{{ formatDateTime(borrowRecord.createdAt) }}</div>
                </div>
                <div class="mb-3">
                  <div class="text-muted mb-1">借用日期</div>
                  <div>{{ formatDate(borrowRecord.borrowDate) }}</div>
                </div>
                <div class="mb-3">
                  <div class="text-muted mb-1">预计归还日期</div>
                  <div>{{ formatDate(borrowRecord.expectedReturnDate) }}</div>
                </div>
                <div class="mb-3">
                  <div class="text-muted mb-1">实际归还日期</div>
                  <div>{{ borrowRecord.actualReturnDate ? formatDate(borrowRecord.actualReturnDate) : '尚未归还' }}</div>
                </div>
              </div>
              <div class="col-md-6">
                <h6 class="fw-bold mb-3">人员信息</h6>
                <div class="mb-3">
                  <div class="text-muted mb-1">借用人</div>
                  <div class="d-flex align-items-center">
                    <div class="rounded-circle bg-light p-2 me-2">
                      <i class="bi bi-person text-primary"></i>
                    </div>
                    <div>{{ borrowRecord.borrower?.name || '未知用户' }}</div>
                  </div>
                </div>
                <div class="mb-3">
                  <div class="text-muted mb-1">审批人</div>
                  <div class="d-flex align-items-center">
                    <div class="rounded-circle bg-light p-2 me-2">
                      <i class="bi bi-person-check text-success"></i>
                    </div>
                    <div>{{ borrowRecord.approver?.name || '尚未审批' }}</div>
                  </div>
                </div>
              </div>
            </div>

            <!-- 借用目的和备注 -->
            <div class="row">
              <div class="col-12 mb-3">
                <h6 class="fw-bold mb-2">借用目的</h6>
                <div class="p-3 bg-light rounded">
                  {{ borrowRecord.purpose || '无' }}
                </div>
              </div>
              <div class="col-12">
                <h6 class="fw-bold mb-2">备注信息</h6>
                <div class="p-3 bg-light rounded">
                  {{ borrowRecord.remarks || '无' }}
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧装备信息和操作按钮 -->
      <div class="col-lg-4 mb-4">
        <div class="card border-0 shadow-sm mb-4">
          <div class="card-header bg-white border-bottom-0 py-3">
            <h5 class="mb-0"><i class="bi bi-box-seam me-2"></i>装备信息</h5>
          </div>
          <div class="card-body">
            <div class="text-center mb-3">
              <div class="rounded-circle bg-light p-4 mx-auto mb-3" style="width: 100px; height: 100px; display: flex; align-items: center; justify-content: center;">
                <i class="bi bi-box-seam text-primary" style="font-size: 3rem;"></i>
              </div>
              <h5>{{ borrowRecord.equipment?.name || '未知装备' }}</h5>
              <div class="badge bg-light text-dark mb-3">
                {{ borrowRecord.equipment?.serialNumber || borrowRecord.equipment?.code || '无编号' }}
              </div>
            </div>
            
            <div class="mb-3">
              <div class="text-muted mb-1">借用数量</div>
              <div class="fs-5">{{ borrowRecord.quantity || 1 }}</div>
            </div>
            
            <div class="mb-3">
              <div class="text-muted mb-1">所属分类</div>
              <div>
                <span class="badge rounded-pill bg-info bg-opacity-10 text-info border border-info border-opacity-25">
                  {{ borrowRecord.equipment?.category?.name || '未分类' }}
                </span>
              </div>
            </div>
          </div>
        </div>

        <!-- 操作按钮 -->
        <div class="card border-0 shadow-sm">
          <div class="card-header bg-white border-bottom-0 py-3">
            <h5 class="mb-0"><i class="bi bi-gear me-2"></i>操作</h5>
          </div>
          <div class="card-body">
            <div class="d-grid gap-2">
              <button 
                v-if="canReturnBorrow" 
                class="btn btn-success" 
                @click="handleReturn"
              >
                <i class="bi bi-box-arrow-in-left me-2"></i>归还装备
              </button>
              
              <button 
                v-if="canApproveBorrow" 
                class="btn btn-primary" 
                @click="handleApprove"
              >
                <i class="bi bi-check-circle me-2"></i>批准申请
              </button>
              
              <button 
                v-if="canRejectBorrow" 
                class="btn btn-outline-danger" 
                @click="handleReject"
              >
                <i class="bi bi-x-circle me-2"></i>拒绝申请
              </button>
              
              <button 
                v-if="canCancelBorrow" 
                class="btn btn-outline-secondary" 
                @click="handleCancel"
              >
                <i class="bi bi-x me-2"></i>取消申请
              </button>
              
              <button 
                class="btn btn-outline-primary" 
                @click="printDetail"
              >
                <i class="bi bi-printer me-2"></i>打印详情
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 操作模态框 -->
    <!-- 归还确认模态框 -->
    <div class="modal fade" id="returnModal" tabindex="-1" aria-hidden="true">
      <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content">
          <div class="modal-header">
            <h5 class="modal-title">确认归还</h5>
            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
          </div>
          <div class="modal-body">
            <p>确定要归还装备 <strong>{{ borrowRecord?.equipment?.name }}</strong> 吗？</p>
            
            <div class="mb-3">
              <label for="returnNotes" class="form-label">归还备注</label>
              <textarea 
                class="form-control" 
                id="returnNotes" 
                rows="3" 
                v-model="returnForm.notes"
              ></textarea>
            </div>
            
            <div class="mb-3">
              <label for="equipmentCondition" class="form-label">装备状态</label>
              <select class="form-select" id="equipmentCondition" v-model="returnForm.condition">
                <option value="良好">良好</option>
                <option value="轻微损坏">轻微损坏</option>
                <option value="严重损坏">严重损坏</option>
              </select>
            </div>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">取消</button>
            <button type="button" class="btn btn-primary" @click="submitReturn" :disabled="returnLoading">
              <span v-if="returnLoading" class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
              确认归还
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 批准确认模态框 -->
    <div class="modal fade" id="approveModal" tabindex="-1" aria-hidden="true">
      <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content">
          <div class="modal-header">
            <h5 class="modal-title">批准借用申请</h5>
            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
          </div>
          <div class="modal-body">
            <p>确定要批准 <strong>{{ borrowRecord?.borrower?.name }}</strong> 的借用申请吗？</p>
            
            <div class="mb-3">
              <label for="approveNotes" class="form-label">审批备注</label>
              <textarea 
                class="form-control" 
                id="approveNotes" 
                rows="3" 
                v-model="approveForm.note"
              ></textarea>
            </div>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">取消</button>
            <button type="button" class="btn btn-primary" @click="submitApprove" :disabled="approveLoading">
              <span v-if="approveLoading" class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
              确认批准
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 拒绝确认模态框 -->
    <div class="modal fade" id="rejectModal" tabindex="-1" aria-hidden="true">
      <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content">
          <div class="modal-header">
            <h5 class="modal-title">拒绝借用申请</h5>
            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
          </div>
          <div class="modal-body">
            <p>确定要拒绝 <strong>{{ borrowRecord?.borrower?.name }}</strong> 的借用申请吗？</p>
            
            <div class="mb-3">
              <label for="rejectReason" class="form-label">拒绝原因</label>
              <textarea 
                class="form-control" 
                id="rejectReason" 
                rows="3" 
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
              确认拒绝
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 取消借用确认模态框 -->
    <div class="modal fade" id="cancelModal" tabindex="-1" aria-hidden="true">
      <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content">
          <div class="modal-header">
            <h5 class="modal-title">取消借用申请</h5>
            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
          </div>
          <div class="modal-body">
            <p>确定要取消借用装备 <strong>{{ borrowRecord?.equipment?.name }}</strong> 的申请吗？</p>
            
            <div class="mb-3">
              <label for="cancelReason" class="form-label">取消原因</label>
              <textarea 
                class="form-control" 
                id="cancelReason" 
                rows="3" 
                v-model="cancelForm.reason"
                :class="{'is-invalid': cancelFormError}"
              ></textarea>
              <div class="invalid-feedback" v-if="cancelFormError">
                {{ cancelFormError }}
              </div>
            </div>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">取消</button>
            <button type="button" class="btn btn-primary" @click="submitCancel" :disabled="cancelLoading">
              <span v-if="cancelLoading" class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
              确认取消
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Modal } from 'bootstrap'
import borrowService from '@/api/borrow'
import { useUserStore } from '@/store/modules/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 借用记录数据
const borrowRecord = ref(null)
const loading = ref(true)
const error = ref(null)

// 模态框实例
let returnModalInstance = null
let approveModalInstance = null
let rejectModalInstance = null
let cancelModalInstance = null

// 表单数据
const returnForm = reactive({
  notes: '',
  condition: '良好'
})
const approveForm = reactive({
  note: ''
})
const rejectForm = reactive({
  reason: ''
})
const rejectFormError = ref('')
const cancelForm = reactive({
  reason: ''
})
const cancelFormError = ref('')

// 加载状态
const returnLoading = ref(false)
const approveLoading = ref(false)
const rejectLoading = ref(false)
const cancelLoading = ref(false)

// 操作权限判断
const canApproveBorrow = computed(() => {
  return userStore.isAdmin && 
    borrowRecord.value && 
    borrowRecord.value.status === 'PENDING'
})

const canRejectBorrow = computed(() => {
  return userStore.isAdmin && 
    borrowRecord.value && 
    borrowRecord.value.status === 'PENDING'
})

const canReturnBorrow = computed(() => {
  const isOwnBorrow = borrowRecord.value && 
    borrowRecord.value.borrower && 
    borrowRecord.value.borrower.id === userStore.userId
  
  return (isOwnBorrow || userStore.isAdmin) && 
    borrowRecord.value && 
    (borrowRecord.value.status === 'BORROWED' || borrowRecord.value.status === 'OVERDUE')
})

const canCancelBorrow = computed(() => {
  const isOwnBorrow = borrowRecord.value && 
    borrowRecord.value.borrower && 
    borrowRecord.value.borrower.id === userStore.userId
  
  return isOwnBorrow && 
    borrowRecord.value && 
    borrowRecord.value.status === 'PENDING'
})

// 初始化
onMounted(async () => {

  // 初始化模态框
  initModalInstances()
  
  // 加载借用记录详情
  await fetchBorrowDetail()
})

// 初始化模态框实例
const initModalInstances = () => {
  const returnModalEl = document.getElementById('returnModal')
  const approveModalEl = document.getElementById('approveModal')
  const rejectModalEl = document.getElementById('rejectModal')
  const cancelModalEl = document.getElementById('cancelModal')
  
  if (returnModalEl) returnModalInstance = new Modal(returnModalEl)
  if (approveModalEl) approveModalInstance = new Modal(approveModalEl)
  if (rejectModalEl) rejectModalInstance = new Modal(rejectModalEl)
  if (cancelModalEl) cancelModalInstance = new Modal(cancelModalEl)
}

// 获取借用记录详情
const fetchBorrowDetail = async () => {
  const id = route.params.id
  if (!id) {
    error.value = '未提供借用记录ID'
    loading.value = false
    return
  }
  
  loading.value = true
  error.value = null
  
  try {

    const data = await borrowService.getById(id)
    borrowRecord.value = data

  } catch (err) {
    console.error('获取借用记录详情失败:', err)
    error.value = '获取借用记录详情失败，请稍后再试'
  } finally {
    loading.value = false
  }
}

// 根据借用状态返回显示标签
const getBorrowStatusLabel = (status) => {
  switch (status) {
    case 'PENDING':
      return '待审批'
    case 'BORROWED':
    case 'APPROVED':
      return '已借出'
    case 'REJECTED':
      return '已拒绝'
    case 'RETURNED':
      return '已归还'
    case 'OVERDUE':
      return '已逾期'
    default:
      return '未知状态'
  }
}

// 格式化日期
const formatDate = (dateString) => {
  if (!dateString) return '-'
  const date = new Date(dateString)
  return date.toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' })
}

// 格式化日期和时间
const formatDateTime = (dateString) => {
  if (!dateString) return '-'
  const date = new Date(dateString)
  return date.toLocaleDateString('zh-CN', { 
    year: 'numeric', 
    month: '2-digit', 
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 返回上一页
const goBack = () => {
  router.back()
}

// 处理归还
const handleReturn = () => {
  returnForm.notes = ''
  returnForm.condition = '良好'
  returnModalInstance.show()
}

// 提交归还
const submitReturn = async () => {
  returnLoading.value = true
  
  try {
    await borrowService.returnEquipment(borrowRecord.value.id, {
      notes: returnForm.notes,
      condition: returnForm.condition
    })
    
    // 关闭模态框
    returnModalInstance.hide()
    
    // 重新加载详情
    await fetchBorrowDetail()
  } catch (error) {
    console.error('归还失败', error)
  } finally {
    returnLoading.value = false
  }
}

// 处理批准
const handleApprove = () => {
  approveForm.note = ''
  approveModalInstance.show()
}

// 提交批准
const submitApprove = async () => {
  approveLoading.value = true
  
  try {
    await borrowService.approveBorrow(borrowRecord.value.id, {
      note: approveForm.note
    })
    
    // 关闭模态框
    approveModalInstance.hide()
    
    // 重新加载详情
    await fetchBorrowDetail()
  } catch (error) {
    console.error('批准失败', error)
  } finally {
    approveLoading.value = false
  }
}

// 处理拒绝
const handleReject = () => {
  rejectForm.reason = ''
  rejectFormError.value = ''
  rejectModalInstance.show()
}

// 提交拒绝
const submitReject = async () => {
  // 验证
  if (!rejectForm.reason) {
    rejectFormError.value = '请输入拒绝原因'
    return
  }
  
  rejectLoading.value = true
  
  try {
    await borrowService.rejectBorrow(borrowRecord.value.id, {
      reason: rejectForm.reason
    })
    
    // 关闭模态框
    rejectModalInstance.hide()
    
    // 重新加载详情
    await fetchBorrowDetail()
  } catch (error) {
    console.error('拒绝失败', error)
  } finally {
    rejectLoading.value = false
  }
}

// 处理取消
const handleCancel = () => {
  cancelForm.reason = ''
  cancelFormError.value = ''
  cancelModalInstance.show()
}

// 提交取消
const submitCancel = async () => {
  // 验证
  if (!cancelForm.reason) {
    cancelFormError.value = '请输入取消原因'
    return
  }
  
  cancelLoading.value = true
  
  try {
    await borrowService.cancelBorrow(borrowRecord.value.id, cancelForm.reason)
    
    // 关闭模态框
    cancelModalInstance.hide()
    
    // 重新加载详情
    await fetchBorrowDetail()
  } catch (error) {
    console.error('取消失败', error)
  } finally {
    cancelLoading.value = false
  }
}

// 打印详情
const printDetail = () => {
  window.print()
}
</script>

<style scoped>
.card {
  border-radius: 0.75rem;
  overflow: hidden;
}

.card-header {
  background-color: #fff;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
}

@media print {
  .container-fluid {
    width: 100%;
    margin: 0;
    padding: 0;
  }
  
  .btn, 
  button:not(.btn-close) {
    display: none !important;
  }
  
  .card {
    border: 1px solid #ddd !important;
    box-shadow: none !important;
    break-inside: avoid;
  }
  
  .card-body {
    padding: 1rem !important;
  }
}
</style> 