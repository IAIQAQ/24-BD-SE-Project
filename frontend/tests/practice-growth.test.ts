import {nextTick,reactive} from 'vue';
import {expect,test,vi} from 'vitest';
import PracticeGrowth from '../src/views/PracticeGrowth.vue';
import source from '../src/views/PracticeGrowth.vue?raw';
import {renderUi} from './render-ui';
test('truthful stages for zero/one/five/100 questions; restoration starts static',()=>{
 for(const [answered,total,stage] of [[0,5,1],[1,5,1],[2,5,1],[3,5,2],[4,5,2],[5,5,3],[0,1,1],[1,1,3],[49,100,1],[50,100,2],[100,100,3]]){
  const s=renderUi(PracticeGrowth,source,()=>({answered,total,active:true}));try{const root=s.nodes.find(n=>n.tag==='div')!;expect(root.props['data-stage']).toBe(stage);expect(s.text(root)).toContain(`已填写 ${answered}/${total} 题`);expect(root.props.class).not.toContain('growth-changing');expect(s.text(root)).not.toMatch(/答对|提交成功|交卷成功/);}finally{s.app.unmount();}
 }
});
test('no division or progress shown before a valid total',()=>{const s=renderUi(PracticeGrowth,source,()=>({answered:0,total:0,active:true}));try{expect(s.nodes.filter(n=>n.tag==='progress'&&n.parent)).toHaveLength(0);expect(s.nodes.filter(n=>n.tag==='div'&&n.parent)).toHaveLength(0);}finally{s.app.unmount();}});
test('only stage changes animate; clearing reverses stage; inactive cancels and unmount clears timers',async()=>{
 vi.useFakeTimers();const props=reactive({answered:0,total:5,active:true});const s=renderUi(PracticeGrowth,source,()=>props);
 try{const root=s.nodes.find(n=>n.tag==='div')!;props.answered=1;await nextTick();expect(root.props.class).not.toContain('growth-changing');props.answered=3;await nextTick();expect(root.props.class).toContain('growth-changing');vi.advanceTimersByTime(400);await nextTick();expect(root.props.class).not.toContain('growth-changing');props.answered=5;await nextTick();expect(root.props['data-stage']).toBe(3);props.active=false;await nextTick();expect(root.props.class).not.toContain('growth-changing');props.active=true;await nextTick();expect(root.props.class).not.toContain('growth-changing');props.answered=2;await nextTick();expect(root.props['data-stage']).toBe(1);}finally{s.app.unmount();expect(vi.getTimerCount()).toBe(0);vi.useRealTimers();}
});

test('failed artwork keeps the real count and progress readable',async()=>{
 const s=renderUi(PracticeGrowth,source,()=>({answered:3,total:5,active:true}));try{s.nodes.find(n=>n.tag==='img')!.props.onError();await nextTick();expect(s.nodes.filter(n=>n.tag==='img'&&n.parent)).toHaveLength(0);expect(s.nodes.find(n=>n.tag==='progress')!.props.value).toBe(3);expect(s.nodes.some(n=>n.parent&&s.text(n).includes('已填写 3/5 题'))).toBe(true);}finally{s.app.unmount();}
});

test('three stages use distinct complete illustrations and visible names, including clearing back',async()=>{
 const props=reactive({answered:1,total:5,active:true});const s=renderUi(PracticeGrowth,source,()=>props);
 try{const root=s.nodes.find(n=>n.tag==='div')!;const seen=[];
 for(const [n,label,file] of [[1,'幼苗','seedling'],[3,'舒展','unfolding'],[5,'长成','grown']] as const){props.answered=n;await nextTick();const images=s.nodes.filter(n=>n.tag==='img'&&n.parent);expect(images).toHaveLength(1);expect(images[0].props.src).toContain(file);expect(s.text(root)).toContain(label);seen.push(images[0].props.src);}
 expect(new Set(seen).size).toBe(3);props.answered=2;await nextTick();expect(s.text(root)).toContain('幼苗');expect(s.nodes.find(n=>n.tag==='img'&&n.parent)!.props.src).toContain('seedling');
 }finally{s.app.unmount();}
});
test('failed illustration can recover when moving to a different stage',async()=>{
 const props=reactive({answered:1,total:5,active:true});const s=renderUi(PracticeGrowth,source,()=>props);
 try{s.nodes.find(n=>n.tag==='img')!.props.onError();await nextTick();props.answered=3;await nextTick();expect(s.nodes.find(n=>n.tag==='img'&&n.parent)!.props.src).toContain('unfolding');}finally{s.app.unmount();}
});
