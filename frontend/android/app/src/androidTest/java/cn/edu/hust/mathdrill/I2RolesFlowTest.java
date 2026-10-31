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
public class I2RolesFlowTest {
 private String eval(WebView view,String script)throws Exception{
  var latch=new CountDownLatch(1);var value=new AtomicReference<String>();
  InstrumentationRegistry.getInstrumentation().runOnMainSync(()->view.evaluateJavascript(script,r->{value.set(r);latch.countDown();}));
  assertTrue(latch.await(10,TimeUnit.SECONDS));return value.get();
 }
 @Test public void parentAssignsStudentCompletesAndAdminReadsRealUi()throws Exception{
  var context=InstrumentationRegistry.getInstrumentation().getTargetContext();JSONObject credentials;
  try(var scanner=new java.util.Scanner(new java.io.File(context.getExternalFilesDir(null),"i1-probe.json"),"UTF-8")){credentials=new JSONObject(scanner.useDelimiter("\\A").next());}
  try(var scenario=ActivityScenario.<MainActivity>launch(new android.content.Intent(context,MainActivity.class))){
   var view=new AtomicReference<WebView>();scenario.onActivity(a->view.set(a.getBridge().getWebView()));
   long deadline=System.currentTimeMillis()+20000;while(!"\"complete\"".equals(eval(view.get(),"document.readyState"))&&System.currentTimeMillis()<deadline)Thread.sleep(100);
   String script="""
    window.__i2Probe={done:false};
    (async()=>{
     const wait=async(check,label)=>{const end=Date.now()+20000;while(Date.now()<end){if(check())return;await new Promise(r=>setTimeout(r,70));}throw new Error('Timeout '+label+' / '+document.body.innerText.slice(0,900));};
     const text=()=>document.body.innerText;
     const button=t=>[...document.querySelectorAll('button')].find(b=>b.innerText.trim()===t);
     const click=async t=>{await wait(()=>button(t)&&!button(t).disabled,t);button(t).click();};
     const fill=(id,value)=>{const input=document.getElementById(id);if(!input)throw new Error('Missing '+id);input.value=value;input.dispatchEvent(new Event('input',{bubbles:true}));};
     const login=async(name,pw,heading)=>{fill('username',name);fill('password',pw);await click('登录学习账号');await wait(()=>text().includes(heading),'role '+heading);};
     const logout=async()=>{await click('退出登录');await wait(()=>button('登录学习账号'),'logout');};
     await wait(()=>button('退出登录')||button('登录学习账号'),'initial');if(button('退出登录'))await logout();
     await login('parent_demo',PASSWORD,'家长学习中心');
     await wait(()=>document.getElementById('childSelect')?.value&&!button('刷新孩子和任务').disabled,'children loaded');
     await click('练习任务');fill('parentTotal','1');await click('预览题目');await wait(()=>button('确认发送以上题目'),'preview');
     const stem=document.querySelector('ol li').innerText;
     await click('确认发送以上题目');await wait(()=>document.querySelector('dialog[open]'),'confirmation');await click('取消');
     await wait(()=>!document.querySelector('dialog[open]'),'cancel');await click('确认发送以上题目');await click('确认操作');
     await wait(()=>text().includes('待完成')&&text().includes('家长安排')&&!button('刷新孩子和任务').disabled,'sent');await logout();
     await login('student_a',PASSWORD,'开始练习');
     await wait(()=>[...document.querySelectorAll('button.record')].some(b=>b.innerText.includes('待完成')),'student task');
     [...document.querySelectorAll('button.record')].find(b=>b.innerText.includes('待完成')).click();
     await wait(()=>document.getElementById('answer')&&!document.getElementById('answer').disabled,'answer');
     if(document.querySelector('.question h1').innerText!==stem)throw new Error('Preview changed');
     const match=stem.match(/(\\d+)\\s*\\+\\s*(\\d+)/);if(!match)throw new Error('Expected addition');fill('answer',String(Number(match[1])+Number(match[2])));
     await click('手动保存');await wait(()=>text().includes('答案已保存'),'saved');await click('结束并检查');await click('确认提交');
     await wait(()=>text().includes('100.00'),'result');await logout();
     await login('parent_demo',PASSWORD,'家长学习中心');await wait(()=>document.getElementById('childSelect')?.value&&!button('刷新孩子和任务').disabled,'children loaded');
     await click('练习任务');await click('查看任务与结果');await wait(()=>text().includes('正确率 100.00%'),'parent result');
     await click('孩子错题');await wait(()=>text().includes('小明的错误记录'),'parent mistakes');await click('学情');await wait(()=>text().includes('每日表现'),'parent stats');await logout();
     await login('admin',ADMIN_PASSWORD,'管理员后台');await wait(()=>text().includes('可用题数：'+EXPECTED_AVAILABLE),'admin real data');await click('题库维护');await wait(()=>document.getElementById('questionStem'),'admin maintenance');await click('Excel导入');await wait(()=>button('下载或分享Excel模板'),'admin import');
     return {roles:3,previewMatched:true,cancelVerified:true,studentAccuracy:'100.00',parentResult:true,parentMistakes:true,parentStats:true,adminMaintenance:true,adminImportEntry:true};
    })().then(data=>window.__i2Probe={done:true,data}).catch(e=>window.__i2Probe={done:true,error:String(e)});
    """.replace("EXPECTED_AVAILABLE",Integer.toString(credentials.optInt("expectedAvailable",120))).replace("ADMIN_PASSWORD",JSONObject.quote(credentials.getString("adminPassword"))).replace("PASSWORD",JSONObject.quote(credentials.getString("password")));
   eval(view.get(),script);JSONObject outcome=null;deadline=System.currentTimeMillis()+180000;
   while(System.currentTimeMillis()<deadline){String raw=eval(view.get(),"JSON.stringify(window.__i2Probe||{})");outcome=new JSONObject((String)new JSONTokener(raw).nextValue());if(outcome.optBoolean("done"))break;Thread.sleep(200);}
   assertNotNull(outcome);assertTrue("Probe timed out",outcome.optBoolean("done"));assertFalse(outcome.optString("error"),outcome.has("error"));System.out.println("I2_UI_PROBE "+outcome.getJSONObject("data"));
   var bitmap=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();try(var output=new java.io.FileOutputStream(new java.io.File(context.getExternalFilesDir(null),"i2-roles.png"))){bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,output);}finally{bitmap.recycle();}
  }
 }
}
