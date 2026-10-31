import {nextTick,reactive} from 'vue';
import {renderUi} from './render-ui';
import source from '../src/views/NormalPracticeConfig.vue?raw';
import {expect,test} from 'vitest';
import NormalPracticeConfig from '../src/views/NormalPracticeConfig.vue';
import {useNormalComposition} from '../src/composables/useNormalComposition';

function mount(){
 const points=[{id:'1',name:'10以内加法',grade:1,semester:'上',available:36},{id:'2',name:'20以内减法',grade:1,semester:'上',available:30}];
 const normal=useNormalComposition();normal.reset(points);normal.toggle('2');
 const props=reactive({points,prefix:'student',disabled:false});
 const host=renderUi(NormalPracticeConfig,source,()=>({...props,total:normal.total.value,selectedIds:normal.selectedIds.value,allocations:normal.allocations.value,onTotal:normal.setTotal,onToggle:normal.toggle,onAllocation:normal.setAllocation}));
 return {...host,normal,props};
}

test('quick quantities emit through existing composition, distribute exactly and keep same-value retry keys',async()=>{
 const s=mount();
 try{
  const five=s.button('5题');expect(five).toBeDefined();expect(five!.props.type).toBe('button');five!.props.onClick();await nextTick();
  expect(s.normal.total.value).toBe(5);expect(s.normal.allocations.value).toEqual({'1':3,'2':2});
  const points=s.props.points,key=s.normal.request('child',points).operationKey;
  s.button('5题')!.props.onClick();await nextTick();expect(s.normal.request('child',points).operationKey).toBe(key);
  s.button('10题')!.props.onClick();await nextTick();expect(s.normal.allocations.value).toEqual({'1':5,'2':5});
  s.button('20题')!.props.onClick();await nextTick();expect(s.normal.total.value).toBe(20);
 }finally{s.app.unmount();}
});

test('custom quantity disclosure keeps default30 and values when closed and reopened',async()=>{
 const s=mount();
 try{
  expect(s.button('自定义')).toBeDefined();expect(s.normal.total.value).toBe(30);
  expect(s.nodes.some(n=>n.tag==='input'&&n.parent&&n.props.id==='total')).toBe(true);
  s.button('5题')!.props.onClick();await nextTick();s.button('自定义')!.props.onClick();await nextTick();
  const input=s.nodes.find(n=>n.tag==='input'&&n.parent&&n.props.id==='total')!;
  expect(input).toBeDefined();input.props.onInput({target:{value:'7'}});await nextTick();expect(s.normal.total.value).toBe(7);
  s.button('自定义')!.props.onClick();await nextTick();s.button('自定义')!.props.onClick();await nextTick();expect(s.normal.total.value).toBe(7);
  expect(s.normal.allocations.value).toEqual({'1':4,'2':3});
 }finally{s.app.unmount();}
});

test('invalid allocation reveals a collapsed panel before browser focuses its input',()=>{
 const s=mount();
 try{
  const panel=s.nodes.find(n=>n.tag==='details'&&n.props.class==='allocation-details')!;
  const target={open:false};expect(panel.props.onInvalidCapture).toBeTypeOf('function');
  panel.props.onInvalidCapture({currentTarget:target});expect(target.open).toBe(true);
 }finally{s.app.unmount();}
});
