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

        <n-form
          ref="formRef"
          :model="formModel"
          :rules="rules"
          label-placement="left"
          :show-label="false"
          size="large"
        >
          <n-form-item path="username">
            <n-input
              v-model:value="formModel.username"
              placeholder="请输入邮箱"
              @keydown.enter="handleSubmit"
            >
              <template #prefix>
                <n-icon :component="PersonOutline" />
              </template>
            </n-input>
          </n-form-item>

          <n-form-item path="password">
            <n-input
              v-model:value="formModel.password"
              type="password"
              show-password-on="click"
              placeholder="请输入密码"
              @keydown.enter="handleSubmit"
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
            @click="handleSubmit"
          >
            {{ submitting ? '登录中...' : '登 录' }}
          </n-button>
        </n-form>
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
import { SpeedometerOutline, PersonOutline, LockClosedOutline } from '@vicons/ionicons5'
import { endpoints, setToken } from '../lib/api'

const router = useRouter()
const message = useMessage()
const formRef = ref<FormInst | null>(null)
const submitting = ref(false)

const formModel = reactive({
  username: '',
  password: '',
})

const rules: FormRules = {
  username: [
    { required: true, message: '请输入邮箱', trigger: ['input', 'blur'] },
    { type: 'email', message: '请输入有效的邮箱地址', trigger: ['blur'] },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: ['input', 'blur'] },
    { min: 6, message: '密码至少 6 位', trigger: ['blur'] },
  ],
}

const handleSubmit = async () => {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }

  submitting.value = true

  try {
    const body = new URLSearchParams()
    body.set('username', formModel.username)
    body.set('password', formModel.password)

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
