<template>
  <div class="container-fluid p-0">
    <div class="row g-0 min-vh-100">
      <!-- 左侧品牌展示区 -->
      <div class="col-lg-6 d-none d-lg-block position-relative bg-image">
        <div class="position-absolute top-0 start-0 w-100 h-100 bg-overlay"></div>
        <div class="position-relative h-100 d-flex flex-column justify-content-center align-items-center px-5 text-white">
          <div class="text-center mb-5 fade-in">
            <h1 class="display-2 fw-bold mb-3 hero-title">人员装备管理系统</h1>
            <p class="lead fs-4 mb-5 hero-subtitle">高效、安全、智能的设备资源管理平台</p>
            
            <div class="row mt-5 g-4">
              <div class="col-md-4">
                <div class="feature-card p-4">
                  <div class="feature-icon-wrapper mb-3">
                    <i class="bi bi-shield-lock"></i>
                  </div>
                  <h5 class="fw-semibold">安全可靠</h5>
                  <p class="small opacity-75 mb-0">多重防护，数据加密存储</p>
                </div>
              </div>
              <div class="col-md-4">
                <div class="feature-card p-4">
                  <div class="feature-icon-wrapper mb-3">
                    <i class="bi bi-lightning-charge"></i>
                  </div>
                  <h5 class="fw-semibold">高效管理</h5>
                  <p class="small opacity-75 mb-0">简化流程，提高工作效率</p>
                </div>
              </div>
              <div class="col-md-4">
                <div class="feature-card p-4">
                  <div class="feature-icon-wrapper mb-3">
                    <i class="bi bi-graph-up"></i>
                  </div>
                  <h5 class="fw-semibold">数据分析</h5>
                  <p class="small opacity-75 mb-0">精准统计，辅助决策支持</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 右侧登录表单 -->
      <div class="col-lg-6 bg-white">
        <div class="login-wrapper d-flex align-items-center justify-content-center h-100 p-4">
          <div class="login-form-container">
            <!-- 移动设备上的标题 -->
            <div class="d-lg-none text-center mb-5">
              <div class="brand-logo-sm mb-3">
                <img src="@/assets/logo.png" alt="Logo" class="img-fluid" style="height: 56px;">
              </div>
              <h2 class="fw-bold text-primary mb-2">人员装备管理系统</h2>
              <p class="text-muted">高效、安全、智能的设备资源管理平台</p>
            </div>
            
            <div class="card shadow-lg rounded-4 border-0 fade-in">
              <div class="card-body p-4 p-lg-5">
                <div class="text-center mb-4">
                  <div class="avatar-icon-wrapper mb-4">
                    <div class="avatar-icon">
                      <i class="bi bi-person-circle"></i>
                    </div>
                  </div>
                  <h3 class="fw-bold mb-1">欢迎回来</h3>
                  <p class="text-muted mb-4">请使用您的账号密码登录系统</p>
                </div>
                
                <!-- 登录表单 -->
                <form @submit.prevent="handleLogin">
                  <!-- 错误提示 -->
                  <div 
                    class="alert alert-danger d-flex align-items-center" 
                    role="alert" 
                    v-if="loginError"
                  >
                    <i class="bi bi-exclamation-triangle-fill flex-shrink-0 me-2"></i>
                    <div>{{ loginError }}</div>
                  </div>
                  
                  <!-- 用户名 -->
                  <div class="form-floating mb-4">
                    <input 
                      type="text" 
                      class="form-control form-control-lg rounded-3" 
                      id="username" 
                      placeholder="用户名"
                      v-model="loginForm.username"
                      :class="{'is-invalid': v$.loginForm.username.$error}"
                      autocomplete="username"
                      @focus="inputFocus = 'username'"
                      @blur="inputFocus = null"
                    >
                    <label :class="{'active': inputFocus === 'username'}" for="username">
                      <i class="bi bi-person me-2"></i>用户名
                    </label>
                    <div class="invalid-feedback" v-if="v$.loginForm.username.$error">
                      {{ v$.loginForm.username.$errors[0].$message }}
                    </div>
                  </div>
                  
                  <!-- 密码 -->
                  <div class="form-floating mb-4 position-relative">
                    <input 
                      :type="showPassword ? 'text' : 'password'" 
                      class="form-control form-control-lg rounded-3" 
                      id="password" 
                      placeholder="密码"
                      v-model="loginForm.password"
                      :class="{'is-invalid': v$.loginForm.password.$error}"
                      autocomplete="current-password"
                      @focus="inputFocus = 'password'"
                      @blur="inputFocus = null"
                    >
                    <label :class="{'active': inputFocus === 'password'}" for="password">
                      <i class="bi bi-lock me-2"></i>密码
                    </label>
                    <button 
                      type="button"
                      class="btn btn-link position-absolute end-0 top-50 translate-middle-y text-decoration-none password-toggle"
                      @click="showPassword = !showPassword"
                    >
                      <i :class="showPassword ? 'bi bi-eye-slash' : 'bi bi-eye'"></i>
                    </button>
                    <div class="invalid-feedback" v-if="v$.loginForm.password.$error">
                      {{ v$.loginForm.password.$errors[0].$message }}
                    </div>
                  </div>
                  
                  <!-- 记住登录和找回密码 -->
                  <div class="d-flex justify-content-between align-items-center mb-4">
                    <div class="form-check form-switch">
                      <input 
                        class="form-check-input" 
                        type="checkbox" 
                        role="switch"
                        id="rememberMe" 
                        v-model="loginForm.rememberMe"
                      >
                      <label class="form-check-label" for="rememberMe">
                        记住我
                      </label>
                    </div>
                    <a href="#" class="text-decoration-none link-primary">忘记密码?</a>
                  </div>
                  
                  <!-- 登录按钮 -->
                  <button 
                    type="submit" 
                    class="btn btn-primary btn-lg w-100 py-3 rounded-pill mb-4 d-flex align-items-center justify-content-center login-btn"
                    :disabled="loading"
                  >
                    <span v-if="loading" class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
                    {{ loading ? '登录中...' : '登录系统' }}
                  </button>
                  
                  <!-- 分隔线 -->
                  <div class="position-relative my-4">
                    <hr>
                    <div class="position-absolute top-50 start-50 translate-middle px-3 bg-white text-muted small">或</div>
                  </div>
                  
                  <!-- 注册链接 -->
                  <div class="text-center">
                    <span class="text-muted">还没有账号？</span>
                    <router-link to="/register" class="text-decoration-none fw-medium">立即注册</router-link>
                  </div>
                </form>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useVuelidate } from '@vuelidate/core'
