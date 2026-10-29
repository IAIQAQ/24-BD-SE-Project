package cn.edu.hust.mathdrill.practice;
import cn.edu.hust.mathdrill.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1") public class ReviewController {
 private final ReviewPracticeService service;public ReviewController(ReviewPracticeService service){this.service=service;}
 @PostMapping("/students/{id}/review-options") Object options(Authentication a,@PathVariable long id,@Valid @RequestBody ReviewPracticeService.Selection input){return ApiResponse.ok(service.options(a,id,input));}
 @PostMapping("/practices/replays") Object replay(Authentication a,@Valid @RequestBody ReviewPracticeService.ReplayInput input){return ApiResponse.ok(service.replay(a,input));}
 @PostMapping("/practices/reinforcements") Object reinforce(Authentication a,@Valid @RequestBody ReviewPracticeService.ReinforceInput input){return ApiResponse.ok(service.reinforce(a,input));}
}
