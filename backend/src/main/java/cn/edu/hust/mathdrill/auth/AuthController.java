package cn.edu.hust.mathdrill.auth;
import cn.edu.hust.mathdrill.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.web.bind.annotation.*;

@RestController
public class AuthController {
    private final AuthenticationManager manager;
    private final AuthService users;
    private final HttpSessionSecurityContextRepository contexts;
    private final HttpSessionCsrfTokenRepository csrf;
    private final org.springframework.security.crypto.password.PasswordEncoder encoder;
    public AuthController(AuthenticationManager manager,AuthService users,HttpSessionSecurityContextRepository contexts,HttpSessionCsrfTokenRepository csrf,org.springframework.security.crypto.password.PasswordEncoder encoder){this.manager=manager;this.users=users;this.contexts=contexts;this.csrf=csrf;this.encoder=encoder;}
    public record Credentials(@NotBlank @Size(max=64)String loginName,@NotBlank String password){}
    public record Registration(@NotBlank @Size(max=64)String loginName,@NotBlank @Size(max=128)String password,@NotBlank @Size(max=64)String nickname,@NotBlank String role,Integer grade){}
    public record ProfileInput(@NotBlank @Size(max=64)String nickname,int grade){}
    @PostMapping("/api/v1/auth/register") public Object register(@Valid @RequestBody Registration input){return ApiResponse.ok(users.register(input.loginName(),input.password(),input.nickname(),input.role(),input.grade(),encoder));}
    @PutMapping("/api/v1/auth/profile") public Object profile(Authentication a,@Valid @RequestBody ProfileInput input){return ApiResponse.ok(users.updateProfile(a,input.nickname(),input.grade()));}
    @GetMapping("/api/v1/auth/csrf") public Object csrf(CsrfToken token){return ApiResponse.ok(Map.of("token",token.getToken(),"headerName",token.getHeaderName()));}
    @PostMapping("/api/v1/auth/login") public Object login(@Valid @RequestBody Credentials input,HttpServletRequest request,HttpServletResponse response){
        Authentication authentication=manager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(input.loginName(),input.password()));
        new ChangeSessionIdAuthenticationStrategy().onAuthentication(authentication,request,response);
        var context=SecurityContextHolder.createEmptyContext();context.setAuthentication(authentication);SecurityContextHolder.setContext(context);contexts.saveContext(context,request,response);
        csrf.saveToken(null,request,response);
        return ApiResponse.ok(users.current(authentication));
    }
    @GetMapping("/api/v1/auth/me") public Object me(Authentication authentication){return ApiResponse.ok(users.current(authentication));}
    @GetMapping("/api/v1/students/{id}/profile") public Object profile(@PathVariable String id,Authentication authentication){
        var profile=users.current(authentication);if(!profile.id().equals(id))throw new AccessDeniedException("Student ownership mismatch");return ApiResponse.ok(profile);
    }
}
