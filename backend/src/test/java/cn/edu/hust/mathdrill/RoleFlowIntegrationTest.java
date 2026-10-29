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
class RoleFlowIntegrationTest {
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
 record Family(Client parent,Client student,String childId,String linkId){}
 Family family(boolean confirm)throws Exception{
   String s="s_"+key(),p="p_"+key();var child=register(s,"STUDENT");register(p,"PARENT");var parent=login(p);var student=login(s);
   var link=call(parent,"POST","/family/requests",Map.of("studentLoginName",s),200);String linkId=link.path("id").asText();
   if(confirm)call(student,"POST","/family/links/"+linkId+"/respond",Map.of("accept",true),200);
   return new Family(parent,student,child.path("id").asText(),linkId);
 }
 @Test void publicRegistrationCannotCreateAdminAndRolesStaySeparated()throws Exception{
   String s="s_"+key();register(s,"STUDENT");call(guest(),"POST","/auth/register",Map.of("loginName",s,"password",password,"nickname","重名","role","STUDENT","grade",1),409);
   call(guest(),"POST","/auth/register",Map.of("loginName","a_"+key(),"password",password,"nickname","管理员","role","ADMIN","grade",1),400);
   call(guest(),"POST","/auth/register",Map.of("loginName","admin","password",password,"nickname","保留名","role","PARENT","grade",1),400);
   String p="p_"+key();register(p,"PARENT");var parent=login(p);assertEquals("PARENT",call(parent,"GET","/auth/me",null,200).path("role").asText());
   call(parent,"POST","/practices",Map.of("pointId","1","total",1,"operationKey",key()),403);call(login(s),"GET","/admin/overview",null,403);
 }
 @Test void pendingFamilyHasNoAccessAndUnbindingPreservesStudentTasks()throws Exception{
   var f=family(false);call(f.parent,"GET","/family/children/"+f.childId+"/practices",null,403);
   call(f.student,"POST","/family/links/"+f.linkId+"/respond",Map.of("accept",true),200);
   var preview=call(f.parent,"POST","/family/children/"+f.childId+"/previews",Map.of("pointId","1","total",3,"operationKey",key()),200);String id=preview.path("id").asText();assertEquals(3,preview.path("items").size());
   assertFalse(preview.toString().contains("standardAnswer"));assertEquals(0,call(f.student,"GET","/practices",null,200).path("total").asInt());
   var sent=call(f.parent,"POST","/family/previews/"+id+"/send",Map.of("confirm",true),200);assertEquals("PENDING",sent.path("status").asText());
   assertEquals(id,call(f.parent,"POST","/family/previews/"+id+"/send",Map.of("confirm",true),200).path("id").asText());
   call(f.parent,"POST","/family/links/"+f.linkId+"/unbind",Map.of("confirm",true),200);
   call(f.parent,"GET","/family/children/"+f.childId+"/practices",null,403);call(f.parent,"POST","/family/practices/"+id+"/withdraw",Map.of("confirm",true),403);
   assertEquals("PENDING",call(f.student,"GET","/practices/"+id+"/draft",null,200).path("status").asText());
   assertEquals("IN_PROGRESS",call(f.student,"POST","/practices/"+id+"/open",Map.of("operationKey",key(),"takeover",false),200).path("status").asText());
 }
 @Test void onlySenderCanWithdrawAndStartedTasksCannotBeWithdrawn()throws Exception{
   var f=family(true);var preview=call(f.parent,"POST","/family/children/"+f.childId+"/previews",Map.of("pointId","1","total",1,"operationKey",key()),200);String id=preview.path("id").asText();
   call(f.parent,"POST","/family/previews/"+id+"/send",Map.of("confirm",true),200);
   call(f.student,"POST","/practices/"+id+"/open",Map.of("operationKey",key(),"takeover",false),200);call(f.parent,"POST","/family/practices/"+id+"/withdraw",Map.of("confirm",true),409);
 }
 @Test void administratorEditsNeedReviewAndDoNotChangeExistingSnapshots()throws Exception{
   var admin=login("admin",adminPassword);call(admin,"GET","/admin/overview",null,200);
   var input=new HashMap<String,Object>(Map.of("pointId","1","stem","测试填空 "+key(),"standardAnswer","15","explanation","测试解析"));
   var created=call(admin,"POST","/admin/questions",input,200);String id=created.path("id").asText();assertFalse(created.path("enabled").asBoolean());
   call(admin,"POST","/admin/questions/"+id+"/enabled",Map.of("enabled",true),409);
   call(admin,"POST","/admin/questions/"+id+"/review",Map.of("confirm",true,"expectedRevision",1),200);call(admin,"POST","/admin/questions/"+id+"/enabled",Map.of("enabled",true),200);
   input.put("explanation","修改后重新核对");var updated=call(admin,"PUT","/admin/questions/"+id,input,200);assertFalse(updated.path("reviewed").asBoolean());assertEquals(2,updated.path("revision").asInt()); call(admin,"POST","/admin/questions/"+id+"/review",Map.of("confirm",true,"expectedRevision",1),409);
 }
 @Test void confirmedMathErrorVoidsUnfinishedTaskButKeepsSavedInput()throws Exception{
   var f=family(true);String id=call(f.parent,"POST","/family/children/"+f.childId+"/previews",Map.of("pointId","1","total",1,"operationKey",key()),200).path("id").asText();call(f.parent,"POST","/family/previews/"+id+"/send",Map.of("confirm",true),200);
   var editor=call(f.student,"POST","/practices/"+id+"/open",Map.of("operationKey",key(),"takeover",false),200);
   var draft=call(f.student,"GET","/practices/"+id+"/draft",null,200);String item=draft.path("items").get(0).path("id").asText();
   var input=Map.of("editorSessionId",editor.path("editorSessionId").asText(),"baseVersion",editor.path("version").asLong(),"operationKey",key(),"currentSeq",1,"answers",List.of(Map.of("itemId",item,"raw","17")));
   call(f.student,"PUT","/practices/"+id+"/answers",input,200);
   long source=db.queryForObject("SELECT question_id FROM practice_item WHERE id=?",Long.class,item);long revision=db.queryForObject("SELECT source_revision FROM practice_item WHERE id=?",Long.class,item);
   String completedKey=key();db.update("INSERT INTO practice(student_id,point_id,total,status,create_key,create_hash,correct_count,submitted_at) SELECT student_id,point_id,1,'COMPLETED',?,?,1,CURRENT_TIMESTAMP(3) FROM practice WHERE id=?",completedKey,"0".repeat(64),id);
   long completed=db.queryForObject("SELECT id FROM practice WHERE student_id=? AND create_key=?",Long.class,f.childId,completedKey);
   db.update("INSERT INTO practice_item(practice_id,seq,question_id,stem,standard_answer,explanation,fingerprint,source_revision,raw,outcome) SELECT ?,1,question_id,stem,standard_answer,explanation,fingerprint,source_revision,standard_answer,'CORRECT' FROM practice_item WHERE id=?",completed,item);
   try{call(login("admin",adminPassword),"POST","/admin/questions/"+source+"/versions/"+revision+"/error",Map.of("confirm",true,"reason","隔离测试的错误版本处置"),200);
     var retained=call(f.student,"GET","/practices/"+completed+"/result",null,200);assertEquals("COMPLETED",retained.path("status").asText());assertEquals(1,retained.path("correct").asInt());assertTrue(retained.path("contentError").asBoolean());assertEquals("100.00",retained.path("accuracy").asText());
     var after=call(f.student,"GET","/practices/"+id+"/draft",null,200);assertEquals("VOID",after.path("status").asText());assertEquals("17",after.path("items").get(0).path("raw").asText());var reconciled=call(f.student,"POST","/practices/"+id+"/submission-status",Map.of("editorSessionId",editor.path("editorSessionId").asText(),"baseVersion",after.path("version").asLong(),"operationKey",key()),200);assertEquals("VOID",reconciled.path("status").asText());assertFalse(reconciled.path("canEdit").asBoolean());call(f.student,"PUT","/practices/"+id+"/answers",input,409);
   }finally{db.update("UPDATE question SET reviewed=TRUE,enabled=TRUE WHERE id=?",source);db.update("UPDATE question_revision SET is_error=FALSE,error_note='' WHERE question_id=? AND revision=?",source,revision);}
 }
 byte[] workbook(boolean invalid)throws Exception{
   try(var book=new org.apache.poi.xssf.usermodel.XSSFWorkbook();var output=new java.io.ByteArrayOutputStream()){
     var sheet=book.createSheet("题库");String[] headers={"题干","年级","学期","知识点","答案类型","标准答案","解析"};var head=sheet.createRow(0);for(int i=0;i<headers.length;i++)head.createCell(i).setCellValue(headers[i]);
     for(int n=0;n<(invalid?2:1);n++){var row=sheet.createRow(n+1);String[] values={"导入测试 "+key(),"1","上册","20以内加法（综合）","整数",invalid&&n==1?"bad":"15","隔离测试解析"};for(int i=0;i<values.length;i++)row.createCell(i).setCellValue(values[i]);}book.write(output);return output.toByteArray();
   }
 }
 JsonNode upload(Client admin,byte[] bytes)throws Exception{var csrf=mvc.perform(get("/api/v1/auth/csrf").secure(true).cookie(admin.cookie)).andReturn();var file=new org.springframework.mock.web.MockMultipartFile("file","catalog.xlsx","application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",bytes);var r=mvc.perform(multipart("/api/v1/admin/imports").file(file).secure(true).cookie(admin.cookie).header("X-CSRF-TOKEN",body(csrf).at("/data/token").asText())).andReturn();assertEquals(200,r.getResponse().getStatus());return body(r).path("data");}
 @Test void excelImportRejectsWholeInvalidBatchAndConfirmsOnlyOnce()throws Exception{
   var admin=login("admin",adminPassword);long before=db.queryForObject("SELECT COUNT(*) FROM question",Long.class);var invalid=upload(admin,workbook(true));assertEquals("REJECTED",invalid.path("state").asText());assertEquals(0,invalid.path("success").asInt());assertEquals(before,db.queryForObject("SELECT COUNT(*) FROM question",Long.class));
   byte[] bytes=workbook(false);var ready=upload(admin,bytes);assertEquals("READY",ready.path("state").asText());assertEquals(ready.path("id"),upload(admin,bytes).path("id"));String batch=ready.path("id").asText();
   assertEquals("IMPORTED",call(admin,"POST","/admin/imports/"+batch+"/confirm",Map.of("confirm",true),200).path("state").asText());call(admin,"POST","/admin/imports/"+batch+"/confirm",Map.of("confirm",true),200);assertEquals(before+1,db.queryForObject("SELECT COUNT(*) FROM question",Long.class));
 }
 @Test void parentStatsAreScopedToCurrentAssociationAndValidDates()throws Exception{
   var f=family(true);var stats=call(f.parent,"GET","/family/children/"+f.childId+"/stats",null,200);assertEquals(0,stats.path("completed").asInt());
   call(f.parent,"GET","/family/children/"+f.childId+"/stats?start=2026-10-28&end=2026-10-01",null,400);
   call(f.parent,"POST","/family/links/"+f.linkId+"/unbind",Map.of("confirm",true),200);call(f.parent,"GET","/family/children/"+f.childId+"/stats",null,403);
 }
 @Test void aSemesterChangeInvalidatesTheFixedPreview()throws Exception{
   var f=family(true);String id=call(f.parent,"POST","/family/children/"+f.childId+"/previews",Map.of("pointId","1","total",1,"operationKey",key()),200).path("id").asText();
   var original=db.queryForMap("SELECT * FROM knowledge_point WHERE id=1");
   try{call(login("admin",adminPassword),"PUT","/admin/points/1",Map.of("name",original.get("name"),"grade",1,"semester","下册","textbook",original.get("textbook"),"evidence",original.get("evidence"),"enabled",true),200);
     call(f.parent,"POST","/family/previews/"+id+"/send",Map.of("confirm",true),409);
   }finally{db.update("UPDATE knowledge_point SET semester=?,revision=revision+1 WHERE id=1",original.get("semester"));}
 }
 @Test void aGradeChangeOrExpiredPreviewRequiresNewPreview()throws Exception{
   var f=family(true);var config=Map.of("pointId","1","total",1,"operationKey",key());String id=call(f.parent,"POST","/family/children/"+f.childId+"/previews",config,200).path("id").asText();
   call(f.student,"PUT","/auth/profile",Map.of("nickname","升年级","grade",2),200);call(f.parent,"POST","/family/previews/"+id+"/send",Map.of("confirm",true),409);
   call(f.student,"PUT","/auth/profile",Map.of("nickname","测试学生","grade",1),200);db.update("UPDATE practice SET created_at=DATE_SUB(CURRENT_TIMESTAMP(3),INTERVAL 16 MINUTE) WHERE id=?",id);call(f.parent,"POST","/family/previews/"+id+"/send",Map.of("confirm",true),409);
 }
}
