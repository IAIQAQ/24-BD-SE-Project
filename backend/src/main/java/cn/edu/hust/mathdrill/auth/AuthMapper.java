package cn.edu.hust.mathdrill.auth;
import org.apache.ibatis.annotations.*;

@Mapper
public interface AuthMapper {
    @Select("SELECT a.id,a.login_name loginName,a.password_hash passwordHash,a.role,a.status,p.nickname,p.grade FROM sys_account a LEFT JOIN student_profile p ON p.account_id=a.id WHERE a.login_name=#{name}")
    Account findByName(String name);
    @Insert("INSERT INTO sys_account(login_name,password_hash,role) VALUES(#{name},#{hash},'STUDENT')")
    void insertAccount(@Param("name") String name,@Param("hash") String hash);
    @Insert("INSERT INTO student_profile(account_id,nickname,grade) VALUES(#{id},#{nickname},1)")
    void insertProfile(@Param("id") long id,@Param("nickname") String nickname);
}
