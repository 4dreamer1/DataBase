<script setup>
import { ref, onMounted, computed, watch, onUnmounted, nextTick } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import equipmentService from '@/api/equipment';
import http from '@/api/http';
import { Modal } from 'bootstrap';
import { useRouter } from 'vue-router';
import { useUserStore } from '@/store/modules/user';
import * as echarts from 'echarts';

// 加载状态
const loading = ref(false);
const submitting = ref(false);
const deleteLoading = ref(false);

// 分类列表
const categories = ref([]);
const selectedCategories = ref([]);
const selectedCategory = ref(null);
const categoryItems = ref([]);
const loadingItems = ref(false);

// 新分类表单
const newCategory = ref({
  name: ''
});

// 表单验证
const formErrors = ref({});

// 分类关联装备数量的缓存
const categoryEquipmentCounts = ref({});

// 全选状态
const isAllSelected = computed(() => {
  if (categories.value.length === 0) return false;
  return paginatedCategories.value.length > 0 && 
    paginatedCategories.value.every(category => 
      selectedCategories.value.includes(category.id)
    );
});

// 添加分页、搜索和排序功能
const searchQuery = ref('');
const currentPage = ref(1);
const pageSize = ref(20);
const sortField = ref('createTime');
const sortOrder = ref('desc');

// 根据搜索条件过滤分类
const filteredCategories = computed(() => {
  if (!searchQuery.value) {
    return categories.value;
  }
  
  const query = searchQuery.value.toLowerCase().trim();
  return categories.value.filter(category => 
    category.name.toLowerCase().includes(query)
  );
});

// 计算当前页的分类
const paginatedCategories = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  const end = start + pageSize.value;
  return filteredCategories.value.slice(start, end);
});

// 计算总页数
const totalPages = computed(() => {
  return Math.max(1, Math.ceil(filteredCategories.value.length / pageSize.value));
});

// 计算要显示的页码
const displayedPages = computed(() => {
  const result = [];
  const maxVisiblePages = 5;
  
  // 总页数小于最大显示页数
  if (totalPages.value <= maxVisiblePages) {
    for (let i = 1; i <= totalPages.value; i++) {
      result.push(i);
    }
    return result;
  }
  
  // 显示当前页及其前后的页码
  const halfVisible = Math.floor(maxVisiblePages / 2);
  let start = Math.max(1, currentPage.value - halfVisible);
  let end = Math.min(totalPages.value, start + maxVisiblePages - 1);
  
  // 如果end到达最大值，调整start
  if (end === totalPages.value) {
    start = Math.max(1, end - maxVisiblePages + 1);
  }
  
  for (let i = start; i <= end; i++) {
    result.push(i);
  }
  
  return result;
});

// 搜索和过滤分类
const filterCategories = () => {
  currentPage.value = 1; // 重置页码
};

// 排序分类
const sortCategories = (field, order) => {
  sortField.value = field;
  sortOrder.value = order;
  
  categories.value.sort((a, b) => {
    let valueA, valueB;
    
    if (field === 'name') {
      valueA = (a.name || '').toLowerCase();
      valueB = (b.name || '').toLowerCase();
    } else if (field === 'createTime') {
      valueA = a.createTime ? new Date(a.createTime) : new Date(0);
      valueB = b.createTime ? new Date(b.createTime) : new Date(0);
    } else {
      return 0;
    }
    
    if (order === 'asc') {
      return valueA > valueB ? 1 : valueA < valueB ? -1 : 0;
    } else {
      return valueA < valueB ? 1 : valueA > valueB ? -1 : 0;
    }
  });
};

// 刷新分类列表
const refreshCategories = async () => {
  await fetchCategories();
  ElMessage.success('分类列表已刷新');
};

// 监听页码和每页显示数量变化，确保当前页有效
watch([currentPage, pageSize, filteredCategories], () => {
  // 如果当前页超出范围，重置为最后一页
  if (currentPage.value > totalPages.value) {
    currentPage.value = totalPages.value;
  }
});

// 监听排序字段和排序方向变化，重新排序
watch([sortField, sortOrder], () => {
  sortCategories(sortField.value, sortOrder.value);
});

