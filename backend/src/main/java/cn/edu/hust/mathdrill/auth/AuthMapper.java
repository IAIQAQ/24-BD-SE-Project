package cn.edu.hust.mathdrill.auth;
import org.apache.ibatis.annotations.*;

@Mapper
public interface AuthMapper {
    @Select("SELECT a.id,a.login_name loginName,a.password_hash passwordHash,a.role,a.status,COALESCE(p.nickname,pp.nickname,IF(a.role='ADMIN','管理员',NULL)) nickname,p.grade FROM sys_account a LEFT JOIN student_profile p ON p.account_id=a.id LEFT JOIN parent_profile pp ON pp.account_id=a.id WHERE a.login_name=#{name}")
    Account findByName(String name);
    @Insert("INSERT INTO sys_account(login_name,password_hash,role) VALUES(#{name},#{hash},'STUDENT')")
    void insertAccount(@Param("name") String name,@Param("hash") String hash);
    @Insert("INSERT INTO student_profile(account_id,nickname,grade) VALUES(#{id},#{nickname},1)")
    void insertProfile(@Param("id") long id,@Param("nickname") String nickname);
    @Insert("INSERT INTO sys_account(login_name,password_hash,role) VALUES(#{name},#{hash},#{role})") void register(@Param("name")String name,@Param("hash")String hash,@Param("role")String role);
    @Insert("INSERT INTO parent_profile(account_id,nickname) VALUES(#{id},#{nickname})") void parent(@Param("id")long id,@Param("nickname")String nickname);
    @Update("UPDATE student_profile SET nickname=#{nickname},grade=#{grade} WHERE account_id=#{id}") void student(@Param("id")long id,@Param("nickname")String nickname,@Param("grade")int grade);
    @Select("SELECT id FROM sys_account WHERE id=#{id} FOR UPDATE") Long lockAccount(long id);
}
