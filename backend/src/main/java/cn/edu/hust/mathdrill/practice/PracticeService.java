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
    private final PracticeMapper mapper;private final AuthService auth;private final ObjectMapper json;
    public PracticeService(PracticeMapper mapper,AuthService auth,ObjectMapper json){this.mapper=mapper;this.auth=auth;this.json=json;}
    public record Create(@NotBlank String pointId,@Min(1) @Max(100) Integer total,@NotBlank @Size(max=64) String operationKey){}
    public record Open(@NotBlank @Size(max=64) String operationKey,boolean takeover){}
    public record Answer(@NotBlank String itemId,@NotNull @Size(max=128) String raw){}
    public record Write(@NotBlank @Size(max=36) String editorSessionId,@Min(0) long baseVersion,@NotBlank @Size(max=64) String operationKey,@NotNull @Size(min=1,max=100) List<@Valid Answer> answers,@Min(1) int currentSeq,Boolean confirmUnanswered,@Valid SegmentInput lastSegment){}
    public record Probe(@NotBlank @Size(max=64) String operationKey,@NotBlank @Size(max=36) String editorSessionId,@Min(0) long baseVersion){}
    public record SegmentInput(@NotBlank @Size(max=36) String editorSessionId,@NotBlank @Size(max=36) String segmentId,@Min(0) long cumulativeMs,boolean ended){}
    private long student(Authentication a){return Long.parseLong(auth.current(a).id());}
    private long number(String s){try{long n=Long.parseLong(s);if(n<=0)throw new NumberFormatException();return n;}catch(Exception e){throw bad("INVALID_ID","编号格式不正确");}}
    private static ResponseStatusException bad(String code,String message){return new ResponseStatusException(HttpStatus.BAD_REQUEST,code+": "+message);}
    private static ResponseStatusException conflict(String code,String message){return new ResponseStatusException(HttpStatus.CONFLICT,code+": "+message);}
    private Practice owned(long student,long id){var p=mapper.lock(id,student);if(p==null)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"FORBIDDEN: 无法访问这份练习");return p;}
    private String encode(Object o){try{return json.writeValueAsString(o);}catch(Exception e){throw new IllegalStateException(e);}}
    private String hash(Object o){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(encode(o).getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
    private Map<String,Object> decode(String s){try{return json.readValue(s,new TypeReference<>(){});}catch(Exception e){throw new IllegalStateException(e);}}
    private String timestamp(LocalDateTime t){return t==null?"":t.atOffset(ZoneOffset.UTC).atZoneSameInstant(ZoneId.of("Asia/Shanghai")).toOffsetDateTime().toString();}
    private Map<String,Object> summary(Practice p){return Map.of("id",p.id.toString(),"pointId",p.pointId.toString(),"total",p.total,"status",p.status,"version",p.version,"currentSeq",p.currentSeq,"elapsedMs",p.elapsedMs,"timingComplete",p.timingComplete,"submittedAt",timestamp(p.submittedAt));}
    private Map<String,Object> editor(Practice p){var m=new LinkedHashMap<>(summary(p));m.put("editorSessionId",p.editorSessionId==null?"":p.editorSessionId);return m;}
    private void editable(Practice p,String session){if(!"IN_PROGRESS".equals(p.status))throw conflict("COMPLETED","练习已完成，请查看结果");if(!Objects.equals(p.editorSessionId,session))throw conflict("EDITOR_CHANGED","另一处已继续，当前输入尚未同步");}
    private void version(Practice p,long version){if(p.version!=version)throw conflict("VERSION_CONFLICT","草稿版本已改变，请重新核对");}
    private void remember(Practice p,String kind,String key,String h,Object response){mapper.receiptPut(p.id,kind,key,h,p.editorSessionId,encode(response));}
    private Map<String,Object> prior(Practice p,String kind,String key,String h){var receipt=mapper.receipt(p.id,kind,key);if(receipt==null)return null;if(!receipt.payloadHash.equals(h))throw conflict("KEY_REUSED","同一个操作编号不能用于不同内容");return decode(receipt.responseJson);}
    private void closeAbnormal(Practice p){if(mapper.closeSegments(p.id)>0)p.timingComplete=false;p.elapsedMs=mapper.elapsed(p.id);}
    public List<Map<String,Object>> points(){var result=mapper.points();for(var p:result)p.put("id",p.get("id").toString());return result;}
    @Transactional public Map<String,Object> list(Authentication a,int page,int size){if(page<1||size<1||size>100)throw bad("INVALID_PAGE","分页参数不正确");long sid=student(a);return Map.of("items",mapper.list(sid,size,(page-1)*size).stream().map(this::summary).toList(),"total",mapper.count(sid),"page",page,"size",size);}
    @Transactional public Map<String,Object> create(Authentication a,Create input){
        long sid=student(a);int total=input.total==null?30:input.total;long point=number(input.pointId);String h=hash(List.of(point,total));mapper.lockStudent(sid);
        var existing=mapper.created(sid,input.operationKey);if(existing!=null){if(!existing.createHash.equals(h))throw conflict("KEY_REUSED","组卷操作编号已被使用");return summary(existing);}
        var available=new ArrayList<>(mapper.available(point));if(available.size()<total)throw conflict("INSUFFICIENT_QUESTIONS","需要 "+total+" 题，当前可用 "+available.size()+" 题");Collections.shuffle(available);
        var p=new Practice();p.studentId=sid;p.pointId=point;p.total=total;p.createKey=input.operationKey;p.createHash=h;mapper.create(p);
        int seq=0;for(var source:available.subList(0,total)){source.questionId=source.id;source.practiceId=p.id;source.seq=++seq;mapper.addItem(source);}return summary(owned(sid,p.id));
    }
    @Transactional public Map<String,Object> draft(Authentication a,long id){var p=owned(student(a),id);var m=new LinkedHashMap<>(summary(p));m.put("items",mapper.items(id).stream().map(i->Map.of("id",i.id.toString(),"seq",i.seq,"stem",i.stem,"raw",i.raw)).toList());return m;}
    @Transactional public Map<String,Object> open(Authentication a,long id,Open input){
        var p=owned(student(a),id);if(!"IN_PROGRESS".equals(p.status))return summary(p);String h=hash(input);var receipt=mapper.receipt(id,"OPEN",input.operationKey);
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
        String h=hash(input);var receipt=mapper.receipt(id,"BLOCK",input.operationKey);
        if(receipt!=null){if(!receipt.payloadHash.equals(h))throw conflict("KEY_REUSED","核对请求内容不一致");var response=decode(receipt.responseJson);boolean canEdit=Boolean.TRUE.equals(response.get("canEdit"))&&Objects.equals(receipt.editorSessionId,p.editorSessionId)&&p.version==((Number)response.get("version")).longValue();response.put("canEdit",canEdit);if(!canEdit)response.remove("editorSessionId");return response;}
        boolean canEdit=Objects.equals(p.editorSessionId,input.editorSessionId)&&p.version==input.baseVersion;
        if(canEdit){closeAbnormal(p);p.editorSessionId=UUID.randomUUID().toString();p.version++;mapper.update(p);}
        var response=new LinkedHashMap<String,Object>();response.put("status","NOT_COMMITTED");response.put("canEdit",canEdit);response.put("version",p.version);if(canEdit)response.put("editorSessionId",p.editorSessionId);
        remember(p,"BLOCK",input.operationKey,h,response);return response;
    }
    private Map<String,Object> result(Practice p){
        if(!"COMPLETED".equals(p.status))throw conflict("NOT_COMPLETED","练习尚未提交");var items=mapper.items(p.id);var response=new LinkedHashMap<>(summary(p));
        response.put("studentId",p.studentId.toString());response.put("correct",p.correctCount);response.put("mistakes",p.total-p.correctCount);response.put("unanswered",items.stream().filter(i->"UNANSWERED".equals(i.outcome)).count());
        response.put("accuracy",BigDecimal.valueOf(p.correctCount*100L).divide(BigDecimal.valueOf(p.total),2,RoundingMode.HALF_UP).toPlainString());
        response.put("items",items.stream().map(this::resultItem).toList());return response;
    }
    private Map<String,Object> resultItem(Item i){return Map.of("id",i.id.toString(),"seq",i.seq,"stem",i.stem,"raw",i.raw,"standardAnswer",i.standardAnswer,"explanation",i.explanation,"outcome",i.outcome,"practiceId",i.practiceId.toString());}
    @Transactional public Map<String,Object> result(Authentication a,long id){return result(owned(student(a),id));}
    @Transactional public Map<String,Object> mistakes(Authentication a,String target,int page,int size,String pointId){
        long sid=student(a);if(sid!=number(target))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"FORBIDDEN: 无法访问其他学生的错题");if(page<1||size<1||size>100)throw bad("INVALID_PAGE","分页参数不正确");
        var items=mapper.mistakes(sid).stream().filter(i->pointId==null||i.pointId==number(pointId)).toList();
        var counts=new LinkedHashMap<String,List<Item>>();for(var i:items)counts.computeIfAbsent(i.fingerprint,k->new ArrayList<>()).add(i);
        var grouped=counts.values().stream().map(history->{var item=new LinkedHashMap<>(resultItem(history.getFirst()));item.put("count",history.size());item.put("history",history.stream().map(this::resultItem).toList());return item;}).toList();
        return Map.of("items",grouped.stream().skip((long)(page-1)*size).limit(size).toList(),"total",grouped.size(),"totalErrors",items.size(),"page",page,"size",size);
    }
    @Transactional public Map<String,Object> mistake(Authentication a,String target,long item){
        long sid=student(a);if(sid!=number(target))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"FORBIDDEN: 无法访问其他学生的错题");var items=mapper.mistakes(sid);var selected=items.stream().filter(i->i.id==item).findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.FORBIDDEN,"FORBIDDEN: 无法访问这道错题"));var response=new LinkedHashMap<>(resultItem(selected));response.put("history",items.stream().filter(i->i.fingerprint.equals(selected.fingerprint)).map(this::resultItem).toList());return response;
    }
}
