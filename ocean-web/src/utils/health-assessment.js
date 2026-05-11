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

export function buildZoneAssessment(zone) {
  const sstLevel = assessSst(zone.sst)
  const chlLevel = assessChl(zone.chl)
  const hwLevel = assessHeatwave(zone.heatwave)
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
      value: zone.sst.avg,
      max: zone.sst.max,
      anomaly: zone.sst.anomaly,
      trend: zone.sst.trend
    },
    chl: {
      level: chlLevel,
      ...LEVELS[chlLevel],
      value: zone.chl.avg,
      max: zone.chl.max,
      trend: zone.chl.trend
    },
    heatwave: {
      level: hwLevel,
      ...LEVELS[hwLevel],
      active: zone.heatwave.active,
      days: zone.heatwave.days
    },
    advice: buildAdvice(sstLevel, chlLevel, hwLevel)
  }
}

export function buildOverallSummary(assessments) {
  const badOnes = assessments.filter(a => a.overall.level === 'bad' || a.overall.level === 'warn')
  if (badOnes.length === 0) {
    return '当前海洋状况总体良好，所有子区域各项指标均在正常范围内。'
  }
  const names = badOnes.map(a => a.label).join('、')
  return `${names}区域需关注，请查看详情了解具体风险。`
}
