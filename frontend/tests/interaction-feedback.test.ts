import {effectScope,ref} from 'vue';
import {afterEach,beforeEach,expect,test,vi} from 'vitest';
import {useInteractionFeedback} from '../src/composables/useInteractionFeedback';
let scopes:ReturnType<typeof effectScope>[];let contexts:any[];let stored:string|null;
class AudioMock{
 state='running';currentTime=0;destination={};voices:any[]=[];resume=vi.fn(async()=>{});suspend=vi.fn(async()=>{});close=vi.fn(async()=>{});
 constructor(){contexts.push(this);}
 createOscillator(){const voice={frequency:{setValueAtTime:vi.fn()},connect:vi.fn(),disconnect:vi.fn(),start:vi.fn(),stop:vi.fn(),onended:null};this.voices.push(voice);return voice;}
 createGain(){return {gain:{setValueAtTime:vi.fn(),linearRampToValueAtTime:vi.fn()},connect:vi.fn(),disconnect:vi.fn()};}
}
const setup=(active=ref(true))=>{const scope=effectScope();scopes.push(scope);return {active,sound:scope.run(()=>useInteractionFeedback(active))!};};
beforeEach(()=>{scopes=[];contexts=[];stored=null;vi.stubGlobal('AudioContext',AudioMock);vi.stubGlobal('localStorage',{getItem:()=>stored,setItem:(_k:string,v:string)=>{stored=v;}});});
afterEach(()=>{scopes.forEach(s=>s.stop());vi.unstubAllGlobals();vi.restoreAllMocks();});
test('default off, explicit enable, local preference and immediate disable',async()=>{const {sound}=setup();expect(sound.enabled.value).toBe(false);expect(await sound.play('tap')).toBe(false);expect(contexts).toHaveLength(0);await sound.setEnabled(true);expect(sound.enabled.value).toBe(true);expect(stored).toBe('true');expect(await sound.play('saved')).toBe(true);const voice=contexts[0].voices[0];await sound.setEnabled(false);expect(voice.stop).toHaveBeenCalled();expect(await sound.play('completed')).toBe(false);expect(stored).toBe('false');});
test('remembered preference creates no context until a gesture; rapid taps and success priority avoid overlap',async()=>{stored='true';const {sound}=setup();expect(sound.enabled.value).toBe(true);expect(contexts).toHaveLength(0);expect(await sound.play('tap')).toBe(true);expect(await sound.play('tap')).toBe(false);expect(await sound.play('completed')).toBe(true);expect(await sound.play('tap')).toBe(false);expect(contexts[0].voices).toHaveLength(2);expect(contexts[0].voices[0].stop.mock.calls.length).toBeGreaterThan(1);});
test('storage errors and unavailable or rejected audio are optional, never business failures',async()=>{vi.stubGlobal('localStorage',{getItem:()=>{throw Error('blocked');},setItem:()=>{throw Error('blocked');}});const {sound}=setup();expect(sound.enabled.value).toBe(false);await sound.setEnabled(true);expect(sound.enabled.value).toBe(true);expect(sound.notice.value).toBeTruthy();contexts[0].resume.mockRejectedValue(new Error('denied'));expect(await sound.play('saved')).toBe(false);expect(sound.enabled.value).toBe(false);});
test('background stops now, return does not replay, pending resume cannot start after stop',async()=>{const {sound,active}=setup();await sound.setEnabled(true);await sound.play('saved');const ctx=contexts[0];active.value=false;expect(ctx.voices[0].stop).toHaveBeenCalled();expect(await sound.play('completed')).toBe(false);active.value=true;expect(ctx.voices).toHaveLength(1);let done!:()=>void;ctx.resume.mockImplementation(()=>new Promise<void>(r=>done=r));const pending=sound.play('saved');sound.stop();done();expect(await pending).toBe(false);expect(ctx.voices).toHaveLength(1);});
test('unmount closes resources and unsupported audio cannot claim enabled',async()=>{const {sound}=setup();await sound.setEnabled(true);scopes[0].stop();expect(contexts[0].close).toHaveBeenCalled();vi.stubGlobal('AudioContext',undefined);const other=setup().sound;await other.setEnabled(true);expect(other.enabled.value).toBe(false);expect(other.notice.value).toBeTruthy();});

test('pending enable cannot turn sound back on after closing or backgrounding',async()=>{
 for(const mode of ['off','background']){
  let release!:()=>void;
  class DelayedAudio extends AudioMock{resume=vi.fn(()=>new Promise<void>(resolve=>release=resolve));}
  vi.stubGlobal('AudioContext',DelayedAudio);const {sound,active}=setup();const enabling=sound.setEnabled(true);expect(sound.enabled.value).toBe(false);
  if(mode==='off')await sound.setEnabled(false);else active.value=false;
  release();await enabling;expect(sound.enabled.value).toBe(false);expect(contexts.at(-1).voices).toHaveLength(0);
 }
});
