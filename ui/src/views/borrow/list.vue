<template>
  <div class="container-fluid py-4">
    <div class="row mb-4">
      <div class="col-12 d-flex justify-content-between align-items-center">
        <h2 class="mb-0 fw-bold">借用记录</h2>
        <button class="btn btn-primary" @click="openBorrowModal">
          <i class="bi bi-plus-lg me-1"></i> 新增借用
        </button>
      </div>
    </div>
    
    <!-- 搜索和筛选 -->
    <div class="row mb-4">
      <div class="col-12">
        <div class="card border-0 shadow-sm">
          <div class="card-body">
            <div class="row g-3">
              <div class="col-lg-3 col-md-4 col-sm-6">
                <div class="form-floating">
                  <input 
                    type="text" 
                    class="form-control" 
                    id="searchQuery" 
                    v-model="searchQuery"
                    placeholder="搜索装备名称或编号"
                  >
                  <label for="searchQuery">搜索装备名称或编号</label>
                </div>
              </div>
              
              <div class="col-lg-3 col-md-4 col-sm-6">
                <div class="form-floating">
                  <select class="form-select" id="statusFilter" v-model="statusFilter">
                    <option value="">全部状态</option>
                    <option value="PENDING">待审批</option>
                    <option value="APPROVED">已批准</option>
                    <option value="REJECTED">已拒绝</option>
                    <option value="BORROWED">已借出</option>
                    <option value="RETURNED">已归还</option>
                    <option value="OVERDUE">已逾期</option>
                  </select>
                  <label for="statusFilter">借用状态</label>
                </div>
              </div>
              
              <div class="col-lg-4 col-md-4 col-sm-6">
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
              
              <div class="col-lg-2 col-md-12 d-flex align-items-center justify-content-end">
                <button class="btn btn-outline-secondary me-2" @click="resetFilters">
                  <i class="bi bi-arrow-clockwise me-1"></i> 重置
                </button>
                <button class="btn btn-primary" @click="fetchBorrows">
                  <i class="bi bi-search me-1"></i> 搜索
                </button>
              </div>
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
                    <th class="border-0">装备名称</th>
                    <th class="border-0">借用人</th>
                    <th class="border-0">借用日期</th>
                    <th class="border-0">预计归还日期</th>
                    <th class="border-0">实际归还日期</th>
                    <th class="border-0">状态</th>
                    <th class="border-0">操作</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="item in borrows" :key="item.id">
                    <td>
                      <div class="d-flex align-items-center">
                        <div class="rounded-circle bg-light p-2 me-2">
                          <i class="bi bi-box-seam text-primary"></i>
                        </div>
                        <div>
                          <div class="fw-bold">{{ item.equipment?.name || '未知装备' }}</div>
                          <div class="small text-muted">{{ item.equipment?.code || item.equipment?.serialNumber || '无编号' }}</div>
                        </div>
                      </div>
                    </td>
                    <td>{{ item.borrower?.name || item.user?.name || '未知用户' }}</td>
                    <td>{{ formatDate(item.borrowDate) }}</td>
                    <td>{{ formatDate(item.expectedReturnDate) }}</td>
                    <td>{{ formatDate(item.actualReturnDate) || '-' }}</td>
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
                    <td colspan="7" class="text-center py-5">
                      <div class="spinner-border text-primary" role="status">
                        <span class="visually-hidden">加载中...</span>
                      </div>
                      <div class="mt-2 text-muted">正在加载数据...</div>
                    </td>
                  </tr>
                  <tr v-else-if="borrows.length === 0">
                    <td colspan="7" class="text-center py-5">
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
          <div class="card-footer bg-transparent py-3">
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
    
    <!-- 新增借用模态框 -->
    <div class="modal fade" id="borrowModal" tabindex="-1" aria-hidden="true">
      <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content">
          <div class="modal-header">
            <h5 class="modal-title">新增借用</h5>
            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
          </div>
          <div class="modal-body">
            <div class="alert alert-info mb-3">
              <i class="bi bi-info-circle me-2"></i> 只有状态为"可用"的装备才能被借用。
            </div>
            <div v-if="availableEquipments.length === 0" class="alert alert-warning mb-3">
              <i class="bi bi-exclamation-triangle me-2"></i> 当前没有可用装备，请联系管理员添加或更新装备状态。
            </div>
            <form>
              <!-- 装备选择 -->
              <div class="mb-3">
                <label for="equipmentSelect" class="form-label">借用装备</label>
                <div class="input-group">
                  <select 
                    class="form-select" 
                    id="equipmentSelect" 
                    v-model="borrowForm.equipmentId"
                    :class="{'is-invalid': borrowFormError.equipmentId}"
                  >
                    <option value="" disabled>请选择需要借用的装备</option>
                    <option v-for="item in availableEquipments" :key="item.id" :value="item.id">
                      {{ item.name }} ({{ item.serialNumber || '无序列号' }}) - 可用数量: {{ item.availableQuantity }}
                    </option>
                  </select>
                  <button 
                    class="btn btn-outline-secondary" 
                    type="button" 
                    title="刷新装备列表"
                    @click="fetchAvailableEquipments"
                  >
                    <i class="bi bi-arrow-clockwise"></i>
                  </button>
                </div>
                <div class="invalid-feedback" v-if="borrowFormError.equipmentId">
                  {{ borrowFormError.equipmentId }}
                </div>
                <div class="form-text text-muted mt-1">
                  当前有 {{ availableEquipments.length }} 件可借用装备
                </div>
              </div>
              
              <!-- 借用日期 -->
              <div class="mb-3">
                <label for="borrowDate" class="form-label">借用日期</label>
                <input 
                  type="date" 
                  class="form-control" 
                  id="borrowDate" 
                  v-model="borrowForm.borrowDate"
                  :class="{'is-invalid': borrowFormError.borrowDate}"
                >
                <div class="invalid-feedback" v-if="borrowFormError.borrowDate">
                  {{ borrowFormError.borrowDate }}
                </div>
              </div>
              
              <!-- 预计归还日期 -->
              <div class="mb-3">
                <label for="expectedReturnDate" class="form-label">预计归还日期</label>
                <input 
                  type="date" 
                  class="form-control" 
                  id="expectedReturnDate" 
                  v-model="borrowForm.expectedReturnDate"
                  :class="{'is-invalid': borrowFormError.expectedReturnDate}"
                >
                <div class="invalid-feedback" v-if="borrowFormError.expectedReturnDate">
                  {{ borrowFormError.expectedReturnDate }}
                </div>
              </div>
              
              <!-- 借用原因 -->
              <div class="mb-3">
                <label for="reason" class="form-label">借用原因</label>
                <textarea 
                  class="form-control" 
                  id="reason" 
                  rows="3" 
                  v-model="borrowForm.reason"
                  :class="{'is-invalid': borrowFormError.reason}"
                ></textarea>
                <div class="invalid-feedback" v-if="borrowFormError.reason">
                  {{ borrowFormError.reason }}
                </div>
              </div>
            </form>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">取消</button>
            <button 
              type="button" 
              class="btn btn-primary" 
              @click="submitBorrow" 
              :disabled="borrowLoading || availableEquipments.length === 0"
            >
              <span v-if="borrowLoading" class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
              {{ borrowLoading ? '提交中...' : '提交申请' }}
            </button>
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
import { useAuthStore } from '@/store/modules/auth'
import { Modal } from 'bootstrap'
import borrowService from '@/api/borrow'
import equipmentService from '@/api/equipment'

