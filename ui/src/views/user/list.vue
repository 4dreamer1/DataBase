<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue';
import userService from '@/api/user';
import { Modal } from 'bootstrap';
import { ElMessage } from 'element-plus';
import { useUserStore } from '@/store/modules/user';
// 类型只在注释中使用
// import { User, UserQuery } from '@/types/user';

// 用户状态管理
const userStore = useUserStore();
const isAdmin = computed(() => userStore.isAdmin);

// 状态变量
const loading = ref(false);
const users = ref([]); // 用户列表
const searchQuery = ref('');
const roleFilter = ref('');
const statusFilter = ref('');
const currentPage = ref(1);
const pageSize = ref(10);
const totalItems = ref(0);
const searchTimeout = ref(null);
const saveLoading = ref(false); // 添加保存状态变量

// 用户表单
const isEdit = ref(false);
const userForm = ref({
  id: null,
  username: '',
  name: '',
  email: '',
  phone: '',
  role: 'ROLE_USER',
  department: '',
  status: 0,
  password: ''
});

// 表单验证
const formErrors = ref({});

// 选中的用户
const selectedUser = ref(null);

// Modal实例
let userModal;
let deleteModal;
let resetPasswordModal;

// 角色列表
const roles = [
  { value: 'ROLE_ADMIN', label: '管理员' },
  { value: 'ROLE_USER', label: '普通用户' }
];

// 状态列表
const statusOptions = [
  { value: 0, label: '正常', class: 'bg-success' },
  { value: 1, label: '禁用', class: 'bg-danger' }
];

// 计算属性
const totalPages = computed(() => Math.ceil(totalItems.value / pageSize.value) || 1);

const paginationPages = computed(() => {
  const maxVisiblePages = 5;
  const pages = [];
  
  let startPage = Math.max(1, currentPage.value - Math.floor(maxVisiblePages / 2));
  let endPage = Math.min(totalPages.value, startPage + maxVisiblePages - 1);
  
  if (endPage - startPage + 1 < maxVisiblePages) {
    startPage = Math.max(1, endPage - maxVisiblePages + 1);
  }
  
  for (let i = startPage; i <= endPage; i++) {
    pages.push(i);
  }
  
  return pages;
});

// 生命周期钩子
onMounted(() => {
  userModal = new Modal(document.getElementById('userModal'));
  deleteModal = new Modal(document.getElementById('deleteModal'));
  resetPasswordModal = new Modal(document.getElementById('resetPasswordModal'));
  
  // 初始加载用户列表
  fetchUsers();
  
  // 移除定时刷新功能，避免不必要的API调用
});

// 获取用户列表
const fetchUsers = async (forceRefresh = false) => {
  if (loading.value) {
    return;
  }
  
  loading.value = true;
  
  try {
    const query = {
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      keyword: searchQuery.value || undefined,
      role: roleFilter.value || undefined,
      status: statusFilter.value !== '' ? parseInt(statusFilter.value) : undefined,
      _t: Date.now() // 添加时间戳参数确保每次请求都不同
    };

    // 根据是否强制刷新选择不同的方法
    const res = forceRefresh 
      ? await userService.forceRefreshUsers(query)
      : await userService.getUsers(query);

    if (res && res.data) {
      users.value = res.data.list || [];
      totalItems.value = res.data.total || 0;
      
      // 调试输出用户数据

    } else {
      console.error('获取用户列表响应格式错误:', res);
      users.value = [];
      totalItems.value = 0;
    }
  } catch (error) {
    console.error('获取用户列表失败', error);
    ElMessage.error('获取用户列表失败，请稍后再试');
    users.value = [];
    totalItems.value = 0;
  } finally {
    loading.value = false;
  }
};

// 添加用户
const openAddDialog = () => {
  // 检查是否有管理员权限
  if (!isAdmin.value) {
    ElMessage.warning('您没有添加用户的权限');
    return;
  }

  // 重置表单和错误
  formErrors.value = {};
  isEdit.value = false;
  userForm.value = {
    id: null,
    username: '',
    name: '',
    email: '',
    phone: '',
    role: 'ROLE_USER',
    department: '',
    status: 0,
    password: ''
  };
  saveLoading.value = false; // 重置保存状态
  userModal.show();
};

