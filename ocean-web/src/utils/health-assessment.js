const LEVELS = {
  good: { label: '优', color: '#22c55e' },
  fine: { label: '良', color: '#22c55e' },
  warn: { label: '中', color: '#f59e0b' },
  bad:  { label: '差', color: '#ef4444' }
}

function assessSst(sst) {
  const anomaly = Math.abs(sst.anomaly || 0)
  if (anomaly > 2.5) return 'bad'
  if (anomaly > 1.5) return 'warn'
  if (anomaly > 0.5) return 'fine'
  return 'good'
}

function assessChl(chl) {
  const avg = chl.avg || 0
  if (avg >= 5.0) return 'bad'
  if (avg >= 3.0) return 'warn'
  if (avg >= 2.0) return 'fine'
  return 'good'
}

function assessHeatwave(hw) {
  if (hw.active) return 'bad'
  return 'good'
}

function worstLevel(...levels) {
  const order = ['good', 'fine', 'warn', 'bad']
  let worst = 'good'
  for (const l of levels) {
    if (order.indexOf(l) > order.indexOf(worst)) worst = l
  }
  return worst
}

function buildAdvice(sstLevel, chlLevel, hwLevel) {
  const advices = []
  if (hwLevel === 'bad') {
    advices.push('远海渔业注意水温变化，评估对远洋作业的潜在影响')
  }
  if (sstLevel === 'bad' || sstLevel === 'warn') {
    advices.push('关注未来 3 天 SST 变化趋势')
  }
  if (chlLevel === 'bad' || chlLevel === 'warn') {
    advices.push('赤潮风险升高，建议加强监测')
  }
  if (advices.length === 0) {
    advices.push('各项指标正常，可正常作业')
  }
  return advices
}

function trendDirection(values) {
  if (values.length < 2) return 'stable'
  const first = values.slice(0, 2).reduce((a, b) => a + b, 0) / 2
  const last = values.slice(-2).reduce((a, b) => a + b, 0) / 2
  if (last - first > 0.3) return 'rising'
  if (first - last > 0.3) return 'falling'
  return 'stable'
}

function trendSummary(recent) {
  if (!recent || recent.length < 3) return '数据不足'

  const anomalies = recent.map(r => (r.sst && r.sst.anomaly) || 0)
  const chls = recent.map(r => (r.chl && r.chl.avg) || 0)
  const grades = recent.map(r => r.overallGrade || 'good')
  const order = ['good', 'fine', 'warn', 'bad']

  const sstDir = trendDirection(anomalies)
  const chlDir = trendDirection(chls)

  const parts = []
  if (sstDir === 'rising' && anomalies[anomalies.length - 1] > 0.5) {
    parts.push('SST 持续偏高')
  } else if (sstDir === 'falling' && anomalies[anomalies.length - 1] > 0.5) {
    parts.push('SST 偏高但趋于回落')
  } else if (sstDir === 'falling') {
    parts.push('SST 逐步回落')
  }

  if (chlDir === 'rising' && chls[chls.length - 1] >= 2.0) {
    parts.push('Chl 趋于上升')
  } else if (chlDir === 'rising') {
    parts.push('Chl 缓慢上升')
  } else if (chlDir === 'falling') {
    parts.push('Chl 趋于下降')
  }

  const lastGrade = grades[grades.length - 1]
  const firstGrade = grades[0]
  if (order.indexOf(lastGrade) > order.indexOf(firstGrade)) {
    parts.push('等级由' + LEVELS[firstGrade].label + '转' + LEVELS[lastGrade].label)
  }

  if (parts.length === 0) return '各项指标稳定，状况良好'
  return parts.join('，')
}

function buildTrendNarrative(recent, current) {
  if (!recent || !recent.length) return '暂无近期数据。'

  const anomalies = recent.map(r => (r.sst && r.sst.anomaly) || 0)
  const sstDir = trendDirection(anomalies)
  const lastGrade = recent[recent.length - 1].overallGrade || 'good'
  const firstGrade = recent[0].overallGrade || 'good'
  const order = ['good', 'fine', 'warn', 'bad']

  let text = '近 5 日'

  if (sstDir === 'rising') {
    text += ' SST 持续偏高'
  } else if (sstDir === 'falling') {
    text += ' SST 逐步回落'
  } else {
    text += ' SST 波动不大'
  }

  const curSst = current.sst || {}
  if (curSst.anomaly != null && Math.abs(curSst.anomaly) > 0.1) {
    const sign = curSst.anomaly > 0 ? '+' : ''
    text += '，今日异常 ' + sign + curSst.anomaly.toFixed(1) + '°C'
  }

  if (order.indexOf(lastGrade) > order.indexOf(firstGrade)) {
    text += '，整体等级由' + LEVELS[firstGrade].label + '转' + LEVELS[lastGrade].label
  } else if (order.indexOf(lastGrade) < order.indexOf(firstGrade)) {
    text += '，整体等级由' + LEVELS[firstGrade].label + '恢复至' + LEVELS[lastGrade].label
  }

  text += '。'

  const curChl = current.chl || {}
  if (curChl.avg != null && curChl.avg > 0) {
    if (curChl.avg >= 3.0) {
      text += ' Chl 浓度 ' + curChl.avg.toFixed(1) + ' mg/m³，偏高'
    } else if (curChl.avg >= 2.0) {
      text += ' Chl 浓度 ' + curChl.avg.toFixed(1) + ' mg/m³，正常偏高'
    } else {
      text += ' Chl 浓度正常'
    }

    const chls = recent.map(r => (r.chl && r.chl.avg) || 0)
    const chlDir = trendDirection(chls)
    if (chlDir === 'rising') {
      text += '，呈上升趋势'
    } else if (chlDir === 'falling') {
      text += '，呈下降趋势'
    } else {
      text += '，无明显变化趋势'
    }
    text += '。'
  }

  return text
}

