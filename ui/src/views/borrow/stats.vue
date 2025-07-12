<template>
  <div class="container-fluid py-4">
    <div class="row mb-4">
      <div class="col-12">
        <h2 class="mb-3 fw-bold text-gradient">借用统计</h2>
        <p class="text-muted">查看装备借用情况统计数据</p>
      </div>
    </div>

    <!-- 加载中 -->
    <div v-if="loading" class="row mb-4">
      <div class="col-12 text-center py-5">
        <div class="spinner-border text-primary spinner-lg" role="status">
          <span class="visually-hidden">加载中...</span>
        </div>
        <div class="mt-3 text-muted">正在加载统计数据...</div>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div v-else class="row mb-4">
      <div class="col-xl-3 col-md-6 mb-4">
        <div class="card border-0 shadow-sm h-100 stat-card">
          <div class="card-body d-flex align-items-center">
            <div class="stat-icon-bg bg-primary me-3">
              <i class="bi bi-box-seam text-white fs-4"></i>
            </div>
            <div>
              <h6 class="text-muted mb-1">当前借出</h6>
              <h3 class="mb-0 fw-bold counter">{{ stats.activeBorrowCount }}</h3>
            </div>
          </div>
          <div class="card-footer bg-transparent border-0 pt-0">
            <div class="progress" style="height: 4px">
              <div class="progress-bar bg-primary" role="progressbar" :style="`width: ${stats.activeBorrowCount / (stats.totalCount || 1) * 100}%`"></div>
            </div>
          </div>
        </div>
      </div>
      
      <div class="col-xl-3 col-md-6 mb-4">
        <div class="card border-0 shadow-sm h-100 stat-card">
          <div class="card-body d-flex align-items-center">
            <div class="stat-icon-bg bg-warning me-3">
              <i class="bi bi-hourglass-split text-white fs-4"></i>
            </div>
            <div>
              <h6 class="text-muted mb-1">待审批</h6>
              <h3 class="mb-0 fw-bold counter">{{ stats.pendingCount }}</h3>
            </div>
          </div>
          <div class="card-footer bg-transparent border-0 pt-0">
            <div class="progress" style="height: 4px">
              <div class="progress-bar bg-warning" role="progressbar" :style="`width: ${stats.pendingCount / (stats.totalCount || 1) * 100}%`"></div>
            </div>
          </div>
        </div>
      </div>
      
      <div class="col-xl-3 col-md-6 mb-4">
        <div class="card border-0 shadow-sm h-100 stat-card">
          <div class="card-body d-flex align-items-center">
            <div class="stat-icon-bg bg-danger me-3">
              <i class="bi bi-exclamation-triangle text-white fs-4"></i>
            </div>
            <div>
              <h6 class="text-muted mb-1">已逾期</h6>
              <h3 class="mb-0 fw-bold counter">{{ stats.overdueCount }}</h3>
            </div>
          </div>
          <div class="card-footer bg-transparent border-0 pt-0">
            <div class="progress" style="height: 4px">
              <div class="progress-bar bg-danger" role="progressbar" :style="`width: ${stats.overdueCount / (stats.totalCount || 1) * 100}%`"></div>
            </div>
          </div>
        </div>
      </div>
      
      <div class="col-xl-3 col-md-6 mb-4">
        <div class="card border-0 shadow-sm h-100 stat-card">
          <div class="card-body d-flex align-items-center">
            <div class="stat-icon-bg bg-success me-3">
              <i class="bi bi-check-circle text-white fs-4"></i>
            </div>
            <div>
              <h6 class="text-muted mb-1">总借用量</h6>
              <h3 class="mb-0 fw-bold counter">{{ stats.totalCount }}</h3>
            </div>
          </div>
          <div class="card-footer bg-transparent border-0 pt-0">
            <div class="progress" style="height: 4px">
              <div class="progress-bar bg-success" role="progressbar" style="width: 100%"></div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 图表 -->
    <div class="row">
      <!-- 借用趋势图 -->
      <div class="col-lg-8 mb-4">
        <div class="card border-0 shadow-sm h-100 chart-card">
          <div class="card-header bg-white border-bottom-0 py-3">
            <div class="d-flex justify-content-between align-items-center">
              <h5 class="mb-0"><i class="bi bi-graph-up text-primary me-2"></i>借用趋势</h5>
              <div class="btn-group period-selector">
                <button 
                  v-for="period in trendPeriods" 
                  :key="period.value"
                  type="button" 
                  class="btn btn-sm" 
                  :class="trendPeriod === period.value ? 'btn-primary' : 'btn-outline-primary'"
                  @click="changeTrendPeriod(period.value)"
                >
                  {{ period.label }}
                </button>
              </div>
            </div>
          </div>
          <div class="card-body">
            <div id="trend-chart" class="chart-container"></div>
          </div>
        </div>
      </div>
      
      <!-- 借用状态分布 -->
      <div class="col-lg-4 mb-4">
        <div class="card border-0 shadow-sm h-100 chart-card">
          <div class="card-header bg-white border-bottom-0 py-3">
            <h5 class="mb-0"><i class="bi bi-pie-chart text-primary me-2"></i>借用状态分布</h5>
          </div>
          <div class="card-body">
            <div id="status-chart" class="chart-container"></div>
          </div>
        </div>
      </div>
      
      <!-- 热门装备排行 -->
      <div class="col-lg-6 mb-4">
        <div class="card border-0 shadow-sm h-100 chart-card">
          <div class="card-header bg-white border-bottom-0 py-3">
            <h5 class="mb-0"><i class="bi bi-trophy text-primary me-2"></i>热门装备排行</h5>
          </div>
          <div class="card-body">
            <div id="equipment-chart" class="chart-container"></div>
          </div>
        </div>
      </div>
      
      <!-- 活跃用户排行 -->
      <div class="col-lg-6 mb-4">
        <div class="card border-0 shadow-sm h-100 chart-card">
          <div class="card-header bg-white border-bottom-0 py-3">
            <h5 class="mb-0"><i class="bi bi-people text-primary me-2"></i>活跃用户排行</h5>
          </div>
          <div class="card-body">
            <div id="user-chart" class="chart-container"></div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, watch } from 'vue'
