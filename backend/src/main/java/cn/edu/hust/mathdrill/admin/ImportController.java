package cn.edu.hust.mathdrill.admin;
import cn.edu.hust.mathdrill.common.ApiResponse;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
@RestController @RequestMapping("/api/v1/admin") public class ImportController {
 private final ImportService service;public ImportController(ImportService service){this.service=service;}
 public record Confirm(boolean confirm){}
 @GetMapping("/import-template") ResponseEntity<byte[]> template(Authentication a){return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=mathdrill-template.xlsx").contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")).body(service.template(a));}
 @PostMapping("/imports") Object upload(Authentication a,@RequestParam MultipartFile file){return ApiResponse.ok(service.upload(a,file));}
 @GetMapping("/imports/{id}") Object status(Authentication a,@PathVariable String id){return ApiResponse.ok(service.status(a,id));}
 @PostMapping("/imports/{id}/confirm") Object confirm(Authentication a,@PathVariable String id,@RequestBody Confirm input){return ApiResponse.ok(service.confirm(a,id,input.confirm()));}
}
