#!/bin/bash
mkdir -p src/components/ui/card src/components/ui/input src/components/ui/label src/components/ui/table src/components/ui/badge src/components/ui/alert src/components/ui/skeleton src/components/ui/switch src/components/ui/dialog src/components/ui/tabs src/components/ui/select src/components/ui/sonner src/components/ui/popover

cat << 'EOF' > src/components/ui/card/Card.vue
<script setup lang="ts">import { cn } from '@/lib/utils'; const props = defineProps<{ class?: string }>()</script>
<template><div :class="cn('rounded-xl border bg-card text-card-foreground shadow', props.class)"><slot /></div></template>
EOF

cat << 'EOF' > src/components/ui/card/CardHeader.vue
<script setup lang="ts">import { cn } from '@/lib/utils'; const props = defineProps<{ class?: string }>()</script>
<template><div :class="cn('flex flex-col space-y-1.5 p-6', props.class)"><slot /></div></template>
EOF

cat << 'EOF' > src/components/ui/card/CardTitle.vue
<script setup lang="ts">import { cn } from '@/lib/utils'; const props = defineProps<{ class?: string }>()</script>
<template><h3 :class="cn('font-semibold leading-none tracking-tight', props.class)"><slot /></h3></template>
EOF

cat << 'EOF' > src/components/ui/card/CardDescription.vue
<script setup lang="ts">import { cn } from '@/lib/utils'; const props = defineProps<{ class?: string }>()</script>
<template><p :class="cn('text-sm text-muted-foreground', props.class)"><slot /></p></template>
EOF

cat << 'EOF' > src/components/ui/card/CardContent.vue
<script setup lang="ts">import { cn } from '@/lib/utils'; const props = defineProps<{ class?: string }>()</script>
<template><div :class="cn('p-6 pt-0', props.class)"><slot /></div></template>
EOF

cat << 'EOF' > src/components/ui/card/CardFooter.vue
<script setup lang="ts">import { cn } from '@/lib/utils'; const props = defineProps<{ class?: string }>()</script>
<template><div :class="cn('flex items-center p-6 pt-0', props.class)"><slot /></div></template>
EOF

cat << 'EOF' > src/components/ui/input/index.ts
export { default as Input } from './Input.vue'
EOF

cat << 'EOF' > src/components/ui/input/Input.vue
<script setup lang="ts">
import { cn } from '@/lib/utils'
defineOptions({ inheritAttrs: false })
const props = defineProps<{ class?: string; modelValue?: string | number }>()
const emit = defineEmits<{ (e: 'update:modelValue', payload: string | number): void }>()
</script>
<template>
  <input
    :value="modelValue"
    @input="emit('update:modelValue', ($event.target as HTMLInputElement).value)"
    :class="cn('flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background file:border-0 file:bg-transparent file:text-sm file:font-medium placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50', props.class)"
    v-bind="$attrs"
  />
</template>
EOF

cat << 'EOF' > src/components/ui/label/index.ts
export { default as Label } from './Label.vue'
EOF

cat << 'EOF' > src/components/ui/label/Label.vue
<script setup lang="ts">
import { cn } from '@/lib/utils'
const props = defineProps<{ class?: string; for?: string }>()
</script>
<template>
  <label :for="props.for" :class="cn('text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70', props.class)"><slot /></label>
</template>
EOF

cat << 'EOF' > src/components/ui/table/index.ts
export { default as Table } from './Table.vue'
export { default as TableHeader } from './TableHeader.vue'
export { default as TableBody } from './TableBody.vue'
export { default as TableFooter } from './TableFooter.vue'
export { default as TableRow } from './TableRow.vue'
export { default as TableHead } from './TableHead.vue'
export { default as TableCell } from './TableCell.vue'
export { default as TableCaption } from './TableCaption.vue'
EOF

cat << 'EOF' > src/components/ui/table/Table.vue
<script setup lang="ts">import { cn } from '@/lib/utils'; const props = defineProps<{ class?: string }>()</script>
<template><div class="relative w-full overflow-auto"><table :class="cn('w-full caption-bottom text-sm', props.class)"><slot /></table></div></template>
EOF

cat << 'EOF' > src/components/ui/table/TableHeader.vue
<script setup lang="ts">import { cn } from '@/lib/utils'; const props = defineProps<{ class?: string }>()</script>
<template><thead :class="cn('[&_tr]:border-b', props.class)"><slot /></thead></template>
EOF

cat << 'EOF' > src/components/ui/table/TableBody.vue
<script setup lang="ts">import { cn } from '@/lib/utils'; const props = defineProps<{ class?: string }>()</script>
<template><tbody :class="cn('[&_tr:last-child]:border-0', props.class)"><slot /></tbody></template>
EOF