import * as echarts from 'echarts/core'
import { 
  BarChart, 
  LineChart, 
  PieChart
} from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
  DatasetComponent,
  TransformComponent
} from 'echarts/components'
import { LabelLayout, UniversalTransition } from 'echarts/features'
import { CanvasRenderer } from 'echarts/renderers'
import borrowService from '@/api/borrow'

// 注册必须的组件
echarts.use([
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
  DatasetComponent,
  TransformComponent,
  BarChart,
  LineChart,
  PieChart,
  LabelLayout,
  UniversalTransition,
  CanvasRenderer
])

// 状态变量
const loading = ref(true)
const stats = reactive({
  activeBorrowCount: 0,
  pendingCount: 0,
  totalCount: 0,
  overdueCount: 0,
  borrowTrend: []
})

// 图表实例
let trendChart = null
let statusChart = null
let equipmentChart = null
let userChart = null

// 趋势图时间段
const trendPeriod = ref('week')
const trendPeriods = [
  { label: '7天', value: 'week' },
  { label: '30天', value: 'month' },
  { label: '季度', value: 'quarter' },
  { label: '一年', value: 'year' }
]

// 获取统计数据
const fetchStatistics = async () => {
  loading.value = true
  
  try {
    console.log('获取借用统计数据...')
    const response = await borrowService.getStatistics()
    console.log('统计数据响应:', response)
    
    // 更新统计数据
    if (typeof response === 'object') {
      stats.activeBorrowCount = response.activeBorrowCount || 0
      stats.pendingCount = response.pendingCount || 0
      stats.totalCount = response.totalCount || 0
      stats.overdueCount = response.overdueCount || 0
      
      // 打印更新后的状态
      console.log('更新后的状态:', {
        activeBorrowCount: stats.activeBorrowCount,
        pendingCount: stats.pendingCount,
        totalCount: stats.totalCount,
        overdueCount: stats.overdueCount
      })
    } else {
      console.warn('获取到的统计数据格式不正确:', response)
      
      // 尝试使用备用方法获取统计数据
      await fetchFallbackStats()
    }
    
    // 请求成功后初始化图表
    initCharts()
    
    // 添加计数器动画
    animateCounters()
    
    // 确保更新状态分布图
    if (statusChart) {
      updateStatusChart()
    }
  } catch (error) {
    console.error('获取借用统计数据失败:', error)
    
    // 提取详细的错误信息
    let errorMsg = '请稍后重试';
    if (error.response?.data?.message) {
      errorMsg = error.response.data.message;
    } else if (error.message) {
      errorMsg = error.message;
    }
    
    console.error('错误详情:', errorMsg);
    alert(`获取统计数据失败: ${errorMsg}`);
    
    // 尝试使用备用方法获取统计数据
    await fetchFallbackStats()
    
    // 即使获取数据失败，也要初始化图表显示
    initCharts()
  } finally {
    loading.value = false
  }
}

