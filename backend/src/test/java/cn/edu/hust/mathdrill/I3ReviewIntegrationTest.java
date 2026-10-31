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
class I3ReviewIntegrationTest {
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

 record Fixture(Client student,String sid,String point,String question,String item,String original,JsonNode result){}
 String newPoint(int grade)throws Exception{
   var a=login("admin",adminPassword);return call(a,"POST","/admin/points",Map.of("name","I3测试"+key(),"grade",grade,"semester","上册","textbook","隔离测试","evidence","独立可推算样例","enabled",true),200).path("id").asText();
 }
 String newQuestion(String point)throws Exception{
   var a=login("admin",adminPassword);String id=call(a,"POST","/admin/questions",Map.of("pointId",point,"stem","2 + 3 =（"+key()+"）","standardAnswer","5","explanation","2再增加3等于5"),200).path("id").asText();
   call(a,"POST","/admin/questions/"+id+"/review",Map.of("confirm",true,"expectedRevision",1),200);call(a,"POST","/admin/questions/"+id+"/enabled",Map.of("enabled",true),200);return id;
 }
 JsonNode attempt(Client student,String point,String raw)throws Exception{
   String id=call(student,"POST","/practices",Map.of("pointId",point,"total",1,"operationKey",key()),200).path("id").asText();
   return complete(student,id,raw);
 }
 JsonNode complete(Client student,String id,String raw)throws Exception{
   var e=call(student,"POST","/practices/"+id+"/open",Map.of("operationKey",key(),"takeover",false),200);var d=call(student,"GET","/practices/"+id+"/draft",null,200);
   return call(student,"POST","/practices/"+id+"/submit",Map.of("editorSessionId",e.path("editorSessionId").asText(),"baseVersion",e.path("version").asLong(),"operationKey",key(),"currentSeq",1,"answers",List.of(Map.of("itemId",d.path("items").get(0).path("id").asText(),"raw",raw))),200);
 }
 Fixture fixture()throws Exception{
   String name="s_"+key();var profile=register(name,"STUDENT");var c=login(name);String point=newPoint(1),q=newQuestion(point);var result=attempt(c,point,"999");
   return new Fixture(c,profile.path("id").asText(),point,q,result.path("items").get(0).path("id").asText(),result.path("id").asText(),result);
 }
 @Test void replayCopiesOldSnapshotAfterNormalEditAndDisable()throws Exception{
   var f=fixture();var a=login("admin",adminPassword);
   call(a,"PUT","/admin/questions/"+f.question,Map.of("pointId",f.point,"stem","新题 "+key(),"standardAnswer","9","explanation","新版本解析"),200);
   db.update("UPDATE knowledge_point SET enabled=FALSE WHERE id=?",f.point);call(f.student,"PUT","/auth/profile",Map.of("nickname","升年级学生","grade",2),200);
   var options=call(f.student,"POST","/students/"+f.sid+"/review-options",Map.of("itemIds",List.of(f.item)),200);assertTrue(options.at("/replay/allowed").asBoolean());
   var input=Map.of("itemIds",List.of(f.item),"operationKey",key());var replay=call(f.student,"POST","/practices/replays",input,200);String id=replay.path("id").asText();
   assertNotEquals(f.original,id);assertEquals("REPLAY",replay.path("practiceKind").asText());assertEquals(1,replay.path("grade").asInt());assertEquals(id,call(f.student,"POST","/practices/replays",input,200).path("id").asText());
   var draft=call(f.student,"GET","/practices/"+id+"/draft",null,200);var item=draft.path("items").get(0);assertNotEquals(f.item,item.path("id").asText());assertEquals("",item.path("raw").asText());assertFalse(draft.toString().contains("standardAnswer"));assertFalse(draft.toString().contains("explanation"));assertEquals(f.result.path("items").get(0).path("stem"),item.path("stem"));
   var result=complete(f.student,id,"5");assertEquals("100.00",result.path("accuracy").asText());assertEquals("5",result.path("items").get(0).path("standardAnswer").asText());assertEquals(f.result.path("items").get(0).path("explanation"),result.path("items").get(0).path("explanation"));assertEquals("0.00",call(f.student,"GET","/practices/"+f.original+"/result",null,200).path("accuracy").asText());
   var mistakes=call(f.student,"GET","/students/"+f.sid+"/mistakes",null,200);assertEquals(1,mistakes.path("totalErrors").asInt());assertEquals(1,mistakes.path("total").asInt());
   var detail=call(f.student,"GET","/students/"+f.sid+"/mistakes/"+f.item,null,200);assertEquals(2,detail.path("history").size());assertTrue(detail.path("history").toString().contains("CORRECT"));
 }
 @Test void replayRejectsForeignCorrectDuplicateAndErrorMarkedItems()throws Exception{
   var f=fixture();String other="s_"+key();register(other,"STUDENT");call(login(other),"POST","/practices/replays",Map.of("itemIds",List.of(f.item),"operationKey",key()),403);
   String parent="p_"+key();register(parent,"PARENT");call(login(parent),"POST","/practices/replays",Map.of("itemIds",List.of(f.item),"operationKey",key()),403);
   call(f.student,"POST","/practices/replays",Map.of("itemIds",List.of(f.item,f.item),"operationKey",key()),400);call(f.student,"POST","/practices/replays",Map.of("itemIds",List.of(),"operationKey",key()),400);
   String correct=attempt(f.student,f.point,"5").path("items").get(0).path("id").asText();call(f.student,"POST","/practices/replays",Map.of("itemIds",List.of(correct),"operationKey",key()),403);
   call(login("admin",adminPassword),"POST","/admin/questions/"+f.question+"/versions/1/error",Map.of("confirm",true,"reason","测试错误版本禁止重练"),200);
   assertFalse(call(f.student,"POST","/students/"+f.sid+"/review-options",Map.of("itemIds",List.of(f.item)),200).at("/replay/allowed").asBoolean());call(f.student,"POST","/practices/replays",Map.of("itemIds",List.of(f.item),"operationKey",key()),409);
 }
 @Test void replayDeduplicatesHistoryButRejectsMixedGrades()throws Exception{
   var f=fixture();String second=attempt(f.student,f.point,"999").path("items").get(0).path("id").asText();var ids=List.of(f.item,second);
   assertEquals(1,call(f.student,"POST","/students/"+f.sid+"/review-options",Map.of("itemIds",ids),200).at("/replay/total").asInt());assertEquals(1,call(f.student,"POST","/practices/replays",Map.of("itemIds",ids,"operationKey",key()),200).path("total").asInt());
   call(f.student,"PUT","/auth/profile",Map.of("nickname","跨年级样例","grade",2),200);String p=newPoint(2);newQuestion(p);String grade2=attempt(f.student,p,"999").path("items").get(0).path("id").asText();call(f.student,"POST","/practices/replays",Map.of("itemIds",List.of(f.item,grade2),"operationKey",key()),400);
 }
 @Test void reinforcementUsesHistoricalGradeAndCurrentReviewedPool()throws Exception{
   var f=fixture();String current=newQuestion(f.point);db.update("UPDATE question SET enabled=FALSE WHERE id=?",f.question);call(f.student,"PUT","/auth/profile",Map.of("nickname","高年级学生","grade",2),200);
   var allocation=List.of(Map.of("pointId",f.point,"total",1));var payload=Map.of("seedItemIds",List.of(f.item),"allocations",allocation,"operationKey",key());
   var options=call(f.student,"POST","/students/"+f.sid+"/review-options",Map.of("itemIds",List.of(f.item)),200);assertEquals(1,options.path("groups").get(0).path("grade").asInt());
   var practice=call(f.student,"POST","/practices/reinforcements",payload,200);assertEquals("REINFORCE",practice.path("practiceKind").asText());assertEquals(1,practice.path("grade").asInt());assertEquals(practice.path("id"),call(f.student,"POST","/practices/reinforcements",payload,200).path("id"));
   var draft=call(f.student,"GET","/practices/"+practice.path("id").asText()+"/draft",null,200);assertFalse(draft.toString().contains("standardAnswer"));assertEquals(Long.parseLong(current),db.queryForObject("SELECT question_id FROM practice_item WHERE id=?",Long.class,draft.path("items").get(0).path("id").asText()));
   long before=db.queryForObject("SELECT COUNT(*) FROM practice WHERE student_id=?",Long.class,f.sid);call(f.student,"POST","/practices/reinforcements",Map.of("seedItemIds",List.of(f.item),"allocations",List.of(Map.of("pointId",f.point,"total",2)),"operationKey",key()),409);assertEquals(before,db.queryForObject("SELECT COUNT(*) FROM practice WHERE student_id=?",Long.class,f.sid));
   call(f.student,"POST","/practices/reinforcements",Map.of("seedItemIds",List.of(f.item),"allocations",List.of(Map.of("pointId",f.point,"total",0)),"operationKey",key()),400);
   call(f.student,"POST","/practices/reinforcements",Map.of("seedItemIds",List.of(f.item),"allocations",List.of(Map.of("pointId",f.point,"total",1),Map.of("pointId",f.point,"total",1)),"operationKey",key()),400);
   db.update("UPDATE knowledge_point SET enabled=FALSE WHERE id=?",f.point);call(f.student,"POST","/practices/reinforcements",Map.of("seedItemIds",List.of(f.item),"allocations",allocation,"operationKey",key()),409);
 }
 @Test void multiPointReinforcementHasExactAllocationAndItemBasedStats()throws Exception{
   var f=fixture();newQuestion(f.point);String secondPoint=newPoint(1);for(int i=0;i<3;i++)newQuestion(secondPoint);String secondItem=attempt(f.student,secondPoint,"999").path("items").get(0).path("id").asText();
   String parentName="p_"+key();register(parentName,"PARENT");var parent=login(parentName);String studentName=call(f.student,"GET","/auth/me",null,200).path("loginName").asText();String link=call(parent,"POST","/family/requests",Map.of("studentLoginName",studentName),200).path("id").asText();call(f.student,"POST","/family/links/"+link+"/respond",Map.of("accept",true),200);
   String id=call(f.student,"POST","/practices/reinforcements",Map.of("seedItemIds",List.of(f.item,secondItem),"allocations",List.of(Map.of("pointId",f.point,"total",2),Map.of("pointId",secondPoint,"total",3)),"operationKey",key()),200).path("id").asText();var e=call(f.student,"POST","/practices/"+id+"/open",Map.of("operationKey",key(),"takeover",false),200);var d=call(f.student,"GET","/practices/"+id+"/draft",null,200);assertEquals(5,d.path("items").size());assertEquals(2,d.path("points").size());
   var counts=new HashMap<String,Integer>();var answers=new ArrayList<Map<String,String>>();for(var item:d.path("items")){String itemId=item.path("id").asText();String point=db.queryForObject("SELECT point_id FROM practice_item WHERE id=?",String.class,itemId);int n=counts.merge(point,1,Integer::sum);answers.add(Map.of("itemId",itemId,"raw",n<=(point.equals(f.point)?1:2)?"5":"999"));}
   assertEquals(2,counts.get(f.point));assertEquals(3,counts.get(secondPoint));var result=call(f.student,"POST","/practices/"+id+"/submit",Map.of("editorSessionId",e.path("editorSessionId").asText(),"baseVersion",e.path("version").asLong(),"operationKey",key(),"currentSeq",1,"answers",answers),200);assertEquals("60.00",result.path("accuracy").asText());
   var stats=call(parent,"GET","/family/children/"+f.sid+"/stats",null,200);assertEquals(7,stats.path("total").asInt());assertEquals(3,stats.path("correct").asInt());assertEquals("42.86",stats.path("accuracy").asText());for(var point:stats.path("points")){if(point.path("pointId").asText().equals(f.point)){assertEquals(3,point.path("total").asInt());assertEquals(1,point.path("correct").asInt());}else if(point.path("pointId").asText().equals(secondPoint)){assertEquals(4,point.path("total").asInt());assertEquals(2,point.path("correct").asInt());}else fail("Unexpected point");}
 }