const router = useRouter()
const authStore = useAuthStore()

// 模态框实例
let borrowModalInstance = null
let returnModalInstance = null
let cancelModalInstance = null

// 列表数据
const borrows = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const totalItems = ref(0)
const totalPages = computed(() => Math.ceil(totalItems.value / pageSize.value))

// 搜索和筛选
const searchQuery = ref('')
const statusFilter = ref('')
const dateRange = reactive({
  start: '',
  end: ''
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

// 新增借用
const availableEquipments = ref([])
const borrowForm = reactive({
  equipmentId: '',
  borrowDate: new Date().toISOString().split('T')[0],
  expectedReturnDate: '',
  reason: ''
})
const borrowFormError = reactive({
  equipmentId: '',
  borrowDate: '',
  expectedReturnDate: '',
  reason: ''
})
const borrowLoading = ref(false)

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
  const borrowModalEl = document.getElementById('borrowModal')
  const returnModalEl = document.getElementById('returnModal')
  const cancelModalEl = document.getElementById('cancelModal')
  
  if (borrowModalEl) borrowModalInstance = new Modal(borrowModalEl)
  if (returnModalEl) returnModalInstance = new Modal(returnModalEl)
  if (cancelModalEl) cancelModalInstance = new Modal(cancelModalEl)
  
  // 监听模态框打开事件，确保每次打开模态框时都获取最新的可用装备
  if (borrowModalEl) {
    borrowModalEl.addEventListener('show.bs.modal', async () => {
      await fetchAvailableEquipments();
    });
  }
  
  // 获取装备和借用数据
  await fetchAvailableEquipments()
  await fetchBorrows()
})

