<template>
  <el-container class="layout-container">
    <!-- 左侧菜单 -->
    <el-aside width="220px" class="sidebar">
      <div class="logo">
        <span class="logo-emoji">🗡️</span>
        <h1>代码江湖</h1>
      </div>
      <el-menu
        :default-active="activeMenu"
        class="sidebar-menu"
        background-color="transparent"
        text-color="#5f7a6a"
        active-text-color="#2e7d32"
        router
      >
        <el-menu-item index="/dashboard">
          <el-icon><DataBoard /></el-icon>
          <span>仪表盘</span>
        </el-menu-item>
        
        <el-sub-menu index="/blog">
          <template #title>
            <el-icon><Document /></el-icon>
            <span>博客管理</span>
          </template>
          <el-menu-item index="/blog/list">博客列表</el-menu-item>
          <el-menu-item index="/blog/edit">发布博客</el-menu-item>
        </el-sub-menu>
        
        <el-menu-item index="/category">
          <el-icon><Folder /></el-icon>
          <span>分类管理</span>
        </el-menu-item>
        
        <el-menu-item index="/tag">
          <el-icon><PriceTag /></el-icon>
          <span>标签管理</span>
        </el-menu-item>
        
        <el-menu-item index="/comment">
          <el-icon><ChatDotRound /></el-icon>
          <span>评论管理</span>
        </el-menu-item>
        
        <el-menu-item index="/link">
          <el-icon><Link /></el-icon>
          <span>链接管理</span>
        </el-menu-item>
        
        <el-menu-item index="/config">
          <el-icon><Setting /></el-icon>
          <span>系统配置</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    
    <!-- 右侧内容 -->
    <el-container>
      <!-- 顶部导航 -->
      <el-header class="header">
        <div class="header-left">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="currentRoute">{{ currentRoute }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-icon><User /></el-icon>
              <span>{{ username }}</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon>
                  个人中心
                </el-dropdown-item>
                <el-dropdown-item command="logout" divided>
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      
      <!-- 内容区域 -->
      <el-main class="main-content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { logout } from '../api/admin'

const route = useRoute()
const router = useRouter()

const username = ref(localStorage.getItem('username') || 'Admin')

const activeMenu = computed(() => route.path)
const currentRoute = computed(() => route.meta?.title || '')

const handleCommand = async (command) => {
  if (command === 'logout') {
    try {
      await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
      await logout()
      localStorage.removeItem('token')
      localStorage.removeItem('username')
      ElMessage.success('退出成功')
      router.push('/login')
    } catch (error) {
      if (error !== 'cancel') console.error(error)
    }
  } else if (command === 'profile') {
    router.push('/profile')
  }
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
}

.sidebar {
  background: linear-gradient(180deg, #e8f5e9 0%, #f1f8e9 100%);
  border-right: 1px solid #d7e8d4;
  overflow: hidden;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border-bottom: 1px solid #d7e8d4;
}

.logo-emoji {
  font-size: 22px;
}

.logo h1 {
  color: #2e7d32;
  font-size: 17px;
  font-weight: 700;
  margin: 0;
  letter-spacing: 1px;
}

.sidebar-menu {
  border-right: none;
  height: calc(100vh - 60px);
  overflow-y: auto;
  padding: 8px 0;
}

.sidebar-menu::-webkit-scrollbar {
  width: 4px;
}
.sidebar-menu::-webkit-scrollbar-thumb {
  background-color: #a5d6a7;
  border-radius: 2px;
}

.sidebar-menu :deep(.el-menu-item),
.sidebar-menu :deep(.el-sub-menu__title) {
  margin: 2px 8px;
  border-radius: 8px;
  height: 44px;
  line-height: 44px;
}

.sidebar-menu :deep(.el-menu-item:hover),
.sidebar-menu :deep(.el-sub-menu__title:hover) {
  background-color: rgba(102, 187, 106, 0.1) !important;
}

.sidebar-menu :deep(.el-menu-item.is-active) {
  background-color: rgba(102, 187, 106, 0.15) !important;
  color: #2e7d32 !important;
  font-weight: 600;
}

.header {
  background-color: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
  border-bottom: 1px solid #f0f0f0;
}

.header-left { display: flex; align-items: center; }
.header-right { display: flex; align-items: center; }

.user-info {
  display: flex;
  align-items: center;
  cursor: pointer;
  padding: 0 12px;
  height: 60px;
  line-height: 60px;
  color: #546e5a;
  font-size: 14px;
  border-radius: 6px;
  transition: background 0.2s;
}
.user-info:hover { background-color: #f1f8e9; }
.user-info .el-icon { margin-right: 5px; }

.main-content {
  background-color: #f6faf6;
  padding: 20px;
  min-height: calc(100vh - 60px);
  overflow-y: auto;
}

.fade-enter-active, .fade-leave-active { transition: opacity 0.2s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
