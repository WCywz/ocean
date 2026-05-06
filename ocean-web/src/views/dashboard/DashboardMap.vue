<template>
  <div>
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px;">
      <h3 class="editorial-section-heading" style="margin: 0;">数据覆盖分布</h3>
      <el-radio-group :model-value="activeType" size="small" @change="$emit('typeChange', $event)">
        <el-radio-button value="SST">SST</el-radio-button>
        <el-radio-button value="CHL">CHL</el-radio-button>
      </el-radio-group>
    </div>
    <OceanMap
      :gridData="gridData"
      :colorRanges="colorRanges"
      :legendLabels="legendLabels"
      :legendTitle="legendTitle"
      :loading="loading"
      :height="height"
      :center="center"
      :zoom="zoom"
      @cellClick="(pos) => $emit('cellClick', pos)"
    />
  </div>
</template>

<script setup>
import OceanMap from '../../components/OceanMap.vue'

defineProps({
  gridData: { type: Array, default: () => [] },
  colorRanges: { type: Array, default: () => [] },
  legendLabels: { type: Array, default: () => [] },
  legendTitle: { type: String, default: '' },
  loading: { type: Boolean, default: false },
  activeType: { type: String, default: 'SST' }
})

defineEmits(['typeChange', 'cellClick'])

const height = '300px'
const center = [29.8, 123.5]
const zoom = 7
</script>

<style scoped>
/* uses editorial-section-heading from editorial.css */
</style>
