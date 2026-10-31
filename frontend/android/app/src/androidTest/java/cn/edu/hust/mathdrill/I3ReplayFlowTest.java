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
public class I3ReplayFlowTest {
 private String eval(WebView view,String script)throws Exception{
  var latch=new CountDownLatch(1);var value=new AtomicReference<String>();
  InstrumentationRegistry.getInstrumentation().runOnMainSync(()->view.evaluateJavascript(script,r->{value.set(r);latch.countDown();}));
  assertTrue(latch.await(10,TimeUnit.SECONDS));return value.get();
 }
 @Test public void studentReplaysWrongQuestionWithoutDeletingHistory()throws Exception{
  var context=InstrumentationRegistry.getInstrumentation().getTargetContext();JSONObject credentials;
  try(var scanner=new java.util.Scanner(new java.io.File(context.getExternalFilesDir(null),"i1-probe.json"),"UTF-8")){credentials=new JSONObject(scanner.useDelimiter("\\A").next());}
  try(var scenario=ActivityScenario.<MainActivity>launch(new android.content.Intent(context,MainActivity.class))){
   var view=new AtomicReference<WebView>();scenario.onActivity(a->view.set(a.getBridge().getWebView()));
   long deadline=System.currentTimeMillis()+20000;while(!"\"complete\"".equals(eval(view.get(),"document.readyState"))&&System.currentTimeMillis()<deadline)Thread.sleep(100);

   String script="""
    window.__i3Probe={done:false};
    (async()=>{
     const wait=async(check,label)=>{const end=Date.now()+20000;while(Date.now()<end){if(check())return;await new Promise(r=>setTimeout(r,70));}throw new Error('Timeout '+label+' / '+document.body.innerText.slice(0,900));};
     const text=()=>document.body.innerText;
     const button=t=>[...document.querySelectorAll('button,summary')].find(b=>b.innerText.trim()===t);
     const click=async t=>{await wait(()=>button(t)&&!button(t).disabled,t);button(t).click();};
     const fill=(id,value)=>{const input=document.getElementById(id);if(!input)throw new Error('Missing '+id);input.value=value;input.dispatchEvent(new Event('input',{bubbles:true}));};
     await wait(()=>button('退出登录')||button('登录学习账号'),'initial');if(button('退出登录')){await click('退出登录');await wait(()=>button('登录学习账号'),'logout');}
     await click('还没有账号？注册学生或家长账号');const name='s_native_'+Date.now();fill('registerName',name);fill('registerNickname','原题重练测试');fill('registerPassword',PASSWORD);await click('确认注册');await wait(()=>button('登录学习账号'),'registered');fill('password',PASSWORD);await click('登录学习账号');await click('开始练习');fill('total','1');await click('开始练习');
     await wait(()=>document.getElementById('answer')&&!document.getElementById('answer').disabled,'first draft');const stem=document.querySelector('.question h1').innerText;const match=stem.match(/(\\d+)\\s*\\+\\s*(\\d+)/);if(!match)throw new Error('Expected addition');const answer=String(Number(match[1])+Number(match[2]));fill('answer','999');await click('结束并检查');await click('确认提交');await wait(()=>text().includes('0.00'),'original result');await click('查看我的错题');
     await wait(()=>document.querySelector('button.record'),'mistake list');document.querySelector('button.record').click();await click('再练一练这道题');await click('再做一次');await click('取消');await click('再做一次');await click('确认操作');
     await wait(()=>document.getElementById('answer')&&!document.getElementById('answer').disabled,'replay draft');if(document.querySelector('.question h1').innerText!==stem||document.getElementById('answer').value!=='')throw new Error('Snapshot or blank answer mismatch');fill('answer',answer);await click('手动保存');await wait(()=>text().includes('答案已保存'),'save');await click('‹ 保存退出');
     await wait(()=>[...document.querySelectorAll('button.record')].some(b=>b.innerText.includes('原题重练')),'replay home');[...document.querySelectorAll('button.record')].find(b=>b.innerText.includes('原题重练')).click();await click('在这里继续答题');await wait(()=>document.getElementById('answer')&&!document.getElementById('answer').disabled,'restored');if(document.getElementById('answer').value!==answer)throw new Error('Saved answer not restored');await click('结束并检查');await click('确认提交');await wait(()=>text().includes('100.00'),'new result');
     await click('查看我的错题');await wait(()=>text().includes('1 道不同题，累计错误 1 次'),'old error preserved');document.querySelector('button.record').click();await click('以前的作答记录');await wait(()=>text().includes('原题重练 · 答对')&&text().includes('普通练习 · 答错'),'both histories');
     return {replaySnapshot:true,cancelPreserved:true,blankAnswer:true,savedRestored:true,newAccuracy:'100.00',oldErrorCount:1,historyContainsCorrectAndWrong:true};
    })().then(data=>window.__i3Probe={done:true,data}).catch(e=>window.__i3Probe={done:true,error:String(e)});
    """.replace("PASSWORD",JSONObject.quote(credentials.getString("password")));
   eval(view.get(),script);JSONObject outcome=null;deadline=System.currentTimeMillis()+180000;
   while(System.currentTimeMillis()<deadline){String raw=eval(view.get(),"JSON.stringify(window.__i3Probe||{})");outcome=new JSONObject((String)new JSONTokener(raw).nextValue());if(outcome.optBoolean("done"))break;Thread.sleep(200);}
   assertNotNull(outcome);assertTrue("Probe timed out",outcome.optBoolean("done"));assertFalse(outcome.optString("error"),outcome.has("error"));System.out.println("I3_REPLAY_PROBE "+outcome.getJSONObject("data"));
   var bitmap=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();try(var output=new java.io.FileOutputStream(new java.io.File(context.getExternalFilesDir(null),"i3-replay.png"))){bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,output);}finally{bitmap.recycle();}
  }
 }
}
