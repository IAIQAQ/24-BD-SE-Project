package cn.edu.hust.mathdrill.family;
import cn.edu.hust.mathdrill.common.ApiResponse;
import cn.edu.hust.mathdrill.practice.PracticeService;
import cn.edu.hust.mathdrill.practice.ReviewPracticeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/family") public class FamilyController {
 private final FamilyService service;public FamilyController(FamilyService service){this.service=service;}
 public record Request(@NotBlank @Size(max=64)String studentLoginName){} public record Confirmation(boolean confirm){} public record Response(boolean accept){}
 @GetMapping("/links") Object links(Authentication a){return ApiResponse.ok(service.relations(a));}
 @PostMapping("/requests") Object request(Authentication a,@Valid @RequestBody Request input){return ApiResponse.ok(service.request(a,input.studentLoginName()));}
 @PostMapping("/links/{id}/respond") Object respond(Authentication a,@PathVariable long id,@RequestBody Response input){return ApiResponse.ok(service.respond(a,id,input.accept()));}
 @PostMapping("/links/{id}/unbind") Object unbind(Authentication a,@PathVariable long id,@RequestBody Confirmation input){return ApiResponse.ok(service.unbind(a,id,input.confirm()));}
 @PostMapping("/children/{child}/previews") Object preview(Authentication a,@PathVariable long child,@Valid @RequestBody PracticeService.Create input){return ApiResponse.ok(service.preview(a,child,input));}
 @PostMapping("/previews/{id}/send") Object send(Authentication a,@PathVariable long id,@RequestBody Confirmation input){return ApiResponse.ok(service.send(a,id,input.confirm()));}
 @PostMapping("/practices/{id}/withdraw") Object withdraw(Authentication a,@PathVariable long id,@RequestBody Confirmation input){return ApiResponse.ok(service.withdraw(a,id,input.confirm()));}
 @GetMapping("/children/{child}/practices") Object tasks(Authentication a,@PathVariable long child){return ApiResponse.ok(service.tasks(a,child));}
 @GetMapping("/children/{child}/practices/{task}") Object view(Authentication a,@PathVariable long child,@PathVariable long task){return ApiResponse.ok(service.view(a,child,task));}
 @GetMapping("/children/{child}/mistakes") Object mistakes(Authentication a,@PathVariable long child){return ApiResponse.ok(service.mistakes(a,child));}
 @GetMapping("/children/{child}/stats") Object stats(Authentication a,@PathVariable long child,@RequestParam(required=false)String start,@RequestParam(required=false)String end){return ApiResponse.ok(service.stats(a,child,start,end));}
 @PostMapping("/children/{child}/review-options") Object reviewOptions(Authentication a,@PathVariable long child,@Valid @RequestBody ReviewPracticeService.Selection input){return ApiResponse.ok(service.reviewOptions(a,child,input));}
 @PostMapping("/children/{child}/reinforcement-previews") Object reinforcementPreview(Authentication a,@PathVariable long child,@Valid @RequestBody ReviewPracticeService.ReinforceInput input){return ApiResponse.ok(service.reinforcementPreview(a,child,input));}
 @GetMapping("/children/{child}/mistakes/{item}") Object mistake(Authentication a,@PathVariable long child,@PathVariable long item){return ApiResponse.ok(service.mistake(a,child,item));}

}
