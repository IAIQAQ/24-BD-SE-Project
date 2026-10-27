package cn.edu.hust.mathdrill;

import com.fasterxml.jackson.databind.*;
import jakarta.servlet.http.Cookie;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties={"server.ssl.enabled=false","mathdrill.seed-enabled=true"})
@AutoConfigureMockMvc
class I1FlowIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate db;
    @Value("${mathdrill.seed-password}") String password;
    record Client(Cookie cookie){}
    String key(){return UUID.randomUUID().toString();}
    JsonNode body(MvcResult r)throws Exception{return json.readTree(r.getResponse().getContentAsString());}
    Client login(String name)throws Exception{
        var token=mvc.perform(get("/api/v1/auth/csrf").secure(true)).andReturn();
        Cookie cookie=token.getResponse().getCookie("MATHDRILL_SESSION");
        var r=mvc.perform(post("/api/v1/auth/login").secure(true).cookie(cookie).header("X-CSRF-TOKEN",body(token).at("/data/token").asText()).contentType("application/json").content(json.writeValueAsString(Map.of("loginName",name,"password",password)))).andReturn();
        assertEquals(200,r.getResponse().getStatus());return new Client(r.getResponse().getCookie("MATHDRILL_SESSION"));
    }
    JsonNode call(Client c,String method,String path,Object data,int expected)throws Exception{
        var request=switch(method){case "POST"->post("/api/v1"+path);case "PUT"->put("/api/v1"+path);default->get("/api/v1"+path);};
        request.secure(true).cookie(c.cookie());
        if(!method.equals("GET")){
            var csrf=mvc.perform(get("/api/v1/auth/csrf").secure(true).cookie(c.cookie())).andReturn();
            request.header("X-CSRF-TOKEN",body(csrf).at("/data/token").asText()).contentType("application/json").content(json.writeValueAsString(data));
        }
        var r=mvc.perform(request).andReturn();assertEquals(expected,r.getResponse().getStatus(),r.getResponse().getContentAsString());return body(r).path("data");
    }
    JsonNode create(Client c,int total,String operationKey,int expected)throws Exception{return call(c,"POST","/practices",Map.of("pointId","1","total",total,"operationKey",operationKey),expected);}
    JsonNode open(Client c,String id,boolean takeover)throws Exception{return call(c,"POST","/practices/"+id+"/open",Map.of("operationKey",key(),"takeover",takeover),200);}
    Map<String,Object> write(JsonNode editor,List<Map<String,Object>> answers,String op){return new HashMap<>(Map.of("editorSessionId",editor.path("editorSessionId").asText(),"baseVersion",editor.path("version").asLong(),"operationKey",op,"answers",answers,"currentSeq",1));}
    List<Map<String,Object>> answers(JsonNode draft,boolean graded){
        var answers=new ArrayList<Map<String,Object>>();int index=0;
        for(var item:draft.path("items")){
            String raw="";
            if(graded){int standard=db.queryForObject("SELECT standard_answer FROM practice_item WHERE id=?",Integer.class,item.path("id").asText());raw=index<24?Integer.toString(standard):index<28?"999":"";}
            answers.add(Map.of("itemId",item.path("id").asText(),"raw",raw));index++;
        }return answers;
    }
    @Test void catalogAndCompositionAreReviewedUniqueAndIdempotent()throws Exception{
        var c=login("student_a");var points=call(c,"GET","/catalog/points",null,200);assertEquals(120,points.get(0).path("available").asInt());
        String op=key();var p=create(c,30,op,200);String id=p.path("id").asText();assertEquals(id,create(c,30,op,200).path("id").asText());create(c,31,op,409);
        create(c,0,key(),400);create(c,101,key(),400);create(c,1,key(),200);create(c,100,key(),200);
        var d=call(c,"GET","/practices/"+id+"/draft",null,200);assertEquals(30,d.path("items").size());
        var stems=new HashSet<String>();for(var item:d.path("items")){assertTrue(stems.add(item.path("stem").asText()));assertFalse(item.has("standardAnswer"));assertFalse(item.has("explanation"));}
        call(login("student_b"),"GET","/practices/"+id+"/draft",null,403);
    }
    @Test void savedRawAnswersRestoreAndOldEditorCannotOverwrite()throws Exception{
        var c=login("student_a");String id=create(c,3,key(),200).path("id").asText();var a=open(c,id,false);
        var draft=call(c,"GET","/practices/"+id+"/draft",null,200);var inputs=answers(draft,false);inputs.set(0,Map.of("itemId",draft.path("items").get(0).path("id").asText(),"raw"," 015 "));
        var payload=write(a,inputs,key());var saved=call(c,"PUT","/practices/"+id+"/answers",payload,200);assertEquals(saved,call(c,"PUT","/practices/"+id+"/answers",payload,200));
        var restored=call(c,"GET","/practices/"+id+"/draft",null,200);assertEquals(" 015 ",restored.path("items").get(0).path("raw").asText());
        var b=open(c,id,true);call(c,"PUT","/practices/"+id+"/answers",payload,409);
        call(c,"POST","/practices/"+id+"/submission-status",Map.of("operationKey",key(),"editorSessionId",a.path("editorSessionId").asText(),"baseVersion",a.path("version").asLong()),200);
        assertEquals(b.path("editorSessionId").asText(),db.queryForObject("SELECT editor_session_id FROM practice WHERE id=?",String.class,id));
    }
    @Test void submissionUsesCurrentAnswersAndProducesOneAtomicResult()throws Exception{
        var c=login("student_a");String id=create(c,30,key(),200).path("id").asText();var editor=open(c,id,false);var d=call(c,"GET","/practices/"+id+"/draft",null,200);
        var payload=write(editor,answers(d,true),key());payload.put("confirmUnanswered",false);call(c,"POST","/practices/"+id+"/submit",payload,400);
        payload.put("confirmUnanswered",true);var result=call(c,"POST","/practices/"+id+"/submit",payload,200);
        assertEquals("80.00",result.path("accuracy").asText());assertEquals(24,result.path("correct").asInt());assertEquals(6,result.path("mistakes").asInt());assertEquals(2,result.path("unanswered").asInt());
        assertEquals(result,call(c,"POST","/practices/"+id+"/submit",payload,200));
        assertEquals(6,db.queryForObject("SELECT COUNT(*) FROM mistake_record WHERE practice_id=?",Integer.class,id));
        call(c,"PUT","/practices/"+id+"/answers",payload,409);
        var history=call(c,"GET","/students/"+result.path("studentId").asText()+"/mistakes",null,200);assertTrue(history.path("totalErrors").asInt()>=6);
    }
    @Test void reconciliationFencesLateSubmissionAndPreservesNewOwner()throws Exception{
        var c=login("student_a");String id=create(c,1,key(),200).path("id").asText();var editor=open(c,id,false);var d=call(c,"GET","/practices/"+id+"/draft",null,200);String op=key();var old=write(editor,answers(d,false),op);old.put("confirmUnanswered",true);
        var probe=Map.of("operationKey",op,"editorSessionId",editor.path("editorSessionId").asText(),"baseVersion",editor.path("version").asLong());
        var checked=call(c,"POST","/practices/"+id+"/submission-status",probe,200);assertEquals("NOT_COMMITTED",checked.path("status").asText());
        call(c,"POST","/practices/"+id+"/submit",old,409);var next=open(c,id,true);
        var repeated=call(c,"POST","/practices/"+id+"/submission-status",probe,200);assertFalse(repeated.path("canEdit").asBoolean());
        assertEquals(next.path("editorSessionId").asText(),db.queryForObject("SELECT editor_session_id FROM practice WHERE id=?",String.class,id));
    }
    @Test void readOnlyDraftDoesNotGiveAnotherClientWriteAuthority()throws Exception{
        var c=login("student_a");String id=create(c,1,key(),200).path("id").asText();open(c,id,false);
        assertFalse(call(c,"GET","/practices/"+id+"/draft",null,200).has("editorSessionId"));
    }
    @Test void oldProbeCannotAcquireTheNewEditorsCredentialsOnRetry()throws Exception{
        var c=login("student_a");String id=create(c,1,key(),200).path("id").asText();var a=open(c,id,false);open(c,id,true);
        var probe=Map.of("operationKey",key(),"editorSessionId",a.path("editorSessionId").asText(),"baseVersion",a.path("version").asLong());
        assertFalse(call(c,"POST","/practices/"+id+"/submission-status",probe,200).path("canEdit").asBoolean());
        var repeated=call(c,"POST","/practices/"+id+"/submission-status",probe,200);assertFalse(repeated.path("canEdit").asBoolean());assertFalse(repeated.has("editorSessionId"));
    }
}
