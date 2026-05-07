export type PresetQuestion = {
  label: string
  prompt: string
}

function normalizeRoads(roads: string[]): string[] {
  const seen = new Set<string>()
  return roads
    .map((road) => road.trim())
    .filter((road) => {
      if (!road || seen.has(road)) return false
      seen.add(road)
      return true
    })
}

function quoteRoad(road: string): string {
  return `「${road}」`
}

function roadPrompt(road: string): PresetQuestion {
  return {
    label: `${road}实时路况`,
    prompt: `请调用 query_traffic 查询道路${quoteRoad(road)}的实时交通数据，并用车流量、平均车速、拥堵指数和密度状态说明当前路况。`,
  }
}

export function buildPresetQuestions(roads: string[]): PresetQuestion[] {
  const normalizedRoads = normalizeRoads(roads)
  const [primaryRoad, secondaryRoad] = normalizedRoads

  if (primaryRoad) {
    const roadList = normalizedRoads.map(quoteRoad).join('、')
    const questions: PresetQuestion[] = [
      {
        label: '最拥堵道路',
        prompt: `请分别调用 query_traffic 查询以下监控道路的实时交通数据：${roadList}。对比 congestion_index、density_status 和平均车速，告诉我当前哪条路最拥堵，并说明依据。`,
      },
      roadPrompt(primaryRoad),
    ]

    if (secondaryRoad) {
      questions.push(roadPrompt(secondaryRoad))
    } else {
      questions.push({
        label: '历史趋势',
        prompt: `请调用 query_history 查询道路${quoteRoad(primaryRoad)}最近 24 小时的历史交通统计数据，并总结车流量、车速和拥堵指数的变化趋势。`,
      })
    }

    questions.push({
      label: '摄像头列表',
      prompt:
        '请调用 list_cameras 获取当前启用的摄像头列表，并按道路汇总摄像头名称、位置和在线可用情况。',
    })

    return questions
  }

  return [
    {
      label: '摄像头列表',
      prompt:
        '请调用 list_cameras 获取当前启用的摄像头列表，并按道路汇总摄像头名称、位置和在线可用情况。',
    },
    {
      label: '道路清单',
      prompt: '请调用 list_cameras 获取当前监控道路和摄像头清单，整理可查询的道路名称。',
    },
    {
      label: '实时路况',
      prompt:
        '请先调用 list_cameras 获取可用道路，再选择当前摄像头覆盖的道路调用 query_traffic 查询实时交通数据。',
    },
    {
      label: '分流建议',
      prompt: '请先调用 list_cameras 获取可用道路，再基于可查询道路的实时交通数据给出分流建议。',
    },
  ]
}
