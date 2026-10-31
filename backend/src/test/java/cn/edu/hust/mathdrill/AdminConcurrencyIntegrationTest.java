package cn.edu.hust.mathdrill;
import cn.edu.hust.mathdrill.admin.AdminService;
import cn.edu.hust.mathdrill.auth.AuthService;
import java.util.*;import java.util.concurrent.*;import java.util.concurrent.atomic.AtomicReference;
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
import static org.junit.jupiter.api.Assertions.*;import static org.mockito.Mockito.*;import static org.mockito.ArgumentMatchers.*;

@SpringBootTest(properties={"server.ssl.enabled=false","mathdrill.seed-enabled=true"})
class AdminConcurrencyIntegrationTest {
 @Autowired JdbcTemplate db;@Autowired AdminService admin;@Autowired PlatformTransactionManager transactions;@MockitoSpyBean AuthService auth;
 Authentication identity(){return UsernamePasswordAuthenticationToken.authenticated("admin","unused",List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));}
 long fixture(){long id=db.queryForObject("SELECT UUID_SHORT()",Long.class);String fp=UUID.randomUUID().toString().replace("-","").repeat(2);db.update("INSERT INTO question(id,point_id,stem,standard_answer,explanation,fingerprint,reviewed,enabled) VALUES(?,1,?,'4','隔离并发测试',?,TRUE,TRUE)",id,"并发测试 "+id,fp);db.update("INSERT INTO question_revision(question_id,revision,point_id,stem,standard_answer,explanation,fingerprint) SELECT id,revision,point_id,stem,standard_answer,explanation,fingerprint FROM question WHERE id=?",id);return id;}
 void gate(CountDownLatch snapshot,CountDownLatch release){doAnswer(invocation->{Object result=invocation.callRealMethod();if(Thread.currentThread().getName().startsWith("i2-race")){snapshot.countDown();assertTrue(release.await(10,TimeUnit.SECONDS));}return result;}).when(auth).requireRole(any(Authentication.class),eq("ADMIN"));}
 @Test void errorDispositionSeesPracticeCommittedAfterItsAuthenticationRead()throws Exception{
  long q=fixture();var snapshot=new CountDownLatch(1);var release=new CountDownLatch(1);gate(snapshot,release);var pool=Executors.newSingleThreadExecutor(r->new Thread(r,"i2-race-error"));var future=new AtomicReference<Future<Object>>();
  try{long practice=new TransactionTemplate(transactions).execute(status->{db.queryForMap("SELECT * FROM question WHERE id=? FOR UPDATE",q);future.set(pool.submit(()->admin.error(identity(),q,1,true,"并发创建后必须作废")));try{assertTrue(snapshot.await(5,TimeUnit.SECONDS));}catch(InterruptedException e){throw new RuntimeException(e);}long student=db.queryForObject("SELECT id FROM sys_account WHERE login_name='student_a'",Long.class);String key=UUID.randomUUID().toString();db.update("INSERT INTO practice(student_id,point_id,total,create_key,create_hash) VALUES(?,1,1,?,?)",student,key,"0".repeat(64));long id=db.queryForObject("SELECT id FROM practice WHERE student_id=? AND create_key=?",Long.class,student,key);db.update("INSERT INTO practice_item(practice_id,seq,question_id,stem,standard_answer,explanation,fingerprint) SELECT ?,1,id,stem,standard_answer,explanation,fingerprint FROM question WHERE id=?",id,q);return id;});release.countDown();future.get().get(10,TimeUnit.SECONDS);assertEquals("VOID",db.queryForObject("SELECT status FROM practice WHERE id=?",String.class,practice));}
  finally{release.countDown();pool.shutdownNow();db.update("UPDATE question SET enabled=FALSE,reviewed=FALSE WHERE id=?",q);}
 }
 @Test void reviewCannotUseAnOldErrorFlagAfterWaitingForTheQuestionLock()throws Exception{
  long q=fixture();var snapshot=new CountDownLatch(1);var release=new CountDownLatch(1);gate(snapshot,release);var pool=Executors.newSingleThreadExecutor(r->new Thread(r,"i2-race-review"));var future=new AtomicReference<Future<Object>>();
  try{new TransactionTemplate(transactions).executeWithoutResult(status->{db.queryForMap("SELECT * FROM question WHERE id=? FOR UPDATE",q);future.set(pool.submit(()->admin.review(identity(),q,true,1L)));try{assertTrue(snapshot.await(5,TimeUnit.SECONDS));}catch(InterruptedException e){throw new RuntimeException(e);}db.update("UPDATE question_revision SET is_error=TRUE WHERE question_id=? AND revision=1",q);db.update("UPDATE question SET enabled=FALSE,reviewed=FALSE WHERE id=?",q);});release.countDown();var failure=assertThrows(ExecutionException.class,()->future.get().get(10,TimeUnit.SECONDS));assertInstanceOf(org.springframework.web.server.ResponseStatusException.class,failure.getCause());assertFalse(db.queryForObject("SELECT reviewed FROM question WHERE id=?",Boolean.class,q));}
  finally{release.countDown();pool.shutdownNow();db.update("UPDATE question SET enabled=FALSE,reviewed=FALSE WHERE id=?",q);}
 }
}
