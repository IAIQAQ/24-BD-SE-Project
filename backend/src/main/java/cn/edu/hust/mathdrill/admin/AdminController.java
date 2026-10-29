package cn.edu.hust.mathdrill.admin;
import cn.edu.hust.mathdrill.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/admin") public class AdminController {
 private final AdminService service;public AdminController(AdminService service){this.service=service;}
 public record Confirm(boolean confirm,Long expectedRevision){} public record Enabled(boolean enabled){} public record ErrorInput(boolean confirm,String reason){}
 @GetMapping("/overview") Object overview(Authentication a){return ApiResponse.ok(service.overview(a));}
 @GetMapping("/points") Object points(Authentication a){return ApiResponse.ok(service.points(a));}
 @PostMapping("/points") Object point(Authentication a,@Valid @RequestBody AdminService.PointInput input){return ApiResponse.ok(service.point(a,null,input));}
 @PutMapping("/points/{id}") Object point(Authentication a,@PathVariable long id,@Valid @RequestBody AdminService.PointInput input){return ApiResponse.ok(service.point(a,id,input));}
 @GetMapping("/questions") Object questions(Authentication a,@RequestParam(defaultValue="")String query,@RequestParam(defaultValue="1")int page){return ApiResponse.ok(service.questions(a,query,page));}
 @PostMapping("/questions") Object question(Authentication a,@Valid @RequestBody AdminService.QuestionInput input){return ApiResponse.ok(service.question(a,null,input));}
 @PutMapping("/questions/{id}") Object question(Authentication a,@PathVariable long id,@Valid @RequestBody AdminService.QuestionInput input){return ApiResponse.ok(service.question(a,id,input));}
 @PostMapping("/questions/{id}/review") Object review(Authentication a,@PathVariable long id,@RequestBody Confirm input){return ApiResponse.ok(service.review(a,id,input.confirm(),input.expectedRevision()));}
 @PostMapping("/questions/{id}/enabled") Object enabled(Authentication a,@PathVariable long id,@RequestBody Enabled input){return ApiResponse.ok(service.enabled(a,id,input.enabled()));}
 @GetMapping("/questions/{id}/versions") Object versions(Authentication a,@PathVariable long id){return ApiResponse.ok(service.versions(a,id));}
 @GetMapping("/questions/{id}/versions/{version}/impact") Object impact(Authentication a,@PathVariable long id,@PathVariable long version){return ApiResponse.ok(service.impact(a,id,version));}
 @PostMapping("/questions/{id}/versions/{version}/error") Object error(Authentication a,@PathVariable long id,@PathVariable long version,@RequestBody ErrorInput input){return ApiResponse.ok(service.error(a,id,version,input.confirm(),input.reason()));}
}
