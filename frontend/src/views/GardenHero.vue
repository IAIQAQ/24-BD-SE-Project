<script setup lang="ts">
import {ref} from 'vue';
import UiIcon from './UiIcon.vue';
import garden from '../assets/sprout/p2-scene/garden.webp';
import SproutWelcome from './SproutWelcome.vue';
withDefaults(defineProps<{busy:boolean;active?:boolean}>(),{active:true});
defineEmits<{start:[]}>();
const backgroundFailed=ref(false);
</script>
<template><section class="garden-hero" aria-label="口算练习"><img v-if="!backgroundFailed" class="garden-background" :src="garden" width="1024" height="512" alt="" @error="backgroundFailed=true"><div class="garden-copy"><h2>一起练口算</h2><p>小芽陪你想一想</p><button class="garden-start" type="button" :disabled="busy" @click="$emit('start')">开始练习<UiIcon name="arrow"/></button></div><div class="garden-character" aria-hidden="true"><SproutWelcome class="garden-mascot" :active="active"/></div></section></template>
<style scoped>
.garden-hero{position:relative;isolation:isolate;width:100%;min-width:0;display:grid;grid-template-columns:minmax(0,1.42fr) minmax(0,1fr);gap:4px;align-items:center;min-height:184px;aspect-ratio:2.05;padding:22px 20px;border-radius:25px;overflow:hidden;background:linear-gradient(120deg,#e9f0db,#dff1df);margin:18px 0 26px}
.garden-background{position:absolute;inset:0;width:100%;height:100%;object-fit:cover;object-position:center bottom;z-index:-1;pointer-events:none}
.garden-copy{min-width:0;z-index:1}
.garden-hero h2{font-size:clamp(26px,8vw,44px);line-height:1.3;white-space:nowrap;margin:0;font-weight:700;letter-spacing:-1px;color:#174d3a}
.garden-hero p{font-size:clamp(14px,3.8vw,19px);color:#55765b;margin:9px 0 17px;white-space:nowrap}
.learning-ui .garden-start{display:flex;align-items:center;justify-content:center;gap:8px;width:100%;min-height:50px;padding:10px 10px;border:0;border-radius:30px;background:linear-gradient(140deg,#367657,#225740);box-shadow:0 4px 8px #285b4920;color:white;font-size:19px;font-weight:600}
.garden-character{align-self:end;display:grid;place-items:end center;min-width:0;width:100%;aspect-ratio:1;pointer-events:none}
.garden-character .garden-mascot{width:116%;max-width:160px;height:auto;display:block;object-fit:contain;transform:translate(0,8px)}
@media(max-width:360px){.garden-hero{padding:18px 14px;min-height:176px}.garden-hero h2{font-size:25px}.garden-hero p{font-size:14px}.learning-ui .garden-start{font-size:17px;gap:5px;padding:10px 6px}.garden-character .garden-mascot{width:113%}}
@media(min-width:600px){.garden-hero{padding:30px 32px;min-height:240px}.garden-character .garden-mascot{max-width:230px}.learning-ui .garden-start{max-width:245px}}
</style>
