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
public class I1FlowTest {
    private String eval(WebView view,String script)throws Exception{
        var latch=new CountDownLatch(1);var value=new AtomicReference<String>();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->view.evaluateJavascript(script,r->{value.set(r);latch.countDown();}));
        assertTrue(latch.await(10,TimeUnit.SECONDS));return value.get();
    }
    @Test public void studentCompletesRealUiFlow()throws Exception{
        var context=InstrumentationRegistry.getInstrumentation().getTargetContext();JSONObject credentials;
        try(var scanner=new java.util.Scanner(new java.io.File(context.getExternalFilesDir(null),"i1-probe.json"),"UTF-8")){credentials=new JSONObject(scanner.useDelimiter("\\A").next());}
        try(var scenario=ActivityScenario.<MainActivity>launch(new android.content.Intent(context,MainActivity.class))){
            var view=new AtomicReference<WebView>();scenario.onActivity(a->view.set(a.getBridge().getWebView()));
            long deadline=System.currentTimeMillis()+20000;while(!"\"complete\"".equals(eval(view.get(),"document.readyState"))&&System.currentTimeMillis()<deadline)Thread.sleep(100);
            String script="""
                window.__i1Probe={done:false};
                (async()=>{
                  const wait=async(check,label)=>{const end=Date.now()+20000;while(Date.now()<end){if(check())return;await new Promise(r=>setTimeout(r,70));}throw new Error('Timeout: '+label+' / '+document.body.innerText.slice(0,700));};
                  const text=()=>document.body.innerText;
                  const waitText=t=>wait(()=>text().includes(t),t);
                  const button=t=>[...document.querySelectorAll('button')].find(b=>b.innerText.trim()===t);
                  const click=async t=>{await wait(()=>button(t)&&!button(t).disabled,'button '+t);button(t).click();};
                  const fill=(id,value)=>{const input=document.getElementById(id);if(!input)throw new Error('Missing input '+id);input.value=value;input.dispatchEvent(new Event('input',{bubbles:true}));};
                  const signIn=async()=>{await wait(()=>button('登录学习账号')&&!button('登录学习账号').disabled,'login ready');fill('username','student_a');fill('password',PASSWORD);document.querySelector('form').requestSubmit();await wait(()=>button('开始练习')&&!button('开始练习').disabled,'home loaded');};
                  await wait(()=>button('退出登录')||button('登录学习账号'),'initial page');
                  if(button('退出登录')){await click('退出登录');await waitText('登录学习账号');}
                  await signIn();await click('开始练习');await waitText('选题数');fill('total','30');await click('开始练习');await wait(()=>document.getElementById('answer')&&!document.getElementById('answer').disabled,'draft loaded');
                  fill('answer','15');await click('下一题');await wait(()=>document.querySelector('.question .label')?.innerText==='第 2 题','question 2');fill('answer','16');
                  await click('手动保存');await waitText('答案已保存');await click('‹ 保存退出');await waitText('开始练习');await click('退出登录');await waitText('登录学习账号');await signIn();
                  const continuing=[...document.querySelectorAll('button.record')].find(b=>b.innerText.includes('进行中'));if(!continuing)throw new Error('Missing saved practice');continuing.click();await waitText('你的答案');await wait(()=>document.querySelector('main').getAttribute('aria-busy')==='false','resume loaded');
                  if(text().includes('这份练习已在别处打开'))await click('在这里继续答题');
                  await wait(()=>document.getElementById('answer')&&!document.getElementById('answer').disabled,'editable');
                  if(document.getElementById('answer').value!=='16')throw new Error('Second saved answer not restored');await click('上一题');await wait(()=>document.querySelector('.question .label')?.innerText==='第 1 题','question 1');
                  if(document.getElementById('answer').value!=='15')throw new Error('First saved answer not restored');
                  for(let i=0;i<30;i++){
                    await wait(()=>document.querySelector('.question .label')?.innerText==='第 '+(i+1)+' 题','question '+(i+1));
                    const stem=document.querySelector('.question h1').innerText;const m=stem.match(/(\\d+)\\s*\\+\\s*(\\d+)/);if(!m)throw new Error('Unexpected question');
                    fill('answer',i<24?String(Number(m[1])+Number(m[2])):i<28?'999':'');
                    if(i<29)await click('下一题');
                  }
                  await click('结束并检查');await waitText('准备提交了吗');if(!text().includes('还有 2 题没做'))throw new Error('Missing unanswered confirmation');
                  await click('仍然交卷');await waitText('80.00');
                  const score=text();if(!score.includes('24')||!score.includes('4')||!score.includes('2'))throw new Error('Unexpected result counters');
                  await click('查看我的错题');await waitText('我的错题');
                  const records=document.querySelectorAll('button.record');if(records.length<6)throw new Error('Missing mistake records');records[0].click();await waitText('以前的作答记录');
                  const result={restored:true,total:30,correct:24,wrong:4,unanswered:2,accuracy:'80.00',mistakeDetails:true};
                  return result;
                })().then(data=>window.__i1Probe={done:true,data}).catch(e=>window.__i1Probe={done:true,error:String(e)});
                """.replace("PASSWORD",JSONObject.quote(credentials.getString("password")));
            eval(view.get(),script);JSONObject outcome=null;deadline=System.currentTimeMillis()+180000;
            while(System.currentTimeMillis()<deadline){String raw=eval(view.get(),"JSON.stringify(window.__i1Probe||{})");outcome=new JSONObject((String)new JSONTokener(raw).nextValue());if(outcome.optBoolean("done"))break;Thread.sleep(200);}
            assertNotNull(outcome);assertTrue("Probe timed out",outcome.optBoolean("done"));assertFalse(outcome.optString("error"),outcome.has("error"));
            System.out.println("I1_UI_PROBE "+outcome.getJSONObject("data"));
            var bitmap=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
            try(var output=new java.io.FileOutputStream(new java.io.File(context.getExternalFilesDir(null),"i1-flow.png"))){bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,output);}finally{bitmap.recycle();}
        }
    }
}
