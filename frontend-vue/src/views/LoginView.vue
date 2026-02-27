<template>
  <section class="card">
    <h2>登录</h2>
    <form class="form" @submit.prevent="onSubmit">
      <label>
        用户名
        <input v-model="username" type="text" placeholder="admin@example.com" />
      </label>
      <label>
        密码
        <input v-model="password" type="password" placeholder="********" />
      </label>
      <button type="submit">登录</button>
    </form>
  </section>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const username = ref('')
const password = ref('')

const onSubmit = () => {
  if (!username.value || !password.value) return
  router.push('/dashboard')
}
</script>

<style scoped>
.card {
  max-width: 420px;
  margin: 30px auto;
  background: #fff;
  border-radius: 14px;
  padding: 20px;
  box-shadow: 0 12px 30px rgba(2, 6, 23, 0.1);
}
.form {
  display: grid;
  gap: 12px;
}
label {
  display: grid;
  gap: 6px;
}
input {
  height: 40px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  padding: 0 10px;
}
button {
  height: 40px;
  border: 0;
  border-radius: 8px;
  background: #0284c7;
  color: #fff;
}
</style>
