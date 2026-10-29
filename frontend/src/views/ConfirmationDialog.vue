<script setup lang="ts">
import {nextTick,onUnmounted,ref,watch} from 'vue';
import {confirmation,finishConfirmation} from '../composables/confirmation';
const dialog=ref<HTMLDialogElement>();
watch(confirmation,async message=>{await nextTick();if(message!==null)dialog.value?.showModal();else dialog.value?.close();});
onUnmounted(()=>finishConfirmation(false));
</script>
<template><dialog ref="dialog" aria-labelledby="confirmationTitle" aria-describedby="confirmationText" @cancel.prevent="finishConfirmation(false)"><h2 id="confirmationTitle">请确认操作</h2><p id="confirmationText">{{confirmation}}</p><div class="two"><button class="secondary" autofocus @click="finishConfirmation(false)">取消</button><button class="primary" @click="finishConfirmation(true)">确认操作</button></div></dialog></template>
<style scoped>dialog{border:0;border-radius:20px;padding:24px;max-width:min(420px,calc(100vw - 48px));box-shadow:0 12px 48px #183c3033}dialog::backdrop{background:#102c2466}p{white-space:pre-line;line-height:1.7}</style>