// 备用方法：直接从借用记录计算统计数据
const fetchFallbackStats = async () => {
  try {
    console.log('使用备用方法获取统计数据...')
    const response = await borrowService.getFallbackStats()
    console.log('备用统计数据响应:', response)
    
    // 更新统计数据
    if (typeof response === 'object') {
      stats.activeBorrowCount = response.activeBorrowCount || 0
      stats.pendingCount = response.pendingCount || 0
      stats.totalCount = response.totalCount || 0
      stats.overdueCount = response.overdueCount || 0
      
      console.log('备用方法更新后的状态:', stats)
    } else {
      console.warn('备用方法返回的数据格式不正确:', response)
    }
  } catch (error) {
    console.error('备用统计方法失败:', error)
  }
}

// 获取热门装备排行
const fetchEquipmentRanking = async () => {
  try {
    const response = await borrowService.getEquipmentRanking()
    // 渲染热门装备排行图表
    renderEquipmentChart(response || [])
  } catch (error) {
    console.error('获取热门装备排行失败:', error)
  }
}

// 获取活跃用户排行
const fetchUserRanking = async () => {
  try {
    const response = await borrowService.getUserRanking()
    // 渲染活跃用户排行图表
    renderUserChart(response || [])
  } catch (error) {
    console.error('获取活跃用户排行失败:', error)
  }
}

// 获取借用趋势数据
const fetchTrendData = async () => {
  try {
    const response = await borrowService.getTrendData({ period: trendPeriod.value })
    // 渲染趋势图
    renderTrendChart(response || [])
  } catch (error) {
    console.error('获取借用趋势数据失败:', error)
  }
}

