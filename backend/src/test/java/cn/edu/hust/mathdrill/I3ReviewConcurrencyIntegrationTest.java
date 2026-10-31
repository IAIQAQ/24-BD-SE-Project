package cn.edu.hust.mathdrill;

import cn.edu.hust.mathdrill.admin.AdminService;
import cn.edu.hust.mathdrill.auth.AuthService;
import cn.edu.hust.mathdrill.practice.ReviewPracticeService;
import cn.edu.hust.mathdrill.practice.PracticeService;
import cn.edu.hust.mathdrill.family.FamilyService;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@SpringBootTest(properties={"server.ssl.enabled=false","mathdrill.seed-enabled=true"})
class I3ReviewConcurrencyIntegrationTest {
 @Autowired JdbcTemplate db;@Autowired AdminService admin;@Autowired ReviewPracticeService review;@Autowired PlatformTransactionManager transactions;@MockitoSpyBean AuthService auth;
 @Autowired PracticeService practices;@Autowired FamilyService family;
 Authentication identity(String name,String role){return UsernamePasswordAuthenticationToken.authenticated(name,"unused",List.of(new SimpleGrantedAuthority("ROLE_"+role)));}
 record Fixture(long question,String item){}
 Fixture fixture(){
  long q=db.queryForObject("SELECT UUID_SHORT()",Long.class);String fp=UUID.randomUUID().toString().replace("-","").repeat(2);db.update("INSERT INTO question(id,point_id,stem,standard_answer,explanation,fingerprint,reviewed,enabled) VALUES(?,1,?,'4','并发验证',?,FALSE,FALSE)",q,"重练竞争样例 "+q,fp);
  db.update("INSERT INTO question_revision(question_id,revision,point_id,stem,standard_answer,explanation,fingerprint) SELECT id,revision,point_id,stem,standard_answer,explanation,fingerprint FROM question WHERE id=?",q);
  long sid=db.queryForObject("SELECT id FROM sys_account WHERE login_name='student_a'",Long.class);String key=UUID.randomUUID().toString();db.update("INSERT INTO practice(student_id,point_id,total,status,create_key,create_hash,correct_count,submitted_at) VALUES(?,1,1,'COMPLETED',?,?,0,CURRENT_TIMESTAMP(3))",sid,key,"0".repeat(64));long p=db.queryForObject("SELECT id FROM practice WHERE student_id=? AND create_key=?",Long.class,sid,key);
  db.update("INSERT INTO practice_item(practice_id,seq,question_id,stem,standard_answer,explanation,fingerprint,raw,outcome) SELECT ?,1,id,stem,standard_answer,explanation,fingerprint,'999','WRONG' FROM question WHERE id=?",p,q);long item=db.queryForObject("SELECT id FROM practice_item WHERE practice_id=?",Long.class,p);db.update("INSERT INTO mistake_record(student_id,practice_id,item_id,fingerprint) VALUES(?,?,?,?)",sid,p,item,fp);return new Fixture(q,Long.toString(item));
 }
 @Test void errorDispositionAndReplayCreationCannotMissEachOther()throws Exception{
  var f=fixture();var snapshot=new CountDownLatch(1);var release=new CountDownLatch(1);String key=UUID.randomUUID().toString();
  doAnswer(invocation->{var result=invocation.callRealMethod();if(Thread.currentThread().getName().equals("replay-race")){snapshot.countDown();assertTrue(release.await(10,TimeUnit.SECONDS));}return result;}).when(auth).requireRole(any(Authentication.class),eq("STUDENT"));
  var pool=Executors.newSingleThreadExecutor(r->new Thread(r,"replay-race"));var future=new AtomicReference<Future<Object>>();
  try{new TransactionTemplate(transactions).executeWithoutResult(status->{db.queryForMap("SELECT * FROM question WHERE id=? FOR UPDATE",f.question);future.set(pool.submit(()->review.replay(identity("student_a","STUDENT"),new ReviewPracticeService.ReplayInput(List.of(f.item),key))));try{assertTrue(snapshot.await(5,TimeUnit.SECONDS));}catch(InterruptedException e){throw new RuntimeException(e);}admin.error(identity("admin","ADMIN"),f.question,1,true,"并发确认后不能新建错误重练");});release.countDown();var failure=assertThrows(ExecutionException.class,()->future.get().get(10,TimeUnit.SECONDS));assertInstanceOf(ResponseStatusException.class,failure.getCause());assertEquals(409,((ResponseStatusException)failure.getCause()).getStatusCode().value());assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM practice WHERE create_key=?",Integer.class,key));}
  finally{release.countDown();pool.shutdownNow();}
 }
 @Test void replayCreatedFirstIsVoidedByLaterContentError(){
  var f=fixture();String key=UUID.randomUUID().toString();review.replay(identity("student_a","STUDENT"),new ReviewPracticeService.ReplayInput(List.of(f.item),key));admin.error(identity("admin","ADMIN"),f.question,1,true,"创建后确认错误");assertEquals("VOID",db.queryForObject("SELECT status FROM practice WHERE create_key=?",String.class,key));
 }

 record ReinforceFixture(long question,long point,String item,long student,long parent,String parentName){}
 ReinforceFixture reinforceFixture(){
  var f=fixture();long point=db.queryForObject("SELECT UUID_SHORT()",Long.class);db.update("INSERT INTO knowledge_point(id,name,grade,semester) VALUES(?,'强化并发隔离',1,'上册')",point);db.update("UPDATE question SET point_id=?,enabled=TRUE,reviewed=TRUE WHERE id=?",point,f.question);db.update("UPDATE question_revision SET point_id=? WHERE question_id=?",point,f.question);
  long source=db.queryForObject("SELECT practice_id FROM practice_item WHERE id=?",Long.class,f.item);db.update("UPDATE practice SET point_id=?,point_name='强化并发隔离' WHERE id=?",point,source);db.update("UPDATE practice_item SET point_id=?,point_revision=1 WHERE id=?",point,f.item);db.update("INSERT INTO practice_point(practice_id,point_id,point_revision,original_grade,point_name,total) VALUES(?,?,1,1,'强化并发隔离',1)",source,point);
  String name="p_race_"+UUID.randomUUID();db.update("INSERT INTO sys_account(login_name,password_hash,role) SELECT ?,password_hash,'PARENT' FROM sys_account WHERE login_name='student_a'",name);long parent=db.queryForObject("SELECT id FROM sys_account WHERE login_name=?",Long.class,name),student=db.queryForObject("SELECT id FROM sys_account WHERE login_name='student_a'",Long.class);db.update("INSERT INTO parent_profile(account_id,nickname) VALUES(?,'隔离并发家长')",parent);db.update("INSERT INTO family_link(parent_id,student_id,state) VALUES(?,?,'ACTIVE')",parent,student);return new ReinforceFixture(f.question,point,f.item,student,parent,name);
 }
 ReviewPracticeService.ReinforceInput input(ReinforceFixture f,String key){return new ReviewPracticeService.ReinforceInput(List.of(f.item),List.of(new ReviewPracticeService.Allocation(Long.toString(f.point),1)),key);}
 long preview(ReinforceFixture f){var result=(Map<?,?>)family.reinforcementPreview(identity(f.parentName,"PARENT"),f.student,input(f,UUID.randomUUID().toString()));return Long.parseLong(result.get("id").toString());}
 String status(long task){return db.queryForObject("SELECT status FROM practice WHERE id=?",String.class,task);}
 Object overlap(Runnable first,java.util.function.Supplier<Object> second,String role)throws Exception{
  var snapshot=new CountDownLatch(1);var release=new CountDownLatch(1);
  doAnswer(invocation->{var result=invocation.callRealMethod();if(Thread.currentThread().getName().equals("reinforce-race")){snapshot.countDown();assertTrue(release.await(10,TimeUnit.SECONDS));}return result;}).when(auth).requireRole(any(Authentication.class),eq(role));
  var pool=Executors.newSingleThreadExecutor(r->new Thread(r,"reinforce-race"));var future=new AtomicReference<Future<Object>>();
  try{new TransactionTemplate(transactions).executeWithoutResult(tx->{first.run();future.set(pool.submit(second::get));try{assertTrue(snapshot.await(5,TimeUnit.SECONDS));}catch(InterruptedException e){Thread.currentThread().interrupt();throw new RuntimeException(e);}});release.countDown();return future.get().get(10,TimeUnit.SECONDS);}
  finally{release.countDown();pool.shutdownNow();}
 }
 void conflictRace(Runnable first,java.util.function.Supplier<Object> second,String role)throws Exception{
  var e=assertThrows(ExecutionException.class,()->overlap(first,second,role));assertInstanceOf(ResponseStatusException.class,e.getCause());assertEquals(409,((ResponseStatusException)e.getCause()).getStatusCode().value());
 }
 @Test void errorCommittedDuringReinforcementCreationWritesNoPractice()throws Exception{
  var f=reinforceFixture();String key=UUID.randomUUID().toString();conflictRace(()->admin.error(identity("admin","ADMIN"),f.question,1,true,"强化组卷前错误"),()->review.reinforce(identity("student_a","STUDENT"),input(f,key)),"STUDENT");assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM practice WHERE create_key=?",Integer.class,key));
 }
 @Test void errorDispositionSeesReinforcementCommittedDuringItsAuthentication()throws Exception{
  var f=reinforceFixture();String key=UUID.randomUUID().toString();overlap(()->review.reinforce(identity("student_a","STUDENT"),input(f,key)),()->admin.error(identity("admin","ADMIN"),f.question,1,true,"强化组卷后错误"),"ADMIN");assertEquals("VOID",db.queryForObject("SELECT status FROM practice WHERE create_key=?",String.class,key));
 }
 @Test void errorCommittedDuringPreviewSendCannotPublishStaleQuestions()throws Exception{
  var f=reinforceFixture();long task=preview(f);conflictRace(()->admin.error(identity("admin","ADMIN"),f.question,1,true,"强化发送前错误"),()->family.send(identity(f.parentName,"PARENT"),task,true),"PARENT");assertEquals("EXPIRED",status(task));assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM practice_receipt WHERE practice_id=?",Integer.class,task));
 }
 @Test void errorDispositionSeesReinforcementSentDuringItsAuthentication()throws Exception{
  var f=reinforceFixture();long task=preview(f);overlap(()->family.send(identity(f.parentName,"PARENT"),task,true),()->admin.error(identity("admin","ADMIN"),f.question,1,true,"强化发送后错误"),"ADMIN");assertEquals("VOID",status(task));
 }
 @Test void withdrawalAfterConcurrentStartIsRejected()throws Exception{
  var f=reinforceFixture();long task=preview(f);family.send(identity(f.parentName,"PARENT"),task,true);conflictRace(()->practices.open(identity("student_a","STUDENT"),task,new PracticeService.Open(UUID.randomUUID().toString(),false)),()->family.withdraw(identity(f.parentName,"PARENT"),task,true),"PARENT");assertEquals("IN_PROGRESS",status(task));assertNotNull(db.queryForObject("SELECT editor_session_id FROM practice WHERE id=?",String.class,task));
 }
 @Test void startAfterConcurrentWithdrawalCannotAcquireEditingRights()throws Exception{
  var f=reinforceFixture();long task=preview(f);family.send(identity(f.parentName,"PARENT"),task,true);var result=(Map<?,?>)overlap(()->family.withdraw(identity(f.parentName,"PARENT"),task,true),()->practices.open(identity("student_a","STUDENT"),task,new PracticeService.Open(UUID.randomUUID().toString(),false)),"STUDENT");assertEquals("WITHDRAWN",result.get("status"));assertEquals("WITHDRAWN",status(task));assertNull(db.queryForObject("SELECT editor_session_id FROM practice WHERE id=?",String.class,task));
 }
}
