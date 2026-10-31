import {afterEach,beforeEach,expect,test,vi} from 'vitest';
import {createRenderer} from 'vue';
import {useLearningApp} from '../src/composables/useLearningApp';
import {ApiError,practiceApi,me,login} from '../src/api/client';
import {confirmation,finishConfirmation} from '../src/composables/confirmation';
const native=vi.hoisted(()=>({enabled:false,listeners:new Map<string,(value:any)=>void>()}));
vi.mock('@capacitor/core',()=>({Capacitor:{isNativePlatform:()=>native.enabled}}));
vi.mock('@capacitor/app',()=>({App:{addListener:vi.fn(async(name:string,callback:(value:any)=>void)=>{native.listeners.set(name,callback);return {remove:async()=>{native.listeners.delete(name);}};}),exitApp:vi.fn()}}));
vi.mock('../src/api/client',()=>({ApiError:class extends Error{constructor(public status:number,message:string,public code=''){super(message);}},me:vi.fn(),login:vi.fn(),logout:vi.fn().mockResolvedValue(undefined),practiceApi:{points:vi.fn(),list:vi.fn(),create:vi.fn(),createNormal:vi.fn(),open:vi.fn(),draft:vi.fn(),save:vi.fn(),segment:vi.fn(),submit:vi.fn(),reconcile:vi.fn(),result:vi.fn()}}));
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
beforeEach(()=>{native.enabled=false;native.listeners.clear();vi.useFakeTimers();vi.clearAllMocks();documentStub=Object.assign(new EventTarget(),{hidden:false});vi.stubGlobal('document',documentStub);vi.stubGlobal('window',Object.assign(new EventTarget(),{confirm:()=>true}));vi.stubGlobal('sessionStorage',{getItem:()=>null,setItem(){},removeItem(){}});});
afterEach(()=>{unmount?.();vi.useRealTimers();vi.unstubAllGlobals();});

test('only successful manual saves publish feedback, not automatic saves or failures',async()=>{
 const app=await setup();expect(app.feedback.value).toBeNull();app.input('2');await vi.advanceTimersByTimeAsync(850);expect(app.feedback.value).toBeNull();await app.manualSave();expect(app.feedback.value).toMatchObject({kind:'saved',practiceId:'1'});const seq=app.feedback.value!.sequence;
 vi.mocked(practiceApi.save).mockRejectedValueOnce(new Error('offline'));app.input('3');await app.manualSave();expect(app.feedback.value!.sequence).toBe(seq);
});
test('new input while manual save is pending is not acknowledged as fully saved',async()=>{
 const app=await setup();app.input('2');let release!:(value:{version:number})=>void;vi.mocked(practiceApi.save).mockImplementationOnce(()=>new Promise(resolve=>release=resolve));const save=app.manualSave();await flush();app.input('3');release({version:2});await save;expect(app.dirty.value).toBe(true);expect(app.feedback.value).toBeNull();
});
test('submit feedback is once per practice and history does not replay it',async()=>{
 const app=await setup();const result={id:'1',contentError:false};vi.mocked(practiceApi.submit).mockResolvedValue(result as any);vi.mocked(practiceApi.result).mockResolvedValue(result as any);app.input('2');await app.confirmSubmit();await app.submit();expect(app.feedback.value).toMatchObject({kind:'completed',practiceId:'1'});const seq=app.feedback.value!.sequence;await app.showResult('1');expect(app.feedback.value!.sequence).toBe(seq);await app.signOut();expect(app.feedback.value).toBeNull();
});
test('failed submit and CHECKING are silent; reconcile completion emits once',async()=>{
 const app=await setup();vi.mocked(practiceApi.submit).mockRejectedValue(new Error('response lost'));app.input('2');await app.confirmSubmit();await app.submit();expect(app.feedback.value).toBeNull();vi.mocked(practiceApi.reconcile).mockResolvedValueOnce({status:'CHECKING'} as any).mockResolvedValue({status:'COMPLETED',result:{id:'1'}} as any);await app.checkSubmission();expect(app.feedback.value).toBeNull();await app.checkSubmission();expect(app.feedback.value).toMatchObject({kind:'completed'});const seq=app.feedback.value!.sequence;await app.checkSubmission();expect(app.feedback.value!.sequence).toBe(seq);
});
test('conflict and confirmed content errors never report success',async()=>{
 const app=await setup();app.conflict.value=true;await app.manualSave();expect(app.feedback.value).toBeNull();app.conflict.value=false;vi.mocked(practiceApi.submit).mockResolvedValue({id:'1',contentError:true} as any);await app.confirmSubmit();await app.submit();expect(app.feedback.value).toBeNull();
});
test('foreground flag changes immediately and returning does not fabricate success',async()=>{
 const app=await setup();documentStub.hidden=true;documentStub.dispatchEvent(new Event('visibilitychange'));expect(app.foreground.value).toBe(false);await flush();documentStub.hidden=false;documentStub.dispatchEvent(new Event('visibilitychange'));expect(app.foreground.value).toBe(true);await flush();expect(app.feedback.value).toBeNull();
});

test('native app-state event updates the same foreground flag without another timer controller',async()=>{
 native.enabled=true;const app=await setup();expect(native.listeners.has('appStateChange')).toBe(true);native.listeners.get('appStateChange')!({isActive:false});expect(app.foreground.value).toBe(false);await flush();native.listeners.get('appStateChange')!({isActive:true});expect(app.foreground.value).toBe(true);await flush();expect(app.feedback.value).toBeNull();
});

test('same completed practice entering the success branch twice still publishes only once',async()=>{
 const app=await setup();vi.mocked(practiceApi.submit).mockResolvedValue({id:'1',contentError:false} as any);app.input('2');await app.confirmSubmit();await app.submit();const seq=app.feedback.value!.sequence;
 await app.submit();expect(practiceApi.submit).toHaveBeenCalledTimes(2);expect(app.feedback.value!.sequence).toBe(seq);expect(app.feedback.value!.kind).toBe('completed');
});