import { required, minLength } from '@vuelidate/validators'
import userService from '@/api/user'
import { useUserStore } from '@/store/modules/user'

// 路由
const router = useRouter()

// 用户状态管理
const userStore = useUserStore()

// 表单数据
const loginForm = reactive({
  username: '',
  password: '',
  rememberMe: false
})

// 状态控制
const loading = ref(false)
const loginError = ref('')
const showPassword = ref(false)
const inputFocus = ref(null)

// 表单验证规则
const rules = {
  loginForm: {
    username: { 
      required: (v) => !!v || '请输入用户名', 
      minLength: minLength(3) || '用户名长度不能少于3个字符' 
    },
    password: { 
      required: (v) => !!v || '请输入密码', 
      minLength: minLength(6) || '密码长度不能少于6个字符' 
    }
  }
}

// 创建验证实例
const v$ = useVuelidate(rules, { loginForm })

// 登录处理
const handleLogin = async () => {
  // 验证表单
  const isValid = await v$.value.$validate()
  if (!isValid) return
  
  // 设置加载状态
  loading.value = true
  loginError.value = ''
  
  try {
    // 调用登录API
    await userStore.login({
      username: loginForm.username,
      password: loginForm.password,
      remember: loginForm.rememberMe
    })
    
    // 登录成功后跳转到首页
    router.push('/')
  } catch (error) {
    // 处理登录失败
    console.error('登录失败', error)
    
    if (error.response && error.response.data) {
      loginError.value = error.response.data.message || '用户名或密码错误'
    } else {
      loginError.value = '登录失败，请稍后再试'
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.bg-image {
  background-image: linear-gradient(135deg, #4361ee 0%, #3a0ca3 100%);
  position: relative;
}

.bg-overlay {
  background-color: rgba(0, 0, 0, 0.2);
  backdrop-filter: blur(5px);
}

.hero-title {
  text-shadow: 0 2px 10px rgba(0, 0, 0, 0.2);
  font-weight: 800 !important;
  letter-spacing: -0.02em;
}

.hero-subtitle {
  text-shadow: 0 1px 5px rgba(0, 0, 0, 0.2);
  font-weight: 300;
  max-width: 600px;
  margin-left: auto;
  margin-right: auto;
}

.feature-card {
  background-color: rgba(255, 255, 255, 0.1);
  border-radius: 15px;
  backdrop-filter: blur(5px);
  transition: all 0.3s ease;
  transform: translateY(0);
  
  &:hover {
    background-color: rgba(255, 255, 255, 0.15);
    transform: translateY(-5px);
  }
  
  .feature-icon-wrapper {
    width: 60px;
    height: 60px;
    margin: 0 auto;
    border-radius: 50%;
    background-color: rgba(255, 255, 255, 0.2);
    display: flex;
    align-items: center;
    justify-content: center;
    
    i {
      font-size: 26px;
      color: white;
    }
  }
}

.login-wrapper {
  overflow-y: auto;
}

.login-form-container {
  width: 100%;
  max-width: 450px;
  margin: 0 auto;
}

.avatar-icon-wrapper {
  width: 80px;
  height: 80px;
  margin: 0 auto;
  border-radius: 50%;
  background: linear-gradient(135deg, #4361ee20, #7209b720);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 10px 15px -3px rgba(67, 97, 238, 0.15);
  
  .avatar-icon {
    font-size: 40px;
    color: #4361ee;
  }
}

.form-floating {
  .form-control {
    border: 1px solid #dee2e6;
    box-shadow: none;
    padding-left: 15px;
    height: 58px;
    
    &:focus {
      border-color: #4361ee;
      box-shadow: 0 0 0 0.25rem rgba(67, 97, 238, 0.25);
    }
    
    &.is-invalid {
      border-color: #f94144;
      box-shadow: none;
    }
  }
  
  label {
    padding-left: 15px;
    color: #6c757d;
    
    &.active {
      color: #4361ee;
    }
  }
}

.password-toggle {
  height: 40px;
  width: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #6c757d;
  z-index: 5;
  
  &:hover {
    color: #4361ee;
  }
}

.login-btn {
  background: linear-gradient(135deg, #4361ee, #7209b7);
  border: none;
  box-shadow: 0 4px 10px rgba(67, 97, 238, 0.3);
  transition: all 0.3s ease;
  
  &:hover:not(:disabled) {
    transform: translateY(-2px);
    box-shadow: 0 6px 15px rgba(67, 97, 238, 0.4);
  }
  
  &:active:not(:disabled) {
    transform: translateY(0);
  }
}

.form-check-input:checked {
  background-color: #4361ee;
  border-color: #4361ee;
}

/* 动画 */
.fade-in {
  animation: fadeIn 0.6s ease-out forwards;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 992px) {
  .login-form-container {
    padding: 20px 0;
  }
}
</style> 