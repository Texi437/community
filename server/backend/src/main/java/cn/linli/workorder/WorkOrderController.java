package cn.linli.workorder;

import cn.linli.auth.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.UUID;
import static cn.linli.workorder.WorkOrderDtos.*;

@RestController
@RequestMapping("/api/work-orders")
public class WorkOrderController {
    private final WorkOrderService service;
    public WorkOrderController(WorkOrderService service) { this.service=service; }

    @PostMapping(consumes=MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Detail create(@AuthenticationPrincipal AuthenticatedUser user,@Valid @RequestBody Create data) {
        return service.create(user,data,List.of());
    }
    @PostMapping(consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Detail createWithImages(@AuthenticationPrincipal AuthenticatedUser user,@Valid @RequestPart("data") Create data,
                                    @RequestPart(value="images",required=false) List<MultipartFile> images) {
        return service.create(user,data,images==null ? List.of() : images);
    }
    @GetMapping
    public Page list(@AuthenticationPrincipal AuthenticatedUser user,
                     @RequestParam(required=false) Status status,@RequestParam(required=false) Category category,
                     @RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size) {
        return service.list(user,status,category,page,size);
    }
    @GetMapping("/categories")
    public List<CategoryOption> categories() {
        return List.of(new CategoryOption(Category.ENVIRONMENT,"环境卫生"),new CategoryOption(Category.FACILITY,"设施维修"),
            new CategoryOption(Category.SECURITY,"安全隐患"),new CategoryOption(Category.NOISE,"噪声扰民"),new CategoryOption(Category.OTHER,"其他事项"));
    }
    @GetMapping("/staff")
    public List<Staff> staff(@AuthenticationPrincipal AuthenticatedUser user) { return service.staff(user); }
    @GetMapping("/{id}")
    public Detail detail(@AuthenticationPrincipal AuthenticatedUser user,@PathVariable UUID id) { return service.detail(user,id.toString()); }
    @PostMapping("/{id}/assign")
    public Detail assign(@AuthenticationPrincipal AuthenticatedUser user,@PathVariable UUID id,@Valid @RequestBody Assign data) { return service.assign(user,id.toString(),data); }
    @PostMapping("/{id}/reject")
    public Detail reject(@AuthenticationPrincipal AuthenticatedUser user,@PathVariable UUID id,@Valid @RequestBody Note data) { return service.reject(user,id.toString(),data); }
    @PostMapping("/{id}/accept")
    public Detail accept(@AuthenticationPrincipal AuthenticatedUser user,@PathVariable UUID id,@Valid @RequestBody Version data) { return service.accept(user,id.toString(),data.version()); }
    @PostMapping("/{id}/start")
    public Detail start(@AuthenticationPrincipal AuthenticatedUser user,@PathVariable UUID id,@Valid @RequestBody Version data) { return service.start(user,id.toString(),data.version()); }
    @PostMapping("/{id}/complete")
    public Detail complete(@AuthenticationPrincipal AuthenticatedUser user,@PathVariable UUID id,@Valid @RequestBody Note data) { return service.complete(user,id.toString(),data); }
    @PostMapping(value = "/{id}/complete", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Detail completeWithImages(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
                                     @Valid @RequestPart("data") Note data,
                                     @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        return service.complete(user, id.toString(), data, images == null ? List.of() : images);
    }
    @GetMapping("/{id}/completion-attachments")
    public List<CompletionAttachment> completionAttachments(@AuthenticationPrincipal AuthenticatedUser user,
                                                             @PathVariable UUID id) {
        return service.completionAttachments(user, id.toString());
    }
    @GetMapping("/{id}/attachments/{assetId}")
    public ResponseEntity<byte[]> image(@AuthenticationPrincipal AuthenticatedUser user,@PathVariable UUID id,@PathVariable UUID assetId) {
        var asset=service.attachment(user,id.toString(),assetId.toString());
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(asset.contentType())).cacheControl(CacheControl.noStore())
            .header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.inline().filename(asset.id()+(asset.contentType().equals("image/png")?".png":".jpg")).build().toString())
            .body(service.content(asset));
    }
}
