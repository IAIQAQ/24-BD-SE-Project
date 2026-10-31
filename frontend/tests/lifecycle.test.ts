import {afterEach,beforeEach,expect,test,vi} from 'vitest';
import {createRenderer} from 'vue';
import {useLearningApp} from '../src/composables/useLearningApp';
import {ApiError,practiceApi,me,login} from '../src/api/client';
import {confirmation,finishConfirmation} from '../src/composables/confirmation';
vi.mock('../src/api/client',()=>({ApiError:class extends Error{constructor(public status:number,message:string,public code=''){super(message);}},me:vi.fn(),login:vi.fn(),logout:vi.fn(),practiceApi:{points:vi.fn(),list:vi.fn(),create:vi.fn(),createNormal:vi.fn(),open:vi.fn(),draft:vi.fn(),save:vi.fn(),segment:vi.fn()}}));
let documentStub:EventTarget&{hidden:boolean};let unmount:()=>void;
const flush=async()=>{for(let i=0;i<30;i++)await Promise.resolve();};
async function setup(){
  let version=1;
  const row={id:'1',pointId:'1',total:1,status:'IN_PROGRESS',version:1,currentSeq:1,elapsedMs:0,timingComplete:true,submittedAt:'',items:[{id:'1',seq:1,stem:'1 + 1 =',raw:''}]};
  vi.mocked(me).mockRejectedValue(new ApiError(401,'请先登录'));
  vi.mocked(login).mockResolvedValue({id:'1',loginName:'student_a',nickname:'小明',role:'STUDENT',grade:1});
  vi.mocked(practiceApi.points).mockResolvedValue([{id:'1',name:'加法',grade:1,semester:'上',available:120}]);
  vi.mocked(practiceApi.list).mockResolvedValue({items:[],total:0,page:1,size:20});
  vi.mocked(practiceApi.create).mockResolvedValue(row);vi.mocked(practiceApi.createNormal).mockResolvedValue(row);
  vi.mocked(practiceApi.open).mockResolvedValue({...row,editorSessionId:'editor'});
  vi.mocked(practiceApi.draft).mockImplementation(async()=>({...row,version}));
  vi.mocked(practiceApi.save).mockImplementation(async()=>({version:++version}));
  vi.mocked(practiceApi.segment).mockResolvedValue({elapsedMs:0});
  const renderer=createRenderer<any,any>({patchProp(){},insert(){},remove(){},createElement:()=>({}),createText:()=>({}),createComment:()=>({}),setText(){},setElementText(){},parentNode:()=>null,nextSibling:()=>null});
  let state!:ReturnType<typeof useLearningApp>;
  const mounted=renderer.createApp({setup(){state=useLearningApp();return()=>null;}});mounted.mount({});unmount=()=>mounted.unmount();await flush();
  await state.signIn();await state.create();return state;
}
beforeEach(()=>{vi.useFakeTimers();vi.clearAllMocks();documentStub=Object.assign(new EventTarget(),{hidden:false});vi.stubGlobal('document',documentStub);vi.stubGlobal('window',Object.assign(new EventTarget(),{confirm:()=>true}));vi.stubGlobal('sessionStorage',{getItem:()=>null,setItem(){},removeItem(){}});});
afterEach(()=>{unmount?.();vi.useRealTimers();vi.unstubAllGlobals();});
test('returning online retries a failed background save and resumes visible timing',async()=>{
  const app=await setup();app.input('2');vi.mocked(practiceApi.save).mockRejectedValueOnce(new Error('offline'));
  documentStub.hidden=true;documentStub.dispatchEvent(new Event('visibilitychange'));await flush();
  documentStub.hidden=false;documentStub.dispatchEvent(new Event('visibilitychange'));await flush();
  await vi.advanceTimersByTimeAsync(2200);
  expect(app.displayTime.value).toBe('0:02');expect(practiceApi.save).toHaveBeenCalledTimes(2);
});
test('background and submit preparation share one closing segment',async()=>{
  const app=await setup();app.input('2');let release!:(value:{elapsedMs:number})=>void;
  vi.mocked(practiceApi.segment).mockImplementation(()=>new Promise(resolve=>release=resolve));
  documentStub.hidden=true;documentStub.dispatchEvent(new Event('visibilitychange'));await flush();
  const submitting=app.confirmSubmit();await flush();release({elapsedMs:0});await submitting;await flush();
  expect(app.error.value).toBe('');expect(app.page.value).toBe('confirm');expect(practiceApi.segment).toHaveBeenCalledTimes(1);
});
test('cancelling the back confirmation keeps the unsaved answer on screen',async()=>{
  const app=await setup();app.input('2');const leaving=app.back();await flush();
  expect(confirmation.value).toBe('保存当前答案并返回首页？');
  finishConfirmation(false);await leaving;
  expect(app.page.value).toBe('practice');expect(app.activeItem.value?.raw).toBe('2');
});

test('normal creation retries the same config after closing and reopening without duplicating',async()=>{
 const app=await setup();app.normal.setTotal(3);vi.mocked(practiceApi.createNormal).mockRejectedValueOnce(new Error('response lost')).mockResolvedValue({id:'1'} as any);
 await app.create();await app.home();app.configure();expect(app.normal.total.value).toBe(3);await app.create();const calls=vi.mocked(practiceApi.createNormal).mock.calls;expect(calls.at(-1)).toEqual(calls.at(-2));
});
test('login waits for the initial cookie restoration to settle',async()=>{
 let reject!:(reason:Error)=>void;vi.mocked(me).mockImplementation(()=>new Promise((_resolve,r)=>reject=r));
 const renderer=createRenderer<any,any>({patchProp(){},insert(){},remove(){},createElement:()=>({}),createText:()=>({}),createComment:()=>({}),setText(){},setElementText(){},parentNode:()=>null,nextSibling:()=>null});
 let state!:ReturnType<typeof useLearningApp>;const mounted=renderer.createApp({setup(){state=useLearningApp();return()=>null;}});mounted.mount({});unmount=()=>mounted.unmount();await flush();await state.signIn();expect(login).not.toHaveBeenCalled();expect(state.busy.value).toBe(true);reject(new ApiError(401,'expired'));await flush();expect(state.busy.value).toBe(false);
});