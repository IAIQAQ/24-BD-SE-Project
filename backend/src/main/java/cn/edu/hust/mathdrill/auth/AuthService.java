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
    public record Profile(String id,String loginName,String nickname,String role,int grade){}
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
