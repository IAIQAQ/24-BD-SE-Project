import {ref} from 'vue';
import {renderToString} from '@vue/server-renderer';
import {createSSRApp} from 'vue';
import {beforeEach,expect,test,vi} from 'vitest';
const fixture=vi.hoisted(()=>({state:null as any}));
vi.mock('../src/composables/useLearningApp',()=>({useLearningApp:()=>fixture.state}));
import LearningApp from '../src/views/LearningApp.vue';

beforeEach(()=>{
 fixture.state={feedback:ref(null),foreground:ref(true),profile:ref({id:'test-only',nickname:'小朋友',role:'STUDENT',grade:1}),username:ref(''),password:ref(''),page:ref('home'),error:ref(''),busy:ref(false),points:ref([]),normal:{},records:ref([]),total:ref(30),draft:ref(null),current:ref(0),activeItem:ref(null),answered:ref(0),missing:ref(0),displayTime:ref('0:00'),saved:ref(''),dirty:ref(false),conflict:ref(false),result:ref(null),mistakes:ref({total:0,totalErrors:0,items:[]}),selectedMistake:ref(null),pending:ref(null),recordPage:ref(1),recordTotal:ref(0),mistakePage:ref(1)};
});
const render=()=>renderToString(createSSRApp(LearningApp));

test('empty home includes welcome, reused empty art and the existing start action',async()=>{
 const html=await render();expect(html).toContain('garden-hero');expect(html).toContain('sprout-empty');expect(html).toContain('开始练习');expect(html).toContain('还没有练习');
});
test('empty mistakes retains a truthful empty message and encouragement',async()=>{
 fixture.state.page.value='mistakes';const html=await render();expect(html).toContain('sprout-empty');expect(html).toContain('暂时没有错题');expect(html).toContain('继续保持，慢慢进步。');
});
test('completion encouragement does not replace a zero score or result actions',async()=>{
 fixture.state.page.value='result';fixture.state.result.value={practiceKind:'NORMAL',correct:0,total:1,mistakes:1,unanswered:0,accuracy:'0.00',elapsedMs:0,timingComplete:true,submittedAt:'2026-10-30T00:00:00',items:[]};
 const html=await render();expect(html).toContain('sprout-complete');expect(html).toContain('0.00');expect(html).toContain('查看我的错题');expect(html).toContain('回到首页');
});
test('answering page contains no companion and keeps answer/save/submit controls',async()=>{
 fixture.state.page.value='practice';fixture.state.draft.value={pointName:'加法',total:1,items:[{id:'i',raw:''}]};fixture.state.activeItem.value={stem:'1 + 1 =',raw:''};
 const html=await render();expect(html).not.toContain('sprout-companion');expect(html).toContain('id="answer"');expect(html).toContain('手动保存');expect(html).toContain('结束并检查');
});

test('student home has a truthful grade header and keeps all illustrated records and pagination',async()=>{
 fixture.state.profile.value.grade=3;
 fixture.state.records.value=[{id:'one',status:'IN_PROGRESS',total:100,currentSeq:2,source:'SELF',practiceKind:'NORMAL',pointName:'长知识点完整保留'},{id:'two',status:'PENDING',total:5,currentSeq:1,source:'PARENT',practiceKind:'NORMAL',pointName:'乘法'}];
 fixture.state.recordTotal.value=21;
 const html=await render();expect(html).toContain('三年级');expect(html).toContain('我的资料');expect(html.match(/class="illustrated-practice"/g)).toHaveLength(2);expect(html).toContain('上次看到第2题');expect(html).toContain('长知识点完整保留');expect(html).toContain('下一页');expect(html).toContain('home-encouragement');
});

test('profile shortcut is disabled during protected practice stages and requests',async()=>{
 for(const page of ['practice','confirm','checking','home']){
  fixture.state.page.value=page;fixture.state.busy.value=page==='home';
  const html=await render();expect(html).toMatch(/<button[^>]*class="student-avatar"[^>]*disabled/);
 }
});

test('practice shows actual filled growth with separate save status and no garden or celebration',async()=>{
 fixture.state.page.value='practice';fixture.state.draft.value={id:'new-practice',total:5,pointName:'长知识点名称完整保留',items:[{id:'i',raw:'wrong'}]};fixture.state.activeItem.value={stem:'2 + 3 =',raw:'wrong'};fixture.state.answered.value=3;fixture.state.saved.value='答案还没保存';
 const html=await render();expect(html).toContain('practice-growth');expect(html).toContain('已填写 3/5 题');expect(html).toContain('答案还没保存');expect(html).not.toContain('garden-hero');expect(html).not.toContain('提交成功');expect(html).toContain('id="answer"');
});

test('sound control is explicit and off by default on practice and profile',async()=>{
 fixture.state.page.value='practice';fixture.state.draft.value={id:'p',total:1,items:[{id:'i',raw:''}]};fixture.state.activeItem.value={stem:'1 + 1 =',raw:''};
 for(const page of ['practice','profile']){fixture.state.page.value=page;const html=await render();expect(html).toContain('提示音：关');expect(html).toContain('aria-pressed="false"');}
});

test('long nickname and 100-question long content stay available with the existing controls',async()=>{
 const nickname='这是用来检查较长昵称是否完整显示的小朋友昵称';const point='两三位数除以一位数（整除）及整十整百数加减法';fixture.state.profile.value.nickname=nickname;fixture.state.records.value=[{id:'long',status:'IN_PROGRESS',total:100,currentSeq:50,pointName:point,source:'PARENT',practiceKind:'NORMAL'}];fixture.state.recordTotal.value=21;
 const html=await render();expect(html).toContain(nickname);expect(html).toContain(point);expect(html).toContain('共100题');expect(html).toContain('上次看到第50题');expect(html).not.toContain('已答50/100');expect(html).toContain('下一页');expect(html).toContain('开始练习');
});
