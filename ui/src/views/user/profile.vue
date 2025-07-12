<script setup>
import { ref, reactive, onMounted } from 'vue';
import userService from '@/api/user';
import { Modal } from 'bootstrap';
import { useAuthStore } from '@/store/modules/auth';
import { useUserStore } from '@/store/modules/user';
// 类型只在注释中使用
// import { User, PasswordForm } from '../../types/user';

// 状态变量
const loading = ref(false);
const user = ref({});
const editMode = ref(false);
const avatarPreview = ref('');
const avatarFile = ref(null);
const authStore = useAuthStore();
const userStore = useUserStore();

// 表单数据
const userForm = reactive({
  name: '',
  email: '',
  phone: '',
  department: ''
});

// 密码表单
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
});

// 表单验证
const passwordError = ref('');

// Modal实例
let passwordModal;

// 生命周期钩子
onMounted(async () => {
  passwordModal = new Modal(document.getElementById('passwordModal'));
  await fetchUserInfo();
});

// 获取当前用户信息
const fetchUserInfo = async () => {
  loading.value = true;
  try {
    const response = await userService.getCurrentUser();
    // 根据不同的响应结构处理数据
    if (response.data) {
      user.value = response.data;
    } else {
      user.value = response;
    }
    initUserForm();

  } catch (error) {
    console.error('获取用户信息失败', error);
  } finally {
    loading.value = false;
  }
};

// 初始化用户表单
const initUserForm = () => {
  userForm.name = user.value.name || '';
  userForm.email = user.value.email || '';
  userForm.phone = user.value.phone || '';
  userForm.department = user.value.department || '';
};

// 切换编辑模式
const toggleEditMode = () => {
  if (editMode.value) {
    // 取消编辑，重置表单
    initUserForm();
  }
  editMode.value = !editMode.value;
};

// 保存用户信息
const saveUserInfo = async () => {
  try {
    loading.value = true;
    await userService.updateProfile(userForm);
    editMode.value = false;
    await fetchUserInfo();
  } catch (error) {
    console.error('保存用户信息失败', error);
  } finally {
    loading.value = false;
  }
};

// 打开修改密码对话框
const openPasswordModal = () => {
  passwordForm.oldPassword = '';
  passwordForm.newPassword = '';
  passwordForm.confirmPassword = '';
  passwordError.value = '';
  passwordModal.show();
};

// 修改密码
const changePassword = async () => {
  // 验证密码
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    passwordError.value = '两次输入的密码不一致';
    return;
  }
  
  try {
    loading.value = true;
    const passwordData = {
      oldPassword: passwordForm.oldPassword,
      password: passwordForm.newPassword
    };
    await userService.changePassword(passwordData);
    passwordModal.hide();
    // 清空表单
    passwordForm.oldPassword = '';
    passwordForm.newPassword = '';
    passwordForm.confirmPassword = '';
  } catch (error) {
    console.error('修改密码失败', error);
    passwordError.value = '修改密码失败，请检查原密码是否正确';
  } finally {
    loading.value = false;
  }
};

// 处理头像上传
const handleAvatarChange = (event) => {
  const file = event.target.files[0];
  if (!file) return;
  
  // 预览头像
  avatarFile.value = file;
  const reader = new FileReader();
  reader.onload = (e) => {
    avatarPreview.value = e.target.result;
  };
  reader.readAsDataURL(file);
};

// 上传头像
const uploadAvatar = async () => {
  if (!avatarFile.value) return;
  
  try {
    loading.value = true;
    const formData = new FormData();
    formData.append('avatar', avatarFile.value);
    
    const response = await userService.uploadAvatar(formData);
    // 更新auth store中的头像
    if (response && response.avatar) {
      authStore.updateUserAvatar(response.avatar);
      userStore.updateUserAvatar(response.avatar);
    }
    await fetchUserInfo();
    avatarPreview.value = '';
    avatarFile.value = null;
  } catch (error) {
    console.error('上传头像失败', error);
  } finally {
    loading.value = false;
  }
};

// 取消头像上传
const cancelAvatarUpload = () => {
  avatarPreview.value = '';
  avatarFile.value = null;
};

