<script setup>
import { ref, onMounted, computed, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useUserStore } from '@/store/modules/user';
import equipmentService from '@/api/equipment';
import borrowService from '@/api/borrow';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Modal } from 'bootstrap';

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();
const isAdmin = computed(() => userStore.isAdmin);
const id = parseInt(route.params.id);

// 装备信息
const loading = ref(true);
const equipment = ref(null);
const category = ref(null);
const borrowHistory = ref([]);
const historyLoading = ref(false);

// 借用表单
const borrowForm = ref({
  equipmentId: '',
  borrowTime: new Date().toISOString().substr(0, 10),
  expectedReturnTime: '',
  quantity: 1,
  reason: '',
  remarks: ''
});

// 初始化模态框
let borrowModal = null;
let deleteModal = null;

// 获取装备详情
const fetchEquipmentDetail = async () => {
  const id = route.params.id;
  if (!id) {
    ElMessage.error('装备ID不能为空');
    router.push('/equipment/list');
    return;
  }
  
  loading.value = true;
  try {
    // 强制清除缓存，确保获取最新数据
    equipmentService.clearCache && equipmentService.clearCache();
    
    // 使用时间戳确保服务器端返回最新数据
    const timestamp = new Date().getTime();
    const data = await equipmentService.getById(id);
    equipment.value = data;
    
    // 设置借用表单
    if (equipment.value?.id) {
      borrowForm.value.equipmentId = equipment.value.id;
      
      // 设置预计归还时间为7天后
      const returnDate = new Date();
      returnDate.setDate(returnDate.getDate() + 7);
      borrowForm.value.expectedReturnTime = returnDate.toISOString().substr(0, 10);
      
      // 获取借用历史
      await fetchBorrowHistory();
    }
  } catch (error) {
    console.error('获取装备详情失败', error);
    ElMessage.error('获取装备详情失败');
  } finally {
    loading.value = false;
  }
};

// 获取借用历史
const fetchBorrowHistory = async () => {
  if (!equipment.value || !equipment.value.id) return;
  
  historyLoading.value = true;
  try {
    const data = await borrowService.getActiveByEquipmentId(equipment.value.id);
    borrowHistory.value = data || [];
  } catch (error) {
    console.error('获取借用历史失败', error);
  } finally {
    historyLoading.value = false;
  }
};

// 打开借用对话框
const openBorrowModal = () => {
  if (borrowModal) {
    borrowModal.show();
  }
};

// 提交借用申请
const submitBorrowRequest = async () => {
  // 表单验证
  if (!borrowForm.value.expectedReturnTime) {
    ElMessage.warning('请选择预计归还日期');
    return;
  }
  
  if (!borrowForm.value.reason) {
    ElMessage.warning('请填写借用原因');
    return;
  }
  
  if (!borrowForm.value.quantity || borrowForm.value.quantity < 1) {
    ElMessage.warning('借用数量必须大于0');
    return;
  }
  
  if (borrowForm.value.quantity > equipment.value.availableQuantity) {
    ElMessage.warning(`最多可借用${equipment.value.availableQuantity}件`);
    return;
  }
  
  try {
    await borrowService.apply(borrowForm.value);
    ElMessage.success('借用申请已提交，请等待审批');
    borrowModal.hide();
    
    // 重新获取装备详情和借用历史
    await fetchEquipmentDetail();
    await fetchBorrowHistory();
  } catch (error) {
    console.error('提交借用申请失败', error);
    if (error.response?.status === 401) {
      ElMessage.error('登录已过期，请重新登录');
      // 延迟跳转到登录页面，以便用户看到错误提示
      setTimeout(() => {
        userStore.logout();
        router.push('/login');
      }, 1500);
    } else {
      ElMessage.error(error.response?.data?.message || '提交借用申请失败');
    }
  }
};

// 前往编辑页面
const goToEdit = () => {
  router.push(`/equipment/edit/${equipment.value.id}`);
};

// 前往列表页面
const goToList = () => {
  router.push('/equipment/list');
};

// 确认删除
const confirmDelete = () => {
  if (deleteModal) {
    deleteModal.show();
  }
};

