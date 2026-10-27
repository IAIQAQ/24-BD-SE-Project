package cn.edu.hust.mathdrill.common;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(AuthenticationException.class) ResponseEntity<?> credentials(AuthenticationException e){return ResponseEntity.status(401).body(ApiResponse.error("INVALID_CREDENTIALS","账号或密码错误，请重新输入"));}
    @ExceptionHandler({MethodArgumentNotValidException.class,HttpMessageNotReadableException.class}) ResponseEntity<?> invalid(Exception e){return ResponseEntity.badRequest().body(ApiResponse.error("INVALID_INPUT","请检查账号与密码格式"));}
}
