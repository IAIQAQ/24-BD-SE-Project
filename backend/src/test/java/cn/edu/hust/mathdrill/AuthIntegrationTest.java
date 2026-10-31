package cn.edu.hust.mathdrill;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"server.ssl.enabled=false", "mathdrill.seed-enabled=true"})
@AutoConfigureMockMvc
class AuthIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Value("${mathdrill.seed-password}") String password;
    record Browser(Cookie cookie,String token) {}
    Browser csrf(Cookie cookie) throws Exception {
        var request=get("/api/v1/auth/csrf").secure(true);
        if(cookie!=null)request.cookie(cookie);
        MvcResult result=mvc.perform(request).andExpect(status().isOk()).andReturn();
        Cookie received=result.getResponse().getCookie("MATHDRILL_SESSION");
        return new Browser(received==null?cookie:received,json.readTree(result.getResponse().getContentAsString()).at("/data/token").asText());
    }
    MvcResult signIn(Browser browser,String user,String pass) throws Exception {
        return mvc.perform(post("/api/v1/auth/login").secure(true).cookie(browser.cookie()).header("X-CSRF-TOKEN",browser.token())
            .contentType("application/json").content(json.writeValueAsString(java.util.Map.of("loginName",user,"password",pass)))).andReturn();
    }
    @Test void missingCsrfAndBadCredentialsAreRejected() throws Exception {
        mvc.perform(post("/api/v1/auth/login").secure(true).contentType("application/json").content("{\"loginName\":\"student_a\",\"password\":\"wrong\"}"))
            .andExpect(status().isForbidden());
        assertEquals(401,signIn(csrf(null),"student_a","wrong").getResponse().getStatus());
        assertEquals(401,signIn(csrf(null),"unknown_student","wrong").getResponse().getStatus());
    }
    @Test void loginRotatesCookieRestoresProfileAndLogoutRevokesSession() throws Exception {
        Browser anonymous=csrf(null);
        var result=signIn(anonymous,"student_a",password);
        assertEquals(200,result.getResponse().getStatus());
        Cookie cookie=result.getResponse().getCookie("MATHDRILL_SESSION");
        assertNotNull(cookie);assertNotEquals(anonymous.cookie().getValue(),cookie.getValue());
        assertTrue(cookie.isHttpOnly());assertTrue(cookie.getSecure());
        mvc.perform(get("/api/v1/auth/me").secure(true).cookie(cookie)).andExpect(status().isOk())
            .andExpect(jsonPath("$.data.loginName").value("student_a")).andExpect(jsonPath("$.data.passwordHash").doesNotExist());
        Browser authenticated=csrf(cookie);
        mvc.perform(post("/api/v1/auth/logout").secure(true).cookie(cookie).header("X-CSRF-TOKEN",authenticated.token())).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/auth/me").secure(true).cookie(cookie)).andExpect(status().isUnauthorized());
    }
    @Test void studentsCannotReadOneAnothersProfileAndPasswordsAreHashed() throws Exception {
        var result=signIn(csrf(null),"student_a",password);
        assertEquals(200,result.getResponse().getStatus());
        Cookie cookie=result.getResponse().getCookie("MATHDRILL_SESSION");
        JsonNode profile=json.readTree(result.getResponse().getContentAsString()).get("data");
        long other=jdbc.queryForObject("SELECT id FROM sys_account WHERE login_name='student_b'",Long.class);
        mvc.perform(get("/api/v1/students/"+profile.get("id").asText()+"/profile").secure(true).cookie(cookie)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/students/"+other+"/profile").secure(true).cookie(cookie)).andExpect(status().isForbidden());
        assertNotEquals(password,jdbc.queryForObject("SELECT password_hash FROM sys_account WHERE login_name='student_a'",String.class));
        assertTrue(jdbc.queryForObject("SELECT COUNT(*) FROM SPRING_SESSION",Integer.class)>0);
    }
    @Test void onlyConfiguredCorsOriginsReceiveCredentialsPermission() throws Exception {
        mvc.perform(options("/api/v1/auth/me").header("Origin","https://localhost").header("Access-Control-Request-Method","GET"))
            .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin","https://localhost"))
            .andExpect(header().string("Access-Control-Allow-Credentials","true"));
        mvc.perform(options("/api/v1/auth/me").header("Origin","https://unexpected.invalid").header("Access-Control-Request-Method","GET"))
            .andExpect(status().isForbidden()).andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
    @Test void expiredDatabaseSessionCannotReadProfile() throws Exception {
        var result=signIn(csrf(null),"student_a",password);
        Cookie cookie=result.getResponse().getCookie("MATHDRILL_SESSION");
        assertNotNull(cookie);
        String sessionId=new String(java.util.Base64.getDecoder().decode(cookie.getValue()),java.nio.charset.StandardCharsets.UTF_8);
        long lastAccess=System.currentTimeMillis()-60000;
        assertEquals(1,jdbc.update("UPDATE SPRING_SESSION SET LAST_ACCESS_TIME=?,MAX_INACTIVE_INTERVAL=1,EXPIRY_TIME=? WHERE SESSION_ID=?",lastAccess,lastAccess+1000,sessionId));
        mvc.perform(get("/api/v1/auth/me").secure(true).cookie(cookie)).andExpect(status().isUnauthorized());
    }
}
