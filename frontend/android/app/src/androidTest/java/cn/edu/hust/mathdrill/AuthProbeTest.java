package cn.edu.hust.mathdrill;

import android.webkit.CookieManager;
import android.webkit.WebView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class AuthProbeTest {
    private String eval(WebView view,String script) throws Exception {
        var latch=new CountDownLatch(1);var value=new AtomicReference<String>();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->view.evaluateJavascript(script,result->{value.set(result);latch.countDown();}));
        assertTrue("WebView callback timed out",latch.await(10,TimeUnit.SECONDS));return value.get();
    }
    @Test public void actualWebViewUsesValidatedTlsAndHttpOnlySession() throws Exception {
        var context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        JSONObject credentials;
        try(var scanner=new java.util.Scanner(new java.io.File(context.getExternalFilesDir(null),"auth-probe.json"),"UTF-8")){
            credentials=new JSONObject(scanner.useDelimiter("\\A").next());
        }
        String mode=InstrumentationRegistry.getArguments().getString("mode","full");
        System.out.println("AUTH_PROBE launching activity");
        try(var scenario=ActivityScenario.<MainActivity>launch(new android.content.Intent(context,MainActivity.class))){
            System.out.println("AUTH_PROBE activity resumed");
            var view=new AtomicReference<WebView>();scenario.onActivity(activity->view.set(activity.getBridge().getWebView()));
            long deadline=System.currentTimeMillis()+30000;
            while(!"\"complete\"".equals(eval(view.get(),"document.readyState"))&&System.currentTimeMillis()<deadline)Thread.sleep(100);
            String script="""
                window.__authProbe={done:false};
                (async()=>{
                  const base='https://localhost:18443/api/v1';
                  const call=async(path,options={})=>{const r=await fetch(base+path,{...options,credentials:'include'});return {status:r.status,body:r.status===204?null:await r.json()};};
                  const post=async(path,body)=>{const c=(await call('/auth/csrf')).body.data;return call(path,{method:'POST',headers:{'Content-Type':'application/json',[c.headerName]:c.token},body:body?JSON.stringify(body):undefined});};
                  const results={health:(await call('/health')).status};
                  if(MODE==='ui'){
                    const wait=async(text)=>{const end=Date.now()+10000;while(Date.now()<end){if(document.body.innerText.includes(text))return;await new Promise(r=>setTimeout(r,100));}throw new Error('UI missing: '+text);};
                    const click=text=>{const b=[...document.querySelectorAll('button')].find(x=>x.innerText===text);if(!b)throw new Error('Button missing: '+text);b.click();};
                    const fill=(id,value)=>{const input=document.getElementById(id);input.value=value;input.dispatchEvent(new Event('input',{bubbles:true}));};
                    fill('username','student_a');fill('password',PASSWORD);document.querySelector('form').requestSubmit();
                    await wait('小明，连接成功');results.uiLogin=true;
                    click('检查已有登录状态');await wait('已恢复服务端登录状态');results.uiRestore=true;
                    click('退出登录');await wait('原会话已撤销');results.uiLogout=true;
                    click('检查已有登录状态');await wait('请先登录');results.uiRevoked=true;
                    fill('username','student_a');fill('password',PASSWORD);document.querySelector('form').requestSubmit();await wait('小明，连接成功');
                    return results;
                  }
                  if(MODE==='resume'){
                    const current=await call('/auth/me');results.restored=current.status;results.loginName=current.body.data?.loginName;
                    return results;
                  }
                  results.missingCsrf=(await call('/auth/login',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({loginName:'student_a',password:'wrong'})})).status;
                  results.badCredentials=(await post('/auth/login',{loginName:'student_a',password:'wrong'})).status;
                  const a=await post('/auth/login',{loginName:'student_a',password:PASSWORD});results.loginA=a.status;
                  results.me=(await call('/auth/me')).status;
                  results.cookieHidden=!document.cookie.includes('MATHDRILL_SESSION');
                  results.logout=(await post('/auth/logout')).status;results.revoked=(await call('/auth/me')).status;
                  results.loginB=(await post('/auth/login',{loginName:'student_b',password:PASSWORD})).status;
                  results.isolation=(await call('/students/'+a.body.data.id+'/profile')).status;
                  await post('/auth/logout');results.finalLoginA=(await post('/auth/login',{loginName:'student_a',password:PASSWORD})).status;
                  return results;
                })().then(data=>window.__authProbe={done:true,data}).catch(e=>window.__authProbe={done:true,error:String(e)});
                """.replace("PASSWORD",JSONObject.quote(credentials.getString("password"))).replace("MODE",JSONObject.quote(mode));
            eval(view.get(),script);
            JSONObject outcome=null;deadline=System.currentTimeMillis()+60000;
            while(System.currentTimeMillis()<deadline){
                String raw=eval(view.get(),"JSON.stringify(window.__authProbe||{})");
                outcome=new JSONObject((String)new JSONTokener(raw).nextValue());if(outcome.optBoolean("done"))break;Thread.sleep(150);
            }
            assertNotNull(outcome);assertTrue("Probe did not complete",outcome.optBoolean("done"));assertFalse(outcome.optString("error"),outcome.has("error"));
            JSONObject data=outcome.getJSONObject("data");System.out.println("AUTH_PROBE "+data);
            assertEquals(200,data.getInt("health"));
            if("resume".equals(mode)){assertEquals(200,data.getInt("restored"));assertEquals("student_a",data.getString("loginName"));}
            else if("ui".equals(mode)){for(String key:new String[]{"uiLogin","uiRestore","uiLogout","uiRevoked"})assertTrue(key,data.getBoolean(key));}
            else{assertEquals(403,data.getInt("missingCsrf"));assertEquals(401,data.getInt("badCredentials"));assertEquals(200,data.getInt("loginA"));assertEquals(200,data.getInt("me"));assertTrue(data.getBoolean("cookieHidden"));assertEquals(204,data.getInt("logout"));assertEquals(401,data.getInt("revoked"));assertEquals(200,data.getInt("loginB"));assertEquals(403,data.getInt("isolation"));assertEquals(200,data.getInt("finalLoginA"));}
            CookieManager.getInstance().flush();
        }
    }
}
