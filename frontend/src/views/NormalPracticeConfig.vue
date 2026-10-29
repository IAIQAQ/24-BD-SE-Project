<script setup lang="ts">
import type {Point} from '../api/contracts';
defineProps<{points:Point[];selectedIds:string[];total:number;allocations:Record<string,number>;prefix:string;disabled:boolean}>();
defineEmits<{toggle:[id:string];total:[value:number];allocation:[id:string,value:number]}>();
</script>
<template>
 <fieldset :disabled="disabled"><legend>选择当前年级知识点</legend>
  <p v-if="!points.length" class="muted">当前年级暂无启用知识点。</p>
  <div v-for="id in selectedIds.filter(id=>!points.some(p=>p.id===id))" :key="id" class="card"><p class="error">已选知识点当前不可用，请取消后重新配置。</p><button type="button" @click="$emit('toggle',id)">取消不可用知识点</button></div>
  <div v-for="p in points" :key="p.id" class="card">
   <label><input class="checkbox" type="checkbox" :id="`${prefix}-point-${p.id}`" :checked="selectedIds.includes(p.id)" @change="$emit('toggle',p.id)">{{p.name}} · 可用{{p.available}}题</label>
   <template v-if="selectedIds.includes(p.id)"><label :for="`${prefix}-allocation-${p.id}`">{{p.name}}题量</label><input :id="`${prefix}-allocation-${p.id}`" type="number" inputmode="numeric" min="1" max="100" step="1" required :value="allocations[p.id]" @input="$emit('allocation',p.id,Number(($event.target as HTMLInputElement).value))"></template>
  </div>
  <label :for="prefix==='student'?'total':'parentTotal'">练习题量（总题量）</label><input :id="prefix==='student'?'total':'parentTotal'" type="number" inputmode="numeric" min="1" max="100" step="1" required :value="total" @input="$emit('total',Number(($event.target as HTMLInputElement).value))">
  <p class="muted">默认30题，可选1—100题。改变知识点选择或总题量后均分，可逐项调整；分配合计必须等于总题量，每份题目不重复。</p>
  <p role="status">已选{{selectedIds.length}}个知识点 · 已分配{{selectedIds.reduce((n,id)=>n+(allocations[id]||0),0)}} / {{total}}题</p>
 </fieldset>
</template>
