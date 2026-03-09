<template>
  <section class="login-wrap">
    <div class="login-card-container">
      <n-card class="login-card" :bordered="false">
        <div class="logo-section">
          <div class="logo-icon">
            <n-icon size="40" color="#2080f0">
              <SpeedometerOutline />
            </n-icon>
          </div>
          <h1 class="system-title">智慧交通监控系统</h1>
          <p class="system-subtitle">Smart Traffic Monitoring</p>
        </div>

        <div class="mode-switch">
          <n-button
            :type="mode === 'login' ? 'primary' : 'default'"
            secondary
            block
            @click="switchMode('login')"
          >
            登录
          </n-button>
          <n-button
            :type="mode === 'register' ? 'primary' : 'default'"
            secondary
            block
            @click="switchMode('register')"
          >
            注册
          </n-button>
        </div>

        <p class="mode-description">
          {{ mode === 'login' ? '使用邮箱和密码登录系统' : '首次使用可直接注册，首个用户默认为管理员' }}
        </p>

        <n-form
          v-if="mode === 'login'"
          ref="loginFormRef"
          :model="loginForm"
          :rules="loginRules"
          label-placement="left"
          :show-label="false"
          size="large"
        >
          <n-form-item path="email">
            <n-input
              v-model:value="loginForm.email"
              placeholder="请输入邮箱"
              @keydown.enter="handleLogin"
            >
              <template #prefix>
                <n-icon :component="PersonOutline" />
              </template>
            </n-input>
          </n-form-item>

          <n-form-item path="password">
            <n-input
              v-model:value="loginForm.password"
              type="password"
              show-password-on="click"
              placeholder="请输入密码"
              @keydown.enter="handleLogin"
            >
              <template #prefix>
                <n-icon :component="LockClosedOutline" />
              </template>
            </n-input>
          </n-form-item>

          <n-button
            type="primary"
            block
            strong
            :loading="submitting"
            :disabled="submitting"
            @click="handleLogin"
          >
            {{ submitting ? '登录中...' : '登 录' }}
          </n-button>
        </n-form>

        <n-form
          v-else
          ref="registerFormRef"
          :model="registerForm"
          :rules="registerRules"
          label-placement="left"
          :show-label="false"
          size="large"
        >
          <n-form-item path="username">
            <n-input
              v-model:value="registerForm.username"
              placeholder="请输入用户名"
              @keydown.enter="handleRegister"
            >
              <template #prefix>
                <n-icon :component="PersonOutline" />
              </template>
            </n-input>
          </n-form-item>

          <n-form-item path="email">
            <n-input
              v-model:value="registerForm.email"
              placeholder="请输入邮箱"
              @keydown.enter="handleRegister"
            >
              <template #prefix>
                <n-icon :component="MailOutline" />
              </template>
            </n-input>
          </n-form-item>

          <n-form-item path="phoneNumber">
            <n-input
              v-model:value="registerForm.phoneNumber"
              placeholder="请输入手机号"
              @keydown.enter="handleRegister"
            >
              <template #prefix>
                <n-icon :component="CallOutline" />
              </template>
            </n-input>
          </n-form-item>

          <n-form-item path="password">
            <n-input
              v-model:value="registerForm.password"
              type="password"
              show-password-on="click"
              placeholder="请输入密码（至少 8 位）"
              @keydown.enter="handleRegister"
            >
              <template #prefix>
                <n-icon :component="LockClosedOutline" />
              </template>
            </n-input>
          </n-form-item>

          <n-form-item path="confirmPassword">
            <n-input
              v-model:value="registerForm.confirmPassword"
              type="password"
              show-password-on="click"
              placeholder="请再次输入密码"
              @keydown.enter="handleRegister"
            >
              <template #prefix>
                <n-icon :component="ShieldCheckmarkOutline" />
              </template>
            </n-input>
          </n-form-item>

          <n-button
            type="primary"
            block
            strong
            :loading="submitting"
            :disabled="submitting"
            @click="handleRegister"
          >
            {{ submitting ? '注册中...' : '注 册' }}
          </n-button>
        </n-form>

        <div class="switch-hint">
          <n-button text type="primary" @click="switchMode(mode === 'login' ? 'register' : 'login')">
            {{ mode === 'login' ? '没有账号？立即注册' : '已有账号？返回登录' }}
          </n-button>
        </div>
      </n-card>

      <p class="copyright">
        &copy; {{ new Date().getFullYear() }} Smart Traffic Monitoring System
      </p>
    </div>
  </section>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useMessage, type FormInst, type FormRules } from 'naive-ui'
import {
  SpeedometerOutline,
  PersonOutline,
  LockClosedOutline,
  MailOutline,
  CallOutline,
  ShieldCheckmarkOutline,
} from '@vicons/ionicons5'
import { endpoints, setToken } from '../lib/api'

