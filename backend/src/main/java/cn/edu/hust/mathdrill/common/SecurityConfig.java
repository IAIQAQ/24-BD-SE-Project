package cn.edu.hust.mathdrill.common;

import cn.edu.hust.mathdrill.auth.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.session.web.http.DefaultCookieSerializer;
import org.springframework.web.cors.*;

@Configuration
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder(){return PasswordEncoderFactories.createDelegatingPasswordEncoder();}
    @Bean HttpSessionCsrfTokenRepository csrfRepository(){return new HttpSessionCsrfTokenRepository();}
    @Bean HttpSessionSecurityContextRepository contextRepository(){return new HttpSessionSecurityContextRepository();}
    @Bean AuthenticationManager authenticationManager(AuthService users,PasswordEncoder encoder){
        DaoAuthenticationProvider provider=new DaoAuthenticationProvider(users);provider.setPasswordEncoder(encoder);return new ProviderManager(provider);
    }
    @Bean DefaultCookieSerializer cookieSerializer(){
        var cookie=new DefaultCookieSerializer();cookie.setCookieName("MATHDRILL_SESSION");cookie.setUseHttpOnlyCookie(true);cookie.setUseSecureCookie(true);cookie.setSameSite("None");cookie.setCookiePath("/");cookie.setCookieMaxAge(86400);return cookie;
    }
    @Bean SecurityFilterChain security(HttpSecurity http,HttpSessionCsrfTokenRepository csrf,HttpSessionSecurityContextRepository context,ObjectMapper json) throws Exception {
        http.cors(cors->{}).csrf(c->c.csrfTokenRepository(csrf)).securityContext(c->c.securityContextRepository(context))
            .authorizeHttpRequests(a->a.requestMatchers("/api/v1/health","/api/v1/auth/csrf","/api/v1/auth/login","/api/v1/auth/register").permitAll().requestMatchers("/api/v1/admin/**").hasRole("ADMIN").anyRequest().authenticated())
            .exceptionHandling(e->e.authenticationEntryPoint((req,res,error)->{res.setStatus(401);res.setContentType("application/json;charset=UTF-8");json.writeValue(res.getOutputStream(),ApiResponse.error("UNAUTHENTICATED","请先登录"));})
                .accessDeniedHandler((req,res,error)->{res.setStatus(403);res.setContentType("application/json;charset=UTF-8");json.writeValue(res.getOutputStream(),ApiResponse.error("FORBIDDEN","当前操作未获授权，请刷新登录状态后重试"));}))
            .logout(l->l.logoutUrl("/api/v1/auth/logout").invalidateHttpSession(true).deleteCookies("MATHDRILL_SESSION").logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)));
        return http.build();
    }
    @Bean CorsConfigurationSource corsConfigurationSource(@Value("${mathdrill.cors-origins}")String origins){
        var c=new CorsConfiguration();c.setAllowedOrigins(Arrays.stream(origins.split(",")).map(String::trim).toList());c.setAllowedMethods(List.of("GET","POST","PUT","OPTIONS"));c.setAllowedHeaders(List.of("Content-Type","X-CSRF-TOKEN"));c.setAllowCredentials(true);
        var source=new UrlBasedCorsConfigurationSource();source.registerCorsConfiguration("/api/v1/**",c);return source;
    }
    @Bean CommandLineRunner seed(AuthService users,PasswordEncoder encoder,@Value("${mathdrill.seed-enabled:false}")boolean enabled,@Value("${mathdrill.seed-password:}")String password,@Value("${mathdrill.admin-password:}")String adminPassword){
        return args->{if(enabled){if(password.length()<16)throw new IllegalStateException("A random development password is required");users.seed("student_a","小明",password,encoder);users.seed("student_b","小华",password,encoder);if(!adminPassword.isBlank())users.seedAdmin(adminPassword,encoder);}};
    }
}
