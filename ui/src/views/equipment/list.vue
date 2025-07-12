<template>
  <div class="container-fluid py-4 fade-in">
    <!-- 页面标题 -->
    <div class="row mb-4">
      <div class="col-12">
        <div class="card shadow-sm border-0 rounded-4">
          <div class="card-body d-flex justify-content-between align-items-center py-3">
            <div class="d-flex align-items-center">
              <div class="page-icon-wrapper me-3">
                <i class="bi bi-box text-primary"></i>
              </div>
              <div>
                <h4 class="mb-0 fw-bold">装备列表</h4>
                <p class="text-muted small mb-0">管理和查看所有装备信息</p>
              </div>
            </div>
            <div class="d-flex gap-2">
              <button 
                class="btn btn-light-primary btn-icon rounded-circle"
                @click="refreshData"
                title="刷新数据">
                <i class="bi bi-arrow-clockwise"></i>
              </button>
              <button 
                class="btn btn-light-success btn-icon-text"
                @click="showExportDialog"
                title="导出数据">
                <i class="bi bi-download me-1"></i>导出
              </button>
              <button 
                class="btn btn-light-primary btn-icon-text"
                @click="showImportDialog"
                title="导入数据">
                <i class="bi bi-upload me-1"></i>导入
              </button>
              <button 
                v-if="isAdmin && selectedEquipments.length > 0" 
                class="btn btn-light-danger btn-icon-text" 
                @click="confirmBulkDelete">
                <i class="bi bi-trash me-1"></i>批量删除 ({{ selectedEquipments.length }})
              </button>
              <button 
                v-if="isAdmin" 
                class="btn btn-primary btn-icon-text" 
                @click="goToCreateEquipment">
                <i class="bi bi-plus-lg me-1"></i>添加装备
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 搜索与筛选 -->
    <div class="row mb-4">
      <div class="col-12">
        <div class="card shadow-sm border-0 rounded-4">
          <div class="card-body">
            <form @submit.prevent="loadEquipments" class="search-form">
              <div class="row g-3">
                <!-- 关键词搜索 -->
                <div class="col-md-4">
                  <div class="search-input-group">
                    <div class="search-icon">
                      <i class="bi bi-search"></i>
                    </div>
                    <input 
                      type="text" 
                      class="form-control form-control-lg border-0 ps-5" 
                      placeholder="搜索装备名称或编号" 
                      v-model="searchQuery.keyword"
                    >
                  </div>
                </div>
                
                <!-- 分类筛选 -->
                <div class="col-md-3">
                  <select class="form-select form-select-lg" v-model="searchQuery.categoryId">
                    <option :value="null">全部分类</option>
                    <option v-for="category in categories" :key="category.id" :value="category.id">
                      {{ category.name }}
                    </option>
                  </select>
                </div>
                
                <!-- 状态筛选 -->
                <div class="col-md-3">
                  <select class="form-select form-select-lg" v-model="searchQuery.status">
                    <option :value="null">全部状态</option>
                    <option value="可用">可用</option>
                    <option value="维修中">维修中</option>
                    <option value="不可用">不可用</option>
                  </select>
                </div>
                
                <!-- 搜索按钮 -->
                <div class="col-md-2 d-flex gap-2">
                  <button type="submit" class="btn btn-primary btn-lg flex-grow-1 btn-icon-text">
                    <i class="bi bi-search me-1"></i>搜索
                  </button>
                  <button 
                    type="button" 
                    class="btn btn-light btn-lg" 
                    @click="clearFilters"
                    v-if="isFiltered"
                    title="清除筛选条件"
                  >
                    <i class="bi bi-x-lg"></i>
                  </button>
                </div>
              </div>
            </form>
          </div>
        </div>
      </div>
    </div>

    <!-- 数据表格 -->
    <div class="row">
      <div class="col-12">
        <div class="card shadow-sm border-0 rounded-4 overflow-hidden">
          <!-- 搜索结果摘要 -->
          <div v-if="isFiltered && !loading" class="d-flex justify-content-between align-items-center p-3 border-bottom bg-light-primary">
            <div class="d-flex align-items-center flex-wrap">
              <span class="badge bg-primary me-2">
                <i class="bi bi-filter me-1"></i>已筛选
              </span>
              <span v-if="searchQuery.keyword" class="filter-tag me-2">
                关键词: <strong>{{ searchQuery.keyword }}</strong>
              </span>
              <span v-if="searchQuery.categoryId" class="filter-tag me-2">
                分类: <strong>{{ getCategoryName(searchQuery.categoryId) }}</strong>
              </span>
              <span v-if="searchQuery.status" class="filter-tag me-2">
                状态: <strong>{{ searchQuery.status }}</strong>
              </span>
              <span class="text-muted ms-2">
                共找到 <strong>{{ totalItems }}</strong> 条结果
              </span>
            </div>
            <button 
              type="button" 
              class="btn btn-sm btn-light-secondary" 
              @click="clearFilters"
            >
              <i class="bi bi-x-circle me-1"></i>
              清除筛选
            </button>
          </div>
          
          <!-- 表格内容 -->
          <div class="table-responsive">
            <table class="table table-hover align-middle mb-0 modern-table">
              <thead class="table-light">
                <tr>
                  <th scope="col" width="40" class="ps-3">
                    <div class="form-check" v-if="isAdmin && equipments.length > 0">
                      <input 
                        class="form-check-input" 
                        type="checkbox" 
                        id="selectAll" 
                        :checked="isAllSelected"
                        @change="toggleSelectAll"
                      >
                    </div>
                  </th>
                  <th scope="col" width="60">序号</th>
                  <th scope="col">名称</th>
                  <th scope="col">序列号</th>
                  <th scope="col">分类</th>
                  <th scope="col" class="text-center">总数量</th>
                  <th scope="col" class="text-center">可用数量</th>
                  <th scope="col" class="text-center">状态</th>
                  <th scope="col" width="150" class="text-center">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="loading">
                  <td colspan="9" class="py-5">
                    <div class="d-flex flex-column align-items-center">
                      <div class="spinner-grow text-primary mb-3" role="status">
                        <span class="visually-hidden">加载中...</span>
                      </div>
                      <p class="text-muted mb-0">加载数据中，请稍候...</p>
                    </div>
                  </td>
                </tr>
                <tr v-else-if="equipments.length === 0">
                  <td colspan="9" class="py-5">
                    <div class="empty-state text-center">
                      <div class="empty-state-icon">
                        <i class="bi bi-inbox"></i>
                      </div>
                      <h5 class="mt-3">暂无数据</h5>
                      <p class="text-muted">没有找到符合条件的装备信息</p>
                      <button class="btn btn-light-primary" @click="clearFilters" v-if="isFiltered">
                        <i class="bi bi-x-circle me-1"></i>清除筛选条件
                      </button>
                    </div>
                  </td>
                </tr>
                <tr v-for="(equipment, index) in equipments" :key="equipment.id" class="equipment-row">
                  <td class="ps-3" v-if="isAdmin">
                    <div class="form-check">
                      <input 
                        class="form-check-input" 
                        type="checkbox" 
                        :id="`equipment-${equipment.id}`" 
                        v-model="selectedEquipments"
                        :value="equipment.id"
                      >
                    </div>
                  </td>
                  <td>
                    <span class="index-badge">{{ (currentPage - 1) * pageSize + index + 1 }}</span>
                  </td>
                  <td>
                    <div class="equipment-name">
                      <a 
                        href="javascript:void(0)" 
                        @click="goToDetail(equipment.id)" 
                        class="text-decoration-none fw-medium text-primary"
                      >
                        {{ equipment.name }}
                      </a>
                      <small v-if="equipment.description" class="d-block text-muted equipment-description">
                        {{ truncateText(equipment.description, 60) }}
                      </small>
                    </div>
                  </td>
                  <td>
                    <span class="serial-number">{{ equipment.serialNumber }}</span>
                  </td>
                  <td>
                    <span class="category-badge">
                      {{ equipment.category ? equipment.category.name : '未分类' }}
                    </span>
                  </td>
                  <td class="text-center">
                    <span class="fw-medium">{{ equipment.quantity }}</span>
                  </td>
                  <td class="text-center">
                    <span 
                      :class="[
                        'quantity-badge',
                        equipment.availableQuantity <= 0 ? 'bg-danger' : 
                        equipment.availableQuantity < equipment.quantity * 0.2 ? 'bg-warning' : 'bg-success'
                      ]"
                    >
                      {{ equipment.availableQuantity }}
                    </span>
                  </td>
                  <td class="text-center">
                    <span 
                      :class="[
                        'status-badge',
                        equipment.availableQuantity <= 0 ? 'status-unavailable' :
                        equipment.status === '可用' ? 'status-available' : 
                        equipment.status === '维修中' ? 'status-maintenance' : 'status-unavailable'
                      ]"
                    >
                      <i :class="[
                        'status-icon bi',
                        equipment.availableQuantity <= 0 ? 'bi-x-circle-fill' :
                        equipment.status === '可用' ? 'bi-check-circle-fill' : 
                        equipment.status === '维修中' ? 'bi-tools' : 'bi-x-circle-fill'
                      ]"></i>
                      {{ equipment.availableQuantity <= 0 ? '不可用' : equipment.status }}
                    </span>
                  </td>
                  <td>
                    <div class="action-buttons">
                      <button type="button" class="btn btn-icon btn-light-primary" @click="goToDetail(equipment.id)" title="查看详情">
                        <i class="bi bi-eye"></i>
                      </button>
                      <button v-if="isAdmin" type="button" class="btn btn-icon btn-light-success" @click="goToEdit(equipment.id)" title="编辑装备">
                        <i class="bi bi-pencil"></i>
                      </button>
                      <button v-if="isAdmin" type="button" class="btn btn-icon btn-light-danger" @click="confirmDelete(equipment)" title="删除装备">
                        <i class="bi bi-trash"></i>
                      </button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          
          <!-- 分页 -->
          <div class="card-footer bg-white border-0 d-flex justify-content-between align-items-center py-3">
            <div class="text-muted small">
              共 <span class="fw-bold text-primary">{{ totalItems }}</span> 条记录
            </div>
            <div>
              <el-pagination
                layout="prev, pager, next"
                :total="totalItems"
                :page-size="pageSize"
                :current-page="currentPage"
                @current-change="handlePageChange"
                class="modern-pagination"
                background
                hide-on-single-page
              />
            </div>
            <div class="page-size-select d-flex align-items-center">
              <span class="text-muted me-2">每页显示</span>
              <select v-model="pageSize" class="form-select form-select-sm" @change="handlePageSizeChange">
                <option :value="10">10</option>
                <option :value="20">20</option>
                <option :value="50">50</option>
                <option :value="100">100</option>
              </select>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 删除确认对话框 -->
    <el-dialog
      v-model="deleteDialogVisible"
      title="确认删除"
      width="400px"
      center
      class="modern-dialog"
    >
      <div class="text-center py-3">
        <div class="dialog-icon mb-3">
          <i class="bi bi-exclamation-triangle"></i>
        </div>
        <h5 class="mb-3">确定要删除此装备吗？</h5>
        <p class="text-muted mb-0">
          {{ equipmentToDelete ? `您即将删除装备"${equipmentToDelete.name}"，此操作不可恢复。` : '此操作不可恢复。' }}
        </p>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <button type="button" class="btn btn-light me-2" @click="deleteDialogVisible = false">取消</button>
          <button type="button" class="btn btn-danger" @click="deleteEquipment" :disabled="deleteLoading">
            <i class="bi bi-trash me-1"></i>{{ deleteLoading ? '删除中...' : '确认删除' }}
          </button>
        </div>
      </template>
    </el-dialog>

    <!-- 批量删除确认对话框 -->
    <el-dialog
      v-model="bulkDeleteDialogVisible"
      title="批量删除"
      width="400px"
      center
      class="modern-dialog"
    >
      <div class="text-center py-3">
        <div class="dialog-icon mb-3 bg-danger">
          <i class="bi bi-exclamation-triangle"></i>
        </div>
        <h5 class="mb-3">确定要批量删除选中的装备吗？</h5>
        <p class="text-muted">此操作将删除 <span class="badge bg-danger">{{ selectedEquipments.length }}</span> 个装备项目，此操作不可恢复</p>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <button type="button" class="btn btn-light me-2" @click="bulkDeleteDialogVisible = false">取消</button>
          <button type="button" class="btn btn-danger" @click="bulkDelete" :disabled="bulkDeleteLoading">
            <i class="bi bi-trash me-1"></i>{{ bulkDeleteLoading ? '删除中...' : '确认删除' }}
          </button>
        </div>
      </template>
    </el-dialog>

    <!-- 导出对话框 -->
    <el-dialog v-model="exportDialogVisible" title="导出数据" width="400px" class="modern-dialog">
      <div class="p-3">
        <div class="mb-4 text-center">
          <div class="dialog-icon mb-3 bg-primary">
            <i class="bi bi-file-earmark-arrow-down"></i>
          </div>
          <h5 class="mb-3">选择导出格式</h5>
        </div>
        <div class="export-options">
          <div class="row g-3">
            <div class="col-6">
              <div 
                :class="['export-option', exportForm.format === 'csv' ? 'active' : '']"
                @click="exportForm.format = 'csv'"
              >
                <div class="export-icon">
                  <i class="bi bi-filetype-csv"></i>
                </div>
                <div class="export-label">CSV格式</div>
                <div class="export-desc">通用格式，可用Excel、文本编辑器等打开</div>
              </div>
            </div>
            <div class="col-6">
              <div 
                :class="['export-option', exportForm.format === 'excel' ? 'active' : '']"
                @click="exportForm.format = 'excel'"
              >
                <div class="export-icon">
                  <i class="bi bi-file-earmark-excel"></i>
                </div>
                <div class="export-label">Excel格式</div>
                <div class="export-desc">包含格式化内容，适合直接使用Excel打开</div>
              </div>
            </div>
          </div>
        </div>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <button class="btn btn-light me-2" @click="exportDialogVisible = false">取消</button>
          <button class="btn btn-primary" @click="exportData">
            <i class="bi bi-download me-1"></i>导出数据
          </button>
        </div>
      </template>
    </el-dialog>

    <!-- 导入对话框 -->
    <el-dialog v-model="importDialogVisible" title="导入数据" width="500px" class="modern-dialog">
      <div class="p-3">
        <div class="mb-4 text-center">
          <div class="dialog-icon mb-3 bg-primary">
            <i class="bi bi-file-earmark-arrow-up"></i>
          </div>
          <h5 class="mb-3">选择要导入的文件</h5>
          <p class="text-muted">支持Excel(.xlsx)和CSV(.csv)格式文件，请确保文件格式正确</p>
        </div>
        
        <div v-if="importLoading" class="import-progress text-center mb-4">
          <div class="progress mb-3">
            <div class="progress-bar progress-bar-striped progress-bar-animated bg-primary" role="progressbar" style="width: 100%"></div>
          </div>
          <p class="text-muted">正在处理数据，请耐心等待...</p>
        </div>
        
        <div class="import-form mt-4" v-if="!importLoading">
          <!-- 文件上传区域 -->
          <div 
            class="file-upload-area" 
            @dragover.prevent="onDragOver" 
            @dragleave.prevent="onDragLeave" 
            @drop.prevent="onFileDrop"
            :class="{ 'dragging': isDragging, 'has-file': importFile }"
          >
            <div v-if="!importFile" class="upload-placeholder">
              <i class="bi bi-cloud-arrow-up"></i>
              <p class="mb-2">拖拽文件到此处或点击上传</p>
              <p class="text-muted small">支持.xlsx和.csv格式</p>
            </div>
            <div v-else class="file-preview">
              <i :class="[
                'file-icon bi',
                importFile.name.endsWith('.xlsx') ? 'bi-file-earmark-excel' : 'bi-filetype-csv'
              ]"></i>
              <div class="file-info">
                <div class="file-name">{{ importFile.name }}</div>
                <div class="file-size">{{ formatFileSize(importFile.size) }}</div>
              </div>
              <button type="button" class="btn btn-sm btn-light-danger" @click="removeFile">
                <i class="bi bi-x-lg"></i>
              </button>
            </div>
            <input 
              type="file" 
              ref="fileInput" 
              class="file-input" 
              @change="onFileChange" 
              accept=".csv,.xlsx"
            >
          </div>
          
          <!-- 模板下载链接 -->
          <div class="template-download text-center mt-3">
            <span class="text-muted">需要模板？</span>
            <a href="javascript:void(0)" @click="downloadTemplate('csv')" class="ms-2">下载CSV模板</a>
            <a href="javascript:void(0)" @click="downloadTemplate('excel')" class="ms-2">下载Excel模板</a>
            
            <div class="format-info mt-3 text-start">
              <button 
                class="btn btn-sm btn-light-secondary d-inline-flex align-items-center" 
                type="button" 
                data-bs-toggle="collapse" 
                data-bs-target="#formatInfoCollapse" 
                aria-expanded="false" 
                aria-controls="formatInfoCollapse"
              >
                <i class="bi bi-info-circle me-1"></i>查看文件格式要求
              </button>
              
              <div class="collapse mt-2" id="formatInfoCollapse">
                <div class="card card-body">
                  <h6 class="mb-2">文件格式要求</h6>
                  <p class="small mb-2">导入文件必须包含以下列：</p>
                  <ul class="small mb-0 ps-3">
                    <li><strong>名称/name</strong> - 装备名称（必填）</li>
                    <li><strong>序列号/serialNumber</strong> - 序列号（必填，唯一）</li>
                    <li><strong>分类/categoryId</strong> - 分类（必填，可使用分类ID或分类名称）</li>
                    <li><strong>数量/quantity</strong> - 总数量（必填，正整数）</li>
                    <li><strong>可用数量/availableQuantity</strong> - 可用数量（必填）</li>
                    <li><strong>状态/status</strong> - 状态（必填，可选：可用、维修中、不可用）</li>
                  </ul>
                </div>
              </div>
            </div>
          </div>
          
          <!-- 导入选项 -->
          <div class="import-options mt-4">
            <div class="form-check form-switch mb-3">
              <input class="form-check-input" type="checkbox" id="updateExisting" v-model="importOptions.updateExisting">
              <label class="form-check-label" for="updateExisting">更新已存在的装备（根据序列号匹配）</label>
            </div>
            <div class="form-check form-switch">
              <input class="form-check-input" type="checkbox" id="skipErrors" v-model="importOptions.skipErrors">
              <label class="form-check-label" for="skipErrors">跳过错误记录并继续导入</label>
            </div>
          </div>
        </div>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <button class="btn btn-light me-2" @click="importDialogVisible = false" :disabled="importLoading">取消</button>
          <button class="btn btn-primary" @click="importData" :disabled="!importFile || importLoading">
            <i class="bi bi-upload me-1"></i>{{ importLoading ? '导入中...' : '开始导入' }}
          </button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/modules/user'
