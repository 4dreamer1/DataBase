<template>
  <div class="app-wrapper">
    <!-- 侧边栏 -->
    <div class="sidebar-container" :class="{ 'is-collapsed': isCollapsed }">
      <div class="logo-container">
        <router-link to="/">
          <div class="logo-content">
            <span class="logo-icon">
              <el-icon v-if="isCollapsed" size="24"><Box /></el-icon>
              <img v-else src="../assets/logo.png" alt="Logo" class="logo-img" />
            </span>
            <h1 class="logo-title" v-if="!isCollapsed">人员装备管理系统</h1>
          </div>
        </router-link>
      </div>
      
      <!-- 侧边菜单 -->
      <el-scrollbar>
        <el-menu
          :default-active="activeMenu"
          :collapse="isCollapsed"
          :unique-opened="true"
          :collapse-transition="false"
          class="el-menu-vertical"
          background-color="#ffffff"
          text-color="#3d5a80"
          active-text-color="#4361ee"
        >
          <sidebar-item
            v-for="route in filteredRoutes"
            :key="route.path"
            :item="route"
            :base-path="route.path"
          />
        </el-menu>
      </el-scrollbar>

      <!-- 底部折叠按钮 -->
      <div class="sidebar-footer" @click="toggleSidebar">
        <el-tooltip
          :content="isCollapsed ? '展开菜单' : '收起菜单'"
          placement="right"
          :show-after="300"
        >
          <div class="collapse-btn">
            <el-icon :size="16">
              <Fold v-if="!isCollapsed" />
              <Expand v-else />
            </el-icon>
          </div>
        </el-tooltip>
      </div>
    </div>

    <!-- 主区域 -->
    <div class="main-container">
      <!-- 顶部导航 -->
      <div class="navbar">
        <!-- 面包屑导航 -->
        <div class="breadcrumb-container">
          <breadcrumb />
        </div>
        
        <!-- 右侧菜单 -->
        <div class="right-menu">
          <!-- 深色模式按钮 -->
          <el-tooltip :content="isDarkMode ? '切换到浅色模式' : '切换到深色模式'" placement="bottom" :show-after="300">
            <div class="right-menu-item" @click="toggleDarkMode">
              <el-icon><component :is="isDarkMode ? 'Sunny' : 'Moon'" /></el-icon>
            </div>
          </el-tooltip>
          
          <!-- 全屏按钮 -->
          <el-tooltip content="全屏" placement="bottom" :show-after="300">
            <div class="right-menu-item" @click="toggleFullScreen">
              <el-icon><FullScreen /></el-icon>
            </div>
          </el-tooltip>
          
          <!-- 用户信息下拉菜单 -->
          <el-dropdown trigger="click" class="user-dropdown">
            <div class="user-info">
              <div class="avatar">
                <img v-if="userAvatar" :src="userAvatar" class="avatar-image" alt="用户头像" />
                <span v-else class="avatar-text">{{ userInitial }}</span>
              </div>
              <div class="user-details" v-if="!isMobile">
                <span class="user-name">{{ userInfo.name || userInfo.username }}</span>
                <span class="user-role">{{ userInfo.role || '用户' }}</span>
              </div>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="goToProfile">
                  <el-icon><User /></el-icon>个人资料
                </el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
      
      <!-- 主内容区域 -->
      <div class="app-main">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <keep-alive>
              <component :is="Component" />
            </keep-alive>
          </transition>
        </router-view>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/store/modules/auth'
import { useUserStore } from '@/store/modules/user'
import { useThemeStore } from '@/store/modules/theme'
import SidebarItem from './components/SidebarItem.vue'
import Breadcrumb from './components/Breadcrumb.vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import { Box, Fold, Expand, ArrowDown, User, SwitchButton, FullScreen, Moon, Sunny } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const userStore = useUserStore()
const themeStore = useThemeStore()

// 深色模式状态
const isDarkMode = computed(() => themeStore.isDarkMode)

// 侧边栏收起状态
const isCollapsed = ref(localStorage.getItem('sidebarStatus') === 'collapsed')
const isMobile = ref(window.innerWidth < 768)

// 监听窗口大小变化
const handleResize = () => {
  isMobile.value = window.innerWidth < 768
  if (isMobile.value) {
    isCollapsed.value = true
  }
}

onMounted(() => {
  window.addEventListener('resize', handleResize)
  handleResize()
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
})

// 当前用户信息
const userInfo = computed(() => authStore.user || {})
const userInitial = computed(() => {
  const name = userInfo.value.name || userInfo.value.username || ''
  return name.charAt(0).toUpperCase()
})
const userAvatar = computed(() => {
  // 优先从auth store获取，如果没有则从user store获取
  return userInfo.value?.avatar || userStore.avatar || null
})

// 获取可显示的路由
const filteredRoutes = computed(() => {
  const routes = router.options.routes
  return routes.filter(route => {
    // 过滤掉hidden和不符合角色权限的路由
    if (route.meta && route.meta.hidden) return false
    if (route.meta && route.meta.roles) {
      return route.meta.roles.some(role => authStore.hasRole(role))
    }
    return true
  })
})

// 获取当前激活的菜单
const activeMenu = computed(() => {
  const { meta, path } = route
  if (meta.activeMenu) {
    return meta.activeMenu
  }
  return path
})

