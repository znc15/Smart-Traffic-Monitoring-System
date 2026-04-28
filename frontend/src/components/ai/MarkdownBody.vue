<template>
  <div class="markdown-body" v-html="renderedContent"></div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { renderMarkdown } from '../../lib/markdown'

const props = defineProps<{
  content: string
}>()

const renderedContent = computed(() => renderMarkdown(props.content))
</script>

<style scoped>
.markdown-body {
  line-height: 1.6;
}
.markdown-body p {
  margin: 0.4em 0;
}
.markdown-body ul,
.markdown-body ol {
  margin: 0.4em 0;
  padding-left: 1.5em;
}
.markdown-body li {
  margin: 0.15em 0;
}
.markdown-body code {
  background: hsl(var(--muted));
  padding: 0.15em 0.4em;
  border-radius: 4px;
  font-size: 0.875em;
}
.markdown-body :not(pre) > code {
  background: hsl(var(--muted));
}
.markdown-body pre {
  margin: 0.5em 0;
  border-radius: 6px;
  overflow-x: auto;
  background: hsl(var(--card));
  border: 1px solid hsl(var(--border));
}
.markdown-body pre code {
  display: block;
  padding: 0.75em 1em;
  background: transparent;
  font-size: 0.85em;
  line-height: 1.5;
}
.markdown-body strong {
  font-weight: 600;
}
.markdown-body blockquote {
  border-left: 3px solid hsl(var(--border));
  margin: 0.5em 0;
  padding-left: 0.75em;
  color: hsl(var(--muted-foreground));
}
.markdown-body table {
  border-collapse: collapse;
  margin: 0.5em 0;
  width: 100%;
  font-size: 0.875em;
}
.markdown-body th,
.markdown-body td {
  border: 1px solid hsl(var(--border));
  padding: 0.4em 0.6em;
  text-align: left;
}
.markdown-body th {
  background: hsl(var(--muted));
  font-weight: 600;
}
</style>