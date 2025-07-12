<template>
  <div class="container-fluid">
    <div class="row min-vh-100">
      <!-- 左侧图片背景 -->
      <div class="col-lg-6 d-none d-lg-block p-0">
        <div class="bg-primary h-100 d-flex align-items-center justify-content-center">
          <div class="text-center text-white p-5">
            <h1 class="display-4 mb-4 fw-bold">人员装备管理系统</h1>
            <p class="lead mb-5">欢迎新用户(*^▽^*)</p>
            <div class="d-flex justify-content-center mt-4">
              <img src="@/assets/logo.png" alt="系统Logo" height="90" style="border-radius: 10px;" />
            </div>
          </div>
        </div>
      </div>
      
      <!-- 右侧注册表单 -->
      <div class="col-lg-6 col-md-12">
        <div class="d-flex align-items-center justify-content-center h-100 py-5">
          <div class="card border-0 shadow-sm" style="width: 26rem;">
            <div class="card-body p-5">
              <!-- 移动设备下的标题 -->
              <div class="d-block d-lg-none text-center mb-4">
                <h2 class="fw-bold text-primary">人员装备管理系统</h2>
                <p class="text-muted">创建您的账户</p>
              </div>
              
              <h5 class="card-title mb-4 d-none d-lg-block fw-bold">用户注册</h5>
              
              <!-- 注册表单 -->
              <form @submit.prevent="handleRegister">
                <!-- 用户名 -->
                <div class="mb-3">
                  <label for="username" class="form-label">用户名</label>
                  <div class="input-group">
                    <span class="input-group-text bg-light border-end-0">
                      <i class="bi bi-person"></i>
                    </span>
                    <input 
                      type="text" 
                      class="form-control border-start-0" 
                      id="username" 
                      v-model="registerForm.username"
                      :class="{'is-invalid': v$.registerForm.username.$error}"
                      placeholder="请输入用户名"
                    >
                  </div>
                  <div class="invalid-feedback" v-if="v$.registerForm.username.$error">
                    {{ v$.registerForm.username.$errors[0].$message }}
                  </div>
                  <small class="form-text text-muted">用户名长度3-20个字符，只能包含字母、数字和下划线</small>
                </div>
                
                <!-- 姓名 -->
                <div class="mb-3">
                  <label for="name" class="form-label">姓名</label>
                  <div class="input-group">
                    <span class="input-group-text bg-light border-end-0">
                      <i class="bi bi-person-badge"></i>
                    </span>
                    <input 
                      type="text" 
                      class="form-control border-start-0" 
                      id="name" 
                      v-model="registerForm.name"
                      :class="{'is-invalid': v$.registerForm.name.$error}"
                      placeholder="请输入您的真实姓名"
                    >
                  </div>
                  <div class="invalid-feedback" v-if="v$.registerForm.name.$error">
                    {{ v$.registerForm.name.$errors[0].$message }}
                  </div>
                </div>
                
                <!-- 邮箱 -->
                <div class="mb-3">
                  <label for="email" class="form-label">邮箱</label>
                  <div class="input-group">
                    <span class="input-group-text bg-light border-end-0">
                      <i class="bi bi-envelope"></i>
                    </span>
                    <input 
                      type="email" 
                      class="form-control border-start-0" 
                      id="email" 
                      v-model="registerForm.email"
                      :class="{'is-invalid': v$.registerForm.email.$error}"
                      placeholder="请输入邮箱地址"
                    >
                  </div>
                  <div class="invalid-feedback" v-if="v$.registerForm.email.$error">
                    {{ v$.registerForm.email.$errors[0].$message }}
                  </div>
                </div>
                
                <!-- 密码 -->
                <div class="mb-3">
                  <label for="password" class="form-label">密码</label>
                  <div class="input-group has-validation">
                    <span class="input-group-text bg-light border-end-0">
                      <i class="bi bi-lock"></i>
                    </span>
                    <input 
                      :type="showPassword ? 'text' : 'password'" 
                      class="form-control border-start-0 border-end-0" 
                      id="password" 
                      v-model="registerForm.password"
                      :class="{'is-invalid': v$.registerForm.password.$error, 'is-valid': registerForm.password && !v$.registerForm.password.$error}"
                      placeholder="请输入密码"
                      @input="validatePasswordMatch"
                    >
                    <span 
                      class="input-group-text bg-light border-start-0 cursor-pointer"
                      @click="showPassword = !showPassword"
                    >
                      <i :class="showPassword ? 'bi bi-eye-slash' : 'bi bi-eye'"></i>
                    </span>
                  <div class="invalid-feedback" v-if="v$.registerForm.password.$error">
                    {{ v$.registerForm.password.$errors[0].$message }}
                    </div>
                  </div>
                  <small class="form-text text-muted">密码长度至少6个字符，建议包含大小写字母、数字和特殊符号</small>
                </div>
                
                <!-- 确认密码 -->
                <div class="mb-4">
                  <label for="confirmPassword" class="form-label">确认密码</label>
                  <div class="input-group has-validation">
                    <span class="input-group-text bg-light border-end-0">
                      <i class="bi bi-lock-fill"></i>
                    </span>
                    <input 
                      :type="showConfirmPassword ? 'text' : 'password'" 
                      class="form-control border-start-0 border-end-0" 
                      id="confirmPassword" 
                      v-model="registerForm.confirmPassword"
                      :class="{'is-invalid': v$.registerForm.confirmPassword.$error, 'is-valid': registerForm.confirmPassword && !v$.registerForm.confirmPassword.$error && passwordsMatch}"
                      placeholder="请再次输入密码"
                      @input="validatePasswordMatch"
                    >
                    <span 
                      class="input-group-text bg-light border-start-0 cursor-pointer"
                      @click="showConfirmPassword = !showConfirmPassword"
                    >
                      <i :class="showConfirmPassword ? 'bi bi-eye-slash' : 'bi bi-eye'"></i>
                    </span>
                    <div class="invalid-feedback" v-if="v$.registerForm.confirmPassword.$error">
                      {{ v$.registerForm.confirmPassword.$errors[0].$message }}
                    </div>
                  </div>
                  <div v-if="!v$.registerForm.confirmPassword.$error && registerForm.confirmPassword && !passwordsMatch" class="text-danger small mt-1">
                    <i class="bi bi-exclamation-triangle-fill me-1"></i>两次输入的密码不一致
                  </div>
                </div>
                
                <!-- 注册按钮 -->
                <button 
                  type="submit" 
                  class="btn btn-primary w-100 py-2 mb-3 d-flex align-items-center justify-content-center"
                  :disabled="loading || !formIsValid"
                >
                  <span v-if="loading" class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
                  {{ loading ? '注册中...' : '注册' }}
                </button>
                
                <div class="text-center mb-3" v-if="!formIsValid && formTouched">
                  <small class="text-danger">
                    <i class="bi bi-exclamation-circle me-1"></i>请完成所有必填字段并确保两次密码输入一致
                  </small>
                </div>
                
                <!-- 已有账号？登录 -->
                <div class="text-center">
                  <span class="text-muted">已有账号？</span>
                  <router-link to="/login" class="text-decoration-none">立即登录</router-link>
                </div>
              </form>
              
              <!-- 错误提示 -->
              <div 
                class="alert alert-danger alert-dismissible fade show mt-3" 
                role="alert" 
                v-if="registerError"
              >
                <i class="bi bi-exclamation-triangle-fill me-2"></i>
                {{ registerError }}
                <button 
                  type="button" 
                  class="btn-close" 
                  @click="registerError = ''"
                  aria-label="Close"
                ></button>
              </div>
              
              <!-- 成功提示 -->
              <div 
                class="alert alert-success alert-dismissible fade show mt-3" 
                role="alert" 
                v-if="registerSuccess"
              >
                <i class="bi bi-check-circle-fill me-2"></i>
                {{ registerSuccess }}
                <button 
                  type="button" 
                  class="btn-close" 
                  @click="registerSuccess = ''"
                  aria-label="Close"
                ></button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useVuelidate } from '@vuelidate/core'
