<script setup lang="ts">
import '../styles/learning-ui.css';
import UiIcon from './UiIcon.vue';
import SproutCompanion from './SproutCompanion.vue';
import GardenHero from './GardenHero.vue';
import PracticeGrowth from './PracticeGrowth.vue';
import StudentHeader from './StudentHeader.vue';
import IllustratedPracticeCard from './IllustratedPracticeCard.vue';
import ConfirmationDialog from './ConfirmationDialog.vue';
import NormalPracticeConfig from './NormalPracticeConfig.vue';
import {useLearningApp} from '../composables/useLearningApp';
import {computed,onUnmounted,ref,watch} from 'vue';
import {useInteractionFeedback} from '../composables/useInteractionFeedback';
import {useReviewSelection} from '../composables/useReviewSelection';
import ReviewPracticePanel from './ReviewPracticePanel.vue';import RegisterForm from './RegisterForm.vue';import ParentWorkspace from './ParentWorkspace.vue';import AdminWorkspace from './AdminWorkspace.vue';import FamilyLinks from './FamilyLinks.vue';import StudentProfileForm from './StudentProfileForm.vue';
const app=useLearningApp();
const {feedback,foreground,profile,username,password,page,error,busy,points,normal,records,total,draft,current,activeItem,answered,missing,displayTime,saved,dirty,conflict,result,mistakes,selectedMistake,pending,recordPage,recordTotal,mistakePage}=app;
const sound=useInteractionFeedback(computed(()=>foreground.value&&profile.value?.role==='STUDENT'));
const celebrating=ref(false),savedPulse=ref(false);
let celebrateTimer:ReturnType<typeof setTimeout>|undefined,saveFeedbackTimer:ReturnType<typeof setTimeout>|undefined;
function clearFeedback(){clearTimeout(celebrateTimer);clearTimeout(saveFeedbackTimer);celebrating.value=false;savedPulse.value=false;}
function tap(action:()=>unknown){void sound.play('tap');action();}
watch(page,clearFeedback);
watch(()=>profile.value?.id,()=>{clearFeedback();sound.stop();});
watch(foreground,active=>{if(!active){clearFeedback();sound.stop();}},{flush:'sync'});
watch(feedback,event=>{
  if(!event||!foreground.value||profile.value?.role!=='STUDENT')return;
  if(event.kind==='saved'&&page.value==='practice'&&draft.value?.id===event.practiceId){
    clearTimeout(saveFeedbackTimer);savedPulse.value=true;void sound.play('saved');saveFeedbackTimer=setTimeout(()=>savedPulse.value=false,900);
  }else if(event.kind==='completed'&&page.value==='result'&&result.value?.id===event.practiceId&&!result.value.contentError){
    clearTimeout(celebrateTimer);celebrating.value=true;void sound.play('completed');celebrateTimer=setTimeout(()=>celebrating.value=false,1800);
  }
},{flush:'post'});
onUnmounted(clearFeedback);
const outcome=(value:string)=>({CORRECT:'答对',WRONG:'答错',UNANSWERED:'未答'}[value]??value);
const state=(value:string)=>({PENDING:'待完成 · 点击开始',IN_PROGRESS:'进行中 · 继续作答',COMPLETED:'已完成 · 查看结果',WITHDRAWN:'已撤回',VOID:'已作废'}[value]??value);
const review=useReviewSelection(()=>profile.value?.id??'');
watch(()=>profile.value?.id,()=>review.clear());
const kind=(value?:string)=>({REPLAY:'原题重练',REINFORCE:'同类强化'}[value??'']??'普通练习');
async function startReplay(){const record=await review.replay();if(record){await app.resume(record.id);if(page.value==='practice')review.clear();}}
async function startReinforce(){const record=await review.reinforce();if(record){await app.resume(record.id);if(page.value==='practice')review.clear();}}
const registrationMessage=ref('');function registered(name:string){username.value=name;password.value='';page.value='login';registrationMessage.value='注册成功，请使用刚才的密码登录。';}
</script>
<template>
  <main :class="{'learning-ui':profile?.role!=='ADMIN','student-ui':profile?.role==='STUDENT'}" :aria-busy="busy">
    <ConfirmationDialog/><StudentHeader v-if="profile?.role==='STUDENT'" :nickname="profile.nickname" :grade="profile.grade" :disabled="busy||['practice','confirm','checking'].includes(page)" @open-profile="page='profile'"/><header v-else><span class="logo">芽</span><strong>小芽口算</strong><span class="badge">快乐练口算</span></header>
    <div v-if="profile&&profile.role!=='STUDENT'" class="account"><span>{{profile.nickname}} · {{profile.role==='STUDENT'?profile.grade+' 年级':profile.role==='PARENT'?'家长':'管理员'}}</span><button @click="app.signOut" :disabled="busy">退出登录</button></div>
    <p v-if="error" class="error" role="alert">{{error}}</p><p v-if="review.error.value" class="error" role="alert">{{review.error.value}}</p>
    <section v-if="page==='login'" class="login">
      <p class="eyebrow">每天一点，慢慢成长</p><h1>今天，也来练一练。</h1><p class="muted">登录后开始练习。</p>
      <p v-if="registrationMessage" class="notice" role="status">{{registrationMessage}}</p><form @submit.prevent="app.signIn"><label for="username">账号</label><input id="username" v-model="username" autocomplete="username" required placeholder="学生、家长或管理员账号"><label for="password">密码</label><input id="password" v-model="password" type="password" autocomplete="current-password" required placeholder="输入密码"><button class="primary" :disabled="busy">{{busy?'正在登录…':'登录学习账号'}}</button></form>
      <button class="secondary" :disabled="busy" @click="app.restore">检查已有登录状态</button>
      <button class="text-button" :disabled="busy" @click="page='register'">还没有账号？注册学生或家长账号</button>
    </section>
    <RegisterForm v-else-if="page==='register'&&!profile" @registered="registered" @cancel="page='login'"/>
    <ParentWorkspace v-else-if="profile?.role==='PARENT'" :profile="profile" @expired="app.restore"/>
    <AdminWorkspace v-else-if="profile?.role==='ADMIN'" @expired="app.restore"/>
    <template v-else-if="profile">
      <section v-if="page==='home'" class="student-home">
        <h1 class="home-greeting">{{profile.nickname}}，准备好了吗？<span aria-hidden="true" class="greeting-rays">✦</span></h1>
        <GardenHero :busy="busy" :active="foreground" @start="tap(app.configure)"/>
        <div v-if="pending" class="notice"><b>有一份练习待核对提交结果</b><button @click="app.resume(pending.id)" :disabled="busy">继续核对</button></div>
        <div class="section-title"><h2>我的练习</h2><button @click="app.home" :disabled="busy">刷新</button></div>
        <div v-if="!records.length&&!error" class="empty"><p>还没有练习</p><SproutCompanion scene="empty"/></div>
        <IllustratedPracticeCard v-for="record in records" :key="record.id" :record="record" :busy="busy" @open="record.status==='COMPLETED'?app.showResult(record.id):tap(()=>app.resume(record.id))"/>
        <div v-if="recordTotal>20" class="pager"><button :disabled="busy||recordPage<=1" @click="app.nextRecords(-1)">上一页</button><span>{{recordPage}}</span><button :disabled="busy||recordPage*20>=recordTotal" @click="app.nextRecords(1)">下一页</button></div>
        <div class="home-encouragement"><SproutCompanion scene="complete">慢慢来，你可以的！</SproutCompanion></div>
      </section>
      <section v-else-if="page==='config'">
        <button class="back" @click="app.home">‹ 返回首页</button><p class="eyebrow">选择适合自己的节奏</p><h1>今天想练什么？</h1>
        <form @submit.prevent="tap(app.create)"><NormalPracticeConfig prefix="student" :points="points" :selected-ids="normal.selectedIds.value" :total="total" :allocations="normal.allocations.value" :disabled="busy" @toggle="normal.toggle" @total="normal.setTotal" @allocation="normal.setAllocation"/><button class="primary" :disabled="busy">{{busy?'正在准备…':'开始练习'}}</button></form>
      </section>
      <section v-else-if="page==='practice'&&draft">
        <div class="practice-top"><button class="back" @click="app.saveExit" :disabled="busy">‹ 保存退出</button><span>{{displayTime}}</span></div><button class="sound-toggle" type="button" :aria-pressed="sound.enabled.value" @click="sound.setEnabled(!sound.enabled.value)">提示音：{{sound.enabled.value?'开':'关'}}</button><p v-if="sound.notice.value" class="muted sound-notice" role="status">{{sound.notice.value}}</p>
        <div class="section-title"><h2>{{draft.pointName??'20 以内加法'}}</h2><span>{{current+1}} / {{draft.total}}</span></div>
        <PracticeGrowth :key="draft.id" :answered="answered" :total="draft.total" :active="foreground&&!busy&&!conflict"/>
        <p class="save-state" :class="{'save-feedback':savedPulse}" role="status"><UiIcon :name="dirty||conflict?'practice':'check'"/> {{saved}}</p>
        <div v-if="conflict" class="notice"><b>这份练习已在别处打开</b><p>如果想在这里继续，请点下面的按钮。这里还没保存的答案暂时只留在本页。</p><button @click="app.takeover" :disabled="busy">在这里继续答题</button></div>
        <div class="question card"><p class="label">第 {{current+1}} 题</p><h1>{{activeItem?.stem}}</h1><label class="answer-label" for="answer">你的答案</label><input id="answer" :value="activeItem?.raw??''" @input="app.input(($event.target as HTMLInputElement).value)" @keydown.enter.prevent="app.select(Math.min(current+1,draft.total-1))" inputmode="numeric" autocomplete="off" maxlength="128" :disabled="busy||conflict" placeholder="在这里填写" aria-label="你的答案"></div>
        <div class="two"><button class="secondary" :disabled="current===0||busy" @click="tap(()=>app.select(current-1))">上一题</button><button class="secondary" :disabled="current===draft.total-1||busy" @click="tap(()=>app.select(current+1))">下一题</button></div>
        <details><summary>选择题号</summary><div class="numbers"><button v-for="(item,index) in draft.items" :key="item.id" :class="{filled:item.raw.trim()!=='',current:index===current}" @click="tap(()=>app.select(index))">{{index+1}}</button></div></details>
        <div class="two"><button class="secondary" :disabled="busy||conflict" @click="app.manualSave">手动保存</button><button class="primary" :disabled="busy||conflict" @click="app.confirmSubmit">结束并检查</button></div>
        <button v-if="dirty||conflict" class="text-button" @click="app.abandon" :disabled="busy">放弃本页未保存修改</button>
      </section>
      <section v-else-if="page==='inactive'&&draft"><p class="eyebrow">任务状态已改变</p><h1>{{state(draft.status)}}</h1><p class="notice">{{draft.errorNote||'这份任务已不能继续作答，原有记录仍保留。'}}</p><button class="primary" @click="app.home">返回练习列表</button></section>
      <FamilyLinks v-else-if="page==='family'" :profile="profile" @expired="app.restore"/>
      <section v-else-if="page==='confirm'&&draft">
        <p class="eyebrow">再检查一次</p><h1>准备提交了吗？</h1><div class="card"><p>共 {{draft.total}} 题，已填写 {{answered}} 题。</p><p v-if="missing">还有 <b>{{missing}}</b> 题没做。现在交卷，这些题会记为未答，不计入答对题数。</p><p v-else>所有题目都已填写。</p><p class="muted">交卷后就能查看成绩，答案不能再修改。</p></div><button class="primary" :disabled="busy" @click="app.submit">{{missing?'仍然交卷':'确认提交'}}</button><button class="secondary" :disabled="busy" @click="app.returnAnswer">返回继续作答</button>
      </section>
      <section v-else-if="page==='checking'">
        <p class="eyebrow">保护每一份作答</p><h1>正在核对提交结果</h1><div class="card"><p>提交结果未确认前，暂时停止输入和计时。</p><p class="muted">请查询是否已经完成，避免重复提交。</p></div><button class="primary" :disabled="busy" @click="app.checkSubmission">{{busy?'正在核对…':'重新核对结果'}}</button><button class="secondary" :disabled="busy" @click="app.home">返回首页，稍后核对</button>
      </section>
      <section v-else-if="page==='result'&&result">
        <div class="result-garden"><div><p class="eyebrow">{{kind(result.practiceKind)}} · 练习完成</p><h1>每一步，都算数。</h1></div><SproutCompanion scene="complete" :celebrating="celebrating"/></div><p v-if="result.contentError" class="error">此练习含已确认错误内容：{{result.errorNote}}。原成绩保留，未自动重算。</p><div class="hero score"><strong>{{result.accuracy}}<small>%</small></strong><p class="score-count">答对 {{result.correct}} / {{result.total}} 题</p><div class="stats"><span><b>{{result.correct}}</b>答对</span><span><b>{{result.mistakes-result.unanswered}}</b>答错</span><span><b>{{result.unanswered}}</b>未答</span></div></div>
        <details class="result-meta"><summary>用时与提交信息</summary><p class="muted">记录用时 {{Math.floor(result.elapsedMs/60000)}} 分 {{Math.floor(result.elapsedMs/1000)%60}} 秒{{result.timingComplete?'':' · 部分用时未记录'}}</p>
        <p class="muted">提交时间 {{result.submittedAt.replace('T',' ').slice(0,19)}} · 成绩已保存</p></details><button class="primary" @click="app.loadMistakes">查看我的错题</button>
        <h2 class="result-heading">逐题结果</h2><details v-for="item in result.items" :key="item.id" class="result-item"><summary><span>{{item.seq}}. {{item.stem}}</span><b :class="item.outcome==='CORRECT'?'correct':'wrong'">{{outcome(item.outcome)}}</b></summary><p>我的答案：{{item.raw.trim()===''?'未答':item.raw}}</p><p>标准答案：{{item.standardAnswer}}</p><p class="muted">{{item.explanation}}</p></details><button class="secondary" @click="app.home">回到首页</button>
      </section>
      <section v-else-if="page==='mistakes'">
        <div class="review-garden"><div><p class="eyebrow">再试一次</p><h1>我的错题</h1><p class="review-hint">想一想，再练练</p></div><SproutCompanion scene="welcome">小芽陪你一起。</SproutCompanion></div><p v-if="mistakes" class="muted">{{mistakes.total}} 道不同题，累计错误 {{mistakes.totalErrors}} 次</p>
        <div v-if="mistakes?.total===0" class="empty"><p>暂时没有错题</p><SproutCompanion scene="empty">继续保持，慢慢进步。</SproutCompanion></div>
        <div class="mistake-toolbar"><span>已选 <b>{{review.selectedIds.value.length}}</b> 题</span><button class="primary" :disabled="busy||review.busy.value||!review.selectedIds.value.length" @click="review.prepare()">去练一练 <UiIcon name="arrow"/></button></div><details><summary>选题说明</summary><p class="muted">可以翻页继续选，不同年级请分开练。原成绩和错题都会保留。</p></details>
        <div v-for="item in mistakes?.items" :key="item.id" class="card mistake-card" :class="{chosen:review.selectedIds.value.includes(item.id)}"><label class="mistake-select"><input class="checkbox" type="checkbox" :checked="review.selectedIds.value.includes(item.id)" :disabled="busy||review.busy.value" @change="review.toggle(item.id)" :aria-label="`选择错题 ${item.stem}`">{{review.selectedIds.value.includes(item.id)?'已选中':'选择这题'}}</label><button class="record" :disabled="busy" @click="app.showMistake(item)"><span class="record-copy"><b>{{item.stem}}</b><small>{{item.grade}}年级 · {{item.pointName}} · {{outcome(item.outcome)}} · 累计 {{item.count}} 次</small></span><span class="record-action">看解析 <UiIcon name="arrow"/></span></button></div>
        <div v-if="(mistakes?.total??0)>20" class="pager"><button :disabled="busy||mistakePage<=1" @click="app.nextMistakes(-1)">上一页</button><span>{{mistakePage}}</span><button :disabled="busy||mistakePage*20>=(mistakes?.total??0)" @click="app.nextMistakes(1)">下一页</button></div>
      </section>
      <section v-else-if="page==='mistakeDetail'&&selectedMistake">
        <button class="back" @click="page='mistakes'">‹ 返回错题本</button><p class="eyebrow">弄懂一道，进步一点</p><h1>{{selectedMistake.stem}}</h1><div class="card"><p>我的答案：{{selectedMistake.raw.trim()===''?'未答':selectedMistake.raw}}</p><p class="correct">标准答案：{{selectedMistake.standardAnswer}}</p><p class="muted">{{selectedMistake.explanation}}</p></div><p class="muted">{{selectedMistake.grade}}年级 · {{selectedMistake.pointName}}</p><p v-if="selectedMistake.replayAllowed===false" class="error">此历史内容已确认有误：{{selectedMistake.replayBlockReason}}，不能原题重练。</p><button class="primary" :disabled="busy||review.busy.value" @click="review.prepare([selectedMistake.id])">再练一练这道题</button><details><summary>以前的作答记录</summary><div v-for="item in selectedMistake.history" :key="item.id" class="card history"><p>{{kind(item.practiceKind)}} · {{outcome(item.outcome)}} · 答案 {{item.raw.trim()===''?'未答':item.raw}}</p><button @click="app.showResult(item.practiceId)">查看该次练习结果</button></div></details>
      </section>
      <ReviewPracticePanel v-if="['mistakes','mistakeDetail'].includes(page)&&review.options.value" :options="review.options.value" :busy="review.busy.value" :grade="review.grade.value" :allocations="review.allocations.value" :allocation-total="review.allocationTotal.value" @grade="review.selectGrade" @allocation="review.setAllocation" @reinforce="startReinforce" @close="review.close" @replay="startReplay"/>
      <nav v-if="!['practice','confirm','checking'].includes(page)" aria-label="主导航"><button :class="{active:page==='home'}" :disabled="busy" @click="app.home"><UiIcon name="practice"/>练习</button><button :class="{active:page==='mistakes'||page==='mistakeDetail'}" :disabled="busy" @click="app.loadMistakes"><UiIcon name="mistakes"/>错题</button><button :class="{active:page==='family'}" @click="page='family'"><UiIcon name="family"/>家庭</button><button :class="{active:page==='profile'}" @click="page='profile'"><UiIcon name="profile"/>我的</button></nav>
      <section v-if="page==='profile'"><p class="eyebrow">我的学习账号</p><h1>{{profile.nickname}}</h1><button class="sound-toggle" type="button" :aria-pressed="sound.enabled.value" @click="sound.setEnabled(!sound.enabled.value)">提示音：{{sound.enabled.value?'开':'关'}}</button><p v-if="sound.notice.value" class="muted sound-notice" role="status">{{sound.notice.value}}</p><StudentProfileForm :profile="profile" :busy="busy" @save="app.saveProfile"/><button class="secondary" :disabled="busy" @click="app.signOut">退出登录</button></section>
    </template>
    <footer>小芽口算 · 一次练习，一点成长</footer>
  </main>
