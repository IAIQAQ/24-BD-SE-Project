<script setup lang="ts">
import {ref,watch} from 'vue';
import UiIcon from './UiIcon.vue';
import type {Point} from '../api/contracts';
const props=defineProps<{points:Point[];selectedIds:string[];total:number;allocations:Record<string,number>;prefix:string;disabled:boolean}>();
defineEmits<{toggle:[id:string];total:[value:number];allocation:[id:string,value:number]}>();
const custom=ref(![5,10,20].includes(props.total));
watch(()=>props.total,value=>{if(![5,10,20].includes(value))custom.value=true;});
</script>
<template>
 <fieldset class="normal-config" :disabled="disabled"><legend>选内容</legend>
  <p v-if="!points.length" class="muted">这个年级暂时还没有可练习的内容，请找家长或老师帮忙。</p>
  <div v-for="id in selectedIds.filter(id=>!points.some(p=>p.id===id))" :key="id" class="card"><p class="error">刚才选的内容暂时不能练了，请取消后重新选。</p><button type="button" @click="$emit('toggle',id)">取消这项选择</button></div>
  <div class="point-grid"><label v-for="p in points" :key="p.id" class="point-choice" :class="{chosen:selectedIds.includes(p.id)}">
   <input class="checkbox" type="checkbox" :id="`${prefix}-point-${p.id}`" :checked="selectedIds.includes(p.id)" @change="$emit('toggle',p.id)"><span class="point-symbol"><UiIcon :name="p.name.includes('加')?'add':p.name.includes('减')?'subtract':p.name.includes('乘')?'multiply':p.name.includes('除')?'divide':'practice'"/></span><b>{{p.name}}</b><small>{{p.available}}道可练</small>
  </label></div>
  <h2>选题数</h2><div class="quantity-buttons" aria-label="练习题数"><button v-for="n in [5,10,20]" :key="n" type="button" :aria-pressed="!custom&&total===n" :class="{selected:!custom&&total===n}" @click="custom=false;$emit('total',n)">{{n}}题</button><button type="button" :aria-pressed="custom" :class="{selected:custom}" @click="custom=!custom">自定义</button></div>
  <div v-if="custom"><label :for="prefix==='student'?'total':'parentTotal'">这次一共练几题？</label><input :id="prefix==='student'?'total':'parentTotal'" type="number" inputmode="numeric" min="1" max="100" step="1" required :value="total" @input="$emit('total',Number(($event.target as HTMLInputElement).value))"></div>
  <details :key="selectedIds.length>1?'multi':'single'" :open="selectedIds.length>1" class="allocation-details" @invalid.capture="($event.currentTarget as HTMLDetailsElement).open=true"><summary>每项练几题？</summary><template v-for="p in points.filter(p=>selectedIds.includes(p.id))" :key="p.id"><label :for="`${prefix}-allocation-${p.id}`">{{p.name}}</label><input :id="`${prefix}-allocation-${p.id}`" type="number" inputmode="numeric" min="1" max="100" step="1" required :value="allocations[p.id]" @input="$emit('allocation',p.id,Number(($event.target as HTMLInputElement).value))"></template></details>
  <p class="config-total" role="status">共 <b>{{total}}</b> 题 · 已安排 {{selectedIds.reduce((n,id)=>n+(allocations[id]||0),0)}} 题</p>
  <details><summary>题量说明</summary><p class="muted">可选1—100题，默认30题。改变内容或总题数后平均分配，也能自己调整。每项至少1题，加起来等于总题数；每份练习不重复抽题。</p></details>
 </fieldset>
</template>