const router = useRouter()
const message = useMessage()
const mode = ref<'login' | 'register'>('login')
const loginFormRef = ref<FormInst | null>(null)
const registerFormRef = ref<FormInst | null>(null)
const submitting = ref(false)

const loginForm = reactive({
  email: '',
  password: '',
})

const registerForm = reactive({
  username: '',
  email: '',
  phoneNumber: '',
  password: '',
  confirmPassword: '',
})

const loginRules: FormRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: ['input', 'blur'] },
    { type: 'email', message: '请输入有效的邮箱地址', trigger: ['blur'] },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: ['input', 'blur'] },
    { min: 8, message: '密码至少 8 位', trigger: ['blur'] },
  ],
}

const registerRules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: ['input', 'blur'] },
    { min: 2, max: 50, message: '用户名长度需在 2-50 位之间', trigger: ['blur'] },
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: ['input', 'blur'] },
    { type: 'email', message: '请输入有效的邮箱地址', trigger: ['blur'] },
  ],
  phoneNumber: [
    { required: true, message: '请输入手机号', trigger: ['input', 'blur'] },
    { min: 6, max: 20, message: '手机号长度需在 6-20 位之间', trigger: ['blur'] },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: ['input', 'blur'] },
    { min: 8, message: '密码至少 8 位', trigger: ['blur'] },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: ['input', 'blur'] },
    {
      validator: () =>
        registerForm.confirmPassword === registerForm.password
          ? true
          : new Error('两次输入的密码不一致'),
      trigger: ['input', 'blur'],
    },
  ],
}

function resetRegisterForm() {
  registerForm.username = ''
  registerForm.email = ''
  registerForm.phoneNumber = ''
  registerForm.password = ''
  registerForm.confirmPassword = ''
}

function switchMode(nextMode: 'login' | 'register') {
  mode.value = nextMode
}

const handleLogin = async () => {
  try {
    await loginFormRef.value?.validate()
  } catch {
    return
  }

  submitting.value = true

  try {
    const body = new URLSearchParams()
    body.set('username', loginForm.email)
    body.set('password', loginForm.password)

    const res = await fetch(endpoints.login, {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: body.toString(),
      credentials: 'include',
    })

    if (!res.ok) {
      const err = await res.json().catch(() => null)
      message.error(err?.detail || '登录失败')
      return
    }

    const payload = await res.json()
    if (payload?.access_token) {
      setToken(payload.access_token)
    }
    await router.replace('/dashboard')
  } catch {
    message.error('网络异常，请稍后再试')
  } finally {
    submitting.value = false
  }
}

const handleRegister = async () => {
  try {
    await registerFormRef.value?.validate()
  } catch {
    return
  }

  submitting.value = true

  try {
    const res = await fetch(endpoints.register, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        username: registerForm.username,
        email: registerForm.email,
        phone_number: registerForm.phoneNumber,
        password: registerForm.password,
      }),
      credentials: 'include',
    })

    if (!res.ok) {
      const err = await res.json().catch(() => null)
      message.error(err?.detail || err?.message || '注册失败')
      return
    }

    message.success('注册成功，请使用邮箱和密码登录')
    loginForm.email = registerForm.email
    loginForm.password = ''
    resetRegisterForm()
    switchMode('login')
  } catch {
    message.error('网络异常，请稍后再试')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.login-wrap {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #e8f4fd 0%, #f0f7ff 50%, #e1ecf7 100%);
  padding: 20px;
}

.login-card-container {
  animation: fadeInUp 0.6s ease-out both;
}

.login-card {
  width: min(400px, 92vw);
  border-radius: 12px;
  box-shadow: 0 8px 40px rgba(0, 0, 0, 0.08);
}

.mode-switch {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
  margin-bottom: 12px;
}

.mode-description {
  margin: 0 0 20px;
  text-align: center;
  font-size: 13px;
  color: #64748b;
}

.logo-section {
  text-align: center;
  margin-bottom: 32px;
}

.logo-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 72px;
  height: 72px;
  border-radius: 16px;
  background: linear-gradient(135deg, #eef4ff 0%, #dbeafe 100%);
  margin-bottom: 16px;
}

.system-title {
  font-size: 22px;
  font-weight: 700;
  color: #1f2937;
  margin: 0 0 4px 0;
  letter-spacing: 1px;
}

.system-subtitle {
  font-size: 13px;
  color: #9ca3af;
  margin: 0;
  letter-spacing: 0.5px;
}

.copyright {
  text-align: center;
  font-size: 12px;
  color: #b0bec5;
  margin-top: 24px;
}

.switch-hint {
  margin-top: 16px;
  display: flex;
  justify-content: center;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(24px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