// 初始化图表
const initCharts = () => {
  const chartTheme = {
    color: ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#3ba272', '#fc8452', '#9a60b4', '#ea7ccc'],
    backgroundColor: 'rgba(0, 0, 0, 0)',
    textStyle: {},
    title: {
      textStyle: {
        color: '#333333'
      },
      subtextStyle: {
        color: '#aaaaaa'
      }
    },
    line: {
      itemStyle: {
        borderWidth: 1
      },
      lineStyle: {
        width: 3
      },
      symbolSize: 8,
      symbol: 'emptyCircle'
    },
    radar: {
      itemStyle: {
        borderWidth: 1
      },
      lineStyle: {
        width: 2
      },
      symbolSize: 4,
      symbol: 'emptyCircle'
    },
    pie: {
      itemStyle: {
        borderWidth: 1,
        borderType: 'solid',
        borderColor: '#ffffff'
      }
    }
  }

  // 初始化趋势图
  trendChart = echarts.init(document.getElementById('trend-chart'))
  trendChart.setOption({
    title: {
      text: '借用趋势',
      left: 'center',
      show: false
    },
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.9)',
      borderColor: '#eee',
      borderWidth: 1,
      textStyle: {
        color: '#333'
      },
      axisPointer: {
        type: 'cross',
        label: {
          backgroundColor: '#6a7985'
        }
      }
    },
    legend: {
      data: ['新增借用', '归还数量'],
      bottom: 0,
      icon: 'roundRect'
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '10%',
      top: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: []
    },
    yAxis: {
      type: 'value',
      splitLine: {
        lineStyle: {
          color: '#f0f0f0'
        }
      }
    },
    series: [
      {
        name: '新增借用',
        type: 'line',
        stack: 'Total',
        data: [],
        smooth: true,
        showSymbol: false,
        emphasis: {
          focus: 'series',
          scale: true
        },
        lineStyle: {
          width: 3,
          color: '#1890ff'
        },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [{
              offset: 0, 
              color: 'rgba(24, 144, 255, 0.4)'
            }, {
              offset: 1, 
              color: 'rgba(24, 144, 255, 0.1)'
            }]
          }
        },
        animation: true,
        animationDuration: 1000
      },
      {
        name: '归还数量',
        type: 'line',
        stack: 'Total',
        data: [],
        smooth: true,
        showSymbol: false,
        emphasis: {
          focus: 'series',
          scale: true
        },
        lineStyle: {
          width: 3,
          color: '#52c41a'
        },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [{
              offset: 0, 
              color: 'rgba(82, 196, 26, 0.4)'
            }, {
              offset: 1, 
              color: 'rgba(82, 196, 26, 0.1)'
            }]
          }
        },
        animation: true,
        animationDelay: 300,
        animationDuration: 1000
      }
    ]
  })
  
  // 初始化状态分布图
  statusChart = echarts.init(document.getElementById('status-chart'))
  statusChart.setOption({
    title: {
      text: '借用状态分布',
      left: 'center',
      show: false
    },
    tooltip: {
      trigger: 'item',
      formatter: '{a} <br/>{b}: {c} ({d}%)',
      backgroundColor: 'rgba(255, 255, 255, 0.9)',
      borderColor: '#eee',
      borderWidth: 1,
      textStyle: {
        color: '#333'
      }
    },
    legend: {
      orient: 'vertical',
      left: 10,
      itemWidth: 10,
      itemHeight: 10,
      data: ['待审批', '已借出', '已逾期', '已归还'],
      textStyle: {
        fontSize: 12
      },
      itemStyle: {
        borderWidth: 0
      }
    },
    series: [
      {
        name: '借用状态',
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: false,
          position: 'center'
        },
        emphasis: {
          label: {
            show: true,
            fontSize: '18',
            fontWeight: 'bold'
          },
          scaleSize: 10
        },
        labelLine: {
          show: false
        },
        animationType: 'scale',
        animationEasing: 'elasticOut',
        animationDelay: function (idx) {
          return Math.random() * 200;
        },
        data: []
      }
    ]
  })
  
  // 更新状态分布图数据
  updateStatusChart()
  
  // 初始化热门装备排行图
  equipmentChart = echarts.init(document.getElementById('equipment-chart'))
  
  // 初始化活跃用户排行图
  userChart = echarts.init(document.getElementById('user-chart'))
  
  // 获取图表数据
  fetchTrendData()
  fetchEquipmentRanking()
  fetchUserRanking()
  
  // 监听窗口大小变化，调整图表大小
  window.addEventListener('resize', handleResize)
}

// 渲染趋势图
const renderTrendChart = (data) => {
  if (!trendChart) return
  
  const dates = data.map(item => item.date)
  const borrowData = data.map(item => item.borrowCount)
  const returnData = data.map(item => item.returnCount)
  
  trendChart.setOption({
    xAxis: {
      data: dates
    },
    series: [
      {
        name: '新增借用',
        data: borrowData
      },
      {
        name: '归还数量',
        data: returnData
      }
    ]
  })
}

