<script setup>
import { ref, onMounted, computed, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox, ElLoading } from 'element-plus';
import equipmentService from '@/api/equipment';

const route = useRoute();
const router = useRouter();
const isEditMode = computed(() => !!route.params.id);

// 加载状态
const loading = ref(false);
const submitting = ref(false);

// 表单数据
const form = ref({
  id: null,
  serialNumber: '',
  name: '',
  categoryId: '',
  description: '',
  model: '',
  manufacturer: '',
  quantity: 1,
  availableQuantity: 1,
  status: '可用'
});

// 表单验证
const formErrors = ref({});

// 分类列表
const categories = ref([]);
const categoriesLoading = ref(false);

// 状态选项
const statusOptions = [
  { value: '可用', label: '可用', badge: 'bg-success' },
  { value: '不可用', label: '不可用', badge: 'bg-danger' },
  { value: '维修中', label: '维修中', badge: 'bg-info' }
];

// 初始化数据
const initData = async () => {

  const id = route.params.id;
  if (id) {

    await fetchEquipmentDetail(id);
  } else {

  }
  await fetchCategories();
};

// 获取装备详情
const fetchEquipmentDetail = async (id) => {
  loading.value = true;
  try {
    const data = await equipmentService.getById(id);
    if (!data) {
      ElMessage.error('装备不存在或已被删除');
      router.push('/equipment/list');
      return;
    }
    
    // 填充表单数据
    form.value = {
      id: data.id,
      serialNumber: data.serialNumber || '',
      name: data.name || '',
      categoryId: data.category?.id || '',
      description: data.description || '',
      model: data.model || '',
      manufacturer: data.manufacturer || '',
      quantity: data.quantity || 1,
      availableQuantity: data.availableQuantity || 1,
      status: data.status !== undefined ? data.status : '可用'
    };


  } catch (error) {
    console.error('获取装备详情失败', error);
    ElMessage.error('获取装备详情失败');
    router.push('/equipment/list');
  } finally {
    loading.value = false;
  }
};