</template>
<style>
:root{font-family:"Microsoft YaHei",system-ui,sans-serif;color:#294d3b;background:#f4f7ef;font-synthesis:none}*{box-sizing:border-box}body{margin:0}main{max-width:470px;margin:auto;padding:calc(24px + env(safe-area-inset-top,0px)) 23px calc(95px + env(safe-area-inset-bottom,0px));min-height:100dvh}header{display:flex;align-items:center;gap:10px;font-size:21px}.logo{display:grid;place-items:center;background:#285b49;color:#e0edba;width:40px;height:40px;border-radius:14px}.badge{margin-left:auto;background:#e3ebd5;border-radius:20px;padding:6px 11px;font-size:11px;color:#72885c}.account{display:flex;justify-content:space-between;margin:21px 0;font-size:13px;color:#7c8c70}button,input{font:inherit}button{cursor:pointer;color:inherit;border:0;background:transparent;min-height:40px}button:disabled{opacity:.48;cursor:wait}button:focus-visible,input:focus-visible,summary:focus-visible{outline:3px solid #8eaf75;outline-offset:3px}h1{font-size:29px;line-height:1.55;font-weight:600;margin:12px 0 22px}h2{font-size:19px;font-weight:600;margin:12px 0}.eyebrow{font-size:11px;letter-spacing:2px;color:#859a6d;margin-top:34px}.muted{font-size:13px;line-height:1.9;color:#7f8f73}.card,.hero{border:1px solid #e1e7d7;background:white;border-radius:19px;padding:22px;margin:20px 0}.hero{background:#e8f0dd;border:0;text-align:center}.hero p{font-size:13px;color:#758965}.sprout{font-size:40px}.primary,.secondary{display:block;width:100%;border-radius:13px;padding:14px;font-size:14px;min-height:49px;margin-top:17px}.primary{background:#285b49;color:white}.secondary{border:1px solid #d9e2ce;background:transparent;color:#64804e}label{display:block;font-size:13px;margin:22px 0 9px}input{width:100%;border:1px solid #d9e2ce;background:white;color:#294d3b;border-radius:13px;min-height:52px;padding:14px;font-size:17px}input::placeholder{color:#a4b098;font-size:13px}.section-title,.practice-top{display:flex;justify-content:space-between;align-items:center;margin-top:22px}.section-title>span,.practice-top>span{font-size:14px;color:#8b9c75}.record{display:flex;align-items:center;gap:13px;border:1px solid #e2e8da;background:white;border-radius:15px;padding:17px;width:100%;text-align:left;margin:12px 0}.record span:last-child{margin-left:auto;color:#90a37e}.record b{font-size:14px;font-weight:500}.record small{display:block;font-size:11px;margin-top:7px;color:#8b997d}.record-icon{display:grid;place-items:center;width:37px;height:37px;border-radius:12px;background:#edf2e3;color:#77985b;flex-shrink:0}.record-icon.warm{background:#f7e9df;color:#ac785c}.error,.notice{padding:15px;background:#f8ece2;color:#9b6047;border-radius:13px;font-size:13px;line-height:1.8;margin:18px 0}.notice button{font-weight:600;text-decoration:underline}.empty{font-size:13px;color:#8b9c7c;text-align:center;padding:25px 10px;line-height:1.9}.quick{display:flex;gap:9px;margin-top:13px}.quick button{flex:1;border:1px solid #dce5cf;border-radius:10px;font-size:12px}.quick .selected{background:#dfeacc;color:#4c7042}.label{font-size:11px;letter-spacing:1px;color:#8b9d77}.back{font-size:13px;color:#7c9565;padding:0}.question{text-align:center;padding:25px 20px}.question h1{font-size:40px;margin:28px 0}.answer-label{font-size:12px;color:#8a9c75}.question input{text-align:center;font-size:27px;max-width:220px;min-height:65px}.two{display:flex;gap:12px}.two>*{flex:1}progress{width:100%;height:6px;border:0;border-radius:6px;accent-color:#89aa67;overflow:hidden;margin-top:10px}.save-state{font-size:11px;color:#7c9565;line-height:1.8}details{margin-top:18px}summary{cursor:pointer;font-size:13px;color:#718a5c;padding:12px 0}.numbers{display:grid;grid-template-columns:repeat(6,1fr);gap:8px;margin:12px 0}.numbers button{border:1px solid #dae2d0;background:white;border-radius:9px;font-size:12px}.numbers .filled{background:#e0ebd2}.numbers .current{border:2px solid #37644b}.text-button{font-size:12px;color:#9b7659;display:block;margin:15px auto}.score>strong{display:block;font-size:57px;font-weight:500;color:#416d40;margin-top:12px}.score>strong small{font-size:22px}.stats{display:flex;justify-content:space-around;margin:24px 0 8px}.stats span{font-size:11px;color:#849970}.stats b{display:block;font-size:22px;font-weight:500;color:#55764a;margin-bottom:7px}.correct{color:#58804c}.wrong{color:#b17655}.result-heading{margin-top:30px}.result-item{border-bottom:1px solid #e0e7d7}.result-item summary{display:flex;justify-content:space-between;font-size:15px}.result-item p{font-size:13px;line-height:1.8}.history p{font-size:13px}.history button{font-size:12px;text-decoration:underline}.pager{display:flex;align-items:center;justify-content:center;gap:22px;font-size:13px}.pager button{color:#6d8955}nav{position:fixed;bottom:0;left:50%;transform:translateX(-50%);max-width:470px;width:100%;display:flex;justify-content:space-around;border-top:1px solid #e0e6d8;background:#f8faf4;padding:10px 15px calc(10px + env(safe-area-inset-bottom,0px));z-index:10}nav button{font-size:13px;color:#99a88c;flex:1}nav .active{color:#386144;font-weight:700}footer{text-align:center;margin-top:35px;font-size:10px;color:#a2af96;line-height:1.8}.login .muted{margin-bottom:27px}@media(max-width:350px){main{padding-left:16px;padding-right:16px}.question h1{font-size:34px}h1{font-size:25px}}
</style>
<style>select,textarea{width:100%;padding:13px;border:1px solid #d9e2ce;border-radius:12px;background:white;color:#294d3b;font:inherit;min-height:49px}textarea{min-height:90px;resize:vertical}input.checkbox{width:auto;min-height:0;vertical-align:middle;margin-right:8px}li{font-size:14px;line-height:1.9;margin:7px 0}</style>
