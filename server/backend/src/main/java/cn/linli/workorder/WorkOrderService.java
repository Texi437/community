package cn.linli.workorder;

import cn.linli.auth.AuthenticatedUser;
import cn.linli.common.ApiException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.UUID;
import static cn.linli.workorder.WorkOrderDtos.*;
import static cn.linli.common.DatabaseValues.pageOffset;

@Service
public class WorkOrderService {
    private final WorkOrderMapper mapper;
    private final ImageStorage storage;
    public WorkOrderService(WorkOrderMapper mapper, ImageStorage storage) { this.mapper=mapper; this.storage=storage; }

    @Transactional
    public Detail create(AuthenticatedUser user, Create data, List<MultipartFile> images) {
        requireRole(user,"RESIDENT");
        if (images.size()>3) { throw new ApiException(HttpStatus.BAD_REQUEST,"INVALID_INPUT","每单最多3张图片"); }
        String id=UUID.randomUUID().toString();
        try { mapper.insert(id,user.id(),data.clientRequestId().toString(),data); }
        catch (DuplicateKeyException ex) { throw conflict("DUPLICATE_SUBMISSION","这次报事已提交，请查看工单列表"); }
        for (var image: images) { mapper.insertAsset(storage.store(id,image)); }
        mapper.log(id,user.id(),user.role(),"CREATE",null,Status.NEW,null);
        return detail(user,id);
    }

    public Page list(AuthenticatedUser user, Status status, Category category, int page, int size) {
        long offset = pageOffset(page, size, "page须大于0，size须在1至50之间");
        return new Page(mapper.list(user.role(),user.id(),status,category,size,offset),
            mapper.count(user.role(),user.id(),status,category),page,size);
    }

    @Transactional(readOnly=true)
    public Detail detail(AuthenticatedUser user, String id) {
        Order order=readable(user,id);
        var attachments=mapper.assets(id).stream().map(a -> new Attachment(a.id(),a.contentType(),a.byteSize(),
            "/api/work-orders/"+id+"/attachments/"+a.id())).toList();
        return new Detail(order,mapper.logs(id),attachments);
    }

    public List<Staff> staff(AuthenticatedUser user) { requireRole(user,"ADMIN"); return mapper.staff(); }

    public FileAsset attachment(AuthenticatedUser user,String id,String assetId) {
        readable(user,id);
        var asset=mapper.asset(id,assetId);
        if(asset==null) { throw notFound(); }
        return asset;
    }
    public byte[] content(FileAsset asset) { return storage.read(asset); }

    @Transactional
    public Detail assign(AuthenticatedUser user,String id,Assign request) {
        requireRole(user,"ADMIN");
        Order order=locked(user,id,request.version(),Status.NEW);
        if(mapper.activeStaff(request.assigneeId())!=1) { throw new ApiException(HttpStatus.BAD_REQUEST,"INVALID_ASSIGNEE","只能指派给启用的工作人员"); }
        change(user,order,"ASSIGN",Status.ASSIGNED,request.assigneeId(),null);
        return detail(user,id);
    }

    @Transactional
    public Detail reject(AuthenticatedUser user,String id,Note request) {
        requireRole(user,"ADMIN");
        Order order=locked(user,id,request.version(),Status.NEW);
        change(user,order,"REJECT",Status.REJECTED,null,request.note());
        return detail(user,id);
    }

    @Transactional
    public Detail accept(AuthenticatedUser user,String id,int version) {
        return staffTransition(user,id,version,Status.ASSIGNED,Status.ACCEPTED,"ACCEPT",null);
    }
    @Transactional
    public Detail start(AuthenticatedUser user,String id,int version) {
        return staffTransition(user,id,version,Status.ACCEPTED,Status.IN_PROGRESS,"START",null);
    }
    @Transactional
    public Detail complete(AuthenticatedUser user,String id,Note request) {
        return complete(user, id, request, List.of());
    }

    @Transactional
    public Detail complete(AuthenticatedUser user, String id, Note request, List<MultipartFile> images) {
        requireRole(user, "STAFF");
        if (images.size() > 3) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "办结最多3张图片");
        }
        Order order = locked(user, id, request.version(), Status.IN_PROGRESS);
        long logId = change(user, order, "COMPLETE", Status.COMPLETED, order.assigneeId(), request.note());
        for (var image : images) {
            FileAsset asset = storage.store(id, image);
            mapper.insertAsset(asset);
            if (mapper.linkCompletionAsset(asset.id(), logId) != 1) {
                throw conflict("INVALID_COMPLETION_ATTACHMENT", "附件与办结记录不匹配");
            }
        }
        return detail(user, id);
    }

    @Transactional(readOnly = true)
    public List<CompletionAttachment> completionAttachments(AuthenticatedUser user, String id) {
        readable(user, id);
        return mapper.completionAttachments(id);
    }
    private Detail staffTransition(AuthenticatedUser user,String id,int version,Status expected,Status next,String action,String note) {
        requireRole(user,"STAFF");
        Order order=locked(user,id,version,expected);
        change(user,order,action,next,order.assigneeId(),note);
        return detail(user,id);
    }
    private Order locked(AuthenticatedUser user,String id,int version,Status expected) {
        if(mapper.lock(id)==null) { throw notFound(); }
        Order order=readable(user,id);
        if(order.version()!=version) { throw conflict("VERSION_CONFLICT","工单已更新，请刷新后重试"); }
        if(order.status()!=expected) { throw conflict("INVALID_TRANSITION","当前工单状态不允许此操作"); }
        return order;
    }
    private long change(AuthenticatedUser user,Order order,String action,Status next,Long assigneeId,String note) {
        if(mapper.transition(order.id(),order.version(),order.status(),next,assigneeId,note)!=1) { throw conflict("VERSION_CONFLICT","工单已更新，请刷新后重试"); }
        return mapper.log(order.id(),user.id(),user.role(),action,order.status(),next,note);
    }
    private Order readable(AuthenticatedUser user,String id) {
        Order order=mapper.find(id);
        if(order==null) { throw notFound(); }
        boolean allowed="ADMIN".equals(user.role()) || ("RESIDENT".equals(user.role()) && order.reporterId()==user.id())
            || ("STAFF".equals(user.role()) && order.assigneeId()!=null && order.assigneeId()==user.id());
        if(!allowed) { throw notFound(); }
        return order;
    }
    private static void requireRole(AuthenticatedUser user,String role) {
        if(!role.equals(user.role())) { throw new ApiException(HttpStatus.FORBIDDEN,"FORBIDDEN","无权执行此操作"); }
    }
    private static ApiException notFound() { return new ApiException(HttpStatus.NOT_FOUND,"NOT_FOUND","工单或附件不存在"); }
    private static ApiException conflict(String code,String message) { return new ApiException(HttpStatus.CONFLICT,code,message); }
}
