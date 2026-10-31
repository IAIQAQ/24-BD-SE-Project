<script setup lang="ts">
import {computed,onMounted,onUnmounted,ref,watch} from 'vue';
import seedling from '../assets/sprout/p3-growth/seedling.webp';
import unfolding from '../assets/sprout/p3-growth/unfolding.webp';
import grown from '../assets/sprout/p3-growth/grown.webp';
const props=defineProps<{answered:number;total:number;active:boolean}>();
const filled=computed(()=>Math.min(props.total,Math.max(0,props.answered)));
const stage=computed(()=>props.total<=0?0:filled.value>=props.total?3:filled.value>=props.total/2?2:1);
const artwork=computed(()=>stage.value===3?grown:stage.value===2?unfolding:seedling);
const stageName=computed(()=>stage.value===3?'长成':stage.value===2?'舒展':'幼苗');
const changing=ref(false),failed=ref(false);
let timer:ReturnType<typeof setTimeout>|undefined;
function stop(){clearTimeout(timer);timer=undefined;changing.value=false;}
function visibility(){if(document.hidden)stop();}
watch(stage,()=>{failed.value=false;stop();if(props.active&&props.total>0&&(typeof document==='undefined'||!document.hidden)){changing.value=true;timer=setTimeout(stop,360);}});
watch(()=>props.active,value=>{if(!value)stop();});
onMounted(()=>{if(typeof document!=='undefined')document.addEventListener('visibilitychange',visibility);});
onUnmounted(()=>{stop();if(typeof document!=='undefined')document.removeEventListener('visibilitychange',visibility);});
</script>
<template><div v-if="total>0" class="practice-growth" :class="{'growth-changing':changing}" :data-stage="stage"><span class="growth-picture" aria-hidden="true"><img v-if="!failed" class="growth-plant" :key="artwork" :src="artwork" width="384" height="384" alt="" @error="failed=true"><span v-else class="growth-fallback">芽</span></span><span class="growth-copy"><span class="growth-heading"><b>已填写 {{filled}}/{{total}} 题</b><span class="growth-stage">{{stageName}}</span></span><progress :value="filled" :max="total" :aria-label="`已填写 ${filled} 题，共 ${total} 题`"></progress></span></div></template>
<style scoped>
.practice-growth{display:flex;align-items:center;gap:12px;background:#edf4e4;border-radius:18px;padding:8px 14px;margin:12px 0 6px}
.growth-picture{display:block;position:relative;flex:0 0 80px;width:80px;height:80px;pointer-events:none}
.growth-picture img{display:block;width:100%;height:100%;object-fit:contain}
.growth-changing .growth-plant{animation:growth-unfold 320ms ease-out 1}
.growth-copy{display:block;flex:1;min-width:0;color:#285b49}
.growth-heading{display:flex;flex-wrap:wrap;align-items:center;gap:4px 8px}
.growth-stage{font-size:14px;line-height:1.5;padding:2px 8px;border-radius:10px;background:#dae9c7;color:#41652f}
.growth-copy b{font-size:16px;font-weight:600}
.growth-copy progress{display:block;width:100%;margin:8px 0 0;height:7px;accent-color:#72994f}
.growth-fallback{display:grid;place-items:center;width:100%;height:100%;font-size:26px;color:#285b49}
@keyframes growth-unfold{from{opacity:.65;transform:translateY(3px)}to{opacity:1;transform:translateY(0)}}
@media(prefers-reduced-motion:reduce){.growth-changing .growth-plant{animation:none}}
</style>