// 获取分类列表
const fetchCategories = async () => {
  categoriesLoading.value = true;
  try {

    // 直接使用fetch获取数据，避免axios解析循环引用问题
    const directResponse = await fetch('/api/categories', {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`,
        'Content-Type': 'application/json'
      }
    });
    
    if (directResponse.ok) {
      // 先获取文本响应
      const text = await directResponse.text();

      // 尝试安全解析JSON
    let categoryList = [];
      try {
        // 手动处理可能的循环引用问题
        let processedText = text.replace(/"equipments":\s*\[\s*\{[^\]]*\}\s*\]/g, '"equipments":[]');
        const data = JSON.parse(processedText);
        categoryList = Array.isArray(data) ? data : [];
      } catch (e) {
        console.error('分类JSON解析错误，尝试手动解析:', e);
        
        // 使用正则表达式提取分类数据 - 更精确的模式
        const categoryRegex = /\{"id":(\d+),"name":"([^"]*)"(?:,"description":"([^"]*)")?/g;
        const categoryMap = new Map(); // 使用Map确保ID唯一
        
        let match;
        while ((match = categoryRegex.exec(text)) !== null) {
          const id = parseInt(match[1]);
          if (!categoryMap.has(id)) {
            categoryMap.set(id, {
              id: id,
              name: match[2],
              description: match[3] || ''
            });
          }
        }
        
        categoryList = Array.from(categoryMap.values());
    }
    
      // 处理分类数据
      categories.value = categoryList.map(item => ({
        id: item.id,
        name: item.name || '未命名分类',
        description: item.description || ''
    }));

      // 当编辑模式下，如果设备有分类但分类列表中不包含该分类
      if (isEditMode.value && form.value.categoryId && 
          !categories.value.some(c => c.id === parseInt(form.value.categoryId))) {
      try {
          // 尝试单独获取该分类
          const catResponse = await fetch(`/api/categories/${form.value.categoryId}`, {
            headers: {
              'Authorization': `Bearer ${localStorage.getItem('token')}`,
              'Content-Type': 'application/json'
            }
          });
          
          if (catResponse.ok) {
            const catText = await catResponse.text();
            let catData;
            
            try {
              catData = JSON.parse(catText);
            } catch (e) {
              // 尝试使用正则从响应中提取
              const catMatch = /"id":(\d+),"name":"([^"]*)"(?:,"description":"([^"]*)")?/.exec(catText);
              if (catMatch) {
                catData = {
                  id: parseInt(catMatch[1]),
                  name: catMatch[2],
                  description: catMatch[3] || ''
          };
              }
            }
            
            if (catData && catData.id) {
              categories.value.push({
                id: catData.id,
                name: catData.name,
                description: catData.description || ''
              });

            }
        }
      } catch (error) {
          console.error('获取单独分类失败:', error);
      }
      }
    } else {
      console.error('获取分类列表失败:', directResponse.status, directResponse.statusText);
      categories.value = [];
      ElMessage.error(`获取分类列表失败: ${directResponse.statusText}`);
    }
    
    if (categories.value.length === 0) {
      ElMessage.warning('未找到任何分类，请先添加分类');
    }
  } catch (error) {
    console.error('获取分类列表失败:', error);
    ElMessage.error('获取分类列表失败');
    categories.value = [];
  } finally {
    categoriesLoading.value = false;
  }
};

// 表单验证
const validateForm = () => {
  const errors = {};
  
  if (!form.value.serialNumber?.trim()) {
    errors.serialNumber = '请输入装备编号';
  } else if (form.value.serialNumber.length > 50) {
    errors.serialNumber = '装备编号不能超过50个字符';
  }
  
  if (!form.value.name?.trim()) {
    errors.name = '请输入装备名称';
  } else if (form.value.name.length > 100) {
    errors.name = '装备名称不能超过100个字符';
  }
  
  const categoryId = form.value.categoryId;
  if (!categoryId || categoryId === '' || categoryId === 0) {
    errors.categoryId = '请选择装备分类';
  }
  
  if (!form.value.quantity || form.value.quantity < 1) {
    errors.quantity = '请输入有效的装备数量';
  }
  
  // 确保可用数量不超过总数量
  if (parseInt(form.value.availableQuantity) > parseInt(form.value.quantity)) {
    form.value.availableQuantity = form.value.quantity;
  }
  
  formErrors.value = errors;
  return Object.keys(errors).length === 0;
};

// 提交表单
const submitForm = async () => {
  if (!validateForm()) return;
  
  try {
    loading.value = true;
    submitting.value = true;
    
    const formData = { ...form.value };
    
    // 确保categoryId是数字类型
    if (formData.categoryId) {
      formData.categoryId = parseInt(formData.categoryId, 10);
    }

    // 使用fetch直接提交请求
    const url = formData.id ? `/api/equipment/${formData.id}` : '/api/equipment';
    const method = formData.id ? 'PUT' : 'POST';
    
    // 检查所选分类是否存在
    const selectedCategory = categories.value.find(c => c.id === formData.categoryId);
    if (!selectedCategory) {
      console.warn('找不到所选分类，可能需要刷新分类列表');
      // 如果找不到所选分类，尝试刷新分类列表
      await fetchCategories();
      
      // 再次检查
      const categoryExists = categories.value.some(c => c.id === formData.categoryId);
      if (!categoryExists) {
        ElMessage.warning('所选分类不存在，请重新选择分类');
        loading.value = false;
        submitting.value = false;
        return;
      }
    }
    
    const response = await fetch(url, {
      method: method,
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(formData)
    });

    if (response.ok) {
      const successMessage = formData.id ? '装备更新成功！' : '装备创建成功！';
      ElMessage.success(successMessage);
    
    // 强制清除缓存并通知其他组件刷新
    if (window.dispatchEvent) {
      window.dispatchEvent(new CustomEvent('equipment-data-changed'));
    }
    
    // 返回列表
      router.push('/equipment/list');
    } else {
      // 处理错误响应
      let errorMessage = '保存装备失败';
      try {
        const responseText = await response.text();
        console.error('错误响应内容:', responseText);
        
        try {
          const errorData = JSON.parse(responseText);
          errorMessage = errorData.message || errorMessage;
        } catch (e) {
          // 如果JSON解析失败，尝试从文本中提取错误信息
          const msgMatch = /"message":"([^"]+)"/.exec(responseText);
          if (msgMatch && msgMatch[1]) {
            errorMessage = msgMatch[1];
          }
        }
      } catch (e) {
        console.error('解析错误响应失败:', e);
      }
      
      ElMessage.error(errorMessage);
    }
  } catch (error) {
    console.error('提交表单失败:', error);
    let errorMsg = '操作失败，请稍后重试';
    
    if (error.response && error.response.data) {
      errorMsg = error.response.data.message || errorMsg;
    }
    
    ElMessage.error(errorMsg);
  } finally {
    loading.value = false;
    submitting.value = false;
  }
};

// 创建新分类
const openCreateCategoryModal = async () => {
  try {
    const { value: categoryName } = await ElMessageBox.prompt(
      '请输入新分类名称',
      '创建分类',
      {
        confirmButtonText: '创建',
        cancelButtonText: '取消',
        inputValidator: (value) => {
          if (!value) return '分类名称不能为空';
          if (value.length > 50) return '分类名称不能超过50个字符';
          if (categories.value.some(c => c.name.toLowerCase() === value.toLowerCase().trim())) {
            return '该分类名称已存在';
          }
          return true;
        },
        inputErrorMessage: '请输入有效的分类名称'
      }
    );
    
    if (categoryName) {
      submitting.value = true;
      try {
        // 使用fetch直接提交请求
        const response = await fetch('/api/categories', {
          method: 'POST',
          headers: {
            'Authorization': `Bearer ${localStorage.getItem('token')}`,
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({ name: categoryName.trim(), description: '' })
        });

        if (response.ok) {
          // 尝试解析响应
          const text = await response.text();

          let newCategory;
          try {
            newCategory = JSON.parse(text);
          } catch (e) {
            console.error('解析创建分类响应失败:', e);
            // 如果解析失败，创建一个临时对象
            newCategory = {
              id: Date.now(), // 使用时间戳作为临时ID
              name: categoryName.trim(),
              description: ''
            };
          }
        
        // 确保返回的分类数据格式正确
        const categoryData = {
          id: newCategory.id || Date.now(),
          name: newCategory.name || categoryName.trim(),
          description: newCategory.description || ''
        };
        
        // 将新分类添加到列表
        categories.value.push(categoryData);
        
        // 选择新创建的分类
        form.value.categoryId = categoryData.id;
        
        ElMessage.success(`分类 "${categoryData.name}" 已成功创建`);
        } else {
          // 处理错误响应
          let errorMessage = '创建分类失败';
          try {
            const errorData = await response.json();
            errorMessage = errorData.message || errorMessage;
          } catch (e) {
            console.error('解析错误响应失败:', e);
          }
          
          // 如果错误消息表明分类已存在，我们可以尝试获取该分类
          if (errorMessage.includes('已存在')) {
            ElMessage.warning(errorMessage);
            // 刷新分类列表以获取最新数据
            await fetchCategories();
            // 尝试找到并选择同名分类
            const existingCategory = categories.value.find(c => 
              c.name.toLowerCase() === categoryName.trim().toLowerCase());
            if (existingCategory) {
              form.value.categoryId = existingCategory.id;
            }
          } else {
            ElMessage.error(errorMessage);
          }
        }
      } catch (error) {
        console.error('创建分类错误:', error);
        ElMessage.error('创建分类失败: ' + (error.message || '未知错误'));
      } finally {
        submitting.value = false;
      }
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('创建分类失败', error);
      ElMessage.error('创建分类失败');
    }
  }
};

// 返回列表
const goBack = () => {
  router.push('/equipment/list');
};

// 重置表单
const resetForm = () => {
  if (isEditMode.value) {
    // 编辑模式下，重置为原始数据
    fetchEquipmentDetail(form.value.id);
  } else {
    // 新增模式下，清空表单
    form.value = {
      id: null,
      serialNumber: '',
      name: '',
      categoryId: '',
      description: '',
      model: '',
      manufacturer: '',
      quantity: 1,
      availableQuantity: 1,
      status: '可用'
    };
  }
  formErrors.value = {};
};

// 生命周期钩子
onMounted(() => {

  initData();
});
</script>

<template>
  <div class="container-fluid py-4">
    <div class="row justify-content-center">
      <div class="col-12 col-lg-10">
        <!-- 页面标题 -->
        <div class="card border-0 shadow-sm mb-4">
          <div class="card-body d-flex justify-content-between align-items-center">
            <h5 class="m-0">
              <i class="bi" :class="isEditMode ? 'bi-pencil-square' : 'bi-plus-circle'"></i>
              {{ isEditMode ? '编辑装备' : '添加装备' }}
            </h5>
            <div>
              <button class="btn btn-outline-secondary me-2" @click="goBack">
                <i class="bi bi-arrow-left me-1"></i>返回
              </button>
            </div>
          </div>
        </div>

        <!-- 表单卡片 -->
        <div class="card border-0 shadow-sm">
          <div class="card-body">
            <form @submit.prevent="submitForm">
              <div class="row g-4">
                <!-- 基本信息 -->
                <div class="col-12">
                  <h6 class="text-primary mb-3 border-bottom pb-2">基本信息</h6>
                </div>

                <!-- 名称 -->
                <div class="col-md-6">
                  <label for="name" class="form-label">装备名称 <span class="text-danger">*</span></label>
                  <input 
                    type="text" 
                    id="name" 
                    class="form-control" 
                    v-model="form.name" 
                    :class="{'is-invalid': formErrors.name}"
                    required
                  >
                  <div class="invalid-feedback" v-if="formErrors.name">{{ formErrors.name }}</div>
                </div>

                <!-- 序列号 -->
                <div class="col-md-6">
                  <label for="serialNumber" class="form-label">序列号 <span class="text-danger">*</span></label>
                  <input 
                    type="text" 
                    id="serialNumber" 
                    class="form-control" 
                    v-model="form.serialNumber"
                    :class="{'is-invalid': formErrors.serialNumber}"
                    required
                  >
                  <div class="invalid-feedback" v-if="formErrors.serialNumber">{{ formErrors.serialNumber }}</div>
                </div>

                <!-- 分类 -->
                <div class="mb-3">
                  <label for="categoryId" class="form-label">分类 <span class="text-danger">*</span></label>
                  <div class="input-group">
                    <select 
                      class="form-select" 
                      id="categoryId"
                      v-model="form.categoryId"
                      :disabled="categoriesLoading"
                      :class="{'is-invalid': formErrors.categoryId}"
                    >
                      <option value="" disabled>请选择分类</option>
                      <option 
                        v-for="category in categories" 
                        :key="category.id" 
                        :value="category.id"
                      >
                        {{ category.name }}
                      </option>
                    </select>
                    <button 
                      type="button"
                      class="btn btn-outline-secondary" 
                      @click="openCreateCategoryModal"
                      title="添加新分类"
                    >
                      <i class="bi bi-plus-lg"></i>
                    </button>
                    <div v-if="formErrors.categoryId" class="invalid-feedback">
                      {{ formErrors.categoryId }}
                    </div>
                  </div>
                  <div v-if="categoriesLoading" class="text-muted small mt-1">
                    <span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span>
                    正在加载分类...
                  </div>
                  <div v-else-if="categories.length === 0" class="text-danger small mt-1">
                    未找到任何分类，请先 <a href="javascript:void(0)" @click="openCreateCategoryModal">添加分类</a>
                  </div>
                </div>

                <!-- 状态 -->
                <div class="col-md-6">
                  <label for="status" class="form-label">状态</label>
                  <select 
                    id="status" 
                    class="form-select" 
                    v-model="form.status"
                  >
                    <option 
                      v-for="option in statusOptions" 
                      :key="option.value" 
                      :value="option.value"
                    >
                      {{ option.label }}
                    </option>
                  </select>
                </div>

                <!-- 数量信息 -->
                <div class="col-12">
                  <h6 class="text-primary mb-3 border-bottom pb-2 mt-2">库存信息</h6>
                </div>

                <!-- 总数量 -->
                <div class="col-md-6">
                  <label for="quantity" class="form-label">总数量 <span class="text-danger">*</span></label>
                  <input 
                    type="number" 
                    id="quantity" 
                    class="form-control" 
                    v-model="form.quantity"
                    :class="{'is-invalid': formErrors.quantity}"
                    min="1" 
                    required
                  >
                  <div class="invalid-feedback" v-if="formErrors.quantity">{{ formErrors.quantity }}</div>
                </div>

                <!-- 可用数量 -->
                <div class="col-md-6">
                  <label for="availableQuantity" class="form-label">可用数量</label>
                  <input 
                    type="number" 
                    id="availableQuantity" 
                    class="form-control" 
                    v-model="form.availableQuantity"
                    :class="{'is-invalid': formErrors.availableQuantity}"
                    min="0"
                  >
                  <div class="form-text" v-if="!formErrors.availableQuantity">留空则默认与总数量相同</div>
                  <div class="invalid-feedback" v-if="formErrors.availableQuantity">{{ formErrors.availableQuantity }}</div>
                </div>

                <!-- 位置信息 -->
                <div class="col-md-12">
                  <label for="location" class="form-label">存放位置</label>
                  <input 
                    type="text" 
                    id="location" 
                    class="form-control" 
                    v-model="form.location"
                    placeholder="例如：实验室A区-02柜"
                  >
                </div>

                <!-- 详细规格信息 -->
                <div class="col-12">
                  <h6 class="text-primary mb-3 border-bottom pb-2 mt-2">详细规格</h6>
                </div>

                <!-- 制造商 -->
                <div class="col-md-6">
                  <label for="manufacturer" class="form-label">制造商</label>
                  <input 
                    type="text" 
                    id="manufacturer" 
                    class="form-control" 
                    v-model="form.manufacturer"
                  >
                </div>

                <!-- 型号 -->
                <div class="col-md-6">
                  <label for="model" class="form-label">型号</label>
                  <input 
                    type="text" 
                    id="model" 
                    class="form-control" 
                    v-model="form.model"
                  >
                </div>

                <!-- 购买日期 -->
                <div class="col-md-6">
                  <label for="purchaseDate" class="form-label">购买日期</label>
                  <input 
                    type="date" 
                    id="purchaseDate" 
                    class="form-control" 
                    v-model="form.purchaseDate"
                  >
                </div>

                <!-- 购买价格 -->
                <div class="col-md-6">
                  <label for="purchasePrice" class="form-label">购买价格</label>
                  <div class="input-group">
                    <span class="input-group-text">¥</span>
                    <input 
                      type="number" 
                      id="purchasePrice" 
                      class="form-control" 
                      v-model="form.purchasePrice"
                      min="0"
                      step="0.01"
                    >
                  </div>
                </div>

                <!-- 描述 -->
                <div class="col-12">
                  <label for="description" class="form-label">装备描述</label>
                  <textarea 
                    id="description" 
                    class="form-control" 
                    v-model="form.description"
                    rows="3"
                    placeholder="请输入装备的详细描述信息"
                  ></textarea>
                </div>

                <!-- 提交按钮 -->
                <div class="col-12 mt-4 text-end">
                  <button type="button" class="btn btn-outline-secondary me-2" @click="resetForm">取消</button>
                  <button 
                    type="submit" 
                    class="btn btn-primary" 
                    :disabled="submitting"
                  >
                    <span class="spinner-border spinner-border-sm me-1" v-if="submitting"></span>
                    {{ isEditMode ? '保存修改' : '创建装备' }}
                  </button>
                </div>
              </div>
            </form>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.card {
  border-radius: 0.75rem;
}

.form-control,
.form-select,
.input-group {
  border-radius: 0.5rem;
}

.btn {
  border-radius: 0.5rem;
}

.list-group-item {
  border-left: none;
  border-right: none;
}

.list-group-item:first-child {
  border-top: none;
}

.list-group-item:last-child {
  border-bottom: none;
}
</style> 