<script setup lang="ts">
import { ref } from 'vue';
import { health,login,me,logout,ApiError,type UserProfile } from './api/client';
const username=ref(''),password=ref(''),status=ref('尚未连接'),error=ref(''),busy=ref(false),profile=ref<UserProfile|null>(null);
async function run(action:()=>Promise<void>){if(busy.value)return;busy.value=true;error.value='';try{await action();}catch(e){if(e instanceof ApiError && e.status===401){profile.value=null;status.value='请重新登录';}error.value=e instanceof Error?e.message:'操作失败，请重试';}finally{busy.value=false;}}
const connect=()=>run(async()=>{status.value=(await health()).status==='UP'?'服务已连接':'服务尚未就绪';});
const signIn=()=>run(async()=>{profile.value=await login(username.value,password.value);password.value='';status.value='已通过真实账号验证';});
const restore=()=>run(async()=>{profile.value=await me();status.value='已恢复服务端登录状态';});
const signOut=()=>run(async()=>{await logout();profile.value=null;password.value='';status.value='已退出，原会话已撤销';});
</script>
<template>
  <main>
    <header><span class="logo">芽</span><b>小芽口算</b><span class="tag">环境验证版</span></header>
    <p class="eyebrow">第一步 · 建立真实连接</p><h1>从一次可靠的<br>连接开始。</h1>
    <p class="intro">这一版先验证服务连接与账号登录。<br>练习功能将在验证通过后接入。</p>
    <section class="connection"><span :class="{online:status.includes('已')}"></span>{{status}}<button :disabled="busy" @click="connect">检查连接</button></section>
    <form v-if="!profile" @submit.prevent="signIn">
      <label for="username">学生账号</label><input id="username" v-model="username" autocomplete="username" required placeholder="输入测试学生账号">
      <label for="password">密码</label><input id="password" v-model="password" type="password" autocomplete="current-password" required placeholder="输入测试密码">
      <button class="primary" :disabled="busy">{{busy?'正在验证…':'登录学习账号'}}</button>
    </form>
    <section v-else class="profile"><span class="avatar">🌱</span><h2>{{profile.nickname}}，连接成功。</h2><p>{{profile.loginName}} · {{profile.grade}} 年级 · 学生</p><p class="note">账号信息来自真实后端，<br>不会在本机模拟成绩或题目。</p><button class="primary" :disabled="busy" @click="signOut">退出登录</button></section>
    <p v-if="error" class="error" role="alert">{{error}}</p><button class="secondary" :disabled="busy" @click="restore">检查已有登录状态</button>
    <footer>仅连接隔离测试服务 · 不内置账号密码<br>最小 APK 与认证验证，不代表首版全部完成</footer>
  </main>
</template>
<style>
*{box-sizing:border-box}body{margin:0;background:#f4f7ef;color:#294d3b;font-family:"Microsoft YaHei",sans-serif}main{max-width:440px;margin:auto;padding:calc(25px + env(safe-area-inset-top,0px)) 25px calc(25px + env(safe-area-inset-bottom,0px));min-height:100dvh}header{display:flex;align-items:center;gap:9px;font-size:18px}.logo{background:#285b49;color:#e0edba;width:35px;height:35px;border-radius:12px;display:grid;place-items:center;font-size:15px}.tag{font-size:10px;background:#e2ebd4;border-radius:20px;padding:5px 8px;margin-left:auto;color:#6e855b}.eyebrow{margin:42px 0 13px;font-size:11px;letter-spacing:2px;color:#83986b}h1{font-size:29px;line-height:1.55;margin:0 0 16px;font-weight:600}.intro{font-size:12px;line-height:1.9;color:#7b8c6d;margin:0 0 27px}.connection{display:flex;align-items:center;gap:8px;padding:15px 12px;background:#eaf0df;border-radius:13px;font-size:11px;margin-bottom:27px}.connection span{height:6px;width:6px;border-radius:50%;background:#b5bda6}.connection .online{background:#6b9752}.connection button{margin-left:auto;border:0;background:none;color:#5e7f46;font-size:11px;padding:7px}label{display:block;font-size:12px;margin:17px 0 9px}input{width:100%;padding:14px;border:1px solid #dce5d0;border-radius:12px;background:white;color:#294d3b;font:inherit;font-size:14px;min-height:49px}input:focus{outline:2px solid #9bb87b;outline-offset:2px}input::placeholder{color:#a8b49b;font-size:12px}button{font:inherit;cursor:pointer}button:disabled{opacity:.5;cursor:wait}.primary{margin-top:23px;width:100%;padding:15px;background:#285b49;color:white;border:0;border-radius:13px;font-size:14px}.secondary{width:100%;border:1px solid #dce5d0;background:transparent;border-radius:12px;padding:13px;color:#6b8653;font-size:12px;margin-top:15px}.error{font-size:12px;line-height:1.8;color:#a5523f;padding:12px;background:#f7eae2;border-radius:10px}footer{text-align:center;font-size:10px;line-height:1.9;color:#9aa78c;margin-top:30px}.profile{text-align:center;background:white;border:1px solid #e1e8d8;border-radius:17px;padding:22px 18px}.avatar{font-size:32px}.profile h2{font-size:20px;font-weight:500}.profile p{font-size:12px;color:#849573;line-height:1.9}.profile .note{margin:20px 0}button:focus-visible{outline:3px solid #8ca768;outline-offset:3px}
</style>