// 获取可用装备
const fetchAvailableEquipments = async () => {
  try {
    // 使用equipmentService替代原生fetch
    const response = await equipmentService.getAvailable();

    // 确保只显示状态为"可用"且可用数量大于0的装备
    if (response && Array.isArray(response)) {
      // 记录原始数据长度
      // 先检查是否有空数据
      const filteredData = response.filter(item => item !== null && item !== undefined);
      if (filteredData.length < response.length) {
        console.warn(`过滤掉了 ${response.length - filteredData.length} 条无效数据`);
      }
      
      availableEquipments.value = filteredData.filter(item => {
        // 检查装备对象完整性
        if (!item) {
          console.warn('发现空装备项');
          return false;
        }
        
        // 检查必要属性
        if (item.status === undefined || item.availableQuantity === undefined) {
          console.warn(`装备ID: ${item.id} 缺少必要属性`, item);
          return false;
        }
        
        // 确保装备状态为"可用"且可用数量大于0
        const isAvailable = item.status === '可用' && item.availableQuantity > 0;
        
        if (!isAvailable) {
          console.warn(`装备 ${item.name || item.id} 不满足借用条件:`, {
            状态: item.status,
            可用数量: item.availableQuantity
          });
        }
        
        return isAvailable;
      });
      
      // 日志记录筛选结果
      if (availableEquipments.value.length > 0) {
        console.log('可用装备列表:', availableEquipments.value.map(e => ({
          id: e.id,
          name: e.name,
          status: e.status,
          availableQuantity: e.availableQuantity
        })));
      } else {
        console.warn('过滤后没有符合条件的可用装备');
      }
    } else {
      console.warn('接口返回的数据格式不符合预期:', response);
      availableEquipments.value = [];
    }
  } catch (error) {
    console.error('获取可用装备失败', error);
    // 显示详细错误信息
    if (error.response) {
      console.error('错误响应:', error.response.status, error.response.data);
    }
    availableEquipments.value = [];
  }
}

// 获取借用记录
const fetchBorrows = async () => {
  loading.value = true
  
  try {
    const params = {
      page: currentPage.value - 1,
      size: pageSize.value,
      query: searchQuery.value,
      status: statusFilter.value,
      startDate: dateRange.start,
      endDate: dateRange.end
    }

    console.log('正在获取借用记录，参数:', params);
    const response = await borrowService.getBorrows(params);
    console.log('获取借用记录响应:', response);

    // 安全地处理响应数据
    if (response && response.data && Array.isArray(response.data.content)) {
      borrows.value = response.data.content;
      totalItems.value = response.data.totalElements || 0;
      console.log(`成功加载 ${borrows.value.length} 条借用记录`);
    } else if (response && Array.isArray(response)) {
      // 处理直接返回数组的情况
      borrows.value = response;
      totalItems.value = response.length;
      console.log(`成功加载 ${borrows.value.length} 条借用记录（数组格式）`);
    } else {
      borrows.value = [];
      totalItems.value = 0;
      console.warn('未获取到有效的借用记录数据或数据格式不符合预期:', response);
      
      // 显示友好的提示信息
      if (!response) {
        alert('暂无借用记录数据');
      }
    }
  } catch (error) {
    console.error('获取借用记录失败', error);
    
    // 提取详细的错误信息
    let errorMsg = '请稍后重试';
    if (error.response?.data?.message) {
      errorMsg = error.response.data.message;
    } else if (error.message) {
      errorMsg = error.message;
    }
    
    console.error('错误详情:', errorMsg);
    alert(`获取借用记录失败: ${errorMsg}`);
    
    borrows.value = [];
    totalItems.value = 0;
  } finally {
    // 确保在任何情况下都将loading设置为false
    loading.value = false;
    console.log('数据加载完成，loading状态已重置');
  }
}

// 重置筛选条件
const resetFilters = () => {
  searchQuery.value = ''
  statusFilter.value = ''
  dateRange.start = ''
  dateRange.end = ''
  currentPage.value = 1
  fetchBorrows()
}

// 分页处理
const goToPage = (page) => {
  if (page < 1 || page > totalPages.value) return
  currentPage.value = page
  fetchBorrows()
}