// 获取角色名称
const getRoleName = (roles) => {
  if (!roles) {
    return '普通用户';
  }
  
  // 处理角色是数组的情况
  if (Array.isArray(roles)) {
    if (roles.length === 0) {
      return '普通用户';
    }
    
    // 处理roles中是对象的情况
    for (const role of roles) {
      if (typeof role === 'object' && role.name === 'ROLE_ADMIN') {
        return '管理员';
      } else if (role === 'ROLE_ADMIN') {
        return '管理员';
      }
    }
  } 
  // 处理角色是字符串的情况
  else if (typeof roles === 'string' && roles === 'ROLE_ADMIN') {
    return '管理员';
  }
  
  return '普通用户';
};

// 格式化日期
const formatDate = (dateString) => {
  if (!dateString) return '-';
  const date = new Date(dateString);
  return date.toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' });
};
</script>

<template>
  <div class="container-fluid py-4">
    <div class="row mb-4">
      <div class="col-12">
        <h4 class="fw-bold mb-0">个人资料</h4>
        <p class="text-muted">查看和管理您的个人信息</p>
      </div>
    </div>
    
    <div class="row">
      <!-- 个人信息卡片 -->
      <div class="col-lg-4 col-md-5 mb-4">
        <div class="card border-0 shadow-sm rounded-4 overflow-hidden">
          <div class="card-body text-center p-4">
            <div v-if="loading" class="my-5 py-5">
              <div class="spinner-border text-primary" role="status">
                <span class="visually-hidden">加载中...</span>
              </div>
            </div>
            
            <div v-else>
              <!-- 头像 -->
              <div class="position-relative mb-4 mx-auto avatar-container">
                <div class="avatar-wrapper">
                  <img 
                    :src="avatarPreview || (user.avatar ? user.avatar : '/favicon.svg')" 
                    alt="User Avatar"
                    class="avatar-img"
                  >
                </div>
                
                <div class="avatar-upload" v-if="editMode">
                  <label for="avatar-upload" class="btn btn-light-primary rounded-circle">
                    <i class="bi bi-camera-fill"></i>
                  </label>
                  <input 
                    type="file" 
                    id="avatar-upload" 
                    class="d-none" 
                    accept="image/*"
                    @change="handleAvatarChange"
                  >
                </div>
              </div>
              
              <!-- 头像预览操作按钮 -->
              <div v-if="avatarPreview" class="d-flex justify-content-center gap-2 mb-3">
                <button class="btn btn-sm btn-primary" @click="uploadAvatar">
                  <i class="bi bi-check-lg me-1"></i>确认
                </button>
                <button class="btn btn-sm btn-outline-secondary" @click="cancelAvatarUpload">
                  <i class="bi bi-x-lg me-1"></i>取消
                </button>
              </div>
              
              <h4 class="fw-bold mb-1">{{ user.name || user.username }}</h4>
              <p class="text-muted mb-3">{{ user.username }}</p>
              
              <div class="mb-4">
                <span class="badge rounded-pill"
                  :class="user.roles && user.roles.some(r => r === 'ROLE_ADMIN' || (typeof r === 'object' && r.name === 'ROLE_ADMIN')) ? 
                    'badge-soft-primary' : 'badge-soft-info'"
                >
                  <i class="bi" :class="user.roles && user.roles.some(r => r === 'ROLE_ADMIN' || (typeof r === 'object' && r.name === 'ROLE_ADMIN')) ? 
                    'bi-shield-fill' : 'bi-person-fill'"></i>
                  {{ getRoleName(user.roles) }}
                </span>
              </div>
              
              <div class="account-stats">
                <div class="stat-item">
                  <div class="stat-icon bg-light-primary">
                    <i class="bi bi-calendar-check text-primary"></i>
                  </div>
                  <div class="stat-details">
                    <div class="stat-label">注册时间</div>
                    <div class="stat-value">{{ formatDate(user.createdTime) || '-' }}</div>
                  </div>
                </div>
                <div class="stat-item">
                  <div class="stat-icon bg-light-success">
                    <i class="bi bi-box-arrow-in-right text-success"></i>
                  </div>
                  <div class="stat-details">
                    <div class="stat-label">最后登录</div>
                    <div class="stat-value">{{ formatDate(user.lastLoginTime) || '-' }}</div>
                  </div>
                </div>
              </div>
              
              <div class="d-grid gap-2 mt-4">
                <button class="btn btn-action" :class="editMode ? 'btn-light-danger' : 'btn-light-primary'" @click="toggleEditMode">
                  <i :class="editMode ? 'bi bi-x-circle' : 'bi bi-pencil'" class="me-2"></i>
                  {{ editMode ? '取消编辑' : '编辑资料' }}
                </button>
                <button 
                  v-if="editMode && avatarPreview" 
                  class="btn btn-success"
                  @click="uploadAvatar"
                >
                  <i class="bi bi-cloud-arrow-up me-2"></i>
                  上传新头像
                </button>
                <button class="btn btn-light-warning btn-action" @click="openPasswordModal">
                  <i class="bi bi-key me-2"></i>
                  修改密码
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 详细信息卡片 -->
      <div class="col-lg-8 col-md-7">
        <div class="card border-0 shadow-sm rounded-4 overflow-hidden mb-4">
          <div class="card-header bg-white border-0 py-3">
            <div class="d-flex justify-content-between align-items-center">
              <div>
                <h5 class="fw-bold mb-0 card-title">个人信息</h5>
                <p class="text-muted small mb-0">管理您的个人资料详情</p>
              </div>
              <div v-if="editMode">
                <button class="btn btn-primary px-4" @click="saveUserInfo">
                  <i class="bi bi-check-circle me-2"></i>
                  保存修改
                </button>
              </div>
            </div>
          </div>
          
          <div class="card-body">
            <div v-if="loading" class="text-center py-5">
              <div class="spinner-border text-primary" role="status">
                <span class="visually-hidden">加载中...</span>
              </div>
            </div>
            
            <div v-else class="profile-form">
              <form>
                <div class="row mb-4">
                  <div class="col-md-6">
                    <div class="mb-3">
                      <label class="form-label fw-medium">用户名</label>
                      <div class="input-group">
                        <span class="input-group-text bg-light border-end-0">
                          <i class="bi bi-person text-muted"></i>
                        </span>
                        <input 
                          type="text" 
                          class="form-control form-control-lg ps-0 border-start-0 bg-light" 
                          :value="user.username"
                          disabled
                        >
                      </div>
                      <div class="form-text text-muted">用户名不可修改</div>
                    </div>
                  </div>
                  <div class="col-md-6">
                    <div class="mb-3">
                      <label class="form-label fw-medium">姓名</label>
                      <div class="input-group">
                        <span class="input-group-text" :class="{'bg-light': !editMode}">
                          <i class="bi bi-person-badge text-muted"></i>
                        </span>
                        <input 
                          type="text" 
                          class="form-control form-control-lg ps-0 border-start-0" 
                          v-model="userForm.name"
                          :disabled="!editMode"
                          :class="{'bg-light': !editMode}"
                        >
                      </div>
                    </div>
                  </div>
                </div>
                
                <div class="row mb-4">
                  <div class="col-md-6">
                    <div class="mb-3">
                      <label class="form-label fw-medium">邮箱地址</label>
                      <div class="input-group">
                        <span class="input-group-text" :class="{'bg-light': !editMode}">
                          <i class="bi bi-envelope text-muted"></i>
                        </span>
                        <input 
                          type="email" 
                          class="form-control form-control-lg ps-0 border-start-0" 
                          v-model="userForm.email"
                          :disabled="!editMode"
                          :class="{'bg-light': !editMode}"
                        >
                      </div>
                    </div>
                  </div>
                  <div class="col-md-6">
                    <div class="mb-3">
                      <label class="form-label fw-medium">电话号码</label>
                      <div class="input-group">
                        <span class="input-group-text" :class="{'bg-light': !editMode}">
                          <i class="bi bi-telephone text-muted"></i>
                        </span>
                        <input 
                          type="tel" 
                          class="form-control form-control-lg ps-0 border-start-0" 
                          v-model="userForm.phone"
                          :disabled="!editMode"
                          :class="{'bg-light': !editMode}"
                        >
                      </div>
                    </div>
                  </div>
                </div>
                
                <div class="mb-4">
                  <label class="form-label fw-medium">部门</label>
                  <div class="input-group">
                    <span class="input-group-text" :class="{'bg-light': !editMode}">
                      <i class="bi bi-building text-muted"></i>
                    </span>
                    <input 
                      type="text" 
                      class="form-control form-control-lg ps-0 border-start-0" 
                      v-model="userForm.department"
                      :disabled="!editMode"
                      :class="{'bg-light': !editMode}"
                    >
                  </div>
                </div>
              </form>
            </div>
          </div>
        </div>

        <!-- 额外信息和活动 -->
        <div class="card border-0 shadow-sm rounded-4 overflow-hidden">
          <div class="card-header bg-white border-0 py-3">
            <h5 class="fw-bold mb-0 card-title">账号安全</h5>
          </div>
          <div class="card-body">
            <div class="security-item d-flex align-items-center mb-4">
              <div class="security-icon bg-light-primary">
                <i class="bi bi-key text-primary"></i>
              </div>
              <div class="security-details flex-grow-1">
                <h6 class="mb-1 fw-bold">密码</h6>
                <p class="mb-0 text-muted small">上次更新: {{ user.lastPasswordResetDate ? formatDate(user.lastPasswordResetDate) : '未知' }}</p>
              </div>
              <button class="btn btn-sm btn-light-primary" @click="openPasswordModal">
                修改
              </button>
            </div>
            
            <div class="security-item d-flex align-items-center">
              <div class="security-icon bg-light-warning">
                <i class="bi bi-shield-lock text-warning"></i>
              </div>
              <div class="security-details flex-grow-1">
                <h6 class="mb-1 fw-bold">账号状态</h6>
                <p class="mb-0 text-muted small">
                  <span class="badge" :class="user.enabled ? 'bg-success' : 'bg-danger'">
                    {{ user.enabled ? '已激活' : '已禁用' }}
                  </span>
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 修改密码对话框 -->
    <div class="modal fade" id="passwordModal" tabindex="-1" aria-hidden="true">
      <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content border-0 shadow-lg rounded-4">
          <div class="modal-header border-0">
            <h5 class="modal-title fw-bold">修改密码</h5>
            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
          </div>
          <div class="modal-body p-4">
            <div class="text-center mb-4">
              <div class="modal-icon bg-light-primary mb-3">
                <i class="bi bi-shield-lock text-primary"></i>
              </div>
              <h5 class="fw-bold">设置新密码</h5>
              <p class="text-muted">请确保您的新密码安全且易于记忆</p>
            </div>
            
            <form>
              <div class="alert alert-danger d-flex align-items-center" v-if="passwordError">
                <i class="bi bi-exclamation-triangle-fill me-2"></i>
                <div>{{ passwordError }}</div>
              </div>
              
              <div class="mb-4">
                <label for="old-password" class="form-label fw-medium">当前密码 <span class="text-danger">*</span></label>
                <div class="input-group">
                  <span class="input-group-text">
                    <i class="bi bi-lock text-muted"></i>
                  </span>
                  <input 
                    type="password" 
                    class="form-control form-control-lg ps-2 border-start-0" 
                    id="old-password" 
                    v-model="passwordForm.oldPassword"
                    required
                  >
                </div>
              </div>
              
              <div class="mb-4">
                <label for="new-password" class="form-label fw-medium">新密码 <span class="text-danger">*</span></label>
                <div class="input-group">
                  <span class="input-group-text">
                    <i class="bi bi-lock text-muted"></i>
                  </span>
                  <input 
                    type="password" 
                    class="form-control form-control-lg ps-2 border-start-0" 
                    id="new-password" 
                    v-model="passwordForm.newPassword"
                    required
                  >
                </div>
              </div>
              
              <div class="mb-4">
                <label for="confirm-password" class="form-label fw-medium">确认新密码 <span class="text-danger">*</span></label>
                <div class="input-group">
                  <span class="input-group-text">
                    <i class="bi bi-lock-fill text-muted"></i>
                  </span>
                  <input 
                    type="password" 
                    class="form-control form-control-lg ps-2 border-start-0" 
                    id="confirm-password" 
                    v-model="passwordForm.confirmPassword"
                    required
                  >
                </div>
              </div>
            </form>
          </div>
          <div class="modal-footer border-0 pt-0">
            <button type="button" class="btn btn-light" data-bs-dismiss="modal">取消</button>
            <button type="button" class="btn btn-primary px-4" @click="changePassword">
              <i class="bi bi-check-circle me-2"></i>确认修改
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
// 导入变量
@import '@/styles/variables.scss';

