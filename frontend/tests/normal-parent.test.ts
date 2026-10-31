import {beforeEach,afterEach,expect,test,vi} from 'vitest';
import {createRenderer,ssrContextKey} from 'vue';
import ParentWorkspace from '../src/views/ParentWorkspace.vue';
import {apiGet,apiPost,practiceApi} from '../src/api/client';
vi.mock('../src/api/client',()=>({ApiError:class extends Error{},apiGet:vi.fn(),apiPost:vi.fn(),practiceApi:{points:vi.fn(),parentReviewOptions:vi.fn()}}));
const flush=async()=>{for(let i=0;i<50;i++)await Promise.resolve();};
const points=[{id:'1',name:'A',grade:1,semester:'上',available:120},{id:'2',name:'B',grade:1,semester:'上',available:120}];
let mounted:any;
async function setup(){
 vi.mocked(apiGet).mockImplementation(async(path:string)=>path==='/family/links'?[{id:'l1',studentId:'c1',nickname:'甲',grade:1,state:'ACTIVE'},{id:'l2',studentId:'c2',nickname:'乙',grade:1,state:'ACTIVE'}]:{items:[]} as any);
 vi.mocked(practiceApi.points).mockResolvedValue(points);
 const renderer=createRenderer<any,any>({patchProp(){},insert(){},remove(){},createElement:()=>({}),createText:()=>({}),createComment:()=>({}),setText(){},setElementText(){},parentNode:()=>null,nextSibling:()=>null});
 mounted=renderer.createApp({...ParentWorkspace,render:()=>null,ssrRender:undefined},{profile:{id:'p',role:'PARENT',nickname:'家长',grade:null,loginName:'p'}});mounted.provide(ssrContextKey,{modules:new Set()});const vm=mounted.mount({});await flush();return (vm as any).$ .setupState;
}
beforeEach(()=>vi.clearAllMocks());afterEach(()=>mounted?.unmount());
test('parent normal preview retries with same key, and refresh retains uncertain configuration',async()=>{
 const s=await setup();s.normal.setTotal(5);s.normal.toggle('2');s.normal.setAllocation('1',2);s.normal.setAllocation('2',3);vi.mocked(apiPost).mockRejectedValueOnce(new Error('lost response')).mockResolvedValue({id:'preview',total:5});await s.compose();await s.refresh();await s.compose();expect(vi.mocked(apiPost).mock.calls[1]).toEqual(vi.mocked(apiPost).mock.calls[0]);expect(s.preview.id).toBe('preview');
});
test('late normal preview cannot enter another child workspace',async()=>{
 const s=await setup();let finish!:(v:any)=>void;vi.mocked(apiPost).mockImplementation(()=>new Promise(r=>finish=r));const pending=s.compose();await flush();s.childId='c2';await flush();finish({id:'old'});await pending;expect(s.preview).toBeNull();
});
test('late normal preview cannot replace a changed configuration',async()=>{
 const s=await setup();let finish!:(v:any)=>void;vi.mocked(apiPost).mockImplementation(()=>new Promise(r=>finish=r));const pending=s.compose();await flush();s.normal.setTotal(5);s.changeConfig();finish({id:'old'});await pending;expect(s.preview).toBeNull();
});