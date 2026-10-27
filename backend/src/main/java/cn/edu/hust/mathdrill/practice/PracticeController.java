package cn.edu.hust.mathdrill.practice;
import cn.edu.hust.mathdrill.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import static cn.edu.hust.mathdrill.practice.PracticeService.*;

@RestController @RequestMapping("/api/v1")
public class PracticeController {
    private final PracticeService service;
    public PracticeController(PracticeService service){this.service=service;}
    @GetMapping("/catalog/points") Object points(){return ApiResponse.ok(service.points());}
    @GetMapping("/practices") Object list(Authentication a,@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="20")int size){return ApiResponse.ok(service.list(a,page,size));}
    @PostMapping("/practices") Object create(Authentication a,@Valid @RequestBody Create input){return ApiResponse.ok(service.create(a,input));}
    @PostMapping("/practices/{id}/open") Object open(Authentication a,@PathVariable long id,@Valid @RequestBody Open input){return ApiResponse.ok(service.open(a,id,input));}
    @GetMapping("/practices/{id}/draft") Object draft(Authentication a,@PathVariable long id){return ApiResponse.ok(service.draft(a,id));}
    @PutMapping("/practices/{id}/answers") Object save(Authentication a,@PathVariable long id,@Valid @RequestBody Write input){return ApiResponse.ok(service.save(a,id,input));}
    @PostMapping("/practices/{id}/segments") Object timing(Authentication a,@PathVariable long id,@Valid @RequestBody SegmentInput input){return ApiResponse.ok(service.timing(a,id,input));}
    @PostMapping("/practices/{id}/submit") Object submit(Authentication a,@PathVariable long id,@Valid @RequestBody Write input){return ApiResponse.ok(service.submit(a,id,input));}
    @PostMapping("/practices/{id}/submission-status") Object probe(Authentication a,@PathVariable long id,@Valid @RequestBody Probe input){return ApiResponse.ok(service.reconcile(a,id,input));}
    @GetMapping("/practices/{id}/result") Object result(Authentication a,@PathVariable long id){return ApiResponse.ok(service.result(a,id));}
    @GetMapping("/students/{id}/mistakes") Object mistakes(Authentication a,@PathVariable String id,@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="20")int size,@RequestParam(required=false)String pointId){return ApiResponse.ok(service.mistakes(a,id,page,size,pointId));}
    @GetMapping("/students/{id}/mistakes/{item}") Object mistake(Authentication a,@PathVariable String id,@PathVariable long item){return ApiResponse.ok(service.mistake(a,id,item));}
}