import equipmentService from '@/api/equipment'
import http from '@/api/http'
import { ElMessage } from 'element-plus'
import axios from 'axios'
// import { useStore } from 'vuex'

// 路由和用户状态
const router = useRouter()
const userStore = useUserStore()
const isAdmin = computed(() => userStore.isAdmin)

// 装备数据状态
const equipments = ref([])
const loading = ref(false)
const totalItems = ref(0)
const totalPages = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const categories = ref([])

// 搜索状态
const searchQuery = ref({
  keyword: '',
  categoryId: null,
  status: null,
  page: 0,
  size: 10
})

// 删除相关状态
const equipmentToDelete = ref(null)
const deleteDialogVisible = ref(false)
const deleteLoading = ref(false)

// 批量删除相关状态
const selectedEquipments = ref([])
const bulkDeleteDialogVisible = ref(false)
const bulkDeleteLoading = ref(false)
const isAllSelected = computed(() => {
  return equipments.value.length > 0 && selectedEquipments.value.length === equipments.value.length
})

// 计算显示的页码
const displayedPages = computed(() => {
  const pages = []
  const maxVisiblePages = 5
  
  if (totalPages.value <= maxVisiblePages) {
    for (let i = 1; i <= totalPages.value; i++) {
      pages.push(i)
    }
  } else {
    const halfVisiblePages = Math.floor(maxVisiblePages / 2)
    let startPage = Math.max(currentPage.value - halfVisiblePages, 1)
    let endPage = Math.min(startPage + maxVisiblePages - 1, totalPages.value)
    
    if (endPage - startPage + 1 < maxVisiblePages) {
      startPage = Math.max(endPage - maxVisiblePages + 1, 1)
    }
    
    for (let i = startPage; i <= endPage; i++) {
      pages.push(i)
    }
  }
  
  return pages
})

