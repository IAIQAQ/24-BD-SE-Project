import {ref} from 'vue';
import type {Point,NormalAllocation} from '../api/contracts';

export function useNormalComposition(){
 const total=ref(30),selectedIds=ref<string[]>([]),allocations=ref<Record<string,number>>({});
 let attempt:{signature:string;operationKey:string}|null=null;
 function distribute(){
  const n=selectedIds.value.length;
  allocations.value=Object.fromEntries(selectedIds.value.map((id,i)=>[id,Number.isInteger(total.value)&&total.value>=n?Math.floor(total.value/n)+(i<total.value%n?1:0):0]));attempt=null;
 }
 function reset(points:Point[]){total.value=30;selectedIds.value=points.length?[points[0]!.id]:[];distribute();}
 function toggle(id:string){selectedIds.value=selectedIds.value.includes(id)?selectedIds.value.filter(p=>p!==id):[...selectedIds.value,id];distribute();}
 function setTotal(value:number){if(Object.is(total.value,value))return;total.value=value;distribute();}
 function setAllocation(id:string,value:number){if(Object.is(allocations.value[id],value))return;allocations.value={...allocations.value,[id]:value};attempt=null;}
 function request(owner:string,points:Point[]){
  if(!owner||!Number.isInteger(total.value)||total.value<1||total.value>100)throw new Error('总题量需为1—100的整数');
  const rows:NormalAllocation[]=selectedIds.value.map(pointId=>({pointId,total:allocations.value[pointId]??0}));
  if(!rows.length||rows.some(row=>!Number.isInteger(row.total)||row.total<1)||rows.reduce((n,row)=>n+row.total,0)!==total.value)throw new Error('请选择知识点，每项至少1整数题，分配合计须等于总题量');
  const shortages:string[]=[];
  for(const row of rows){const point=points.find(p=>p.id===row.pointId);if(!point)throw new Error('知识点已变化，请重新选择');if(row.total>point.available)shortages.push(`${point.name}需要 ${row.total} 题，当前可用 ${point.available} 题`);}
  if(shortages.length)throw new Error(shortages.join('；'));
  rows.sort((a,b)=>a.pointId.length-b.pointId.length||a.pointId.localeCompare(b.pointId));
  const signature=JSON.stringify([owner,rows]);if(attempt?.signature!==signature)attempt={signature,operationKey:crypto.randomUUID()};
  return {allocations:rows,operationKey:attempt.operationKey};
 }
 function isCurrent(key:string){return attempt?.operationKey===key;}
 function finish(){attempt=null;}
 return {total,selectedIds,allocations,reset,toggle,setTotal,setAllocation,request,isCurrent,finish};
}
