<script setup lang="ts">
import {ref} from 'vue';
import welcome from '../assets/sprout/p2-scene/welcome.webp';
import avatar from '../assets/sprout/p2-scene/avatar.webp';
defineProps<{nickname:string;grade:number|null;disabled?:boolean}>();
defineEmits<{openProfile:[]}>();
const logoFailed=ref(false),avatarFailed=ref(false);
</script>
<template><header class="student-header"><span class="student-brand"><img v-if="!logoFailed" :src="welcome" width="48" height="48" alt="" @error="logoFailed=true"><span v-else class="brand-fallback" aria-hidden="true">芽</span><strong>小芽口算</strong></span><span class="student-grade">{{({'1':'一','2':'二','3':'三'} as Record<string,string>)[String(grade)]??grade}}年级</span><button class="student-avatar" type="button" :disabled="disabled" aria-label="我的资料" :title="nickname+'的资料'" @click="$emit('openProfile')"><img v-if="!avatarFailed" :src="avatar" width="48" height="48" alt="" @error="avatarFailed=true"><span v-else aria-hidden="true">我</span></button></header></template>
<style scoped>
.learning-ui .student-header{display:flex;gap:8px;align-items:center;margin:0 0 24px}
.student-brand{display:flex;align-items:center;gap:6px;margin-right:auto;min-width:0}
.student-brand img{width:35px;height:42px;object-fit:contain}
.student-brand strong{font-size:23px;white-space:nowrap;letter-spacing:0;color:#164b36}
.brand-fallback{width:35px;color:#285b49;text-align:center;font-size:24px}
.student-grade{font-size:15px;font-weight:600;color:#285b49;background:#e9f1d9;border-radius:22px;padding:8px 12px;white-space:nowrap}
.learning-ui .student-avatar{display:grid;place-items:center;width:48px;height:48px;padding:0;flex-shrink:0;border-radius:50%;overflow:hidden;background:#e6efce;color:#285b49}
.student-avatar img{width:100%;height:100%;object-fit:contain}
@media(max-width:360px){.student-brand strong{font-size:21px}.student-brand img{width:30px}.student-grade{font-size:14px;padding:7px 9px}.learning-ui .student-header{gap:6px}}
</style>
