package cn.linli.workorder;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class WorkOrderDtos {
    private WorkOrderDtos() { }
    public enum Category { ENVIRONMENT, FACILITY, SECURITY, NOISE, OTHER }
    public enum Status { NEW, ASSIGNED, ACCEPTED, IN_PROGRESS, COMPLETED, REJECTED }
    public record Create(@NotNull UUID clientRequestId, @NotBlank @Size(max=100) String title,
                         @NotNull Category category, @NotBlank @Size(max=200) String location,
                         @NotBlank @Size(max=2000) String description) { }
    public record Version(@NotNull @Min(0) Integer version) { }
    public record Assign(@NotNull @Min(0) Integer version, @NotNull @Positive Long assigneeId) { }
    public record Note(@NotNull @Min(0) Integer version, @NotBlank @Size(max=1000) String note) { }
    public record Order(String id, long reporterId, String reporterNickname, Long assigneeId,
                        String assigneeNickname, String title, Category category, String location,
                        String description, Status status, int version, String result,
                        Instant createdAt, Instant updatedAt) {
        @JsonProperty("displayStatus")
        public String displayStatus() {
            return switch(status) {
                case NEW, ASSIGNED -> "待受理";
                case ACCEPTED, IN_PROGRESS -> "处理中";
                case COMPLETED -> "已完成";
                case REJECTED -> "已驳回";
            };
        }
    }
    public record Log(long id, long actorId, String actorNickname, String actorRole, String action,
                      String fromStatus, String toStatus, String note, Instant createdAt) { }
    public record Attachment(String id, String contentType, long byteSize, String url) { }
    public record CompletionAttachment(String assetId, long logId) { }
    public record Detail(Order workOrder, List<Log> timeline, List<Attachment> attachments) { }
    public record Page(List<Order> items, long total, int page, int size) { }
    public record Staff(long id, String nickname) { }
    public record CategoryOption(Category code, String label) { }
}