// 借用装备
const borrowEquipment = () => {
  if (borrowModal) {
    borrowModal.show();
  }
};

// 获取状态图标
const getStatusIcon = (status) => {
  switch (status) {
    case '可用': return 'bi-check-circle-fill';
    case '维修中': return 'bi-tools';
    case '报废': return 'bi-x-circle-fill';
    default: return 'bi-question-circle-fill';
  }
};

// 计算可用百分比
const calcAvailablePercentage = () => {
  if (!equipment.value || !equipment.value.quantity || equipment.value.quantity <= 0) {
    return 0;
  }
  return Math.round((equipment.value.availableQuantity / equipment.value.quantity) * 100);
};

// 获取借用状态文本
const getBorrowStatusText = (status) => {
  switch (status) {
    case 'PENDING': return '待审批';
    case 'APPROVED': return '使用中';
    case 'REJECTED': return '已拒绝';
    case 'RETURNED': return '已归还';
    case 'OVERDUE': return '已逾期';
    default: return '未知';
  }
};

// 格式化状态
const formatStatus = (status) => {
  switch (status) {
    case 0: return { text: '不可用', class: 'bg-danger' };
    case 1: return { text: '可用', class: 'bg-success' };
    case 2: return { text: '借用中', class: 'bg-warning' };
    case 3: return { text: '维修中', class: 'bg-info' };
    default: return { text: '未知', class: 'bg-secondary' };
  }
};

// 格式化借用状态
const formatBorrowStatus = (status) => {
  switch (status) {
    case 0: return { text: '待审批', class: 'bg-secondary' };
    case 1: return { text: '使用中', class: 'bg-success' };
    case 2: return { text: '已拒绝', class: 'bg-danger' };
    case 3: return { text: '已归还', class: 'bg-info' };
    default: return { text: '未知', class: 'bg-secondary' };
  }
};

// 格式化日期
const formatDate = (date) => {
  if (!date) return '-';
  return new Date(date).toLocaleDateString('zh-CN');
};

// 删除装备
const deleteEquipment = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要删除该装备吗？删除后不可恢复。',
      '确认删除',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning',
      }
    );
    
    await equipmentService.delete(equipment.value.id);
    ElMessage.success('装备已删除');
    router.push('/equipment/list');
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除装备失败', error);
      ElMessage.error('删除装备失败');
    }
  }
};

// 生命周期钩子
onMounted(() => {
  // 初始化模态框
  setTimeout(() => {
    borrowModal = new Modal(document.getElementById('borrowModal'));
    deleteModal = new Modal(document.getElementById('deleteModal'));
  }, 100);
});

// 使用immediate确保组件挂载后即时运行一次
watch(() => route.params.id, async (newId, oldId) => {

  if (newId) {
    // 重置状态
    equipment.value = null;
    borrowHistory.value = [];
    // 获取数据
    await fetchEquipmentDetail();
  }
}, { immediate: true });
</script>