.card {
  border-radius: $--border-radius-large;
  transition: box-shadow 0.3s;
  
  &:hover {
    box-shadow: $--box-shadow-dark;
  }
  
  .card-title {
    color: $--color-text-primary;
  }
}

// 头像样式
.avatar-container {
  width: 120px;
  height: 120px;
}

.avatar-wrapper {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  overflow: hidden;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.1);
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-upload {
  position: absolute;
  right: 0;
  bottom: 0;
}

.avatar-upload .btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.15);
}

// 账号统计卡片
.account-stats {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin: 20px 0;
  
  .stat-item {
    display: flex;
    align-items: center;
    padding: 10px;
    background-color: $--background-color-base;
    border-radius: $--border-radius-base;
    
    .stat-icon {
      width: 40px;
      height: 40px;
      border-radius: $--border-radius-base;
      display: flex;
      align-items: center;
      justify-content: center;
      margin-right: 12px;
      flex-shrink: 0;
      
      i {
        font-size: 18px;
      }
    }
    
    .stat-details {
      .stat-label {
        font-size: 12px;
        color: $--color-text-secondary;
        margin-bottom: 2px;
      }
      
      .stat-value {
        font-weight: 600;
        font-size: 14px;
        color: $--color-text-primary;
      }
    }
  }
}

