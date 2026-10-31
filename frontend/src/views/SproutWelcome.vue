<script setup lang="ts">
import {computed,onMounted,onUnmounted,ref,watch} from 'vue';
import welcome from '../assets/sprout/p2-scene/welcome.webp';
const props=withDefaults(defineProps<{active?:boolean}>(),{active:true});
const loaded=ref(false),failed=ref(false),stopped=ref(!props.active);
const moving=computed(()=>props.active&&loaded.value&&!failed.value&&!stopped.value);
watch(()=>props.active,value=>{if(!value)stopped.value=true;});
function visibility(){if(document.hidden)stopped.value=true;}
onMounted(()=>{if(typeof document!=='undefined'){visibility();document.addEventListener('visibilitychange',visibility);}});
onUnmounted(()=>{if(typeof document!=='undefined')document.removeEventListener('visibilitychange',visibility);});
</script>
<template><span class="leaf-welcome" :class="{'leaf-welcome-active':moving}" aria-hidden="true"><template v-if="!failed"><img class="sprout-body-layer" :src="welcome" width="384" height="384" alt="" @load="loaded=true" @error="failed=true"><img class="sprout-leaf-layer" :src="welcome" width="384" height="384" alt="" @error="failed=true" @animationend="stopped=true"></template><span v-else class="leaf-fallback">芽</span></span></template>
<style scoped>
.leaf-welcome{position:relative;display:block;aspect-ratio:1;pointer-events:none}
.leaf-welcome img{display:block;width:100%;height:100%;object-fit:contain}
/* Both layers share the same raster; a tiny stem overlap avoids a moving seam. */
.sprout-body-layer{clip-path:inset(36.7% 0 0)}
.leaf-welcome .sprout-leaf-layer{position:absolute;inset:0;clip-path:inset(0 0 63%);transform-origin:50% 37%}
.leaf-welcome-active .sprout-leaf-layer{animation:sprout-leaf-hello 2.4s ease-in-out 1}
.leaf-fallback{display:grid;place-items:center;width:100%;height:100%;border-radius:30%;background:#d9eac5;color:#285b49;font-size:40px}
@keyframes sprout-leaf-hello{0%,100%{transform:rotate(0)}20%{transform:rotate(-3deg)}45%{transform:rotate(3deg)}70%{transform:rotate(-2deg)}85%{transform:rotate(1deg)}}
@media(prefers-reduced-motion:reduce){.leaf-welcome-active .sprout-leaf-layer{animation:none}}
</style>