<template>
  <div class="container-fluid py-4">
    <!-- 页面标题 -->
    <div class="row mb-4">
      <div class="col-12">
        <div class="card border-0 shadow-sm">
          <div class="card-body d-flex justify-content-between align-items-center">
            <h5 class="m-0">
              <i class="bi bi-box me-2"></i>装备详情
              <span v-if="equipment" class="text-muted ms-2 small">ID: {{ equipment.id }}</span>
            </h5>
            <div>
              <button class="btn btn-outline-secondary me-2" @click="goToList">
                <i class="bi bi-arrow-left me-1"></i>返回
              </button>
              <button v-if="isAdmin" class="btn btn-primary" @click="goToEdit">
                <i class="bi bi-pencil me-1"></i>编辑
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="text-center py-5">
      <div class="spinner-border text-primary" role="status">
        <span class="visually-hidden">加载中...</span>
      </div>
      <p class="text-muted mt-3">正在加载装备信息...</p>
    </div>

    <!-- 装备不存在 -->
    <div v-else-if="!equipment" class="row">
      <div class="col-12">
        <div class="card border-0 shadow-sm">
          <div class="card-body text-center py-5">
            <i class="bi bi-exclamation-circle text-danger fs-1"></i>
            <h4 class="mt-3">装备不存在</h4>
            <p class="text-muted">找不到该装备信息或已被删除</p>
            <button class="btn btn-primary" @click="goToList">
              <i class="bi bi-list me-1"></i>查看所有装备
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 装备详情内容 -->
    <div v-else class="row">
      <!-- 基本信息 -->
      <div class="col-lg-8">
        <div class="card border-0 shadow-sm mb-4">
          <div class="card-body">
            <h6 class="text-primary border-bottom pb-2 mb-4">基本信息</h6>
            <div class="row g-3">
              <div class="col-md-6">
                <div class="small text-muted mb-1">装备名称</div>
                <div class="fw-bold">{{ equipment.name }}</div>
              </div>
              <div class="col-md-6">
                <div class="small text-muted mb-1">序列号</div>
                <div>
                  <span class="badge bg-light text-dark p-2">{{ equipment.serialNumber }}</span>
                </div>
              </div>
              <div class="col-md-6">
                <div class="small text-muted mb-1">分类</div>
                <div>
                  <span 
                    class="badge rounded-pill bg-info bg-opacity-10 text-info border border-info border-opacity-25 p-2"
                  >
                    <i class="bi bi-folder me-1"></i>
                    {{ equipment.category ? equipment.category.name : '未分类' }}
                  </span>
                </div>
              </div>
              <div class="col-md-6">
                <div class="small text-muted mb-1">状态</div>
                <div>
                  <span 
                    :class="[
                      'badge p-2',
                      equipment.status === '可用' ? 'bg-success' : 
                      equipment.status === '维修中' ? 'bg-warning text-dark' : 'bg-danger'
                    ]"
                  >
                    <i class="bi" :class="getStatusIcon(equipment.status)"></i>
                    {{ equipment.status }}
                  </span>
                </div>
              </div>
              <div class="col-md-6">
                <div class="small text-muted mb-1">总数量</div>
                <div class="fw-bold">{{ equipment.quantity }} 件</div>
              </div>
              <div class="col-md-6">
                <div class="small text-muted mb-1">可用数量</div>
                <div>
                  <span 
                    :class="[
                      'badge p-2',
                      equipment.availableQuantity <= 0 ? 'bg-danger' : 
                      equipment.availableQuantity < equipment.quantity * 0.2 ? 'bg-warning text-dark' : 'bg-success'
                    ]"
                  >
                    {{ equipment.availableQuantity }} / {{ equipment.quantity }}
                  </span>
                </div>
              </div>
              <div class="col-md-12">
                <div class="small text-muted mb-1">描述</div>
                <div>{{ equipment.description || '无描述信息' }}</div>
              </div>
            </div>
          </div>
        </div>

        <!-- 详细规格 -->
        <div class="card border-0 shadow-sm mb-4">
          <div class="card-body">
            <h6 class="text-primary border-bottom pb-2 mb-4">详细规格</h6>
            <div class="row g-3">
              <div class="col-md-6">
                <div class="small text-muted mb-1">制造商</div>
                <div>{{ equipment.manufacturer || '未知' }}</div>
              </div>
              <div class="col-md-6">
                <div class="small text-muted mb-1">型号</div>
                <div>{{ equipment.model || '未知' }}</div>
              </div>
              <div class="col-md-6">
                <div class="small text-muted mb-1">购买日期</div>
                <div>{{ formatDate(equipment.purchaseDate) || '未知' }}</div>
              </div>
              <div class="col-md-6">
                <div class="small text-muted mb-1">购买价格</div>
                <div>{{ equipment.purchasePrice ? `¥${equipment.purchasePrice.toFixed(2)}` : '未知' }}</div>
              </div>
              <div class="col-md-6">
                <div class="small text-muted mb-1">存放位置</div>
                <div>{{ equipment.location || '未指定' }}</div>
              </div>
            </div>
          </div>
        </div>

        <!-- 近期借用记录 -->
        <div class="card border-0 shadow-sm mb-4">
          <div class="card-body">
            <div class="d-flex justify-content-between align-items-center mb-3">
              <h6 class="text-primary border-bottom pb-2 w-100 mb-0">近期借用记录</h6>
            </div>
            <div v-if="historyLoading" class="text-center py-4">
              <div class="spinner-border spinner-border-sm text-primary" role="status"></div>
              <span class="ms-2">加载中...</span>
            </div>
            <div v-else-if="borrowHistory.length === 0" class="text-center py-4">
              <i class="bi bi-clock-history text-muted fs-3"></i>
              <p class="mb-0 mt-2 text-muted">暂无借用记录</p>
            </div>
            <div v-else class="table-responsive">
              <table class="table table-sm table-hover">
                <thead class="table-light">
                  <tr>
                    <th>借用人</th>
                    <th>借用时间</th>
                    <th>归还时间</th>
                    <th class="text-center">状态</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="borrow in borrowHistory" :key="borrow.id">
                    <td>{{ borrow.user ? borrow.user.username : '未知用户' }}</td>
                    <td>{{ formatDate(borrow.borrowTime) }}</td>
                    <td>{{ formatDate(borrow.returnTime) || '未归还' }}</td>
                    <td class="text-center">
                      <span 
                        :class="[
                          'badge',
                          borrow.status === 'RETURNED' ? 'bg-success' : 
                          borrow.status === 'PENDING' ? 'bg-warning text-dark' : 
                          borrow.status === 'OVERDUE' ? 'bg-danger' : 'bg-primary'
                        ]"
                      >
                        {{ getBorrowStatusText(borrow.status) }}
                      </span>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>

      <!-- 侧边信息 -->
      <div class="col-lg-4">
        <!-- 库存状态卡片 -->
        <div class="card border-0 shadow-sm mb-4">
          <div class="card-body">
            <h6 class="text-primary border-bottom pb-2 mb-3">库存状态</h6>
            <div class="progress mb-3" style="height: 20px;">
              <div 
                class="progress-bar progress-bar-striped" 
                role="progressbar" 
                :style="`width: ${calcAvailablePercentage()}%`"
                :class="{
                  'bg-success': calcAvailablePercentage() > 50,
                  'bg-warning': calcAvailablePercentage() <= 50 && calcAvailablePercentage() > 20,
                  'bg-danger': calcAvailablePercentage() <= 20
                }"
              >
                {{ calcAvailablePercentage() }}%
              </div>
            </div>
            <div class="d-flex justify-content-between text-muted small">
              <div>总数量: {{ equipment.quantity }}</div>
              <div>可用数量: {{ equipment.availableQuantity }}</div>
            </div>
            <hr>
            <div v-if="equipment.availableQuantity <= equipment.quantity * 0.2" class="alert alert-warning mb-0">
              <i class="bi bi-exclamation-triangle-fill me-2"></i>
              库存不足，请及时补充
            </div>
            <div v-else-if="equipment.availableQuantity === 0" class="alert alert-danger mb-0">
              <i class="bi bi-exclamation-triangle-fill me-2"></i>
              库存已用尽，无法借用
            </div>
            <div v-else class="alert alert-success mb-0">
              <i class="bi bi-check-circle-fill me-2"></i>
              库存充足
            </div>
          </div>
        </div>

        <!-- 操作记录 -->
        <div class="card border-0 shadow-sm mb-4">
          <div class="card-body">
            <h6 class="text-primary border-bottom pb-2 mb-3">系统信息</h6>
            <ul class="list-group list-group-flush">
              <li class="list-group-item px-0 d-flex justify-content-between">
                <span class="text-muted">创建时间</span>
                <span>{{ formatDate(equipment.createdAt) }}</span>
              </li>
              <li class="list-group-item px-0 d-flex justify-content-between">
                <span class="text-muted">最后更新</span>
                <span>{{ formatDate(equipment.updatedAt) }}</span>
              </li>
            </ul>
          </div>
        </div>

        <!-- 快捷操作 -->
        <div class="card border-0 shadow-sm mb-4">
          <div class="card-body">
            <h6 class="text-primary border-bottom pb-2 mb-3">快捷操作</h6>
            <div class="d-grid gap-2">
              <button 
                class="btn btn-outline-primary" 
                :disabled="equipment.availableQuantity <= 0"
                @click="borrowEquipment"
              >
                <i class="bi bi-box-arrow-in-right me-1"></i>
                借用装备
              </button>
              <button 
                v-if="isAdmin" 
                class="btn btn-outline-warning"
                @click="goToEdit"
              >
                <i class="bi bi-pencil-square me-1"></i>
                编辑装备
              </button>
              <button 
                v-if="isAdmin" 
                class="btn btn-outline-danger"
                @click="confirmDelete"
              >
                <i class="bi bi-trash me-1"></i>
                删除装备
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 删除确认对话框 -->
    <div class="modal fade" id="deleteModal" tabindex="-1" aria-hidden="true" ref="deleteModal">
      <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content">
          <div class="modal-header">
            <h5 class="modal-title">删除确认</h5>
            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
          </div>
          <div class="modal-body">
            <p>确定要删除装备 <span class="fw-bold text-danger">{{ equipment?.name }}</span> 吗？</p>
            <div class="alert alert-warning">
              <i class="bi bi-exclamation-triangle-fill me-2"></i>
              此操作不可恢复，删除后相关借用记录也可能受到影响。
            </div>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">取消</button>
            <button type="button" class="btn btn-danger" @click="deleteEquipment">
              <i class="bi bi-trash me-1"></i>确认删除
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 借用装备模态框 -->
    <div class="modal fade" id="borrowModal" tabindex="-1" aria-hidden="true">
      <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content">
          <div class="modal-header">
            <h5 class="modal-title">借用装备</h5>
            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
          </div>
          <div class="modal-body">
            <form>
              <div class="mb-3">
                <label for="borrowTime" class="form-label">借用日期</label>
                <input 
                  type="date"
                  id="borrowTime"
                  class="form-control"
                  v-model="borrowForm.borrowTime"
                  :min="new Date().toISOString().substr(0, 10)"
                  readonly
                >
              </div>
              <div class="mb-3">
                <label for="expectedReturnTime" class="form-label">预计归还日期</label>
                <input 
                  type="date"
                  id="expectedReturnTime"
                  class="form-control"
                  v-model="borrowForm.expectedReturnTime"
                  :min="new Date().toISOString().substr(0, 10)"
                >
                <div class="form-text">请选择您预计归还装备的时间</div>
              </div>
              <div class="mb-3">
                <label for="quantity" class="form-label">借用数量</label>
                <input 
                  type="number"
                  id="quantity"
                  class="form-control"
                  v-model.number="borrowForm.quantity"
                  min="1"
                  :max="equipment ? equipment.availableQuantity : 1"
                >
                <div class="form-text">最多可借用 {{ equipment ? equipment.availableQuantity : 0 }} 件</div>
              </div>
              <div class="mb-3">
                <label for="reason" class="form-label">借用原因</label>
                <textarea 
                  id="reason"
                  class="form-control"
                  v-model="borrowForm.reason"
                  rows="3"
                  placeholder="请简要说明借用此装备的原因"
                ></textarea>
              </div>
            </form>
            <div class="alert alert-info">
              <i class="bi bi-info-circle-fill me-2"></i>
              借用申请提交后需要管理员审批，请耐心等待。
            </div>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">取消</button>
            <button type="button" class="btn btn-primary" @click="submitBorrowRequest">
              <i class="bi bi-check-circle me-1"></i>提交申请
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.card {
  border-radius: 0.75rem;
  transition: box-shadow 0.2s;
}

.card-header {
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
}

.badge {
  padding: 0.6em 0.85em;
  font-weight: normal;
}

.badge.rounded-pill {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.list-group-item-action {
  transition: all 0.2s;
}

.list-group-item-action:hover {
  background-color: rgba(0, 123, 255, 0.05);
}

.border {
  border-color: rgba(0, 0, 0, 0.1) !important;
}

.text-muted {
  color: #6c757d !important;
}

.fw-medium {
  font-weight: 500 !important;
}
</style> 