cat << 'EOF' > src/components/ui/table/TableFooter.vue
<script setup lang="ts">import { cn } from '@/lib/utils'; const props = defineProps<{ class?: string }>()</script>
<template><tfoot :class="cn('bg-primary font-medium text-primary-foreground', props.class)"><slot /></tfoot></template>
EOF

cat << 'EOF' > src/components/ui/table/TableRow.vue
<script setup lang="ts">import { cn } from '@/lib/utils'; const props = defineProps<{ class?: string }>()</script>
<template><tr :class="cn('border-b transition-colors hover:bg-muted/50 data-[state=selected]:bg-muted', props.class)"><slot /></tr></template>
EOF

cat << 'EOF' > src/components/ui/table/TableHead.vue
<script setup lang="ts">import { cn } from '@/lib/utils'; const props = defineProps<{ class?: string }>()</script>
<template><th :class="cn('h-12 px-4 text-left align-middle font-medium text-muted-foreground [&:has([role=checkbox])]:pr-0', props.class)"><slot /></th></template>
EOF

cat << 'EOF' > src/components/ui/table/TableCell.vue
<script setup lang="ts">import { cn } from '@/lib/utils'; const props = defineProps<{ class?: string }>()</script>
<template><td :class="cn('p-4 align-middle [&:has([role=checkbox])]:pr-0', props.class)"><slot /></td></template>
EOF

cat << 'EOF' > src/components/ui/table/TableCaption.vue
<script setup lang="ts">import { cn } from '@/lib/utils'; const props = defineProps<{ class?: string }>()</script>
<template><caption :class="cn('mt-4 text-sm text-muted-foreground', props.class)"><slot /></caption></template>
EOF

cat << 'EOF' > src/components/ui/badge/index.ts
import { cva, type VariantProps } from 'class-variance-authority'
export const badgeVariants = cva(
  'inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-semibold transition-colors focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2',
  {
    variants: {
      variant: {
        default: 'border-transparent bg-primary text-primary-foreground hover:bg-primary/80',
        secondary: 'border-transparent bg-secondary text-secondary-foreground hover:bg-secondary/80',
        destructive: 'border-transparent bg-destructive text-destructive-foreground hover:bg-destructive/80',
        warning: 'border-transparent bg-yellow-500 text-primary-foreground hover:bg-yellow-500/80',
        outline: 'text-foreground',
      },
    },
    defaultVariants: { variant: 'default' },
  }
)
export type BadgeVariants = VariantProps<typeof badgeVariants>
export { default as Badge } from './Badge.vue'
EOF

cat << 'EOF' > src/components/ui/badge/Badge.vue
<script setup lang="ts">
import { cn } from '@/lib/utils'
import { badgeVariants, type BadgeVariants } from '.'
interface Props { variant?: BadgeVariants['variant']; class?: string }
const props = defineProps<Props>()
</script>
<template><div :class="cn(badgeVariants({ variant }), props.class)"><slot /></div></template>
EOF

cat << 'EOF' > src/components/ui/alert/index.ts
import { cva, type VariantProps } from 'class-variance-authority'
export const alertVariants = cva(
  'relative w-full rounded-lg border p-4 [&>svg~*]:pl-7 [&>svg+div]:translate-y-[-3px] [&>svg]:absolute [&>svg]:left-4 [&>svg]:top-4 [&>svg]:text-foreground',
  {
    variants: {
      variant: {
        default: 'bg-background text-foreground',
        destructive: 'border-destructive/50 text-destructive dark:border-destructive [&>svg]:text-destructive',
        warning: 'border-warning/50 text-warning dark:border-warning [&>svg]:text-warning',
        success: 'border-success/50 text-success dark:border-success [&>svg]:text-success',
      },
    },
    defaultVariants: { variant: 'default' },
  }
)
export type AlertVariants = VariantProps<typeof alertVariants>
export { default as Alert } from './Alert.vue'
export { default as AlertTitle } from './AlertTitle.vue'
export { default as AlertDescription } from './AlertDescription.vue'
EOF

cat << 'EOF' > src/components/ui/alert/Alert.vue
<script setup lang="ts">
import { cn } from '@/lib/utils'
import { alertVariants, type AlertVariants } from '.'
interface Props { variant?: AlertVariants['variant']; class?: string }
const props = defineProps<Props>()
</script>
<template><div :class="cn(alertVariants({ variant }), props.class)" role="alert"><slot /></div></template>
EOF