 @Test void reinforcementPreservesHistoricalCategoriesWhenReplayDeduplicates()throws Exception{
   var f=fixture();String second=newPoint(1);var a=login("admin",adminPassword);
   var old=f.result.path("items").get(0);
   call(a,"PUT","/admin/questions/"+f.question,Map.of("pointId",second,"stem",old.path("stem").asText(),"standardAnswer","5","explanation","2再增加3等于5"),200);
   call(a,"POST","/admin/questions/"+f.question+"/review",Map.of("confirm",true,"expectedRevision",2),200);call(a,"POST","/admin/questions/"+f.question+"/enabled",Map.of("enabled",true),200);
   String later=attempt(f.student,second,"999").path("items").get(0).path("id").asText();newQuestion(f.point);
   var ids=List.of(f.item,later);var options=call(f.student,"POST","/students/"+f.sid+"/review-options",Map.of("itemIds",ids),200);
   assertEquals(1,options.at("/replay/total").asInt());var group=options.path("groups").get(0);assertEquals(2,group.path("points").size());assertEquals(new HashSet<>(ids),json.convertValue(group.path("seedItemIds"),Set.class));
   var allocations=List.of(Map.of("pointId",f.point,"total",1),Map.of("pointId",second,"total",1));var result=call(f.student,"POST","/practices/reinforcements",Map.of("seedItemIds",ids,"allocations",allocations,"operationKey",key()),200);assertEquals(2,result.path("total").asInt());assertEquals(2,result.path("points").size());
 }

