<script setup lang="ts">
import {computed,ref,watch} from 'vue';
import welcome from '../assets/sprout/p2-scene/welcome.webp';
import complete from '../assets/sprout/p2-scene/complete.webp';
const props=defineProps<{scene:'welcome'|'complete'|'empty';celebrating?:boolean}>();
const failed=ref(false);
const imageUrl=computed(()=>props.scene==='complete'?complete:welcome);
const message=computed(()=>({welcome:'小芽陪你想一想。',complete:'认真完成，真棒！',empty:'从一道题开始吧。'}[props.scene]));
watch(()=>props.scene,()=>{failed.value=false;});
</script>
<template><div class="sprout-companion" :class="[`sprout-${scene}`,{'is-celebrating':scene==='complete'&&celebrating}]"><div class="sprout-picture" aria-hidden="true"><span v-if="celebrating&&scene==='complete'" class="celebration-sparks" aria-hidden="true"><i>✦</i><i>✦</i><i>✦</i></span><img v-if="!failed" :key="imageUrl" :src="imageUrl" width="512" height="512" alt="" decoding="async" @error="failed=true"><span v-else class="sprout-fallback">芽</span></div><p><slot>{{message}}</slot></p></div></template>
<style scoped>
.sprout-companion{display:flex;align-items:center;gap:14px;max-width:100%;pointer-events:none}
.sprout-picture{width:120px;aspect-ratio:1;flex:0 0 120px;display:grid;place-items:center}
.sprout-picture img{display:block;width:100%;height:100%;object-fit:contain}
.sprout-fallback{display:grid;place-items:center;width:56px;height:56px;border-radius:20px;background:#e8f0dd;color:#285b49;font-size:28px}
.sprout-companion p{margin:0;font-size:16px;line-height:1.6;color:#48683e;font-weight:500}
.sprout-welcome{flex-direction:column;gap:0;text-align:center;flex:0 0 128px}
.sprout-welcome .sprout-picture{width:128px;flex-basis:128px}
.sprout-welcome p{font-size:14px}
.sprout-complete{justify-content:center;margin:10px 0 18px;background:#edf3e5;border-radius:24px;padding:8px 16px}
.sprout-empty{justify-content:center;margin:12px 0}
.sprout-empty .sprout-picture{width:88px;flex-basis:88px}
@media(max-width:380px){.sprout-welcome{flex-basis:104px}.sprout-welcome .sprout-picture{width:104px;flex-basis:104px}.sprout-companion{gap:10px}.sprout-welcome{gap:0}.sprout-complete .sprout-picture{width:96px;flex-basis:96px}}
.sprout-picture{position:relative}
.celebration-sparks{position:absolute;inset:0;pointer-events:none;color:#e7ba49}
.celebration-sparks i{position:absolute;font-style:normal;font-size:22px;animation:sprout-spark 1.8s ease-out 1}
.celebration-sparks i:nth-child(1){left:0;top:14%}.celebration-sparks i:nth-child(2){right:0;top:2%;animation-delay:80ms}.celebration-sparks i:nth-child(3){right:4%;bottom:15%;font-size:16px}
.is-celebrating .sprout-picture img{animation:sprout-celebrate 1.8s ease-out 1}
@keyframes sprout-celebrate{0%,100%{transform:translateY(0)}20%{transform:translateY(-8px) rotate(-3deg)}40%{transform:translateY(0) rotate(2deg)}60%{transform:translateY(-4px)}80%{transform:translateY(0)}}
@keyframes sprout-spark{0%{opacity:0;transform:scale(.7)}25%{opacity:1;transform:scale(1)}100%{opacity:0;transform:scale(1.1) translateY(-8px)}}
@media(prefers-reduced-motion:reduce){.is-celebrating .sprout-picture img{animation:none}.celebration-sparks{display:none}}
</style>