// 更新状态分布图数据
const updateStatusChart = () => {
  if (!statusChart) return
  
  console.log('更新状态分布图数据，当前统计:', stats)
  
  // 计算已归还数量（总数 - 当前借出 - 待审批 - 逾期）
  const returnedCount = Math.max(0, stats.totalCount - stats.activeBorrowCount - stats.pendingCount - stats.overdueCount)
  
  const chartData = [
    { value: stats.pendingCount, name: '待审批', itemStyle: { color: '#faad14' } },
    { value: stats.activeBorrowCount, name: '已借出', itemStyle: { color: '#1890ff' } },
    { value: stats.overdueCount, name: '已逾期', itemStyle: { color: '#f5222d' } },
    { value: returnedCount, name: '已归还', itemStyle: { color: '#52c41a' } }
  ]
  
  // 过滤掉值为0的数据，避免显示空扇区
  const filteredData = chartData.filter(item => item.value > 0)
  
  console.log('状态分布图数据:', filteredData)
  
  statusChart.setOption({
    series: [{
      data: filteredData.length > 0 ? filteredData : [{ value: 1, name: '暂无数据', itemStyle: { color: '#d9d9d9' } }]
    }]
  })
}

// 渲染热门装备排行图
const renderEquipmentChart = (data) => {
  if (!equipmentChart) return
  
  // 只取前10条数据
  const topData = data.slice(0, 10)
  const equipmentNames = topData.map(item => item.name)
  const borrowCounts = topData.map(item => item.borrowCount)
  
  equipmentChart.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      },
      backgroundColor: 'rgba(255, 255, 255, 0.9)',
      borderColor: '#eee',
      borderWidth: 1,
      textStyle: {
        color: '#333'
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'value',
      boundaryGap: [0, 0.01],
      splitLine: {
        lineStyle: {
          color: '#f0f0f0'
        }
      }
    },
    yAxis: {
      type: 'category',
      data: equipmentNames.reverse(),
      axisLabel: {
        formatter: function(value) {
          if (value && value.length > 10) {
            return value.substring(0, 10) + '...';
          }
          return value || '未知';
        }
      }
    },
    series: [
      {
        name: '借用次数',
        type: 'bar',
        data: borrowCounts.reverse(),
        itemStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 1,
            y2: 0,
            colorStops: [{
              offset: 0,
              color: '#1890ff'
            }, {
              offset: 1,
              color: '#36cfc9'
            }]
          },
          borderRadius: [0, 4, 4, 0]
        },
        emphasis: {
          itemStyle: {
            color: {
              type: 'linear',
              x: 0,
              y: 0,
              x2: 1,
              y2: 0,
              colorStops: [{
                offset: 0,
                color: '#0960bd'
              }, {
                offset: 1,
                color: '#21a6a0'
              }]
            }
          }
        },
        animationDelay: function (idx) {
          return idx * 100;
        },
        animationDuration: 1000
      }
    ],
    animationEasing: 'elasticOut'
  })
}

// 渲染活跃用户排行图
const renderUserChart = (data) => {
  if (!userChart) return
  
  // 只取前10条数据
  const topData = data.slice(0, 10)
  const userNames = topData.map(item => item.name)
  const borrowCounts = topData.map(item => item.borrowCount)
  
  userChart.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      },
      backgroundColor: 'rgba(255, 255, 255, 0.9)',
      borderColor: '#eee',
      borderWidth: 1,
      textStyle: {
        color: '#333'
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'value',
      boundaryGap: [0, 0.01],
      splitLine: {
        lineStyle: {
          color: '#f0f0f0'
        }
      }
    },
    yAxis: {
      type: 'category',
      data: userNames.reverse(),
      axisLabel: {
        formatter: function(value) {
          if (value && value.length > 10) {
            return value.substring(0, 10) + '...';
          }
          return value || '未知';
        }
      }
    },
    series: [
      {
        name: '借用次数',
        type: 'bar',
        data: borrowCounts.reverse(),
        itemStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 1,
            y2: 0,
            colorStops: [{
              offset: 0,
              color: '#722ed1'
            }, {
              offset: 1,
              color: '#f759ab'
            }]
          },
          borderRadius: [0, 4, 4, 0]
        },
        emphasis: {
          itemStyle: {
            color: {
              type: 'linear',
              x: 0,
              y: 0,
              x2: 1,
              y2: 0,
              colorStops: [{
                offset: 0,
                color: '#5b25a8'
              }, {
                offset: 1,
                color: '#d43f8a'
              }]
            }
          }
        },
        animationDelay: function (idx) {
          return idx * 100 + 300;
        },
        animationDuration: 1000
      }
    ],
    animationEasing: 'elasticOut'
  })
}

