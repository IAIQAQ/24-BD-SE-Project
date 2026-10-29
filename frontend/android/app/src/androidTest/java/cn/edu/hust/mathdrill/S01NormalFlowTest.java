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
public class S01NormalFlowTest {
 private String eval(WebView view,String script)throws Exception{
  var latch=new CountDownLatch(1);var value=new AtomicReference<String>();
  InstrumentationRegistry.getInstrumentation().runOnMainSync(()->view.evaluateJavascript(script,r->{value.set(r);latch.countDown();}));
  assertTrue(latch.await(10,TimeUnit.SECONDS));return value.get();
 }
 @Test public void studentAndParentNormalMultiPointFlow()throws Exception{
  var context=InstrumentationRegistry.getInstrumentation().getTargetContext();JSONObject credentials;
  try(var scanner=new java.util.Scanner(new java.io.File(context.getExternalFilesDir(null),"i1-probe.json"),"UTF-8")){credentials=new JSONObject(scanner.useDelimiter("\\A").next());}
  try(var scenario=ActivityScenario.<MainActivity>launch(new android.content.Intent(context,MainActivity.class))){
   var view=new AtomicReference<WebView>();scenario.onActivity(a->view.set(a.getBridge().getWebView()));
   long deadline=System.currentTimeMillis()+20000;while(!"\"complete\"".equals(eval(view.get(),"document.readyState"))&&System.currentTimeMillis()<deadline)Thread.sleep(100);
   String script="""
    window.__s01Normal={done:false};
    (async()=>{
     const points=POINTS;
     const wait=async(check,label)=>{const end=Date.now()+20000;while(Date.now()<end){if(check())return;await new Promise(r=>setTimeout(r,70));}throw new Error('Timeout '+label+' / '+document.body.innerText.slice(0,800));};
     const text=()=>document.body.innerText,button=t=>[...document.querySelectorAll('button')].find(b=>b.innerText.trim()===t);
     const click=async t=>{await wait(()=>button(t)&&!button(t).disabled,t);button(t).click();};
     const fill=(id,value)=>{const input=document.getElementById(id);if(!input)throw new Error('Missing '+id);input.value=value;input.dispatchEvent(new Event('input',{bubbles:true}));};
     const login=async(name,heading)=>{fill('username',name);fill('password',PASSWORD);await click('登录学习账号');await wait(()=>text().includes(heading),'login '+heading);};
     const logout=async()=>{await click('退出登录');await wait(()=>button('登录学习账号'),'logout');};
     const draft=()=>document.getElementById('answer')&&!document.getElementById('answer').disabled;
     const configure=async prefix=>{
      await wait(()=>document.getElementById(prefix+'-point-'+points[1].id),'points');
      for(const input of [...document.querySelectorAll('fieldset input[type="checkbox"]')].filter(i=>i.checked))input.click();
      document.getElementById(prefix+'-point-'+points[0].id).click();await new Promise(r=>setTimeout(r,0));document.getElementById(prefix+'-point-'+points[1].id).click();await new Promise(r=>setTimeout(r,0));
      fill(prefix==='student'?'total':'parentTotal','5');await new Promise(r=>setTimeout(r,0));fill(prefix+'-allocation-'+points[0].id,'2');fill(prefix+'-allocation-'+points[1].id,'3');
     };
     const complete=async()=>{const counts={A:0,B:0},stems=[];for(let i=0;i<5;i++){await wait(()=>document.querySelector('.question .label')?.innerText==='第 '+(i+1)+' 题','seq');const stem=document.querySelector('.question h1').innerText;const group=stem.includes('S01A')?'A':stem.includes('S01B')?'B':null;if(!group)throw new Error('Foreign question '+stem);stems.push(stem);counts[group]++;const match=stem.match(/(\\d+)\\s*\\+\\s*(\\d+)/);const answer=String(Number(match[1])+Number(match[2]));fill('answer',counts[group]<=(group==='A'?1:2)?answer:'999');if(i<4)await click('下一题');}if(counts.A!==2||counts.B!==3||new Set(stems).size!==5)throw new Error('Allocation mismatch');await click('结束并检查');await click('确认提交');await wait(()=>text().includes('60.00'),'60 percent');return stems;};
     await wait(()=>button('退出登录')||button('登录学习账号'),'initial');if(button('退出登录'))await logout();
     await login('s01_ui_parent','家长学习中心');await wait(()=>document.getElementById('childSelect')?.value&&!button('刷新孩子和任务').disabled,'initial parent');await click('学情');await wait(()=>text().includes('暂无正式完成记录')||text().includes('完成'),'initial stats');const beforeTotal=Number(text().match(/共(\\d+)题/)?.[1]||0);await logout();
     await login('s01_ui_student','开始新练习');await click('开始新练习');await configure('student');
     await click('‹ 返回首页');await click('开始新练习');if(document.getElementById('student-allocation-'+points[0].id).value!=='2')throw new Error('Config lost');
     await click('生成练习，开始作答');await wait(draft,'self draft');const firstStem=document.querySelector('.question h1').innerText;fill('answer','5');await click('手动保存');await wait(()=>text().includes('已保存到服务端'),'saved');await click('‹ 保存退出');
     await wait(()=>document.querySelector('button.record')&&!document.querySelector('button.record').disabled,'record');document.querySelector('button.record').click();await wait(()=>draft()||button('在此继续（重新载入草稿）'),'resume');if(button('在此继续（重新载入草稿）'))await click('在此继续（重新载入草稿）');await wait(draft,'restored');if(document.querySelector('.question h1').innerText!==firstStem||document.getElementById('answer').value!=='5')throw new Error('Restore mismatch');await complete();await logout();
     await login('s01_ui_parent','家长学习中心');await wait(()=>document.getElementById('childSelect')?.value&&!button('刷新孩子和任务').disabled,'child');await click('练习任务');await configure('parent');await click('生成题目预览');await wait(()=>button('确认发送以上题目'),'preview');const preview=[...document.querySelectorAll('ol li')].map(i=>i.innerText);if(preview.length!==5)throw new Error('Preview count');await click('确认发送以上题目');await click('取消');await click('确认发送以上题目');await click('确认操作');await wait(()=>text().includes('待完成 · 家长安排')&&!button('刷新孩子和任务').disabled,'sent');await logout();
     await login('s01_ui_student','开始新练习');await wait(()=>[...document.querySelectorAll('button.record')].some(b=>b.innerText.includes('待完成')),'received');[...document.querySelectorAll('button.record')].find(b=>b.innerText.includes('待完成')).click();await wait(draft,'parent draft');const received=await complete();if(JSON.stringify(received)!==JSON.stringify(preview))throw new Error('Preview changed');await logout();
     await login('s01_ui_parent','家长学习中心');await wait(()=>document.getElementById('childSelect')?.value&&!button('刷新孩子和任务').disabled,'parent return');await click('学情');await wait(()=>text().includes('60.00')&&text().includes('每日表现'),'weighted stats');
     if(!text().includes('共'+(beforeTotal+10)+'题'))throw new Error('Expected 10 additional completed questions');
     return {selfAllocation:'A2+B3',selfAccuracy:'60.00',savedRestored:true,configReopened:true,fixedPreview:true,parentAccuracy:'60.00',summaryAdded:10,summaryAccuracy:'60.00'};
    })().then(data=>window.__s01Normal={done:true,data}).catch(e=>window.__s01Normal={done:true,error:String(e)});
    """.replace("POINTS",credentials.getJSONArray("s01Points").toString()).replace("PASSWORD",JSONObject.quote(credentials.getString("password")));
   eval(view.get(),script);JSONObject outcome=null;deadline=System.currentTimeMillis()+240000;
   while(System.currentTimeMillis()<deadline){String raw=eval(view.get(),"JSON.stringify(window.__s01Normal||{})");outcome=new JSONObject((String)new JSONTokener(raw).nextValue());if(outcome.optBoolean("done"))break;Thread.sleep(200);}
   assertNotNull(outcome);assertTrue("Probe timed out",outcome.optBoolean("done"));assertFalse(outcome.optString("error"),outcome.has("error"));System.out.println("S01_NORMAL_PROBE "+outcome.getJSONObject("data"));
   var bitmap=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();try(var output=new java.io.FileOutputStream(new java.io.File(context.getExternalFilesDir(null),"s01-normal.png"))){bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,output);}finally{bitmap.recycle();}
  }
 }
}
