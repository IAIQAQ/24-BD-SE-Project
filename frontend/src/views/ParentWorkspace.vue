<script setup lang="ts">
import UiIcon from './UiIcon.vue';
import {confirmAction} from '../composables/confirmation';
import {useReviewSelection} from '../composables/useReviewSelection';
import ReviewPracticePanel from './ReviewPracticePanel.vue';
import NormalPracticeConfig from './NormalPracticeConfig.vue';
import {useNormalComposition} from '../composables/useNormalComposition';
import {computed,onMounted,ref,watch} from 'vue';import {apiGet,apiPost,ApiError,practiceApi,type UserProfile} from '../api/client';import type {Point,ResultItem,Summary,Mistake} from '../api/contracts';import FamilyLinks,{type FamilyLink} from './FamilyLinks.vue';
const props=defineProps<{profile:UserProfile}>(),emit=defineEmits<{expired:[]}>();
const tab=ref('children'),links=ref<FamilyLink[]>([]),childId=ref(''),points=ref<Point[]>([]),tasks=ref<Summary[]>([]),preview=ref<any>(null),detail=ref<any>(null),mistakes=ref<ResultItem[]>([]),stats=ref<any>(null),start=ref(''),end=ref(''),busy=ref(false),error=ref(''),notice=ref('');const normal=useNormalComposition();let normalOwner='';
const review=useReviewSelection(()=>childId.value,true),mistakeDetail=ref<Mistake|null>(null);
const kind=(s?:string)=>({REPLAY:'原题重练',REINFORCE:'同类强化'}[s??'']??'普通练习');
watch(childId,()=>{review.clear();preview.value=null;mistakeDetail.value=null;});
watch(()=>[review.selectedIds.value,review.grade.value,review.allocations.value],()=>{preview.value=null;});
async function previewReinforcement(){const record=await review.reinforce();if(record){preview.value=record;notice.value='请核对当前孩子、强化年级及以下实际题目，15分钟内有效。';tab.value='tasks';}}
const viewMistake=(item:ResultItem)=>run(async()=>{mistakeDetail.value=await apiGet<Mistake>(`/family/children/${childId.value}/mistakes/${item.id}`);});
const children=computed(()=>links.value.filter(l=>l.state==='ACTIVE')),child=computed(()=>children.value.find(l=>l.studentId===childId.value));
const state=(s:string)=>({PREVIEW:'未发送',PENDING:'待完成',IN_PROGRESS:'进行中',COMPLETED:'已完成',WITHDRAWN:'已撤回',VOID:'已作废'}[s]??s);
async function run(f:()=>Promise<void>){if(busy.value)return;busy.value=true;error.value='';try{await f();}catch(e){error.value=e instanceof Error?e.message:'操作失败';if(e instanceof ApiError&&e.status===401)emit('expired');if(e instanceof ApiError&&e.status===403){tasks.value=[];detail.value=null;preview.value=null;mistakes.value=[];}}finally{busy.value=false;}}
async function loadChild(){review.clear();mistakeDetail.value=null;tasks.value=[];preview.value=null;detail.value=null;mistakes.value=[];stats.value=null;if(!childId.value)return;const selected=childId.value;const [list,catalog]=await Promise.all([apiGet<{items:Summary[]}>(`/family/children/${selected}/practices`),practiceApi.points()]);if(selected!==childId.value)return;tasks.value=list.items;points.value=catalog.filter(p=>p.grade===child.value?.grade);const owner=selected+':'+child.value?.grade;if(normalOwner!==owner){normal.reset(points.value);normalOwner=owner;}}
async function reloadLinks(){links.value=await apiGet<FamilyLink[]>('/family/links');if(!children.value.some(l=>l.studentId===childId.value))childId.value=children.value[0]?.studentId??'';await loadChild();}
const refresh=()=>run(reloadLinks),choose=()=>run(loadChild);
function changeConfig(){preview.value=null;}
const compose=()=>run(async()=>{review.clear();const selected=childId.value,input=normal.request(selected,points.value);const result=await apiPost(`/family/children/${selected}/previews`,input);if(selected!==childId.value||!normal.isCurrent(input.operationKey))return;preview.value=result;notice.value='请核对孩子及以下实际题目，预览15分钟内有效。';});
const send=()=>run(async()=>{if(!preview.value||!await confirmAction(`确认将以下 ${preview.value.total} 题发送给 ${child.value?.nickname}？`))return;await apiPost(`/family/previews/${preview.value.id}/send`,{confirm:true});preview.value=null;normal.finish();notice.value='已发送，学生可在练习列表中查看。';await loadChild();});
const view=(p:Summary)=>run(async()=>{detail.value=await apiGet(`/family/children/${childId.value}/practices/${p.id}`);});
const withdraw=(p:Summary)=>run(async()=>{if(!await confirmAction('确认撤回尚未开始的这份任务？'))return;await apiPost(`/family/practices/${p.id}/withdraw`,{confirm:true});await loadChild();});
const loadMistakes=()=>run(async()=>{mistakes.value=[];if(childId.value)mistakes.value=await apiGet<ResultItem[]>(`/family/children/${childId.value}/mistakes`);tab.value='mistakes';});
const loadStats=()=>run(async()=>{stats.value=null;if(!childId.value)return;const params=new URLSearchParams();if(start.value)params.set('start',start.value);if(end.value)params.set('end',end.value);stats.value=await apiGet(`/family/children/${childId.value}/stats?${params}`);tab.value='stats';});
onMounted(refresh);
</script>
<template>
<section class="parent-workspace">
 <p class="eyebrow">陪孩子慢慢成长</p><h1>家长学习中心</h1>
 <p class="error" v-if="error" role="alert">{{error}}</p><p class="error" v-if="review.error.value" role="alert">{{review.error.value}}</p><p class="notice" v-if="notice" role="status">{{notice}}</p>
 <div class="child-focus"><label for="childSelect">当前孩子</label><select id="childSelect" v-model="childId" :disabled="busy" @change="choose"><option value="">请选择已关联孩子</option><option v-for="item in children" :key="item.id" :value="item.studentId">{{item.nickname}} · {{item.grade}}年级</option></select><button class="text-button" :disabled="busy" @click="refresh">刷新孩子和任务</button></div>
 <div class="parent-tabs" aria-label="家长功能">
  <button :class="{active:tab==='children'}" :aria-pressed="tab==='children'" @click="tab='children'"><UiIcon name="family"/>孩子与关联</button>
  <button :class="{active:tab==='tasks'}" :aria-pressed="tab==='tasks'" @click="tab='tasks'"><UiIcon name="practice"/>练习任务</button>
  <button :class="{active:tab==='mistakes'}" :aria-pressed="tab==='mistakes'" :disabled="busy||!childId" @click="loadMistakes"><UiIcon name="mistakes"/>孩子错题</button>
  <button :class="{active:tab==='stats'}" :aria-pressed="tab==='stats'" :disabled="busy||!childId" @click="loadStats"><UiIcon name="profile"/>学情</button>
 </div>
 <FamilyLinks v-if="tab==='children'" :profile="profile" @changed="refresh" @expired="emit('expired')"/>
 <template v-else-if="childId&&tab==='tasks'">
  <h2>给{{child?.nickname}}安排练习</h2>
  <form @submit.prevent="compose"><NormalPracticeConfig prefix="parent" :points="points" :selected-ids="normal.selectedIds.value" :total="normal.total.value" :allocations="normal.allocations.value" :disabled="busy" @toggle="id=>{normal.toggle(id);changeConfig();}" @total="value=>{normal.setTotal(value);changeConfig();}" @allocation="(id,value)=>{normal.setAllocation(id,value);changeConfig();}"/><button class="primary" :disabled="busy||!normal.selectedIds.value.length">预览题目 <UiIcon name="arrow"/></button></form>
  <div class="card" v-if="preview"><span class="task-label parent">尚未发送</span><h2>发送给{{child?.nickname}}</h2><p>{{kind(preview.practiceKind)}} · {{preview.grade}}年级 · {{preview.total}}题</p><p class="muted">请核对以下题目，预览15分钟内有效。</p><ol><li v-for="item in preview.items" :key="item.id">{{item.stem}}</li></ol><button class="primary" :disabled="busy" @click="send">确认发送以上题目 <UiIcon name="arrow"/></button></div>
  <div class="section-title"><h2>练习记录</h2><span>最近100条</span></div><p v-if="!tasks.length&&!error" class="empty">还没有练习任务。</p>
  <div v-for="p in tasks" :key="p.id" class="card"><div class="task-card-head"><b>{{p.total}}题练习</b><span class="status-pill">{{state(p.status)}}</span></div><span class="task-label" :class="{parent:p.source==='PARENT'}">{{p.source==='PARENT'?'家长安排':'学生自主'}} · {{kind(p.practiceKind)}}</span><p class="muted">{{p.pointName}}</p><button class="secondary" :disabled="busy" @click="view(p)">查看任务与结果 <UiIcon name="arrow"/></button><button class="text-button" v-if="p.status==='PENDING'&&p.senderId===profile.id" :disabled="busy" @click="withdraw(p)">撤回未开始任务</button></div>
  <div class="card" v-if="detail"><h2>任务详情 · {{state(detail.status)}}</h2><p class="muted">{{kind(detail.practiceKind)}} · {{detail.grade}}年级</p><p v-if="detail.contentError" class="error">内容有误：{{detail.errorNote}}</p><template v-if="detail.accuracy"><div class="metric-grid"><div><strong>{{detail.correct}}/{{detail.total}}</strong><small>答对题数</small></div><div><strong>{{detail.accuracy}}%</strong><small>正确率</small></div></div></template><p v-else>共{{detail.total}}题，已填写{{detail.answered??0}}题</p><details><summary>逐题详情</summary><ol><li v-for="item in detail.items" :key="item.id">{{item.stem}}<template v-if="detail.status==='COMPLETED'"><p>孩子的答案：{{item.raw||'未答'}} · 标准答案：{{item.standardAnswer}}</p><p class="muted">{{item.explanation}}</p></template></li></ol></details></div>
 </template>
 <template v-else-if="childId&&tab==='mistakes'">
  <h2>{{child?.nickname}}的错题</h2><p class="empty" v-if="!mistakes.length&&!error">还没有错题记录。</p>
  <div class="mistake-toolbar"><span>已选 <b>{{review.selectedIds.value.length}}</b> 题</span><button class="primary" :disabled="busy||review.busy.value||!review.selectedIds.value.length" @click="review.prepare()">安排同类练习</button></div>
  <div class="card mistake-card" v-for="item in mistakes" :key="item.id" :class="{chosen:review.selectedIds.value.includes(item.id)}"><label class="mistake-select"><input class="checkbox" type="checkbox" :checked="review.selectedIds.value.includes(item.id)" :disabled="busy||review.busy.value" @change="review.toggle(item.id)" :aria-label="`选择错题 ${item.stem}`">{{review.selectedIds.value.includes(item.id)?'已选中':'选择这题'}}</label><h2>{{item.stem}}</h2><p class="muted">{{item.grade}}年级 · {{item.pointName}}</p><details><summary>查看答案与解析</summary><p>孩子的答案：{{item.raw||'未答'}} · 标准答案：{{item.standardAnswer}}</p><p class="muted">{{item.explanation}}</p></details><button class="secondary" :disabled="busy||review.busy.value" @click="review.prepare([item.id])">练同类题</button><button :disabled="busy" @click="viewMistake(item)">查看历次作答</button></div>
  <div class="card" v-if="mistakeDetail"><h2>孩子历次作答</h2><p v-for="item in mistakeDetail.history" :key="item.id">{{kind(item.practiceKind)}} · {{item.outcome==='CORRECT'?'答对':item.outcome==='WRONG'?'答错':'未答'}} · {{item.raw||'未答'}}</p></div>
  <ReviewPracticePanel v-if="review.options.value" parent :options="review.options.value" :busy="review.busy.value" :grade="review.grade.value" :allocations="review.allocations.value" :allocation-total="review.allocationTotal.value" @grade="review.selectGrade" @allocation="review.setAllocation" @reinforce="previewReinforcement" @close="review.close"/>
 </template>
 <template v-else-if="childId&&tab==='stats'">
  <h2>{{child?.nickname}}的学情</h2>
  <details><summary>选择日期</summary><form @submit.prevent="loadStats"><label for="statsStart">开始日期</label><input id="statsStart" v-model="start" type="date"><label for="statsEnd">结束日期</label><input id="statsEnd" v-model="end" type="date"><p class="muted">留空查看最近7天，按北京时间统计。</p><button class="secondary" :disabled="busy">查询学情</button></form></details>
  <div v-if="stats" class="card"><p class="muted">{{stats.start}} 至 {{stats.end}}</p><p v-if="!stats.completed">还没有已完成的练习。</p><template v-else><div class="metric-grid"><div><strong>{{stats.completed}}</strong><small>完成次数</small></div><div><strong>{{stats.total}}</strong><small>练习题数</small></div><div><strong>{{stats.accuracy}}%</strong><small>正确率</small></div></div><p class="muted">记录用时 {{Math.floor(stats.elapsedMs/1000)}}秒</p><details><summary>每日表现</summary><p v-for="day in stats.days" :key="day.day">{{day.day}} · {{day.total}}题 · 答对{{day.correct}}题</p></details><details><summary>知识点表现</summary><p v-for="p in stats.points" :key="p.pointId+':'+p.grade+':'+p.pointName">{{p.grade}}年级 · {{p.pointName}} · {{p.total}}题 · 答对{{p.correct}}题</p></details></template></div>
 </template>
 <p v-else class="empty">请先选择或关联孩子。</p>
</section>
</template>