// 添加计数器动画效果
const animateCounters = () => {
  const animateValue = (target, start, end, duration) => {
    if (start === end) return;
    const range = end - start;
    let current = start;
    const increment = end > start ? 1 : -1;
    const stepTime = Math.abs(Math.floor(duration / range));
    const counter = setInterval(() => {
      current += increment;
      target.textContent = current;
      if (current === end) {
        clearInterval(counter);
      }
    }, stepTime);
  };

  document.querySelectorAll('.counter').forEach(counter => {
    const target = parseInt(counter.textContent);
    if (!isNaN(target)) {
      animateValue(counter, 0, target, 1000);
    }
  });
}

// 处理窗口大小变化
const handleResize = () => {
  trendChart && trendChart.resize()
  statusChart && statusChart.resize()
  equipmentChart && equipmentChart.resize()
  userChart && userChart.resize()
}

// 改变趋势图时间范围
const changeTrendPeriod = (period) => {
  trendPeriod.value = period
  fetchTrendData()
}

// 生命周期钩子
onMounted(() => {
  fetchStatistics()
})

// 监视统计数据变化，确保图表更新
watch(() => [stats.activeBorrowCount, stats.pendingCount, stats.totalCount, stats.overdueCount], () => {
  console.log('统计数据变更，更新图表')
  if (statusChart) {
    updateStatusChart()
  }
}, { deep: true })

onBeforeUnmount(() => {
  // 移除窗口大小变化监听
  window.removeEventListener('resize', handleResize)
  
  // 销毁图表实例
  trendChart && trendChart.dispose()
  statusChart && statusChart.dispose()
  equipmentChart && equipmentChart.dispose()
  userChart && userChart.dispose()
})
</script>

<style scoped>
.card {
  border-radius: 0.75rem;
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  overflow: hidden;
  border: none !important;
}

.stat-card {
  position: relative;
}

.stat-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 15px 30px rgba(0, 0, 0, 0.07) !important;
}

.chart-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 15px 30px rgba(0, 0, 0, 0.07) !important;
}

.card-header {
  border-bottom: none;
  background: transparent;
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

.stat-icon-bg {
  width: 70px;
  height: 70px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  margin-right: 1rem;
  box-shadow: 0 6px 12px rgba(0, 0, 0, 0.1);
}

.bg-primary {
  background: linear-gradient(135deg, #1890ff 0%, #0050b3 100%) !important;
}

.bg-warning {
  background: linear-gradient(135deg, #faad14 0%, #d48806 100%) !important;
}

.bg-danger {
  background: linear-gradient(135deg, #f5222d 0%, #cf1322 100%) !important;
}

.bg-success {
  background: linear-gradient(135deg, #52c41a 0%, #389e0d 100%) !important;
}

.chart-container {
  width: 100%;
  height: 320px;
}

.text-gradient {
  background: linear-gradient(90deg, #1890ff, #722ed1);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  display: inline-block;
}

.period-selector .btn {
  border-radius: 20px;
  font-size: 0.8rem;
  padding: 0.25rem 0.75rem;
  transition: all 0.2s;
}

.period-selector .btn-outline-primary {
  background-color: transparent;
  border-color: #e6f7ff;
  color: #1890ff;
}

.period-selector .btn-outline-primary:hover {
  background-color: #e6f7ff;
  border-color: #1890ff;
  color: #1890ff;
}

.period-selector .btn-primary {
  background: linear-gradient(to right, #1890ff, #096dd9);
  border: none;
  box-shadow: 0 2px 6px rgba(24, 144, 255, 0.3);
}

.spinner-lg {
  width: 3rem;
  height: 3rem;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.row > div {
  animation: fadeInUp 0.5s ease-out forwards;
}

.row > div:nth-child(1) { animation-delay: 0s; }
.row > div:nth-child(2) { animation-delay: 0.1s; }
.row > div:nth-child(3) { animation-delay: 0.2s; }
.row > div:nth-child(4) { animation-delay: 0.3s; }
</style> 