 @Test void reviewBoundariesRejectOversizedSelectionsAndFractionalAllocations()throws Exception{
   var f=fixture();call(f.student,"POST","/students/"+f.sid+"/review-options",Map.of("itemIds",Collections.nCopies(101,f.item)),400);call(f.student,"POST","/practices/replays",Map.of("itemIds",Collections.nCopies(101,f.item),"operationKey",key()),400);
   long before=db.queryForObject("SELECT COUNT(*) FROM practice WHERE student_id=?",Long.class,f.sid);
   call(f.student,"POST","/practices/reinforcements",Map.of("seedItemIds",List.of(f.item),"allocations",List.of(Map.of("pointId",f.point,"total",1.5)),"operationKey",key()),400);
   assertEquals(before,db.queryForObject("SELECT COUNT(*) FROM practice WHERE student_id=?",Long.class,f.sid));
   call(f.student,"POST","/practices",Map.of("pointId",f.point,"total",1.5,"operationKey",key()),400);
   String second=newPoint(1);newQuestion(second);String later=attempt(f.student,second,"999").path("items").get(0).path("id").asText();
   call(f.student,"POST","/practices/reinforcements",Map.of("seedItemIds",List.of(f.item,later),"allocations",List.of(Map.of("pointId",f.point,"total",1)),"operationKey",key()),400);
   call(f.student,"POST","/practices/reinforcements",Map.of("seedItemIds",List.of(f.item,later),"allocations",List.of(Map.of("pointId",f.point,"total",51),Map.of("pointId",second,"total",50)),"operationKey",key()),400);
   call(f.student,"PUT","/auth/profile",Map.of("nickname","跨年级边界","grade",2),200);String grade2=newPoint(2);newQuestion(grade2);String item2=attempt(f.student,grade2,"999").path("items").get(0).path("id").asText();call(f.student,"POST","/practices/reinforcements",Map.of("seedItemIds",List.of(f.item,item2),"allocations",List.of(Map.of("pointId",f.point,"total",1),Map.of("pointId",grade2,"total",1)),"operationKey",key()),400);
 }