cat << 'EOF' > src/components/ui/alert/AlertTitle.vue
<script setup lang="ts">import { cn } from '@/lib/utils'; const props = defineProps<{ class?: string }>()</script>
<template><h5 :class="cn('mb-1 font-medium leading-none tracking-tight', props.class)"><slot /></h5></template>
EOF

cat << 'EOF' > src/components/ui/alert/AlertDescription.vue
<script setup lang="ts">import { cn } from '@/lib/utils'; const props = defineProps<{ class?: string }>()</script>
<template><div :class="cn('text-sm [&_p]:leading-relaxed', props.class)"><slot /></div></template>
EOF

cat << 'EOF' > src/components/ui/skeleton/index.ts
export { default as Skeleton } from './Skeleton.vue'
EOF

cat << 'EOF' > src/components/ui/skeleton/Skeleton.vue
<script setup lang="ts">import { cn } from '@/lib/utils'; const props = defineProps<{ class?: string }>()</script>
<template><div :class="cn('animate-pulse rounded-md bg-muted', props.class)" /></template>
EOF

cat << 'EOF' > src/components/ui/switch/index.ts
export { default as Switch } from './Switch.vue'
EOF

cat << 'EOF' > src/components/ui/switch/Switch.vue
<script setup lang="ts">
import { cn } from '@/lib/utils'
import { SwitchRoot, SwitchThumb, type SwitchRootProps, type SwitchRootEmits, useForwardPropsEmits } from 'radix-vue'
const props = defineProps<SwitchRootProps & { class?: string }>()
const emits = defineEmits<SwitchRootEmits>()
const forwarded = useForwardPropsEmits(props, emits)
</script>
<template>
  <SwitchRoot v-bind="forwarded" :class="cn('peer inline-flex h-6 w-11 shrink-0 cursor-pointer items-center rounded-full border-2 border-transparent transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background disabled:cursor-not-allowed disabled:opacity-50 data-[state=checked]:bg-primary data-[state=unchecked]:bg-input', props.class)">
    <SwitchThumb :class="cn('pointer-events-none block h-5 w-5 rounded-full bg-background shadow-lg ring-0 transition-transform data-[state=checked]:translate-x-5 data-[state=unchecked]:translate-x-0')" />
  </SwitchRoot>
</template>
EOF

cat << 'EOF' > src/components/ui/tabs/index.ts
export { default as Tabs } from './Tabs.vue'
export { default as TabsList } from './TabsList.vue'
export { default as TabsTrigger } from './TabsTrigger.vue'
export { default as TabsContent } from './TabsContent.vue'
EOF

cat << 'EOF' > src/components/ui/tabs/Tabs.vue
<script setup lang="ts">
import { TabsRoot, type TabsRootProps, type TabsRootEmits, useForwardPropsEmits } from 'radix-vue'
const props = defineProps<TabsRootProps>()
const emits = defineEmits<TabsRootEmits>()
const forwarded = useForwardPropsEmits(props, emits)
</script>
<template><TabsRoot v-bind="forwarded"><slot /></TabsRoot></template>
EOF

cat << 'EOF' > src/components/ui/tabs/TabsList.vue
<script setup lang="ts">
import { cn } from '@/lib/utils'
import { TabsList, type TabsListProps } from 'radix-vue'
const props = defineProps<TabsListProps & { class?: string }>()
</script>
<template><TabsList v-bind="props" :class="cn('inline-flex h-10 items-center justify-center rounded-md bg-muted p-1 text-muted-foreground', props.class)"><slot /></TabsList></template>
EOF

cat << 'EOF' > src/components/ui/tabs/TabsTrigger.vue
<script setup lang="ts">
import { cn } from '@/lib/utils'
import { TabsTrigger, type TabsTriggerProps } from 'radix-vue'
const props = defineProps<TabsTriggerProps & { class?: string }>()
</script>
<template><TabsTrigger v-bind="props" :class="cn('inline-flex items-center justify-center whitespace-nowrap rounded-sm px-3 py-1.5 text-sm font-medium ring-offset-background transition-all focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50 data-[state=active]:bg-background data-[state=active]:text-foreground data-[state=active]:shadow-sm', props.class)"><slot /></TabsTrigger></template>
EOF

cat << 'EOF' > src/components/ui/tabs/TabsContent.vue
<script setup lang="ts">
import { cn } from '@/lib/utils'
import { TabsContent, type TabsContentProps } from 'radix-vue'
const props = defineProps<TabsContentProps & { class?: string }>()
</script>
<template><TabsContent v-bind="props" :class="cn('mt-2 ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2', props.class)"><slot /></TabsContent></template>
EOF

