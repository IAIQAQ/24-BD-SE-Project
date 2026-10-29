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

@SpringBootTest(properties={"server.ssl.enabled=false","mathdrill.seed-enabled=true"}) @AutoConfigureMockMvc
class S01NormalPracticeIntegrationTest {
 @Autowired MockMvc mvc;@Autowired ObjectMapper json;@Autowired JdbcTemplate db;
 @Value("${mathdrill.seed-password}")String password;
 @Value("${mathdrill.admin-password}")String adminPassword;
 record Client(Cookie cookie){}
 String key(){return UUID.randomUUID().toString();}
 JsonNode body(MvcResult r)throws Exception{return json.readTree(r.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));}
 Client guest()throws Exception{var r=mvc.perform(get("/api/v1/auth/csrf").secure(true)).andReturn();return new Client(r.getResponse().getCookie("MATHDRILL_SESSION"));}
 JsonNode call(Client c,String method,String path,Object data,int expected)throws Exception{
   var req=method.equals("POST")?post("/api/v1"+path):method.equals("PUT")?put("/api/v1"+path):get("/api/v1"+path);req.secure(true).cookie(c.cookie);
   if(!method.equals("GET")){var csrf=mvc.perform(get("/api/v1/auth/csrf").secure(true).cookie(c.cookie)).andReturn();req.header("X-CSRF-TOKEN",body(csrf).at("/data/token").asText()).contentType("application/json").content(json.writeValueAsString(data));}
   var r=mvc.perform(req).andReturn();assertEquals(expected,r.getResponse().getStatus(),r.getResponse().getContentAsString());return body(r).path("data");
 }
 JsonNode register(String name,String role)throws Exception{return call(guest(),"POST","/auth/register",Map.of("loginName",name,"password",password,"nickname","测试"+role,"role",role,"grade",1),200);}
 Client login(String name)throws Exception{return login(name,password);}
 Client login(String name,String secret)throws Exception{var c=guest();var csrf=mvc.perform(get("/api/v1/auth/csrf").secure(true).cookie(c.cookie)).andReturn();var r=mvc.perform(post("/api/v1/auth/login").secure(true).cookie(c.cookie).header("X-CSRF-TOKEN",body(csrf).at("/data/token").asText()).contentType("application/json").content(json.writeValueAsString(Map.of("loginName",name,"password",secret)))).andReturn();assertEquals(200,r.getResponse().getStatus());return new Client(r.getResponse().getCookie("MATHDRILL_SESSION"));}

 String newPoint(int grade)throws Exception{
   var a=login("admin",adminPassword);return call(a,"POST","/admin/points",Map.of("name","I3测试"+key(),"grade",grade,"semester","上册","textbook","隔离测试","evidence","独立可推算样例","enabled",true),200).path("id").asText();
 }
 String newQuestion(String point)throws Exception{
   var a=login("admin",adminPassword);String id=call(a,"POST","/admin/questions",Map.of("pointId",point,"stem","2 + 3 =（"+key()+"）","standardAnswer","5","explanation","2再增加3等于5"),200).path("id").asText();
   call(a,"POST","/admin/questions/"+id+"/review",Map.of("confirm",true,"expectedRevision",1),200);call(a,"POST","/admin/questions/"+id+"/enabled",Map.of("enabled",true),200);return id;
 }
 record Family(Client student,Client parent,String sid,String link,String a,String b){}
 Family fixture()throws Exception{
  assertTrue(db.queryForObject("SELECT DATABASE()",String.class).equals("mathdrill_test"),"Tests must use the dedicated test database");
  String s="s01s_"+key(),p="s01p_"+key();String sid=register(s,"STUDENT").path("id").asText();register(p,"PARENT");var student=login(s);var parent=login(p);
  String link=call(parent,"POST","/family/requests",Map.of("studentLoginName",s),200).path("id").asText();call(student,"POST","/family/links/"+link+"/respond",Map.of("accept",true),200);
  String a=newPoint(1),b=newPoint(1);for(int i=0;i<2;i++)newQuestion(a);for(int i=0;i<3;i++)newQuestion(b);return new Family(student,parent,sid,link,a,b);
 }
 Map<String,Object> input(Family f,String key){return Map.of("allocations",List.of(Map.of("pointId",f.a,"total",2),Map.of("pointId",f.b,"total",3)),"operationKey",key);}
 String preview(Family f)throws Exception{return call(f.parent,"POST","/family/children/"+f.sid+"/previews",input(f,key()),200).path("id").asText();}
 @Test void normalMultiPointHasExactCountsWeightedStatsAndNoAnswerLeak()throws Exception{
  var f=fixture();var created=call(f.student,"POST","/practices",input(f,key()),200);String id=created.path("id").asText();assertEquals("NORMAL",created.path("practiceKind").asText());assertEquals("SELF",created.path("source").asText());
  var e=call(f.student,"POST","/practices/"+id+"/open",Map.of("operationKey",key()),200);var draft=call(f.student,"GET","/practices/"+id+"/draft",null,200);assertEquals(5,draft.path("items").size());assertEquals(2,draft.path("points").size());assertFalse(draft.toString().contains("standardAnswer"));assertFalse(draft.toString().contains("explanation"));
  var counts=new HashMap<String,Integer>();var questions=new HashSet<Long>();var answers=new ArrayList<Map<String,String>>();
  for(var item:draft.path("items")){String iid=item.path("id").asText();var row=db.queryForMap("SELECT point_id,question_id FROM practice_item WHERE id=?",iid);String point=row.get("point_id").toString();assertTrue(questions.add(((Number)row.get("question_id")).longValue()));int n=counts.merge(point,1,Integer::sum);answers.add(Map.of("itemId",iid,"raw",n<=(point.equals(f.a)?1:2)?"5":"999"));}
  assertEquals(2,counts.get(f.a));assertEquals(3,counts.get(f.b));
  var result=call(f.student,"POST","/practices/"+id+"/submit",Map.of("editorSessionId",e.path("editorSessionId").asText(),"baseVersion",e.path("version").asLong(),"operationKey",key(),"currentSeq",1,"answers",answers),200);assertEquals("60.00",result.path("accuracy").asText());
  var stats=call(f.parent,"GET","/family/children/"+f.sid+"/stats",null,200);assertEquals(5,stats.path("total").asInt());assertEquals("60.00",stats.path("accuracy").asText());for(var p:stats.path("points")){assertEquals(p.path("pointId").asText().equals(f.a)?2:3,p.path("total").asInt());assertEquals(p.path("pointId").asText().equals(f.a)?1:2,p.path("correct").asInt());}
 }
 @Test void normalizedRetryIsUniqueAndLegacyReceiptsStillWork()throws Exception{
  var f=fixture();String k=key();var created=call(f.student,"POST","/practices",input(f,k),200);
  var reversed=Map.of("allocations",List.of(Map.of("pointId",f.b,"total",3),Map.of("pointId",f.a,"total",2)),"operationKey",k);assertEquals(created.path("id"),call(f.student,"POST","/practices",reversed,200).path("id"));
  call(f.student,"POST","/practices",Map.of("allocations",List.of(Map.of("pointId",f.a,"total",1)),"operationKey",k),409);
  for(var client:List.of(f.student,f.parent)){
   String path=client==f.student?"/practices":"/family/children/"+f.sid+"/previews";String oldKey=key();var old=Map.of("pointId","1","total",1,"operationKey",oldKey);var legacy=call(client,"POST",path,old,200);assertEquals(legacy.path("id"),call(client,"POST",path,old,200).path("id"));
   assertEquals(legacy.path("id"),call(client,"POST",path,Map.of("allocations",List.of(Map.of("pointId","1","total",1)),"operationKey",oldKey),200).path("id"));
  }
  assertEquals(30,call(f.student,"POST","/practices",Map.of("pointId","1","operationKey",key()),200).path("total").asInt());
 }
 @Test void invalidAllocationsAndInsufficientPoolsNeverPersist()throws Exception{
  var f=fixture();long before=db.queryForObject("SELECT COUNT(*) FROM practice WHERE student_id=?",Long.class,f.sid);
  for(Object allocations:List.of(List.of(),List.of(Map.of("pointId",f.a,"total",0)),List.of(Map.of("pointId",f.a,"total",1.5)),List.of(Map.of("pointId",f.a,"total",1),Map.of("pointId",f.a,"total",1)),List.of(Map.of("pointId",f.a,"total",51),Map.of("pointId",f.b,"total",50)),List.of(Map.of("pointId",f.a)),Arrays.asList((Object)null))){call(f.student,"POST","/practices",Map.of("allocations",allocations,"operationKey",key()),400);}
  call(f.student,"POST","/practices",Map.of("pointId",f.a,"total",1,"allocations",List.of(Map.of("pointId",f.b,"total",1)),"operationKey",key()),400);
  call(f.student,"POST","/practices",Map.of("allocations",List.of(Map.of("pointId",f.a,"total",3),Map.of("pointId",f.b,"total",4)),"operationKey",key()),409);
  assertEquals(before,db.queryForObject("SELECT COUNT(*) FROM practice WHERE student_id=?",Long.class,f.sid));assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM practice_item i JOIN practice p ON p.id=i.practice_id WHERE p.student_id=?",Integer.class,f.sid));
  for(int total:List.of(1,100)){assertEquals(total,call(f.student,"POST","/practices",Map.of("allocations",List.of(Map.of("pointId","1","total",total)),"operationKey",key()),200).path("total").asInt());}
 }
 @Test void currentGradeAndReviewedEnabledPoolsAreMandatory()throws Exception{
  var f=fixture();String grade2=newPoint(2);newQuestion(grade2);
  call(f.student,"POST","/practices",Map.of("allocations",List.of(Map.of("pointId",f.a,"total",1),Map.of("pointId",grade2,"total",1)),"operationKey",key()),400);
  db.update("UPDATE knowledge_point SET enabled=FALSE WHERE id=?",f.a);call(f.student,"POST","/practices",input(f,key()),400);db.update("UPDATE knowledge_point SET enabled=TRUE WHERE id=?",f.a);
  db.update("UPDATE question SET reviewed=FALSE WHERE point_id=?",f.b);call(f.student,"POST","/practices",input(f,key()),409);db.update("UPDATE question SET reviewed=TRUE,enabled=FALSE WHERE point_id=?",f.b);call(f.student,"POST","/practices",input(f,key()),409);
 }
 @Test void parentPreviewIsFixedAndSendingRechecksEveryPoint()throws Exception{
  var f=fixture();var before=call(f.parent,"POST","/family/children/"+f.sid+"/previews",input(f,key()),200);String id=before.path("id").asText();
  assertEquals("PENDING",call(f.parent,"POST","/family/previews/"+id+"/send",Map.of("confirm",true),200).path("status").asText());assertEquals(id,call(f.parent,"POST","/family/previews/"+id+"/send",Map.of("confirm",true),200).path("id").asText());
  var draft=call(f.student,"GET","/practices/"+id+"/draft",null,200);assertEquals(before.path("items").size(),draft.path("items").size());for(int i=0;i<5;i++)assertEquals(before.path("items").get(i).path("stem"),draft.path("items").get(i).path("stem"));
  call(f.parent,"POST","/practices/"+id+"/open",Map.of("operationKey",key()),403);
  for(String point:List.of(f.a,f.b)){String changed=preview(f);db.update("UPDATE knowledge_point SET revision=revision+1 WHERE id=?",point);call(f.parent,"POST","/family/previews/"+changed+"/send",Map.of("confirm",true),409);}
 }
 @Test void parentPreviewRejectsExpiredChangedQuestionsGradeAndInactiveFamily()throws Exception{
  var f=fixture();String expired=preview(f);db.update("UPDATE practice SET created_at=DATE_SUB(created_at,INTERVAL 16 MINUTE) WHERE id=?",expired);call(f.parent,"POST","/family/previews/"+expired+"/send",Map.of("confirm",true),409);
  String questionChanged=preview(f);db.update("UPDATE question SET revision=revision+1 WHERE id=(SELECT question_id FROM practice_item WHERE practice_id=? LIMIT 1)",questionChanged);call(f.parent,"POST","/family/previews/"+questionChanged+"/send",Map.of("confirm",true),409);
  String gradeChanged=preview(f);call(f.student,"PUT","/auth/profile",Map.of("nickname","升年级","grade",2),200);call(f.parent,"POST","/family/previews/"+gradeChanged+"/send",Map.of("confirm",true),409);call(f.student,"PUT","/auth/profile",Map.of("nickname","恢复年级","grade",1),200);
  String unbound=preview(f);call(f.parent,"POST","/family/links/"+f.link+"/unbind",Map.of("confirm",true),200);call(f.parent,"POST","/family/previews/"+unbound+"/send",Map.of("confirm",true),403);call(f.parent,"POST","/family/children/"+f.sid+"/previews",input(f,key()),403);
 }
}
