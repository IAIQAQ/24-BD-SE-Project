package cn.edu.hust.mathdrill.admin;
import cn.edu.hust.mathdrill.auth.AuthService;
import cn.edu.hust.mathdrill.practice.PracticeMapper;
import jakarta.validation.constraints.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.web.server.ResponseStatusException;

@Service public class AdminService {
 private final JdbcTemplate db;private final AuthService auth;private final PracticeMapper practices;
 public AdminService(JdbcTemplate db,AuthService auth,PracticeMapper practices){this.db=db;this.auth=auth;this.practices=practices;}
 public record QuestionInput(@NotBlank String pointId,@NotBlank @Size(max=200)String stem,@NotBlank @Size(max=128)String standardAnswer,@NotBlank @Size(max=500)String explanation){}
 public record PointInput(@NotBlank @Size(max=100)String name,@Min(1)@Max(3)int grade,@NotBlank String semester,@NotBlank @Size(max=256)String textbook,@NotBlank @Size(max=1000)String evidence,boolean enabled){}
 private ResponseStatusException bad(String text){return new ResponseStatusException(HttpStatus.BAD_REQUEST,"INVALID_INPUT: "+text);}
 private void admin(Authentication a){auth.requireRole(a,"ADMIN");}
 private long id(String value){try{long v=Long.parseLong(value);if(v<=0)throw new NumberFormatException();return v;}catch(Exception e){throw bad("编号不正确");}}
 private Map<String,Object> view(Map<String,Object> row){var out=new LinkedHashMap<String,Object>();row.forEach((k,v)->{String key=java.util.regex.Pattern.compile("_([a-z])").matcher(k).replaceAll(m->m.group(1).toUpperCase());out.put(key,v!=null&&(key.equals("id")||key.endsWith("Id"))?v.toString():v);});return out;}
 private Map<String,Object> question(long id,boolean lock){var rows=db.queryForList("SELECT * FROM question WHERE id=?"+(lock?" FOR UPDATE":""),id);if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"NOT_FOUND: 题目不存在");return rows.getFirst();}
 private String normalize(String value){return value.trim().replaceAll("\\s+","").replace('＋','+').replace('－','-').replace('−','-').replace('×','*').replace('÷','/');}
 private String fingerprint(String stem,String answer){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest((normalize(stem)+"|INTEGER|"+new BigInteger(answer.trim())).getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw bad("标准答案需为非负整数");}}
 void validate(QuestionInput input,Long except){
  if(!input.standardAnswer.trim().matches("[0-9]+"))throw bad("标准答案需为非负整数");long point=id(input.pointId);if(db.queryForObject("SELECT COUNT(*) FROM knowledge_point WHERE id=?",Long.class,point)!=1)throw bad("知识点不存在");
  String candidate=fingerprint(input.stem,input.standardAnswer);
  for(var existing:db.queryForList("SELECT id,stem,standard_answer FROM question")){long other=((Number)existing.get("id")).longValue();if(Objects.equals(except,other))continue;if(candidate.equals(fingerprint((String)existing.get("stem"),(String)existing.get("standard_answer"))))throw new ResponseStatusException(HttpStatus.CONFLICT,"DUPLICATE: 已存在相同题目与标准答案");}
 }
 private void revision(long id){db.update("INSERT INTO question_revision(question_id,revision,point_id,stem,standard_answer,explanation,fingerprint) SELECT id,revision,point_id,stem,standard_answer,explanation,fingerprint FROM question WHERE id=?",id);}
 @Transactional public Object overview(Authentication a){admin(a);return Map.of("questionTotal",db.queryForObject("SELECT COUNT(*) FROM question",Long.class),"available",db.queryForObject("SELECT COUNT(*) FROM question q JOIN knowledge_point k ON k.id=q.point_id WHERE q.reviewed=TRUE AND q.enabled=TRUE AND k.enabled=TRUE",Long.class),"students",db.queryForObject("SELECT COUNT(*) FROM sys_account WHERE role='STUDENT'",Long.class),"completed",db.queryForObject("SELECT COUNT(*) FROM practice WHERE status='COMPLETED'",Long.class));}
 @Transactional public Object points(Authentication a){admin(a);return db.queryForList("SELECT * FROM knowledge_point ORDER BY grade,id").stream().map(this::view).toList();}
 @Transactional public Object point(Authentication a,Long key,PointInput input){admin(a);if(!Set.of("上册","下册").contains(input.semester))throw bad("学期需填写上册或下册");long point=key==null?db.queryForObject("SELECT UUID_SHORT()",Long.class):key;if(key==null)db.update("INSERT INTO knowledge_point(id,name,grade,semester,textbook,evidence,enabled) VALUES(?,?,?,?,?,?,?)",point,input.name,input.grade,input.semester,input.textbook,input.evidence,input.enabled);else if(db.update("UPDATE knowledge_point SET name=?,grade=?,semester=?,textbook=?,evidence=?,enabled=?,revision=revision+1 WHERE id=?",input.name,input.grade,input.semester,input.textbook,input.evidence,input.enabled,point)==0)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"NOT_FOUND: 知识点不存在");return view(db.queryForMap("SELECT * FROM knowledge_point WHERE id=?",point));}
 @Transactional public Object questions(Authentication a,String query,int page){admin(a);if(page<1)throw bad("分页不正确");String term="%"+(query==null?"":query)+"%";return Map.of("items",db.queryForList("SELECT q.*,k.name point_name,k.grade,k.semester FROM question q JOIN knowledge_point k ON k.id=q.point_id WHERE q.stem LIKE ? ORDER BY q.id DESC LIMIT 30 OFFSET ?",term,(page-1)*30).stream().map(this::view).toList(),"total",db.queryForObject("SELECT COUNT(*) FROM question WHERE stem LIKE ?",Long.class,term),"page",page);}
 @Transactional public Object question(Authentication a,Long key,QuestionInput input){admin(a);if(key!=null)question(key,true);validate(input,key);long q=key==null?db.queryForObject("SELECT UUID_SHORT()",Long.class):key;String fp=fingerprint(input.stem,input.standardAnswer);
  if(key==null)db.update("INSERT INTO question(id,point_id,stem,standard_answer,explanation,fingerprint,reviewed,enabled) VALUES(?,?,?,?,?,?,FALSE,FALSE)",q,id(input.pointId),input.stem.trim(),new BigInteger(input.standardAnswer.trim()).toString(),input.explanation.trim(),fp);
  else db.update("UPDATE question SET point_id=?,stem=?,standard_answer=?,explanation=?,fingerprint=?,reviewed=FALSE,enabled=FALSE,revision=revision+1 WHERE id=?",id(input.pointId),input.stem.trim(),new BigInteger(input.standardAnswer.trim()).toString(),input.explanation.trim(),fp,q);
  revision(q);return view(question(q,false));
 }
 @Transactional public Object review(Authentication a,long id,boolean confirm,Long expectedRevision){admin(a);if(!confirm||expectedRevision==null)throw bad("请确认已核对当前版本题干、分类、答案及解析");var q=question(id,true);if(((Number)q.get("revision")).longValue()!=expectedRevision)throw new ResponseStatusException(HttpStatus.CONFLICT,"VERSION_CHANGED: 内容已修改，请刷新后重新核对");if(Boolean.TRUE.equals(db.queryForObject("SELECT is_error FROM question_revision WHERE question_id=? AND revision=? FOR UPDATE",Boolean.class,id,q.get("revision"))))throw new ResponseStatusException(HttpStatus.CONFLICT,"ERROR_VERSION: 错误内容需先修改产生新版本");db.update("UPDATE question SET reviewed=TRUE WHERE id=?",id);return view(question(id,false));}
 @Transactional public Object enabled(Authentication a,long id,boolean enabled){admin(a);var q=question(id,true);if(enabled&&(!Boolean.TRUE.equals(q.get("reviewed"))||!Boolean.TRUE.equals(db.queryForObject("SELECT enabled FROM knowledge_point WHERE id=?",Boolean.class,q.get("point_id")))))throw new ResponseStatusException(HttpStatus.CONFLICT,"REVIEW_REQUIRED: 题目需核对通过且知识点启用后才能启用");db.update("UPDATE question SET enabled=? WHERE id=?",enabled,id);return view(question(id,false));}
 @Transactional public Object versions(Authentication a,long id){admin(a);question(id,false);return db.queryForList("SELECT * FROM question_revision WHERE question_id=? ORDER BY revision DESC",id).stream().map(this::view).toList();}
 @Transactional public Object impact(Authentication a,long id,long revision){admin(a);question(id,false);return Map.of("unfinished",db.queryForObject("SELECT COUNT(DISTINCT p.id) FROM practice p JOIN practice_item i ON i.practice_id=p.id WHERE i.question_id=? AND i.source_revision=? AND p.status IN ('PENDING','IN_PROGRESS')",Long.class,id,revision),"completed",db.queryForObject("SELECT COUNT(DISTINCT p.id) FROM practice p JOIN practice_item i ON i.practice_id=p.id WHERE i.question_id=? AND i.source_revision=? AND p.status='COMPLETED'",Long.class,id,revision));}
 // 取得题目锁之后，必须看见此前组卷事务刚提交的练习。
 @Transactional(isolation=Isolation.READ_COMMITTED) public Object error(Authentication a,long id,long version,boolean confirm,String reason){admin(a);if(!confirm||reason==null||reason.isBlank()||reason.length()>500)throw bad("确认错误需填写原因并再次确认");var current=question(id,true);if(db.queryForObject("SELECT COUNT(*) FROM question_revision WHERE question_id=? AND revision=?",Long.class,id,version)==0)throw bad("内容版本不存在");
  db.update("UPDATE question_revision SET is_error=TRUE,error_note=? WHERE question_id=? AND revision=?",reason,id,version);if(((Number)current.get("revision")).longValue()==version)db.update("UPDATE question SET enabled=FALSE,reviewed=FALSE WHERE id=?",id);
  int affected=0;for(var task:db.queryForList("SELECT DISTINCT p.id,p.student_id FROM practice p JOIN practice_item i ON i.practice_id=p.id WHERE i.question_id=? AND i.source_revision=? ORDER BY p.id",id,version)){
    long taskId=((Number)task.get("id")).longValue();var p=practices.lock(taskId,((Number)task.get("student_id")).longValue());
    String state=Set.of("IN_PROGRESS","PENDING").contains(p.status)?"VOID":"PREVIEW".equals(p.status)?"EXPIRED":p.status;
    db.update("UPDATE practice SET status=?,content_error=TRUE,error_note=?,version=version+1 WHERE id=?",state,reason,taskId);if("VOID".equals(state)||"EXPIRED".equals(state))practices.closeSegments(taskId);affected++;
  }return Map.of("questionId",Long.toString(id),"revision",version,"affectedPractices",affected);
 }
}
