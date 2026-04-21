# API Key Endpoints Selection UI Improvement

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Refactor the "Allowed Endpoints" input field in the API Key creation/editing form to use a checkbox list dynamically generated from the `/api/v1/api-docs` endpoint.

**Architecture:** We will modify `DeveloperCenterView.vue` to compute a unique list of available endpoints from the `apiDocs` response. The form state for `allowedEndpoints` will be updated from a comma-separated string to an array of strings (`string[]`). A checkbox group UI will be implemented in the dialog, along with a "Select All / Deselect All" convenience button.

**Tech Stack:** Vue 3, Tailwind CSS, shadcn-vue.

---

### Task 1: Add Checkbox Component

**Files:**
- Create: `frontend/src/components/ui/checkbox/Checkbox.vue`
- Create: `frontend/src/components/ui/checkbox/index.ts`

- [ ] **Step 1: Install or create the Checkbox component**

Run the following command to add the shadcn Checkbox component if it's not present, or create the files manually based on shadcn-vue standards. Since we have a script to run shadcn CLI, let's use it or create it manually.

```bash
cd frontend && npx shadcn-vue@latest add checkbox
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/components/ui/checkbox
git commit -m "feat: add shadcn checkbox component"
```

---

### Task 2: Refactor State and Compute Available Endpoints

**Files:**
- Modify: `frontend/src/views/DeveloperCenterView.vue`

- [ ] **Step 1: Compute unique endpoints**

Update the script setup to extract unique paths from `apiDocs.value.endpoints` and change `clientForm` to use an array.

```vue
// Modify in frontend/src/views/DeveloperCenterView.vue

import { ref, onMounted, computed } from 'vue'
import { Checkbox } from '@/components/ui/checkbox' // Add import

// ... existing refs ...

const clientForm = ref({
  name: '',
  description: '',
  allowedEndpoints: [] as string[], // Changed from string to string[]
  rateLimit: 1000
})

// Compute unique endpoints from apiDocs
const availableEndpoints = computed(() => {
  if (!apiDocs.value || !apiDocs.value.endpoints) return []
  const paths = new Set<string>()
  apiDocs.value.endpoints.forEach((ep: any) => {
    if (ep.path) paths.add(ep.path)
  })
  return Array.from(paths).sort()
})

const isAllEndpointsSelected = computed(() => {
  return availableEndpoints.value.length > 0 && 
         clientForm.value.allowedEndpoints.length === availableEndpoints.value.length
})

function toggleAllEndpoints() {
  if (isAllEndpointsSelected.value) {
    clientForm.value.allowedEndpoints = []
  } else {
    clientForm.value.allowedEndpoints = [...availableEndpoints.value]
  }
}

// ... update openAddClientDialog and editClient ...
function openAddClientDialog() {
  editingClientId.value = null
  clientForm.value = { name: '', description: '', allowedEndpoints: [], rateLimit: 1000 }
  showClientDialog.value = true
}

function editClient(client: any) {
  editingClientId.value = client.id
  clientForm.value = {
    name: client.name,
    description: client.description || '',
    allowedEndpoints: client.allowed_endpoints ? [...client.allowed_endpoints] : [],
    rateLimit: client.rate_limit || 1000
  }
  showClientDialog.value = true
}

// ... update saveClient ...
async function saveClient() {
  try {
    const payload = {
      name: clientForm.value.name,
      description: clientForm.value.description,
      allowedEndpoints: clientForm.value.allowedEndpoints, // No split needed anymore
      rateLimit: Number(clientForm.value.rateLimit)
    }
    // ... rest of saveClient remains the same
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/views/DeveloperCenterView.vue
git commit -m "feat: refactor API key form state for allowed endpoints array"
```

---

### Task 3: Refactor the UI Form to Use Checkboxes

**Files:**
- Modify: `frontend/src/views/DeveloperCenterView.vue`

- [ ] **Step 1: Replace the Input with a Checkbox list**

In the `<template>` section, find the `clientEndpoints` Input and replace it with a ScrollArea or max-height div containing the checkboxes.

```vue
<!-- Replace this section in frontend/src/views/DeveloperCenterView.vue -->
          <div class="space-y-2">
            <div class="flex items-center justify-between">
              <Label>允许的端点</Label>
              <Button type="button" variant="ghost" size="sm" @click="toggleAllEndpoints">
                {{ isAllEndpointsSelected ? '取消全选' : '全选' }}
              </Button>
            </div>
            <div class="border rounded-md p-4 max-h-60 overflow-y-auto space-y-3 bg-muted/20">
              <div v-if="availableEndpoints.length === 0" class="text-sm text-muted-foreground text-center py-2">
                暂无可用的端点
              </div>
              <div v-for="ep in availableEndpoints" :key="ep" class="flex items-center space-x-2">
                <Checkbox 
                  :id="`ep-${ep}`" 
                  :value="ep"
                  :checked="clientForm.allowedEndpoints.includes(ep)"
                  @update:checked="(checked) => {
                    if (checked) {
                      clientForm.allowedEndpoints.push(ep)
                    } else {
                      clientForm.allowedEndpoints = clientForm.allowedEndpoints.filter(e => e !== ep)
                    }
                  }"
                />
                <Label :for="`ep-${ep}`" class="text-sm font-mono cursor-pointer font-normal">{{ ep }}</Label>
              </div>
            </div>
            <p class="text-xs text-muted-foreground">留空表示不允许访问任何端点。如需允许所有，请全选。</p>
          </div>
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/views/DeveloperCenterView.vue
git commit -m "feat: replace allowed endpoints input with checkbox list"
```

---

### Task 4: Verify Component Styles

**Files:**
- Test UI in browser.

- [ ] **Step 1: Build and preview**

```bash
cd frontend && pnpm run build
```
Verify the build succeeds without TypeScript errors. Then preview the UI to ensure the checkboxes look correct.