const handlePageSizeChange = () => {
  currentPage.value = 1
  fetchBorrows()
}

// 打开新增借用模态框
const openBorrowModal = async () => {
  // 先开始获取最新的可用装备列表，不等待完成
  fetchAvailableEquipments();
  
  // 重置借用表单和错误信息
  borrowForm.equipmentId = '';
  borrowForm.borrowDate = new Date().toISOString().split('T')[0];
  borrowForm.expectedReturnDate = '';
  borrowForm.reason = '';
  
  Object.keys(borrowFormError).forEach(key => {
    borrowFormError[key] = '';
  });

  // 显示模态框
  if (borrowModalInstance) {
    borrowModalInstance.show();
  }
}

// 提交借用申请
const submitBorrow = async () => {
  // 验证表单
  let isValid = true
  
  if (!borrowForm.equipmentId) {
    borrowFormError.equipmentId = '请选择要借用的装备'
    isValid = false
  } else {
    // 确认选择的装备仍然可用
    const selectedEquipment = availableEquipments.value.find(e => e.id === borrowForm.equipmentId)
    if (!selectedEquipment) {
      borrowFormError.equipmentId = '所选装备不在可用装备列表中，请重新选择'
      isValid = false
    } else if (!(selectedEquipment.status === '可用' && selectedEquipment.availableQuantity > 0)) {
      borrowFormError.equipmentId = `所选装备"${selectedEquipment.name}"当前状态不可借用，请重新选择`
      isValid = false
      // 刷新装备列表
      await fetchAvailableEquipments()
    }
  }
  
  if (!borrowForm.borrowDate) {
    borrowFormError.borrowDate = '请选择借用日期'
    isValid = false
  }
  
  if (!borrowForm.expectedReturnDate) {
    borrowFormError.expectedReturnDate = '请选择预计归还日期'
    isValid = false
  } else if (new Date(borrowForm.expectedReturnDate) < new Date(borrowForm.borrowDate)) {
    borrowFormError.expectedReturnDate = '预计归还日期不能早于借用日期'
    isValid = false
  }
  
  if (!borrowForm.reason) {
    borrowFormError.reason = '请输入借用原因'
    isValid = false
  }
  
  if (!isValid) return
  
  borrowLoading.value = true
  
  try {
    // 构建借用请求数据
    const borrowRequest = {
      equipmentId: borrowForm.equipmentId,
      quantity: 1, // 默认借用数量为1
      // 将日期字符串转换为ISO格式的日期时间字符串
      expectedReturnDate: new Date(borrowForm.expectedReturnDate + 'T23:59:59').toISOString(),
      purpose: borrowForm.reason,
      remarks: ''
    }

    // 使用 create 方法创建借用记录
    await borrowService.create(borrowRequest)
    
    // 关闭模态框
    borrowModalInstance.hide()
    
    // 刷新列表
    fetchBorrows()
    
    // 显示成功消息
    alert('借用申请已提交，请等待审批')
  } catch (error) {
    console.error('提交借用申请失败', error)
    
    // 提取错误信息
    let errorMsg = '请稍后再试'
    if (error.response?.data?.message) {
      errorMsg = error.response.data.message
      
      // 如果是装备状态相关错误，刷新装备列表
      if (errorMsg.includes('状态') || errorMsg.includes('可用')) {
        await fetchAvailableEquipments()
      }
    }
    
    alert('提交失败：' + errorMsg)
  } finally {
    borrowLoading.value = false
  }
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
    fetchBorrows()
    
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
    fetchBorrows()
    
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
  if (!dateString) return null
  
  try {
    const date = new Date(dateString)
    // 检查日期是否有效
    if (isNaN(date.getTime())) {
      console.warn('无效的日期格式:', dateString)
      return '-'
    }
    return date.toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' })
  } catch (error) {
    console.error('日期格式化错误:', error)
    return '-'
  }
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

// 判断是否可以归还
const canReturn = (borrow) => {
  return (borrow.status === 'BORROWED' || borrow.status === 'APPROVED') && 
         authStore.userId === borrow.user?.id
}

// 判断是否可以取消
const canCancel = (borrow) => {
  return borrow.status === 'PENDING' && authStore.userId === borrow.user?.id
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

.form-floating > .form-select {
  height: calc(3.5rem + 2px);
  padding-top: 1.625rem;
  padding-bottom: 0.625rem;
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
</style> 