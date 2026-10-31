import {nextTick,reactive} from 'vue';
import {expect,test} from 'vitest';
import SproutCompanion from '../src/views/SproutCompanion.vue';
import source from '../src/views/SproutCompanion.vue?raw';
import {renderUi} from './render-ui';

test('welcome and empty reuse the local decorative asset; complete uses its own asset',async()=>{
 const props=reactive({scene:'welcome' as 'welcome'|'complete'|'empty'});const s=renderUi(SproutCompanion,source,()=>props);
 try{
  const image=()=>s.nodes.find(n=>n.tag==='img'&&n.parent)!;
  expect(image()).toBeDefined();expect(image().props.src).toContain('welcome.webp');expect(image().props.alt).toBe('');
  expect(image().props.width).toBeTruthy();expect(image().props.height).toBeTruthy();
  const welcome=image().props.src;props.scene='empty';await nextTick();expect(image().props.src).toBe(welcome);
  props.scene='complete';await nextTick();expect(image().props.src).toContain('complete.webp');
 }finally{s.app.unmount();}
});

test('an image error removes the broken image but preserves readable encouragement and scene changes recover',async()=>{
 const props=reactive({scene:'welcome' as 'welcome'|'complete'|'empty'});const s=renderUi(SproutCompanion,source,()=>props);
 try{
  const image=s.nodes.find(n=>n.tag==='img'&&n.parent);expect(image).toBeDefined();
  image!.props.onError();await nextTick();expect(s.nodes.some(n=>n.tag==='img'&&n.parent)).toBe(false);
  expect(s.nodes.some(n=>n.tag==='p'&&n.parent&&s.text(n).trim().length>0)).toBe(true);
  props.scene='complete';await nextTick();expect(s.nodes.find(n=>n.tag==='img'&&n.parent)?.props.src).toContain('complete.webp');
 }finally{s.app.unmount();}
});

test('celebration is opt-in, decorative and leaves encouragement visible',()=>{
 const s=renderUi(SproutCompanion,source,()=>({scene:'complete',celebrating:true}));try{const root=s.nodes.find(n=>n.tag==='div')!;expect(root.props.class).toContain('is-celebrating');expect(s.text(root)).toContain('认真完成，真棒！');expect(s.nodes.filter(n=>n.props['aria-hidden']==='true').length).toBeGreaterThan(1);}finally{s.app.unmount();}
});
