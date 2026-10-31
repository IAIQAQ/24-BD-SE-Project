import {onScopeDispose,readonly,ref,watch,type Ref} from 'vue';
type SoundKind='tap'|'saved'|'completed';
const preferenceKey='mathdrill-hint-sound';
export function useInteractionFeedback(active:Readonly<Ref<boolean>>){
  const enabled=ref(false),notice=ref('');
  try{enabled.value=localStorage.getItem(preferenceKey)==='true';}catch{/* Device preferences are optional. */}
  let context:AudioContext|null=null,voice:OscillatorNode|null=null,gain:GainNode|null=null;
  let generation=0,disposed=false,lastTap=-Infinity,priorityUntil=0;
  function persist(value:boolean){try{localStorage.setItem(preferenceKey,String(value));}catch{notice.value='本机暂时无法记住提示音设置';}}
  function stop(){generation++;priorityUntil=0;if(voice){try{voice.stop();}catch{}voice.disconnect();voice=null;}gain?.disconnect();gain=null;}
  function audio(){if(!context){if(typeof AudioContext==='undefined')throw new Error('audio unavailable');context=new AudioContext();}return context;}
  function fail(){stop();enabled.value=false;persist(false);notice.value='提示音暂不可用，可以继续练习';}
  async function setEnabled(value:boolean){
    stop();enabled.value=false;notice.value='';if(!value){persist(false);return;}
    if(!active.value||disposed)return;const ticket=generation;
    try{await audio().resume();if(ticket!==generation||disposed||!active.value)return;enabled.value=true;persist(true);}catch{if(ticket===generation)fail();}
  }
  async function play(kind:SoundKind):Promise<boolean>{
    if(!enabled.value||!active.value||disposed)return false;
    const now=performance.now();if(kind==='tap'&&(now-lastTap<120||now<priorityUntil))return false;
    if(kind==='tap')lastTap=now;
    stop();const ticket=generation,duration=kind==='tap'?.06:kind==='saved'?.14:.45;
    if(kind!=='tap')priorityUntil=now+duration*1000;
    try{
      const ctx=audio();await ctx.resume();if(ticket!==generation||disposed||!active.value||!enabled.value)return false;
      const oscillator=ctx.createOscillator(),volume=ctx.createGain(),start=ctx.currentTime;
      voice=oscillator;gain=volume;oscillator.type='sine';
      const frequencies=kind==='tap'?[660]:kind==='saved'?[523]:[523,659,784];
      frequencies.forEach((frequency,i)=>oscillator.frequency.setValueAtTime(frequency,start+i*duration/frequencies.length));
      volume.gain.setValueAtTime(0,start);volume.gain.linearRampToValueAtTime(.025,start+.01);volume.gain.linearRampToValueAtTime(0,start+duration);
      oscillator.connect(volume);volume.connect(ctx.destination);
      oscillator.onended=()=>{oscillator.disconnect();volume.disconnect();if(voice===oscillator){voice=null;gain=null;}};
      oscillator.start(start);oscillator.stop(start+duration);return true;
    }catch{if(ticket===generation)fail();return false;}
  }
  watch(active,value=>{if(!value){stop();if(context)void context.suspend().catch(()=>{});}},{flush:'sync'});
  onScopeDispose(()=>{disposed=true;stop();if(context)void context.close().catch(()=>{});});
  return {enabled:readonly(enabled),notice:readonly(notice),setEnabled,play,stop};
}
