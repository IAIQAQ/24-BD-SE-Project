import {computed,ref} from 'vue';
import {practiceApi} from '../api/client';
import type {ReviewOptions,Summary} from '../api/contracts';
import {confirmAction} from './confirmation';

export function useReviewSelection(studentId:()=>string,parent=false){
 const selectedIds=ref<string[]>([]),options=ref<ReviewOptions|null>(null),busy=ref(false),error=ref('');
 const grade=ref(0),allocations=ref<Record<string,number>>({});
 const group=computed(()=>options.value?.groups?.find(g=>g.grade===grade.value));
 const allocationTotal=computed(()=>Object.values(allocations.value).reduce((a,b)=>a+b,0));
 let generation=0,attempt:string|null=null,reinforceAttempt:string|null=null;
 function clear(){generation++;selectedIds.value=[];options.value=null;error.value='';attempt=null;reinforceAttempt=null;allocations.value={};grade.value=0;}
 function change(ids:string[]){generation++;selectedIds.value=ids;options.value=null;attempt=null;reinforceAttempt=null;allocations.value={};error.value='';}
 function selectGrade(value:number){if(busy.value||grade.value===value)return;configureGrade(value);}
 function configureGrade(value:number){generation++;grade.value=value;reinforceAttempt=null;const points=options.value?.groups?.find(g=>g.grade===value)?.points??[];const total=Math.max(30,points.length);allocations.value=Object.fromEntries(points.map((p,i)=>[p.pointId,Math.floor(total/points.length)+(i<total%points.length?1:0)]));}
 function setAllocation(pointId:string,total:number){if(busy.value||!(pointId in allocations.value)||allocations.value[pointId]===total)return;generation++;allocations.value={...allocations.value,[pointId]:total};reinforceAttempt=null;error.value='';}
 function toggle(id:string){if(busy.value)return;const ids=selectedIds.value;if(ids.includes(id))change(ids.filter(v=>v!==id));else if(ids.length<100)change([...ids,id]);else error.value='每次最多选择100条错题，请分批练习';}
 async function prepare(ids?:string[]){
  if(busy.value)return;if(ids&&JSON.stringify([...ids].sort())!==JSON.stringify([...selectedIds.value].sort()))change([...ids]);if(!selectedIds.value.length){error.value='请先选择错题';return;}
  busy.value=true;error.value='';const token=generation,student=studentId();
  try{const data=await (parent?practiceApi.parentReviewOptions(student,[...selectedIds.value]):practiceApi.reviewOptions(student,[...selectedIds.value]));if(token===generation&&student===studentId()){options.value=data;const current=data.groups.find(g=>g.grade===grade.value);if(!current||JSON.stringify(current.points.map(p=>p.pointId).sort())!==JSON.stringify(Object.keys(allocations.value).sort()))configureGrade(current?.grade??data.groups[0]?.grade??0);}}
  catch(e){if(token===generation)error.value=e instanceof Error?e.message:'无法核对所选错题';}finally{busy.value=false;}
 }
 async function replay():Promise<Summary|null>{
  if(parent||busy.value||!options.value?.replay.allowed)return null;
  busy.value=true;error.value='';const token=generation,student=studentId(),ids=[...selectedIds.value];
  try{
   if(!await confirmAction(`按所选历史内容新建 ${options.value.replay.total} 题原题重练？\n旧错误和旧成绩仍保留，本次从空白答案开始。`))return null;
   if(token!==generation||student!==studentId())return null;
   attempt??=crypto.randomUUID();const record=await practiceApi.replay(ids,attempt);
   return token===generation&&student===studentId()?record:null;
  }catch(e){if(token===generation)error.value=e instanceof Error?e.message:'创建失败，请重试核对';return null;}
  finally{busy.value=false;}
 }
 async function reinforce():Promise<Summary|null>{
  if(busy.value||!group.value)return null;const points=group.value.points;
  if(points.some(p=>!Number.isInteger(allocations.value[p.pointId])||allocations.value[p.pointId]<1)||allocationTotal.value>100||allocationTotal.value<1){error.value='每个知识点至少1题，总题量需为1—100的整数';return null;}
  const insufficient=points.find(p=>allocations.value[p.pointId]>p.available);if(insufficient){error.value=`${insufficient.pointName}需要 ${allocations.value[insufficient.pointId]} 题，当前可用 ${insufficient.available} 题，请调整`;return null;}
  busy.value=true;error.value='';const token=generation,student=studentId(),ids=[...group.value.seedItemIds],distribution=points.map(p=>({pointId:p.pointId,total:allocations.value[p.pointId]}));
  try{if(!parent&&!await confirmAction(`新建 ${grade.value} 年级同类强化，共 ${allocationTotal.value} 题？\n本次从当前可用题库抽题，保留原错题与历史。`))return null;if(token!==generation||student!==studentId())return null;reinforceAttempt??=crypto.randomUUID();const record=await (parent?practiceApi.parentReinforcePreview(student,ids,distribution,reinforceAttempt):practiceApi.reinforce(ids,distribution,reinforceAttempt));return token===generation&&student===studentId()?record:null;}
  catch(e){if(token===generation)error.value=e instanceof Error?e.message:'强化创建失败，请重试';return null;}finally{busy.value=false;}
 }
 return {selectedIds,options,busy,error,grade,allocations,allocationTotal,group,toggle,prepare,replay,reinforce,selectGrade,setAllocation,clear,close:()=>{options.value=null;}};
}
