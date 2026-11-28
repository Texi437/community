package cn.linli.interest;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class InterestDtos {
    private InterestDtos() {}

    public enum Intent { UNKNOWN, WILLING, NOT_NOW }

    public record Topic(String code, String name, int sortOrder) {}

    public record Interests(
        @NotNull @Size(max = 100) Map<@NotBlank @Size(max = 32) String, @NotNull @Min(0) @Max(5) Integer> strengths
    ) {}

    public record Slot(@NotNull Instant startsAt, @NotNull Instant endsAt) {}

    public record ParticipationWrite(
        @NotNull Intent intent, Instant validUntil, @NotNull List<@NotNull @Valid Slot> slots,
        @NotNull @Min(0) Integer version
    ) {}

    public record Participation(Intent intent, Instant validUntil, List<Slot> slots, int version, boolean expired) {}

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record Opportunity(
        String id, String topicCode, long venueId, Instant startsAt, Instant endsAt, String title, String reason,
        Integer potentialCount, Integer willingCount, Integer availableCount, boolean showExactCount,
        String sourceType, String sourceVersion, Instant generatedAt, Instant expiresAt
    ) {}
}