import { required, minLength, email, helpers } from '@vuelidate/validators'
import userService from '@/api/user'
import { useUserStore } from '@/store/modules/user'

// 路由
const router = useRouter()

// 用户状态管理
const userStore = useUserStore()

// 表单数据
const registerForm = reactive({
  username: '',
  name: '',
  email: '',
  password: '',
  confirmPassword: ''
})

// 状态控制
const loading = ref(false)
const registerError = ref('')
const registerSuccess = ref('')
const showPassword = ref(false)
const showConfirmPassword = ref(false)
const passwordsMatch = ref(true)

// 自定义验证规则
const usernamePattern = helpers.regex(/^[a-zA-Z0-9_]{3,20}$/)

// 密码一致性检查
const sameAsPassword = (value) => value === registerForm.password

// 表单验证规则
const rules = {
  registerForm: {
    username: { 
      required: helpers.withMessage('请输入用户名', required),
      minLength: helpers.withMessage('用户名长度至少3个字符', minLength(3)),
      usernamePattern: helpers.withMessage('用户名只能包含字母、数字和下划线', usernamePattern)
    },
    name: { 
      required: helpers.withMessage('请输入姓名', required) 
    },
    email: { 
      required: helpers.withMessage('请输入邮箱', required),
      email: helpers.withMessage('请输入有效的邮箱地址', email)
    },
    password: {
      required: helpers.withMessage('请输入密码', required),
      minLength: helpers.withMessage('密码长度至少6个字符', minLength(6))
    },
    confirmPassword: {
      required: helpers.withMessage('请确认密码', required),
      sameAsPassword: helpers.withMessage('两次输入的密码不一致', sameAsPassword)
    }
  }
}

