package cn.edu.hust.mathdrill.auth;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService implements UserDetailsService {
    private final AuthMapper mapper;
    public AuthService(AuthMapper mapper){this.mapper=mapper;}
    @Override public UserDetails loadUserByUsername(String name){
        Account a=mapper.findByName(name);
        if(a==null)throw new UsernameNotFoundException("Invalid credentials");
        return User.withUsername(a.loginName()).password(a.passwordHash()).roles(a.role()).disabled(!"ACTIVE".equals(a.status())).build();
    }
    public record Profile(String id,String loginName,String nickname,String role,Integer grade){}
    public Profile requireRole(Authentication a,String role){var profile=current(a);if(!role.equals(profile.role()))throw new org.springframework.security.access.AccessDeniedException("Role required");return profile;}
    @Transactional public Profile register(String name,String password,String nickname,String role,Integer grade,PasswordEncoder encoder){
        if(!java.util.Set.of("STUDENT","PARENT").contains(role)||!name.matches("[A-Za-z0-9_-]{3,64}")||password.length()<8||password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72||nickname.isBlank()||nickname.length()>64||("STUDENT".equals(role)&&(grade==null||grade<1||grade>3)))throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"INVALID_INPUT: 账号需3—64位字母数字、下划线或短横线；密码至少8位且不超过72字节；请检查昵称和年级");
        if(name.equalsIgnoreCase("admin"))throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"RESERVED_NAME: 此登录名保留给系统管理账号");
        if(mapper.findByName(name)!=null)throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,"LOGIN_NAME_EXISTS: 账号已存在");
        mapper.register(name,encoder.encode(password),role);var account=mapper.findByName(name);
        if("STUDENT".equals(role)){mapper.insertProfile(account.id(),nickname);mapper.student(account.id(),nickname,grade);}else mapper.parent(account.id(),nickname);
        return new Profile(Long.toString(account.id()),name,nickname,role,"STUDENT".equals(role)?grade:null);
    }
    @Transactional public Profile updateProfile(Authentication a,String nickname,int grade){var p=requireRole(a,"STUDENT");if(nickname.isBlank()||nickname.length()>64||grade<1||grade>3)throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"INVALID_INPUT: 昵称或年级不正确");mapper.lockAccount(Long.parseLong(p.id()));mapper.student(Long.parseLong(p.id()),nickname,grade);return current(a);}
    @Transactional public void seedAdmin(String password,PasswordEncoder encoder){if(password==null||password.length()<16)throw new IllegalStateException("A separate random admin password is required");if(mapper.findByName("admin")==null)mapper.register("admin",encoder.encode(password),"ADMIN");}
    public Profile current(Authentication authentication){
        if(authentication==null)throw new BadCredentialsException("Invalid session");
        Account a=mapper.findByName(authentication.getName());
        if(a==null||!"ACTIVE".equals(a.status())||a.nickname()==null)throw new BadCredentialsException("Invalid session");
        return new Profile(Long.toString(a.id()),a.loginName(),a.nickname(),a.role(),a.grade());
    }
    @Transactional public void seed(String name,String nickname,String password,PasswordEncoder encoder){
        Account a=mapper.findByName(name);
        if(a==null){mapper.insertAccount(name,encoder.encode(password));a=mapper.findByName(name);}
        if(a.nickname()==null)mapper.insertProfile(a.id(),nickname);
    }
}
