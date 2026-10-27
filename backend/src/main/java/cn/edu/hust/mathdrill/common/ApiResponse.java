package cn.edu.hust.mathdrill.common;
import java.util.Map;
import java.util.UUID;

public final class ApiResponse {
    private ApiResponse() {}
    public static Map<String,Object> ok(Object data) {return Map.of("code","OK","data",data,"traceId",UUID.randomUUID().toString());}
    public static Map<String,Object> error(String code,String message) {return Map.of("code",code,"message",message,"traceId",UUID.randomUUID().toString());}
}