cat << 'EOF' > src/components/ui/dialog/index.ts
export { default as Dialog } from './Dialog.vue'
export { default as DialogTrigger } from './DialogTrigger.vue'
export { default as DialogContent } from './DialogContent.vue'
export { default as DialogHeader } from './DialogHeader.vue'
export { default as DialogTitle } from './DialogTitle.vue'
export { default as DialogDescription } from './DialogDescription.vue'
export { default as DialogFooter } from './DialogFooter.vue'
export { default as DialogClose } from './DialogClose.vue'
EOF

cat << 'EOF' > src/components/ui/dialog/Dialog.vue
<script setup lang="ts">
import { DialogRoot, type DialogRootProps, type DialogRootEmits, useForwardPropsEmits } from 'radix-vue'
const props = defineProps<DialogRootProps>()
const emits = defineEmits<DialogRootEmits>()
const forwarded = useForwardPropsEmits(props, emits)
</script>
<template><DialogRoot v-bind="forwarded"><slot /></DialogRoot></template>
EOF

cat << 'EOF' > src/components/ui/dialog/DialogTrigger.vue
<script setup lang="ts">
import { DialogTrigger, type DialogTriggerProps } from 'radix-vue'
const props = defineProps<DialogTriggerProps>()
</script>
<template><DialogTrigger v-bind="props"><slot /></DialogTrigger></template>
EOF

cat << 'EOF' > src/components/ui/dialog/DialogContent.vue
<script setup lang="ts">
import { cn } from '@/lib/utils'
import { DialogContent, DialogOverlay, DialogPortal, DialogClose, type DialogContentProps, type DialogContentEmits, useForwardPropsEmits } from 'radix-vue'
import { X } from 'lucide-vue-next'
const props = defineProps<DialogContentProps & { class?: string }>()
const emits = defineEmits<DialogContentEmits>()
const forwarded = useForwardPropsEmits(props, emits)
</script>
<template>
  <DialogPortal>
    <DialogOverlay class="fixed inset-0 z-50 bg-black/80 data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0" />
    <DialogContent v-bind="forwarded" :class="cn('fixed left-[50%] top-[50%] z-50 grid w-full max-w-lg translate-x-[-50%] translate-y-[-50%] gap-4 border bg-background p-6 shadow-lg duration-200 data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0 data-[state=closed]:zoom-out-95 data-[state=open]:zoom-in-95 data-[state=closed]:slide-out-to-left-1/2 data-[state=closed]:slide-out-to-top-[48%] data-[state=open]:slide-in-from-left-1/2 data-[state=open]:slide-in-from-top-[48%] sm:rounded-lg', props.class)">
      <slot />
      <DialogClose class="absolute right-4 top-4 rounded-sm opacity-70 ring-offset-background transition-opacity hover:opacity-100 focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2 disabled:pointer-events-none data-[state=open]:bg-accent data-[state=open]:text-muted-foreground"><X class="h-4 w-4" /><span class="sr-only">Close</span></DialogClose>
    </DialogContent>
  </DialogPortal>
</template>
EOF

cat << 'EOF' > src/components/ui/dialog/DialogHeader.vue
<script setup lang="ts">import { cn } from '@/lib/utils'; const props = defineProps<{ class?: string }>()</script>
<template><div :class="cn('flex flex-col space-y-1.5 text-center sm:text-left', props.class)"><slot /></div></template>
EOF

cat << 'EOF' > src/components/ui/dialog/DialogTitle.vue
<script setup lang="ts">
import { cn } from '@/lib/utils'
import { DialogTitle, type DialogTitleProps } from 'radix-vue'
const props = defineProps<DialogTitleProps & { class?: string }>()
</script>
<template><DialogTitle v-bind="props" :class="cn('text-lg font-semibold leading-none tracking-tight', props.class)"><slot /></DialogTitle></template>
EOF

cat << 'EOF' > src/components/ui/dialog/DialogDescription.vue
<script setup lang="ts">
import { cn } from '@/lib/utils'
import { DialogDescription, type DialogDescriptionProps } from 'radix-vue'
const props = defineProps<DialogDescriptionProps & { class?: string }>()
</script>
<template><DialogDescription v-bind="props" :class="cn('text-sm text-muted-foreground', props.class)"><slot /></DialogDescription></template>
EOF

cat << 'EOF' > src/components/ui/dialog/DialogFooter.vue
<script setup lang="ts">import { cn } from '@/lib/utils'; const props = defineProps<{ class?: string }>()</script>
<template><div :class="cn('flex flex-col-reverse sm:flex-row sm:justify-end sm:space-x-2', props.class)"><slot /></div></template>
EOF

