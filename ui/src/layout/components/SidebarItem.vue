<template>
  <div v-if="!item.meta || !item.meta.hidden">
    <!-- 外层菜单 -->
    <template v-if="hasOneShowingChild(item.children, item) && (!onlyOneChild.children || onlyOneChild.noShowingChildren) && !item.alwaysShow">
      <app-link v-if="onlyOneChild.meta" :to="resolvePath(onlyOneChild.path)">
        <el-menu-item :index="resolvePath(onlyOneChild.path)" :class="{'submenu-title-noDropdown': !isNest}">
          <el-icon v-if="onlyOneChild.meta.icon">
            <component :is="onlyOneChild.meta.icon" />
          </el-icon>
          <template #title>
            <span>{{ onlyOneChild.meta.title }}</span>
            <el-badge v-if="onlyOneChild.meta.badge" value="new" class="item-badge" />
          </template>
        </el-menu-item>
      </app-link>
    </template>
    
    <!-- 有子菜单 -->
    <el-sub-menu v-else ref="subMenu" :index="resolvePath(item.path)" popper-append-to-body>
      <template #title>
        <el-icon v-if="item.meta && item.meta.icon">
          <component :is="item.meta.icon" />
        </el-icon>
        <span>{{ item.meta.title }}</span>
      </template>
      
      <!-- 递归调用 -->
      <sidebar-item
        v-for="child in item.children"
        :key="child.path"
        :is-nest="true"
        :item="child"
        :base-path="resolvePath(child.path)"
        class="nest-menu"
      />
    </el-sub-menu>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { isExternal } from '@/utils/validate'
import AppLink from './Link.vue'
import path from 'path-browserify'
import { useAuthStore } from '@/store/modules/auth'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

const props = defineProps({
  // 路由对象
  item: {
    type: Object,
    required: true
  },
  // 基础路径
  basePath: {
    type: String,
    default: ''
  },
  // 是否嵌套
  isNest: {
    type: Boolean,
    default: false
  }
})

const authStore = useAuthStore()
const onlyOneChild = ref(null)

// 判断是否只有一个显示的子项
const hasOneShowingChild = (children = [], parent) => {
  if (!children) {
    children = []
  }
  
  const showingChildren = children.filter(item => {
    if (item.meta && item.meta.hidden) {
      return false
    }
    
    // 如果有角色限制，检查权限
    if (item.meta && item.meta.roles) {
      return item.meta.roles.some(role => authStore.hasRole(role))
    }
    
    return true
  })
  
  // 当只有一个子路由时直接显示
  if (showingChildren.length === 1) {
    onlyOneChild.value = showingChildren[0]
    return true
  }
  
  // 没有子路由则显示父路由
  if (showingChildren.length === 0) {
    onlyOneChild.value = { ...parent, path: '', noShowingChildren: true }
    return true
  }
  
  return false
}

// 解析路径
const resolvePath = (routePath) => {
  if (isExternal(routePath)) {
    return routePath
  }
  if (isExternal(props.basePath)) {
    return props.basePath
  }
  return path.resolve(props.basePath, routePath)
}
</script>

<style lang="scss" scoped>
.item-badge {
  margin-left: 5px;
}

.el-menu-item.is-active {
  background-color: #1890ff1a !important; 
}

.el-menu-item:hover {
  background-color: #f5f5f5 !important;
}

.el-sub-menu__title:hover {
  background-color: #f5f5f5 !important;
}

.el-icon {
  margin-right: 12px;
  font-size: 18px;
  vertical-align: middle;
  color: #606266;
  transition: all 0.3s;
}

// 活跃状态下的图标颜色
.el-menu-item.is-active .el-icon {
  color: #4361ee;
}

// 鼠标悬停时的图标颜色
.el-menu-item:hover .el-icon,
.el-sub-menu__title:hover .el-icon {
  color: #4361ee;
}
</style> 