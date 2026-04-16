<template>
  <div class="login-container">
    <div class="login-bg-shapes">
      <div class="shape shape-1"></div>
      <div class="shape shape-2"></div>
      <div class="shape shape-3"></div>
    </div>
    <div class="login-card">
      <div class="login-header">
        <div class="login-logo">
          <span class="logo-icon">🗡️</span>
        </div>
        <h2>代码江湖博客后台管理</h2>
        <p>Code Jianghu Blog Admin</p>
      </div>
      
      <el-form
        ref="formRef"
        :model="loginForm"
        :rules="rules"
        class="login-form"
        @submit.prevent="handleLogin"
      >
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="用户名"
            prefix-icon="User"
            size="large"
          />
        </el-form-item>
        
        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="密码"
            prefix-icon="Lock"
            size="large"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        
        <el-form-item prop="captcha">
          <div class="captcha-wrapper">
            <el-input
              v-model="loginForm.captcha"
              placeholder="验证码"
              prefix-icon="CircleCheck"
              size="large"
              class="captcha-input"
              @keyup.enter="handleLogin"
            />
            <img
              :src="captchaUrl"
              class="captcha-img"
              alt="验证码"
              @click="refreshCaptcha"
              title="点击刷新"
            />
          </div>
        </el-form-item>
        
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            class="login-button"
            :loading="loading"
            @click="handleLogin"
          >
            登 录
          </el-button>
        </el-form-item>
      </el-form>

      <div class="login-footer">
        <p>© 代码江湖 · Code Jianghu</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login, getKaptcha } from '../api/admin'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)
const captchaUrl = ref('')

const loginForm = ref({
  username: '',
  password: '',
  captcha: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  captcha: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}

const refreshCaptcha = async () => {
  try {
    const response = await getKaptcha()
    const url = URL.createObjectURL(response.data)
    captchaUrl.value = url
  } catch (error) {
    console.error('获取验证码失败:', error)
  }
}

const handleLogin = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      const params = {
        userName: loginForm.value.username,
        password: loginForm.value.password,
        verifyCode: loginForm.value.captcha
      }
      const res = await login(params)
      const data = res.data || {}
      localStorage.setItem('token', data.token || 'logged_in')
      localStorage.setItem('username', data.nickName || loginForm.value.username)
      ElMessage.success('登录成功')
      router.push('/dashboard')
    } catch (error) {
      refreshCaptcha()
      loginForm.value.captcha = ''
    } finally {
      loading.value = false
    }
  })
}

onMounted(() => refreshCaptcha())
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #e0f2f1 0%, #e8f5e9 35%, #f1f8e9 65%, #fff8e1 100%);
  position: relative;
  overflow: hidden;
}

/* 背景装饰圆 */
.login-bg-shapes {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.shape {
  position: absolute;
  border-radius: 50%;
  opacity: 0.35;
}
.shape-1 {
  width: 400px; height: 400px;
  background: radial-gradient(circle, #a5d6a7, transparent 70%);
  top: -120px; left: -100px;
}
.shape-2 {
  width: 300px; height: 300px;
  background: radial-gradient(circle, #80cbc4, transparent 70%);
  bottom: -80px; right: -60px;
}
.shape-3 {
  width: 200px; height: 200px;
  background: radial-gradient(circle, #ffcc80, transparent 70%);
  top: 40%; right: 15%;
}

.login-card {
  width: 400px;
  padding: 40px 36px 24px;
  background: rgba(255,255,255,0.82);
  backdrop-filter: blur(16px);
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.06), 0 1px 4px rgba(0,0,0,0.04);
  border: 1px solid rgba(255,255,255,0.6);
  position: relative;
  z-index: 1;
}

.login-header {
  text-align: center;
  margin-bottom: 32px;
}

.login-logo {
  margin-bottom: 14px;
}

.logo-icon {
  font-size: 42px;
  filter: drop-shadow(0 2px 4px rgba(0,0,0,0.1));
}

.login-header h2 {
  font-size: 22px;
  color: #2e7d32;
  margin: 0 0 6px;
  font-weight: 700;
  letter-spacing: 1px;
}

.login-header p {
  font-size: 13px;
  color: #90a4ae;
  margin: 0;
  letter-spacing: 0.5px;
}

.login-form {
  margin-top: 8px;
}

.login-form :deep(.el-input__wrapper) {
  border-radius: 8px;
  box-shadow: 0 0 0 1px #c8e6c9 inset;
  background: #fafffe;
}
.login-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #81c784 inset;
}
.login-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #66bb6a inset !important;
}

.captcha-wrapper {
  display: flex;
  gap: 12px;
  width: 100%;
}

.captcha-input {
  flex: 1;
}

.captcha-img {
  width: 120px;
  height: 40px;
  border-radius: 8px;
  cursor: pointer;
  border: 1px solid #c8e6c9;
  transition: border-color 0.2s;
}
.captcha-img:hover {
  border-color: #66bb6a;
}

.login-button {
  width: 100%;
  margin-top: 10px;
  background: linear-gradient(135deg, #66bb6a 0%, #43a047 100%);
  border: none;
  border-radius: 8px;
  font-size: 15px;
  letter-spacing: 6px;
  height: 42px;
}
.login-button:hover {
  background: linear-gradient(135deg, #4caf50 0%, #388e3c 100%);
}

.login-footer {
  text-align: center;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #e8f5e9;
}
.login-footer p {
  margin: 0;
  font-size: 12px;
  color: #b0bec5;
}
</style>
