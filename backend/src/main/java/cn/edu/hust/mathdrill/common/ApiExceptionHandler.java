package cn.edu.hust.mathdrill.common;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(org.springframework.dao.DuplicateKeyException.class) ResponseEntity<?> duplicate(Exception e){return ResponseEntity.status(409).body(ApiResponse.error("DUPLICATE","内容或账号已存在，请勿重复提交"));}
    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class) ResponseEntity<?> business(org.springframework.web.server.ResponseStatusException e){String reason=e.getReason()==null?"操作失败":e.getReason();String[] parts=reason.split(": ",2);return ResponseEntity.status(e.getStatusCode()).body(ApiResponse.error(parts[0],parts.length>1?parts[1]:reason));}
    @ExceptionHandler(org.springframework.dao.PessimisticLockingFailureException.class) ResponseEntity<?> locking(Exception e){return ResponseEntity.status(503).body(ApiResponse.error("CHECKING","操作仍在处理，请稍后核对"));}
    @ExceptionHandler(AuthenticationException.class) ResponseEntity<?> credentials(AuthenticationException e){return ResponseEntity.status(401).body(ApiResponse.error("INVALID_CREDENTIALS","账号或密码错误，请重新输入"));}
    @ExceptionHandler({MethodArgumentNotValidException.class,HttpMessageNotReadableException.class}) ResponseEntity<?> invalid(Exception e){return ResponseEntity.badRequest().body(ApiResponse.error("INVALID_INPUT","请检查账号与密码格式"));}
}
