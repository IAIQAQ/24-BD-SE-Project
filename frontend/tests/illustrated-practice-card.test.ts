import {nextTick,reactive} from 'vue';
import {expect,test} from 'vitest';
import IllustratedPracticeCard from '../src/views/IllustratedPracticeCard.vue';
import source from '../src/views/IllustratedPracticeCard.vue?raw';
import {renderUi} from './render-ui';
import type {Summary} from '../src/api/contracts';
const row:Summary={id:'practice-7',pointId:'1',total:100,status:'IN_PROGRESS',version:1,currentSeq:2,elapsedMs:1000,timingComplete:true,submittedAt:'',pointName:'整十、整百数加法（1000以内）、两三位数除以一位数（整除）',source:'PARENT',practiceKind:'REINFORCE'};

test('all practice states preserve identity, status, full point names and truthful next actions',async()=>{
 let opened='';const props=reactive({record:{...row},busy:false,onOpen:(id:string)=>opened=id});const s=renderUi(IllustratedPracticeCard,source,()=>props);
 try{
  for(const [status,action] of [['PENDING','开始'],['IN_PROGRESS','继续'],['COMPLETED','看结果'],['WITHDRAWN','查看'],['VOID','查看']]){
   props.record.status=status!;await nextTick();const button=s.nodes.find(n=>n.tag==='button'&&n.parent)!;
   expect(button).toBeDefined();expect(s.text(button)).toContain(action);button.props.onClick();expect(opened).toBe(row.id);
   expect(s.nodes.some(n=>n.parent&&s.text(n).includes(row.pointName!))).toBe(true);
   expect(s.text(button)).toContain('家长安排');expect(s.text(button)).toContain('同类强化');
   expect(s.text(button)).not.toContain('2/100');
  }
 }finally{s.app.unmount();}
});
test('continue card uses viewed question number; art failure and busy keep structure',async()=>{
 const props=reactive({record:{...row},busy:false});const s=renderUi(IllustratedPracticeCard,source,()=>props);
 try{
  const button=s.nodes.find(n=>n.tag==='button'&&n.parent)!;expect(button).toBeDefined();expect(s.text(button)).toContain('上次看到第2题');
  const image=s.nodes.find(n=>n.tag==='img'&&n.parent)!;expect(image).toBeDefined();image.props.onError();await nextTick();expect(s.nodes.some(n=>n.tag==='img'&&n.parent)).toBe(false);
  props.busy=true;await nextTick();expect(button.props.disabled).toBe(true);
  props.record.status='PENDING';await nextTick();expect(s.text(button)).not.toContain('已保存');
 }finally{s.app.unmount();}
});