// 创建验证实例
const v$ = useVuelidate(rules, { registerForm })

// 表单状态
const formTouched = ref(false)
const formIsValid = computed(() => {
  if (!registerForm.username || !registerForm.name || !registerForm.email || 
      !registerForm.password || !registerForm.confirmPassword) {
    return false
  }
  return registerForm.password === registerForm.confirmPassword && 
         !v$.value.$error
})

// 注册处理
const handleRegister = async () => {
  formTouched.value = true
  
  // 验证密码匹配
  if (registerForm.password !== registerForm.confirmPassword) {
    passwordsMatch.value = false
    registerError.value = '两次输入的密码不一致，请重新输入'
    return
  }
  
  // 验证表单
  const isValid = await v$.value.$validate()
  if (!isValid) return
  
  // 设置加载状态
  loading.value = true
  registerError.value = ''
  registerSuccess.value = ''
  
  try {
    // 调用注册API
    await userStore.register({
      username: registerForm.username,
      nickname: registerForm.name,
      name: registerForm.name,
      email: registerForm.email,
      password: registerForm.password,
      confirmPassword: registerForm.confirmPassword
    })
    
    // 注册成功
    registerSuccess.value = '注册成功！正在跳转到登录页面...'
    
    // 延迟跳转到登录页
    setTimeout(() => {
      router.push('/login')
    }, 2000)
  } catch (error) {
    // 处理注册失败
    console.error('注册失败', error)
    
    if (error.response && error.response.data) {
      registerError.value = error.response.data.message || '注册失败，请检查输入信息'
    } else {
      registerError.value = '注册失败，请稍后再试'
    }
  } finally {
    loading.value = false
  }
}

// 实时验证密码匹配
const validatePasswordMatch = () => {
  formTouched.value = true
  passwordsMatch.value = registerForm.password === registerForm.confirmPassword
  if (registerForm.confirmPassword) {
    v$.value.registerForm.confirmPassword.$touch()
  }
}
</script>

<style scoped>
.bg-primary {
  background: linear-gradient(135deg, #2979ff 0%, #1e88e5 100%) !important;
}

.cursor-pointer {
  cursor: pointer;
}

/* 响应式调整 */
@media (max-width: 992px) {
  .card {
    width: 85% !important;
    max-width: 450px;
  }
}
</style> 