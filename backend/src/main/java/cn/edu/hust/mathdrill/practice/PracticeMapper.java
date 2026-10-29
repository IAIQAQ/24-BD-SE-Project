package cn.edu.hust.mathdrill.practice;

import java.time.LocalDateTime;
import java.util.*;
import org.apache.ibatis.annotations.*;

@Mapper
public interface PracticeMapper {
    class Practice {
        public Long id,studentId,pointId,version,elapsedMs,senderId,pointRevision;
        public Integer total,currentSeq,correctCount,originalGrade,recipientGradeAtPreview;
        public String practiceKind,seedItemIds;
        public String status,editorSessionId,createKey,createHash,submitKey,submitHash,source,pointName,errorNote;
        public Boolean timingComplete,contentError;
        public LocalDateTime createdAt,submittedAt;
    }
    class Item {
        public Long id,practiceId,questionId,pointId,revision,sourceRevision,pointRevision,originItemId;
        public Integer seq,grade;
        public String stem,standardAnswer,explanation,fingerprint,raw,outcome,pointName,practiceKind,errorReason;
        public Boolean isError;
        public LocalDateTime submittedAt;
    }
    String ITEM_VIEW="SELECT i.id,i.practice_id,i.seq,i.question_id,i.stem,i.standard_answer,i.explanation,i.fingerprint,i.raw,i.outcome,i.source_revision,i.origin_item_id,COALESCE(i.point_id,p.point_id) point_id,COALESCE(i.point_revision,p.point_revision) point_revision,p.original_grade grade,COALESCE(pp.point_name,p.point_name) point_name,p.practice_kind,p.submitted_at,COALESCE(qr.is_error,FALSE) is_error,COALESCE(qr.error_note,'') error_reason FROM practice_item i JOIN practice p ON p.id=i.practice_id LEFT JOIN practice_point pp ON pp.practice_id=i.practice_id AND pp.point_id=i.point_id AND pp.point_revision=i.point_revision LEFT JOIN question_revision qr ON qr.question_id=i.question_id AND qr.revision=i.source_revision ";
    class Receipt { public String payloadHash,editorSessionId,responseJson; }
    class Segment { public Long cumulativeMs;public String editorSessionId;public Boolean ended;public LocalDateTime startedAt; }
    @Select("SELECT id FROM sys_account WHERE id=#{id} FOR UPDATE") Long lockStudent(long id);
    @Select("SELECT * FROM practice WHERE id=#{id} AND student_id=#{student} FOR UPDATE") Practice lock(@Param("id")long id,@Param("student")long student);
    @Select("SELECT * FROM practice WHERE id=#{id}") Practice byId(long id);
    @Select("SELECT grade FROM knowledge_point WHERE id=#{id} AND enabled=TRUE FOR UPDATE") Integer pointGrade(long id);
    @Select("SELECT name FROM knowledge_point WHERE id=#{id} FOR UPDATE") String pointName(long id);
    @Select("SELECT revision FROM knowledge_point WHERE id=#{id} FOR UPDATE") Long pointRevision(long id);
    @Select("SELECT grade FROM student_profile WHERE account_id=#{id} FOR UPDATE") Integer studentGrade(long id);
    @Select("SELECT * FROM practice WHERE student_id=#{student} AND create_key=#{key} FOR UPDATE") Practice created(@Param("student")long student,@Param("key")String key);
    @Select("SELECT * FROM practice WHERE student_id=#{student} AND status NOT IN ('PREVIEW','EXPIRED') ORDER BY id DESC LIMIT #{limit} OFFSET #{offset}") List<Practice> list(@Param("student")long student,@Param("limit")int limit,@Param("offset")int offset);
    @Select("SELECT COUNT(*) FROM practice WHERE student_id=#{student} AND status NOT IN ('PREVIEW','EXPIRED')") long count(long student);
    @Select("SELECT k.id,k.name,k.grade,k.semester,COUNT(q.id) available FROM knowledge_point k LEFT JOIN question q ON q.point_id=k.id AND q.enabled=TRUE AND q.reviewed=TRUE WHERE k.enabled=TRUE GROUP BY k.id ORDER BY k.id") List<Map<String,Object>> points();
    @Select("SELECT q.* FROM question q JOIN knowledge_point k ON k.id=q.point_id WHERE q.point_id=#{point} AND q.enabled=TRUE AND q.reviewed=TRUE AND k.enabled=TRUE ORDER BY q.id FOR UPDATE") List<Item> available(long point);
    @Insert("INSERT INTO practice(student_id,point_id,total,create_key,create_hash,status,source,sender_id,original_grade,point_name,point_revision,practice_kind,seed_item_ids,recipient_grade_at_preview) VALUES(#{studentId},#{pointId},#{total},#{createKey},#{createHash},#{status},#{source},#{senderId},#{originalGrade},#{pointName},#{pointRevision},#{practiceKind},#{seedItemIds},#{recipientGradeAtPreview})") @Options(useGeneratedKeys=true,keyProperty="id") void create(Practice p);
    @Insert("INSERT INTO practice_item(practice_id,seq,question_id,stem,standard_answer,explanation,fingerprint,source_revision,point_id,point_revision,origin_item_id) VALUES(#{practiceId},#{seq},#{questionId},#{stem},#{standardAnswer},#{explanation},#{fingerprint},#{sourceRevision},#{pointId},#{pointRevision},#{originItemId})") void addItem(Item item);
    @Insert("INSERT INTO practice_point(practice_id,point_id,point_revision,original_grade,point_name,total) VALUES(#{practice},#{point.pointId},#{point.pointRevision},#{point.grade},#{point.pointName},#{point.total})") void addPoint(@Param("practice")long practice,@Param("point")PracticeService.PointSnapshot point);
    @Select("SELECT point_id pointId,point_revision pointRevision,original_grade grade,point_name pointName,total FROM practice_point WHERE practice_id=#{id} ORDER BY point_id,point_revision") List<Map<String,Object>> practicePoints(long id);
    @Select("SELECT nickname FROM parent_profile WHERE account_id=#{id}") String senderName(long id);
    @Select(ITEM_VIEW+"WHERE i.practice_id=#{id} ORDER BY i.seq") List<Item> items(long id);
    @Update("UPDATE practice SET status=#{status},version=#{version},editor_session_id=#{editorSessionId},current_seq=#{currentSeq},submit_key=#{submitKey},submit_hash=#{submitHash},correct_count=#{correctCount},elapsed_ms=#{elapsedMs},timing_complete=#{timingComplete},submitted_at=#{submittedAt} WHERE id=#{id}") void update(Practice p);
    @Update("UPDATE practice_item SET raw=#{raw},outcome=#{outcome} WHERE id=#{id}") void answer(Item i);
    @Select("SELECT * FROM practice_receipt WHERE practice_id=#{id} AND kind=#{kind} AND operation_key=#{key}") Receipt receipt(@Param("id")long id,@Param("kind")String kind,@Param("key")String key);
    @Insert("INSERT INTO practice_receipt(practice_id,kind,operation_key,payload_hash,editor_session_id,response_json) VALUES(#{id},#{kind},#{key},#{hash},#{editor},#{response})") void receiptPut(@Param("id")long id,@Param("kind")String kind,@Param("key")String key,@Param("hash")String hash,@Param("editor")String editor,@Param("response")String response);
    @Select("SELECT * FROM practice_segment WHERE practice_id=#{id} AND segment_id=#{segment}") Segment segment(@Param("id")long id,@Param("segment")String segment);
    @Insert("INSERT INTO practice_segment(practice_id,editor_session_id,segment_id,cumulative_ms,ended,started_at) VALUES(#{id},#{editor},#{segment},#{ms},#{ended},TIMESTAMPADD(MICROSECOND,-#{ms}*1000,CURRENT_TIMESTAMP(3)))") void addSegment(@Param("id")long id,@Param("editor")String editor,@Param("segment")String segment,@Param("ms")long ms,@Param("ended")boolean ended);
    @Select("SELECT GREATEST(0,TIMESTAMPDIFF(MICROSECOND,created_at,CURRENT_TIMESTAMP(3)) DIV 1000) FROM practice WHERE id=#{id}") long ageMs(long id);
    @Select("SELECT GREATEST(0,TIMESTAMPDIFF(MICROSECOND,started_at,CURRENT_TIMESTAMP(3)) DIV 1000) FROM practice_segment WHERE practice_id=#{id} AND segment_id=#{segment}") long segmentAgeMs(@Param("id")long id,@Param("segment")String segment);
    @Update("UPDATE practice_segment SET cumulative_ms=#{ms},ended=#{ended} WHERE practice_id=#{id} AND segment_id=#{segment}") void updateSegment(@Param("id")long id,@Param("segment")String segment,@Param("ms")long ms,@Param("ended")boolean ended);
    @Update("UPDATE practice_segment SET ended=TRUE WHERE practice_id=#{id} AND ended=FALSE") int closeSegments(long id);
    @Select("SELECT COALESCE(SUM(cumulative_ms),0) FROM practice_segment WHERE practice_id=#{id}") long elapsed(long id);
    @Insert("INSERT INTO mistake_record(student_id,practice_id,item_id,fingerprint) VALUES(#{student},#{practice},#{item},#{fingerprint})") void mistake(@Param("student")long student,@Param("practice")long practice,@Param("item")long item,@Param("fingerprint")String fingerprint);
    @Select(ITEM_VIEW+"JOIN mistake_record m ON m.item_id=i.id WHERE m.student_id=#{student} ORDER BY m.id DESC") List<Item> mistakes(long student);
}
