package cn.edu.hust.mathdrill.practice;
import java.util.*;
import org.apache.ibatis.annotations.*;
import cn.edu.hust.mathdrill.practice.PracticeMapper.Item;

@Mapper public interface ReviewMapper {
 @Select(PracticeMapper.ITEM_VIEW+"JOIN mistake_record m ON m.item_id=i.id WHERE m.student_id=#{student} AND p.student_id=#{student} AND p.status='COMPLETED' AND i.id=#{id}") Item source(@Param("student")long student,@Param("id")long id);
 @Select(PracticeMapper.ITEM_VIEW+"WHERE p.student_id=#{student} AND p.status='COMPLETED' AND i.fingerprint=#{fingerprint} ORDER BY p.submitted_at DESC,i.id DESC") List<Item> history(@Param("student")long student,@Param("fingerprint")String fingerprint);
 @Select("SELECT id FROM question WHERE id=#{id} FOR UPDATE") Long lockQuestion(long id);
 @Select("SELECT is_error FROM question_revision WHERE question_id=#{id} AND revision=#{revision} FOR UPDATE") Boolean error(@Param("id")long id,@Param("revision")long revision);
 @Select("SELECT * FROM knowledge_point WHERE id=#{id}") Map<String,Object> point(long id);
 @Select("SELECT * FROM knowledge_point WHERE id=#{id} FOR UPDATE") Map<String,Object> lockPoint(long id);
 @Select("SELECT COUNT(*) FROM question q JOIN knowledge_point k ON k.id=q.point_id WHERE q.point_id=#{id} AND q.enabled=TRUE AND q.reviewed=TRUE AND k.enabled=TRUE") int availableCount(long id);
 @Select({"<script>SELECT q.* FROM question q JOIN knowledge_point k ON k.id=q.point_id WHERE q.enabled=TRUE AND q.reviewed=TRUE AND k.enabled=TRUE AND q.point_id IN <foreach item='id' collection='points' open='(' separator=',' close=')'>#{id}</foreach> ORDER BY q.id FOR UPDATE</script>"}) List<Item> available(@Param("points")List<Long> points);
}
