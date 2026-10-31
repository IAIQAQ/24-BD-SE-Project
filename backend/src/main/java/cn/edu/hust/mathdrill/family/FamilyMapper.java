package cn.edu.hust.mathdrill.family;
import java.util.*;
import org.apache.ibatis.annotations.*;
@Mapper public interface FamilyMapper {
 class Link{public Long id,parentId,studentId;public String state;}
 @Select("SELECT a.id FROM sys_account a JOIN student_profile s ON s.account_id=a.id WHERE a.login_name=#{name} AND a.role='STUDENT' AND a.status='ACTIVE'") Long student(String name);
 @Select("SELECT * FROM family_link WHERE parent_id=#{parent} AND student_id=#{student} FOR UPDATE") Link pair(@Param("parent")long parent,@Param("student")long student);
 @Select("SELECT * FROM family_link WHERE id=#{id} FOR UPDATE") Link lock(long id);
 @Insert("INSERT INTO family_link(parent_id,student_id) VALUES(#{parentId},#{studentId})") @Options(useGeneratedKeys=true,keyProperty="id") void add(Link link);
 @Update("UPDATE family_link SET state=#{state} WHERE id=#{id}") void update(Link link);
 @Select("SELECT l.id,l.parent_id parentId,l.student_id studentId,l.state,a.login_name loginName,s.nickname,s.grade FROM family_link l JOIN sys_account a ON a.id=l.student_id JOIN student_profile s ON s.account_id=l.student_id WHERE l.parent_id=#{id} ORDER BY l.id DESC") List<Map<String,Object>> children(long id);
 @Select("SELECT l.id,l.parent_id parentId,l.student_id studentId,l.state,a.login_name loginName,p.nickname FROM family_link l JOIN sys_account a ON a.id=l.parent_id JOIN parent_profile p ON p.account_id=l.parent_id WHERE l.student_id=#{id} ORDER BY l.id DESC") List<Map<String,Object>> parents(long id);
 @Select("SELECT grade FROM student_profile WHERE account_id=#{id} FOR UPDATE") Integer grade(long id);
 @Select("SELECT * FROM question WHERE id=#{id} FOR UPDATE") Map<String,Object> question(long id);
 @Select("SELECT * FROM knowledge_point WHERE id=#{id} FOR UPDATE") Map<String,Object> point(long id);
}