cat << 'EOF' > src/components/ui/dialog/DialogClose.vue
<script setup lang="ts">
import { DialogClose, type DialogCloseProps } from 'radix-vue'
const props = defineProps<DialogCloseProps>()
</script>
<template><DialogClose v-bind="props"><slot /></DialogClose></template>
EOF

cat << 'EOF' > src/components/ui/select/index.ts
export { default as Select } from './Select.vue'
EOF

cat << 'EOF' > src/components/ui/select/Select.vue
<script setup lang="ts">
import { cn } from '@/lib/utils'
defineOptions({ inheritAttrs: false })
const props = defineProps<{ modelValue?: string | number; options?: Array<{ label: string; value: string | number }>; placeholder?: string; class?: string }>()
const emit = defineEmits<{ (e: 'update:modelValue', value: string | number): void }>()
</script>
<template>
  <div class="relative">
    <select :value="modelValue" @change="emit('update:modelValue', ($event.target as HTMLSelectElement).value)" :class="cn('flex h-10 w-full appearance-none items-center justify-between rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50', props.class)" v-bind="$attrs">
      <option v-if="placeholder" value="" disabled selected hidden>{{ placeholder }}</option>
      <option v-for="opt in options" :key="String(opt.value)" :value="opt.value">{{ opt.label }}</option>
    </select>
    <div class="pointer-events-none absolute inset-y-0 right-3 flex items-center"><svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="h-4 w-4 opacity-50"><polyline points="6 9 12 15 18 9"></polyline></svg></div>
  </div>
</template>
EOF

cat << 'EOF' > src/components/ui/sonner/index.ts
export { default as Toaster } from './Sonner.vue'
EOF

cat << 'EOF' > src/components/ui/sonner/Sonner.vue
<script lang="ts" setup>
import { Toaster as Sonner, type ToasterProps } from 'vue-sonner'
const props = defineProps<ToasterProps>()
</script>
<template>
  <Sonner class="toaster group" v-bind="props" :toast-options="{ classes: { toast: 'group toast group-[.toaster]:bg-background group-[.toaster]:text-foreground group-[.toaster]:border-border group-[.toaster]:shadow-lg', description: 'group-[.toast]:text-muted-foreground', actionButton: 'group-[.toast]:bg-primary group-[.toast]:text-primary-foreground', cancelButton: 'group-[.toast]:bg-muted group-[.toast]:text-muted-foreground' } }" />
</template>
EOF

cat << 'EOF' > src/components/ui/popover/index.ts
export { default as Popover } from './Popover.vue'
export { default as PopoverTrigger } from './PopoverTrigger.vue'
export { default as PopoverContent } from './PopoverContent.vue'
EOF

cat << 'EOF' > src/components/ui/popover/Popover.vue
<script setup lang="ts">
import { PopoverRoot, type PopoverRootProps, type PopoverRootEmits, useForwardPropsEmits } from 'radix-vue'
const props = defineProps<PopoverRootProps>()
const emits = defineEmits<PopoverRootEmits>()
const forwarded = useForwardPropsEmits(props, emits)
</script>
<template><PopoverRoot v-bind="forwarded"><slot /></PopoverRoot></template>
EOF

cat << 'EOF' > src/components/ui/popover/PopoverTrigger.vue
<script setup lang="ts">
import { PopoverTrigger, type PopoverTriggerProps } from 'radix-vue'
const props = defineProps<PopoverTriggerProps>()
</script>
<template><PopoverTrigger v-bind="props"><slot /></PopoverTrigger></template>
EOF

cat << 'EOF' > src/components/ui/popover/PopoverContent.vue
<script setup lang="ts">
import { cn } from '@/lib/utils'
import { PopoverContent, PopoverPortal, type PopoverContentProps, type PopoverContentEmits, useForwardPropsEmits } from 'radix-vue'
const props = defineProps<PopoverContentProps & { class?: string }>()
const emits = defineEmits<PopoverContentEmits>()
const forwarded = useForwardPropsEmits(props, emits)
</script>
<template>
  <PopoverPortal>
    <PopoverContent v-bind="forwarded" :class="cn('z-50 w-72 rounded-md border bg-popover p-4 text-popover-foreground shadow-md outline-none data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0 data-[state=closed]:zoom-out-95 data-[state=open]:zoom-in-95 data-[side=bottom]:slide-in-from-top-2 data-[side=left]:slide-in-from-right-2 data-[side=right]:slide-in-from-left-2 data-[side=top]:slide-in-from-bottom-2', props.class)">
      <slot />
    </PopoverContent>
  </PopoverPortal>
</template>
EOF

