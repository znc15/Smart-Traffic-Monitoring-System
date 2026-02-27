<template>
  <div class="layout">
    <header class="topbar">
      <h1>Smart Traffic Vue Console</h1>
      <nav>
        <RouterLink to="/dashboard">首页</RouterLink>
        <RouterLink to="/analytics">分析</RouterLink>
        <RouterLink to="/admin">管理</RouterLink>
      </nav>
    </header>
    <main>
      <RouterView />
    </main>
  </div>
</template>

<script setup lang="ts"></script>

<style scoped>
.layout {
  min-height: 100vh;
  background: linear-gradient(135deg, #eff6ff, #f8fafc);
  color: #0f172a;
}
.topbar {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  background: #0f172a;
  color: #fff;
}
.topbar nav {
  display: flex;
  gap: 16px;
}
.topbar a {
  color: #cbd5e1;
  text-decoration: none;
}
.topbar a.router-link-active {
  color: #38bdf8;
}
main {
  padding: 20px;
}
</style>
