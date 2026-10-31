import {ref} from 'vue';
export const confirmation=ref<string|null>(null);
let resolvePending:((accepted:boolean)=>void)|null=null;
export function confirmAction(message:string):Promise<boolean>{
  if(resolvePending)return Promise.resolve(false);
  confirmation.value=message;
  return new Promise(resolve=>{resolvePending=resolve;});
}
export function finishConfirmation(accepted:boolean){
  const resolve=resolvePending;resolvePending=null;confirmation.value=null;resolve?.(accepted);
}