// 编辑用户
const openEditDialog = (user) => {
  formErrors.value = {};
  isEdit.value = true;
  userForm.value = {
    id: user.id,
    username: user.username,
    name: user.name || '',
    email: user.email,
    phone: user.phone || '',
    role: user.role || (user.roles && user.roles.length > 0 ? 
      (typeof user.roles[0] === 'object' ? user.roles[0].name : user.roles[0]) : 'ROLE_USER'),
    department: user.department || '',
    status: user.status,
    password: '' // 编辑时不设置密码
  };
  userModal.show();
};

// 确认删除
const confirmDelete = (user) => {
  selectedUser.value = user;
  deleteModal.show();
};

// 确认重置密码
const confirmResetPassword = (user) => {
  selectedUser.value = user;
  resetPasswordModal.show();
};

// 保存用户
const saveUser = async () => {
  // 表单验证
  formErrors.value = validateUserForm();
  if (Object.keys(formErrors.value).length > 0) {
    // 显示第一个错误信息
    ElMessage.warning(Object.values(formErrors.value)[0]);
    return;
  }
  
  saveLoading.value = true; // 使用单独的保存状态变量
  
  try {
    if (isEdit.value) {
      await userService.updateUser(userForm.value.id, userForm.value);
      ElMessage.success('用户更新成功');
    } else {
      await userService.createUser(userForm.value);
      ElMessage.success('用户创建成功');
    }
    
    userModal.hide();
    await fetchUsers(true);
  } catch (error) {
    console.error('保存用户失败', error);
    if (error.response?.data?.message) {
      ElMessage.error(error.response.data.message);
    } else {
      ElMessage.error('保存用户失败，请稍后再试');
    }
  } finally {
    saveLoading.value = false; // 重置保存状态
  }
};

// 验证用户表单
const validateUserForm = () => {
  const errors = {};
  
  // 用户名验证
  if (!userForm.value.username) {
    errors.username = '请输入用户名';
  } else if (userForm.value.username.length < 3) {
    errors.username = '用户名长度至少为3个字符';
  }
  
  // 邮箱验证
  if (!userForm.value.email) {
    errors.email = '请输入邮箱';
  } else {
    const emailRegex = /^[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,6}$/;
    if (!emailRegex.test(userForm.value.email)) {
      errors.email = '请输入有效的邮箱地址';
    }
  }
  
  // 姓名验证
  if (!userForm.value.name) {
    errors.name = '请输入姓名';
  }
  
  // 密码验证（仅在添加用户时或编辑时填写了密码）
  if (!isEdit.value && !userForm.value.password) {
    errors.password = '请输入密码';
  } else if (userForm.value.password && userForm.value.password.length < 6) {
    errors.password = '密码长度至少为6个字符';
  }
  
  return errors;
};

// 删除用户
const deleteUser = async () => {
  if (!selectedUser.value) return;
  
  try {
    loading.value = true;
    await userService.deleteUser(selectedUser.value.id);
    deleteModal.hide();
    ElMessage.success(`用户 ${selectedUser.value.username} 已成功删除`);
    await fetchUsers(true);
  } catch (error) {
    console.error('删除用户失败', error);
    ElMessage.error('删除用户失败: ' + (error.response?.data?.message || '请稍后再试'));
  } finally {
    loading.value = false;
  }
};

// 重置密码
const resetPassword = async () => {
  if (!selectedUser.value) return;
  
  try {
    loading.value = true;
    await userService.resetPassword(selectedUser.value.id);
    resetPasswordModal.hide();
    ElMessage.success(`用户 ${selectedUser.value.username} 的密码已重置为默认密码`);
  } catch (error) {
    console.error('重置密码失败', error);
    ElMessage.error('重置密码失败: ' + (error.response?.data?.message || '请稍后再试'));
  } finally {
    loading.value = false;
  }
};

