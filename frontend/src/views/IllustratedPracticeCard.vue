<script setup lang="ts">
import {computed,ref,watch} from 'vue';
import type {Summary} from '../api/contracts';
import UiIcon from './UiIcon.vue';
import notebook from '../assets/sprout/p2-scene/task-notebook.webp';
import book from '../assets/sprout/p2-scene/continue-book.webp';
const props=defineProps<{record:Summary;busy:boolean}>();
defineEmits<{open:[id:string]}>();
const failed=ref(false);
const artwork=computed(()=>props.record.status==='PENDING'?notebook:book);
const action=computed(()=>({PENDING:'开始',IN_PROGRESS:'继续',COMPLETED:'看结果',WITHDRAWN:'查看',VOID:'查看'}[props.record.status]??'查看'));
const status=computed(()=>({PENDING:'待完成',IN_PROGRESS:'进行中',COMPLETED:'已完成',WITHDRAWN:'已撤回',VOID:'已作废'}[props.record.status]??props.record.status));
const kind=computed(()=>props.record.practiceKind==='REPLAY'?'原题重练':props.record.practiceKind==='REINFORCE'?'同类强化':'普通练习');
watch(artwork,()=>{failed.value=false;});
</script>
<template><article class="illustrated-practice"><button class="record illustrated-card" type="button" :disabled="busy" @click="$emit('open',record.id)"><span class="practice-art" aria-hidden="true"><img v-if="!failed" :src="artwork" width="256" height="256" alt="" @error="failed=true"><UiIcon v-else name="practice"/></span><span class="practice-caption"><b>{{record.status==='IN_PROGRESS'?'继续上次练习':record.total+'题练习'}}</b><span class="practice-tag" :class="{parent:record.source==='PARENT'}">{{record.source==='PARENT'?'家长安排 · ':''}}{{kind}}</span><small v-if="record.status==='IN_PROGRESS'">上次看到第{{record.currentSeq}}题 · 共{{record.total}}题<br>练习已保存</small><small v-else>{{status}}</small></span><span class="practice-action" :class="{start:record.status==='PENDING'}">{{action}}<UiIcon name="arrow"/></span></button><details class="practice-content"><summary>练习内容</summary><p>{{record.pointName??'口算练习'}} · {{status}}</p></details></article></template>
<style scoped>
.illustrated-practice{background:#fffef9;border:1px solid #e6ecde;border-radius:24px;box-shadow:0 5px 15px #244f3610;overflow:hidden;margin:14px 0}
.learning-ui .illustrated-card{display:grid;grid-template-columns:80px minmax(0,1fr) auto;gap:10px;border:0;border-radius:0;background:transparent;box-shadow:none;margin:0;padding:14px 12px 8px;min-height:106px;width:100%;text-align:left;align-items:center}
.practice-art{display:grid;place-items:center;width:80px;height:80px;flex-shrink:0}
.practice-art img{display:block;width:100%;height:100%;object-fit:contain}
.practice-caption{display:flex;flex-direction:column;gap:5px;min-width:0}
.learning-ui .practice-caption b{font-size:18px;color:#1b4d39;font-weight:700;line-height:1.45}
.practice-tag{align-self:flex-start;font-size:14px!important;line-height:1.5;background:#edf2e2;border-radius:8px;padding:3px 6px;color:#57744e!important;overflow-wrap:anywhere}
.practice-tag.parent{background:#fff0d6;color:#9a6936!important}
.learning-ui .practice-caption small{font-size:14px;color:#6c8460;margin-top:0;line-height:1.5}
.learning-ui .practice-action{display:flex;align-items:center;justify-content:center;gap:3px;flex-shrink:0;font-size:14px;white-space:nowrap;color:#285b49;margin-left:0!important;min-height:48px;padding:5px 2px}
.practice-action.start{padding:8px 12px;background:linear-gradient(130deg,#39785a,#275d43);border-radius:24px;color:white!important}
.learning-ui .practice-action .ui-icon{width:20px;height:20px}
.learning-ui .practice-content{margin:0;padding:0 18px 4px}
.learning-ui .practice-content summary{font-size:14px;min-height:48px;padding:5px 0;color:#6b7c60}
.practice-content p{font-size:14px;margin:6px 0 12px;color:#586e50;overflow-wrap:anywhere}
@media(max-width:360px){.learning-ui .illustrated-card{grid-template-columns:58px minmax(0,1fr) auto;padding:12px 10px 6px;gap:8px}.practice-art{width:58px;height:66px}.learning-ui .practice-caption b{font-size:16px}.learning-ui .practice-action{font-size:13px}.practice-action.start{padding:7px 9px}.learning-ui .practice-action .ui-icon{width:17px;height:17px}}
</style>
