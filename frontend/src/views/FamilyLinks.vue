<script setup lang="ts">
import {confirmAction} from '../composables/confirmation';
import {onMounted,ref} from 'vue';import {apiGet,apiPost,ApiError,type UserProfile} from '../api/client';
export interface FamilyLink {id:string;parentId:string;studentId:string;state:string;loginName:string;nickname:string;grade?:number}
const props=defineProps<{profile:UserProfile}>(),emit=defineEmits<{changed:[];expired:[]}>();const links=ref<FamilyLink[]>([]),studentName=ref(''),busy=ref(false),error=ref('');
const state=(s:string)=>({PENDING:'等待学生确认',ACTIVE:'已关联',REJECTED:'已拒绝',UNBOUND:'已解除'}[s]??s);
async function run(f:()=>Promise<void>){if(busy.value)return;busy.value=true;error.value='';try{await f();}catch(e){error.value=e instanceof Error?e.message:'操作失败';if(e instanceof ApiError&&e.status===401)emit('expired');}finally{busy.value=false;}}
async function load(){links.value=await apiGet<FamilyLink[]>('/family/links');}
const refresh=()=>run(load);
const apply=()=>run(async()=>{await apiPost('/family/requests',{studentLoginName:studentName.value.trim()});studentName.value='';await load();emit('changed');});
const respond=(link:FamilyLink,accept:boolean)=>run(async()=>{await apiPost(`/family/links/${link.id}/respond`,{accept});await load();emit('changed');});
const unbind=(link:FamilyLink)=>run(async()=>{if(!await confirmAction(`确认解除与“${link.nickname}”的关联？学生已有任务和历史仍保留。`))return;await apiPost(`/family/links/${link.id}/unbind`,{confirm:true});await load();emit('changed');});
onMounted(refresh);
</script>
<template><section><div class="section-title"><h2>我的家庭</h2><button :disabled="busy" @click="refresh">刷新关联</button></div><p v-if="error" role="alert" class="error">{{error}}</p><form v-if="profile.role==='PARENT'" @submit.prevent="apply"><label for="studentLoginName">孩子账号</label><input id="studentLoginName" v-model="studentName" required maxlength="64" placeholder="请向孩子确认账号"><button class="primary" :disabled="busy">申请关联</button><p class="muted">孩子登录同意后，即可安排练习。</p></form><p class="empty" v-if="!links.length&&!error">还没有关联的家人。</p><div v-for="link in links" :key="link.id" class="card"><h2>{{link.nickname}}</h2><p class="muted">{{link.loginName}} · {{state(link.state)}}</p><div v-if="profile.role==='STUDENT'&&link.state==='PENDING'" class="two"><button class="primary" :disabled="busy" @click="respond(link,true)">同意关联</button><button class="secondary" :disabled="busy" @click="respond(link,false)">拒绝申请</button></div><button v-if="link.state==='ACTIVE'" class="text-button" :disabled="busy" @click="unbind(link)">解除关联</button><button v-if="profile.role==='PARENT'&&['REJECTED','UNBOUND'].includes(link.state)" :disabled="busy" @click="studentName=link.loginName">重新填写申请</button></div></section></template>