// 切换用户状态
const toggleUserStatus = async (user) => {
  try {
    loading.value = true;
    const newStatus = user.status === 0 ? 1 : 0;
    await userService.toggleUserStatus(user.id, newStatus);
    await fetchUsers(true);
  } catch (error) {
    console.error('切换用户状态失败', error);
    ElMessage.error('切换用户状态失败: ' + (error.response?.data?.message || '请稍后再试'));
  } finally {
    loading.value = false;
  }
};

// 搜索功能：防抖处理
const handleSearch = () => {
  if (searchTimeout.value) {
    clearTimeout(searchTimeout.value);
  }
  
  searchTimeout.value = setTimeout(() => {
    currentPage.value = 1;
    fetchUsers();
  }, 300);
};

// 切换页码
const changePage = (page) => {
  if (page < 1 || page > totalPages.value) return;
  currentPage.value = page;
  fetchUsers();
};

// 处理每页显示数量变化
const handlePageSizeChange = () => {
  currentPage.value = 1; // 重置为第一页
  fetchUsers();
};

// 获取角色名称
const getRoleName = (role) => {
  if (!role) return '普通用户';
  
  const roleValue = typeof role === 'object' ? (role.name || '') : role;
  const found = roles.find(r => r.value === roleValue);
  
  if (found) {
    return found.label;
  }
  
  // 处理不同格式的角色名
  if (roleValue.includes && roleValue.includes('ADMIN')) {
    return '管理员';
  } else if (roleValue.includes && roleValue.includes('USER')) {
    return '普通用户';
  }
  
  return roleValue || '普通用户';
};

// 获取状态信息
const getStatusInfo = (status) => {
  const found = statusOptions.find(s => s.value === status);
  return found || { label: '未知', class: 'bg-secondary' };
};

// 格式化日期
const formatDate = (dateString) => {
  if (!dateString) return '-';
  const date = new Date(dateString);
  return date.toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' });
};

// 获取用户角色
const getUserRoles = (user) => {
  if (!user) return ['ROLE_USER'];
  
  if (user.roles && Array.isArray(user.roles) && user.roles.length > 0) {
    return user.roles;
  } else if (user.role) {
    return [user.role];
  } else {
    return ['ROLE_USER'];
  }
};

// 生成随机密码
const generatePassword = () => {
  const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()';
  let password = '';
  // 确保包含至少一个数字、一个小写字母、一个大写字母和一个特殊字符
  password += chars.charAt(Math.floor(Math.random() * 26) + 26); // 小写字母
  password += chars.charAt(Math.floor(Math.random() * 26)); // 大写字母
  password += chars.charAt(Math.floor(Math.random() * 10) + 52); // 数字
  password += chars.charAt(Math.floor(Math.random() * 10) + 62); // 特殊字符
  
  // 添加额外的随机字符，使密码长度达到10位
  for (let i = 0; i < 6; i++) {
    password += chars.charAt(Math.floor(Math.random() * chars.length));
  }
  
  // 打乱字符顺序
  return password.split('').sort(() => 0.5 - Math.random()).join('');
};

// 刷新按钮处理函数
const handleRefresh = () => {
  if (loading.value) return;
  loading.value = true;
  ElMessage.info('正在刷新用户列表...');
  fetchUsers(true)
    .finally(() => {
      loading.value = false;
    });
};
</script>

