package cn.edu.hust.mathdrill.practice;

import cn.edu.hust.mathdrill.auth.AuthService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static cn.edu.hust.mathdrill.practice.PracticeMapper.*;

@Service
public class PracticeService {
    private final PracticeMapper mapper;private final AuthService auth;private final ObjectMapper json;private final ReviewMapper reviews;
    public PracticeService(PracticeMapper mapper,AuthService auth,ObjectMapper json,ReviewMapper reviews){this.mapper=mapper;this.auth=auth;this.json=json;this.reviews=reviews;}
    public record Allocation(@NotBlank String pointId,@NotNull @Min(1) @Max(100) Integer total){}
    public record Create(String pointId,@Min(1) @Max(100) Integer total,@NotBlank @Size(max=64) String operationKey,@Size(min=1,max=100) List<@NotNull @Valid Allocation> allocations){}
    public record Open(@NotBlank @Size(max=64) String operationKey,boolean takeover){}
    public record Answer(@NotBlank String itemId,@NotNull @Size(max=128) String raw){}
    public record Write(@NotBlank @Size(max=36) String editorSessionId,@Min(0) long baseVersion,@NotBlank @Size(max=64) String operationKey,@NotNull @Size(min=1,max=100) List<@Valid Answer> answers,@Min(1) int currentSeq,Boolean confirmUnanswered,@Valid SegmentInput lastSegment){}
    public record Probe(@NotBlank @Size(max=64) String operationKey,@NotBlank @Size(max=36) String editorSessionId,@Min(0) long baseVersion){}
    public record SegmentInput(@NotBlank @Size(max=36) String editorSessionId,@NotBlank @Size(max=36) String segmentId,@Min(0) long cumulativeMs,boolean ended){}
    private long student(Authentication a){return Long.parseLong(auth.requireRole(a,"STUDENT").id());}
    private long number(String s){try{long n=Long.parseLong(s);if(n<=0)throw new NumberFormatException();return n;}catch(Exception e){throw bad("INVALID_ID","编号格式不正确");}}
    private static ResponseStatusException bad(String code,String message){return new ResponseStatusException(HttpStatus.BAD_REQUEST,code+": "+message);}
    private static ResponseStatusException conflict(String code,String message){return new ResponseStatusException(HttpStatus.CONFLICT,code+": "+message);}
    private Practice owned(long student,long id){var p=mapper.lock(id,student);if(p==null||Set.of("PREVIEW","EXPIRED").contains(p.status))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"FORBIDDEN: 无法访问这份练习");return p;}
    private String encode(Object o){try{return json.writeValueAsString(o);}catch(Exception e){throw new IllegalStateException(e);}}
    private String hash(Object o){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(encode(o).getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
    private Map<String,Object> decode(String s){try{return json.readValue(s,new TypeReference<>(){});}catch(Exception e){throw new IllegalStateException(e);}}
    private String timestamp(LocalDateTime t){return t==null?"":t.atOffset(ZoneOffset.UTC).atZoneSameInstant(ZoneId.of("Asia/Shanghai")).toOffsetDateTime().toString();}
    private Map<String,Object> summary(Practice p){return Map.of("id",p.id.toString(),"pointId",p.pointId.toString(),"total",p.total,"status",p.status,"version",p.version,"currentSeq",p.currentSeq,"elapsedMs",p.elapsedMs,"timingComplete",p.timingComplete,"submittedAt",timestamp(p.submittedAt));}
    private Map<String,Object> editor(Practice p){var m=new LinkedHashMap<>(summary(p));m.put("editorSessionId",p.editorSessionId==null?"":p.editorSessionId);return m;}
    public Map<String,Object> taskSummary(Practice p){var m=new LinkedHashMap<>(summary(p));m.put("source",p.source);m.put("senderId",p.senderId==null?"":p.senderId.toString());m.put("grade",p.originalGrade);m.put("pointName",p.pointName);m.put("contentError",p.contentError);m.put("errorNote",p.errorNote);m.put("practiceKind",p.practiceKind);m.put("senderName",p.senderId==null?"":mapper.senderName(p.senderId));var points=mapper.practicePoints(p.id);for(var point:points){point.put("pointId",point.get("pointId").toString());}m.put("points",points);return m;}
    private void editable(Practice p,String session){if(!"IN_PROGRESS".equals(p.status))throw conflict("COMPLETED".equals(p.status)?"COMPLETED":"TASK_INACTIVE","当前状态不允许继续作答或提交");if(!Objects.equals(p.editorSessionId,session))throw conflict("EDITOR_CHANGED","另一处已继续，当前输入尚未同步");}
    private void version(Practice p,long version){if(p.version!=version)throw conflict("VERSION_CONFLICT","草稿版本已改变，请重新核对");}
    private void remember(Practice p,String kind,String key,String h,Object response){mapper.receiptPut(p.id,kind,key,h,p.editorSessionId,encode(response));}
    private Map<String,Object> prior(Practice p,String kind,String key,String h){var receipt=mapper.receipt(p.id,kind,key);if(receipt==null)return null;if(!receipt.payloadHash.equals(h))throw conflict("KEY_REUSED","同一个操作编号不能用于不同内容");return decode(receipt.responseJson);}
    private void closeAbnormal(Practice p){if(mapper.closeSegments(p.id)>0)p.timingComplete=false;p.elapsedMs=mapper.elapsed(p.id);}
    public List<Map<String,Object>> points(){var result=mapper.points();for(var p:result)p.put("id",p.get("id").toString());return result;}
    @Transactional public Map<String,Object> list(Authentication a,int page,int size){if(page<1||size<1||size>100)throw bad("INVALID_PAGE","分页参数不正确");long sid=student(a);return Map.of("items",mapper.list(sid,size,(page-1)*size).stream().map(this::taskSummary).toList(),"total",mapper.count(sid),"page",page,"size",size);}
    @Transactional public Map<String,Object> create(Authentication a,Create input){
        return taskSummary(compose(student(a),input,null));
    }
    private Practice compose(long sid,Create input,Long parent){
        var allocations=new TreeMap<Long,Integer>();
        if(input.allocations!=null){
            if(input.pointId!=null||input.total!=null)throw bad("INVALID_ALLOCATION","不能同时提供单点与多点配置");
            for(var allocation:input.allocations){if(allocation==null||allocation.total==null||allocation.total<1||allocation.total>100||allocations.put(number(allocation.pointId),allocation.total)!=null)throw bad("INVALID_ALLOCATION","每个知识点需1—100整数题且不能重复");}
        }else allocations.put(number(input.pointId),input.total==null?30:input.total);
        int total=allocations.values().stream().mapToInt(Integer::intValue).sum();if(total<1||total>100)throw bad("INVALID_TOTAL","合计需1—100题");
        // 单点沿用旧哈希，使升级前结果不明的请求仍可按原键核对。
        String h=allocations.size()==1?hash(parent==null?List.of(allocations.firstKey(),total):List.of(allocations.firstKey(),total,parent)):hash(List.of("NORMAL",allocations,parent==null?"SELF":parent));mapper.lockStudent(sid);
        String operationKey=parent==null?input.operationKey:"P"+parent+":"+input.operationKey;
        var existing=mapper.created(sid,operationKey);if(existing!=null){if(!existing.createHash.equals(h))throw conflict("KEY_REUSED","组卷操作编号已被使用");return existing;}
        int grade=mapper.studentGrade(sid);var points=new ArrayList<PointSnapshot>();
        for(var entry:allocations.entrySet()){
            var point=reviews.lockPoint(entry.getKey());if(point==null||!Boolean.TRUE.equals(point.get("enabled"))||((Number)point.get("grade")).intValue()!=grade)throw bad("GRADE_MISMATCH","请选择学生当前年级的启用知识点");
            points.add(new PointSnapshot(entry.getKey(),((Number)point.get("revision")).longValue(),grade,point.get("name").toString(),entry.getValue()));
        }
        var pool=reviews.available(new ArrayList<>(allocations.keySet()));var chosen=new ArrayList<Item>();var shortages=new ArrayList<String>();
        for(var point:points){
            var available=new ArrayList<>(pool.stream().filter(i->i.pointId==point.pointId()).toList());
            if(available.size()<point.total()){shortages.add(point.pointName()+"需要 "+point.total()+" 题，当前可用 "+available.size()+" 题");continue;}
            Collections.shuffle(available);for(var item:available.subList(0,point.total())){item.questionId=item.id;item.sourceRevision=item.revision;item.pointRevision=point.pointRevision();chosen.add(item);}
        }
        if(!shortages.isEmpty())throw conflict("INSUFFICIENT_QUESTIONS",String.join("；",shortages));Collections.shuffle(chosen);
        return persistSnapshot(new SnapshotInput(sid,parent,"NORMAL",operationKey,h,grade,points,chosen,List.of()));
    }
    public record PointSnapshot(long pointId,long pointRevision,int grade,String pointName,int total){}
    public record SnapshotInput(long studentId,Long senderId,String practiceKind,String operationKey,String requestHash,int recipientGradeAtPreview,List<PointSnapshot> pointSnapshots,List<Item> itemSnapshots,List<String> seedItemIds){}
    Practice persistSnapshot(SnapshotInput input){
        if(input.itemSnapshots.isEmpty()||input.itemSnapshots.size()>100||input.pointSnapshots.isEmpty())throw bad("INVALID_TOTAL","每份练习需1—100题");
        var first=input.pointSnapshots.getFirst();if(input.pointSnapshots.stream().anyMatch(p->p.grade!=first.grade))throw bad("MIXED_GRADES","请按年级分别创建练习");
        var p=new Practice();p.studentId=input.studentId;p.senderId=input.senderId;p.source=input.senderId==null?"SELF":"PARENT";p.status=input.senderId==null?"IN_PROGRESS":"PREVIEW";p.practiceKind=input.practiceKind;p.seedItemIds=encode(input.seedItemIds);p.recipientGradeAtPreview=input.recipientGradeAtPreview;
        p.total=input.itemSnapshots.size();p.pointId=first.pointId;p.pointRevision=first.pointRevision;p.originalGrade=first.grade;p.createKey=input.operationKey;p.createHash=input.requestHash;
        String name=String.join("、",input.pointSnapshots.stream().map(PointSnapshot::pointName).distinct().toList());p.pointName=name.length()>100?"多知识点练习（"+input.pointSnapshots.size()+"项）":name;mapper.create(p);
        for(var point:input.pointSnapshots)mapper.addPoint(p.id,point);
        int seq=0;for(var item:input.itemSnapshots){item.practiceId=p.id;item.seq=++seq;item.raw="";item.outcome=null;mapper.addItem(item);}return mapper.lock(p.id,p.studentId);
    }
    @Transactional public Map<String,Object> previewForParent(long parent,long child,Create input){var p=compose(child,input,parent);var m=new LinkedHashMap<>(taskSummary(p));m.put("items",mapper.items(p.id).stream().map(i->Map.of("id",i.id.toString(),"seq",i.seq,"stem",i.stem)).toList());return m;}
    @Transactional public Map<String,Object> readForParent(long child,long id){var p=owned(child,id);if("COMPLETED".equals(p.status))return result(p);var m=new LinkedHashMap<>(taskSummary(p));m.put("answered",mapper.items(id).stream().filter(i->!i.raw.isBlank()).count());m.put("items",mapper.items(id).stream().map(i->Map.of("id",i.id.toString(),"seq",i.seq,"stem",i.stem)).toList());return m;}
    @Transactional public Object mistakesForParent(long child){return mapper.mistakes(child).stream().map(this::resultItem).toList();}
    @Transactional public Map<String,Object> draft(Authentication a,long id){var p=owned(student(a),id);var m=new LinkedHashMap<>(taskSummary(p));m.put("items",mapper.items(id).stream().map(i->Map.of("id",i.id.toString(),"seq",i.seq,"stem",i.stem,"raw",i.raw)).toList());return m;}
    @Transactional public Map<String,Object> open(Authentication a,long id,Open input){
        var p=owned(student(a),id);if("PENDING".equals(p.status)){p.status="IN_PROGRESS";mapper.update(p);}if(!"IN_PROGRESS".equals(p.status))return taskSummary(p);String h=hash(input);var receipt=mapper.receipt(id,"OPEN",input.operationKey);
        if(receipt!=null){if(!receipt.payloadHash.equals(h))throw conflict("KEY_REUSED","打开操作内容已改变");if(!Objects.equals(receipt.editorSessionId,p.editorSessionId))throw conflict("EDITOR_CHANGED","旧打开操作已失效");return decode(receipt.responseJson);}
        if(p.editorSessionId!=null&&!input.takeover)throw conflict("EDITOR_ACTIVE","练习已在另一处打开，请明确选择在此继续");
        closeAbnormal(p);p.editorSessionId=UUID.randomUUID().toString();p.version++;mapper.update(p);var response=editor(p);remember(p,"OPEN",input.operationKey,h,response);return response;
    }
    private List<Item> apply(Practice p,Write input,boolean grade){
        var items=mapper.items(p.id);if(input.answers.size()!=p.total||input.currentSeq>p.total)throw bad("INVALID_ANSWERS","答案和题号必须对应本卷");
        var indexed=new HashMap<String,String>();for(var answer:input.answers)if(indexed.put(answer.itemId,answer.raw)!=null)throw bad("DUPLICATE_ANSWER","题目答案重复");
        for(var i:items){String raw=indexed.get(i.id.toString());if(raw==null)throw bad("INVALID_ANSWERS","答案必须与本卷题目一一对应");i.raw=raw;
            if(grade){String trimmed=raw.trim();if(!trimmed.isEmpty()&&!trimmed.matches("[0-9]+"))throw bad("INVALID_INTEGER","第 "+i.seq+" 题请输入非负整数");
                if(trimmed.isEmpty()&&!Boolean.TRUE.equals(input.confirmUnanswered))throw bad("UNANSWERED_CONFIRM_REQUIRED","存在漏答，请确认后提交");
                i.outcome=trimmed.isEmpty()?"UNANSWERED":new BigInteger(trimmed).equals(new BigInteger(i.standardAnswer))?"CORRECT":"WRONG";
            }mapper.answer(i);
        }p.currentSeq=input.currentSeq;return items;
    }
    @Transactional public Map<String,Object> save(Authentication a,long id,Write input){
        var p=owned(student(a),id);editable(p,input.editorSessionId);String h=hash(input);var previous=prior(p,"SAVE",input.operationKey,h);if(previous!=null)return previous;version(p,input.baseVersion);
        apply(p,input,false);p.version++;mapper.update(p);var response=Map.<String,Object>of("operationKey",input.operationKey,"version",p.version,"savedAt",OffsetDateTime.now(ZoneOffset.ofHours(8)).toString());remember(p,"SAVE",input.operationKey,h,response);return response;
    }
    private void segment(Practice p,SegmentInput input){
        editable(p,input.editorSessionId);if(input.cumulativeMs>mapper.ageMs(p.id)+2000)throw bad("INVALID_TIME","用时超出练习存续时间");
        var previous=mapper.segment(p.id,input.segmentId);if(previous==null){mapper.addSegment(p.id,input.editorSessionId,input.segmentId,input.cumulativeMs,input.ended);}
        else{if(!Objects.equals(previous.editorSessionId,input.editorSessionId))throw conflict("SEGMENT_OWNER","计时片段已失效");
            if(input.cumulativeMs<previous.cumulativeMs||(previous.ended&&input.cumulativeMs!=previous.cumulativeMs))throw bad("TIME_REGRESSION","计时片段不允许回退或重开");
            if(input.cumulativeMs>mapper.segmentAgeMs(p.id,input.segmentId)+2000)throw bad("INVALID_TIME","计时增长过快");
            mapper.updateSegment(p.id,input.segmentId,input.cumulativeMs,previous.ended||input.ended);
        }p.elapsedMs=mapper.elapsed(p.id);if(p.elapsedMs>mapper.ageMs(p.id)+2000)throw bad("INVALID_TIME","总用时超出练习存续时间");mapper.update(p);
    }
    @Transactional public Map<String,Object> timing(Authentication a,long id,SegmentInput input){var p=owned(student(a),id);segment(p,input);return Map.of("elapsedMs",p.elapsedMs,"cumulativeMs",input.cumulativeMs,"timingComplete",p.timingComplete);}
    @Transactional public Map<String,Object> submit(Authentication a,long id,Write input){
        var p=owned(student(a),id);String h=hash(input);
        if("COMPLETED".equals(p.status)){if(input.operationKey.equals(p.submitKey)&&!h.equals(p.submitHash))throw conflict("KEY_REUSED","提交编号对应的内容不一致");return result(p);}
        if(mapper.receipt(id,"BLOCK",input.operationKey)!=null)throw conflict("SUBMISSION_FENCED","这次提交已被核对阻断，请重新打开后提交");editable(p,input.editorSessionId);version(p,input.baseVersion);
        var items=apply(p,input,true);if(input.lastSegment!=null)segment(p,input.lastSegment);closeAbnormal(p);
        p.correctCount=(int)items.stream().filter(i->i.outcome.equals("CORRECT")).count();for(var i:items)if(!i.outcome.equals("CORRECT"))mapper.mistake(p.studentId,p.id,i.id,i.fingerprint);
        p.status="COMPLETED";p.version++;p.submitKey=input.operationKey;p.submitHash=h;p.submittedAt=LocalDateTime.now(ZoneOffset.UTC).truncatedTo(java.time.temporal.ChronoUnit.MILLIS);mapper.update(p);return result(p);
    }
    @Transactional public Map<String,Object> reconcile(Authentication a,long id,Probe input){
        var p=owned(student(a),id);if("COMPLETED".equals(p.status))return Map.of("status","COMPLETED","result",result(p),"canEdit",false);
        if(!"IN_PROGRESS".equals(p.status))return Map.of("status",p.status,"version",p.version,"canEdit",false);
        String h=hash(input);var receipt=mapper.receipt(id,"BLOCK",input.operationKey);
        if(receipt!=null){if(!receipt.payloadHash.equals(h))throw conflict("KEY_REUSED","核对请求内容不一致");var response=decode(receipt.responseJson);boolean canEdit=Boolean.TRUE.equals(response.get("canEdit"))&&Objects.equals(receipt.editorSessionId,p.editorSessionId)&&p.version==((Number)response.get("version")).longValue();response.put("canEdit",canEdit);if(!canEdit)response.remove("editorSessionId");return response;}
        boolean canEdit=Objects.equals(p.editorSessionId,input.editorSessionId)&&p.version==input.baseVersion;
        if(canEdit){closeAbnormal(p);p.editorSessionId=UUID.randomUUID().toString();p.version++;mapper.update(p);}
        var response=new LinkedHashMap<String,Object>();response.put("status","NOT_COMMITTED");response.put("canEdit",canEdit);response.put("version",p.version);if(canEdit)response.put("editorSessionId",p.editorSessionId);
        remember(p,"BLOCK",input.operationKey,h,response);return response;
    }
    private Map<String,Object> result(Practice p){
        if(!"COMPLETED".equals(p.status))throw conflict("NOT_COMPLETED","练习尚未提交");var items=mapper.items(p.id);var response=new LinkedHashMap<>(taskSummary(p));
        response.put("studentId",p.studentId.toString());response.put("correct",p.correctCount);response.put("mistakes",p.total-p.correctCount);response.put("unanswered",items.stream().filter(i->"UNANSWERED".equals(i.outcome)).count());
        response.put("accuracy",BigDecimal.valueOf(p.correctCount*100L).divide(BigDecimal.valueOf(p.total),2,RoundingMode.HALF_UP).toPlainString());
        response.put("items",items.stream().map(this::resultItem).toList());return response;
    }
    private Map<String,Object> resultItem(Item i){var m=new LinkedHashMap<String,Object>();m.put("id",i.id.toString());m.put("seq",i.seq);m.put("stem",i.stem);m.put("raw",i.raw);m.put("standardAnswer",i.standardAnswer);m.put("explanation",i.explanation);m.put("outcome",i.outcome);m.put("practiceId",i.practiceId.toString());m.put("grade",i.grade);m.put("pointId",i.pointId.toString());m.put("pointName",i.pointName);m.put("pointRevision",i.pointRevision);m.put("practiceKind",i.practiceKind);m.put("replayAllowed",!Boolean.TRUE.equals(i.isError));m.put("replayBlockReason",Boolean.TRUE.equals(i.isError)?i.errorReason:"");return m;}

    @Transactional public Map<String,Object> result(Authentication a,long id){return result(owned(student(a),id));}
    @Transactional public Map<String,Object> mistakes(Authentication a,String target,int page,int size,String pointId){
        long sid=student(a);if(sid!=number(target))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"FORBIDDEN: 无法访问其他学生的错题");if(page<1||size<1||size>100)throw bad("INVALID_PAGE","分页参数不正确");
        var items=mapper.mistakes(sid).stream().filter(i->pointId==null||i.pointId==number(pointId)).toList();
        var counts=new LinkedHashMap<String,List<Item>>();for(var i:items)counts.computeIfAbsent(i.fingerprint,k->new ArrayList<>()).add(i);
        var grouped=counts.values().stream().map(history->{var item=new LinkedHashMap<>(resultItem(history.getFirst()));item.put("count",history.size());item.put("history",history.stream().map(this::resultItem).toList());return item;}).toList();
        return Map.of("items",grouped.stream().skip((long)(page-1)*size).limit(size).toList(),"total",grouped.size(),"totalErrors",items.size(),"page",page,"size",size);
    }
    @Transactional public Map<String,Object> mistake(Authentication a,String target,long item){long sid=student(a);if(sid!=number(target))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"FORBIDDEN: 无法访问其他学生的错题");return mistakeForParent(sid,item);}
    @Transactional public Map<String,Object> mistakeForParent(long sid,long item){var selected=reviews.source(sid,item);if(selected==null)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"FORBIDDEN: 无法访问这道错题");var response=new LinkedHashMap<>(resultItem(selected));response.put("history",reviews.history(sid,selected.fingerprint).stream().map(this::resultItem).toList());response.put("count",mapper.mistakes(sid).stream().filter(i->i.fingerprint.equals(selected.fingerprint)).count());return response;}
}