// 切换侧边栏收起状态
const toggleSidebar = () => {
  isCollapsed.value = !isCollapsed.value
  localStorage.setItem('sidebarStatus', isCollapsed.value ? 'collapsed' : 'expanded')
}

// 切换全屏
const toggleFullScreen = () => {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen()
  } else if (document.exitFullscreen) {
    document.exitFullscreen()
  }
}

// 跳转到个人资料页面
const goToProfile = () => {
  router.push('/user/profile')
}

// 退出登录
const handleLogout = () => {
  ElMessageBox.confirm('确定要退出登录吗?', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    userStore.logout()
    ElMessage({
      type: 'success',
      message: '退出登录成功'
    })
  }).catch(() => {})
}

// 切换深色模式
const toggleDarkMode = () => {
  themeStore.toggleDarkMode()
}
</script>

<style lang="scss" scoped>
// 导入全局变量
@import '@/styles/variables.scss';

.app-wrapper {
  position: relative;
  height: 100%;
  width: 100%;
  display: flex;
  
  // 侧边栏
  .sidebar-container {
    width: $--sidebar-width;
    height: 100%;
    background-color: #ffffff;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    box-shadow: 0 0 15px rgba(0, 0, 0, 0.05);
    position: relative;
    z-index: 10;
    
    &.is-collapsed {
      width: $--sidebar-collapse-width;
    }
    
    .logo-container {
      height: $--header-height;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 0 16px;
      border-bottom: 1px solid $--border-color-lighter;
      overflow: hidden;
      
      a {
        text-decoration: none;
        display: block;
        width: 100%;
      }
      
      .logo-content {
        display: flex;
        align-items: center;
        
        .logo-icon {
          display: flex;
          align-items: center;
          justify-content: center;
          min-width: 32px;
        }
        
        .logo-img {
          height: 32px;
          width: auto;
        }
        
        .logo-title {
          margin: 0 0 0 12px;
          color: $--color-primary;
          font-size: 18px;
          font-weight: 600;
          white-space: nowrap;
          overflow: hidden;
        }
      }
    }
    
    .el-menu-vertical {
      border-right: none;
      
      &:not(.el-menu--collapse) {
        width: $--sidebar-width;
      }
    }
    
    .sidebar-footer {
      position: absolute;
      bottom: 0;
      left: 0;
      right: 0;
      height: 48px;
      display: flex;
      align-items: center;
      justify-content: center;
      border-top: 1px solid $--border-color-lighter;
      background-color: #fff;
      cursor: pointer;
      
      .collapse-btn {
        width: 48px;
        height: 48px;
        display: flex;
        align-items: center;
        justify-content: center;
        transition: all 0.3s;
        
        &:hover {
          color: $--color-primary;
          background-color: rgba($--color-primary, 0.1);
        }
      }
    }
  }
  
  // 主区域
  .main-container {
    flex: 1;
    display: flex;
    flex-direction: column;
    overflow: hidden;
    
    // 顶部导航
    .navbar {
      height: $--header-height;
      box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
      background-color: #fff;
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 0;
      
      .breadcrumb-container {
        flex: 1;
        padding: 0 16px;
      }
      
      .right-menu {
        display: flex;
        align-items: center;
        height: 100%;
        
        .right-menu-item {
          padding: 0 12px;
          height: 100%;
          display: flex;
          align-items: center;
          justify-content: center;
          cursor: pointer;
          transition: background 0.3s;
          
          &:hover {
            background-color: rgba($--color-primary, 0.05);
            color: $--color-primary;
          }
        }
        
        .user-dropdown {
          cursor: pointer;
          height: 100%;
          
          .user-info {
            height: 100%;
            padding: 0 16px;
            display: flex;
            align-items: center;
            transition: background 0.3s;
            
            &:hover {
              background-color: rgba($--color-primary, 0.05);
            }
            
            .avatar {
              width: 36px;
              height: 36px;
              border-radius: 50%;
              background: linear-gradient(135deg, $--color-primary 0%, $--color-primary-dark 100%);
              color: white;
              display: flex;
              align-items: center;
              justify-content: center;
              font-weight: 600;
              font-size: 16px;
              margin-right: 10px;
              overflow: hidden;
              
              .avatar-image {
                width: 100%;
                height: 100%;
                object-fit: cover;
              }
            }
            
            .user-details {
              display: flex;
              flex-direction: column;
              justify-content: center;
              margin-right: 8px;
              
              .user-name {
                font-size: 14px;
                font-weight: 500;
                color: $--color-text-primary;
                line-height: 1.2;
              }
              
              .user-role {
                font-size: 12px;
                color: $--color-text-secondary;
              }
            }
          }
        }
      }
    }
    
    // 主内容区域
    .app-main {
      flex: 1;
      padding: 16px;
      overflow-y: auto;
      background-color: $--background-color-base;
    }
  }
}

@media (max-width: 768px) {
  .sidebar-container {
    position: fixed;
    top: 0;
    left: 0;
    z-index: 1001;
    transform: translateX(-100%);
    
    &.is-collapsed {
      transform: translateX(0);
      width: $--sidebar-width !important;
    }
  }
}
</style> 