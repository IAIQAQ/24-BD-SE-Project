<script setup lang="ts">
defineProps<{name:'practice'|'mistakes'|'family'|'profile'|'add'|'subtract'|'multiply'|'divide'|'check'|'arrow'}>();
const paths={practice:'M4 3h16v18H4z M8 8h8 M8 12h8 M8 16h4',mistakes:'M6 3h14v18H6z M3 7h6 M3 12h6 M3 17h6 M12 8h4 M12 13h4',family:'M8 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8 M16 11a3 3 0 1 0 0-6 M2 21v-3a6 6 0 0 1 12 0v3 M17 14a5 5 0 0 1 5 5v2',profile:'M12 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8 M4 21v-2a8 8 0 0 1 16 0v2',add:'M5 12h14 M12 5v14',subtract:'M5 12h14',multiply:'M6 6l12 12 M18 6L6 18',divide:'M5 12h14 M12 5h.01 M12 19h.01',check:'M5 12l4 4L19 6',arrow:'M5 12h14 M13 6l6 6-6 6'};
</script>
<template><svg class="ui-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" :stroke-width="name==='divide'?3:2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" focusable="false"><path :d="paths[name]"/></svg></template>