function buildOutlookNarrative(forecast, current) {
  if (!forecast || !forecast.length) return '暂无预报数据。'

  const order = ['good', 'fine', 'warn', 'bad']
  const curGrade = (current.overallGrade) || 'good'
  const forecastGrades = forecast.map(f => f.overallGrade || 'good')
  const worstFc = forecastGrades.reduce((w, g) => order.indexOf(g) > order.indexOf(w) ? g : w, 'good')
  const bestFc = forecastGrades.reduce((b, g) => order.indexOf(g) < order.indexOf(b) ? g : b, 'bad')

  let text = '未来 ' + forecast.length + ' 日'

  if (order.indexOf(worstFc) > order.indexOf(curGrade)) {
    text += ' 有恶化趋势'
  } else if (order.indexOf(bestFc) < order.indexOf(curGrade)) {
    const betterDay = forecast.find(f => order.indexOf(f.overallGrade || 'good') < order.indexOf(curGrade))
    if (betterDay) {
      text += ' ' + betterDay.date.slice(5) + ' 预计恢复至' + LEVELS[betterDay.overallGrade || 'good'].label
    } else {
      text += ' 预计逐步恢复'
    }
  } else if (worstFc === bestFc && worstFc === curGrade) {
    text += ' 维持' + LEVELS[curGrade].label + '水平'
  } else {
    text += ' 波动不大'
  }

  text += '。'

  const curChl = current.chl || {}
  const curSst = current.sst || {}
  const hw = current.heatwave || {}

  const advices = []
  if (hw.active) {
    advices.push('海洋热浪持续活跃，建议关注远海渔业')
  }
  if ((curSst.anomaly != null && Math.abs(curSst.anomaly) > 1.5) || worstFc === 'bad' || worstFc === 'warn') {
    advices.push('建议关注 SST 变化趋势')
  }
  if ((curChl.avg != null && curChl.avg >= 3.0) || (curChl.avg != null && curChl.avg >= 2.0 && worstFc === 'warn')) {
    advices.push('赤潮风险需关注')
  }

  if (advices.length > 0) {
    text += ' ' + advices.join('。') + '。'
  }

  return text
}

export function buildZoneAssessment(zone) {
  const cur = zone.current || {}
  const sstLevel = assessSst(cur.sst || {})
  const chlLevel = assessChl(cur.chl || {})
  const hwLevel = assessHeatwave(cur.heatwave || {})
  const overallLevel = worstLevel(sstLevel, chlLevel, hwLevel)

  return {
    id: zone.id,
    label: zone.label,
    overall: {
      level: overallLevel,
      ...LEVELS[overallLevel]
    },
    sst: {
      level: sstLevel,
      ...LEVELS[sstLevel],
      value: (cur.sst || {}).avg,
      max: (cur.sst || {}).max,
      anomaly: (cur.sst || {}).anomaly,
      trend: (cur.sst || {}).trend || 'stable'
    },
    chl: {
      level: chlLevel,
      ...LEVELS[chlLevel],
      value: (cur.chl || {}).avg,
      max: (cur.chl || {}).max,
      trend: (cur.chl || {}).trend || 'stable'
    },
    heatwave: {
      level: hwLevel,
      ...LEVELS[hwLevel],
      active: (cur.heatwave || {}).active || false,
      days: (cur.heatwave || {}).days || 0
    },
    recent: zone.recent || [],
    forecast: zone.forecast || [],
    trendText: trendSummary(zone.recent || []),
    trendNarrative: buildTrendNarrative(zone.recent || [], cur),
    outlookNarrative: buildOutlookNarrative(zone.forecast || [], cur),
    advice: buildAdvice(sstLevel, chlLevel, hwLevel)
  }
}

export function buildOverallSummary(assessments) {
  const badOnes = assessments.filter(a => a.overall.level === 'bad' || a.overall.level === 'warn')
  if (badOnes.length === 0) {
    return '当前海洋状况总体良好，所有子区域各项指标均在正常范围内。'
  }
  const names = badOnes.map(a => a.label).join('、')
  return names + '区域需关注，请查看详情了解具体风险。'
}
