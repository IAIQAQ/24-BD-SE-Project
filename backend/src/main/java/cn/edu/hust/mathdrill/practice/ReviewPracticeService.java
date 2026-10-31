package cn.edu.hust.mathdrill.practice;

import cn.edu.hust.mathdrill.auth.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.*;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static cn.edu.hust.mathdrill.practice.PracticeMapper.*;
import static cn.edu.hust.mathdrill.practice.PracticeService.*;

@Service public class ReviewPracticeService {
 private final ReviewMapper reviews;private final PracticeMapper mapper;private final PracticeService practices;private final AuthService auth;private final ObjectMapper json;
 public ReviewPracticeService(ReviewMapper reviews,PracticeMapper mapper,PracticeService practices,AuthService auth,ObjectMapper json){this.reviews=reviews;this.mapper=mapper;this.practices=practices;this.auth=auth;this.json=json;}
 public record Selection(@NotEmpty @Size(max=100) List<@NotBlank String> itemIds){}
 public record ReplayInput(@NotEmpty @Size(max=100) List<@NotBlank String> itemIds,@NotBlank @Size(max=64) String operationKey){}
 public record Allocation(@NotBlank String pointId,@Min(1) @Max(100) int total){}
 public record ReinforceInput(@NotEmpty @Size(max=100) List<@NotBlank String> seedItemIds,@NotEmpty @Size(max=100) List<@Valid Allocation> allocations,@NotBlank @Size(max=64) String operationKey){}
 private ResponseStatusException bad(String message){return new ResponseStatusException(HttpStatus.BAD_REQUEST,"INVALID_SELECTION: "+message);}
 private ResponseStatusException conflict(String message){return new ResponseStatusException(HttpStatus.CONFLICT,"REVIEW_CHANGED: "+message);}
 private long id(String value){try{long id=Long.parseLong(value);if(id<=0)throw new NumberFormatException();return id;}catch(Exception e){throw bad("题目编号格式不正确");}}
 private long student(Authentication a){return Long.parseLong(auth.requireRole(a,"STUDENT").id());}
 private List<Long> ids(List<String> input){if(input==null||input.isEmpty()||input.size()>100)throw bad("请选择1—100条历史错题");var ids=input.stream().map(this::id).sorted().toList();if(new HashSet<>(ids).size()!=ids.size())throw bad("请勿重复选择相同记录");return ids;}
 private List<Item> selected(long sid,List<Long> ids){var items=new ArrayList<Item>();for(long id:ids){var item=reviews.source(sid,id);if(item==null)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"FORBIDDEN: 只能选择本人的正式错题或漏答记录");items.add(item);}return items;}
 private List<Item> unique(List<Item> items){var unique=new LinkedHashMap<String,Item>();items.stream().sorted(Comparator.comparing((Item i)->i.submittedAt).thenComparing(i->i.id).reversed()).forEach(i->unique.putIfAbsent(i.grade+":"+i.fingerprint,i));return new ArrayList<>(unique.values());}
 @Transactional public Object options(Authentication a,long sid,Selection input){if(student(a)!=sid)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"FORBIDDEN: 无法查看其他学生的错题");return optionsFor(sid,input.itemIds);}
 public Object optionsFor(long sid,List<String> ids){
  var selected=selected(sid,ids(ids));var unique=unique(selected);var grades=unique.stream().map(i->i.grade).distinct().sorted().toList();String reason=selected.stream().anyMatch(i->Boolean.TRUE.equals(i.isError))?"所选历史版本已确认内容有误，不能原题重练":grades.size()>1?"请按年级分别创建原题重练":"";
  var rows=unique.stream().map(i->Map.of("id",i.id.toString(),"stem",i.stem,"grade",i.grade,"pointId",i.pointId.toString(),"pointName",i.pointName)).toList();
  var groups=new ArrayList<Map<String,Object>>();for(int grade:grades){var points=new TreeMap<Long,Item>();for(var item:selected)if(item.grade==grade)points.putIfAbsent(item.pointId,item);var choices=new ArrayList<Map<String,Object>>();for(var item:points.values()){
    var current=reviews.point(item.pointId);boolean valid=current!=null&&Boolean.TRUE.equals(current.get("enabled"))&&((Number)current.get("grade")).intValue()==grade;
    choices.add(Map.of("pointId",item.pointId.toString(),"pointName",current==null?item.pointName:current.get("name"),"available",valid?reviews.availableCount(item.pointId):0,"reason",valid?"":"知识点已停用或原年级归属已改变"));
   }groups.add(Map.of("grade",grade,"seedItemIds",selected.stream().filter(i->i.grade==grade).map(i->i.id.toString()).toList(),"points",choices));}
  return Map.of("replay",Map.of("allowed",reason.isEmpty(),"reason",reason,"total",unique.size(),"items",rows),"grades",grades,"groups",groups);
 }
 private String hash(Object value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json.writeValueAsBytes(value)));}catch(Exception e){throw new IllegalStateException(e);}}
 @Transactional public Object replay(Authentication a,ReplayInput input){
  long sid=student(a);var ids=ids(input.itemIds);String hash=hash(List.of("REPLAY",sid,ids));mapper.lockStudent(sid);
  var old=mapper.created(sid,input.operationKey);if(old!=null){if(!Objects.equals(old.createHash,hash))throw conflict("操作编号已用于其他内容");return practices.taskSummary(old);}
  var selected=selected(sid,ids);if(selected.stream().map(i->i.grade).distinct().count()!=1)throw bad("请按年级分别创建原题重练");
  for(long question:selected.stream().map(i->i.questionId).distinct().sorted().toList())reviews.lockQuestion(question);
  for(var item:selected){var error=reviews.error(item.questionId,item.sourceRevision);if(error==null||error)throw conflict("所选历史版本已确认错误或无法核对，请调整选择");}
  var items=unique(selected);var pointGroups=new LinkedHashMap<String,List<Item>>();for(var item:items)pointGroups.computeIfAbsent(item.pointId+":"+item.pointRevision,k->new ArrayList<>()).add(item);
  var points=pointGroups.values().stream().map(group->{var i=group.getFirst();return new PointSnapshot(i.pointId,i.pointRevision,i.grade,i.pointName,group.size());}).sorted(Comparator.comparingLong(PointSnapshot::pointId).thenComparingLong(PointSnapshot::pointRevision)).toList();
  for(var item:items)item.originItemId=item.id;
  var p=practices.persistSnapshot(new SnapshotInput(sid,null,"REPLAY",input.operationKey,hash,mapper.studentGrade(sid),points,items,ids.stream().map(Object::toString).toList()));return practices.taskSummary(p);
 }
 @Transactional public Object reinforce(Authentication a,ReinforceInput input){return practices.taskSummary(createReinforcement(student(a),null,input));}
 @Transactional public Object parentPreview(long parent,long student,ReinforceInput input){var p=createReinforcement(student,parent,input);var response=new LinkedHashMap<>(practices.taskSummary(p));response.put("items",mapper.items(p.id).stream().map(i->Map.of("id",i.id.toString(),"seq",i.seq,"stem",i.stem)).toList());return response;}
 private Practice createReinforcement(long sid,Long parent,ReinforceInput input){
  var ids=ids(input.seedItemIds);var allocations=new TreeMap<Long,Integer>();for(var allocation:input.allocations){if(allocation.total<1||allocation.total>100||allocations.put(id(allocation.pointId),allocation.total)!=null)throw bad("每个知识点需1—100题且不能重复分配");}
  int total=allocations.values().stream().mapToInt(Integer::intValue).sum();if(total<1||total>100)throw bad("每份强化总题量需1—100题");String key=parent==null?input.operationKey:"P"+parent+":"+input.operationKey;String hash=hash(List.of("REINFORCE",sid,parent==null?"SELF":parent,ids,allocations));mapper.lockStudent(sid);
  var old=mapper.created(sid,key);if(old!=null){if(!Objects.equals(old.createHash,hash))throw conflict("操作编号已用于其他内容");return old;}
  var sources=selected(sid,ids);if(sources.stream().map(i->i.grade).distinct().count()!=1)throw bad("请按历史年级分别配置强化");int grade=sources.getFirst().grade;
  var required=new TreeSet<Long>();for(var item:sources)required.add(item.pointId);if(!allocations.keySet().equals(required))throw bad("题量分配必须对应本次所选错题的全部知识点");
  int recipientGrade=mapper.studentGrade(sid);var pointSnapshots=new ArrayList<PointSnapshot>();for(var entry:allocations.entrySet()){
   var point=reviews.lockPoint(entry.getKey());if(point==null||!Boolean.TRUE.equals(point.get("enabled"))||((Number)point.get("grade")).intValue()!=grade)throw conflict("知识点已停用或原年级归属已改变，请重新选择");
   pointSnapshots.add(new PointSnapshot(entry.getKey(),((Number)point.get("revision")).longValue(),grade,point.get("name").toString(),entry.getValue()));
  }
  var pool=reviews.available(new ArrayList<>(required));var items=new ArrayList<Item>();for(var point:pointSnapshots){var available=new ArrayList<>(pool.stream().filter(i->i.pointId==point.pointId()).toList());if(available.size()<point.total())throw conflict(point.pointName()+"需要 "+point.total()+" 题，当前可用 "+available.size()+" 题，请调整题量");Collections.shuffle(available);
   for(var item:available.subList(0,point.total())){item.questionId=item.id;item.sourceRevision=item.revision;item.pointRevision=point.pointRevision();items.add(item);}
  }Collections.shuffle(items);
  return practices.persistSnapshot(new SnapshotInput(sid,parent,"REINFORCE",key,hash,recipientGrade,pointSnapshots,items,ids.stream().map(Object::toString).toList()));
 }
}