// 加载装备列表
const loadEquipments = async () => {
  try {
    loading.value = true;
    selectedEquipments.value = [];
    
    // 构建查询参数
    const queryParams = new URLSearchParams();
    
    // 添加搜索条件
    if (searchQuery.value.keyword) {
      queryParams.append('keyword', searchQuery.value.keyword);
    }
    
    if (searchQuery.value.categoryId) {
      queryParams.append('categoryId', searchQuery.value.categoryId);
    }
    
    // 特殊处理"不可用"状态 - 当选择"不可用"时，从后端获取所有装备并在前端筛选
    // 其他状态直接传给后端
    if (searchQuery.value.status && searchQuery.value.status !== '不可用') {
      queryParams.append('status', searchQuery.value.status);
    }
    
    // 添加分页信息
    queryParams.append('page', currentPage.value - 1); // 后端页码从0开始
    queryParams.append('size', searchQuery.value.status === '不可用' ? 1000 : pageSize.value); // 不可用状态时获取更多数据用于前端筛选
    
    // 添加排序信息
    queryParams.append('sort', 'id,desc');
    
    // 防止缓存
    queryParams.append('_t', new Date().getTime());

    // 构建完整URL
    const url = `/api/equipment?${queryParams.toString()}`;
    
    // 发送请求
    const response = await fetch(url, {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`,
        'Content-Type': 'application/json'
      }
    });
    
    if (!response.ok) {
      throw new Error(`HTTP错误: ${response.status}`);
    }
    
    const text = await response.text();

    let data;
    try {
      data = JSON.parse(text);
    } catch (e) {
      console.error('解析响应JSON失败:', e);
      throw new Error('解析装备数据失败');
    }

    // 处理响应数据
    let equipmentsList = [];
    let totalElementsCount = 0;

    if (Array.isArray(data)) {
      // 数组响应
      equipmentsList = data;
      totalElementsCount = data.length;
    } else if (data && Array.isArray(data.content)) {
      // 分页响应
      equipmentsList = data.content;
      totalElementsCount = data.totalElements || data.content.length;
      totalPages.value = data.totalPages || 1;
    } else if (data && typeof data === 'object') {
      // 单个对象响应
      equipmentsList = [data];
      totalElementsCount = 1;
    } else {
      // 异常情况，设置空数组
      console.warn('未识别的响应格式');
      equipmentsList = [];
      totalElementsCount = 0;
    }
    
    // 如果筛选"不可用"状态，在前端过滤
    if (searchQuery.value.status === '不可用') {
      equipmentsList = equipmentsList.filter(item => item.availableQuantity <= 0 || item.status === '不可用');
      // 根据筛选结果更新总数
      totalElementsCount = equipmentsList.length;
      
      // 手动处理分页
      const startIndex = (currentPage.value - 1) * pageSize.value;
      const endIndex = Math.min(startIndex + pageSize.value, equipmentsList.length);
      equipments.value = equipmentsList.slice(startIndex, endIndex);
      
      // 重新计算总页数
      totalPages.value = Math.max(1, Math.ceil(totalElementsCount / pageSize.value));
    } else {
      // 其他状态直接使用后端返回的结果
      equipments.value = equipmentsList;
      totalPages.value = Math.max(1, Math.ceil(totalElementsCount / pageSize.value));
    }
    
    // 设置总记录数
    totalItems.value = totalElementsCount;
    
    console.log('装备列表加载成功，显示', equipments.value.length, '条记录，搜索条件:', 
      JSON.stringify({
        keyword: searchQuery.value.keyword,
        categoryId: searchQuery.value.categoryId, 
        status: searchQuery.value.status
      })
    );
  } catch (error) {
    console.error('加载装备列表失败:', error);
    ElMessage.error('加载装备列表失败: ' + (error.message || '服务器错误'));
    equipments.value = [];
    totalItems.value = 0;
    totalPages.value = 1;
  } finally {
    loading.value = false;
  }
};

// 加载分类列表
const loadCategories = async () => {
  try {

    const response = await http.get('/categories');
    
    if (Array.isArray(response)) {
      // 使用Map确保ID唯一
      const categoryMap = new Map();
      
      // 遍历数据，确保ID唯一
      response.forEach(category => {
        if (category && category.id && !categoryMap.has(category.id)) {
          categoryMap.set(category.id, {
            id: category.id,
            name: category.name || '未命名分类',
            description: category.description || ''
          });
        }
      });
      
      // 转换Map为数组
      categories.value = Array.from(categoryMap.values());

    } else {
      console.warn('分类响应格式不符合预期:', response);
      categories.value = [];
    }
  } catch (error) {
    console.error('加载分类列表失败:', error);
    ElMessage.error('加载分类列表失败: ' + (error.message || '服务器错误'));
    categories.value = [];
  }
};

// 刷新数据
const refreshData = () => {
  loadEquipments()
  loadCategories()
  ElMessage.success('数据已刷新')
}

// 切换页码
const changePage = (page) => {
  if (page >= 1 && page <= totalPages.value) {
    currentPage.value = page
    loadEquipments()
  }
}

// 导航方法
const goToDetail = (id) => {
  router.push(`/equipment/${id}`)
}

const goToEdit = (id) => {
  router.push(`/equipment/edit/${id}`)
}

const goToCreateEquipment = () => {
  router.push('/equipment/edit')
}

// 删除装备
const confirmDelete = (equipment) => {
  equipmentToDelete.value = equipment
  deleteDialogVisible.value = true
}

const deleteEquipment = async () => {
  if (!equipmentToDelete.value) return
  
  try {
    deleteLoading.value = true
    await equipmentService.delete(equipmentToDelete.value.id)
    deleteDialogVisible.value = false
    loadEquipments()
  } catch (error) {
    console.error('删除装备失败:', error)
    ElMessage.error('删除装备失败，请稍后重试')
  } finally {
    deleteLoading.value = false
  }
}

// 批量选择和删除
const toggleSelectAll = () => {
  if (isAllSelected.value) {
    selectedEquipments.value = []
  } else {
    selectedEquipments.value = equipments.value.map(item => item.id)
  }
}

const confirmBulkDelete = () => {
  if (selectedEquipments.value.length === 0) {
    ElMessage.warning('请先选择要删除的装备')
    return
  }
  bulkDeleteDialogVisible.value = true
}

const bulkDelete = async () => {
  if (selectedEquipments.value.length === 0) return
  
  try {
    bulkDeleteLoading.value = true
    await equipmentService.bulkDelete(selectedEquipments.value)
    bulkDeleteDialogVisible.value = false
    selectedEquipments.value = []
    loadEquipments()
  } catch (error) {
    console.error('批量删除装备失败:', error)
    ElMessage.error('批量删除装备失败，请稍后重试')
  } finally {
    bulkDeleteLoading.value = false
  }
}

// 监听搜索条件变化
watch(searchQuery, (newVal, oldVal) => {
  // 如果值发生了变化且不是page属性，重置页码
  if (
    newVal.keyword !== oldVal.keyword ||
    newVal.categoryId !== oldVal.categoryId ||
    newVal.status !== oldVal.status
  ) {
    currentPage.value = 1
    loadEquipments()
  }
}, { deep: true })

// 生命周期钩子
onMounted(async () => {
  // 先加载分类，因为装备显示需要分类信息
  await loadCategories()
  
  // 然后加载装备列表
  await loadEquipments()
  
  // 添加全局事件监听，当装备数据变化时刷新列表
  window.addEventListener('equipment-data-changed', () => {

    loadCategories().then(() => loadEquipments());
  });
})

onUnmounted(() => {
  // 移除事件监听器
  window.removeEventListener('equipment-data-changed', () => {

  });
})

// 清除筛选条件
const clearFilters = () => {
  searchQuery.value.keyword = ''
  searchQuery.value.categoryId = null
  searchQuery.value.status = null
  currentPage.value = 1
  loadEquipments()
}

// 判断是否已过滤
const isFiltered = computed(() => {
  return searchQuery.value.keyword || searchQuery.value.categoryId || searchQuery.value.status
})

// 获取分类名称
const getCategoryName = (id) => {
  const category = categories.value.find(c => c.id === id)
  return category ? category.name : '未命名分类'
}

// 导出对话框
const exportDialogVisible = ref(false)
const exportForm = ref({
  format: 'csv'
})

const showExportDialog = () => {
  exportDialogVisible.value = true
}

// 导出数据
const exportData = async () => {
  try {
    // 构建查询参数
    const params = new URLSearchParams()
    if (searchQuery.value.keyword) {
      params.append('keyword', searchQuery.value.keyword)
    }
    if (searchQuery.value.categoryId) {
      params.append('categoryId', searchQuery.value.categoryId)
    }
    if (searchQuery.value.status) {
      params.append('status', searchQuery.value.status)
    }
    
    // 添加导出格式参数
    params.append('format', exportForm.value.format)
    
    // 关闭对话框
    exportDialogVisible.value = false
    
    ElMessage.info('正在准备导出数据，请稍候...')
    
    // 根据格式确定MIME类型和文件扩展名
    const mimeType = exportForm.value.format === 'excel' 
      ? 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
      : 'text/csv;charset=UTF-8'
    
    const fileExtension = exportForm.value.format === 'excel' ? 'xlsx' : 'csv'
    
    // 获取token
    const token = userStore.token || localStorage.getItem('token')
    
    if (!token) {
      throw new Error('未找到有效的身份验证令牌，请重新登录')
    }

    // 使用axios发送请求，以二进制形式获取文件
    const response = await axios({
      url: `/api/equipment/export?${params.toString()}`,
      method: 'GET',
      responseType: 'blob', // 指定响应类型为blob，以便正确处理二进制数据
      headers: {
        'Authorization': `Bearer ${token}`,
        'Accept': mimeType
      },
      validateStatus: function (status) {
        return status < 500 // 允许400-499的状态码，以便我们可以处理它们
      }
    })
    
    // 检查响应状态
    if (response.status !== 200) {
      // 如果响应不是200 OK，尝试解析错误信息
      const reader = new FileReader()
      
      reader.onload = () => {
        try {
          const errorData = JSON.parse(reader.result)
          ElMessage.error(`导出失败: ${errorData.message || '服务器错误'}`)
        } catch (e) {
          ElMessage.error(`导出失败: HTTP状态码 ${response.status}`)
        }
      }
      
      reader.readAsText(response.data)
      return
    }
    
    // 获取文件名
    let filename = `装备列表_${new Date().getTime()}.${fileExtension}`
    
    // 尝试从响应头中获取文件名
    const contentDisposition = response.headers['content-disposition']

    if (contentDisposition) {
      const filenameMatch = contentDisposition.match(/filename\*=UTF-8''([^;]+)/)
      if (filenameMatch && filenameMatch[1]) {
        filename = decodeURIComponent(filenameMatch[1])
      } else {
        const regularMatch = contentDisposition.match(/filename="([^"]+)"/)
        if (regularMatch && regularMatch[1]) {
          filename = regularMatch[1]
        }
      }
    }
    
    // 检查响应是否包含错误信息
    if (response.data.type === 'application/json') {
      // 如果返回的是JSON而不是文件，可能是错误信息
      const reader = new FileReader()
      reader.onload = function() {
        try {
          const errorData = JSON.parse(reader.result)
          ElMessage.error(`导出失败: ${errorData.message || '未知错误'}`)
        } catch (e) {
          ElMessage.error('导出失败: 服务器返回了无效的响应')
        }
      }
      reader.readAsText(response.data)
      return
    }
    
    // 处理blob数据
    const blob = new Blob([response.data], { type: mimeType })
    
    // 创建下载链接
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = filename
    
    // 触发下载
    document.body.appendChild(link)
    link.click()
    
    // 清理
    setTimeout(() => {
      document.body.removeChild(link)
      window.URL.revokeObjectURL(url)
    }, 100)
    
    ElMessage.success('数据导出成功')
  } catch (error) {
    console.error('导出失败:', error)
    ElMessage.error('数据导出失败: ' + (error.message || '服务器错误'))
  }
}

// 导入对话框
const importDialogVisible = ref(false)
const importFile = ref(null)
const fileInput = ref(null)
const isDragging = ref(false)
const importLoading = ref(false)
const importOptions = ref({
  updateExisting: true,
  skipErrors: false
})

// 显示导入对话框
const showImportDialog = () => {
  importDialogVisible.value = true
  importFile.value = null
}

// 拖拽文件处理
const onDragOver = () => {
  isDragging.value = true
}

const onDragLeave = () => {
  isDragging.value = false
}

const onFileDrop = (event) => {
  isDragging.value = false
  const files = event.dataTransfer.files
  if (files.length > 0) {
    validateAndSetFile(files[0])
  }
}

// 文件选择处理
const onFileChange = (event) => {
  const files = event.target.files
  if (files.length > 0) {
    validateAndSetFile(files[0])
  }
}

// 验证并设置文件
const validateAndSetFile = (file) => {
  // 验证文件类型
  if (!file.name.endsWith('.csv') && !file.name.endsWith('.xlsx')) {
    ElMessage.error('请上传.csv或.xlsx格式的文件')
    return
  }
  
  // 验证文件大小（最大10MB）
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.error('文件大小不能超过10MB')
    return
  }
  
  importFile.value = file
}

// 移除选择的文件
const removeFile = () => {
  importFile.value = null
  if (fileInput.value) {
    fileInput.value.value = ''
  }
}

// 格式化文件大小
const formatFileSize = (size) => {
  if (size < 1024) {
    return size + ' B'
  } else if (size < 1024 * 1024) {
    return (size / 1024).toFixed(2) + ' KB'
  } else {
    return (size / (1024 * 1024)).toFixed(2) + ' MB'
  }
}

// 下载导入模板
const downloadTemplate = async (format) => {
  try {
    // 显示加载提示
    ElMessage.info('正在准备下载模板，请稍候...');
    
    // 使用装备服务获取模板
    const response = await equipmentService.getImportTemplate(format);
    
    // 获取文件扩展名
    const fileExtension = format === 'excel' ? 'xlsx' : 'csv';
    
    // 创建下载链接
    const url = window.URL.createObjectURL(response);
    const link = document.createElement('a');
    link.href = url;
    link.download = `装备导入模板.${fileExtension}`;
    
    // 触发下载
    document.body.appendChild(link);
    link.click();
    
    // 清理
    setTimeout(() => {
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
    }, 100);
    
    ElMessage.success('模板下载成功');
  } catch (error) {
    console.error('下载模板失败:', error);
    ElMessage.error('下载模板失败: ' + (error.message || '服务器错误'));
  }
};

// 导入数据
const importData = async () => {
  if (!importFile.value) {
    ElMessage.warning('请先选择要导入的文件');
    return;
  }
  
  try {
    importLoading.value = true;
    
    // 先检查认证状态是否有效
    try {
      const isAuthenticated = await equipmentService.checkAuthentication();
      if (!isAuthenticated) {
        ElMessage.error('会话已过期，请重新登录');
        setTimeout(() => {
          userStore.logout();
          router.push('/login');
        }, 1500);
        return;
      }
    } catch (authError) {
      console.error('认证检查失败:', authError);
      if (authError.message && authError.message.includes('Network Error')) {
        ElMessage.error('网络连接错误，请检查网络后重试');
      } else {
        ElMessage.error('认证检查失败，请重新登录后再尝试导入');
        setTimeout(() => {
          userStore.logout();
        }, 1500);
      }
      return;
    }
    
    // 调用装备服务的导入方法
    const response = await equipmentService.import(importFile.value, {
      updateExisting: importOptions.value.updateExisting,
      skipErrors: importOptions.value.skipErrors
    });
    
    // 显示成功消息
    ElMessage.success(response.message || '导入成功');
    
    // 关闭导入对话框
    importDialogVisible.value = false;
    importFile.value = null;
    
    // 刷新列表数据
    loadEquipments();
  } catch (error) {
    console.error('导入数据失败:', error);
    
    // 处理401错误，引导用户重新登录
    if (error.response?.status === 401) {
      ElMessage.error('您的登录信息已过期，请重新登录后再尝试导入');
      setTimeout(() => {
        userStore.logout();
        router.push('/login');
      }, 1500);
      return;
    }
    
    // 处理网络错误
    if (error.message && error.message.includes('Network Error')) {
      ElMessage.error('网络连接错误，请检查网络后重试');
      return;
    }
    
    // 处理后端返回的错误信息
    if (error.response?.data?.message) {
      ElMessage.error(`导入失败: ${error.response.data.message}`);
      return;
    }
    
    // 其他错误处理
    ElMessage.error('导入失败，请检查文件格式是否正确');
  } finally {
    importLoading.value = false;
  }
};

// 添加辅助函数
const truncateText = (text, length) => {
  if (!text) return '';
  return text.length > length ? text.substring(0, length) + '...' : text;
};

// 处理页面切换
const handlePageChange = (page) => {
  currentPage.value = page;
  
  // 如果是不可用状态筛选，不需要重新请求数据，直接在前端处理
  if (searchQuery.value.status === '不可用') {
    loadEquipments();
  } else {
    loadEquipments();
  }
};

// 处理每页显示数量变化
const handlePageSizeChange = () => {
  currentPage.value = 1;
  loadEquipments();
};
</script>

<style lang="scss" scoped>
// 导入变量
@import "../../styles/variables.scss";

// 现代化美观样式
.page-icon-wrapper {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background-color: rgba($--color-primary, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  
  i {
    font-size: 22px;
  }
}

// 搜索表单
.search-form {
  .search-input-group {
    position: relative;
    
    .search-icon {
      position: absolute;
      left: 16px;
      top: 50%;
      transform: translateY(-50%);
      color: $--color-text-secondary;
      z-index: 1;
    }
    
    .form-control {
      height: 48px;
      border-radius: 12px;
      box-shadow: 0 2px 5px rgba(0,0,0,0.02);
      transition: all 0.25s;
      
      &:focus {
        box-shadow: 0 4px 10px rgba($--color-primary, 0.15);
        border-color: $--color-primary;
      }
    }
  }
  
  .form-select {
    height: 48px;
    border-radius: 12px;
    padding-left: 16px;
    background-position: right 16px center;
    box-shadow: 0 2px 5px rgba(0,0,0,0.02);
    
    &:focus {
      box-shadow: 0 4px 10px rgba($--color-primary, 0.15);
      border-color: $--color-primary;
    }
  }
}

// 过滤标签
.filter-tag {
  display: inline-flex;
  align-items: center;
  padding: 4px 12px;
  background: rgba($--color-primary, 0.05);
  border-radius: 20px;
  font-size: 0.875rem;
  color: $--color-text-regular;
  
  strong {
    color: $--color-primary;
  }
}

// 表格样式
.modern-table {
  th {
    font-weight: 600;
    text-transform: uppercase;
    font-size: 0.75rem;
    letter-spacing: 0.5px;
    color: $--color-text-secondary;
    padding: 12px 16px;
  }
  
  td {
    padding: 16px;
    vertical-align: middle;
  }
  
  .equipment-row {
    transition: all 0.2s;
    
    &:hover {
      background-color: rgba($--color-primary, 0.02);
    }
  }
  
  .form-check-input {
    cursor: pointer;
    width: 18px;
    height: 18px;
    
    &:checked {
      background-color: $--color-primary;
      border-color: $--color-primary;
    }
  }
}

// 序号标签
.index-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 6px;
  background-color: $--border-color-lighter;
  color: $--color-text-secondary;
  font-size: 0.875rem;
  font-weight: 500;
}

// 装备信息
.equipment-name {
  a {
    display: block;
    margin-bottom: 4px;
    font-weight: 500;
    
    &:hover {
      text-decoration: underline !important;
    }
  }
  
  .equipment-description {
    font-size: 0.75rem;
    color: $--color-text-secondary;
    display: block;
    max-width: 300px;
  }
}

// 序列号样式
.serial-number {
  display: inline-block;
  padding: 4px 8px;
  border-radius: 6px;
  background-color: $--border-color-lighter;
  color: $--color-text-regular;
  font-size: 0.75rem;
  font-family: monospace;
  letter-spacing: 0.5px;
}

// 分类徽章
.category-badge {
  display: inline-flex;
  align-items: center;
  padding: 5px 10px;
  border-radius: 20px;
  background-color: rgba($--color-info, 0.1);
  color: $--color-info;
  font-size: 0.75rem;
  font-weight: 500;
}

// 数量徽章
.quantity-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 28px;
  height: 28px;
  border-radius: 6px;
  padding: 0 8px;
  color: white;
  font-weight: 600;
  font-size: 0.85rem;
  
  &.bg-success {
    background-color: $--color-success;
  }
  
  &.bg-warning {
    background-color: $--color-warning;
  }
  
  &.bg-danger {
    background-color: $--color-danger;
  }
}

// 状态徽章
.status-badge {
  display: inline-flex;
  align-items: center;
  padding: 5px 12px;
  border-radius: 20px;
  font-size: 0.75rem;
  font-weight: 500;
  
  .status-icon {
    margin-right: 4px;
    font-size: 0.75rem;
  }
  
  &.status-available {
    background-color: rgba($--color-success, 0.1);
    color: $--color-success;
  }
  
  &.status-maintenance {
    background-color: rgba($--color-warning, 0.1);
    color: $--color-warning;
  }
  
  &.status-unavailable {
    background-color: rgba($--color-danger, 0.1);
    color: $--color-danger;
  }
}

// 操作按钮
.action-buttons {
  display: flex;
  justify-content: center;
  gap: 6px;
  
  .btn-icon {
    width: 35px;
    height: 35px;
    padding: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 8px;
    transition: all 0.2s;
    
    &:hover {
      transform: translateY(-2px);
    }
    
    i {
      font-size: 0.875rem;
    }
    
    &.btn-light-primary {
      background-color: rgba($--color-primary, 0.1);
      color: $--color-primary;
      border: none;
      
      &:hover {
        background-color: rgba($--color-primary, 0.2);
      }
    }
    
    &.btn-light-success {
      background-color: rgba($--color-success, 0.1);
      color: $--color-success;
      border: none;
      
      &:hover {
        background-color: rgba($--color-success, 0.2);
      }
    }
    
    &.btn-light-danger {
      background-color: rgba($--color-danger, 0.1);
      color: $--color-danger;
      border: none;
      
      &:hover {
        background-color: rgba($--color-danger, 0.2);
      }
    }
  }
}

// 空状态
.empty-state {
  padding: 30px 0;
  
  .empty-state-icon {
    width: 80px;
    height: 80px;
    margin: 0 auto;
    border-radius: 50%;
    background-color: $--border-color-lighter;
    display: flex;
    align-items: center;
    justify-content: center;
    
    i {
      font-size: 32px;
      color: $--color-text-secondary;
    }
  }
}

// 自定义按钮样式
.btn-light-primary {
  background-color: rgba($--color-primary, 0.1);
  color: $--color-primary;
  border: none;
  
  &:hover {
    background-color: rgba($--color-primary, 0.2);
  }
}

.btn-light-success {
  background-color: rgba($--color-success, 0.1);
  color: $--color-success;
  border: none;
  
  &:hover {
    background-color: rgba($--color-success, 0.2);
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

.btn-light-secondary {
  background-color: rgba($--color-text-secondary, 0.1);
  color: $--color-text-secondary;
  border: none;
  
  &:hover {
    background-color: rgba($--color-text-secondary, 0.2);
  }
}

.btn-icon-text {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  
  i {
    margin-right: 4px;
  }
}

// 圆形图标按钮
.btn-icon {
  width: 40px;
  height: 40px;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

// 对话框样式
.modern-dialog {
  :deep(.el-dialog__header) {
    padding: 16px 24px;
    margin: 0;
    border-bottom: 1px solid $--border-color-lighter;
  }
  
  :deep(.el-dialog__title) {
    font-weight: 600;
    font-size: 1.125rem;
  }
  
  :deep(.el-dialog__body) {
    padding: 24px;
  }
  
  :deep(.el-dialog__footer) {
    padding: 16px 24px;
    border-top: 1px solid $--border-color-lighter;
  }
  
  .dialog-icon {
    width: 70px;
    height: 70px;
    margin: 0 auto;
    border-radius: 50%;
    background-color: rgba($--color-warning, 0.1);
    display: flex;
    align-items: center;
    justify-content: center;
    
    i {
      font-size: 32px;
      color: $--color-warning;
    }
    
    &.bg-danger {
      background-color: rgba($--color-danger, 0.1);
      
      i {
        color: $--color-danger;
      }
    }
  }
}

.bg-light-primary {
  background-color: rgba($--color-primary, 0.05);
}

// 自定义分页样式
:deep(.modern-pagination) {
  .el-pager li {
    margin: 0 3px;
    min-width: 34px;
    height: 34px;
    line-height: 34px;
    border-radius: 8px;
    font-weight: 500;
    
    &.is-active {
      background-color: $--color-primary;
      font-weight: 600;
    }
  }
  
  .btn-prev, .btn-next {
    border-radius: 8px;
    min-width: 34px;
    height: 34px;
    line-height: 34px;
    padding: 0;
  }
}

// 淡入动画
.fade-in {
  animation: fadeIn 0.4s ease-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

// 文件上传区域样式
.file-upload-area {
  border: 2px dashed $--border-color-base;
  border-radius: 12px;
  padding: 30px;
  text-align: center;
  position: relative;
  transition: all 0.3s;
  cursor: pointer;
  
  &:hover {
    border-color: $--color-primary;
    background-color: rgba($--color-primary, 0.02);
  }
  
  &.dragging {
    border-color: $--color-primary;
    background-color: rgba($--color-primary, 0.05);
  }
  
  &.has-file {
    border-style: solid;
    border-color: $--color-primary;
    background-color: rgba($--color-primary, 0.03);
  }
  
  .upload-placeholder {
    i {
      font-size: 40px;
      color: $--color-primary;
      margin-bottom: 16px;
    }
  }
  
  .file-input {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    opacity: 0;
    cursor: pointer;
    z-index: 1;
  }
  
  .file-preview {
    display: flex;
    align-items: center;
    padding: 10px;
    
    .file-icon {
      font-size: 30px;
      margin-right: 15px;
      
      &.bi-file-earmark-excel {
        color: #217346; // Excel green
      }
      
      &.bi-filetype-csv {
        color: #f8991c; // Orange for CSV
      }
    }
    
    .file-info {
      flex-grow: 1;
      text-align: left;
      
      .file-name {
        font-weight: 500;
        margin-bottom: 4px;
        word-break: break-all;
      }
      
      .file-size {
        color: $--color-text-secondary;
        font-size: 0.75rem;
      }
    }
  }
}

// 模板下载链接
.template-download {
  a {
    color: $--color-primary;
    text-decoration: none;
    
    &:hover {
      text-decoration: underline;
    }
  }
}

// 导入选项
.import-options {
  border-top: 1px solid $--border-color-lighter;
  padding-top: 16px;
}

// 导出选项样式
.export-options {
  .export-option {
    border: 1px solid $--border-color-lighter;
    border-radius: 12px;
    padding: 16px;
    height: 100%;
    cursor: pointer;
    transition: all 0.2s;
    
    &:hover {
      border-color: $--color-primary;
      background-color: rgba($--color-primary, 0.02);
    }
    
    &.active {
      border-color: $--color-primary;
      background-color: rgba($--color-primary, 0.05);
      box-shadow: 0 4px 12px rgba($--color-primary, 0.15);
      
      .export-icon {
        background-color: $--color-primary;
        color: white;
      }
      
      .export-label {
        color: $--color-primary;
      }
    }
    
    .export-icon {
      width: 50px;
      height: 50px;
      border-radius: 10px;
      background-color: rgba($--color-primary, 0.1);
      color: $--color-primary;
      display: flex;
      align-items: center;
      justify-content: center;
      margin-bottom: 12px;
      
      i {
        font-size: 24px;
      }
    }
    
    .export-label {
      font-weight: 600;
      margin-bottom: 6px;
      font-size: 0.95rem;
    }
    
    .export-desc {
      font-size: 0.75rem;
      color: $--color-text-secondary;
      line-height: 1.4;
    }
  }
}

.dialog-icon {
  &.bg-primary {
    background-color: rgba($--color-primary, 0.1);
    
    i {
      color: $--color-primary;
    }
  }
}
</style> 