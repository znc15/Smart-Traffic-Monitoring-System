import { Marked } from 'marked'
import hljs from 'highlight.js'

const marked = new Marked({
  gfm: true,
  breaks: true,
})

marked.use({
  renderer: {
    code({ text, lang }: { text: string; lang?: string }) {
      const language = lang && hljs.getLanguage(lang) ? lang : undefined
      const highlighted = language
        ? hljs.highlight(text, { language }).value
        : hljs.highlightAuto(text).value
      return `<pre class="hljs-pre"><code class="hljs${language ? ` language-${language}` : ''}">${highlighted}</code></pre>`
    },
  },
})

export function renderMarkdown(source: string): string {
  return marked.parse(source) as string
}