<template>
  <div class="container-fluid py-4">
    <div class="card shadow-sm mb-4">
      <div class="card-header bg-white py-3">
        <div class="row align-items-center">
          <div class="col">
            <h5 class="mb-0 fw-bold text-primary">用户管理</h5>
          </div>
          <div class="col-auto">
            <button class="btn btn-outline-primary me-2" @click="handleRefresh" :disabled="loading">
              <i class="bi bi-arrow-clockwise me-1" :class="{ 'spin': loading }"></i> 
              {{ loading ? '刷新中...' : '刷新列表' }}
            </button>
            <button class="btn btn-primary" @click="openAddDialog" :disabled="!isAdmin">
              <i class="bi bi-person-plus me-1"></i> 添加用户
            </button>
          </div>
        </div>
      </div>
      
      <div class="card-body">
        <!-- 搜索和筛选 -->
        <div class="row g-3 mb-4">
          <div class="col-md-5">
            <div class="input-group">
              <span class="input-group-text bg-light border-end-0">
                <i class="bi bi-search text-muted"></i>
              </span>
              <input 
                type="text" 
                class="form-control border-start-0" 
                placeholder="搜索用户名、邮箱或部门..." 
                v-model="searchQuery"
                @input="handleSearch"
              >
            </div>
          </div>
          <div class="col-md-2">
            <select class="form-select" v-model="roleFilter" @change="fetchUsers">
              <option value="">全部角色</option>
              <option v-for="role in roles" :key="role.value" :value="role.value">
                {{ role.label }}
              </option>
            </select>
          </div>
          <div class="col-md-2">
            <select class="form-select" v-model="statusFilter" @change="fetchUsers">
              <option value="">全部状态</option>
              <option v-for="status in statusOptions" :key="status.value" :value="status.value">
                {{ status.label }}
              </option>
            </select>
          </div>
          <div class="col-md-3 text-end">
            <span class="text-muted">共 {{ totalItems }} 条记录</span>
          </div>
        </div>
        
        <!-- 用户列表表格 -->
        <div class="table-responsive">
          <table class="table table-hover align-middle">
            <thead class="table-light">
              <tr>
                <th scope="col" style="width: 50px;">#</th>
                <th scope="col">用户信息</th>
                <th scope="col">邮箱</th>
                <th scope="col">部门</th>
                <th scope="col" style="width: 100px;">角色</th>
                <th scope="col" style="width: 100px;">状态</th>
                <th scope="col" style="width: 180px;" class="text-end">操作</th>
              </tr>
            </thead>
            <tbody>
              <template v-if="loading && users.length === 0">
                <tr v-for="i in 5" :key="i">
                  <td colspan="7" class="p-3">
                    <div class="placeholder-glow">
                      <span class="placeholder col-12"></span>
                    </div>
                  </td>
                </tr>
              </template>
              <template v-else-if="users.length === 0">
                <tr>
                  <td colspan="7" class="text-center py-5">
                    <div class="text-muted">
                      <i class="bi bi-people fs-2 d-block mb-2"></i>
                      <p>暂无用户数据</p>
                      <div class="mt-3">
                        <button class="btn btn-sm btn-outline-primary" @click="handleRefresh">
                          <i class="bi bi-arrow-clockwise me-1"></i> 刷新列表
                        </button>
                      </div>
                    </div>
                  </td>
                </tr>
              </template>
              <template v-else>
                <tr v-for="(user, index) in users" :key="user.id || index">
                  <td>{{ (currentPage - 1) * pageSize + index + 1 }}</td>
                  <td>
                    <div class="d-flex align-items-center">
                      <div class="avatar-circle bg-light me-2">
                        <img v-if="user.avatar" :src="user.avatar" class="avatar-image" alt="用户头像" />
                        <span v-else>{{ user.username && user.username.charAt(0) ? user.username.charAt(0).toUpperCase() : '?' }}</span>
                      </div>
                      <div>
                        <div class="fw-medium">{{ user.username || '未知用户' }}</div>
                        <small class="text-muted">{{ user.name || '-' }}</small>
                      </div>
                    </div>
                  </td>
                  <td>{{ user.email || '-' }}</td>
                  <td>{{ user.department || '-' }}</td>
                  <td>
                    <span 
                      v-for="role in getUserRoles(user)" 
                      :key="role" 
                      class="badge rounded-pill me-1" 
                      :class="role && role.includes && role.includes('ADMIN') ? 'bg-primary' : 'bg-info'"
                    >
                      {{ getRoleName(role) }}
                    </span>
                  </td>
                  <td>
                    <span class="badge rounded-pill" :class="getStatusInfo(user.status).class">
                      {{ getStatusInfo(user.status).label }}
                    </span>
                  </td>
                  <td class="text-end">
                    <div class="btn-group btn-group-sm">
                      <button class="btn btn-outline-primary" title="编辑用户" @click="openEditDialog(user)" :disabled="!isAdmin">
                        <i class="bi bi-pencil"></i>
                      </button>
                      <button class="btn btn-outline-warning" title="重置密码" @click="confirmResetPassword(user)" :disabled="!isAdmin">
                        <i class="bi bi-key"></i>
                      </button>
                      <button 
                        class="btn"
                        :class="user.status === 0 ? 'btn-outline-danger' : 'btn-outline-success'"
                        :title="user.status === 0 ? '禁用用户' : '启用用户'"
                        @click="toggleUserStatus(user)"
                        :disabled="!isAdmin"
                      >
                        <i :class="user.status === 0 ? 'bi bi-slash-circle' : 'bi bi-check-circle'"></i>
                      </button>
                      <button class="btn btn-outline-danger" title="删除用户" @click="confirmDelete(user)" :disabled="!isAdmin">
                        <i class="bi bi-trash"></i>
                      </button>
                    </div>
                  </td>
                </tr>
              </template>
            </tbody>
          </table>
        </div>
        
        <!-- 分页 -->
        <nav v-if="totalItems > 0" class="mt-4">
          <div class="row align-items-center">
            <div class="col-auto">
              <select class="form-select form-select-sm" v-model="pageSize" @change="handlePageSizeChange">
                <option :value="10">10条/页</option>
                <option :value="20">20条/页</option>
                <option :value="50">50条/页</option>
              </select>
            </div>
            <div class="col d-flex justify-content-center">
              <ul class="pagination pagination-sm mb-0">
                <li class="page-item" :class="{ disabled: currentPage <= 1 }">
                  <a class="page-link" href="#" @click.prevent="changePage(1)" aria-label="首页">
                    <i class="bi bi-chevron-double-left"></i>
                  </a>
                </li>
                <li class="page-item" :class="{ disabled: currentPage <= 1 }">
                  <a class="page-link" href="#" @click.prevent="changePage(currentPage - 1)" aria-label="上一页">
                    <i class="bi bi-chevron-left"></i>
                  </a>
                </li>
                <li v-for="page in paginationPages" :key="page" class="page-item" :class="{ active: page === currentPage }">
                  <a class="page-link" href="#" @click.prevent="changePage(page)">{{ page }}</a>
                </li>
                <li class="page-item" :class="{ disabled: currentPage >= totalPages }">
                  <a class="page-link" href="#" @click.prevent="changePage(currentPage + 1)" aria-label="下一页">
                    <i class="bi bi-chevron-right"></i>
                  </a>
                </li>
                <li class="page-item" :class="{ disabled: currentPage >= totalPages }">
                  <a class="page-link" href="#" @click.prevent="changePage(totalPages)" aria-label="末页">
                    <i class="bi bi-chevron-double-right"></i>
                  </a>
                </li>
              </ul>
            </div>
            <div class="col-auto text-muted small">
              第 {{ currentPage }}/{{ totalPages }} 页，共 {{ totalItems }} 条记录
            </div>
          </div>
        </nav>
      </div>
    </div>
    
    <!-- 添加/编辑用户对话框 -->
    <div class="modal fade" id="userModal" tabindex="-1" aria-hidden="true">
      <div class="modal-dialog modal-lg">
        <div class="modal-content">
          <div class="modal-header">
            <h5 class="modal-title">{{ isEdit ? '编辑' : '添加' }}用户</h5>
            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
          </div>
          <div class="modal-body">
            <form @submit.prevent="saveUser" class="needs-validation" novalidate>
              <div class="row">
                <div class="col-md-6">
                  <div class="mb-3">
                    <label for="username" class="form-label">用户名 <span class="text-danger">*</span></label>
                    <input 
                      type="text" 
                      class="form-control" 
                      :class="{'is-invalid': formErrors.username}"
                      id="username" 
                      v-model="userForm.username"
                      :disabled="isEdit"
                      required
                      minlength="3"
                      maxlength="20"
                    >
                    <div v-if="formErrors.username" class="invalid-feedback">{{ formErrors.username }}</div>
                    <div v-else-if="isEdit" class="form-text text-muted">用户名不可修改</div>
                    <div v-else class="form-text text-muted">用户名长度为3-20个字符</div>
                  </div>
                </div>
                <div class="col-md-6">
                  <div class="mb-3">
                    <label for="name" class="form-label">姓名 <span class="text-danger">*</span></label>
                    <input 
                      type="text" 
                      class="form-control" 
                      :class="{'is-invalid': formErrors.name}"
                      id="name" 
                      v-model="userForm.name" 
                      required
                      maxlength="50"
                    >
                    <div v-if="formErrors.name" class="invalid-feedback">{{ formErrors.name }}</div>
                  </div>
                </div>
              </div>
              
              <div class="row">
                <div class="col-md-6">
                  <div class="mb-3">
                    <label for="email" class="form-label">邮箱 <span class="text-danger">*</span></label>
                    <input 
                      type="email" 
                      class="form-control" 
                      :class="{'is-invalid': formErrors.email}"
                      id="email" 
                      v-model="userForm.email" 
                      required
                      maxlength="50"
                    >
                    <div v-if="formErrors.email" class="invalid-feedback">{{ formErrors.email }}</div>
                  </div>
                </div>
                <div class="col-md-6">
                  <div class="mb-3">
                    <label for="phone" class="form-label">电话</label>
                    <input 
                      type="tel" 
                      class="form-control" 
                      id="phone" 
                      v-model="userForm.phone"
                      maxlength="20"
                    >
                  </div>
                </div>
              </div>
              
              <div class="row">
                <div class="col-md-6">
                  <div class="mb-3">
                    <label for="role" class="form-label">角色 <span class="text-danger">*</span></label>
                    <select class="form-select" id="role" v-model="userForm.role" required>
                      <option v-for="role in roles" :key="role.value" :value="role.value">
                        {{ role.label }}
                      </option>
                    </select>
                  </div>
                </div>
                <div class="col-md-6">
                  <div class="mb-3">
                    <label for="department" class="form-label">部门</label>
                    <input 
                      type="text" 
                      class="form-control" 
                      id="department" 
                      v-model="userForm.department"
                      maxlength="100"
                    >
                  </div>
                </div>
              </div>
              
              <div class="row">
                <div class="col-md-6">
                  <div class="mb-3">
                    <label for="status" class="form-label">状态</label>
                    <select class="form-select" id="status" v-model="userForm.status">
                      <option v-for="status in statusOptions" :key="status.value" :value="status.value">
                        {{ status.label }}
                      </option>
                    </select>
                  </div>
                </div>
                <div class="col-md-6">
                  <div class="mb-3">
                    <label for="password" class="form-label">
                      密码 <span v-if="!isEdit" class="text-danger">*</span>
                    </label>
                    <div class="input-group">
                      <input 
                        type="password" 
                        class="form-control" 
                        :class="{'is-invalid': formErrors.password}"
                        id="password" 
                        v-model="userForm.password" 
                        :required="!isEdit"
                        minlength="6"
                        maxlength="40"
                        autocomplete="new-password"
                      >
                      <button class="btn btn-outline-secondary" type="button" @click="userForm.password = generatePassword()">
                        <i class="bi bi-magic"></i> 生成密码
                      </button>
                    </div>
                    <div v-if="formErrors.password" class="invalid-feedback">{{ formErrors.password }}</div>
                    <div v-else class="form-text text-muted">
                      {{ isEdit ? '留空表示不修改密码' : '密码长度至少为6个字符' }}
                    </div>
                  </div>
                </div>
              </div>
            </form>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">取消</button>
            <button type="button" class="btn btn-primary" @click="saveUser" :disabled="saveLoading">
              <span v-if="saveLoading" class="spinner-border spinner-border-sm me-1" role="status" aria-hidden="true"></span>
              {{ saveLoading ? '保存中...' : '保存' }}
            </button>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 删除确认对话框 -->
    <div class="modal fade" id="deleteModal" tabindex="-1" aria-hidden="true">
      <div class="modal-dialog modal-sm">
        <div class="modal-content">
          <div class="modal-header border-bottom-0">
            <h5 class="modal-title text-danger"><i class="bi bi-exclamation-triangle me-2"></i>删除用户</h5>
            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
          </div>
          <div class="modal-body">
            <p>您确定要删除用户 <strong>{{ selectedUser?.username }}</strong> 吗？</p>
            <div class="alert alert-warning">
              <i class="bi bi-exclamation-triangle-fill me-2"></i>
              此操作不可恢复，用户的所有数据将被永久删除。
            </div>
          </div>
          <div class="modal-footer border-top-0">
            <button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">取消</button>
            <button type="button" class="btn btn-danger" @click="deleteUser" :disabled="loading">
              <span v-if="loading" class="spinner-border spinner-border-sm me-1" role="status" aria-hidden="true"></span>
              确认删除
            </button>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 重置密码确认对话框 -->
    <div class="modal fade" id="resetPasswordModal" tabindex="-1" aria-hidden="true">
      <div class="modal-dialog modal-sm">
        <div class="modal-content">
          <div class="modal-header border-bottom-0">
            <h5 class="modal-title"><i class="bi bi-key me-2"></i>重置密码</h5>
            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
          </div>
          <div class="modal-body">
            <p>您确定要重置用户 <strong>{{ selectedUser?.username }}</strong> 的密码吗？</p>
            <div class="alert alert-info">
              <i class="bi bi-info-circle-fill me-2"></i>
              密码将被重置为系统默认密码。
            </div>
          </div>
          <div class="modal-footer border-top-0">
            <button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">取消</button>
            <button type="button" class="btn btn-warning" @click="resetPassword" :disabled="loading">
              <span v-if="loading" class="spinner-border spinner-border-sm me-1" role="status" aria-hidden="true"></span>
              重置密码
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.table th {
  font-weight: 600;
  font-size: 0.8rem;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.card {
  border-radius: 0.75rem;
  border: none;
  box-shadow: 0 0.125rem 0.25rem rgba(0, 0, 0, 0.075);
}

.avatar-circle {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  color: var(--bs-primary);
  overflow: hidden;
}

.avatar-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.badge-soft-primary {
  color: var(--bs-primary);
  background-color: rgba(var(--bs-primary-rgb), 0.1);
}

.badge-soft-info {
  color: var(--bs-info);
  background-color: rgba(var(--bs-info-rgb), 0.1);
}

.badge-soft-success {
  color: var(--bs-success);
  background-color: rgba(var(--bs-success-rgb), 0.1);
}

.badge-soft-danger {
  color: var(--bs-danger);
  background-color: rgba(var(--bs-danger-rgb), 0.1);
}

.badge-soft-warning {
  color: var(--bs-warning);
  background-color: rgba(var(--bs-warning-rgb), 0.1);
}

.btn-group .btn {
  border-radius: 0;
}

.btn-group .btn:first-child {
  border-top-left-radius: 0.25rem;
  border-bottom-left-radius: 0.25rem;
}

.btn-group .btn:last-child {
  border-top-right-radius: 0.25rem;
  border-bottom-right-radius: 0.25rem;
}

/* 表格行悬停效果 */
.table tbody tr:hover {
  background-color: rgba(0, 123, 255, 0.03);
}

/* 占位符动画 */
@keyframes placeholderShimmer {
  0% {
    background-position: -468px 0;
  }
  100% {
    background-position: 468px 0;
  }
}

.placeholder-glow .placeholder {
  animation: placeholderShimmer 1.5s linear infinite;
  background: linear-gradient(to right, #f6f7f8 8%, #edeef1 18%, #f6f7f8 33%);
  background-size: 800px 104px;
  height: 12px;
  border-radius: 4px;
}

.modal-header, .modal-footer {
  padding: 1rem 1.5rem;
}

.modal-body {
  padding: 1.5rem;
}

.form-control:focus, .form-select:focus {
  border-color: #80bdff;
  box-shadow: 0 0 0 0.25rem rgba(0, 123, 255, 0.25);
}

.form-control.is-invalid:focus, .form-select.is-invalid:focus {
  border-color: #dc3545;
  box-shadow: 0 0 0 0.25rem rgba(220, 53, 69, 0.25);
}

/* 添加刷新图标旋转动画 */
.spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}
</style> 