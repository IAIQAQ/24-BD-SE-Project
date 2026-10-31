import {computed,onMounted,onUnmounted,readonly,ref} from 'vue';
import {confirmation,confirmAction,finishConfirmation} from './confirmation';
import {App} from '@capacitor/app';
import {Capacitor} from '@capacitor/core';
import {ApiError,login,logout,me,practiceApi,updateProfile,type UserProfile} from '../api/client';
import type {Draft,Editor,Mistake,Page,Point,Probe,Result,Summary} from '../api/contracts';
import {SaveQueue} from './saveQueue';
import {ForegroundClock} from './foregroundClock';
import {useNormalComposition} from './useNormalComposition';

export type PracticeFeedback={sequence:number;kind:'saved'|'completed';practiceId:string};
export function useLearningApp(){
  const profile=ref<UserProfile|null>(null),username=ref(''),password=ref(''),page=ref('login');
  const error=ref(''),busy=ref(true),points=ref<Point[]>([]),records=ref<Summary[]>([]),recordPage=ref(1),recordTotal=ref(0);
  const feedback=ref<PracticeFeedback|null>(null),foreground=ref(typeof document==='undefined'||!document.hidden);
  let feedbackSequence=0;const completedFeedbackIds=new Set<string>();
  function resetFeedback(){feedback.value=null;completedFeedbackIds.clear();}
  function publishFeedback(kind:PracticeFeedback['kind'],id:string){
    if(profile.value?.role!=='STUDENT')return;
    if(kind==='completed'){if(completedFeedbackIds.has(id))return;completedFeedbackIds.add(id);if(result.value?.contentError)return;}
    feedback.value={sequence:++feedbackSequence,kind,practiceId:id};
  }
  const normal=useNormalComposition();let normalOwner='';
  const total=normal.total,draft=ref<Draft|null>(null),current=ref(0),saved=ref('尚未作答'),dirty=ref(false),conflict=ref(false),takingOver=ref(false);
  const result=ref<Result|null>(null),mistakes=ref<Page<Mistake>|null>(null),selectedMistake=ref<Mistake|null>(null),mistakePage=ref(1);
  const pending=ref<{id:string;probe:Probe}|null>(null),seconds=ref(0),elapsedBase=ref(0);
  let editor:Editor|null=null,queue:SaveQueue|null=null,clock:ForegroundClock|null=null,segmentId='',lastSegment:any=null;
  let debounce:ReturnType<typeof setTimeout>|undefined,tick:ReturnType<typeof setInterval>|undefined;
  let timingTail:Promise<unknown>=Promise.resolve(),lifecycleTail:Promise<unknown>=Promise.resolve(),stopping:Promise<void>|null=null,timingBusy=false;
  const activeItem=computed(()=>draft.value?.items[current.value]);
  const answered=computed(()=>draft.value?.items.filter(i=>i.raw.trim()!=='').length??0);
  const missing=computed(()=>(draft.value?.total??0)-answered.value);
  const displayTime=computed(()=>`${Math.floor(seconds.value/60)}:${String(seconds.value%60).padStart(2,'0')}`);
  function report(e:unknown){
    error.value=e instanceof Error?e.message:'操作失败，请重试';
    if(e instanceof ApiError&&e.status===401){pauseTimer();resetFeedback();profile.value=null;page.value='login';error.value='登录已失效，请重新登录。已经保存的答案不会丢。';}
    if(e instanceof ApiError&&e.status===409&&['EDITOR_CHANGED','VERSION_CONFLICT','EDITOR_ACTIVE'].includes(e.code)){conflict.value=true;pauseTimer();}
  }
  async function run(action:()=>Promise<void>){if(busy.value)return;busy.value=true;error.value='';try{await action();}catch(e){report(e);}finally{busy.value=false;}}
  function storageKey(){return `mathdrill-pending:${profile.value?.id}`;}
  function persistPending(){if(pending.value)sessionStorage.setItem(storageKey(),JSON.stringify(pending.value));else sessionStorage.removeItem(storageKey());}
  async function loadHome(){
    const [catalog,list]=await Promise.all([practiceApi.points(),practiceApi.list(recordPage.value)]);
    points.value=catalog.filter(p=>p.grade===profile.value?.grade);const owner=`${profile.value?.id}:${profile.value?.grade}`;if(normalOwner!==owner){normal.reset(points.value);normalOwner=owner;}records.value=list.items;recordTotal.value=list.total;
  }
  async function authenticated(user:UserProfile){
    resetFeedback();profile.value=user;password.value='';draft.value=null;editor=null;queue=null;conflict.value=false;dirty.value=false;normalOwner='';result.value=null;mistakes.value=null;page.value='home';recordPage.value=1;
    pending.value=null;const stored=sessionStorage.getItem(storageKey());if(stored){try{pending.value=JSON.parse(stored);}catch{sessionStorage.removeItem(storageKey());}}
    if(user.role==='STUDENT')await loadHome();
  }
  const signIn=()=>run(async()=>authenticated(await login(username.value,password.value)));
  const restore=()=>run(async()=>authenticated(await me()));
  const home=()=>run(async()=>{await leavePractice();page.value='home';await loadHome();});
  async function openPractice(id:string,takeover=false){
    pauseTimer();await timingTail.catch(()=>{});
    if(pending.value?.id===id){page.value='checking';await reconcile();return;}
    const raw=await practiceApi.draft(id);
    if(raw.status==='COMPLETED'){result.value=await practiceApi.result(id);page.value='result';return;}
    if(!['PENDING','IN_PROGRESS'].includes(raw.status)){draft.value=raw;page.value='inactive';return;}
    draft.value=raw;current.value=raw.currentSeq-1;conflict.value=false;takingOver.value=false;page.value='practice';
    try{editor=await practiceApi.open(id,crypto.randomUUID(),takeover);}
    catch(e){if(e instanceof ApiError&&e.code==='EDITOR_ACTIVE'){conflict.value=true;takingOver.value=true;editor=null;queue=null;saved.value='此练习已在另一处打开；可查看已保存内容';return;}throw e;}
    // 打开可能在锁内推进版本，之后重新取持久草稿。
    draft.value=await practiceApi.draft(id);current.value=draft.value.currentSeq-1;
    queue=new SaveQueue(editor.version,payload=>practiceApi.save(id,payload));dirty.value=false;saved.value='已找回上次保存的答案';elapsedBase.value=draft.value.elapsedMs;
    startTimer();
  }
  const resume=(id:string)=>run(()=>openPractice(id));
  const takeover=()=>run(async()=>{if(!draft.value)return;if(dirty.value&&!await confirmAction('将打开上次保存的答案，本页还没保存的内容会被替换。要在这里继续吗？'))return;await openPractice(draft.value.id,true);});
  const configure=()=>{page.value='config';error.value='';};
  const create=()=>run(async()=>{
    const owner=profile.value?.id??'',input=normal.request(owner,points.value);
    const record=await practiceApi.createNormal(input.allocations,input.operationKey);if(profile.value?.id!==owner||!normal.isCurrent(input.operationKey))return;await openPractice(record.id);normal.finish();
  });
  function snapshot(){if(!draft.value||!editor)throw new Error('请先选择在这里继续答题');return {editorSessionId:editor.editorSessionId,currentSeq:current.value+1,answers:draft.value.items.map(i=>({itemId:i.id,raw:i.raw}))};}
  function mark(){if(!queue)return;queue.change();dirty.value=true;saved.value='答案还没保存';clearTimeout(debounce);debounce=setTimeout(()=>{void save().catch(report);},800);}
  function input(value:string){if(!activeItem.value||conflict.value)return;activeItem.value.raw=value;mark();}
  function select(index:number){if(!draft.value||index<0||index>=draft.value.total)return;current.value=index;if(!conflict.value)mark();}
  async function save(){
    clearTimeout(debounce);if(!queue||!draft.value||conflict.value)return;
    const currentQueue=queue;const payload=snapshot();saved.value='正在保存…';
    try{await currentQueue.save(payload);if(queue===currentQueue){dirty.value=currentQueue.dirty;saved.value=dirty.value?'新答案正在等待保存':'答案已保存';}}
    catch(e){saved.value='还没确认保存成功，请先不要关闭页面';throw e;}
  }
  const manualSave=()=>run(async()=>{
    if(!queue||!draft.value||conflict.value)return;
    const originalQueue=queue,id=draft.value.id,owner=profile.value?.id;
    await save();
    if(queue===originalQueue&&!originalQueue.dirty&&draft.value?.id===id&&profile.value?.id===owner&&!conflict.value)publishFeedback('saved',id);
  });
  async function syncTime(ended=false){
    if(!draft.value||!editor||!clock)return;
    const id=draft.value.id,payload={editorSessionId:editor.editorSessionId,segmentId,cumulativeMs:clock.elapsed(),ended};
    if(ended)lastSegment=payload;
    timingTail=timingTail.catch(()=>{}).then(()=>practiceApi.segment(id,payload));await timingTail;
  }
  function pauseTimer(){if(clock){clock.pause();seconds.value=Math.floor((elapsedBase.value+clock.elapsed())/1000);}clearInterval(tick);tick=undefined;}
  function startTimer(){
    if(conflict.value||document.hidden||!editor||!draft.value)return;
    clock=new ForegroundClock();clock.start();segmentId=crypto.randomUUID();lastSegment=null;
    clearInterval(tick);tick=setInterval(()=>{
      if(clock)seconds.value=Math.floor((elapsedBase.value+clock.elapsed())/1000);
      if(!timingBusy){timingBusy=true;void syncTime().catch(e=>{if(e instanceof ApiError&&[401,409].includes(e.status))report(e);}).finally(()=>timingBusy=false);}
    },1000);
  }
  async function stopTime(){
    if(stopping)return stopping;
    pauseTimer();const finished=clock;if(!finished)return;
    stopping=(async()=>{await syncTime(true);elapsedBase.value+=finished.elapsed();if(clock===finished)clock=null;})().finally(()=>{stopping=null;});
    return stopping;
  }
  async function leavePractice(){
    if(page.value==='practice'&&!conflict.value){await save();await stopTime();}
    else if(clock)await stopTime();
    clearTimeout(debounce);
  }
  const saveExit=()=>run(async()=>{await leavePractice();page.value='home';await loadHome();});
  const abandon=()=>run(async()=>{if(!await confirmAction('要放弃本页还没保存的答案吗？以前保存的答案会保留。'))return;clearTimeout(debounce);pauseTimer();await stopTime().catch(()=>{});draft.value=null;queue=null;editor=null;dirty.value=false;page.value='home';await loadHome();});
  const confirmSubmit=()=>run(async()=>{
    const invalid=draft.value?.items.findIndex(i=>i.raw.trim()!==''&&!/^[0-9]+$/.test(i.raw.trim()))??-1;
    if(invalid>=0){current.value=invalid;throw new Error(`第 ${invalid+1} 题格式不正确，请填写0或更大的整数，如0、5、12`);}
    await save();await stopTime();page.value='confirm';
  });
  const returnAnswer=()=>{page.value='practice';startTimer();};
  const submit=()=>run(async()=>{
    if(!draft.value||!editor||!queue)return;
    const id=draft.value.id,operationKey=crypto.randomUUID(),probe={operationKey,editorSessionId:editor.editorSessionId,baseVersion:queue.version};
    const payload={...snapshot(),...probe,confirmUnanswered:true,lastSegment};pending.value={id,probe};persistPending();page.value='checking';
    try{result.value=await practiceApi.submit(id,payload);pending.value=null;persistPending();dirty.value=false;page.value='result';publishFeedback('completed',id);}
    catch(e){if(e instanceof ApiError&&e.status===400){pending.value=null;persistPending();page.value='practice';startTimer();}report(e);}
  });
  async function reconcile(){
    if(!pending.value)return;const p=pending.value;const checked=await practiceApi.reconcile(p.id,p.probe);
    if(checked.status==='COMPLETED'){result.value=checked.result??await practiceApi.result(p.id);pending.value=null;persistPending();page.value='result';publishFeedback('completed',p.id);return;}
    if(checked.status==='CHECKING'){page.value='checking';return;}
    pending.value=null;persistPending();
    if(checked.status==='VOID'||checked.status==='WITHDRAWN'){draft.value=await practiceApi.draft(p.id);page.value='inactive';return;}
    if(checked.canEdit&&checked.editorSessionId){
      const raw=await practiceApi.draft(p.id);if(!draft.value||draft.value.id!==p.id)draft.value=raw;
      editor={...raw,editorSessionId:checked.editorSessionId,version:checked.version};queue=new SaveQueue(checked.version,payload=>practiceApi.save(p.id,payload));queue.change();dirty.value=true;conflict.value=false;elapsedBase.value=raw.elapsedMs;page.value='practice';saved.value='已确认未提交，可继续作答';startTimer();
    }else{draft.value=await practiceApi.draft(p.id);page.value='practice';conflict.value=true;takingOver.value=true;error.value='还没有交卷，这份练习已在别处打开。请先选择在这里继续。';}
  }
  const checkSubmission=()=>run(reconcile);
  const showResult=(id:string)=>run(async()=>{result.value=await practiceApi.result(id);page.value='result';});
  const loadMistakes=()=>run(async()=>{if(!profile.value)return;await leavePractice();mistakes.value=await practiceApi.mistakes(profile.value.id,mistakePage.value);page.value='mistakes';});
  const showMistake=(item:Mistake)=>run(async()=>{if(!profile.value)return;selectedMistake.value=await practiceApi.mistake(profile.value.id,item.id);page.value='mistakeDetail';});
  const nextRecords=(step:number)=>run(async()=>{recordPage.value+=step;await loadHome();});
  const nextMistakes=(step:number)=>{mistakePage.value+=step;void loadMistakes();};
  const signOut=()=>run(async()=>{
    if(dirty.value&&!await confirmAction('仍有未保存输入，先保存再退出登录？取消可留在本页。'))return;
    await leavePractice();await logout();resetFeedback();profile.value=null;draft.value=null;queue=null;editor=null;records.value=[];mistakes.value=null;result.value=null;page.value='login';dirty.value=false;pauseTimer();
  });
  const saveProfile=(nickname:string,grade:number)=>run(async()=>{profile.value=await updateProfile(nickname,grade);page.value='home';await loadHome();});
  async function visibility(active:boolean){
    if(page.value!=='practice'||conflict.value||!editor||!draft.value)return;
    if(!active){pauseTimer();try{await save();await stopTime();}catch(e){report(e);}}
    else{try{await save();await stopTime();const raw=await practiceApi.draft(draft.value.id);if(raw.status==='COMPLETED'){result.value=await practiceApi.result(raw.id);page.value='result';return;}if(queue&&raw.version!==queue.version){conflict.value=true;error.value='答案已在别处更新，请确认是否要在这里继续。';return;}elapsedBase.value=raw.elapsedMs;startTimer();}catch(e){report(e);if(!conflict.value&&profile.value)startTimer();}}
  }
  const transition=(active:boolean)=>{foreground.value=active;lifecycleTail=lifecycleTail.catch(()=>{}).then(()=>visibility(active));};
  const visible=()=>transition(!document.hidden);
  const beforeUnload=(e:BeforeUnloadEvent)=>{if(dirty.value||pending.value){e.preventDefault();e.returnValue='';}};
  const back=async()=>{if(page.value==='practice'){if(dirty.value&&!await confirmAction('保存当前答案并返回首页？'))return;void saveExit();}else if(page.value==='confirm')returnAnswer();else if(page.value==='checking'){page.value='home';void loadHome().catch(report);}else if(page.value==='mistakeDetail')page.value='mistakes';else if(page.value!=='home'&&page.value!=='login')void home();};
  const nativeListeners:{remove:()=>Promise<void>}[]=[];
  onMounted(async()=>{
    if(!Capacitor.isNativePlatform())document.addEventListener('visibilitychange',visible);window.addEventListener('beforeunload',beforeUnload);
    if(Capacitor.isNativePlatform()){
      nativeListeners.push(await App.addListener('appStateChange',({isActive})=>transition(isActive)));
      nativeListeners.push(await App.addListener('backButton',async()=>{if(confirmation.value!==null){finishConfirmation(false);return;}if(page.value==='home'||page.value==='login'){if(await confirmAction('退出小芽口算？'))void App.exitApp();}else void back();}));
    }
    try{await authenticated(await me());}catch(e){if(!(e instanceof ApiError&&e.status===401))report(e);}finally{busy.value=false;}
  });
  onUnmounted(()=>{clearInterval(tick);clearTimeout(debounce);document.removeEventListener('visibilitychange',visible);window.removeEventListener('beforeunload',beforeUnload);for(const l of nativeListeners)void l.remove();});
  return {feedback:readonly(feedback),foreground:readonly(foreground),profile,username,password,page,error,busy,points,normal,records,total,draft,current,activeItem,answered,missing,displayTime,saved,dirty,conflict,takingOver,result,mistakes,selectedMistake,pending,recordPage,recordTotal,mistakePage,
    signIn,restore,signOut,saveProfile,home,configure,create,resume,takeover,input,select,manualSave,saveExit,abandon,confirmSubmit,returnAnswer,submit,checkSubmission,showResult,loadMistakes,showMistake,nextRecords,nextMistakes,back};
}
