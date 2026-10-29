package cn.edu.hust.mathdrill;

import android.webkit.WebView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import org.json.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class I3LearningFlowTest {
 private String eval(WebView view,String script)throws Exception{
  var latch=new CountDownLatch(1);var value=new AtomicReference<String>();
  InstrumentationRegistry.getInstrumentation().runOnMainSync(()->view.evaluateJavascript(script,r->{value.set(r);latch.countDown();}));
  assertTrue(latch.await(10,TimeUnit.SECONDS));return value.get();
 }
 @Test public void studentAndParentReinforcementUseSharedAnswerFlow()throws Exception{
  var context=InstrumentationRegistry.getInstrumentation().getTargetContext();JSONObject credentials;
  try(var scanner=new java.util.Scanner(new java.io.File(context.getExternalFilesDir(null),"i1-probe.json"),"UTF-8")){credentials=new JSONObject(scanner.useDelimiter("\\A").next());}
  try(var scenario=ActivityScenario.<MainActivity>launch(new android.content.Intent(context,MainActivity.class))){
   var view=new AtomicReference<WebView>();scenario.onActivity(a->view.set(a.getBridge().getWebView()));
   long deadline=System.currentTimeMillis()+20000;while(!"\"complete\"".equals(eval(view.get(),"document.readyState"))&&System.currentTimeMillis()<deadline)Thread.sleep(100);
   String script="""
    window.__i3Learning={done:false};
    (async()=>{
     const wait=async(check,label)=>{const end=Date.now()+20000;while(Date.now()<end){if(check())return;await new Promise(r=>setTimeout(r,70));}throw new Error('Timeout '+label+' / '+document.body.innerText.slice(0,1000));};
     const text=()=>document.body.innerText;
     const button=t=>[...document.querySelectorAll('button')].find(b=>b.innerText.trim()===t);
     const click=async t=>{await wait(()=>button(t)&&!button(t).disabled,t);button(t).click();};
     const fill=(id,value)=>{const input=document.getElementById(id);if(!input)throw new Error('Missing '+id);input.value=value;input.dispatchEvent(new Event('input',{bubbles:true}));};
     const login=async(name,heading)=>{fill('username',name);fill('password',PASSWORD);await click('登录学习账号');await wait(()=>text().includes(heading),'role '+heading);};
     const logout=async()=>{await click('退出登录');await wait(()=>button('登录学习账号'),'logout');};
     const answerFor=stem=>{const m=stem.match(/(\\d+)\\s*\\+\\s*(\\d+)/);if(!m)throw new Error('Expected independently calculable addition: '+stem);return String(Number(m[1])+Number(m[2]));};
     const draft=()=>document.getElementById('answer')&&!document.getElementById('answer').disabled;
     const latestRecord=(kind,status)=>[...document.querySelectorAll('button.record')].find(b=>b.innerText.includes(kind)&&b.innerText.includes(status));
     await wait(()=>button('退出登录')||button('登录学习账号'),'initial');if(button('退出登录'))await logout();
     await login('student_a','开始新练习');await click('开始新练习');fill('total','1');await click('生成练习，开始作答');await wait(draft,'original draft');fill('answer','999');await click('结束并检查');await click('确认提交');await wait(()=>text().includes('0.00'),'original wrong');
     await click('查看我的错题');await wait(()=>document.querySelector('button.record'),'wrong list');const oldCount=text().match(/累计错误 (\\d+) 次/)[1];document.querySelector('button.record').click();await click('重练或强化这道题');await wait(()=>document.querySelector('input[id^="reinforce-"]'),'student reinforcement config');
     const allocationId=document.querySelector('input[id^="reinforce-"]').id;fill(allocationId,'1');await click('返回选择');await click('重练或强化这道题');await wait(()=>document.getElementById(allocationId),'reopened config');if(document.getElementById(allocationId).value!=='1')throw new Error('Reopening lost allocation');
     await click('开始同类强化');await click('取消');await click('开始同类强化');await click('确认操作');await wait(draft,'student reinforcement draft');if(document.getElementById('answer').value!=='')throw new Error('Nonblank new answer');const studentStem=document.querySelector('.question h1').innerText;const studentAnswer=answerFor(studentStem);fill('answer',studentAnswer);await click('手动保存');await wait(()=>text().includes('已保存到服务端'),'saved');await click('‹ 保存退出');
     await wait(()=>latestRecord('同类强化','进行中')&&!latestRecord('同类强化','进行中').disabled,'saved record');latestRecord('同类强化','进行中').click();await wait(()=>draft()||button('在此继续（重新载入草稿）'),'resume state');if(!draft())await click('在此继续（重新载入草稿）');await wait(draft,'restored draft');if(document.querySelector('.question h1').innerText!==studentStem||document.getElementById('answer').value!==studentAnswer)throw new Error('Restore mismatch');await click('结束并检查');await click('确认提交');await wait(()=>text().includes('100.00')&&text().includes('同类强化'),'student result');await click('查看我的错题');await wait(()=>text().includes('累计错误 '+oldCount+' 次'),'old errors retained');await logout();
     await login('parent_demo','家长学习中心');await wait(()=>document.getElementById('childSelect')?.value&&!button('刷新孩子和任务').disabled,'existing family');await click('孩子错题');await wait(()=>button('强化这道题'),'parent mistakes');await click('强化这道题');await wait(()=>document.querySelector('input[id^="reinforce-"]'),'parent config');fill(document.querySelector('input[id^="reinforce-"]').id,'1');await click('生成强化题目预览');await wait(()=>button('确认发送以上题目'),'fixed preview');const previewStem=document.querySelector('ol li').innerText;
     await click('确认发送以上题目');await click('取消');await click('确认发送以上题目');await click('确认操作');await wait(()=>text().includes('待完成 · 家长安排')&&!button('刷新孩子和任务').disabled,'sent');await logout();
     await login('student_a','开始新练习');await wait(()=>latestRecord('同类强化','待完成'),'received task');latestRecord('同类强化','待完成').click();await wait(draft,'assigned draft');if(document.querySelector('.question h1').innerText!==previewStem)throw new Error('Fixed preview changed');fill('answer',answerFor(previewStem));await click('手动保存');await wait(()=>text().includes('已保存到服务端'),'assigned save');await click('结束并检查');await click('确认提交');await wait(()=>text().includes('100.00')&&text().includes('同类强化'),'assigned result');await logout();
     await login('parent_demo','家长学习中心');await wait(()=>document.getElementById('childSelect')?.value&&!button('刷新孩子和任务').disabled,'parent refreshed');await click('练习任务');await click('查看任务与结果');await wait(()=>text().includes('正确率 100.00%')&&text().includes(previewStem),'parent result');await click('孩子错题');await click('查看历次作答');await wait(()=>text().includes('孩子历次作答'),'parent history');await click('学情');await wait(()=>text().includes('每日表现')&&text().includes('知识点表现'),'parent stats');
     return {studentReinforcement:true,reopenedAllocation:1,cancelPreserved:true,blankAnswer:true,savedRestored:true,oldErrorsRetained:true,parentFixedPreview:true,previewMatched:true,parentResult:true,parentHistory:true,parentStats:true,accuracy:'100.00'};
    })().then(data=>window.__i3Learning={done:true,data}).catch(e=>window.__i3Learning={done:true,error:String(e)});
    """.replace("PASSWORD",JSONObject.quote(credentials.getString("password")));
   eval(view.get(),script);JSONObject outcome=null;deadline=System.currentTimeMillis()+240000;
   while(System.currentTimeMillis()<deadline){String raw=eval(view.get(),"JSON.stringify(window.__i3Learning||{})");outcome=new JSONObject((String)new JSONTokener(raw).nextValue());if(outcome.optBoolean("done"))break;Thread.sleep(200);}
   assertNotNull(outcome);assertTrue("Probe timed out",outcome.optBoolean("done"));assertFalse(outcome.optString("error"),outcome.has("error"));System.out.println("I3_LEARNING_PROBE "+outcome.getJSONObject("data"));
   var bitmap=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();try(var output=new java.io.FileOutputStream(new java.io.File(context.getExternalFilesDir(null),"i3-learning.png"))){bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,output);}finally{bitmap.recycle();}
  }
 }
}
