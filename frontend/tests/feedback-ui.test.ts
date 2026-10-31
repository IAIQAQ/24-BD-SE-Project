import {beforeEach,expect,test,vi} from 'vitest';
import {ref,nextTick} from 'vue';
const fixture=vi.hoisted(()=>({state:null as any,sound:null as any}));
vi.mock('../src/composables/useLearningApp',()=>({useLearningApp:()=>fixture.state}));
vi.mock('../src/composables/useInteractionFeedback',()=>({useInteractionFeedback:()=>fixture.sound}));
vi.mock('../src/views/SproutCompanion.vue',async()=>{const {defineComponent,h}=await import('vue');return {default:defineComponent({props:['scene','celebrating'],setup:props=>()=>h('div',{'data-celebrating':!!props.celebrating},'companion')})};});
vi.mock('../src/views/IllustratedPracticeCard.vue',async()=>{const {defineComponent,h}=await import('vue');return {default:defineComponent({props:['record','busy'],emits:['open'],setup:(props,{emit})=>()=>h('button',{onClick:()=>emit('open',props.record.id)},'open-record')})};});
import LearningApp from '../src/views/LearningApp.vue';
import source from '../src/views/LearningApp.vue?raw';
import {renderUi} from './render-ui';
beforeEach(()=>{
 fixture.sound={enabled:ref(false),notice:ref(''),play:vi.fn(async()=>true),stop:vi.fn(),setEnabled:vi.fn()};
 fixture.state={feedback:ref(null),foreground:ref(true),profile:ref({id:'s',role:'STUDENT',nickname:'小朋友',grade:1}),username:ref(''),password:ref(''),page:ref('config'),error:ref(''),busy:ref(false),points:ref([]),normal:{selectedIds:ref([]),allocations:ref([])},records:ref([]),total:ref(1),draft:ref({id:'p',total:1,items:[{id:'i',raw:'2'}]}),current:ref(0),activeItem:ref({stem:'1 + 1 =',raw:'2'}),answered:ref(1),missing:ref(0),displayTime:ref('0:00'),saved:ref(''),dirty:ref(false),conflict:ref(false),result:ref({id:'p',practiceKind:'NORMAL',accuracy:'100.00',correct:1,total:1,mistakes:0,unanswered:0,elapsedMs:0,submittedAt:'2026-10-31',items:[]}),mistakes:ref(null),selectedMistake:ref(null),pending:ref(null),recordPage:ref(1),recordTotal:ref(0),mistakePage:ref(1),create:()=>{fixture.state.page.value='practice';}};
});
test('navigation triggered by start does not cancel its own tap sound',async()=>{const s=renderUi(LearningApp,source,()=>({}));try{const form=s.nodes.find(n=>n.tag==='form'&&n.parent)!;form.props.onSubmit({preventDefault(){}});await nextTick();expect(fixture.sound.play).toHaveBeenCalledWith('tap');expect(fixture.state.page.value).toBe('practice');expect(fixture.sound.stop).not.toHaveBeenCalled();}finally{s.app.unmount();}});
test('only fresh completion celebrates; background consumes it and old results stay static',async()=>{
 vi.useFakeTimers();const s=renderUi(LearningApp,source,()=>({}));try{fixture.state.page.value='result';fixture.state.feedback.value={kind:'completed',sequence:1,practiceId:'p'};await nextTick();const visible=()=>s.nodes.some(n=>n.parent&&n.props['data-celebrating']===true);expect(visible()).toBe(true);expect(fixture.sound.play).toHaveBeenCalledWith('completed');vi.advanceTimersByTime(1801);await nextTick();expect(visible()).toBe(false);fixture.state.page.value='home';await nextTick();fixture.state.page.value='result';await nextTick();expect(visible()).toBe(false);fixture.state.feedback.value={kind:'completed',sequence:2,practiceId:'p'};await nextTick();expect(visible()).toBe(true);fixture.state.foreground.value=false;await nextTick();expect(visible()).toBe(false);fixture.state.foreground.value=true;await nextTick();expect(visible()).toBe(false);}finally{s.app.unmount();vi.useRealTimers();}
});

test('looking at a completed record is silent, not a start/continue tap',async()=>{
 fixture.state.page.value='home';fixture.state.records.value=[{id:'old-result',status:'COMPLETED',total:5}];fixture.state.showResult=vi.fn();const s=renderUi(LearningApp,source,()=>({}));try{s.button('open-record')!.props.onClick();await nextTick();expect(fixture.state.showResult).toHaveBeenCalledWith('old-result');expect(fixture.sound.play).not.toHaveBeenCalled();}finally{s.app.unmount();}
});