// 按钮样式
.btn-action {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 42px;
  transition: all 0.3s;
  
  &:hover {
    transform: translateY(-2px);
  }
}

.btn-light-primary {
  background-color: rgba($--color-primary, 0.1);
  color: $--color-primary;
  border: none;
  
  &:hover {
    background-color: rgba($--color-primary, 0.2);
  }
}

.btn-light-danger {
  background-color: rgba($--color-danger, 0.1);
  color: $--color-danger;
  border: none;
  
  &:hover {
    background-color: rgba($--color-danger, 0.2);
  }
}

.btn-light-warning {
  background-color: rgba($--color-warning, 0.1);
  color: $--color-warning;
  border: none;
  
  &:hover {
    background-color: rgba($--color-warning, 0.2);
  }
}

// 表单样式
.profile-form {
  .form-label {
    color: $--color-text-secondary;
    margin-bottom: 0.5rem;
  }
  
  .input-group-text {
    background-color: transparent;
    border-right: none;
  }
  
  .form-control {
    border-left: none;
    height: 48px;
    
    &:focus {
      box-shadow: none;
      border-color: $--border-color-base;
      
      & + .input-group-text {
        border-color: $--border-color-base;
      }
    }
    
    &:disabled {
      background-color: $--background-color-base;
    }
  }
}

// 安全区块
.security-item {
  padding: 16px;
  border-radius: $--border-radius-base;
  transition: all 0.2s;
  
  &:hover {
    background-color: $--background-color-base;
  }
  
  .security-icon {
    width: 48px;
    height: 48px;
    border-radius: $--border-radius-base;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-right: 16px;
    flex-shrink: 0;
    
    i {
      font-size: 20px;
    }
  }
}

// 模态框样式
.modal-content {
  .modal-icon {
    width: 70px;
    height: 70px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    margin: 0 auto;
    
    i {
      font-size: 30px;
    }
  }
}

.bg-light-primary { background-color: rgba($--color-primary, 0.1); }
.bg-light-success { background-color: rgba($--color-success, 0.1); }
.bg-light-warning { background-color: rgba($--color-warning, 0.1); }
.bg-light-danger { background-color: rgba($--color-danger, 0.1); }

.badge-soft-primary {
  background-color: rgba(13, 110, 253, 0.1);
  color: #0d6efd;
}

.badge-soft-info {
  background-color: rgba(13, 202, 240, 0.1);
  color: #0dcaf0;
}
</style> 