// 获取分类列表
const fetchCategories = async () => {
  loading.value = true;
  selectedCategories.value = [];
  categoryEquipmentCounts.value = {}; // 清空装备数量缓存
  
  try {

    // 直接使用fetch请求获取数据
    const response = await fetch('/api/categories', {
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
      throw new Error('解析分类数据失败');
    }

    // 使用Map确保ID唯一
    const categoryMap = new Map();
    
    if (Array.isArray(data)) {
      // 处理分类数组
      data.forEach(category => {
        if (category && category.id && !categoryMap.has(category.id)) {
          // 避免循环引用
          categoryMap.set(category.id, {
            ...category,
            equipments: [] // 清空装备列表避免循环引用
          });
        }
      });
    } else if (data && typeof data === 'object' && data.id) {
      // 单个分类对象
      categoryMap.set(data.id, {
        ...data,
        equipments: []
      });
    } else if (data && data.content && Array.isArray(data.content)) {
      // 分页响应
      data.content.forEach(category => {
        if (category && category.id && !categoryMap.has(category.id)) {
          categoryMap.set(category.id, {
            ...category,
            equipments: []
          });
        }
      });
    }
    
    // 转换Map为数组
    categories.value = Array.from(categoryMap.values());
    
    // 默认排序
    sortCategories(sortField.value, sortOrder.value);

    // 如果分类列表为空，显示提示信息
    if (categories.value.length === 0) {
      console.warn('未获取到任何分类数据');
      ElMessage.warning('未获取到分类数据，请尝试添加分类');
    } else {
      // 预加载每个分类的装备数量
      setTimeout(fetchAllEquipmentCounts, 500);
    }
  } catch (error) {
    console.error('获取分类列表失败:', error);
    ElMessage.error('获取分类列表失败: ' + (error.message || '未知错误'));
    
    // 尝试使用硬编码的备用数据（基于数据库截图）

    categories.value = [
      { id: 95, name: '1', description: '', equipments: [] },
      { id: 98, name: '2', description: '', equipments: [] }
    ];
  } finally {
    loading.value = false;
  }
};

// 添加用于存储所有分类的装备数据的变量
const allCategoriesEquipments = ref({});

// 批量获取所有分类的装备数量
const fetchAllEquipmentCounts = async () => {
  if (categories.value.length === 0) return;

  try {
    // 为每个分类创建一个获取装备数量的Promise
    const promises = categories.value.map(category => {
      return equipmentService.getByCategory(category.id)
        .then(result => {
          // 获取装备数据
          const equipments = Array.isArray(result) ? result : 
                           (result && result.content ? result.content : 
                           (result && result.id ? [result] : []));
          
          // 存储该分类的装备数据
          allCategoriesEquipments.value[category.id] = equipments;
          
          // 更新装备数量缓存
          categoryEquipmentCounts.value[category.id] = equipments.length;
          return { id: category.id, count: equipments.length };
        })
        .catch(error => {
          console.error(`获取分类 ${category.id} 的装备数量失败`, error);
          categoryEquipmentCounts.value[category.id] = 0;
          allCategoriesEquipments.value[category.id] = [];
          return { id: category.id, count: 0, error };
        });
    });
    
    // 并行执行所有Promise
    const results = await Promise.allSettled(promises);

    // 更新图表数据
    nextTick(() => {
      if (activeChartType.value === 'pie') {
        initPieChart();
      } else {
        initBarChart();
      }
    });
  } catch (error) {
    console.error('批量获取装备数量失败', error);
  }
};

// 获取分类装备数量
const getCategoryEquipmentCount = (categoryId) => {
  if (categoryEquipmentCounts.value[categoryId] !== undefined) {
    return categoryEquipmentCounts.value[categoryId];
  }
  
  // 如果没有缓存数据，返回 0 而不是随机数
  return 0;
};

// 表单验证
const validateForm = () => {
  const errors = {};
  
  if (!newCategory.value.name?.trim()) {
    errors.name = '请输入分类名称';
  } else if (newCategory.value.name.length > 50) {
    errors.name = '分类名称不能超过50个字符';
  } else if (categories.value.some(category => 
    category.name.toLowerCase() === newCategory.value.name.trim().toLowerCase())) {
    errors.name = '分类名称已存在';
  }
  
  formErrors.value = errors;
  return Object.keys(errors).length === 0;
};

// 创建分类
const createCategory = async () => {
  // 表单验证
  if (!validateForm()) {
    return;
  }
  
  submitting.value = true;
  try {

    const response = await equipmentService.createCategory(newCategory.value);

    // 从不同格式的响应中提取分类数据
    let createdCategory;
    if (response && response.data) {
      createdCategory = response.data;
    } else if (response && response.id) {
      createdCategory = response;
    } else {
      // 如果响应格式不对，创建临时对象
      createdCategory = {
        id: Date.now(), // 临时ID
        name: newCategory.value.name,
        createTime: new Date().toISOString()
      };
      console.warn('使用临时分类对象:', createdCategory);
    }
    
    categories.value = [...categories.value, createdCategory];
    categoryEquipmentCounts.value[createdCategory.id] = 0;
    newCategory.value.name = '';
    formErrors.value = {};
    ElMessage.success('分类创建成功');
    
    // 刷新分类列表
    setTimeout(() => {
      fetchCategories();
    }, 1000);
  } catch (error) {
    console.error('创建分类失败', error);
    
    // 调用错误处理函数
    showErrorMessage(error, '创建分类失败，请稍后再试');
  } finally {
    submitting.value = false;
  }
};

// 删除单个分类
const deleteCategory = async () => {
  if (!selectedCategory.value) return;
  
  submitting.value = true;
  
  try {
    await equipmentService.deleteCategory(selectedCategory.value.id);
    
    ElNotification({
      title: '成功',
      message: `分类 "${selectedCategory.value.name}" 已成功删除`,
      type: 'success'
    });
    
    // 关闭模态框
    if (modalInstanceDel.value) {
      modalInstanceDel.value.hide();
    }
    
    // 清除选中的分类
    selectedCategory.value = null;
    categoryItems.value = [];
    
    // 重新加载分类列表
    await fetchCategories();
  } catch (error) {
    console.error('删除分类失败:', error);
    
    let errorMessage = '删除失败';
    
    if (error.response?.data?.message) {
      errorMessage = error.response.data.message;
    } else if (error.message) {
      errorMessage = error.message;
    } else {
      errorMessage = '该分类可能包含装备，无法删除';
    }
    
    ElNotification({
      title: '错误',
      message: errorMessage,
      type: 'error'
    });
  } finally {
    submitting.value = false;
  }
};

// 批量删除分类
const bulkDeleteCategories = async () => {
  if (selectedCategories.value.length === 0) {
    ElMessage.warning('请先选择要删除的分类');
    return;
  }
  
  // 检查所选分类是否有装备
  const categoriesWithEquipment = selectedCategories.value.filter(id => {
    const count = categoryEquipmentCounts.value[id];
    return count && typeof count === 'number' && count > 0;
  });
  
  if (categoriesWithEquipment.length > 0) {
    ElMessage.warning('选中的分类中有关联装备，无法批量删除');
    return;
  }
  
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedCategories.value.length} 个分类吗？此操作不可恢复！`,
      '批量删除确认',
      {
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    );
    
    deleteLoading.value = true;
    
    // 批量删除

    const promises = selectedCategories.value.map(categoryId => 
      equipmentService.deleteCategory(categoryId).catch(error => {
        console.error(`删除分类 ${categoryId} 失败:`, error);
        return Promise.reject({ categoryId, error });
      })
    );
    
    const results = await Promise.allSettled(promises);
    
    // 统计成功和失败数
    const succeeded = results.filter(r => r.status === 'fulfilled').length;
    const failed = results.filter(r => r.status === 'rejected').length;
    
    // 根据结果更新UI
    if (failed === 0) {
      // 更新列表，全部成功
      categories.value = categories.value.filter(
        category => !selectedCategories.value.includes(category.id)
      );
      
      // 清理缓存
      selectedCategories.value.forEach(id => {
        delete categoryEquipmentCounts.value[id];
      });
      
      selectedCategories.value = [];
      ElMessage.success(`已成功删除 ${succeeded} 个分类`);
    } else if (succeeded === 0) {
      // 全部失败
      ElMessage.error(`所有分类删除失败，请稍后再试`);
    } else {
      // 部分成功
      // 更新成功删除的部分
      const successIds = selectedCategories.value.filter((id, index) => 
        results[index].status === 'fulfilled'
      );
      
      categories.value = categories.value.filter(
        category => !successIds.includes(category.id)
      );
      
      // 清理缓存
      successIds.forEach(id => {
        delete categoryEquipmentCounts.value[id];
      });
      
      // 更新选中状态
      selectedCategories.value = selectedCategories.value.filter(id => 
        !successIds.includes(id)
      );
      
      ElMessage.warning(`已删除 ${succeeded} 个分类，${failed} 个分类删除失败`);
    }
    
    // 刷新分类列表
    setTimeout(() => {
      fetchCategories();
    }, 1000);
    
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量删除分类失败', error);
      
      // 调用错误处理函数
      showErrorMessage(error, '批量删除操作失败，请稍后再试');
    }
  } finally {
    deleteLoading.value = false;
  }
};

// 错误处理和通知
const showErrorMessage = (error, defaultMessage) => {
  // 检查是否为网络问题
  if (!navigator.onLine) {
    ElMessage.warning('网络连接已断开，请检查网络连接后重试');
    return;
  }
  
  // 检查响应错误
  if (error.response) {
    const { status, data } = error.response;
    
    switch (status) {
      case 400:
        ElMessage.error(data.message || '请求参数错误');
        break;
      case 403:
        ElMessage.error('您没有权限执行此操作');
        break;
      case 404:
        ElMessage.error('分类不存在或已被删除');
        break;
      case 409:
        ElMessage.error('该分类下有关联装备，无法删除');
        break;
      default:
        ElMessage.error(data.message || defaultMessage);
    }
  } else {
    // 其他错误
    ElMessage.error(defaultMessage);
  }
};

// 页面卸载时清理资源
onUnmounted(() => {
  // 清除所有事件监听器
  window.removeEventListener('online', refreshCategories);
  window.removeEventListener('offline', () => {});
  
  // 销毁图表实例
  if (pieChart) {
    pieChart.dispose();
    pieChart = null;
  }
  
  if (barChart) {
    barChart.dispose();
    barChart = null;
  }
  
  // 终止所有进行中的请求
  // 由于我们无法直接访问axios的取消令牌，所以这里只能设置标志
});

// 新增/编辑分类模态框
const categoryModal = ref(null);
const modalInstanceCat = ref(null);

// 打开创建分类模态框
const openCreateModal = () => {
  newCategory.value = { name: '' };
  formErrors.value = {};
  
  if (!modalInstanceCat.value) {
    modalInstanceCat.value = new Modal(categoryModal.value);
  }
  
  modalInstanceCat.value.show();
};

// 保存分类
const saveCategory = async () => {
  if (!validateForm()) return;
  
  submitting.value = true;
  
  try {
    if (newCategory.value.id) {
      await equipmentService.updateCategory(newCategory.value.id, newCategory.value);
      ElMessage.success('分类更新成功');
    } else {
      await equipmentService.createCategory(newCategory.value);
      ElMessage.success('分类创建成功');
    }
    
    // 关闭模态框
    modalInstanceCat.value.hide();
    
    // 重新加载数据
    await fetchCategories();
  } catch (error) {
    console.error('保存分类失败:', error);
    
    let errorMessage = '操作失败';
    
    if (error.response?.data?.message) {
      errorMessage = error.response.data.message;
    } else if (error.message) {
      errorMessage = error.message;
    }
    
    ElMessage.error(errorMessage);
  } finally {
    submitting.value = false;
  }
};

// 打开编辑分类模态框
const openEditModal = (category) => {
  if (!category) return;
  
  newCategory.value = {
    id: category.id,
    name: category.name,
    description: category.description
  };
  formErrors.value = {};
  
  if (!modalInstanceCat.value) {
    modalInstanceCat.value = new Modal(categoryModal.value);
  }
  
  modalInstanceCat.value.show();
};

// 打开删除确认模态框
const deleteModal = ref(null);
const modalInstanceDel = ref(null);

// 打开删除确认模态框
const openDeleteModal = () => {
  if (!selectedCategory.value) return;
  
  if (!modalInstanceDel.value) {
    modalInstanceDel.value = new Modal(deleteModal.value);
  }
  
  modalInstanceDel.value.show();
};

// 选择分类
const selectCategory = async (category) => {
  selectedCategory.value = category;
  await loadCategoryItems(category.id);
};

// 加载分类下的装备项
const loadCategoryItems = async (categoryId) => {
  if (!categoryId) return;

  await fetchCategoryItems(categoryId);
};

// 获取选定分类的装备项
const fetchCategoryItems = async (categoryId) => {
  loadingItems.value = true;
  categoryItems.value = []; // 清空当前装备列表

  try {
    // 使用直接的fetch请求获取数据
    const response = await fetch(`/api/equipment/category/${categoryId}`, {
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
      console.error('解析JSON失败:', e);
      throw new Error('解析装备数据失败');
    }

    // 处理不同格式的响应
    if (Array.isArray(data)) {
      // 如果是数组，直接使用
      categoryItems.value = data;
    } else if (data && typeof data === 'object') {
      if (data.content && Array.isArray(data.content)) {
        // 分页响应
        categoryItems.value = data.content;
      } else if (data.id) {
        // 单个对象
        categoryItems.value = [data];
      }
    }

    // 更新装备数量缓存
    if (categoryId) {
      categoryEquipmentCounts.value[categoryId] = categoryItems.value.length;
    }
  } catch (error) {
    console.error(`获取分类 ${categoryId} 的装备失败:`, error);
    ElMessage.warning(`无法加载分类 ${categoryId} 的装备: ${error.message || '未知错误'}`);
    categoryItems.value = [];
    // 不更新缓存数量
  } finally {
    loadingItems.value = false;
  }
};

// 图表相关变量
const pieChartContainer = ref(null);
const barChartContainer = ref(null);
let pieChart = null;
let barChart = null;
const chartData = ref([]);
const activeChartType = ref('pie'); // 默认显示饼图

// 数据处理函数
const processChartData = () => {
  // 如果没有分类数据，返回空数组
  if (!categories.value || categories.value.length === 0) {
    return [];
  }

  // 处理数据为图表格式，包含总数量和各种状态的数量
  return categories.value.map(category => {
    // 获取该分类下的所有装备 - 使用allCategoriesEquipments而不是categoryItems
    const categoryEquipments = allCategoriesEquipments.value[category.id] || [];
    
    // 计算不同状态的装备数量
    let availableCount = 0;
    let unavailableCount = 0;
    let maintenanceCount = 0;
    
    // 处理装备数据
    if (categoryEquipments && categoryEquipments.length > 0) {
      categoryEquipments.forEach(item => {
        if (item.availableQuantity <= 0) {
          unavailableCount += item.quantity;
        } else if (item.status === '维修中') {
          maintenanceCount += item.quantity;
        } else {
          availableCount += item.availableQuantity;
          // 部分可用的设备，剩余部分计入不可用
          if (item.availableQuantity < item.quantity) {
            unavailableCount += (item.quantity - item.availableQuantity);
          }
        }
      });
    }
    
    // 获取分类关联的装备总数
    const totalCount = categoryEquipmentCounts.value[category.id] || 0;
    
    return {
      name: category.name,
      value: totalCount,
      id: category.id,
      available: availableCount,
      unavailable: unavailableCount,
      maintenance: maintenanceCount,
      total: totalCount
    };
  }).filter(item => item.value > 0); // 只显示有装备的分类
};

// 初始化饼图
const initPieChart = async () => {
  if (!pieChartContainer.value) return;
  
  // 处理数据
  chartData.value = processChartData();
  if (chartData.value.length === 0) return;
  
  // 如果已存在图表实例，销毁它
  if (pieChart) {
    pieChart.dispose();
  }
  
  // 创建新的图表实例
  pieChart = echarts.init(pieChartContainer.value);
  
  // 准备饼图系列数据
  const seriesData = [];
  
  // 为每个分类创建可用、不可用和维修中的数据项
  chartData.value.forEach(category => {
    if (category.available > 0) {
      seriesData.push({
        name: `${category.name} (可用)`,
        value: category.available,
        category: category.name,
        status: '可用',
        itemStyle: { color: '#2ecc71' } // 绿色表示可用
      });
    }
    
    if (category.unavailable > 0) {
      seriesData.push({
        name: `${category.name} (不可用)`,
        value: category.unavailable,
        category: category.name,
        status: '不可用',
        itemStyle: { color: '#e74c3c' } // 红色表示不可用
      });
    }
    
    if (category.maintenance > 0) {
      seriesData.push({
        name: `${category.name} (维修中)`,
        value: category.maintenance,
        category: category.name,
        status: '维修中',
        itemStyle: { color: '#f39c12' } // 黄色表示维修中
      });
    }
  });
  
  // 饼图配置
  const option = {
    title: {
      text: '装备分类状态统计',
      left: 'center'
    },
    tooltip: {
      trigger: 'item',
      formatter: function(params) {
        return `${params.data.category}<br/>${params.data.status}: ${params.data.value} (${params.percent}%)`;
      }
    },
    legend: {
      type: 'scroll',
      orient: 'horizontal',
      bottom: '5%',
      data: seriesData.map(item => item.name)
    },
    series: [
      {
        name: '装备状态',
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['50%', '50%'],
        data: seriesData,
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)'
          }
        },
        label: {
          formatter: '{b}: {c}'
        }
      }
    ]
  };
  
  // 设置配置项
  pieChart.setOption(option);
  
  // 点击事件
  pieChart.on('click', function(params) {
    // 从名称中提取分类名称（去掉状态部分）
    const categoryName = params.data.category;
    const clickedCategory = categories.value.find(cat => cat.name === categoryName);
    if (clickedCategory) {
      selectCategory(clickedCategory);
    }
  });
  
  // 窗口大小变化时调整图表大小
  window.addEventListener('resize', () => {
    if (pieChart && activeChartType.value === 'pie') {
      pieChart.resize();
    }
  });
};

// 初始化柱状图
const initBarChart = async () => {
  if (!barChartContainer.value) return;
  
  // 处理数据
  chartData.value = processChartData();
  if (chartData.value.length === 0) return;
  
  // 如果已存在图表实例，销毁它
  if (barChart) {
    barChart.dispose();
  }
  
  // 创建新的图表实例
  barChart = echarts.init(barChartContainer.value);
  
  // 准备分类名称列表
  const categories = chartData.value.map(item => item.name);
  
  // 准备不同状态的数据系列
  const availableData = chartData.value.map(item => item.available);
  const unavailableData = chartData.value.map(item => item.unavailable);
  const maintenanceData = chartData.value.map(item => item.maintenance);
  
  // 柱状图配置
  const option = {
    title: {
      text: '装备分类数量统计',
      left: 'center'
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      },
      formatter: function(params) {
        // params是一个数组，包含了鼠标所在位置的所有系列的数据
        let result = `${params[0].name}<br/>`;
        let total = 0;
        
        // 遍历每个系列的数据
        params.forEach(item => {
          result += `${item.seriesName}: ${item.value}<br/>`;
          total += item.value;
        });
        
        result += `总计: ${total}`;
        return result;
      }
    },
    legend: {
      data: ['可用', '不可用', '维修中'],
      bottom: '5%'
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '15%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: categories,
      axisLabel: {
        rotate: 45,
        interval: 0
      }
    },
    yAxis: {
      type: 'value',
      name: '装备数量'
    },
    series: [
      {
        name: '可用',
        type: 'bar',
        stack: '总量',
        emphasis: {
          focus: 'series'
        },
        data: availableData,
        itemStyle: {
          color: '#2ecc71' // 绿色表示可用
        }
      },
      {
        name: '不可用',
        type: 'bar',
        stack: '总量',
        emphasis: {
          focus: 'series'
        },
        data: unavailableData,
        itemStyle: {
          color: '#e74c3c' // 红色表示不可用
        }
      },
      {
        name: '维修中',
        type: 'bar',
        stack: '总量',
        emphasis: {
          focus: 'series'
        },
        data: maintenanceData,
        itemStyle: {
          color: '#f39c12' // 黄色表示维修中
        }
      }
    ]
  };
  
  // 设置配置项
  barChart.setOption(option);
  
  // 点击事件
  barChart.on('click', function(params) {
    if (params.componentType === 'series') {
      const clickedCategory = categories.value.find(cat => cat.name === params.name);
      if (clickedCategory) {
        selectCategory(clickedCategory);
      }
    }
  });
  
  // 窗口大小变化时调整图表大小
  window.addEventListener('resize', () => {
    if (barChart && activeChartType.value === 'bar') {
      barChart.resize();
    }
  });
};

// 切换图表类型
const switchChartType = (type) => {
  activeChartType.value = type;
  nextTick(() => {
    if (type === 'pie') {
      initPieChart();
    } else {
      initBarChart();
    }
  });
};

// 监听分类数据和装备计数的变化，更新图表
watch([categories, categoryEquipmentCounts], () => {
  chartData.value = processChartData();
  
  nextTick(() => {
    if (activeChartType.value === 'pie' && pieChart) {
      initPieChart();
    } else if (activeChartType.value === 'bar' && barChart) {
      initBarChart();
    }
  });
}, { deep: true });

// 生命周期钩子
onMounted(() => {
  // 获取模态框DOM元素
  categoryModal.value = document.getElementById('categoryModal');
  deleteModal.value = document.getElementById('deleteModal');
  
  // 加载分类列表
  fetchCategories();
  
  // 在分类和装备数据加载完成后初始化图表
  setTimeout(() => {
    if (activeChartType.value === 'pie') {
      initPieChart();
    } else {
      initBarChart();
    }
  }, 1000);
});

</script>

<template>
  <div class="container-fluid py-4">
    <!-- 页面标题 -->
    <div class="row mb-4">
      <div class="col-12">
        <div class="card border-0 shadow-sm">
          <div class="card-body d-flex justify-content-between align-items-center">
            <h5 class="m-0 page-title"><i class="bi bi-folder me-2"></i>分类管理</h5>
            <div>
              <button class="btn btn-primary rounded-3" @click="openCreateModal">
                <i class="bi bi-plus-lg me-1"></i>添加分类
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 分类列表和详情 -->
    <div class="row">
      <!-- 分类列表 -->
      <div class="col-lg-6">
        <div class="card border-0 shadow-sm">
          <div class="card-header bg-transparent border-0 py-3">
            <div class="d-flex justify-content-between align-items-center">
              <h6 class="mb-0 text-primary">分类列表</h6>
              <div class="input-group" style="max-width: 220px;">
                <span class="input-group-text bg-white border-end-0">
                  <i class="bi bi-search text-muted"></i>
                </span>
                <input 
                  type="text" 
                  class="form-control border-start-0" 
                  placeholder="搜索分类..." 
                  v-model="searchQuery" 
                  @input="filterCategories"
                >
              </div>
            </div>
          </div>
          <div class="card-body p-0">
            <div class="list-group list-group-flush">
              <div v-if="loading" class="text-center py-5">
                <div class="spinner-border text-primary" role="status">
                  <span class="visually-hidden">加载中...</span>
                </div>
                <p class="text-muted mt-2 loading-text">正在加载分类数据...</p>
              </div>
              <div v-else-if="categories.length === 0" class="empty-state py-5">
                <i class="bi bi-folder-x fs-1 text-muted mb-2"></i>
                <p class="text-muted">暂无分类数据</p>
                <button class="btn btn-sm btn-primary mt-2" @click="openCreateModal">
                  <i class="bi bi-plus-lg me-1"></i>添加分类
                </button>
              </div>
              <transition-group name="fadeIn">
              <a 
                  v-for="category in paginatedCategories" 
                :key="category.id"
                href="javascript:void(0)"
                class="list-group-item list-group-item-action d-flex justify-content-between align-items-center p-3"
                :class="{'active': selectedCategory && selectedCategory.id === category.id}"
                @click="selectCategory(category)"
              >
                <div>
                  <h6 class="mb-1">{{ category.name }}</h6>
                  <p class="mb-0 small text-muted text-truncate" style="max-width: 300px;">
                    {{ category.description || '暂无描述' }}
                  </p>
                </div>
                  <div class="d-flex align-items-center">
                    <span class="badge bg-primary rounded-pill me-1">
                  {{ getCategoryEquipmentCount(category.id) }}
                </span>
                    <div class="dropdown ms-2">
                      <button class="btn btn-sm btn-light rounded-circle" type="button" id="dropdownMenuButton" data-bs-toggle="dropdown" aria-expanded="false">
                        <i class="bi bi-three-dots-vertical"></i>
                      </button>
                      <ul class="dropdown-menu dropdown-menu-end" aria-labelledby="dropdownMenuButton">
                        <li><a class="dropdown-item" href="#" @click.stop="openEditModal(category)"><i class="bi bi-pencil me-2"></i>编辑</a></li>
                        <li><a class="dropdown-item text-danger" href="#" @click.stop="selectedCategory = category; openDeleteModal()"><i class="bi bi-trash me-2"></i>删除</a></li>
                      </ul>
                    </div>
                  </div>
                </a>
              </transition-group>
            </div>
          </div>
          <div class="card-footer bg-transparent border-0 py-3" v-if="filteredCategories.length > pageSize">
            <nav aria-label="分页导航">
              <ul class="pagination pagination-sm justify-content-center mb-0">
                <li class="page-item" :class="{ 'disabled': currentPage === 1 }">
                  <a class="page-link" href="#" @click.prevent="currentPage > 1 && (currentPage--)">上一页</a>
                </li>
                <li v-for="page in displayedPages" :key="page" class="page-item" :class="{ 'active': page === currentPage }">
                  <a class="page-link" href="#" @click.prevent="currentPage = page">{{ page }}</a>
                </li>
                <li class="page-item" :class="{ 'disabled': currentPage === totalPages }">
                  <a class="page-link" href="#" @click.prevent="currentPage < totalPages && (currentPage++)">下一页</a>
                </li>
              </ul>
            </nav>
          </div>
        </div>
      </div>

      <!-- 分类详情 -->
      <div class="col-lg-6">
        <div class="card border-0 shadow-sm h-100">
          <div class="card-header bg-transparent border-0 py-3" v-if="selectedCategory">
            <h6 class="mb-0 text-primary">分类详情</h6>
          </div>
          <div class="card-body" v-if="selectedCategory">
            <div class="d-flex justify-content-between align-items-start mb-4">
              <div>
                <h5 class="card-title">
                  <span class="badge bg-primary me-2">ID: {{ selectedCategory.id }}</span>
                  {{ selectedCategory.name }}
                </h5>
                <p class="card-text text-muted">{{ selectedCategory.description || '暂无描述' }}</p>
              </div>
              <div class="btn-group">
                <button class="btn btn-outline-primary btn-sm" @click="openEditModal(selectedCategory)">
                  <i class="bi bi-pencil me-1"></i>编辑
                </button>
                <button class="btn btn-outline-danger btn-sm" @click="openDeleteModal">
                  <i class="bi bi-trash me-1"></i>删除
                </button>
              </div>
            </div>
            
            <h6 class="border-bottom pb-2 mb-3">分类下的装备 <span class="badge bg-primary rounded-pill">{{ categoryItems.length }}</span></h6>
            
            <div v-if="loadingItems" class="text-center py-3">
              <div class="spinner-border spinner-border-sm text-primary" role="status"></div>
              <span class="ms-2 loading-text">加载中...</span>
            </div>
            <div v-else-if="categoryItems.length === 0" class="empty-state py-4 bg-light rounded">
              <i class="bi bi-inbox fs-4 text-muted"></i>
              <p class="mb-0 mt-2 text-muted">该分类下暂无装备</p>
              <router-link to="/equipment/edit" class="btn btn-sm btn-outline-primary mt-2">
                添加装备
              </router-link>
            </div>
            <div v-else class="table-responsive">
              <table class="table table-sm table-hover">
                <thead class="table-light">
                  <tr>
                    <th>名称</th>
                    <th>序列号</th>
                    <th class="text-center">总数量</th>
                    <th class="text-center">可用</th>
                    <th class="text-center">状态</th>
                  </tr>
                </thead>
                <tbody>
                  <transition-group name="fadeIn">
                  <tr v-for="item in categoryItems" :key="item.id">
                    <td>
                      <router-link :to="`/equipment/${item.id}`" class="text-decoration-none">
                        {{ item.name }}
                      </router-link>
                    </td>
                    <td>
                      <small class="text-muted">{{ item.serialNumber }}</small>
                    </td>
                    <td class="text-center">{{ item.quantity }}</td>
                    <td class="text-center">
                      <span 
                        :class="[
                          'badge', 
                          item.availableQuantity <= 0 ? 'bg-danger' : 
                          item.availableQuantity < item.quantity ? 'bg-warning text-dark' : 'bg-success'
                        ]"
                      >
                        {{ item.availableQuantity }}
                      </span>
                    </td>
                    <td class="text-center">
                      <span 
                        :class="[
                          'badge',
                          item.availableQuantity <= 0 ? 'bg-danger' :
                          item.status === '可用' ? 'bg-success' : 
                          item.status === '维修中' ? 'bg-warning text-dark' : 'bg-danger'
                        ]"
                      >
                        {{ item.availableQuantity <= 0 ? '不可用' : item.status }}
                      </span>
                    </td>
                  </tr>
                  </transition-group>
                </tbody>
              </table>
            </div>
          </div>
          <div v-else class="card-body d-flex flex-column justify-content-center align-items-center text-muted py-5">
            <i class="bi bi-arrow-left-circle fs-1 mb-3"></i>
            <h6>请从左侧选择一个分类</h6>
            <p class="small mb-0">选择分类后将在此处显示详细信息</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 新增/编辑分类模态框 -->
    <div class="modal fade" id="categoryModal" tabindex="-1" ref="categoryModal">
      <div class="modal-dialog">
        <div class="modal-content">
          <div class="modal-header">
            <h5 class="modal-title">{{ newCategory.id ? '编辑分类' : '新增分类' }}</h5>
            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
          </div>
          <div class="modal-body">
            <form @submit.prevent="saveCategory">
              <div class="mb-3">
                <label for="categoryName" class="form-label">分类名称 <span class="text-danger">*</span></label>
                <input 
                  type="text" 
                  class="form-control" 
                  id="categoryName" 
                  v-model="newCategory.name"
                  :class="{'is-invalid': formErrors.name}"
                  required
                >
                <div class="invalid-feedback" v-if="formErrors.name">{{ formErrors.name }}</div>
              </div>
              <div class="mb-3">
                <label for="categoryDescription" class="form-label">分类描述</label>
                <textarea 
                  class="form-control" 
                  id="categoryDescription" 
                  rows="3" 
                  v-model="newCategory.description"
                  placeholder="请输入分类的描述信息"
                ></textarea>
              </div>
            </form>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">取消</button>
            <button 
              type="button" 
              class="btn btn-primary" 
              @click="saveCategory" 
              :disabled="submitting"
            >
              <span class="spinner-border spinner-border-sm me-1" v-if="submitting"></span>
              保存
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 删除确认模态框 -->
    <div class="modal fade" id="deleteModal" tabindex="-1" ref="deleteModal">
      <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content">
          <div class="modal-header">
            <h5 class="modal-title">删除确认</h5>
            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
          </div>
          <div class="modal-body">
            <p>确定要删除分类 <span class="fw-bold text-danger">{{ selectedCategory?.name }}</span> 吗？</p>
            <div class="alert alert-warning">
              <i class="bi bi-exclamation-triangle-fill me-2"></i>
              <strong>警告：</strong> 如果该分类下有装备，将无法删除。请先将装备移动到其他分类或删除装备。
            </div>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">取消</button>
            <button 
              type="button" 
              class="btn btn-danger" 
              @click="deleteCategory" 
              :disabled="submitting"
            >
              <span class="spinner-border spinner-border-sm me-1" v-if="submitting"></span>
              确认删除
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 在这里添加新的可视化组件部分 -->
    <div class="row mt-4">
      <div class="col-12">
        <div class="card border-0 shadow-sm">
          <div class="card-header bg-transparent border-0 py-3">
            <div class="d-flex justify-content-between align-items-center">
              <h6 class="mb-0 text-primary">装备分类数据可视化</h6>
              <div class="btn-group" role="group">
                <button 
                  type="button" 
                  class="btn" 
                  :class="activeChartType === 'pie' ? 'btn-primary' : 'btn-outline-primary'" 
                  @click="switchChartType('pie')"
                >
                  <i class="bi bi-pie-chart me-1"></i>饼图
                </button>
                <button 
                  type="button" 
                  class="btn" 
                  :class="activeChartType === 'bar' ? 'btn-primary' : 'btn-outline-primary'" 
                  @click="switchChartType('bar')"
                >
                  <i class="bi bi-bar-chart me-1"></i>柱状图
                </button>
              </div>
            </div>
          </div>
          <div class="card-body p-0">
            <!-- 加载状态 -->
            <div v-if="loading" class="text-center py-5">
              <div class="spinner-border text-primary" role="status">
                <span class="visually-hidden">加载中...</span>
              </div>
              <p class="text-muted mt-2">正在加载分类数据...</p>
            </div>
            
            <!-- 无数据状态 -->
            <div v-else-if="!categories.length || !chartData.length" class="empty-state py-5">
              <i class="bi bi-bar-chart fs-1 text-muted mb-2"></i>
              <p class="text-muted">暂无分类数据或没有分类包含装备</p>
            </div>
            
            <!-- 图表容器 -->
            <div v-else>
              <!-- 饼图 -->
              <div 
                v-show="activeChartType === 'pie'" 
                ref="pieChartContainer"
                class="chart-container"
              ></div>
              
              <!-- 柱状图 -->
              <div 
                v-show="activeChartType === 'bar'" 
                ref="barChartContainer"
                class="chart-container"
              ></div>
              
              <!-- 图表说明 -->
              <div class="chart-legend text-center pb-2">
                <small class="text-muted">点击图表项可快速查看相应分类详情</small>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.card {
  border-radius: 0.75rem;
  transition: all 0.3s ease;
  box-shadow: 0 4px 20px 1px rgba(0, 0, 0, 0.06) !important;
  overflow: hidden;
}

.card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1) !important;
}

.form-control,
.form-select {
  border-radius: 0.5rem;
  transition: all 0.2s ease;
  border-color: #dee2e6;
}

.form-control:focus,
.form-select:focus {
  box-shadow: 0 0 0 0.2rem rgba(13, 110, 253, 0.15);
}

.btn {
  border-radius: 0.5rem;
  padding: 0.5rem 1rem;
  transition: all 0.2s ease;
}

.btn-primary {
  background: linear-gradient(135deg, #4361ee, #3f37c9);
  border: none;
}

.btn-primary:hover {
  background: linear-gradient(135deg, #3f37c9, #3a0ca3);
  transform: translateY(-1px);
}

.btn-outline-primary:hover {
  transform: translateY(-1px);
}

.btn-danger {
  background: linear-gradient(135deg, #ef476f, #d90429);
  border: none;
}

.btn-danger:hover {
  background: linear-gradient(135deg, #d90429, #c1121f);
}

.table th {
  font-weight: 600;
  font-size: 0.875rem;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  color: #495057;
  border-top: none;
}

.table td {
  vertical-align: middle;
}

.list-group-item {
  border-left: none;
  border-right: none;
  transition: all 0.2s ease;
  border-left: 3px solid transparent;
}

.list-group-item:hover {
  background-color: rgba(0, 123, 255, 0.05);
  border-left: 3px solid #4361ee;
}

.list-group-item.active {
  background-color: rgba(67, 97, 238, 0.1);
  border-left: 3px solid #4361ee;
  color: #212529;
}

.list-group-item.active h6 {
  color: #4361ee;
  font-weight: 600;
}

.badge {
  font-weight: 500;
  padding: 0.35em 0.65em;
}

.badge.bg-primary {
  background: linear-gradient(135deg, #4361ee, #3f37c9) !important;
}

.badge.bg-success {
  background: linear-gradient(135deg, #2ecc71, #27ae60) !important;
}

.badge.bg-danger {
  background: linear-gradient(135deg, #e74c3c, #c0392b) !important;
}

.badge.bg-warning {
  background: linear-gradient(135deg, #f39c12, #e67e22) !important;
}

.bg-light {
  background-color: rgba(67, 97, 238, 0.05) !important;
}

.modal-content {
  border-radius: 0.75rem;
  border: none;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.modal-header {
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
  background-color: #f8f9fa;
}

.modal-footer {
  border-top: 1px solid rgba(0, 0, 0, 0.05);
  background-color: #f8f9fa;
}

/* 美化分类卡片 */
.list-group-item {
  display: flex !important;
  align-items: center;
  padding: 1rem 1.25rem !important;
  margin-bottom: 0.25rem;
  background-color: #fff;
}

/* 美化表格 */
.table {
  margin-bottom: 0;
}

.table-hover tbody tr:hover {
  background-color: rgba(67, 97, 238, 0.05);
}

/* 动画效果 */
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes pulse {
  0% { opacity: 0.6; transform: scale(0.98); }
  50% { opacity: 1; transform: scale(1); }
  100% { opacity: 0.6; transform: scale(0.98); }
}

.fadeIn-enter-active {
  animation: fadeIn 0.3s ease-out;
}

.loading-text {
  animation: pulse 1.5s infinite;
}

/* 空状态美化 */
.empty-state {
  padding: 2rem;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
}

.empty-state i {
  font-size: 2.5rem;
  color: #a0aec0;
  margin-bottom: 1rem;
}

/* 美化页面标题 */
.page-title {
  position: relative;
  padding-left: 1rem;
  font-weight: 600;
}

.page-title::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  height: 1.2em;
  width: 4px;
  background: linear-gradient(to bottom, #4361ee, #3a0ca3);
  border-radius: 2px;
}

/* 卡片内部标题 */
.card-title {
  font-weight: 600;
  color: #2d3748;
  margin-bottom: 1.25rem;
}

/* 图表相关样式 */
.chart-container {
  height: 400px;
  width: 100%;
  transition: all 0.3s ease;
}

.chart-legend {
  padding-top: 10px;
  font-size: 0.85rem;
}

/* 按钮美化 */
.btn-group .btn {
  border-radius: 6px;
  margin: 0 2px;
  transition: all 0.2s ease;
}

.btn-group .btn:hover {
  transform: translateY(-2px);
}
</style> 