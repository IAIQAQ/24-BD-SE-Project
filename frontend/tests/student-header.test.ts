import {expect,test} from 'vitest';
import StudentHeader from '../src/views/StudentHeader.vue';
import source from '../src/views/StudentHeader.vue?raw';
import {renderUi} from './render-ui';

test('header displays the actual grade and avatar opens the existing profile route',()=>{
 let opened=0;const s=renderUi(StudentHeader,source,()=>({nickname:'小红',grade:3,onOpenProfile:()=>opened++}));
 try{
  const root=s.nodes.find(n=>n.tag==='header')!;expect(s.text(root)).toContain('三年级');
  const avatar=s.nodes.find(n=>n.tag==='button'&&n.props['aria-label']==='我的资料')!;expect(avatar).toBeDefined();avatar.props.onClick();expect(opened).toBe(1);
  expect(s.nodes.filter(n=>n.tag==='select'&&n.parent)).toHaveLength(0);
 }finally{s.app.unmount();}
});
