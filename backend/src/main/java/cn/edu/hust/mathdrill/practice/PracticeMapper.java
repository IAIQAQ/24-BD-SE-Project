package cn.edu.hust.mathdrill.practice;

import java.time.LocalDateTime;
import java.util.*;
import org.apache.ibatis.annotations.*;

@Mapper
public interface PracticeMapper {
    class Practice {
        public Long id,studentId,pointId,version,elapsedMs;
        public Integer total,currentSeq,correctCount;
        public String status,editorSessionId,createKey,createHash,submitKey,submitHash;
        public Boolean timingComplete;
        public LocalDateTime createdAt,submittedAt;
    }
    class Item {
        public Long id,practiceId,questionId,pointId;
        public Integer seq;
        public String stem,standardAnswer,explanation,fingerprint,raw,outcome;
    }
    class Receipt { public String payloadHash,editorSessionId,responseJson; }
    class Segment { public Long cumulativeMs;public String editorSessionId;public Boolean ended;public LocalDateTime startedAt; }
    @Select("SELECT id FROM sys_account WHERE id=#{id} FOR UPDATE") Long lockStudent(long id);
    @Select("SELECT * FROM practice WHERE id=#{id} AND student_id=#{student} FOR UPDATE") Practice lock(@Param("id")long id,@Param("student")long student);
    @Select("SELECT * FROM practice WHERE student_id=#{student} AND create_key=#{key}") Practice created(@Param("student")long student,@Param("key")String key);
    @Select("SELECT * FROM practice WHERE student_id=#{student} ORDER BY id DESC LIMIT #{limit} OFFSET #{offset}") List<Practice> list(@Param("student")long student,@Param("limit")int limit,@Param("offset")int offset);
    @Select("SELECT COUNT(*) FROM practice WHERE student_id=#{student}") long count(long student);
    @Select("SELECT k.id,k.name,k.grade,k.semester,COUNT(q.id) available FROM knowledge_point k LEFT JOIN question q ON q.point_id=k.id AND q.enabled=TRUE AND q.reviewed=TRUE WHERE k.enabled=TRUE GROUP BY k.id ORDER BY k.id") List<Map<String,Object>> points();
    @Select("SELECT q.* FROM question q JOIN knowledge_point k ON k.id=q.point_id WHERE q.point_id=#{point} AND q.enabled=TRUE AND q.reviewed=TRUE AND k.enabled=TRUE ORDER BY q.id") List<Item> available(long point);
    @Insert("INSERT INTO practice(student_id,point_id,total,create_key,create_hash) VALUES(#{studentId},#{pointId},#{total},#{createKey},#{createHash})") @Options(useGeneratedKeys=true,keyProperty="id") void create(Practice p);
    @Insert("INSERT INTO practice_item(practice_id,seq,question_id,stem,standard_answer,explanation,fingerprint) VALUES(#{practiceId},#{seq},#{questionId},#{stem},#{standardAnswer},#{explanation},#{fingerprint})") void addItem(Item item);
    @Select("SELECT * FROM practice_item WHERE practice_id=#{id} ORDER BY seq") List<Item> items(long id);
    @Update("UPDATE practice SET status=#{status},version=#{version},editor_session_id=#{editorSessionId},current_seq=#{currentSeq},submit_key=#{submitKey},submit_hash=#{submitHash},correct_count=#{correctCount},elapsed_ms=#{elapsedMs},timing_complete=#{timingComplete},submitted_at=#{submittedAt} WHERE id=#{id}") void update(Practice p);
    @Update("UPDATE practice_item SET raw=#{raw},outcome=#{outcome} WHERE id=#{id}") void answer(Item i);
    @Select("SELECT * FROM practice_receipt WHERE practice_id=#{id} AND kind=#{kind} AND operation_key=#{key}") Receipt receipt(@Param("id")long id,@Param("kind")String kind,@Param("key")String key);
    @Insert("INSERT INTO practice_receipt(practice_id,kind,operation_key,payload_hash,editor_session_id,response_json) VALUES(#{id},#{kind},#{key},#{hash},#{editor},#{response})") void receiptPut(@Param("id")long id,@Param("kind")String kind,@Param("key")String key,@Param("hash")String hash,@Param("editor")String editor,@Param("response")String response);
    @Select("SELECT * FROM practice_segment WHERE practice_id=#{id} AND segment_id=#{segment}") Segment segment(@Param("id")long id,@Param("segment")String segment);
    @Insert("INSERT INTO practice_segment(practice_id,editor_session_id,segment_id,cumulative_ms,ended) VALUES(#{id},#{editor},#{segment},#{ms},#{ended})") void addSegment(@Param("id")long id,@Param("editor")String editor,@Param("segment")String segment,@Param("ms")long ms,@Param("ended")boolean ended);
    @Update("UPDATE practice_segment SET cumulative_ms=#{ms},ended=#{ended} WHERE practice_id=#{id} AND segment_id=#{segment}") void updateSegment(@Param("id")long id,@Param("segment")String segment,@Param("ms")long ms,@Param("ended")boolean ended);
    @Update("UPDATE practice_segment SET ended=TRUE WHERE practice_id=#{id} AND ended=FALSE") int closeSegments(long id);
    @Select("SELECT COALESCE(SUM(cumulative_ms),0) FROM practice_segment WHERE practice_id=#{id}") long elapsed(long id);
    @Insert("INSERT INTO mistake_record(student_id,practice_id,item_id,fingerprint) VALUES(#{student},#{practice},#{item},#{fingerprint})") void mistake(@Param("student")long student,@Param("practice")long practice,@Param("item")long item,@Param("fingerprint")String fingerprint);
    @Select("SELECT i.*,p.point_id FROM mistake_record m JOIN practice_item i ON i.id=m.item_id JOIN practice p ON p.id=m.practice_id WHERE m.student_id=#{student} ORDER BY m.id DESC") List<Item> mistakes(long student);
}
