package cn.linli.activity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

public final class ActivityDtos {
    static final String UUID_PATTERN = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}";

    private ActivityDtos() {}

    public enum ActivityStatus { PENDING_REVIEW, REJECTED, PUBLISHED, ENDED, CANCELLED }
    public enum EnrollmentStatus { CONFIRMED, WITHDRAWN, CANCELLED }
    public enum ReviewMethod { AUTO, MANUAL }
    public enum ReviewReason { VENUE_HOURS, SPEAKER }

    public record CreateRequest(
        @NotBlank @Pattern(regexp = UUID_PATTERN) String clientRequestId,
        @NotBlank @Pattern(regexp = "[A-Z0-9_]{1,32}") String topicCode,
        @NotBlank @Size(max = 100) String title,
        @NotBlank @Size(max = 2000) String description,
        @NotNull @Positive Long venueId,
        @NotNull Instant startsAt,
        @NotNull Instant endsAt,
        @NotNull @Positive Integer capacity,
        @NotNull Boolean usesSpeaker
    ) {}

    public record ReviewRequest(@NotNull @PositiveOrZero Integer version, @NotNull Boolean approved) {}
    public record VersionRequest(@NotNull @PositiveOrZero Integer version) {}

    public record Venue(long id, String name, int capacity, LocalTime opensAt, LocalTime closesAt,
                        boolean allowsSpeaker, String ruleText, String status) {}

    public record ActivityView(String id, String topicCode, String title, String description,
                               long venueId, String venueName, Instant startsAt, Instant endsAt,
                               int capacity, boolean usesSpeaker, ActivityStatus status, int version,
                               ReviewMethod reviewMethod, List<ReviewReason> reviewReasons, Instant reviewedAt,
                               String organizerNickname, boolean organizer, long enrollmentCount,
                               EnrollmentStatus enrollmentStatus, boolean registrationOpen) {}

    public record Participant(String nickname) {}
    public record Page<T>(List<T> items, long total, int page, int size) {}
}
