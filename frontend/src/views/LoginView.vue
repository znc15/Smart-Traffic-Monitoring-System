<template>
  <section class="min-h-screen flex items-center justify-center bg-background p-4 sm:p-8">
    <div class="w-full max-w-md space-y-8 animate-in fade-in slide-in-from-bottom-8 duration-700">
      <div class="text-center space-y-2">
        <div class="mx-auto flex h-16 w-16 items-center justify-center rounded-2xl bg-primary/10 text-primary mb-6">
          <Activity class="h-8 w-8" />
        </div>
        <h1 class="text-2xl font-semibold tracking-tight text-foreground">智慧交通监控系统</h1>
        <p class="text-sm text-muted-foreground">Smart Traffic Monitoring</p>
      </div>

      <Card class="border-muted/50 shadow-xl shadow-primary/5 bg-card/50 backdrop-blur-sm">
        <CardHeader class="space-y-1">
          <div class="flex p-1 bg-muted rounded-md mb-4">
            <button
              :class="['flex-1 rounded-sm py-1.5 text-sm font-medium transition-all', mode === 'login' ? 'bg-background text-foreground shadow-sm' : 'text-muted-foreground hover:bg-muted-foreground/10']"
              @click="switchMode('login')"
            >
              登录
            </button>
            <button
              :class="['flex-1 rounded-sm py-1.5 text-sm font-medium transition-all', mode === 'register' ? 'bg-background text-foreground shadow-sm' : 'text-muted-foreground hover:bg-muted-foreground/10']"
              @click="switchMode('register')"
            >
              注册
            </button>
          </div>
          <CardDescription class="text-center">
            {{ mode === 'login' ? '使用邮箱和密码登录系统' : '首次使用可直接注册，首个用户默认为管理员' }}
          </CardDescription>
        </CardHeader>
        <CardContent>
          <form v-if="mode === 'login'" @submit.prevent="handleLogin" class="space-y-4">
            <div class="space-y-2">
              <Label for="email">邮箱</Label>
              <div class="relative">
                <User class="absolute left-3 top-2.5 h-4 w-4 text-muted-foreground" />
                <Input id="email" type="email" placeholder="name@example.com" class="pl-9" v-model="loginForm.email" required />
              </div>
            </div>
            <div class="space-y-2">
              <Label for="password">密码</Label>
              <div class="relative">
                <Lock class="absolute left-3 top-2.5 h-4 w-4 text-muted-foreground" />
                <Input id="password" type="password" placeholder="••••••••" class="pl-9" v-model="loginForm.password" required minlength="8" />
              </div>
            </div>
            <Button type="submit" class="w-full mt-2" :disabled="submitting">
              {{ submitting ? '登录中...' : '登 录' }}
            </Button>
          </form>

          <form v-else @submit.prevent="handleRegister" class="space-y-4">
            <div class="space-y-2">
              <Label for="reg-username">用户名</Label>
              <div class="relative">
                <User class="absolute left-3 top-2.5 h-4 w-4 text-muted-foreground" />
                <Input id="reg-username" type="text" placeholder="John Doe" class="pl-9" v-model="registerForm.username" required minlength="2" />
              </div>
            </div>
            <div class="space-y-2">
              <Label for="reg-email">邮箱</Label>
              <div class="relative">
                <Mail class="absolute left-3 top-2.5 h-4 w-4 text-muted-foreground" />
                <Input id="reg-email" type="email" placeholder="name@example.com" class="pl-9" v-model="registerForm.email" required />
              </div>
            </div>
            <div class="space-y-2">
              <Label for="reg-phone">手机号</Label>
              <div class="relative">
                <Phone class="absolute left-3 top-2.5 h-4 w-4 text-muted-foreground" />
                <Input id="reg-phone" type="tel" placeholder="13800000000" class="pl-9" v-model="registerForm.phoneNumber" required minlength="6" />
              </div>
            </div>
            <div class="space-y-2">
              <Label for="reg-password">密码</Label>
              <div class="relative">
                <Lock class="absolute left-3 top-2.5 h-4 w-4 text-muted-foreground" />
                <Input id="reg-password" type="password" placeholder="至少 8 位" class="pl-9" v-model="registerForm.password" required minlength="8" />
              </div>
            </div>
            <div class="space-y-2">
              <Label for="reg-confirm">确认密码</Label>
              <div class="relative">
                <ShieldCheck class="absolute left-3 top-2.5 h-4 w-4 text-muted-foreground" />
                <Input id="reg-confirm" type="password" placeholder="再次输入密码" class="pl-9" v-model="registerForm.confirmPassword" required minlength="8" />
              </div>
            </div>
            <Button type="submit" class="w-full mt-2" :disabled="submitting">
              {{ submitting ? '注册中...' : '注 册' }}
            </Button>
          </form>

          <div class="mt-6 text-center">
            <Button variant="link" class="text-xs text-muted-foreground hover:text-primary" @click="switchMode(mode === 'login' ? 'register' : 'login')">
              {{ mode === 'login' ? '没有账号？立即注册' : '已有账号？返回登录' }}
            </Button>
          </div>
        </CardContent>
      </Card>

      <p class="text-center text-xs text-muted-foreground">
        &copy; {{ new Date().getFullYear() }} Smart Traffic Monitoring System
      </p>
    </div>
  </section>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { toast } from 'vue-sonner'
import { Activity, User, Lock, Mail, Phone, ShieldCheck } from 'lucide-vue-next'
import { Card, CardHeader, CardContent, CardDescription } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Button } from '@/components/ui/button'
import { endpoints, setToken } from '../lib/api'

const router = useRouter()
const mode = ref<'login' | 'register'>('login')
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
      toast.error(err?.detail || '登录失败')
      return
    }

    const payload = await res.json()
    if (payload?.access_token) {
      setToken(payload.access_token)
    }
    await router.replace('/dashboard')
    toast.success('登录成功')
  } catch {
    toast.error('网络异常，请稍后再试')
  } finally {
    submitting.value = false
  }
}

const handleRegister = async () => {
  if (registerForm.password !== registerForm.confirmPassword) {
    toast.error('两次输入的密码不一致')
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
      toast.error(err?.detail || err?.message || '注册失败')
      return
    }

    toast.success('注册成功，请使用邮箱和密码登录')
    loginForm.email = registerForm.email
    loginForm.password = ''
    resetRegisterForm()
    switchMode('login')
  } catch {
    toast.error('网络异常，请稍后再试')
  } finally {
    submitting.value = false
  }
}
</script>
