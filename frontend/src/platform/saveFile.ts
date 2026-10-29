import {Capacitor} from '@capacitor/core';
import {Directory,Filesystem} from '@capacitor/filesystem';
import {Share} from '@capacitor/share';
export async function saveFile(blob:Blob,name:string){
  if(Capacitor.isNativePlatform()){
    const base64=await new Promise<string>((resolve,reject)=>{const reader=new FileReader();reader.onload=()=>resolve(String(reader.result).split(',')[1]);reader.onerror=()=>reject(new Error('文件读取失败'));reader.readAsDataURL(blob);});
    const file=await Filesystem.writeFile({path:name,data:base64,directory:Directory.Cache});await Share.share({title:name,files:[file.uri],dialogTitle:'保存或分享题库模板'});
  }else{const url=URL.createObjectURL(blob);const link=document.createElement('a');link.href=url;link.download=name;link.click();setTimeout(()=>URL.revokeObjectURL(url),1000);}
}
