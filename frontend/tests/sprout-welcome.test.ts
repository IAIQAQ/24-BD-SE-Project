import {reactive,nextTick} from 'vue';
import {expect,test,vi} from 'vitest';
import SproutWelcome from '../src/views/SproutWelcome.vue';
import source from '../src/views/SproutWelcome.vue?raw';
import {renderUi} from './render-ui';
test('only the leaf layer moves once and inactive cancels without replay',async()=>{
 const props=reactive({active:true});const s=renderUi(SproutWelcome,source,()=>props);
 try{const root=s.nodes.find(n=>n.tag==='span')!;const images=s.nodes.filter(n=>n.tag==='img'&&n.parent);expect(images).toHaveLength(2);
 images[0].props.onLoad();await nextTick();expect(root.props.class).toContain('leaf-welcome-active');
 props.active=false;await nextTick();expect(root.props.class).not.toContain('leaf-welcome-active');props.active=true;await nextTick();expect(root.props.class).not.toContain('leaf-welcome-active');
 }finally{s.app.unmount();}
});
test('animation end stays still and image failure has a fallback',async()=>{
 const s=renderUi(SproutWelcome,source,()=>({active:true}));
 try{const root=s.nodes.find(n=>n.tag==='span')!;const images=s.nodes.filter(n=>n.tag==='img'&&n.parent);images[0].props.onLoad();await nextTick();images[1].props.onAnimationend();await nextTick();expect(root.props.class).not.toContain('leaf-welcome-active');images[0].props.onError();await nextTick();expect(s.nodes.filter(n=>n.tag==='img'&&n.parent)).toHaveLength(0);expect(s.text(root)).toContain('芽');}finally{s.app.unmount();}
});

test('hiding cancels welcome permanently and removes the visibility listener on unmount',async()=>{
 let listener:(()=>void)|undefined;
 const doc={hidden:false,addEventListener:vi.fn((_name:string,fn:()=>void)=>{listener=fn;}),removeEventListener:vi.fn()};vi.stubGlobal('document',doc);
 const s=renderUi(SproutWelcome,source,()=>({active:true}));
 try{const root=s.nodes.find(n=>n.tag==='span')!;s.nodes.find(n=>n.tag==='img')!.props.onLoad();await nextTick();expect(root.props.class).toContain('leaf-welcome-active');doc.hidden=true;listener!();await nextTick();expect(root.props.class).not.toContain('leaf-welcome-active');doc.hidden=false;listener!();await nextTick();expect(root.props.class).not.toContain('leaf-welcome-active');}finally{s.app.unmount();expect(doc.removeEventListener).toHaveBeenCalledWith('visibilitychange',listener);vi.unstubAllGlobals();}
});
