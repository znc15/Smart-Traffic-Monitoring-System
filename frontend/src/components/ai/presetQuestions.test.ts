import { describe, expect, it } from 'vitest'
import { buildPresetQuestions } from './presetQuestions'

describe('buildPresetQuestions', () => {
  it('builds actionable road-specific prompts when roads are available', () => {
    const questions = buildPresetQuestions(['中间', '门口'])

    expect(questions.map((q) => q.label)).toEqual([
      '最拥堵道路',
      '中间实时路况',
      '门口实时路况',
      '摄像头列表',
    ])
    expect(questions[0].prompt).toContain('query_traffic')
    expect(questions[0].prompt).toContain('「中间」')
    expect(questions[0].prompt).toContain('「门口」')
    expect(questions[1].prompt).toContain('道路「中间」')
    expect(questions[2].prompt).toContain('道路「门口」')
    expect(questions[3].prompt).toContain('list_cameras')
  })

  it('deduplicates and trims road names before building prompts', () => {
    const questions = buildPresetQuestions([' 中间 ', '中间', '门口'])

    expect(questions[0].prompt).toContain('「中间」、「门口」')
    expect(questions.filter((q) => q.label === '中间实时路况')).toHaveLength(1)
  })

  it('keeps fallback prompts usable when roads cannot be loaded', () => {
    const questions = buildPresetQuestions([])

    expect(questions).toHaveLength(4)
    expect(questions.every((q) => q.prompt.includes('调用'))).toBe(true)
    expect(questions.some((q) => q.prompt.includes('list_cameras'))).toBe(true)
  })
})