 Client associatedParent(Fixture f,boolean confirm)throws Exception{
   String name="p_"+key();register(name,"PARENT");var parent=login(name);String studentName=call(f.student,"GET","/auth/me",null,200).path("loginName").asText();String link=call(parent,"POST","/family/requests",Map.of("studentLoginName",studentName),200).path("id").asText();if(confirm)call(f.student,"POST","/family/links/"+link+"/respond",Map.of("accept",true),200);return parent;
 }
 Map<String,Object> reinforcement(Fixture f){return Map.of("seedItemIds",List.of(f.item),"allocations",List.of(Map.of("pointId",f.point,"total",1)),"operationKey",key());}
 @Test void parentHistoricalReinforcementUsesFixedPreviewAndCurrentAssociation()throws Exception{
   var f=fixture();var parent=associatedParent(f,false);call(parent,"POST","/family/children/"+f.sid+"/review-options",Map.of("itemIds",List.of(f.item)),403);
   String link=call(f.student,"GET","/family/links",null,200).get(0).path("id").asText();call(f.student,"POST","/family/links/"+link+"/respond",Map.of("accept",true),200);call(f.student,"PUT","/auth/profile",Map.of("nickname","高年级孩子","grade",2),200);
   assertEquals(1,call(parent,"POST","/family/children/"+f.sid+"/review-options",Map.of("itemIds",List.of(f.item)),200).path("groups").get(0).path("grade").asInt());
   var foreign=fixture();call(parent,"POST","/family/children/"+f.sid+"/reinforcement-previews",reinforcement(foreign),403);call(f.student,"POST","/practices/reinforcements",reinforcement(foreign),403);call(parent,"POST","/practices/reinforcements",reinforcement(f),403);
   var payload=reinforcement(f);var preview=call(parent,"POST","/family/children/"+f.sid+"/reinforcement-previews",payload,200);String id=preview.path("id").asText();assertEquals("REINFORCE",preview.path("practiceKind").asText());assertEquals("PREVIEW",preview.path("status").asText());assertFalse(preview.toString().contains("standardAnswer"));assertEquals(id,call(parent,"POST","/family/children/"+f.sid+"/reinforcement-previews",payload,200).path("id").asText());assertEquals(1,call(f.student,"GET","/practices",null,200).path("total").asInt());call(parent,"POST","/practices/"+id+"/open",Map.of("operationKey",key(),"takeover",false),403);
   var detail=call(parent,"GET","/family/children/"+f.sid+"/mistakes/"+f.item,null,200);assertEquals(1,detail.path("history").size());
   call(parent,"POST","/family/previews/"+id+"/send",Map.of("confirm",true),200);call(parent,"POST","/family/previews/"+id+"/send",Map.of("confirm",true),200);var draft=call(f.student,"GET","/practices/"+id+"/draft",null,200);assertEquals(preview.path("items").get(0).path("stem"),draft.path("items").get(0).path("stem"));complete(f.student,id,"5");assertEquals("100.00",call(parent,"GET","/family/children/"+f.sid+"/practices/"+id,null,200).path("accuracy").asText());
   call(parent,"POST","/practices/replays",Map.of("itemIds",List.of(f.item),"operationKey",key()),403);call(parent,"POST","/family/links/"+link+"/unbind",Map.of("confirm",true),200);call(parent,"POST","/family/children/"+f.sid+"/reinforcement-previews",reinforcement(f),403);call(parent,"GET","/family/children/"+f.sid+"/mistakes/"+f.item,null,403);call(parent,"POST","/family/previews/"+id+"/send",Map.of("confirm",true),403);
 }
 @Test void parentMultiPointPreviewRejectsEveryChangedCategoryAndLaterGradeChange()throws Exception{
   var f=fixture();String second=newPoint(1);newQuestion(second);String secondItem=attempt(f.student,second,"999").path("items").get(0).path("id").asText();var parent=associatedParent(f,true);
   var payload=new HashMap<String,Object>(Map.of("seedItemIds",List.of(f.item,secondItem),"allocations",List.of(Map.of("pointId",f.point,"total",1),Map.of("pointId",second,"total",1)),"operationKey",key()));
   String id=call(parent,"POST","/family/children/"+f.sid+"/reinforcement-previews",payload,200).path("id").asText();db.update("UPDATE knowledge_point SET revision=revision+1 WHERE id=?",second);call(parent,"POST","/family/previews/"+id+"/send",Map.of("confirm",true),409);
   payload.put("operationKey",key());id=call(parent,"POST","/family/children/"+f.sid+"/reinforcement-previews",payload,200).path("id").asText();call(f.student,"PUT","/auth/profile",Map.of("nickname","再次升年级","grade",2),200);call(parent,"POST","/family/previews/"+id+"/send",Map.of("confirm",true),409);
   payload.put("operationKey",key());id=call(parent,"POST","/family/children/"+f.sid+"/reinforcement-previews",payload,200).path("id").asText();call(parent,"POST","/family/previews/"+id+"/send",Map.of("confirm",true),200);call(parent,"POST","/family/practices/"+id+"/withdraw",Map.of("confirm",true),200);call(f.student,"PUT","/practices/"+id+"/answers",Map.of(),400);
   payload.put("operationKey",key());id=call(parent,"POST","/family/children/"+f.sid+"/reinforcement-previews",payload,200).path("id").asText();db.update("UPDATE practice SET created_at=DATE_SUB(CURRENT_TIMESTAMP(3),INTERVAL 16 MINUTE) WHERE id=?",id);call(parent,"POST","/family/previews/"+id+"/send",Map.of("confirm",true),409);
 }

}
