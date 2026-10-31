import {reactive,nextTick} from 'vue';
import {expect,test} from 'vitest';
import GardenHero from '../src/views/GardenHero.vue';
import source from '../src/views/GardenHero.vue?raw';
import {renderUi} from './render-ui';

test('garden start uses the existing event and respects busy state',async()=>{
 let starts=0;const props=reactive({busy:false,onStart:()=>starts++});const s=renderUi(GardenHero,source,()=>props);
 try{const start=s.button('开始练习');expect(start).toBeDefined();start!.props.onClick();expect(starts).toBe(1);props.busy=true;await nextTick();expect(start!.props.disabled).toBe(true);}finally{s.app.unmount();}
});
test('background error keeps the real start button usable (mascot fallback covered by sprout-welcome tests)',async()=>{
 let starts=0;const s=renderUi(GardenHero,source,()=>({busy:false,onStart:()=>starts++}));
 try{
  const images=s.nodes.filter(n=>n.tag==='img'&&n.parent);expect(images.length).toBe(1);
  for(const image of images){expect(image.props.alt).toBe('');image.props.onError();}await nextTick();
  expect(s.nodes.some(n=>n.tag==='img'&&n.parent)).toBe(false);s.button('开始练习')!.props.onClick();expect(starts).toBe(1);
 }finally{s.app.unmount();}
});
