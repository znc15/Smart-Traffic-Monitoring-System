<template>
  <section class="login-wrap">
    <div class="card">
      <h2>登录系统</h2>
      <p class="hint">使用管理员或用户邮箱登录</p>
      <form @submit.prevent="onSubmit">
        <label>
          邮箱
          <input v-model="username" type="text" placeholder="admin@example.com" />
        </label>
        <label>
          密码
          <input v-model="password" type="password" placeholder="请输入密码" />
        </label>
        <button :disabled="submitting" type="submit">
          {{ submitting ? '登录中...' : '登录' }}
        </button>
      </form>
      <p v-if="errorText" class="error">{{ errorText }}</p>
    </div>
  </section>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { endpoints, setToken } from '../lib/api'

const router = useRouter()
const username = ref('')
const password = ref('')
const submitting = ref(false)
const errorText = ref('')

const onSubmit = async () => {
  if (!username.value || !password.value) {
    errorText.value = '请输入邮箱和密码'
    return
  }

  submitting.value = true
  errorText.value = ''

  try {
    const body = new URLSearchParams()
    body.set('username', username.value)
    body.set('password', password.value)

    const res = await fetch(endpoints.login, {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: body.toString(),
      credentials: 'include'
    })

    if (!res.ok) {
      const err = await res.json().catch(() => null)
      errorText.value = err?.detail || '登录失败'
      return
    }

    const payload = await res.json()
    if (payload?.access_token) {
      setToken(payload.access_token)
    }
    await router.replace('/dashboard')
  } catch {
    errorText.value = '网络异常，请稍后再试'
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.login-wrap {
  min-height: calc(100vh - 40px);
  display: flex;
  align-items: center;
  justify-content: center;
}

.card {
  width: min(420px, 92vw);
  border-radius: 14px;
  background: #ffffff;
  padding: 24px;
  box-shadow: 0 10px 32px rgba(2, 6, 23, 0.12);
}

.hint {
  color: #64748b;
  margin-top: -4px;
  margin-bottom: 14px;
}

form {
  display: grid;
  gap: 12px;
}

label {
  display: grid;
  gap: 6px;
  font-size: 14px;
}

input {
  height: 40px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  padding: 0 10px;
}

button {
  margin-top: 4px;
  height: 40px;
  border: 0;
  border-radius: 8px;
  background: #0ea5e9;
  color: #fff;
  cursor: pointer;
}

button:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.error {
  margin-top: 12px;
  color: #dc2626;
  font-size: 14px;
}
</style>
